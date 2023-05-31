package com.kamesuta.physxmc;

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

public class PhysxTerrain {

    private final PxPhysics physics;

    private PxRigidStatic actor;
    private final List<PxShape> terrainShapes = new ArrayList<>();

    public PhysxTerrain(PxPhysics physics) {
        this.physics = physics;
    }

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

    public PxRigidStatic getActor() {
        return actor;
    }

    public void release() {
        actor.release();
        terrainShapes.forEach(PxBase::release);
    }

    private boolean areNeighboursEmpty(World level, Block pos) {
        return pos.getY() >= level.getMaxHeight() || pos.getY() <= level.getMinHeight()
                || (pos.getY() < level.getMaxHeight() - 1 && pos.getRelative(BlockFace.UP).isEmpty())
                || (pos.getY() > level.getMaxHeight() && pos.getRelative(BlockFace.DOWN).isEmpty())
                || pos.getRelative(BlockFace.NORTH).isEmpty()
                || pos.getRelative(BlockFace.EAST).isEmpty()
                || pos.getRelative(BlockFace.SOUTH).isEmpty()
                || pos.getRelative(BlockFace.WEST).isEmpty();
    }
}
