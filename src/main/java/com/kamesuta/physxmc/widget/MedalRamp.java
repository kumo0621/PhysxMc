package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * 物理演算ランプクラス
 * MedalPusherと同様のパターンでDisplayedPhysxBoxをラップ
 */
public class MedalRamp {
    
    @Getter
    private final Location location;
    @Getter
    private final double pitch;
    @Getter
    private final double width;
    @Getter
    private final double length;
    @Getter
    private final double thickness;
    @Getter
    private final Material material;
    
    private DisplayedPhysxBox rampBox;
    
    /**
     * ランプを作成
     * @param location 中心位置
     * @param pitch 傾斜角度(度)
     * @param width 幅
     * @param length 長さ
     * @param thickness 厚み
     * @param material マテリアル
     */
    public MedalRamp(Location location, double pitch, double width, double length, double thickness, Material material) {
        this.location = location.clone();
        this.pitch = pitch;
        this.width = width;
        this.length = length;
        this.thickness = thickness;
        this.material = material;
        
        createRamp();
    }
    
    /**
     * ランプの物理オブジェクトを作成
     */
    private void createRamp() {
        // ランプの位置と角度を設定
        Location rampLocation = location.clone();
        rampLocation.setPitch((float) pitch);
        
        Vector scale = new Vector(width, thickness, length);
        ItemStack item = new ItemStack(material);
        
        // pusherと同じパターンで作成（高密度、プッシャーフラグなし）
        rampBox = PhysxMc.displayedBoxHolder.createDisplayedBox(
            rampLocation,
            scale,
            item,
            List.of(new Vector()),
            1000.0f, // 高密度
            false    // プッシャーフラグはfalse
        );
        
        // pusherと同じパターンでキネマティック設定
        if (rampBox != null) {
            rampBox.makeKinematic(true);
        }
    }
    
    /**
     * ランプが有効かチェック
     */
    public boolean isValid() {
        return rampBox != null && !rampBox.isDisplayDead() && 
               rampBox.getActor() != null && rampBox.getActor().isReleasable();
    }
    
    /**
     * ランプの存在チェック（保存用）
     */
    public boolean exists() {
        return rampBox != null && !rampBox.isDisplayDead();
    }
    
    /**
     * ランプを破壊
     */
    public void destroy() {
        if (rampBox != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(rampBox);
        }
    }
    
    /**
     * ランプの中心位置を取得
     */
    public Location getCenterLocation() {
        return location.clone();
    }
    
    /**
     * ステータス情報を取得
     */
    public String getStatusInfo() {
        return String.format("ランプ(%.1f°, %.1fx%.1fx%.1f, %s) @ %.1f,%.1f,%.1f", 
            pitch, width, length, thickness, material.name(),
            location.getX(), location.getY(), location.getZ());
    }
    
    /**
     * 内部のDisplayedPhysxBoxを取得（保存用）
     */
    public DisplayedPhysxBox getRampBox() {
        return rampBox;
    }
}