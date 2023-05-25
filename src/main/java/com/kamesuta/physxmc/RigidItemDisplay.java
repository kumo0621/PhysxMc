package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;

import java.util.HashMap;
import java.util.Map;

public class RigidItemDisplay {

    private static final Map<ItemDisplay, PhysxBox> itemDisplayList = new HashMap<>();

    private static final Map<Player, PhysxBox> playerCollisionList = new HashMap<>();

    public static Vector3f convertToEulerAngles(Quaternionf quaternion) {
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesXYZ(euler);

        // ラジアンから度に変換
        euler.mul((float) (180.0 / Math.PI));

        return euler;
    }

    public static Quaternionf convertToQuaternion(double eulerX, double eulerY, double eulerZ) {
        Vector3f angles = new Vector3f((float) Math.toRadians(eulerX), (float) Math.toRadians(eulerY), (float) Math.toRadians(eulerZ));
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotationXYZ(angles.x, angles.y, angles.z);

        return quaternion;
    }

    public static float[] convertToYawPitch(double eulerX, double eulerY, double eulerZ) {
        double yaw = eulerY;
        double pitch = -eulerX;

        // 正規化
        yaw = normalizeAngle(yaw);
        pitch = normalizeAngle(pitch);

        return new float[]{(float) yaw, (float) pitch};
    }

    public static float[] convertToEulerAngles(double yaw, double pitch) {
        double eulerX = -pitch;
        double eulerY = yaw;
        double eulerZ = 0.0;

        // 正規化
        eulerX = normalizeAngle(eulerX);
        eulerY = normalizeAngle(eulerY);

        return new float[]{(float) eulerX, (float) eulerY, (float) eulerZ};
    }

    public static double normalizeAngle(double angle) {
        angle %= 360.0;

        if (angle < -180.0) {
            angle += 360.0;
        } else if (angle > 180.0) {
            angle -= 360.0;
        }

        return angle;
    }

    public void create(Player player) {
        if (!player.getInventory().getItemInMainHand().getType().isBlock()) {
            return;
        }

        int multiplier = player.getInventory().getHeldItemSlot() + 1;

        ItemDisplay itemDisplay = player.getWorld().spawn(player.getLocation(), ItemDisplay.class);
        itemDisplay.setItemStack(player.getInventory().getItemInMainHand());
        Transformation transformation = itemDisplay.getTransformation();
        transformation.getScale().x = multiplier;
        transformation.getScale().y = multiplier;
        transformation.getScale().z = multiplier;
        itemDisplay.setTransformation(transformation);
        itemDisplay.setGravity(false);

        Location loc = player.getLocation();
        Vector3f playerRot = player.getLocation().getDirection().toVector3f();
        Quaternionf playerQuat = convertToQuaternion(playerRot.x, playerRot.y, playerRot.z);
        PxBoxGeometry boxGeometry = new PxBoxGeometry(0.5f * multiplier, 0.5f * multiplier, 0.5f * multiplier);
        PhysxBox box = PhysxMc.physx.addBox(new PxVec3((float) loc.x(), (float) loc.y(), (float) loc.z()), new PxQuat(playerQuat.x, playerQuat.y, playerQuat.z, playerQuat.w), boxGeometry);

        playerRot = player.getLocation().getDirection().multiply(200).toVector3f();
        PxVec3 force = new PxVec3(playerRot.x, playerRot.y, playerRot.z);
        box.addForce(force);
        itemDisplayList.put(itemDisplay, box);
    }

    public void update() {
        itemDisplayList.forEach((itemDisplay, box) -> {
            Quaternionf boxQuat = new Quaternionf(box.getPos().getQ().getX(), box.getPos().getQ().getY(), box.getPos().getQ().getZ(), box.getPos().getQ().getW());
            Transformation transformation = itemDisplay.getTransformation();
            transformation.getLeftRotation().set(boxQuat);
            itemDisplay.setTransformation(transformation);
            itemDisplay.teleport(new Location(itemDisplay.getWorld(), box.getPos().getP().getX(), box.getPos().getP().getY(), box.getPos().getP().getZ()));
        });

        Bukkit.getOnlinePlayers().forEach(player -> {
            Location loc = player.getLocation();
            if (playerCollisionList.get(player) == null) {
                PhysxBox box = PhysxMc.physx.addBox(new PxVec3((float) loc.x(), (float) loc.y() + 1, (float) loc.z()), new PxQuat(PxIDENTITYEnum.PxIdentity));
                playerCollisionList.put(player, box);
            }
            PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
            PxVec3 vec3 = new PxVec3((float) loc.x(), (float) loc.y() + 1, (float) loc.z());
            tmpPose.setP(vec3);
            vec3.destroy();
            playerCollisionList.get(player).setPos(tmpPose);
        });
    }

    public void destroyAll() {
        itemDisplayList.forEach((itemDisplay, box) -> {
            itemDisplay.remove();
            box.release();
        });
    }


}
