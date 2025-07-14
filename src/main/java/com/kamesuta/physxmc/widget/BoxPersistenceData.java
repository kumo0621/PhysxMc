package com.kamesuta.physxmc.widget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ボックスオブジェクトの永続化用データクラス
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoxPersistenceData {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double scaleX;
    private double scaleY;
    private double scaleZ;
    private String materialName;
    private float density;
    private List<VectorData> offsets;
    
    // 物理状態（位置と回転）
    private double physicsX;
    private double physicsY;
    private double physicsZ;
    private float physicsQx;
    private float physicsQy;
    private float physicsQz;
    private float physicsQw;
    
    // 速度状態
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double angularVelocityX;
    private double angularVelocityY;
    private double angularVelocityZ;
    
    // プッシャーフラグ
    private boolean isPusher;
    
    // コインフラグ
    private boolean isCoin;
    
    /**
     * Vectorのオフセットを保存するためのデータクラス
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VectorData {
        private double x;
        private double y;
        private double z;
        
        public static VectorData fromVector(Vector vector) {
            return new VectorData(vector.getX(), vector.getY(), vector.getZ());
        }
        
        public Vector toVector() {
            return new Vector(x, y, z);
        }
    }
    
    /**
     * Locationを作成
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    /**
     * 物理座標からLocationを作成
     */
    public Location toPhysicsLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, physicsX, physicsY, physicsZ);
    }
    
    /**
     * Materialを作成
     */
    public Material toMaterial() {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.STONE; // デフォルトにフォールバック
        }
    }
    
    /**
     * ItemStackを作成
     */
    public ItemStack toItemStack() {
        return new ItemStack(toMaterial());
    }
    
    /**
     * スケールベクターを作成
     */
    public Vector toScale() {
        return new Vector(scaleX, scaleY, scaleZ);
    }
    
    /**
     * オフセットリストを作成
     */
    public List<Vector> toOffsets() {
        return offsets.stream()
                .map(VectorData::toVector)
                .collect(Collectors.toList());
    }
} 