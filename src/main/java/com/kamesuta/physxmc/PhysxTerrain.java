package com.kamesuta.physxmc;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import physx.common.PxBase;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 地形(動かない箱)をその形状と共に保持するクラス
 */
public class PhysxTerrain {

    private final PxPhysics physics;

    @Getter
    private PxRigidStatic actor;
    private final List<PxShape> terrainShapes = new ArrayList<>();

    public PhysxTerrain(PxPhysics physics) {
        this.physics = physics;
    }

    /**
     * チャンクの形に応じた地形を作る
     * @param defaultMaterial　地形のマテリアル
     * @param chunk チャンク
     * @return 地形のオブジェクト
     */
    public PxActor createTerrain(PxMaterial defaultMaterial, Chunk chunk) {
        // create default simulation shape flags
        PxShapeFlags defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxVec3 tmpVec = new PxVec3(chunk.getX() * 16, 0f, chunk.getZ() * 16);
        tmpPose.setP(tmpVec);
        tmpVec.destroy();
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);
        PxRigidStatic terrain = physics.createRigidStatic(tmpPose);
        PxBoxGeometry terrainGeometry = new PxBoxGeometry(0.5f, 0.5f, 0.5f);   // PxBoxGeometry uses half-sizes

        
        final int minY = chunk.getWorld().getMinHeight();
        final int maxY = chunk.getWorld().getMaxHeight();
        for (int x = 0; x <= 15; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = 0; z <= 15; ++z) {
                    Block block = chunk.getBlock(x, y, z);
                    if(!areNeighboursEmpty(chunk.getWorld(), block) || block.getBoundingBox().getVolume() == 0)
                        continue;

                    PxShape shape = physics.createShape(terrainGeometry, defaultMaterial, true, defaultShapeFlags);
                    shape.setSimulationFilterData(tmpFilterData);
                    PxVec3 tmpVec2 = new PxVec3(x + 0.5f, y + 0.5f, z + 0.5f);
                    tmpPose.setP(tmpVec2);
                    tmpVec2.destroy();
                    shape.setLocalPose(tmpPose);
                    terrain.attachShape(shape);
                    terrainShapes.add(shape);
                }
            }
        }
        
        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        terrainGeometry.destroy();

        actor = terrain;

        return terrain;
    }

    /**
     * 地形とその形状を破壊する
     */
    public void release() {
        actor.release();
        terrainShapes.forEach(PxBase::release);
    }

    /**
     * ブロックの隣に空洞があるか判定する
     * @param level 世界
     * @param pos 座標
     * @return ブロックの隣に空洞があるかどうか
     */
    private static boolean areNeighboursEmpty(World level, Block pos) {
        return pos.getY() >= level.getMaxHeight() || pos.getY() <= level.getMinHeight()
                || (pos.getY() < level.getMaxHeight() - 1 && pos.getRelative(BlockFace.UP).isEmpty())
                || (pos.getY() > level.getMaxHeight() && pos.getRelative(BlockFace.DOWN).isEmpty())
                || level.isChunkLoaded(pos.getRelative(BlockFace.NORTH).getX(), pos.getRelative(BlockFace.NORTH).getZ()) && pos.getRelative(BlockFace.NORTH).isEmpty()
                || level.isChunkLoaded(pos.getRelative(BlockFace.EAST).getX(), pos.getRelative(BlockFace.EAST).getZ()) && pos.getRelative(BlockFace.EAST).isEmpty()
                || level.isChunkLoaded(pos.getRelative(BlockFace.SOUTH).getX(), pos.getRelative(BlockFace.SOUTH).getZ()) && pos.getRelative(BlockFace.SOUTH).isEmpty()
                || level.isChunkLoaded(pos.getRelative(BlockFace.WEST).getX(), pos.getRelative(BlockFace.WEST).getZ()) && pos.getRelative(BlockFace.WEST).isEmpty();
    }
}
