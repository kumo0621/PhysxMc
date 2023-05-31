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

import static com.kamesuta.physxmc.ConversionUtility.convertToQuaternion;

public class RigidItemDisplay {

    private static final Map<ItemDisplay, PhysxBox> itemDisplayList = new HashMap<>();

    private static final Map<Player, PhysxBox> playerCollisionList = new HashMap<>();

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
            PhysxMc.physx.removeBox(box);
        });
        itemDisplayList.clear();
    }
}
