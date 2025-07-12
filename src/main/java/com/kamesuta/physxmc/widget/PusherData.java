package com.kamesuta.physxmc.widget;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * プッシャーの設定を保存するためのデータクラス
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PusherData {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private double height;
    private int width;
    private double length;
    private double moveRange;
    private String materialName;
    private double speed;
    private double currentPosition;  // 現在の位置（動作状態）
    private boolean movingForward;   // 移動方向（動作状態）
    
    /**
     * MedalPusherからPusherDataを作成
     */
    public static PusherData fromPusher(MedalPusher pusher) {
        Location location = pusher.getCenterLocation();
        return new PusherData(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch(),
            pusher.getHeight(),
            pusher.getWidth(),
            pusher.getLength(),
            pusher.getMoveRange(),
            pusher.getPusherMaterial().name(),
            pusher.getSpeed(),
            pusher.getCurrentPosition(),
            pusher.isMovingForward()
        );
    }
    
    /**
     * PusherDataからLocationを作成
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    /**
     * PusherDataからMaterialを作成
     */
    public Material toMaterial() {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.IRON_BLOCK; // デフォルトにフォールバック
        }
    }
} 