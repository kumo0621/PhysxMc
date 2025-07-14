package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxSphere;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 物理オブジェクト（ボックスとスフィア）の永続化を管理するクラス
 */
public class PhysicsObjectManager {
    
    private final File dataFolder;
    private final File boxFile;
    private final File sphereFile;
    private final Logger logger;
    
    public PhysicsObjectManager(File dataFolder) {
        this.dataFolder = dataFolder;
        this.boxFile = new File(dataFolder, "boxes.yml");
        this.sphereFile = new File(dataFolder, "spheres.yml");
        this.logger = Bukkit.getLogger();
        
        // データフォルダが存在しない場合は作成
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * DisplayedPhysxBoxからBoxPersistenceDataを作成
     */
    private BoxPersistenceData createBoxData(DisplayedPhysxBox box) {
        BoxPersistenceData data = new BoxPersistenceData();
        
        // 表示情報
        Location location = box.getLocation();
        data.setWorldName(location.getWorld().getName());
        data.setX(location.getX());
        data.setY(location.getY());
        data.setZ(location.getZ());
        data.setYaw(location.getYaw());
        data.setPitch(location.getPitch());
        
        // 物理状態
        if (box.getActor() != null && box.getActor().isReleasable()) {
            physx.common.PxVec3 physicsPos = box.getPos().getP();
            physx.common.PxQuat physicsRot = box.getPos().getQ();
            data.setPhysicsX(physicsPos.getX());
            data.setPhysicsY(physicsPos.getY());
            data.setPhysicsZ(physicsPos.getZ());
            data.setPhysicsQx(physicsRot.getX());
            data.setPhysicsQy(physicsRot.getY());
            data.setPhysicsQz(physicsRot.getZ());
            data.setPhysicsQw(physicsRot.getW());
        } else {
            // アクターが無効な場合は表示位置を物理位置として使用
            data.setPhysicsX(location.getX());
            data.setPhysicsY(location.getY());
            data.setPhysicsZ(location.getZ());
            data.setPhysicsQx(0);
            data.setPhysicsQy(0);
            data.setPhysicsQz(0);
            data.setPhysicsQw(1);
        }
        
        // 速度情報の取得（PhysXアクターから実際の速度を取得）
        if (box.getActor() != null && box.getActor().isReleasable()) {
            try {
                physx.common.PxVec3 linearVel = box.getActor().getLinearVelocity();
                physx.common.PxVec3 angularVel = box.getActor().getAngularVelocity();
                
                data.setVelocityX(linearVel.getX());
                data.setVelocityY(linearVel.getY());
                data.setVelocityZ(linearVel.getZ());
                data.setAngularVelocityX(angularVel.getX());
                data.setAngularVelocityY(angularVel.getY());
                data.setAngularVelocityZ(angularVel.getZ());
                
                // PhysXオブジェクトのメモリ解放
                linearVel.destroy();
                angularVel.destroy();
            } catch (Exception e) {
                logger.warning("ボックスの速度取得に失敗: " + e.getMessage() + " - 速度を0で設定");
                data.setVelocityX(0);
                data.setVelocityY(0);
                data.setVelocityZ(0);
                data.setAngularVelocityX(0);
                data.setAngularVelocityY(0);
                data.setAngularVelocityZ(0);
            }
        } else {
            // アクターがない場合は速度を0で設定
            data.setVelocityX(0);
            data.setVelocityY(0);
            data.setVelocityZ(0);
            data.setAngularVelocityX(0);
            data.setAngularVelocityY(0);
            data.setAngularVelocityZ(0);
        }
        
        // ディスプレイ情報（最初のディスプレイデータから取得）
        if (!box.displayMap.isEmpty()) {
            DisplayedPhysxBox.DisplayData firstDisplay = box.displayMap.get(0);
            if (firstDisplay.getDisplays().length > 0) {
                org.bukkit.entity.BlockDisplay blockDisplay = firstDisplay.getDisplays()[0];
                org.bukkit.util.Transformation transformation = blockDisplay.getTransformation();
                data.setScaleX(transformation.getScale().x);
                data.setScaleY(transformation.getScale().y);
                data.setScaleZ(transformation.getScale().z);
                data.setMaterialName(blockDisplay.getBlock().getMaterial().name());
            }
        }
        
        // オフセット情報
        List<BoxPersistenceData.VectorData> offsetData = new ArrayList<>();
        for (DisplayedPhysxBox.DisplayData displayData : box.displayMap) {
            offsetData.add(BoxPersistenceData.VectorData.fromVector(displayData.getOffset()));
        }
        data.setOffsets(offsetData);
        
        // デフォルト密度（詳細な密度情報が取得できない場合）
        data.setDensity(com.kamesuta.physxmc.PhysxSetting.getDefaultDensity());
        
        // プッシャーフラグを設定
        data.setPusher(box.isPusher());
        
        return data;
    }
    
    /**
     * DisplayedPhysxSphereからSpherePersistenceDataを作成
     */
    private SpherePersistenceData createSphereData(DisplayedPhysxSphere sphere) {
        SpherePersistenceData data = new SpherePersistenceData();
        
        // 表示情報
        Location location = sphere.getLocation();
        data.setWorldName(location.getWorld().getName());
        data.setX(location.getX());
        data.setY(location.getY());
        data.setZ(location.getZ());
        data.setYaw(location.getYaw());
        data.setPitch(location.getPitch());
        
        // 物理状態
        physx.common.PxVec3 physicsPos = sphere.getPos().getP();
        physx.common.PxQuat physicsRot = sphere.getPos().getQ();
        data.setPhysicsX(physicsPos.getX());
        data.setPhysicsY(physicsPos.getY());
        data.setPhysicsZ(physicsPos.getZ());
        data.setPhysicsQx(physicsRot.getX());
        data.setPhysicsQy(physicsRot.getY());
        data.setPhysicsQz(physicsRot.getZ());
        data.setPhysicsQw(physicsRot.getW());
        
        // 速度情報の取得（PhysXアクターから実際の速度を取得）
        if (sphere.getActor() != null) {
            try {
                physx.common.PxVec3 linearVel = sphere.getActor().getLinearVelocity();
                physx.common.PxVec3 angularVel = sphere.getActor().getAngularVelocity();
                
                data.setVelocityX(linearVel.getX());
                data.setVelocityY(linearVel.getY());
                data.setVelocityZ(linearVel.getZ());
                data.setAngularVelocityX(angularVel.getX());
                data.setAngularVelocityY(angularVel.getY());
                data.setAngularVelocityZ(angularVel.getZ());
                
                // PhysXオブジェクトのメモリ解放
                linearVel.destroy();
                angularVel.destroy();
            } catch (Exception e) {
                logger.warning("スフィアの速度取得に失敗: " + e.getMessage() + " - 速度を0で設定");
                data.setVelocityX(0);
                data.setVelocityY(0);
                data.setVelocityZ(0);
                data.setAngularVelocityX(0);
                data.setAngularVelocityY(0);
                data.setAngularVelocityZ(0);
            }
        } else {
            logger.warning("スフィアにアクターが存在しません - 速度を0で設定");
            data.setVelocityX(0);
            data.setVelocityY(0);
            data.setVelocityZ(0);
            data.setAngularVelocityX(0);
            data.setAngularVelocityY(0);
            data.setAngularVelocityZ(0);
        }
        
        // 球体固有の情報
        data.setRadius(sphere.getRadius());
        
        // マテリアル情報（最初のディスプレイから取得）
        if (!sphere.displayMap.isEmpty()) {
            DisplayedPhysxSphere.DisplayData firstDisplay = sphere.displayMap.get(0);
            if (firstDisplay.getDisplays().length > 0) {
                org.bukkit.entity.BlockDisplay blockDisplay = firstDisplay.getDisplays()[0];
                data.setMaterialName(blockDisplay.getBlock().getMaterial().name());
            }
        }
        
        // デフォルト密度
        data.setDensity(com.kamesuta.physxmc.PhysxSetting.getDefaultDensity());
        
        return data;
    }
    
    /**
     * 全てのボックスオブジェクトを保存
     */
    public void saveBoxes() {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            List<Map<String, Object>> dataList = new ArrayList<>();
            int savedCount = 0;
            int skippedCount = 0;
            
            // DisplayedBoxHolderから全てのボックスを取得
            List<DisplayedPhysxBox> boxes = PhysxMc.displayedBoxHolder.getAllBoxes();
            
            // 無効なボックスを事前に除去してリストをクリーンアップ
            List<DisplayedPhysxBox> invalidBoxes = new ArrayList<>();
            for (DisplayedPhysxBox box : boxes) {
                if (box != null && (box.getActor() == null || !box.getActor().isReleasable())) {
                    invalidBoxes.add(box);
                }
            }
            
            // 無効なボックスを除去
            for (DisplayedPhysxBox invalidBox : invalidBoxes) {
                PhysxMc.displayedBoxHolder.destroySpecific(invalidBox);
                logger.info("無効なボックスオブジェクトを除去しました");
            }
            
            // リストを更新（無効なボックスを除去後）
            boxes = PhysxMc.displayedBoxHolder.getAllBoxes();
            
            for (DisplayedPhysxBox box : boxes) {
                if (box != null && !box.isDisplayDead() && !box.isPusher()) { // プッシャーの物理オブジェクトは除外
                    try {
                        // アクターが無効な場合は事前にスキップ
                        if (box.getActor() == null || !box.getActor().isReleasable()) {
                            skippedCount++;
                            logger.warning("ボックス保存スキップ: 物理アクターが存在しないか解放済み（無効なオブジェクト）");
                            continue;
                        }
                        
                        BoxPersistenceData data = createBoxData(box);
                        if (data == null) {
                            // アクターが無効なボックスをスキップ
                            skippedCount++;
                            continue;
                        }
                        Map<String, Object> map = new HashMap<>();
                        
                        // 基本情報
                        map.put("worldName", data.getWorldName());
                        map.put("x", data.getX());
                        map.put("y", data.getY());
                        map.put("z", data.getZ());
                        map.put("yaw", data.getYaw());
                        map.put("pitch", data.getPitch());
                        
                        // スケール情報
                        map.put("scaleX", data.getScaleX());
                        map.put("scaleY", data.getScaleY());
                        map.put("scaleZ", data.getScaleZ());
                        
                        // マテリアルと密度
                        map.put("materialName", data.getMaterialName());
                        map.put("density", data.getDensity());
                        
                        // 物理状態
                        map.put("physicsX", data.getPhysicsX());
                        map.put("physicsY", data.getPhysicsY());
                        map.put("physicsZ", data.getPhysicsZ());
                        map.put("physicsQx", data.getPhysicsQx());
                        map.put("physicsQy", data.getPhysicsQy());
                        map.put("physicsQz", data.getPhysicsQz());
                        map.put("physicsQw", data.getPhysicsQw());
                        
                        // 速度状態
                        map.put("velocityX", data.getVelocityX());
                        map.put("velocityY", data.getVelocityY());
                        map.put("velocityZ", data.getVelocityZ());
                        map.put("angularVelocityX", data.getAngularVelocityX());
                        map.put("angularVelocityY", data.getAngularVelocityY());
                        map.put("angularVelocityZ", data.getAngularVelocityZ());
                        
                        // オフセット情報
                        List<Map<String, Double>> offsetMaps = new ArrayList<>();
                        for (BoxPersistenceData.VectorData offset : data.getOffsets()) {
                            Map<String, Double> offsetMap = new HashMap<>();
                            offsetMap.put("x", offset.getX());
                            offsetMap.put("y", offset.getY());
                            offsetMap.put("z", offset.getZ());
                            offsetMaps.add(offsetMap);
                        }
                        map.put("offsets", offsetMaps);
                        
                        // プッシャーフラグ
                        map.put("isPusher", data.isPusher());
                        
                        dataList.add(map);
                        savedCount++;
                        logger.info("ボックス保存成功: " + (box.isCoin() ? "コイン" : "ブロック") + " at " + data.getPhysicsX() + "," + data.getPhysicsY() + "," + data.getPhysicsZ());
                    } catch (Exception e) {
                        skippedCount++;
                        logger.warning("ボックス保存失敗: " + e.getMessage());
                    }
                } else {
                    skippedCount++;
                    if (box == null) {
                        logger.warning("ボックス保存スキップ: オブジェクトがnull");
                    } else if (box.isDisplayDead()) {
                        logger.warning("ボックス保存スキップ: 表示が無効（削除済み）");
                    } else if (box.isPusher()) {
                        logger.info("ボックス保存スキップ: プッシャーオブジェクト（PusherManagerで管理）");
                    } else {
                        logger.warning("ボックス保存スキップ: 不明な理由");
                    }
                }
            }
            
            yaml.set("boxes", dataList);
            yaml.save(boxFile);
            logger.info("ボックスデータを保存しました: " + savedCount + "個保存、" + skippedCount + "個スキップ");
        } catch (IOException e) {
            logger.severe("ボックスデータの保存に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * 全てのスフィアオブジェクトを保存
     */
    public void saveSpheres() {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            List<Map<String, Object>> dataList = new ArrayList<>();
            int savedCount = 0;
            int skippedCount = 0;
            
            // DisplayedSphereHolderから全てのスフィアを取得
            List<DisplayedPhysxSphere> spheres = PhysxMc.displayedSphereHolder.getAllSpheres();
            
            for (DisplayedPhysxSphere sphere : spheres) {
                if (sphere != null && !sphere.isDisplayDead()) {
                    try {
                        SpherePersistenceData data = createSphereData(sphere);
                        Map<String, Object> map = new HashMap<>();
                        
                        // 基本情報
                        map.put("worldName", data.getWorldName());
                        map.put("x", data.getX());
                        map.put("y", data.getY());
                        map.put("z", data.getZ());
                        map.put("yaw", data.getYaw());
                        map.put("pitch", data.getPitch());
                        
                        // スフィア固有情報
                        map.put("radius", data.getRadius());
                        map.put("materialName", data.getMaterialName());
                        map.put("density", data.getDensity());
                        
                        // 物理状態
                        map.put("physicsX", data.getPhysicsX());
                        map.put("physicsY", data.getPhysicsY());
                        map.put("physicsZ", data.getPhysicsZ());
                        map.put("physicsQx", data.getPhysicsQx());
                        map.put("physicsQy", data.getPhysicsQy());
                        map.put("physicsQz", data.getPhysicsQz());
                        map.put("physicsQw", data.getPhysicsQw());
                        
                        // 速度状態
                        map.put("velocityX", data.getVelocityX());
                        map.put("velocityY", data.getVelocityY());
                        map.put("velocityZ", data.getVelocityZ());
                        map.put("angularVelocityX", data.getAngularVelocityX());
                        map.put("angularVelocityY", data.getAngularVelocityY());
                        map.put("angularVelocityZ", data.getAngularVelocityZ());
                        
                        dataList.add(map);
                        savedCount++;
                        logger.info("スフィア保存成功: 半径" + data.getRadius() + " at " + data.getPhysicsX() + "," + data.getPhysicsY() + "," + data.getPhysicsZ());
                    } catch (Exception e) {
                        skippedCount++;
                        logger.warning("スフィア保存失敗: " + e.getMessage());
                    }
                } else {
                    skippedCount++;
                    if (sphere == null) {
                        logger.warning("スフィア保存スキップ: オブジェクトがnull");
                    } else if (sphere.isDisplayDead()) {
                        logger.warning("スフィア保存スキップ: 表示が無効（削除済み）");
                    } else {
                        logger.warning("スフィア保存スキップ: 不明な理由");
                    }
                }
            }
            
            yaml.set("spheres", dataList);
            yaml.save(sphereFile);
            logger.info("スフィアデータを保存しました: " + savedCount + "個保存、" + skippedCount + "個スキップ");
        } catch (IOException e) {
            logger.severe("スフィアデータの保存に失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * 保存されたボックスオブジェクトを読み込み
     */
    public void loadBoxes() {
        if (!boxFile.exists()) {
            logger.info("ボックスデータファイルが存在しません");
            return;
        }
        
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(boxFile);
            List<Map<?, ?>> dataList = yaml.getMapList("boxes");
            if (dataList == null || dataList.isEmpty()) {
                logger.info("ボックスデータが空です");
                return;
            }
            
            int loadedCount = 0;
            for (Map<?, ?> map : dataList) {
                try {
                    BoxPersistenceData data = parseBoxData(map);
                    if (data != null && data.toLocation() != null) {
                        // ボックスオブジェクトを復元
                        restoreBox(data);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    logger.warning("ボックスデータの解析に失敗しました: " + e.getMessage());
                }
            }
            
            logger.info("ボックスデータを読み込みました: " + loadedCount + "個");
        } catch (Exception e) {
            logger.severe("ボックスデータの読み込みに失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * 保存されたスフィアオブジェクトを読み込み
     */
    public void loadSpheres() {
        if (!sphereFile.exists()) {
            logger.info("スフィアデータファイルが存在しません");
            return;
        }
        
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(sphereFile);
            List<Map<?, ?>> dataList = yaml.getMapList("spheres");
            if (dataList == null || dataList.isEmpty()) {
                logger.info("スフィアデータが空です");
                return;
            }
            
            int loadedCount = 0;
            for (Map<?, ?> map : dataList) {
                try {
                    SpherePersistenceData data = parseSphereData(map);
                    if (data != null && data.toLocation() != null) {
                        // スフィアオブジェクトを復元
                        restoreSphere(data);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    logger.warning("スフィアデータの解析に失敗しました: " + e.getMessage());
                }
            }
            
            logger.info("スフィアデータを読み込みました: " + loadedCount + "個");
        } catch (Exception e) {
            logger.severe("スフィアデータの読み込みに失敗しました: " + e.getMessage());
        }
    }
    
    /**
     * YAMLマップからBoxPersistenceDataを解析
     */
    private BoxPersistenceData parseBoxData(Map<?, ?> map) {
        BoxPersistenceData data = new BoxPersistenceData();
        
        try {
            // 基本情報
            data.setWorldName((String) map.get("worldName"));
            data.setX(((Number) map.get("x")).doubleValue());
            data.setY(((Number) map.get("y")).doubleValue());
            data.setZ(((Number) map.get("z")).doubleValue());
            data.setYaw(((Number) map.get("yaw")).floatValue());
            data.setPitch(((Number) map.get("pitch")).floatValue());
            
            // スケール情報
            data.setScaleX(((Number) map.get("scaleX")).doubleValue());
            data.setScaleY(((Number) map.get("scaleY")).doubleValue());
            data.setScaleZ(((Number) map.get("scaleZ")).doubleValue());
            
            // マテリアルと密度
            data.setMaterialName((String) map.get("materialName"));
            data.setDensity(((Number) map.get("density")).floatValue());
            
            // 物理状態
            data.setPhysicsX(((Number) map.get("physicsX")).doubleValue());
            data.setPhysicsY(((Number) map.get("physicsY")).doubleValue());
            data.setPhysicsZ(((Number) map.get("physicsZ")).doubleValue());
            data.setPhysicsQx(((Number) map.get("physicsQx")).floatValue());
            data.setPhysicsQy(((Number) map.get("physicsQy")).floatValue());
            data.setPhysicsQz(((Number) map.get("physicsQz")).floatValue());
            data.setPhysicsQw(((Number) map.get("physicsQw")).floatValue());
            
            // 速度状態
            data.setVelocityX(((Number) map.get("velocityX")).doubleValue());
            data.setVelocityY(((Number) map.get("velocityY")).doubleValue());
            data.setVelocityZ(((Number) map.get("velocityZ")).doubleValue());
            data.setAngularVelocityX(((Number) map.get("angularVelocityX")).doubleValue());
            data.setAngularVelocityY(((Number) map.get("angularVelocityY")).doubleValue());
            data.setAngularVelocityZ(((Number) map.get("angularVelocityZ")).doubleValue());
            
            // オフセット情報
            List<Map<String, Double>> offsetMaps = (List<Map<String, Double>>) map.get("offsets");
            List<BoxPersistenceData.VectorData> offsets = new ArrayList<>();
            if (offsetMaps != null) {
                for (Map<String, Double> offsetMap : offsetMaps) {
                    double x = offsetMap.get("x");
                    double y = offsetMap.get("y");
                    double z = offsetMap.get("z");
                    offsets.add(new BoxPersistenceData.VectorData(x, y, z));
                }
            }
            data.setOffsets(offsets);
            
            // プッシャーフラグを読み込み（デフォルトは false）
            data.setPusher(map.containsKey("isPusher") ? (Boolean) map.get("isPusher") : false);
            
            return data;
        } catch (Exception e) {
            logger.warning("ボックスデータの解析エラー: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * YAMLマップからSpherePersistenceDataを解析
     */
    private SpherePersistenceData parseSphereData(Map<?, ?> map) {
        SpherePersistenceData data = new SpherePersistenceData();
        
        try {
            // 基本情報
            data.setWorldName((String) map.get("worldName"));
            data.setX(((Number) map.get("x")).doubleValue());
            data.setY(((Number) map.get("y")).doubleValue());
            data.setZ(((Number) map.get("z")).doubleValue());
            data.setYaw(((Number) map.get("yaw")).floatValue());
            data.setPitch(((Number) map.get("pitch")).floatValue());
            
            // スフィア固有情報
            data.setRadius(((Number) map.get("radius")).doubleValue());
            data.setMaterialName((String) map.get("materialName"));
            data.setDensity(((Number) map.get("density")).floatValue());
            
            // 物理状態
            data.setPhysicsX(((Number) map.get("physicsX")).doubleValue());
            data.setPhysicsY(((Number) map.get("physicsY")).doubleValue());
            data.setPhysicsZ(((Number) map.get("physicsZ")).doubleValue());
            data.setPhysicsQx(((Number) map.get("physicsQx")).floatValue());
            data.setPhysicsQy(((Number) map.get("physicsQy")).floatValue());
            data.setPhysicsQz(((Number) map.get("physicsQz")).floatValue());
            data.setPhysicsQw(((Number) map.get("physicsQw")).floatValue());
            
            // 速度状態
            data.setVelocityX(((Number) map.get("velocityX")).doubleValue());
            data.setVelocityY(((Number) map.get("velocityY")).doubleValue());
            data.setVelocityZ(((Number) map.get("velocityZ")).doubleValue());
            data.setAngularVelocityX(((Number) map.get("angularVelocityX")).doubleValue());
            data.setAngularVelocityY(((Number) map.get("angularVelocityY")).doubleValue());
            data.setAngularVelocityZ(((Number) map.get("angularVelocityZ")).doubleValue());
            
            return data;
        } catch (Exception e) {
            logger.warning("スフィアデータの解析エラー: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * BoxPersistenceDataからボックスオブジェクトを復元
     */
    private void restoreBox(BoxPersistenceData data) {
        try {
            // プッシャーフラグが設定されている場合は復元をスキップ
            // プッシャーはPusherManagerによって管理されるため
            if (data.isPusher()) {
                logger.info("プッシャーデータをスキップ: PusherManagerで管理されます");
                return;
            }
            
            Location location = data.toPhysicsLocation(); // 物理的な位置を使用
            if (location == null) {
                logger.warning("ワールドが見つからないため、ボックスを復元できませんでした: " + data.getWorldName());
                return;
            }
            
            // 物理的な回転状態を設定
            location.setYaw((float) Math.toDegrees(Math.atan2(-data.getPhysicsQy(), data.getPhysicsQw()) * 2));
            location.setPitch((float) Math.toDegrees(Math.asin(2 * (data.getPhysicsQx() * data.getPhysicsQw() - data.getPhysicsQy() * data.getPhysicsQz()))));
            
            // ボックスオブジェクトを作成（プッシャーフラグ付き）
            DisplayedPhysxBox box = PhysxMc.displayedBoxHolder.createDisplayedBox(
                location,
                data.toScale(),
                data.toItemStack(),
                data.toOffsets(),
                data.getDensity(),
                data.isPusher()
            );
            
            if (box != null) {
                // アクターが正常に作成されているか確認
                if (box.getActor() == null) {
                    logger.severe("復元されたボックスにアクターがありません - 物理演算が無効");
                    return;
                }
                
                // 物理的な位置と回転を正確に設定
                org.joml.Quaternionf quat = new org.joml.Quaternionf(
                    data.getPhysicsQx(),
                    data.getPhysicsQy(),
                    data.getPhysicsQz(),
                    data.getPhysicsQw()
                );
                Vector physicsPos = new Vector(data.getPhysicsX(), data.getPhysicsY(), data.getPhysicsZ());
                
                // 少し待ってから物理演算を復元（安全性のため）
                new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            // キネマティック制御で正確な位置に移動
                            box.makeKinematic(true);
                            box.moveKinematic(physicsPos, quat);
                            
                            // 少し待ってから物理シミュレーションを再開
                            new org.bukkit.scheduler.BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        box.makeKinematic(false); // 物理シミュレーションを再開
                                        
                                        // 速度状態の復元
                                        if (box.getActor() != null) {
                                            var actor = box.getActor();
                                            
                                            // 線形速度を設定
                                            var linearVel = new physx.common.PxVec3(
                                                (float) data.getVelocityX(),
                                                (float) data.getVelocityY(),
                                                (float) data.getVelocityZ()
                                            );
                                            actor.setLinearVelocity(linearVel);
                                            
                                            // 角速度を設定
                                            var angularVel = new physx.common.PxVec3(
                                                (float) data.getAngularVelocityX(),
                                                (float) data.getAngularVelocityY(),
                                                (float) data.getAngularVelocityZ()
                                            );
                                            actor.setAngularVelocity(angularVel);
                                            
                                            // PhysXオブジェクトを解放
                                            linearVel.destroy();
                                            angularVel.destroy();
                                            
                                            logger.info("ボックス復元完了（物理演算有効）: " + data.getMaterialName() + " at " + data.getPhysicsX() + "," + data.getPhysicsY() + "," + data.getPhysicsZ());
                                        } else {
                                            logger.warning("復元後のアクターが無効です: " + data.getMaterialName());
                                        }
                                    } catch (Exception e) {
                                        logger.severe("物理演算再開中にエラー: " + e.getMessage());
                                    }
                                }
                            }.runTaskLater(com.kamesuta.physxmc.PhysxMc.getPlugin(com.kamesuta.physxmc.PhysxMc.class), 2L);
                        } catch (Exception e) {
                            logger.severe("位置復元中にエラー: " + e.getMessage());
                        }
                    }
                }.runTaskLater(com.kamesuta.physxmc.PhysxMc.getPlugin(com.kamesuta.physxmc.PhysxMc.class), 1L);
                
            } else {
                logger.warning("ボックスの作成に失敗しました: " + data.getMaterialName());
            }
        } catch (Exception e) {
            logger.warning("ボックス復元エラー: " + e.getMessage());
        }
    }
    
    /**
     * SpherePersistenceDataからスフィアオブジェクトを復元
     */
    private void restoreSphere(SpherePersistenceData data) {
        try {
            Location location = data.toPhysicsLocation(); // 物理的な位置を使用
            if (location == null) {
                logger.warning("ワールドが見つからないため、スフィアを復元できませんでした: " + data.getWorldName());
                return;
            }
            
            // 物理的な回転状態を設定
            location.setYaw((float) Math.toDegrees(Math.atan2(-data.getPhysicsQy(), data.getPhysicsQw()) * 2));
            location.setPitch((float) Math.toDegrees(Math.asin(2 * (data.getPhysicsQx() * data.getPhysicsQw() - data.getPhysicsQy() * data.getPhysicsQz()))));
            
            // スフィアオブジェクトを作成
            DisplayedPhysxSphere sphere = PhysxMc.displayedSphereHolder.createDisplayedSphere(
                location,
                data.getRadius(),
                data.toMaterial(),
                data.getDensity()
            );
            
            if (sphere != null) {
                // アクターが正常に作成されているか確認
                if (sphere.getActor() == null) {
                    logger.severe("復元されたスフィアにアクターがありません - 物理演算が無効");
                    return;
                }
                
                // 物理的な位置と回転を正確に設定
                org.joml.Quaternionf quat = new org.joml.Quaternionf(
                    data.getPhysicsQx(),
                    data.getPhysicsQy(),
                    data.getPhysicsQz(),
                    data.getPhysicsQw()
                );
                Vector physicsPos = new Vector(data.getPhysicsX(), data.getPhysicsY(), data.getPhysicsZ());
                
                // キネマティック制御で正確な位置に移動
                sphere.makeKinematic(true);
                sphere.moveKinematic(physicsPos, quat);
                sphere.makeKinematic(false); // 物理シミュレーションを再開
                
                // 速度状態の復元
                if (sphere.getActor() != null) {
                    var actor = sphere.getActor();
                    
                    // 線形速度を設定
                    var linearVel = new physx.common.PxVec3(
                        (float) data.getVelocityX(),
                        (float) data.getVelocityY(),
                        (float) data.getVelocityZ()
                    );
                    actor.setLinearVelocity(linearVel);
                    
                    // 角速度を設定
                    var angularVel = new physx.common.PxVec3(
                        (float) data.getAngularVelocityX(),
                        (float) data.getAngularVelocityY(),
                        (float) data.getAngularVelocityZ()
                    );
                    actor.setAngularVelocity(angularVel);
                    
                    // PhysXオブジェクトを解放
                    linearVel.destroy();
                    angularVel.destroy();
                }
                
                logger.info("スフィア復元成功: " + data.getMaterialName() + " 半径" + data.getRadius() + " at " + data.getPhysicsX() + "," + data.getPhysicsY() + "," + data.getPhysicsZ());
            } else {
                logger.warning("スフィアの作成に失敗しました");
            }
        } catch (Exception e) {
            logger.warning("スフィア復元エラー: " + e.getMessage());
        }
    }
    
    /**
     * 全ての物理オブジェクトを保存
     */
    public void saveAll() {
        logger.info("物理オブジェクトの永続化データ保存を開始...");
        saveBoxes();
        saveSpheres();
        logger.info("物理オブジェクトの永続化データ保存完了");
    }
    
    /**
     * 全ての物理オブジェクトを読み込み
     */
    public void loadAll() {
        logger.info("物理オブジェクトの永続化データ読み込みを開始...");
        
        // 読み込み前に既存のオブジェクトをクリア（重複防止）
        clearExistingObjects();
        
        loadBoxes();
        loadSpheres();
        logger.info("物理オブジェクトの永続化データ読み込み完了");
    }
    
    /**
     * 既存の物理オブジェクトをクリアする（重複防止）
     */
    private void clearExistingObjects() {
        logger.info("既存の物理オブジェクトをクリア中...");
        
        if (PhysxMc.displayedBoxHolder != null) {
            // 既存のボックスオブジェクトを削除（プッシャーを除く）
            List<DisplayedPhysxBox> existingBoxes = new ArrayList<>(PhysxMc.displayedBoxHolder.getAllBoxes());
            int boxCount = 0;
            int pusherCount = 0;
            for (DisplayedPhysxBox box : existingBoxes) {
                if (box != null) {
                    if (box.isPusher()) {
                        pusherCount++; // プッシャーの物理オブジェクトはカウントのみ
                    } else {
                        PhysxMc.displayedBoxHolder.destroySpecific(box);
                        boxCount++;
                    }
                }
            }
            if (boxCount > 0 || pusherCount > 0) {
                logger.info("既存オブジェクト処理: ボックス削除 " + boxCount + "個, プッシャー保持 " + pusherCount + "個");
            }
        }
        
        if (PhysxMc.displayedSphereHolder != null) {
            // 既存のスフィアオブジェクトを削除
            List<DisplayedPhysxSphere> existingSpheres = new ArrayList<>(PhysxMc.displayedSphereHolder.getAllSpheres());
            int sphereCount = existingSpheres.size();
            for (DisplayedPhysxSphere sphere : existingSpheres) {
                if (sphere != null) {
                    PhysxMc.displayedSphereHolder.destroySpecific(sphere);
                }
            }
            if (sphereCount > 0) {
                logger.info("既存のスフィアオブジェクトを削除: " + sphereCount + "個");
            }
        }
        
        logger.info("既存の物理オブジェクトのクリア完了");
    }
} 