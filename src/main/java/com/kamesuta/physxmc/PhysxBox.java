package com.kamesuta.physxmc;

import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.extensions.PxRigidBodyExt;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

public class PhysxBox {

    private final PxPhysics physics;

    private PxRigidDynamic actor;
    private PxShape boxShape;

    public PhysxBox(PxPhysics physics) {
        this.physics = physics;
    }

    public PxRigidDynamic createBox(PxMaterial defaultMaterial, PxVec3 pos, PxQuat quat) {
        return createBox(defaultMaterial, pos, quat, new PxBoxGeometry(0.5f, 0.5f, 0.5f));// PxBoxGeometry uses half-sizes
    }

    public PxRigidDynamic createBox(PxMaterial defaultMaterial, PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry) {
        // create default simulation shape flags
        PxShapeFlags defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxFilterData tmpFilterData = new PxFilterData(1, 1, 0, 0);

        // create a small dynamic actor with size 1x1x1, which will fall on the ground
        tmpPose.setP(pos);
        tmpPose.setQ(quat);
        boxShape = physics.createShape(boxGeometry, defaultMaterial, true, defaultShapeFlags);
        PxRigidDynamic box = physics.createRigidDynamic(tmpPose);
        boxShape.setSimulationFilterData(tmpFilterData);
        box.attachShape(boxShape);

        PxRigidBodyExt.updateMassAndInertia(box, 0.1f);

        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        boxGeometry.destroy();
        pos.destroy();

        this.actor = box;
        return box;
    }

    public PxTransform getPos() {
        return actor.getGlobalPose();
    }

    public void setPos(PxTransform transform) {
        actor.setGlobalPose(transform);
        transform.destroy();
    }

    public PxRigidDynamic getActor() {
        return actor;
    }

    public void release() {
        actor.release();
        boxShape.release();
    }

    public void addForce(PxVec3 vec3) {
        actor.addForce(vec3, PxForceModeEnum.eFORCE);
        vec3.destroy();
    }
}
