package com.kamesuta.physxmc.widget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * プッシャーを管理するクラス
 */
public class PusherManager {
    
    private final List<MedalPusher> pushers = new ArrayList<>();
    private final File dataFile;
    private final File yamlFile;
    private final Gson gson;
    private final Logger logger;
    
    public PusherManager(File dataFolder) {
        this.dataFile = new File(dataFolder, "pushers.json"); // 旧互換（未使用）
        this.yamlFile = new File(dataFolder, "pushers.yml");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.logger = Bukkit.getLogger();
        
        // データフォルダが存在しない場合は作成
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
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
        // 自動保存
        savePushers();
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
            // 自動保存
            savePushers();
            return true;
        }
        
        return false;
    }
    
    /**
     * プッシャー設定をファイルに保存
     */
    public void savePushers() {
        try {
            org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
            List<java.util.Map<String, Object>> dataList = new java.util.ArrayList<>();
            for (MedalPusher pusher : pushers) {
                if (pusher.isValid()) {
                    PusherData d = PusherData.fromPusher(pusher);
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("world", d.getWorldName());
                    map.put("x", d.getX());
                    map.put("y", d.getY());
                    map.put("z", d.getZ());
                    map.put("yaw", d.getYaw());
                    map.put("pitch", d.getPitch());
                    map.put("height", d.getHeight());
                    map.put("width", d.getWidth());
                    map.put("length", d.getLength());
                    map.put("range", d.getMoveRange());
                    map.put("material", d.getMaterialName());
                    map.put("speed", d.getSpeed());
                    dataList.add(map);
                }
            }
            yaml.set("pushers", dataList);
            yaml.save(yamlFile);
            logger.info("プッシャーデータを保存しました: " + dataList.size() + "個");
        } catch (IOException e) {
            logger.severe("プッシャーデータの保存に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * プッシャー設定をファイルから読み込み
     */
    public void loadPushers() {
        if (!yamlFile.exists()) {
            logger.info("プッシャーデータファイルが存在しません");
            return;
        }
        
        try {
            org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(yamlFile);
            java.util.List<java.util.Map<?,?>> dataList = yaml.getMapList("pushers");
            if (dataList == null || dataList.isEmpty()) {
                logger.info("プッシャーデータが空です");
                return;
            }

            int loadedCount = 0;
            for (java.util.Map<?,?> map : dataList) {
                try {
                    PusherData data = new PusherData();
                    data.setWorldName((String) map.get("world"));
                    data.setX(((Number) map.get("x")).doubleValue());
                    data.setY(((Number) map.get("y")).doubleValue());
                    data.setZ(((Number) map.get("z")).doubleValue());
                    data.setYaw(((Number) map.get("yaw")).floatValue());
                    data.setPitch(((Number) map.get("pitch")).floatValue());
                    data.setHeight(((Number) map.get("height")).doubleValue());
                    data.setWidth(((Number) map.get("width")).intValue());
                    data.setLength(((Number) map.get("length")).doubleValue());
                    data.setMoveRange(((Number) map.get("range")).doubleValue());
                    data.setMaterialName((String) map.get("material"));
                    data.setSpeed(((Number) map.get("speed")).doubleValue());

                    Location location = data.toLocation();
                    if (location != null) {
                        MedalPusher pusher = new MedalPusher(
                            location,
                            data.getHeight(),
                            data.getWidth(),
                            data.getLength(),
                            data.getMoveRange(),
                            data.toMaterial(),
                            data.getSpeed()
                        );
                        pushers.add(pusher);
                        loadedCount++;
                    } else {
                        logger.warning("ワールドが見つからないため、プッシャーを復元できませんでした: " + data.getWorldName());
                    }
                } catch (Exception e) {
                    logger.warning("プッシャーデータの解析に失敗しました: " + e.getMessage());
                }
            }

            logger.info("プッシャーデータを読み込みました: " + loadedCount + "個");
        } catch (Exception e) {
            logger.severe("プッシャーデータの読み込みに失敗しました: " + e.getMessage());
        }
    }
} 