package com.kamesuta.physxmc;

import com.comphenix.protocol.utility.MinecraftReflection;
import net.kyori.adventure.chat.ChatType;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;

public class BoundingBoxUtil {
    /**
     * 初期化が完了しているかどうか
     */
    private static boolean initialized = false;

    /**
     * NMS: CraftBlockData.getState
     */
    private static Method craftBlockDataGetStateMethod;

    /**
     * NMS: CraftWorld.getHandle
     */
    private static Method craftWorldGetHandleMethod;

    /**
     * NMS: CraftEntity.getHandle
     */
    private static Method craftEntityGetHandleMethod;

    /**
     * NMS: CraftRayTraceResult.fromNMS
     */
    private static Method craftRayTraceResultFromNMSMethod;

    /**
     * NMS: BlockPosition::new
     */
    private static Constructor<?> blockPositionConstructor;

    /**
     * NMS: VoxelShapeCollision.of
     */
    private static Method voxelShapeCollisionOfMethod;

    /**
     * NMS: BlockBase.BlockData.a(IBlockAccess world, BlockPosition pos, VoxelShapeCollision context)
     */
    private static Method blockDataGetOutlineShapeMethod;

    /**
     * NMS: Vec3D::new
     */
    private static Constructor<?> vec3dConstructor;

    /**
     * NMS: VoxelShape.a(Vec3D start, Vec3D end, BlockPosition pos)
     */
    private static Method voxelShapeRaycastMethod;

    /**
     * NMS: CraftVoxelShape::new
     */
    private static Constructor<?> craftVoxelShapeConstructor;

    /**
     * NMS: CraftVoxelShape.getBoundingBoxes
     */
    private static Method craftVoxelShapeGetBoundingBoxes;

    /**
     * 必要なリフレクションフィールドを取得
     */
    public static void init() throws ReflectiveOperationException {
        Class<?> craftBlockDataClass = MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData");
        craftBlockDataGetStateMethod = craftBlockDataClass.getMethod("getState");

        Class<?> craftWorldClass = MinecraftReflection.getCraftBukkitClass("CraftWorld");
        craftWorldGetHandleMethod = craftWorldClass.getMethod("getHandle");

        Class<?> craftEntityClass = MinecraftReflection.getCraftBukkitClass("entity.CraftEntity");
        craftEntityGetHandleMethod = craftEntityClass.getMethod("getHandle");

        Class<?> movingObjectPosition = MinecraftReflection.getMinecraftClass("world.phys.MovingObjectPosition");
        Class<?> craftRayTraceResultClass = MinecraftReflection.getCraftBukkitClass("util.CraftRayTraceResult");
        craftRayTraceResultFromNMSMethod = craftRayTraceResultClass.getMethod("fromNMS", World.class, movingObjectPosition);

        Class<?> blockPositionClass = MinecraftReflection.getMinecraftClass("core.BlockPosition");
        blockPositionConstructor = blockPositionClass.getConstructor(int.class, int.class, int.class);

        Class<?> entityClass = MinecraftReflection.getMinecraftClass("world.entity.Entity");
        Class<?> voxelShapeCollisionClass = MinecraftReflection.getMinecraftClass("world.phys.shapes.VoxelShapeCollision");
        voxelShapeCollisionOfMethod = voxelShapeCollisionClass.getMethod("a", entityClass);

        Class<?> blockAccessClass = MinecraftReflection.getMinecraftClass("world.level.IBlockAccess");
        Class<?> blockDataClass = MinecraftReflection.getMinecraftClass("world.level.block.state.BlockBase$BlockData");
        blockDataGetOutlineShapeMethod = blockDataClass.getMethod("a", blockAccessClass, blockPositionClass, voxelShapeCollisionClass);

        Class<?> vec3dClass = MinecraftReflection.getMinecraftClass("world.phys.Vec3D");
        vec3dConstructor = vec3dClass.getConstructor(double.class, double.class, double.class);

        Class<?> voxelShapeClass = MinecraftReflection.getMinecraftClass("world.phys.shapes.VoxelShape");
        voxelShapeRaycastMethod = voxelShapeClass.getMethod("a", vec3dClass, vec3dClass, blockPositionClass);

        Class<?> craftVoxelShapeClass = MinecraftReflection.getCraftBukkitClass("util.CraftVoxelShape");
        craftVoxelShapeConstructor = craftVoxelShapeClass.getConstructor(voxelShapeClass);
        craftVoxelShapeGetBoundingBoxes = craftVoxelShapeClass.getMethod("getBoundingBoxes");

        // 初期化完了
        initialized = true;
    }

    /**
     * アウトラインを取得
     *
     * @param block ブロック
     * @return NMS: VoxelShape
     */
    public static Object getOutline(Entity entity, BlockData block) throws ReflectiveOperationException {
        if (!initialized) {
            throw new ReflectiveOperationException("BlockOutline is not initialized.");
        }

        // ワールドを取得
        World world = entity.getWorld();
        // NMS: worldを取得
        Object nmsWorld = craftWorldGetHandleMethod.invoke(world);
        // NMS: entityを取得
        Object nmsEntity = craftEntityGetHandleMethod.invoke(entity);

        // NMS: VoxelShapeCollisionを作成
        Object nmsVoxelShapeCollision = voxelShapeCollisionOfMethod.invoke(null, nmsEntity);
        // NMS: BlockPositionを作成
        Object nmsBlockPosition = blockPositionConstructor.newInstance(0, 0, 0);

        // NMS: stateを取得
        Object nmsState = craftBlockDataGetStateMethod.invoke(block);
        // NMS: getOutlineShapeを呼び出し
        return blockDataGetOutlineShapeMethod.invoke(nmsState, nmsWorld, nmsBlockPosition, nmsVoxelShapeCollision);
    }

    /**
     * アウトラインをバウンディングボックスに変換
     *
     * @param nmsOutline NMS: VoxelShape
     * @return バウンディングボックスのリスト
     */
    public static Collection<BoundingBox> getOutlineBoxes(Object nmsOutline) throws ReflectiveOperationException {
        if (!initialized) {
            throw new ReflectiveOperationException("BlockOutline is not initialized.");
        }

        // NMS: バウンディングボックスのリストを取得
        Object nmsCraftVoxelShape = craftVoxelShapeConstructor.newInstance(nmsOutline);
        @SuppressWarnings("unchecked")
        Collection<BoundingBox> nmsBoundingBoxes = (Collection<BoundingBox>) craftVoxelShapeGetBoundingBoxes.invoke(nmsCraftVoxelShape);

        return nmsBoundingBoxes;
    }
    
    public static Vector getCenterFromBoundingBox(BoundingBox boundingBox){
        Vector center = new Vector(boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
        return center.subtract(new Vector(0.5d, 0.5d, 0.5d));
    }
    
    public static Vector getGeometryFromBoundingBox(BoundingBox boundingBox){
        return new Vector(boundingBox.getWidthX() / 2, boundingBox.getHeight() / 2, boundingBox.getWidthZ() / 2);
    }
}
