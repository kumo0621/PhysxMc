package com.kamesuta.physxmc.widget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;

/**
 * ランプの設定を保存するためのデータクラス
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RampData {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double width;
    private double length;
    private double thickness;
    private String materialName;
    
    /**
     * DisplayedPhysxBoxからRampDataを作成
     */
    public static RampData fromRamp(DisplayedPhysxBox ramp) {
        Location location = ramp.getLocation();
        
        // DisplayMap の最初のデータからスケールとマテリアルを取得
        double width = 1.0, length = 1.0, thickness = 1.0;
        String materialName = "IRON_BLOCK";
        
        if (!ramp.displayMap.isEmpty()) {
            DisplayedPhysxBox.DisplayData firstDisplay = ramp.displayMap.get(0);
            if (firstDisplay.getDisplays().length > 0) {
                org.bukkit.entity.BlockDisplay blockDisplay = firstDisplay.getDisplays()[0];
                org.bukkit.util.Transformation transformation = blockDisplay.getTransformation();
                width = transformation.getScale().x;
                thickness = transformation.getScale().y;
                length = transformation.getScale().z;
                materialName = blockDisplay.getBlock().getMaterial().name();
            }
        }
        
        // 物理演算の実際の角度を取得（PhysXアクターから）
        float actualYaw = location.getYaw();
        float actualPitch = location.getPitch();
        
        if (ramp.getActor() != null && ramp.getActor().isReleasable()) {
            try {
                // PhysXからの回転情報を取得
                physx.common.PxTransform transform = ramp.getPos();
                physx.common.PxVec3 pos = transform.getP();
                physx.common.PxQuat quat = transform.getQ();
                
                // クォータニオンから角度を計算
                actualYaw = (float) Math.toDegrees(Math.atan2(2 * (quat.getW() * quat.getY() + quat.getX() * quat.getZ()), 
                    1 - 2 * (quat.getY() * quat.getY() + quat.getZ() * quat.getZ())));
                actualPitch = (float) Math.toDegrees(Math.asin(2 * (quat.getW() * quat.getX() - quat.getY() * quat.getZ())));
                
                // PhysXオブジェクトのクリーンアップ
                pos.destroy();
                quat.destroy();
                transform.destroy();
            } catch (Exception e) {
                // フォールバック: 表示位置の角度を使用
                actualYaw = location.getYaw();
                actualPitch = location.getPitch();
            }
        }
        
        return new RampData(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            actualYaw,
            actualPitch,
            width,
            length,
            thickness,
            materialName
        );
    }
    
    /**
     * RampDataからLocationを作成
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    /**
     * RampDataからMaterialを作成
     */
    public Material toMaterial() {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.STONE; // デフォルトにフォールバック
        }
    }
}