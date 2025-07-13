package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxSphere;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ジャックポットチャンス機能
 * 回転する円盤でボールを転がして穴に落とす物理抽選システム
 */
public class JackpotRoulette {
    
    private final Location centerLocation;
    private final double radius;
    private final double rotationSpeed;
    private final Material diskMaterial;
    private final Material winSlotMaterial;
    private final Material normalSlotMaterial;
    
    @Getter
    private DisplayedPhysxBox rotatingDisk; // 回転する円盤
    private final List<RouletteSlot> slots = new ArrayList<>(); // 穴（スロット）
    
    private double currentRotation = 0.0; // 現在の回転角度
    private final Logger logger;
    
    // 設定値
    private static final int SLOT_COUNT = 5; // 穴の数
    private static final double DISK_THICKNESS = 0.2; // 円盤の厚さ
    private static final double SLOT_DEPTH = 1.0; // 穴の深さ
    private static final double SLOT_WIDTH = 1.5; // 穴の幅
    
    /**
     * ジャックポットルーレットを作成
     * @param centerLocation 中心位置
     * @param radius 円盤の半径
     * @param rotationSpeed 回転速度（ラジアン/tick）
     * @param diskMaterial 円盤の材質
     * @param winSlotMaterial 当たり穴の材質
     * @param normalSlotMaterial 通常穴の材質
     */
    public JackpotRoulette(Location centerLocation, double radius, double rotationSpeed, 
                          Material diskMaterial, Material winSlotMaterial, Material normalSlotMaterial) {
        this.centerLocation = centerLocation.clone();
        this.radius = radius;
        this.rotationSpeed = rotationSpeed;
        this.diskMaterial = diskMaterial;
        this.winSlotMaterial = winSlotMaterial;
        this.normalSlotMaterial = normalSlotMaterial;
        this.logger = PhysxMc.getPlugin(PhysxMc.class).getLogger();
        
        createRoulette();
    }
    
    /**
     * ルーレット全体を作成
     */
    private void createRoulette() {
        createRotatingDisk();
        createSlots();
        logger.info("ジャックポットルーレットを作成: 半径" + radius + ", 穴" + SLOT_COUNT + "個");
    }
    
    /**
     * 回転する円盤を作成
     */
    private void createRotatingDisk() {
        Vector diskSize = new Vector(radius * 2, DISK_THICKNESS, radius * 2);
        ItemStack diskItem = new ItemStack(diskMaterial);
        List<Vector> offsets = List.of(new Vector()); // 単一のディスプレイ
        
        rotatingDisk = PhysxMc.displayedBoxHolder.createDisplayedBox(
            centerLocation.clone(),
            diskSize,
            diskItem,
            offsets,
            500.0f, // 重い密度で安定性確保
            false   // プッシャーではない
        );
        
        if (rotatingDisk != null) {
            rotatingDisk.makeKinematic(true); // キネマティック制御で回転
            logger.info("回転円盤を作成: サイズ " + diskSize);
        } else {
            logger.warning("回転円盤の作成に失敗しました");
        }
    }
    
    /**
     * 穴（スロット）を作成
     */
    private void createSlots() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            double angle = (2 * Math.PI * i) / SLOT_COUNT; // 等間隔で配置
            boolean isWinSlot = (i == 0); // 最初の穴を当たり穴に設定
            
            // 穴の位置を計算（円盤の外周）
            double slotX = centerLocation.getX() + Math.cos(angle) * (radius + SLOT_WIDTH / 2);
            double slotZ = centerLocation.getZ() + Math.sin(angle) * (radius + SLOT_WIDTH / 2);
            double slotY = centerLocation.getY() - SLOT_DEPTH / 2; // 少し下に配置
            
            Location slotLocation = new Location(centerLocation.getWorld(), slotX, slotY, slotZ);
            Material slotMaterial = isWinSlot ? winSlotMaterial : normalSlotMaterial;
            
            RouletteSlot slot = new RouletteSlot(i, slotLocation, slotMaterial, isWinSlot, angle);
            slots.add(slot);
            
            logger.info("スロット" + i + "を作成: " + (isWinSlot ? "当たり" : "通常") + " at " + 
                       String.format("%.1f,%.1f,%.1f", slotX, slotY, slotZ));
        }
    }
    
    /**
     * 毎tick更新処理
     */
    public void update() {
        if (rotatingDisk != null && !rotatingDisk.isDisplayDead()) {
            // 円盤を回転
            currentRotation += rotationSpeed;
            if (currentRotation >= 2 * Math.PI) {
                currentRotation -= 2 * Math.PI; // 2πで正規化
            }
            
            // Y軸周りの回転を適用
            Quaternionf rotation = new Quaternionf().rotateY((float) currentRotation);
            rotatingDisk.moveKinematic(new Vector(centerLocation.getX(), centerLocation.getY(), centerLocation.getZ()), rotation);
        }
        
        // スロットの更新
        for (RouletteSlot slot : slots) {
            slot.update();
        }
    }
    
    /**
     * ボールを投入
     * @param player 投入したプレイヤー
     * @return 投入されたボールのオブジェクト
     */
    public DisplayedPhysxSphere launchBall(Player player) {
        // 中央より少し上の位置からボールを投入
        Location ballLocation = centerLocation.clone().add(0, 2, 0);
        
        // ボールを作成（適度な大きさと重さ）
        DisplayedPhysxSphere ball = PhysxMc.displayedSphereHolder.createDisplayedSphere(
            ballLocation,
            0.3, // 半径
            Material.GOLD_BLOCK, // 金色のボール
            3.0f // 密度
        );
        
        if (ball != null) {
            // 少しランダムな初期速度を与える
            double randomX = (Math.random() - 0.5) * 2.0;
            double randomZ = (Math.random() - 0.5) * 2.0;
            Vector initialVelocity = new Vector(randomX, -1.0, randomZ);
            
            // ボールに初期速度を適用
            if (ball.getActor() != null) {
                physx.common.PxVec3 velocity = new physx.common.PxVec3((float) initialVelocity.getX(), 
                                                                       (float) initialVelocity.getY(), 
                                                                       (float) initialVelocity.getZ());
                ball.getActor().setLinearVelocity(velocity);
                velocity.destroy();
            }
            
            logger.info(player.getName() + "がジャックポットボールを投入しました");
        } else {
            logger.warning("ボールの作成に失敗しました");
        }
        
        return ball;
    }
    
    /**
     * ボールがスロットに落ちたかチェック
     * @param ballLocation ボールの位置
     * @return 落ちたスロット、なければnull
     */
    public RouletteSlot checkBallInSlot(Location ballLocation) {
        for (RouletteSlot slot : slots) {
            if (slot.isLocationInSlot(ballLocation)) {
                return slot;
            }
        }
        return null;
    }
    
    /**
     * ルーレットが有効かどうか
     */
    public boolean isValid() {
        return rotatingDisk != null && !rotatingDisk.isDisplayDead() && !slots.isEmpty();
    }
    
    /**
     * ルーレットを破壊
     */
    public void destroy() {
        if (rotatingDisk != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(rotatingDisk);
            rotatingDisk = null;
        }
        
        for (RouletteSlot slot : slots) {
            slot.destroy();
        }
        slots.clear();
        
        logger.info("ジャックポットルーレットを破壊しました");
    }
    
    /**
     * 中心位置を取得
     */
    public Location getCenterLocation() {
        return centerLocation.clone();
    }
    
    /**
     * 穴の詳細情報を取得
     */
    public String getSlotInfo() {
        StringBuilder info = new StringBuilder();
        info.append("ジャックポットルーレット情報:\n");
        info.append("- 半径: ").append(radius).append("\n");
        info.append("- 回転速度: ").append(String.format("%.3f", rotationSpeed)).append(" rad/tick\n");
        info.append("- 穴の数: ").append(slots.size()).append("\n");
        
        for (int i = 0; i < slots.size(); i++) {
            RouletteSlot slot = slots.get(i);
            info.append("  穴").append(i).append(": ").append(slot.isWinSlot() ? "当たり" : "通常").append("\n");
        }
        
        return info.toString();
    }
}