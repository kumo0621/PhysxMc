package com.kamesuta.physxmc;

import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

public class PhysxGround {

    private PxPhysics physics;

    private PxRigidStatic actor;
    private PxShape groundShape;

    public PhysxGround(PxPhysics physics){
        this.physics = physics;
    }

    public PxActor createGround(PxMaterial defaultMaterial){
        // create default simulation shape flags
        PxShapeFlags defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);

        // create a large static box with size 20x1x20 as ground
        PxVec3 tmpVec = new PxVec3(0f, -61f, 0f);
        tmpPose.setP(tmpVec);
        PxBoxGeometry groundGeometry = new PxBoxGeometry(100f, 0.5f, 100f);   // PxBoxGeometry uses half-sizes
        groundShape = physics.createShape(groundGeometry, defaultMaterial, true, defaultShapeFlags);
        PxRigidStatic ground = physics.createRigidStatic(tmpPose);
        groundShape.setSimulationFilterData(tmpFilterData);
        ground.attachShape(groundShape);

        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        groundGeometry.destroy();
        
        actor = ground;

        return ground;
    }

    public PxRigidStatic getActor(){
        return actor;
    }

    public void release(){
        actor.release();
        groundShape.release();
    }
}
