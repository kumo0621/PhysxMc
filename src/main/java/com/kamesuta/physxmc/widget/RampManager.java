package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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

    private final List<DisplayedPhysxBox> ramps = new ArrayList<>();
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
            // ランプ中心位置をコピーしてピッチのみ設定
            Location center = location.clone();
            center.setPitch((float) pitchDeg);

            Vector scale = new Vector(width, thickness, length);
            ItemStack item = new ItemStack(material);

            DisplayedPhysxBox ramp = PhysxMc.displayedBoxHolder.createDisplayedBox(center, scale, item, List.of(new Vector()));
            
            if (ramp == null) {
                logger.severe("ランプ作成失敗: DisplayedPhysxBoxの作成に失敗しました");
                return null;
            }
            
            // アクターが有効であることを再確認
            if (ramp.getActor() == null) {
                logger.severe("ランプ作成失敗: PhysXアクターが無効です");
                return null;
            }
            
            // 少し待ってからキネマティック設定（PhysXが安定してから）
            org.bukkit.scheduler.BukkitRunnable kinematicTask = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (ramp.getActor() != null && ramp.getActor().isReleasable()) {
                            // 動かないランプとして設定
                            ramp.makeKinematic(true);
                            logger.info("ランプをキネマティック状態に設定しました");
                        } else {
                            logger.warning("ランプのアクターが無効のため、キネマティック設定をスキップしました");
                        }
                    } catch (Exception e) {
                        logger.severe("ランプのキネマティック設定中にエラー: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };
            
            // 1tick後に実行（PhysXの初期化を待つ）
            kinematicTask.runTaskLater(com.kamesuta.physxmc.PhysxMc.getPlugin(com.kamesuta.physxmc.PhysxMc.class), 1L);
            
            ramps.add(ramp);
            
            // 自動保存
            saveRamps();
            logger.info("ランプを作成しました: " + ramps.size() + "個目");
            return ramp;
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
        Iterator<DisplayedPhysxBox> iterator = ramps.iterator();
        while (iterator.hasNext()) {
            DisplayedPhysxBox ramp = iterator.next();
            if (ramp.isDisplayDead()) {
                PhysxMc.displayedBoxHolder.destroySpecific(ramp);
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
        DisplayedPhysxBox nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (DisplayedPhysxBox ramp : ramps) {
            double distance = ramp.getLocation().distance(location);
            if (distance < nearestDistance && distance <= maxDistance) {
                nearestDistance = distance;
                nearest = ramp;
            }
        }

        if (nearest != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(nearest);
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
        for (DisplayedPhysxBox ramp : ramps) {
            PhysxMc.displayedBoxHolder.destroySpecific(ramp);
        }
        ramps.clear();
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
            
            for (DisplayedPhysxBox ramp : ramps) {
                // ランプが有効かチェック
                if (!ramp.isDisplayDead()) {
                    try {
                        RampData data = RampData.fromRamp(ramp);
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("world", data.getWorldName());
                        map.put("x", data.getX());
                        map.put("y", data.getY());
                        map.put("z", data.getZ());
                        map.put("yaw", data.getYaw());
                        map.put("pitch", data.getPitch());
                        map.put("width", data.getWidth());
                        map.put("length", data.getLength());
                        map.put("thickness", data.getThickness());
                        map.put("material", data.getMaterialName());
                        dataList.add(map);
                        savedCount++;
                        logger.info("ランプ保存成功: " + data.getMaterialName() + " @ " + data.getWorldName());
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
        // 読み込み前に既存のランプをクリア（重複防止）
        if (!ramps.isEmpty()) {
            logger.info("既存のランプをクリア中: " + ramps.size() + "個");
            destroyAll();
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
                    RampData data = new RampData();
                    data.setWorldName((String) map.get("world"));
                    data.setX(((Number) map.get("x")).doubleValue());
                    data.setY(((Number) map.get("y")).doubleValue());
                    data.setZ(((Number) map.get("z")).doubleValue());
                    data.setYaw(((Number) map.get("yaw")).floatValue());
                    data.setPitch(((Number) map.get("pitch")).floatValue());
                    data.setWidth(((Number) map.get("width")).doubleValue());
                    data.setLength(((Number) map.get("length")).doubleValue());
                    data.setThickness(((Number) map.get("thickness")).doubleValue());
                    data.setMaterialName((String) map.get("material"));

                    Location location = data.toLocation();
                    if (location != null) {
                        // ランプを復元（保存時の自動保存を避けるため一時的にリストから削除）
                        Vector scale = new Vector(data.getWidth(), data.getThickness(), data.getLength());
                        ItemStack item = new ItemStack(data.toMaterial());
                        
                        DisplayedPhysxBox ramp = PhysxMc.displayedBoxHolder.createDisplayedBox(location, scale, item, List.of(new Vector()));
                        if (ramp != null) {
                            ramp.makeKinematic(true);
                            ramps.add(ramp);
                            loadedCount++;
                            logger.info("ランプ復元成功: " + data.getMaterialName() + " @ " + data.getWorldName());
                        } else {
                            logger.warning("ランプの物理演算作成に失敗しました: " + data.getMaterialName());
                        }
                    } else {
                        logger.warning("ワールドが見つからないため、ランプを復元できませんでした: " + data.getWorldName());
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