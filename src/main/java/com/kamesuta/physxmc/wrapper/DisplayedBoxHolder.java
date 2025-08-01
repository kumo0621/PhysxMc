package com.kamesuta.physxmc.wrapper;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.core.BoxData;
import com.kamesuta.physxmc.utils.BoundingBoxUtil;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
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
     * プレイヤーがブロックを投げるとき、連結するブロックのオフセットのマップ
     */
    @Getter
    private final Map<Player, List<Vector>> offsetMap = new HashMap<>();

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
        List<Vector> offsets = offsetMap.get(player);
        if(offsets == null)
            offsets = List.of(new Vector());
        return createDisplayedBox(player.getEyeLocation(), new Vector(scale, scale, scale), player.getInventory().getItemInMainHand(), offsets);
    }

    /**
     * DisplayedPhysxBoxを1個生成
     *
     * @param location  場所
     * @param scale     大きさ
     * @param itemStack 元となるブロック
     * @param offsets 複数ブロックを連結する際のオフセット
     * @return
     */
    public DisplayedPhysxBox createDisplayedBox(Location location, Vector scale, ItemStack itemStack, List<Vector> offsets) {
        return createDisplayedBox(location, scale, itemStack, offsets, -1f); // デフォルト密度を使用
    }

    /**
     * DisplayedPhysxBoxを1個生成（密度指定可能）
     *
     * @param location  場所
     * @param scale     大きさ
     * @param itemStack 元となるブロック
     * @param offsets 複数ブロックを連結する際のオフセット
     * @param density 密度（負の値の場合はデフォルト密度を使用）
     * @return
     */
    public DisplayedPhysxBox createDisplayedBox(Location location, Vector scale, ItemStack itemStack, List<Vector> offsets, float density) {
        return createDisplayedBox(location, scale, itemStack, offsets, density, false);
    }

    /**
     * DisplayedPhysxBoxを1個生成（密度・プッシャーフラグ指定可能）
     *
     * @param location  場所
     * @param scale     大きさ
     * @param itemStack 元となるブロック
     * @param offsets 複数ブロックを連結する際のオフセット
     * @param density 密度（負の値の場合はデフォルト密度を使用）
     * @param isPusher プッシャーの一部かどうか
     * @return
     */
    public DisplayedPhysxBox createDisplayedBox(Location location, Vector scale, ItemStack itemStack, List<Vector> offsets, float density, boolean isPusher) {
        try {
            // 入力値の検証
            if (location == null || location.getWorld() == null) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: 無効な位置");
                return null;
            }
            
            if (scale == null || scale.getX() <= 0 || scale.getY() <= 0 || scale.getZ() <= 0) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: 無効なスケール " + scale);
                return null;
            }
            
            if (itemStack == null) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: アイテムスタックがnull");
                return null;
            }
            
            if (offsets == null || offsets.isEmpty()) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: オフセットがnullまたは空");
                return null;
            }
            
            // 安全な角度の正規化
            float yaw = location.getYaw();
            float pitch = location.getPitch();
            
            org.bukkit.Bukkit.getLogger().info("デバッグ: 元の角度 - yaw=" + yaw + ", pitch=" + pitch);
            
            // NaNやInfinityのチェック
            if (Float.isNaN(yaw) || Float.isInfinite(yaw)) yaw = 0;
            if (Float.isNaN(pitch) || Float.isInfinite(pitch)) pitch = 0;
            
            // 角度の範囲制限
            while (yaw > 180) yaw -= 360;
            while (yaw < -180) yaw += 360;
            pitch = Math.max(-90, Math.min(90, pitch));
            
            org.bukkit.Bukkit.getLogger().info("デバッグ: 正規化後の角度 - yaw=" + yaw + ", pitch=" + pitch);
            
            Quaternionf quat = new Quaternionf()
                    .rotateY((float) -Math.toRadians(yaw))
                    .rotateX((float) Math.toRadians(pitch));
                    
            // クォータニオンの正規化
            quat.normalize();
            
            org.bukkit.Bukkit.getLogger().info("デバッグ: クォータニオン - x=" + quat.x + ", y=" + quat.y + ", z=" + quat.z + ", w=" + quat.w);
            
            Map<BlockDisplay[], Vector> displayMap = new HashMap<>();
            Map<PxBoxGeometry, PxVec3> boxGeometries = new HashMap<>();

            for (Vector offset : offsets) {
                // なめらかな補完のために2つBlockDisplayを作る
                BlockDisplay[] display = new BlockDisplay[]{createDisplay(itemStack, location, scale, quat), createDisplay(itemStack, location, scale, quat)};
                displayMap.put(display, new Vector(offset.getX(), offset.getY(), offset.getZ()));
                Map<PxBoxGeometry, PxVec3> boxGeometry = getBoxGeometries(itemStack, display[0], scale, new Vector(offset.getX(), offset.getY(), offset.getZ()).multiply(scale));
                boxGeometries.putAll(boxGeometry);
            }

            // 密度の決定と検証
            float finalDensity = density > 0 ? density : com.kamesuta.physxmc.PhysxSetting.getDefaultDensity();
            if (Float.isNaN(finalDensity) || Float.isInfinite(finalDensity) || finalDensity <= 0) {
                finalDensity = com.kamesuta.physxmc.PhysxSetting.getDefaultDensity();
            }
            
            // 位置の検証
            double x = location.x();
            double y = location.y();
            double z = location.z();
            
            if (Double.isNaN(x) || Double.isInfinite(x)) x = 0;
            if (Double.isNaN(y) || Double.isInfinite(y)) y = 100;
            if (Double.isNaN(z) || Double.isInfinite(z)) z = 0;
            
            BoxData data = new BoxData(new PxVec3((float) x, (float) y, (float) z), new PxQuat(quat.x, quat.y, quat.z, quat.w), boxGeometries, false, finalDensity);
            DisplayedPhysxBox box = PhysxMc.physxWorld.addBox(data, displayMap, density == com.kamesuta.physxmc.PhysxSetting.getCoinDensity(), isPusher);
            
            if (box != null) {
                // アクターが正常に作成されているか検証
                if (box.getActor() == null) {
                    org.bukkit.Bukkit.getLogger().severe("ボックス作成失敗: PhysXアクターが作成されませんでした");
                    // 失敗したDisplayをクリーンアップ
                    for (DisplayedPhysxBox.DisplayData data2 : box.displayMap) {
                        for (org.bukkit.entity.BlockDisplay display : data2.getDisplays()) {
                            display.remove();
                        }
                    }
                    return null;
                }
                blockDisplayList.add(box);
                return box;
            } else {
                org.bukkit.Bukkit.getLogger().severe("ボックス作成失敗: PhysXWorldからnullが返されました");
                return null;
            }
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ブロックの判定取得
     */
    private static Map<PxBoxGeometry, PxVec3> getBoxGeometries(ItemStack itemStack, Entity display, Vector scale, Vector offset){
        BlockData blockData = itemStack.getType().createBlockData();
        Collection<BoundingBox> boundingBoxes;
        try {
            boundingBoxes = BoundingBoxUtil.getOutlineBoxes(BoundingBoxUtil.getOutline(display, blockData));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Map<PxBoxGeometry, PxVec3> boxGeometries = new HashMap<>();
        for (BoundingBox boundingBox : boundingBoxes) {
            Vector geometry = BoundingBoxUtil.getGeometryFromBoundingBox(boundingBox).multiply(scale);
            Vector center = BoundingBoxUtil.getCenterFromBoundingBox(boundingBox).multiply(scale);
            boxGeometries.put(new PxBoxGeometry((float) geometry.getX(), (float) geometry.getY(), (float) geometry.getZ()), new PxVec3((float) center.getX() + (float)offset.getX(), (float) center.getY() + (float)offset.getY(), (float) center.getZ() + (float)offset.getZ()));
        }
        return boxGeometries;
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
                for (DisplayedPhysxBox.DisplayData data : box.displayMap){
                    for (BlockDisplay blockDisplay : data.getDisplays()) {
                        blockDisplay.remove();
                    }
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
        int destroyedCount = blockDisplayList.size();
        blockDisplayList.forEach(box -> {
            for (DisplayedPhysxBox.DisplayData data : box.displayMap){
                for (BlockDisplay blockDisplay : data.getDisplays()) {
                    blockDisplay.remove();
                }
            }
            PhysxMc.physxWorld.removeBox(box);
        });
        blockDisplayList.clear();
        if (destroyedCount > 0) {
            java.util.logging.Logger.getLogger("PhysxMc").info("DisplayedBoxHolder: " + destroyedCount + "個のボックスを削除しました");
        }
    }

    public void destroySpecific(DisplayedPhysxBox box) {
        if (box == null || !blockDisplayList.remove(box))
            return;

        for (DisplayedPhysxBox.DisplayData data : box.displayMap){
            for (BlockDisplay blockDisplay : data.getDisplays()) {
                blockDisplay.remove();
            }
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

    /**
     * 全てのボックスオブジェクトのリストを取得（永続化用）
     */
    public List<DisplayedPhysxBox> getAllBoxes() {
        return new ArrayList<>(blockDisplayList);
    }
}
