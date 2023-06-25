package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxPhysics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kamesuta.physxmc.ConversionUtility.convertToQuaternion;

/**
 * ItemDisplayを生成し、物理演算世界の箱と結びつけるクラス
 */
public class RigidItemDisplay {

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

        // TP用に2つのItemDisplayを生成
        ItemDisplay[] displays = new ItemDisplay[] {
                createItemDisplay(player.getInventory().getItemInMainHand(), player.getEyeLocation(), scale),
                createItemDisplay(player.getInventory().getItemInMainHand(), player.getEyeLocation(), scale),
        };

        DisplayedPhysxBox box = createDisplayedBox(player.getEyeLocation(), scale, displays);
        Vector3f rot = player.getEyeLocation().getDirection().clone().multiply(PhysxSetting.getThrowPower() * Math.pow(scale, 3)).toVector3f();
        PxVec3 force = new PxVec3(rot.x, rot.y, rot.z);
        box.addForce(force);

        itemDisplayList.add(box);
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

    private DisplayedPhysxBox createDisplayedBox(Location location, float scale, ItemDisplay[] displays) {
        Vector3f rot = location.getDirection().clone().toVector3f();
        Quaternionf quat = convertToQuaternion(rot.x, rot.y, rot.z);
        PxBoxGeometry boxGeometry = new PxBoxGeometry(0.5f * scale, 0.5f * scale, 0.5f * scale);
        return PhysxMc.physxWorld.addBox(new PxVec3((float) location.x(), (float) location.y(), (float) location.z()), new PxQuat(quat.x, quat.y, quat.z, quat.w), boxGeometry, displays);
    }

    /**
     * ワールドに存在する全てのItemDisplayの座標と回転を箱に基づいて更新する
     */
    public void update() {
        itemDisplayList.forEach(block -> {

            PxQuat q = block.getPos().getQ();
            PxVec3 p = block.getPos().getP();
            Location pos = new Location(block.display[0].getWorld(), p.getX(), p.getY(), p.getZ());

            // スワップのフェーズ管理 (2ティックかけてスワップが完了する)
            if (block.swapPhase == 2) {
                block.swap(pos);
                block.swapPhase = 0;
            }
            if (block.swapPhase == 1) {
                block.preSwap(pos);
                block.swapPhase = 2;
            }
            // 位置が16マス以上離れていたら次のティックからスワップを開始する
            if (block.swapPhase == 0 && block.display[0].getLocation().toVector().distance(new Vector(p.getX(), p.getY(), p.getZ())) > 16) {
                block.swapPhase = 1;
            }

            for (ItemDisplay itemDisplay : block.display) {
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
        });

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
}
