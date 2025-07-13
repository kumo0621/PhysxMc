package com.kamesuta.physxmc.widget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * スフィアオブジェクトの永続化用データクラス
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpherePersistenceData {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double radius;
    private String materialName;
    private float density;
    
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
            return Material.GOLD_BLOCK; // デフォルトにフォールバック
        }
    }
} 