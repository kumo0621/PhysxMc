package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
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
 * 指定された位置で壁のサイズが変化するプッシャー
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
    
    private DisplayedPhysxBox pusherBox;
    private double currentScale = 1.0; // 1.0から1.0+moveRangeの間で変化
    private boolean extending = true; // 伸び中かどうか
    private final double scaleSpeed = 0.02; // スケール変化速度
    private final Vector baseScale; // 基本サイズ
    
    /**
     * プッシャーを作成
     * @param location 中心位置
     * @param height 高さ（ブロック数）
     * @param width 横幅（ブロック数）
     * @param moveRange 伸びる範囲（倍率）
     * @param material プッシャーのブロック
     */
    public MedalPusher(Location location, int height, int width, double moveRange, Material material) {
        // プレイヤーの向きに関係なく、常に北向き（Z軸負方向）に固定
        this.centerLocation = location.clone();
        this.centerLocation.setYaw(0);    // 北向き固定
        this.centerLocation.setPitch(0);  // 水平固定
        
        this.height = height;
        this.width = width;
        this.moveRange = moveRange;
        this.pusherMaterial = material;
        this.baseScale = new Vector(width, height, 1.0); // Z軸（奥行き）が変化する
        
        createPusher();
    }
    
    /**
     * プッシャーの物理オブジェクトを作成
     */
    private void createPusher() {
        ItemStack pusherItem = new ItemStack(pusherMaterial);
        Vector scale = baseScale.clone(); // 基本サイズから開始
        List<Vector> offsets = List.of(new Vector()); // 単一のオブジェクト
        
        // プッシャーの位置（固定）
        Location pusherLocation = centerLocation.clone();
        
        // プッシャーを作成（キネマティック設定で重力の影響を受けない）
        pusherBox = PhysxMc.displayedBoxHolder.createDisplayedBox(
            pusherLocation,
            scale,
            pusherItem,
            offsets,
            1000.0f // 重い密度でしっかりとした押し出し力を確保
        );
        
        // キネマティックに設定（重力の影響を受けず、位置は固定）
        pusherBox.makeKinematic(true);
    }
    
    /**
     * プッシャーの更新処理（毎tick呼び出される）
     */
    public void update() {
        if (pusherBox == null || pusherBox.isDisplayDead()) {
            return;
        }
        
        // スケール変化を決定
        if (extending) {
            currentScale += scaleSpeed;
            if (currentScale >= 1.0 + moveRange) {
                currentScale = 1.0 + moveRange;
                extending = false;
            }
        } else {
            currentScale -= scaleSpeed;
            if (currentScale <= 1.0) {
                currentScale = 1.0;
                extending = true;
            }
        }
        
        // 新しいスケールを計算（Z軸のみ変化、角度は固定）
        Vector newScale = new Vector(
            baseScale.getX(),           // 幅は固定
            baseScale.getY(),           // 高さは固定
            baseScale.getZ() * currentScale  // 奥行きのみ変化
        );
        
        // プッシャーの表示スケールを更新（位置と角度は固定）
        pusherBox.updateScale(newScale);
    }
    
    /**
     * プッシャーを破壊
     */
    public void destroy() {
        if (pusherBox != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(pusherBox);
            pusherBox = null;
        }
    }
    
    /**
     * プッシャーが有効かどうか
     */
    public boolean isValid() {
        return pusherBox != null && !pusherBox.isDisplayDead();
    }
} 