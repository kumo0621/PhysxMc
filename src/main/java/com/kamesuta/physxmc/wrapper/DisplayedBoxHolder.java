package com.kamesuta.physxmc.wrapper;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.utils.BoundingBoxUtil;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxActor;
import physx.physics.PxForceModeEnum;
import physx.physics.PxRigidActor;

import java.util.*;

/**
 * OverWorld内の全てのDisplayedPhysxBoxを保持するクラス
 */
public class DisplayedBoxHolder {

    private final List<DisplayedPhysxBox> blockDisplayList = new ArrayList<>();

    /**
     * デバッグモードでプレイヤーがブロックを右クリックした時、座標にBlockDisplayを1個生成して、箱と紐づける
     *
     * @param player プレイヤー
     */
    public DisplayedPhysxBox debugCreate(Player player) {
        if (!player.getInventory().getItemInMainHand().getType().isBlock()) {
            return null;
        }

        int scale = player.getInventory().getHeldItemSlot() + 1;
        return createDisplayedBox(player.getEyeLocation(), new Vector(scale, scale, scale), player.getInventory().getItemInMainHand());
    }

    /**
     * DisplayedPhysxBoxを1個生成
     *
     * @param location  場所
     * @param scale     大きさ
     * @param itemStack 元となるブロック
     * @return
     */
    public DisplayedPhysxBox createDisplayedBox(Location location, Vector scale, ItemStack itemStack) {
        Quaternionf quat = new Quaternionf()
                .rotateY((float) -Math.toRadians(location.getYaw()))
                .rotateX((float) Math.toRadians(location.getPitch()));

        // なめらかな補完のために2つBlockDisplayを作る
        BlockDisplay[] display = new BlockDisplay[]{createDisplay(itemStack, location, scale, quat), createDisplay(itemStack, location, scale, quat)};

        BlockData blockData = itemStack.getType().createBlockData();
        Collection<BoundingBox> boundingBoxes;
        try {
            boundingBoxes = BoundingBoxUtil.getOutlineBoxes(BoundingBoxUtil.getOutline(display[0], blockData));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Map<PxBoxGeometry, PxVec3> boxGeometries = new HashMap<>();
        for (BoundingBox boundingBox : boundingBoxes) {
            Vector geometry = BoundingBoxUtil.getGeometryFromBoundingBox(boundingBox).multiply(scale);
            Vector center = BoundingBoxUtil.getCenterFromBoundingBox(boundingBox).multiply(scale);
            boxGeometries.put(new PxBoxGeometry((float) geometry.getX(), (float) geometry.getY(), (float) geometry.getZ()), new PxVec3((float) center.getX(), (float) center.getY(), (float) center.getZ()));
        }

        DisplayedPhysxBox box = PhysxMc.physxWorld.addBox(new PxVec3((float) location.x(), (float) location.y(), (float) location.z()), new PxQuat(quat.x, quat.y, quat.z, quat.w), boxGeometries, display);
        blockDisplayList.add(box);
        return box;
    }

    /**
     * 指定されたパラメータでBlockDisplayを生成
     *
     * @param itemStack itemstack
     * @param location  場所
     * @param scale     大きさ
     * @param boxQuat   回転
     * @return　作ったBlockDisplay
     */
    private static BlockDisplay createDisplay(ItemStack itemStack, Location location, Vector scale, Quaternionf boxQuat) {
        BlockDisplay blockDisplay = location.getWorld().spawn(location, BlockDisplay.class);
        blockDisplay.setBlock(itemStack.getType().createBlockData());
        Transformation transformation = blockDisplay.getTransformation();
        transformation.getTranslation().add(-0.5f, -0.5f, -0.5f);
        transformation.getScale().x = (float) scale.getX();
        transformation.getScale().y = (float) scale.getY();
        transformation.getScale().z = (float) scale.getZ();
        transformation.getLeftRotation().set(boxQuat);
        blockDisplay.setTransformation(transformation);
        blockDisplay.setGravity(false);
        return blockDisplay;
    }

    /**
     * ワールドに存在する全ての箱を更新する
     */
    public void update() {

        destroyUnusableBox();
        for (DisplayedPhysxBox displayedPhysxBox : blockDisplayList) {
            displayedPhysxBox.update();

            if (!displayedPhysxBox.isSleeping())
                PhysxMc.physxWorld.registerChunksToLoadNextTick(displayedPhysxBox.getSurroundingChunks());
        }
        PhysxMc.physxWorld.setReadyToUpdateChunks();
    }

    /**
     * ワールドの外に落ちたりdisplayが破壊されていたりする不良品を削除
     */
    private void destroyUnusableBox() {
        blockDisplayList.removeIf(box -> {
            if (box == null)
                return false;

            if (box.getLocation().y() < -128 || box.isDisplayDead()) {
                for (BlockDisplay blockDisplay : box.display) {
                    blockDisplay.remove();
                }
                PhysxMc.physxWorld.removeBox(box);
                return true;
            }

            return false;
        });
    }

    /**
     * 全てのBlockDisplayと箱を消去する
     */
    public void destroyAll() {
        blockDisplayList.forEach(block -> {
            for (BlockDisplay blockDisplay : block.display) {
                blockDisplay.remove();
            }
            PhysxMc.physxWorld.removeBox(block);
        });
        blockDisplayList.clear();
    }

    public void destroySpecific(DisplayedPhysxBox box) {
        if (box == null || !blockDisplayList.remove(box))
            return;

        for (BlockDisplay blockDisplay : box.display) {
            blockDisplay.remove();
        }
        PhysxMc.physxWorld.removeBox(box);
    }

    public boolean hasBox(DisplayedPhysxBox box) {
        return blockDisplayList.contains(box);
    }

    /**
     * 全ての箱に対して爆発を適用する
     */
    public void executeExplosion(Location location, float strength) {
        Vector3d tmp = new Vector3d();
        double explosionStrengthSquared = strength * 2.0 * strength * 2.0;

        for (DisplayedPhysxBox box : blockDisplayList) {
            PxVec3 pos = box.getPos().getP();
            tmp.set(pos.getX(), pos.getY(), pos.getZ());
            double distanceSquared = location.toVector().toVector3d().distanceSquared(tmp);
            if (distanceSquared <= explosionStrengthSquared) {
                double distance = Math.sqrt(distanceSquared);
                Vector3d direction = tmp.sub(location.toVector().toVector3d()).normalize();
                direction.y += 2.0;
                direction.normalize();
                double realStrength = (1.0 - (Math.min(Math.max(distance / (strength * 2.0), 0f), 1f))) * 15.0;

                PxVec3 pxVec = new PxVec3(
                        (float) (direction.x * realStrength),
                        (float) (direction.y * realStrength),
                        (float) (direction.z * realStrength)
                );
                box.addForce(pxVec, PxForceModeEnum.eVELOCITY_CHANGE);
            }
        }
    }

    /**
     * 世界内でRayCastしてBoxを探す
     *
     * @param location 始点
     * @param distance 距離
     * @return 見つかったBox
     */
    @Nullable
    public DisplayedPhysxBox raycast(Location location, float distance) {
        PxRigidActor actor = PhysxMc.physxWorld.raycast(location, distance);
        if (actor == null)
            return null;
        return blockDisplayList.stream().filter(displayedPhysxBox -> displayedPhysxBox.getActor().equals(actor)).findFirst().orElse(null);
    }

    /**
     * 箱本体をDisplaydPhysxBoxの形に変換
     *
     * @param actor 箱の物理オブジェクトクラス
     * @return ワールドにあるDisplaydPhysxBox
     */
    public DisplayedPhysxBox getBox(PxActor actor) {
        return blockDisplayList.stream().filter(displayedPhysxBox -> displayedPhysxBox.getActor().equals(actor)).findFirst().orElse(null);
    }
}
