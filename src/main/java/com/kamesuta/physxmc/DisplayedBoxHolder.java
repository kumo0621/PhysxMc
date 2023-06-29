package com.kamesuta.physxmc;

import lombok.Data;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxForceModeEnum;

import java.util.ArrayList;
import java.util.List;

import static com.kamesuta.physxmc.ConversionUtility.convertToQuaternion;

/**
 * ItemDisplayと物理演算世界の箱を保持するクラス
 */
public class DisplayedBoxHolder {

    private static final List<DisplayedPhysxBox> itemDisplayList = new ArrayList<>();

//    private static final Map<Player, PhysxBox> playerCollisionList = new HashMap<>();

    /**
     * プレイヤーがブロックを持っていたとき、座標にItemDisplayを1個生成して、箱と紐づける
     *
     * @param player プレイヤー
     */
    public void debugCreate(Player player) {
        if (!player.getInventory().getItemInMainHand().getType().isBlock()) {
            return;
        }

        int scale = player.getInventory().getHeldItemSlot() + 1;
        DisplayedPhysxBox box = createDisplayedBox(player.getEyeLocation(), scale, player.getInventory().getItemInMainHand());
        box.throwBox(player.getEyeLocation(), scale);

        itemDisplayList.add(box);
    }

    private DisplayedPhysxBox createDisplayedBox(Location location, float scale, ItemStack itemStack) {
        // なめらかな補完のために2つitemdisplayを作る
        ItemDisplay[] display = new ItemDisplay[]{createItemDisplay(itemStack, location, scale), createItemDisplay(itemStack, location, scale)};
        Vector3f rot = location.getDirection().clone().toVector3f();
        Quaternionf quat = convertToQuaternion(rot.x, rot.y, rot.z);
        PxBoxGeometry boxGeometry = new PxBoxGeometry(0.5f * scale, 0.5f * scale, 0.5f * scale);
        return PhysxMc.physxWorld.addBox(new PxVec3((float) location.x(), (float) location.y(), (float) location.z()), new PxQuat(quat.x, quat.y, quat.z, quat.w), boxGeometry, display);
    }

    private ItemDisplay createItemDisplay(ItemStack itemStack, Location location, float scale) {
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

    /**
     * ワールドに存在する全ての箱を更新する
     */
    public void update() {
        itemDisplayList.forEach(displayedPhysxBox -> {
            displayedPhysxBox.update();

            if (!displayedPhysxBox.isSleeping())
                PhysxMc.physxWorld.registerChunksToLoadNextTick(displayedPhysxBox.getSurroundingChunks());
        });
        PhysxMc.physxWorld.setReadyToUpdateChunks();

        //TODO:プレイヤーの接触判定を適切に実装
//        Bukkit.getOnlinePlayers().forEach(player -> {
//            Location loc = player.getLocation();
//            if (playerCollisionList.get(player) == null) {
//                PhysxBox box = PhysxMc.physxWorld.addBox(new PxVec3((float) loc.x(), (float) loc.y() + 1, (float) loc.z()), new PxQuat(PxIDENTITYEnum.PxIdentity));
//                playerCollisionList.put(player, box);
//            }
//            PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
//            PxVec3 vec3 = new PxVec3((float) loc.x(), (float) loc.y() + 1, (float) loc.z());
//            tmpPose.setP(vec3);
//            vec3.destroy();
//            playerCollisionList.get(player).setPos(tmpPose);
//        });
    }

    /**
     * 全てのitemdisplayと箱を消去する
     */
    public void destroyAll() {
        itemDisplayList.forEach(block -> {
            for (ItemDisplay itemDisplay : block.display) {
                itemDisplay.remove();
            }
            PhysxMc.physxWorld.removeBox(block);
        });
        itemDisplayList.clear();
    }

    /**
     * 全ての箱に対して爆発を適用する
     */
     public void executeExplosion(Location location, float strength) {
        Vector3d tmp = new Vector3d();
        double explosionStrengthSquared = strength * 2.0 * strength * 2.0;

        for (DisplayedPhysxBox box : itemDisplayList) {
            PxVec3 pos = box.getPos().getP();
            tmp.set(pos.getX(), pos.getY(), pos.getZ());
            double distanceSquared = location.toVector().toVector3d().distanceSquared(tmp);
            if (distanceSquared <= explosionStrengthSquared) {
                double distance = Math.sqrt(distanceSquared);
                Vector3d direction = tmp.sub(location.toVector().toVector3d()).normalize();
                direction.y += 2.0;
                direction.normalize();
                double realStrength = (1.0 - (Math.min(Math.max( distance / (strength * 2.0), 0f),1f))) * 15.0;
                
                PxVec3 pxVec = new PxVec3(
                        (float) (direction.x * realStrength),
                        (float) (direction.y * realStrength),
                        (float) (direction.z * realStrength)
                );
                box.addForce(pxVec, PxForceModeEnum.eVELOCITY_CHANGE);
            }
        }
    }
}
