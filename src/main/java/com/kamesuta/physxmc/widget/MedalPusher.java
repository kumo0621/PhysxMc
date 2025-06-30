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
 * 単一の物理オブジェクトによるサイズ変化方式で伸び縮みを実現
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
    
    private DisplayedPhysxBox pusher;       // 単一のプッシャーオブジェクト
    private double currentScale = 0.5;      // 初期スケール（0.5から0.5+moveRangeまで）
    private boolean extending = true;       // 伸び中かどうか
    private final double baseScale = 0.5;   // 基本サイズ
    private final Vector originalScale;     // 元のスケール
    
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
        
        // 元のスケールを設定（幅, 高さ, 基本の奥行き）
        this.originalScale = new Vector(width, height, baseScale);
        
        createPusher();
    }
    
    /**
     * プッシャーの物理オブジェクトを作成
     */
    private void createPusher() {
        ItemStack pusherItem = new ItemStack(pusherMaterial);
        List<Vector> offsets = List.of(new Vector()); // 単一のオブジェクト
        
        // 単一のプッシャーオブジェクトを作成
        pusher = PhysxMc.displayedBoxHolder.createDisplayedBox(
            centerLocation.clone(),
            originalScale.clone(), // 初期スケール
            pusherItem,
            offsets,
            1000.0f // 重い密度で確実な衝突
        );
        pusher.makeKinematic(true); // 重力の影響を受けない、手動制御
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
            currentScale += moveSpeed;
            if (currentScale >= baseScale + moveRange) {
                currentScale = baseScale + moveRange;
                extending = false;
            }
        } else {
            currentScale -= moveSpeed;
            if (currentScale <= baseScale) {
                currentScale = baseScale;
                extending = true;
            }
        }
        
        // プッシャーの新しい位置を計算（Z軸方向に移動）
        Location newLocation = centerLocation.clone();
        double pushDistance = (currentScale - baseScale); // 0.0 から moveRange まで
        newLocation.add(0, 0, -pushDistance); // 北向きに押し出し
        
        // プッシャーを新しい位置に移動（キネマティック制御）
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
} 