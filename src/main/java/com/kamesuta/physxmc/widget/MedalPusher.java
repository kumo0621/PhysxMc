package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * メダルゲーム用のプッシャークラス
 * 基本壁と動的押し出し部分の2つの物理オブジェクトで構成
 */
public class MedalPusher {
    
    @Getter
    private final Location centerLocation;
    @Getter
    private final int height;
    @Getter
    private final int width;
    @Getter
    private final double moveRange;
    @Getter
    private final Material pusherMaterial;
    @Getter
    private final double speed;  // 個別速度
    
    private DisplayedPhysxBox baseWall;     // 基本の固定壁
    private DisplayedPhysxBox dynamicPart;  // 動的な押し出し部分
    private double currentPosition = 0.0;   // 0.0からmoveRangeまでの位置
    private boolean extending = true;       // 伸び中かどうか
    
    /**
     * プッシャーを作成
     * @param location 中心位置
     * @param height 高さ（ブロック数）
     * @param width 横幅（ブロック数）
     * @param moveRange 伸びる範囲（ブロック数）
     * @param material プッシャーのブロック
     * @param speed 個別の動作速度
     */
    public MedalPusher(Location location, int height, int width, double moveRange, Material material, double speed) {
        // プレイヤーの向きに関係なく、常に北向き（Z軸負方向）に固定
        this.centerLocation = location.clone();
        this.centerLocation.setYaw(0);    // 北向き固定
        this.centerLocation.setPitch(0);  // 水平固定
        
        this.height = height;
        this.width = width;
        this.moveRange = moveRange;
        this.pusherMaterial = material;
        this.speed = speed;  // 個別速度を保存
        
        createPusher();
    }
    
    /**
     * プッシャーの物理オブジェクトを作成
     */
    private void createPusher() {
        ItemStack pusherItem = new ItemStack(pusherMaterial);
        List<Vector> offsets = List.of(new Vector()); // 単一のオブジェクト
        
        // 1. 基本壁の作成（固定、厚さ0.5ブロック）
        Vector baseWallScale = new Vector(width, height, 0.5);
        Location baseWallLocation = centerLocation.clone();
        
        baseWall = PhysxMc.displayedBoxHolder.createDisplayedBox(
            baseWallLocation,
            baseWallScale,
            pusherItem,
            offsets,
            1000.0f // 重い密度で確実な衝突
        );
        baseWall.makeKinematic(true); // 重力の影響を受けない固定壁
        
        // 2. 動的押し出し部分の作成（移動する、厚さ可変）
        Vector dynamicPartScale = new Vector(width, height, 0.1); // 初期は薄く
        Location dynamicPartLocation = centerLocation.clone();
        dynamicPartLocation.add(0, 0, -0.75); // 基本壁の前方に配置
        
        dynamicPart = PhysxMc.displayedBoxHolder.createDisplayedBox(
            dynamicPartLocation,
            dynamicPartScale,
            pusherItem,
            offsets,
            1000.0f // 重い密度で確実な押し出し
        );
        dynamicPart.makeKinematic(true); // 重力の影響を受けず、手動制御
    }
    
    /**
     * プッシャーの更新処理（毎tick呼び出される）
     */
    public void update() {
        if (!isValid()) {
            return;
        }
        
        // 個別に設定された速度を使用
        double moveSpeed = this.speed;
        
        // 位置変化を決定
        if (extending) {
            currentPosition += moveSpeed;
            if (currentPosition >= moveRange) {
                currentPosition = moveRange;
                extending = false;
            }
        } else {
            currentPosition -= moveSpeed;
            if (currentPosition <= 0.0) {
                currentPosition = 0.0;
                extending = true;
            }
        }
        
        // 動的部分の新しい位置を計算（Z軸方向に移動）
        Location newLocation = centerLocation.clone();
        newLocation.add(0, 0, -0.75 - currentPosition); // 基本壁から前方に押し出し
        
        // 動的部分を新しい位置に移動（角度は固定）
        Vector newPos = new Vector(newLocation.getX(), newLocation.getY(), newLocation.getZ());
        dynamicPart.moveKinematic(newPos, dynamicPart.getQuat());
    }
    
    /**
     * プッシャーを破壊
     */
    public void destroy() {
        if (baseWall != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(baseWall);
            baseWall = null;
        }
        if (dynamicPart != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(dynamicPart);
            dynamicPart = null;
        }
    }
    
    /**
     * プッシャーが有効かどうか
     */
    public boolean isValid() {
        return baseWall != null && !baseWall.isDisplayDead() 
            && dynamicPart != null && !dynamicPart.isDisplayDead();
    }
} 