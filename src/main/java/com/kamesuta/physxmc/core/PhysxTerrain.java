package com.kamesuta.physxmc.core;

import lombok.Getter;
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

    /**
     * 地形本体
     */
    @Getter
    protected PxRigidStatic actor;

    /**
     * 地形にアタッチされた形状たち
     */
    protected final List<PxShape> terrainShapes = new ArrayList<>();

    public static final String name = "terrain";

    /**
     * y=-61にフラットな地形を作る
     *
     * @param defaultMaterial 　地形のマテリアル
     * @return 地形のオブジェクト
     */
    public PhysxTerrain(PxPhysics physics, PxMaterial defaultMaterial) {
        if(physics == null)
            return;
        
        // create default simulation shape flags
        PxShapeFlags defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);

        // create a large static box with size 20x1x20 as ground
        PxVec3 tmpVec = new PxVec3(0f, -61f, 0f);
        tmpPose.setP(tmpVec);
        PxBoxGeometry groundGeometry = new PxBoxGeometry(100f, 0.5f, 100f);   // PxBoxGeometry uses half-sizes
        PxShape groundShape = physics.createShape(groundGeometry, defaultMaterial, true, defaultShapeFlags);
        PxRigidStatic ground = physics.createRigidStatic(tmpPose);
        groundShape.setSimulationFilterData(tmpFilterData);
        terrainShapes.add(groundShape);
        ground.attachShape(groundShape);
        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
    }

    /**
     * 地形とその形状を破壊する
     */
    public void release() {
        actor.release();
        terrainShapes.forEach(PxBase::release);
    }
}
