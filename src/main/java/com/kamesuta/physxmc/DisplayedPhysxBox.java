package com.kamesuta.physxmc;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxPhysics;

import static com.kamesuta.physxmc.Physx.defaultMaterial;

/**
 * Minecraft世界で表示可能なPhysxBox
 */
public class DisplayedPhysxBox extends PhysxBox {
    
    /**
     * 表示用のItemDisplay
     */
    public ItemDisplay[] display;
    /**
     * スワップのフェーズ管理
     */
    public int swapPhase = 0;

    public DisplayedPhysxBox(PxPhysics physics, PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry, ItemDisplay display) {
        super(physics, defaultMaterial, pos, quat, boxGeometry);
        // TP用に2つのItemDisplayを生成
        this.display = new ItemDisplay[]{display, display};
    }

    /**
     * 物理の箱とItemDisplayを同期する
     */
    public void update(){
        PxQuat q = getPos().getQ();
        PxVec3 p = getPos().getP();
        Location pos = new Location(display[0].getWorld(), p.getX(), p.getY(), p.getZ());

        // スワップのフェーズ管理 (2ティックかけてスワップが完了する)
        if (swapPhase == 2) {
            swap(pos);
            swapPhase = 0;
        }
        if (swapPhase == 1) {
            preSwap(pos);
            swapPhase = 2;
        }
        // 位置が16マス以上離れていたら次のティックからスワップを開始する
        if (swapPhase == 0 && display[0].getLocation().toVector().distance(new Vector(p.getX(), p.getY(), p.getZ())) > 16) {
            swapPhase = 1;
        }

        for (ItemDisplay itemDisplay : display) {
            Location prev = itemDisplay.getLocation();

            Quaternionf boxQuat = new Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
            Transformation transformation = itemDisplay.getTransformation();
            transformation.getLeftRotation().set(boxQuat);
            transformation.getTranslation().set(p.getX() - prev.getX(), p.getY() - prev.getY(), p.getZ() - prev.getZ());
            itemDisplay.setTransformation(transformation);
            // なめらかに補完する
            itemDisplay.setInterpolationDelay(0);
            itemDisplay.setInterpolationDuration(1);
            // itemDisplay.teleport(new Location(itemDisplay.getWorld(), p.getX(), p.getY(), p.getZ()));
        }
    }

    /**
     * スワップの1ティック前に呼ぶ
     * @param pos 新しい位置
     */
    private void preSwap(Location pos) {
        display[0].setVisibleByDefault(true);
    }

    /**
     * TPの移動が見えないようにスワップする
     * @param pos 新しい位置
     */
    private void swap(Location pos) {
        display[1].setVisibleByDefault(false);
        display[1].teleport(pos);

        ItemDisplay temp = display[1];
        display[1] = display[0];
        display[0] = temp;
    }

    public void throwBox(Location location, int scale){
        double power = PhysxSetting.getThrowPower() * Math.pow(scale, 3);
        Vector3f rot = location.getDirection().clone().multiply(power).toVector3f();
        PxVec3 force = new PxVec3(rot.x, rot.y, rot.z);
        addForce(force);
    }
}
