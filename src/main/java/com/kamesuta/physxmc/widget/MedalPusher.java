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
 * 固定サイズの四角形が前後に位置移動する方式でメダルを押し出す
 * 本物のメダルゲームのプッシャーと同じ動作を再現
 */
public class MedalPusher {
    
    @Getter
    private final Location centerLocation;
    @Getter
    private final double height;
    @Getter
    private final int width;
    @Getter
    private final double length;  // 追加: 長さ（奥行き）
    @Getter
    private final double moveRange;
    @Getter
    private final Material pusherMaterial;
    @Getter
    private final double speed;  // 個別速度
    
    private DisplayedPhysxBox pusher;           // 固定サイズのプッシャーオブジェクト
    @Getter
    private double currentPosition = 0.0;       // 現在の位置（0.0から moveRange まで）
    @Getter
    private boolean movingForward = true;       // 前に進んでいるかどうか
    private final Vector pusherSize;            // プッシャーの固定サイズ
    
    /**
     * プッシャーを作成
     * @param location 中心位置
     * @param height 高さ（ブロック数、小数可）
     * @param width 横幅（ブロック数、正の整数）
     * @param length 長さ・奥行き（ブロック数）
     * @param moveRange 前後の移動範囲（ブロック数）
     * @param material プッシャーのブロック
     * @param speed 個別の動作速度
     */
    public MedalPusher(Location location, double height, int width, double length, double moveRange, Material material, double speed) {
        this(location, height, width, length, moveRange, material, speed, 0.0, true);
    }
    
    /**
     * プッシャーを作成（状態復元用）
     * @param location 中心位置
     * @param height 高さ（ブロック数、小数可）
     * @param width 横幅（ブロック数、正の整数）
     * @param length 長さ・奥行き（ブロック数）
     * @param moveRange 前後の移動範囲（ブロック数）
     * @param material プッシャーのブロック
     * @param speed 個別の動作速度
     * @param currentPosition 現在の位置（復元用）
     * @param movingForward 移動方向（復元用）
     */
    public MedalPusher(Location location, double height, int width, double length, double moveRange, Material material, double speed, double currentPosition, boolean movingForward) {
        // プレイヤーの向きに関係なく、常に北向き（Z軸負方向）に固定
        this.centerLocation = location.clone();
        this.centerLocation.setYaw(0);    // 北向き固定
        this.centerLocation.setPitch(0);  // 水平固定
        
        this.height = height;
        this.width = width;
        this.length = length;  // 長さを保存
        this.moveRange = moveRange;
        this.pusherMaterial = material;
        this.speed = speed;  // 個別速度を保存
        this.currentPosition = currentPosition;  // 状態復元
        this.movingForward = movingForward;      // 状態復元
        
        // プッシャーの固定サイズを設定（幅, 高さ, 奥行き）
        // 奥行きは指定された長さを使用
        this.pusherSize = new Vector(width, height, length);
        
        createPusher();
        
        // 復元時は保存された状態に基づいて初期位置を設定
        // 次のupdate()で正しい位置に移動する
        if (currentPosition > 0.0) {
            // 保存された位置に即座に移動
            Location restoredLocation = centerLocation.clone();
            restoredLocation.add(0, 0, -currentPosition); // 北向きに移動
            pusher.moveKinematic(restoredLocation);
        }
        
        // 復元時のデバッグログ
        PhysxMc.getPlugin(PhysxMc.class).getLogger().info(
            String.format("プッシャー復元: 位置 %.2f/%.2f, 方向: %s, サイズ: %dx%.1fx%.1f", 
                currentPosition, moveRange, movingForward ? "前進" : "後退",
                width, height, length)
        );
    }
    
    /**
     * 固定サイズのプッシャー物理オブジェクトを作成
     */
    private void createPusher() {
        ItemStack pusherItem = new ItemStack(pusherMaterial);
        List<Vector> offsets = List.of(new Vector()); // 単一のオブジェクト
        
        // 固定サイズのプッシャーオブジェクトを作成（プッシャーフラグ付き）
        pusher = PhysxMc.displayedBoxHolder.createDisplayedBox(
            centerLocation.clone(),
            pusherSize.clone(), // 固定サイズ（長さ含む）
            pusherItem,
            offsets,
            1000.0f, // 重い密度で確実な衝突
            true     // プッシャーフラグ
        );
        pusher.makeKinematic(true); // 重力の影響を受けない、手動制御
    }
    
    /**
     * プッシャーの更新処理（毎tick呼び出される）
     * 固定サイズの四角形が前後に位置移動してメダルを押し出す
     */
    public void update() {
        if (!isValid()) {
            return;
        }
        
        // 個別に設定された速度を使用
        double moveSpeed = this.speed;
        
        // 前後の位置移動を計算
        if (movingForward) {
            currentPosition += moveSpeed;
            if (currentPosition >= moveRange) {
                currentPosition = moveRange;
                movingForward = false; // 後退開始
            }
        } else {
            currentPosition -= moveSpeed;
            if (currentPosition <= 0.0) {
                currentPosition = 0.0;
                movingForward = true; // 前進開始
            }
        }
        
        // プッシャーの新しい位置を計算（Z軸方向に移動）
        Location newLocation = centerLocation.clone();
        newLocation.add(0, 0, -currentPosition); // 北向きに押し出し
        
        // 固定サイズのプッシャーを新しい位置に移動（キネマティック制御）
        pusher.moveKinematic(newLocation);
    }
    
    /**
     * プッシャーを破壊
     */
    public void destroy() {
        if (pusher != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(pusher);
            pusher = null;
        }
    }
    
    /**
     * プッシャーが有効かどうか
     */
    public boolean isValid() {
        return pusher != null && !pusher.isDisplayDead();
    }
    
    /**
     * プッシャーが存在するかどうか（保存用の判定）
     * より緩い条件でプッシャーの存在を判定
     */
    public boolean exists() {
        return pusher != null;
    }
    
    /**
     * プッシャーの詳細状態を取得（デバッグ用）
     */
    public String getStatusInfo() {
        if (pusher == null) {
            return "プッシャーオブジェクトがnull";
        } else if (pusher.isDisplayDead()) {
            return "プッシャーのディスプレイが削除済み";
        } else {
            return String.format("正常 (位置: %.2f/%.2f, 方向: %s)", 
                currentPosition, moveRange, movingForward ? "前進" : "後退");
        }
    }
} 