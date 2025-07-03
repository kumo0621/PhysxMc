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
    private final Gson gson;
    private final Logger logger;
    
    public PusherManager(File dataFolder) {
        this.dataFile = new File(dataFolder, "pushers.json");
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
        // 自動保存
        savePushers();
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
            List<PusherData> pusherDataList = new ArrayList<>();
            for (MedalPusher pusher : pushers) {
                if (pusher.isValid()) {
                    pusherDataList.add(PusherData.fromPusher(pusher));
                }
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(pusherDataList, writer);
            }
            
            logger.info("プッシャーデータを保存しました: " + pusherDataList.size() + "個");
        } catch (IOException e) {
            logger.severe("プッシャーデータの保存に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * プッシャー設定をファイルから読み込み
     */
    public void loadPushers() {
        if (!dataFile.exists()) {
            logger.info("プッシャーデータファイルが存在しません");
            return;
        }
        
        try {
            try (FileReader reader = new FileReader(dataFile)) {
                Type listType = new TypeToken<List<PusherData>>(){}.getType();
                List<PusherData> pusherDataList = gson.fromJson(reader, listType);
                
                if (pusherDataList == null) {
                    logger.info("プッシャーデータが空です");
                    return;
                }
                
                int loadedCount = 0;
                for (PusherData data : pusherDataList) {
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
                }
                
                logger.info("プッシャーデータを読み込みました: " + loadedCount + "個");
            }
        } catch (IOException e) {
            logger.severe("プッシャーデータの読み込みに失敗しました: " + e.getMessage());
        }
    }
} 