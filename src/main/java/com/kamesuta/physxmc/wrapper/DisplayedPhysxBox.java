package com.kamesuta.physxmc.wrapper;

import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.core.BoxData;
import com.kamesuta.physxmc.core.PhysxBox;
import com.kamesuta.physxmc.utils.ConversionUtility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.physics.PxForceModeEnum;
import physx.physics.PxPhysics;
import physx.physics.PxRigidBodyFlagEnum;

import java.util.*;

import static com.kamesuta.physxmc.core.Physx.defaultMaterial;
import static com.kamesuta.physxmc.core.Physx.coinMaterial;

/**
 * Minecraft世界で表示可能なPhysxBox
 */
public class DisplayedPhysxBox extends PhysxBox {

    /**
     * 表示用のBlockDisplay
     */
    public List<DisplayData> displayMap = new ArrayList<>();
    
    private World world;
    
    /**
     * このオブジェクトがコインかどうか
     */
    private boolean isCoin = false;

    public DisplayedPhysxBox(PxPhysics physics, BoxData data, Map<BlockDisplay[], Vector> display) {
        this(physics, data, display, false);
    }

    public DisplayedPhysxBox(PxPhysics physics, BoxData data, Map<BlockDisplay[], Vector> display, boolean isCoin) {
        super(physics, isCoin ? coinMaterial : defaultMaterial, data);

        display.forEach((blockDisplays, vector) -> this.displayMap.add(new DisplayData(blockDisplays, vector, 0)));
        BlockDisplay[] firstDisplay = displayMap.get(0).displays;
        world = firstDisplay[0].getWorld();
        this.isCoin = isCoin;
    }

    /**
     * このオブジェクトがコインかどうか
     */
    public boolean isCoin() {
        return isCoin;
    }

    public void update() {
        trySwap();
    }

    /**
     * 物理の箱とBlockDisplayを同期する
     */
    private void trySwap() {
        for (DisplayData displayData : displayMap) {
            BlockDisplay[] displays = displayData.getDisplays();
            Vector offset = displayData.getOffset();
            int swapPhase = displayData.getSwapPhase();
            
            PxQuat q = getPos().getQ();
            PxVec3 p = getPos().getP();
            Location pos = new Location(displays[0].getWorld(), p.getX(), p.getY(), p.getZ());

            // スワップのフェーズ管理 (2ティックかけてスワップが完了する)
            if (swapPhase == 2) {
                swap(pos, displays);
                swapPhase = 0;
            }
            if (swapPhase == 1) {
                preSwap(pos, displays);
                swapPhase = 2;
            }
            // 位置が16マス以上離れていたら次のティックからスワップを開始する
            if (swapPhase == 0 && displays[0].getLocation().toVector().distance(new Vector(p.getX(), p.getY(), p.getZ())) > 16) {
                swapPhase = 1;
            }

            for (BlockDisplay blockDisplay : displays) {
                Location prev = blockDisplay.getLocation();

                Quaternionf boxQuat = new Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
                Transformation transformation = blockDisplay.getTransformation();
                transformation.getLeftRotation().set(boxQuat);
                transformation.getTranslation().set(p.getX() - prev.getX(), p.getY() - prev.getY(), p.getZ() - prev.getZ());
                Matrix4f matrix = ConversionUtility.getTransformationMatrix(transformation);
                matrix.translate(-.5f + (float) offset.getX(), -.5f + (float) offset.getY(), -.5f + (float) offset.getZ());
                blockDisplay.setTransformationMatrix(matrix);
                // なめらかに補完する
                blockDisplay.setInterpolationDelay(0);
                blockDisplay.setInterpolationDuration(1);
                // blockDisplay.teleport(new Location(blockDisplay.getWorld(), p.getX(), p.getY(), p.getZ()));
            }
        }
    }

    /**
     * スワップの1ティック前に呼ぶ
     *
     * @param pos 新しい位置
     */
    private void preSwap(Location pos, BlockDisplay[] display) {
        display[0].setVisibleByDefault(true);
    }

    /**
     * TPの移動が見えないようにスワップする
     *
     * @param pos 新しい位置
     */
    private void swap(Location pos, BlockDisplay[] display) {
        display[1].setVisibleByDefault(false);
        display[1].teleport(pos);

        BlockDisplay temp = display[1];
        display[1] = display[0];
        display[0] = temp;
    }

    /**
     * 箱をコンフィグで設定したパワーで投げる
     *
     * @param location 向き
     */
    public void throwBox(Location location) {
        // コインの場合はコイン専用の投擲力を使用
        double power = isCoin ? PhysxSetting.getCoinThrowPower() : PhysxSetting.getThrowPower();
        Vector3f rot = location.getDirection().clone().multiply(power).toVector3f();
        PxVec3 force = new PxVec3(rot.x, rot.y, rot.z);
        addForce(force, PxForceModeEnum.eVELOCITY_CHANGE);
    }

    /**
     * 箱の周囲のチャンクを取得
     *
     * @return 箱があるチャンクとその8方にあるチャンク
     */
    public Collection<Chunk> getSurroundingChunks() {
        int[] offset = {-1, 0, 1};
        
        PxVec3 p = getPos().getP();
        Location pos = new Location(world, p.getX(), p.getY(), p.getZ());
        int baseX = pos.getChunk().getX();
        int baseZ = pos.getChunk().getZ();

        Collection<Chunk> chunksAround = new HashSet<>();
        for (int x : offset) {
            for (int z : offset) {
                Chunk chunk = world.getChunkAt(baseX + x, baseZ + z);
                chunksAround.add(chunk);
            }
        }
        return chunksAround;
    }

    /**
     * boxの持つ回転を取得する
     *
     * @return
     */
    public Quaternionf getQuat() {
        PxQuat q = getPos().getQ();
        return new Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
    }

    /**
     * boxの座標と回転をminecraftのLocationの形式で取得する(rollは失われる)
     *
     * @return location
     */
    public Location getLocation() {
        PxVec3 vec3 = getPos().getP();
        PxQuat q = getPos().getQ();
        Quaternionf boxQuat = new Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
        Vector3f dir = ConversionUtility.convertToEulerAngles(boxQuat);
        Vector dir2 = new Vector(dir.x, dir.y, dir.z);
        Location loc = new Location(world, vec3.getX(), vec3.getY(), vec3.getZ());
        loc.setDirection(dir2);
        return loc;
    }

    /**
     * boxが重力の影響を受けないようにするか変更する
     */
    public void makeKinematic(boolean flag) {
        getActor().setRigidBodyFlag(PxRigidBodyFlagEnum.eKINEMATIC, flag);
    }

    /**
     * 重力の影響を受けないboxを移動させる
     */
    public void moveKinematic(Location location) {
        Vector p = new Vector((float) location.x(), (float) location.y(), (float) location.z());

        // 相対的に回転させる
        Quaternionf quat = new Quaternionf();
        quat.rotateY((float) -Math.toRadians(location.getYaw()));
        quat.rotateX((float) Math.toRadians(location.getPitch()));

        moveKinematic(p, quat);
    }

    public void moveKinematic(Vector pos, Quaternionf rot) {
        PxVec3 p = new PxVec3((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());

        PxQuat q = new PxQuat(rot.x, rot.y, rot.z, rot.w);
        PxTransform transform = new PxTransform(PxIDENTITYEnum.PxIdentity);
        transform.setP(p);
        transform.setQ(q);
        getActor().setKinematicTarget(transform);
        p.destroy();
        q.destroy();
        transform.destroy();
    }

    /**
     * 表示部分のDisplayがMinecraft側でkillされたかどうか
     * @return
     */
    public boolean isDisplayDead(){
        BlockDisplay[] display = displayMap.get(0).displays;
        return display[0].isDead() || display[1].isDead();
    }

    /**
     * オブジェクトのスケールを変更する（プッシャー用）
     * @param newScale 新しいスケール
     */
    public void updateScale(Vector newScale) {
        if (displayMap == null || displayMap.isEmpty()) {
            return;
        }
        
        for (DisplayData displayData : displayMap) {
            for (BlockDisplay blockDisplay : displayData.getDisplays()) {
                Transformation transformation = blockDisplay.getTransformation();
                transformation.getScale().x = (float) newScale.getX();
                transformation.getScale().y = (float) newScale.getY();
                transformation.getScale().z = (float) newScale.getZ();
                blockDisplay.setTransformation(transformation);
                // なめらかに補完する
                blockDisplay.setInterpolationDelay(0);
                blockDisplay.setInterpolationDuration(1);
            }
        }
    }

    @AllArgsConstructor
    @Data
    public class DisplayData{
        private final BlockDisplay[] displays;
        private final Vector offset;
        
        /**
         * スワップのフェーズ管理
         */
        private int swapPhase;
    }
}
