package com.kamesuta.physxmc.widget;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * プッシャーを管理するクラス
 */
public class PusherManager {
    
    private final List<MedalPusher> pushers = new ArrayList<>();
    
    /**
     * 新しいプッシャーを作成して追加
     * @param location 中心位置
     * @param height 高さ（小数可）
     * @param width 横幅
     * @param length 長さ・奥行き
     * @param moveRange 動く幅
     * @param material プッシャーのブロック
     * @param speed 個別の動作速度
     * @return 作成されたプッシャー
     */
    public MedalPusher createPusher(Location location, double height, int width, double length, double moveRange, Material material, double speed) {
        MedalPusher pusher = new MedalPusher(location, height, width, length, moveRange, material, speed);
        pushers.add(pusher);
        return pusher;
    }
    
    /**
     * 全てのプッシャーを更新（毎tick呼び出される）
     */
    public void update() {
        Iterator<MedalPusher> iterator = pushers.iterator();
        while (iterator.hasNext()) {
            MedalPusher pusher = iterator.next();
            if (pusher.isValid()) {
                pusher.update();
            } else {
                pusher.destroy();
                iterator.remove();
            }
        }
    }
    
    /**
     * 全てのプッシャーを破壊
     */
    public void destroyAll() {
        for (MedalPusher pusher : pushers) {
            pusher.destroy();
        }
        pushers.clear();
    }
    
    /**
     * プッシャーの数を取得
     */
    public int getPusherCount() {
        return pushers.size();
    }
    
    /**
     * 指定した位置に最も近いプッシャーを削除
     * @param location 基準位置
     * @param maxDistance 最大距離
     * @return 削除されたかどうか
     */
    public boolean removeNearestPusher(Location location, double maxDistance) {
        MedalPusher nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (MedalPusher pusher : pushers) {
            double distance = pusher.getCenterLocation().distance(location);
            if (distance < nearestDistance && distance <= maxDistance) {
                nearestDistance = distance;
                nearest = pusher;
            }
        }
        
        if (nearest != null) {
            nearest.destroy();
            pushers.remove(nearest);
            return true;
        }
        
        return false;
    }
} 