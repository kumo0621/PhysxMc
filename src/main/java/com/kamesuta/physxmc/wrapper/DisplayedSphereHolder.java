package com.kamesuta.physxmc.wrapper;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.core.SphereData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxSphereGeometry;
import physx.physics.PxActor;

import java.util.*;

/**
 * OverWorld内の全てのDisplayedPhysxSphereを保持するクラス
 */
public class DisplayedSphereHolder {

    private final List<DisplayedPhysxSphere> sphereDisplayList = new ArrayList<>();

    /**
     * DisplayedPhysxSphereを1個生成
     *
     * @param location  場所
     * @param radius    半径
     * @param material  表示用のマテリアル
     * @param density   密度
     * @return
     */
    public DisplayedPhysxSphere createDisplayedSphere(Location location, double radius, Material material, float density) {
        Quaternionf quat = new Quaternionf()
                .rotateY((float) -Math.toRadians(location.getYaw()))
                .rotateX((float) Math.toRadians(location.getPitch()));
        Map<BlockDisplay[], Vector> displayMap = new HashMap<>();
        Map<PxSphereGeometry, PxVec3> sphereGeometries = new HashMap<>();

        // 球体の表示用BlockDisplayを作成
        BlockDisplay[] display = new BlockDisplay[]{
            createDisplay(material, location, radius, quat), 
            createDisplay(material, location, radius, quat)
        };
        displayMap.put(display, new Vector(0, 0, 0));
        
        // 球体のジオメトリを作成
        sphereGeometries.put(new PxSphereGeometry((float) radius), new PxVec3(0, 0, 0));

        SphereData data = new SphereData(
            new PxVec3((float) location.x(), (float) location.y(), (float) location.z()), 
            new PxQuat(quat.x, quat.y, quat.z, quat.w), 
            sphereGeometries, 
            false, 
            density
        );
        
        DisplayedPhysxSphere sphere = PhysxMc.physxWorld.addSphere(data, displayMap, radius);
        sphereDisplayList.add(sphere);
        return sphere;
    }

    /**
     * 指定されたパラメータでBlockDisplayを生成
     *
     * @param material  球体のマテリアル
     * @param location  場所
     * @param radius    半径
     * @param boxQuat   回転
     * @return　作ったBlockDisplay
     */
    private static BlockDisplay createDisplay(Material material, Location location, double radius, Quaternionf boxQuat) {
        BlockDisplay blockDisplay = location.getWorld().spawn(location, BlockDisplay.class);
        blockDisplay.setBlock(material.createBlockData());
        Transformation transformation = blockDisplay.getTransformation();
        transformation.getTranslation().add(-0.5f, -0.5f, -0.5f);
        // 球体は半径の2倍がスケール
        float scale = (float) (radius * 2);
        transformation.getScale().x = scale;
        transformation.getScale().y = scale;
        transformation.getScale().z = scale;
        transformation.getLeftRotation().set(boxQuat);
        blockDisplay.setTransformation(transformation);
        blockDisplay.setGravity(false);
        return blockDisplay;
    }

    /**
     * ワールドに存在する全ての球体を更新する
     */
    public void update() {
        destroyUnusableSphere();
        for (DisplayedPhysxSphere displayedPhysxSphere : sphereDisplayList) {
            displayedPhysxSphere.update();

            if (!displayedPhysxSphere.isSleeping())
                PhysxMc.physxWorld.registerChunksToLoadNextTick(displayedPhysxSphere.getSurroundingChunks());
        }
        PhysxMc.physxWorld.setReadyToUpdateChunks();
    }

    /**
     * ワールドの外に落ちたりdisplayが破壊されていたりする不良品を削除
     */
    private void destroyUnusableSphere() {
        sphereDisplayList.removeIf(sphere -> {
            if (sphere == null)
                return false;

            if (sphere.getLocation().y() < -128 || sphere.isDisplayDead()) {
                for (DisplayedPhysxSphere.DisplayData data : sphere.displayMap){
                    for (BlockDisplay blockDisplay : data.getDisplays()) {
                        blockDisplay.remove();
                    }
                }
                PhysxMc.physxWorld.removeSphere(sphere);
                return true;
            }

            return false;
        });
    }

    /**
     * 全てのBlockDisplayと球体を消去する
     */
    public void destroyAll() {
        sphereDisplayList.forEach(sphere -> {
            for (DisplayedPhysxSphere.DisplayData data : sphere.displayMap){
                for (BlockDisplay blockDisplay : data.getDisplays()) {
                    blockDisplay.remove();
                }
            }
            PhysxMc.physxWorld.removeSphere(sphere);
        });
        sphereDisplayList.clear();
    }

    public void destroySpecific(DisplayedPhysxSphere sphere) {
        if (sphere == null || !sphereDisplayList.remove(sphere))
            return;

        for (DisplayedPhysxSphere.DisplayData data : sphere.displayMap){
            for (BlockDisplay blockDisplay : data.getDisplays()) {
                blockDisplay.remove();
            }
        }
        PhysxMc.physxWorld.removeSphere(sphere);
    }

    /**
     * 指定した球体が存在するかどうか
     */
    public boolean hasSphere(DisplayedPhysxSphere sphere) {
        return sphereDisplayList.contains(sphere);
    }

    /**
     * 爆発で球体を吹き飛ばす
     */
    public void executeExplosion(Location location, float strength) {
        for (DisplayedPhysxSphere sphere : sphereDisplayList) {
            Location sphereLocation = sphere.getLocation();
            double distance = sphereLocation.distance(location);
            if (distance < strength * 5) {
                Vector direction = sphereLocation.toVector().subtract(location.toVector()).normalize();
                double power = strength * 10 / (distance + 1);
                
                PxVec3 force = new PxVec3(
                    (float) (direction.getX() * power), 
                    (float) (direction.getY() * power + 5), 
                    (float) (direction.getZ() * power)
                );
                sphere.addForce(force, physx.physics.PxForceModeEnum.eVELOCITY_CHANGE);
            }
        }
    }

    /**
     * レイキャストで球体を取得
     */
    @Nullable
    public DisplayedPhysxSphere raycast(Location location, float distance) {
        return PhysxMc.physxWorld.raycastSphere(location, distance);
    }

    /**
     * PxActorから球体を取得
     */
    public DisplayedPhysxSphere getSphere(PxActor actor) {
        for (DisplayedPhysxSphere sphere : sphereDisplayList) {
            if (sphere.getActor().equals(actor))
                return sphere;
        }
        return null;
    }

    /**
     * 全てのスフィアオブジェクトのリストを取得（永続化用）
     */
    public List<DisplayedPhysxSphere> getAllSpheres() {
        return new ArrayList<>(sphereDisplayList);
    }
} 