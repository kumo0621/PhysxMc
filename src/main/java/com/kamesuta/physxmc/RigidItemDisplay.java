package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

/**
 * ItemDisplayを生成し、物理演算世界の箱と結びつけるクラス
 */
public class RigidItemDisplay {

    private static final Map<ItemDisplay, PhysxBox> itemDisplayList = new HashMap<>();

    private static final Map<Player, PhysxBox> playerCollisionList = new HashMap<>();

    /**
     * プレイヤーの座標にItemDisplayを1個生成して、箱と紐づける
     * @param player プレイヤー
     */
    public void create(Player player) {
        if (!player.getInventory().getItemInMainHand().getType().isBlock()) {
            return;
        }

        int scale = player.getInventory().getHeldItemSlot() + 1;
        ItemDisplay itemDisplay = createItemDisplay(player.getInventory().getItemInMainHand(), player.getEyeLocation(), scale);
        PhysxBox box = createBox(player.getEyeLocation(), scale);
        
        itemDisplayList.put(itemDisplay, box);
    }

    private ItemDisplay createItemDisplay(ItemStack itemStack, Location location, float scale){
        ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class);
        itemDisplay.setItemStack(itemStack);
        Transformation transformation = itemDisplay.getTransformation();
        transformation.getScale().x = scale;
        transformation.getScale().y = scale;
        transformation.getScale().z = scale;
        itemDisplay.setTransformation(transformation);
        itemDisplay.setGravity(false);
        return itemDisplay;
    }
    
    private PhysxBox createBox(Location location, float scale){
        Vector3f playerRot = location.getDirection().clone().toVector3f();
        Quaternionf playerQuat = convertToQuaternion(playerRot.x, playerRot.y, playerRot.z);
        PxBoxGeometry boxGeometry = new PxBoxGeometry(0.5f * scale, 0.5f * scale, 0.5f * scale);
        PhysxBox box = PhysxMc.physxWorld.addBox(new PxVec3((float) location.x(), (float) location.y(), (float) location.z()), new PxQuat(playerQuat.x, playerQuat.y, playerQuat.z, playerQuat.w), boxGeometry);

        playerRot = location.getDirection().clone().multiply(200 * Math.pow(scale, 3)).toVector3f();
        PxVec3 force = new PxVec3(playerRot.x, playerRot.y, playerRot.z);
        box.addForce(force);
        return box;
    }
    
    /**
     * ワールドに存在する全てのItemDisplayの座標と回転を箱に基づいて更新する
     */
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
                PhysxBox box = PhysxMc.physxWorld.addBox(new PxVec3((float) loc.x(), (float) loc.y() + 1, (float) loc.z()), new PxQuat(PxIDENTITYEnum.PxIdentity));
                playerCollisionList.put(player, box);
            }
            PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
            PxVec3 vec3 = new PxVec3((float) loc.x(), (float) loc.y() + 1, (float) loc.z());
            tmpPose.setP(vec3);
            vec3.destroy();
            playerCollisionList.get(player).setPos(tmpPose);
        });
    }

    /**
     * 全てのitemdisplayと箱を消去する
     */
    public void destroyAll() {
        itemDisplayList.forEach((itemDisplay, box) -> {
            itemDisplay.remove();
            PhysxMc.physxWorld.removeBox(box);
        });
        itemDisplayList.clear();
    }
}
