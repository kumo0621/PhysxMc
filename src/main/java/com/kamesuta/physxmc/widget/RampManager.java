package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * 傾斜板(ランプ)を管理するクラス
 */
public class RampManager {

    private final List<MedalRamp> ramps = new ArrayList<>();
    private final File yamlFile;
    private final Logger logger;
    
    public RampManager(File dataFolder) {
        this.yamlFile = new File(dataFolder, "ramps.yml");
        this.logger = Bukkit.getLogger();
        
        // データフォルダが存在しない場合は作成
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * ランプを生成してシーンに追加
     *
     * @param location   生成位置 (プレイヤー位置基準)
     * @param pitchDeg   傾斜角度(度)
     * @param width      幅(ブロック数)
     * @param length     長さ(ブロック数)
     * @param thickness  厚み(ブロック数)
     * @param material   表面ブロック
     * @return 生成されたDisplayedPhysxBox
     */
    public DisplayedPhysxBox createRamp(Location location, double pitchDeg, double width, double length, double thickness, Material material) {
        // 入力値の検証
        if (location == null) {
            logger.severe("ランプ作成失敗: 位置がnullです");
            return null;
        }
        
        if (location.getWorld() == null) {
            logger.severe("ランプ作成失敗: ワールドがnullです");
            return null;
        }
        
        if (material == null) {
            logger.severe("ランプ作成失敗: マテリアルがnullです");
            return null;
        }
        
        if (width <= 0 || length <= 0 || thickness <= 0) {
            logger.severe("ランプ作成失敗: 無効なサイズ (width=" + width + ", length=" + length + ", thickness=" + thickness + ")");
            return null;
        }
        
        try {
            // MedalRampを使用して作成（pusherと同じパターン）
            MedalRamp ramp = new MedalRamp(location, pitchDeg, width, length, thickness, material);
            
            if (!ramp.isValid()) {
                logger.severe("ランプ作成失敗: MedalRampの作成に失敗しました");
                ramp.destroy();
                return null;
            }
            
            ramps.add(ramp);
            
            // 自動保存
            saveRamps();
            logger.info("ランプを作成しました: " + ramps.size() + "個目 - " + ramp.getStatusInfo());
            return ramp.getRampBox();
        } catch (Exception e) {
            logger.severe("ランプ作成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * すべてのランプを更新(無効化されたものを除去)
     */
    public void update() {
        Iterator<MedalRamp> iterator = ramps.iterator();
        while (iterator.hasNext()) {
            MedalRamp ramp = iterator.next();
            if (!ramp.isValid()) {
                ramp.destroy();
                iterator.remove();
            }
        }
    }

    /**
     * 付近のランプを削除
     *
     * @param location    基準位置
     * @param maxDistance 最大距離
     * @return 削除できたか
     */
    public boolean removeNearestRamp(Location location, double maxDistance) {
        MedalRamp nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (MedalRamp ramp : ramps) {
            double distance = ramp.getCenterLocation().distance(location);
            if (distance < nearestDistance && distance <= maxDistance) {
                nearestDistance = distance;
                nearest = ramp;
            }
        }

        if (nearest != null) {
            nearest.destroy();
            ramps.remove(nearest);
            
            // 自動保存
            saveRamps();
            return true;
        }
        return false;
    }

    /**
     * 全ランプを削除
     */
    public void destroyAll() {
        int destroyedCount = ramps.size();
        for (MedalRamp ramp : ramps) {
            ramp.destroy();
        }
        ramps.clear();
        if (destroyedCount > 0) {
            logger.info("RampManager: " + destroyedCount + "個のランプを削除しました");
        }
    }

    /**
     * ランプ数取得
     */
    public int getRampCount() {
        return ramps.size();
    }
    
    /**
     * ランプ設定をファイルに保存
     */
    public void saveRamps() {
        try {
            org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
            List<java.util.Map<String, Object>> dataList = new java.util.ArrayList<>();
            int savedCount = 0;
            int skippedCount = 0;
            
            for (MedalRamp ramp : ramps) {
                // ランプが有効かチェック
                if (ramp.exists()) {
                    try {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("world", ramp.getLocation().getWorld().getName());
                        map.put("x", ramp.getLocation().getX());
                        map.put("y", ramp.getLocation().getY());
                        map.put("z", ramp.getLocation().getZ());
                        map.put("yaw", ramp.getLocation().getYaw());
                        map.put("pitch", ramp.getPitch());
                        map.put("width", ramp.getWidth());
                        map.put("length", ramp.getLength());
                        map.put("thickness", ramp.getThickness());
                        map.put("material", ramp.getMaterial().name());
                        dataList.add(map);
                        savedCount++;
                        logger.info("ランプ保存成功: " + ramp.getMaterial().name() + " @ " + ramp.getLocation().getWorld().getName());
                    } catch (Exception e) {
                        skippedCount++;
                        logger.warning("ランプ保存失敗: " + e.getMessage());
                    }
                } else {
                    skippedCount++;
                    logger.warning("ランプ保存スキップ: 無効なランプ");
                }
            }
            
            yaml.set("ramps", dataList);
            yaml.save(yamlFile);
            logger.info("ランプデータを保存しました: " + savedCount + "個保存、" + skippedCount + "個スキップ");
        } catch (IOException e) {
            logger.severe("ランプデータの保存に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * ランプ設定をファイルから読み込み
     */
    public void loadRamps() {
        // 注意：クリーンアップはPhysxMc.javaで実行済み
        if (!ramps.isEmpty()) {
            logger.warning("ランプが既に存在します(" + ramps.size() + "個)。重複読み込みの可能性があります。");
        }
        
        if (!yamlFile.exists()) {
            logger.info("ランプデータファイルが存在しません");
            return;
        }
        
        try {
            org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(yamlFile);
            java.util.List<java.util.Map<?,?>> dataList = yaml.getMapList("ramps");
            if (dataList == null || dataList.isEmpty()) {
                logger.info("ランプデータが空です");
                return;
            }

            int loadedCount = 0;
            for (java.util.Map<?,?> map : dataList) {
                try {
                    String worldName = (String) map.get("world");
                    double x = ((Number) map.get("x")).doubleValue();
                    double y = ((Number) map.get("y")).doubleValue();
                    double z = ((Number) map.get("z")).doubleValue();
                    float yaw = ((Number) map.get("yaw")).floatValue();
                    double pitch = ((Number) map.get("pitch")).doubleValue();
                    double width = ((Number) map.get("width")).doubleValue();
                    double length = ((Number) map.get("length")).doubleValue();
                    double thickness = ((Number) map.get("thickness")).doubleValue();
                    String materialName = (String) map.get("material");

                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location location = new Location(world, x, y, z, yaw, 0);
                        Material material = Material.valueOf(materialName);
                        
                        // MedalRampを使用して復元
                        MedalRamp ramp = new MedalRamp(location, pitch, width, length, thickness, material);
                        
                        if (ramp.isValid()) {
                            ramps.add(ramp);
                            loadedCount++;
                            logger.info("ランプ復元成功: " + materialName + " @ " + worldName);
                        } else {
                            logger.warning("ランプの物理演算作成に失敗しました: " + materialName);
                            ramp.destroy();
                        }
                    } else {
                        logger.warning("ワールドが見つからないため、ランプを復元できませんでした: " + worldName);
                    }
                } catch (Exception e) {
                    logger.warning("ランプデータの解析に失敗しました: " + e.getMessage());
                }
            }

            logger.info("ランプデータを読み込みました: " + loadedCount + "個");
        } catch (Exception e) {
            logger.severe("ランプデータの読み込みに失敗しました: " + e.getMessage());
        }
    }
} 