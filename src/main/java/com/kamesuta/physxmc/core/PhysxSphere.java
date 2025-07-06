package com.kamesuta.physxmc.core;

import lombok.Getter;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.extensions.PxRigidBodyExt;
import physx.geometry.PxSphereGeometry;
import physx.physics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 物理演算システムで使う球体と、それにアタッチされた形状を格納するクラス
 */
public class PhysxSphere {

    /**
     * 球体本体
     */
    @Getter
    private final PxRigidDynamic actor;

    /**
     * 球体にアタッチされた形状(接触判定)達
     */
    private final List<PxShape> sphereShapes = new ArrayList<>();

    /**
     * 物理演算される球体を作る
     */
    public PhysxSphere(PxPhysics physics, PxMaterial material, SphereData data) {
        // create default simulation shape flags
        PxShapeFlags defaultShapeFlags;
        if (!data.isTrigger())
            defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        else
            defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eTRIGGER_SHAPE.value));//triggerはraycastに引っかからないようにする
        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        // create a sphere with contact reporting enabled
        int reportContactFlags = PxPairFlagEnum.eNOTIFY_TOUCH_FOUND.value | PxPairFlagEnum.eNOTIFY_TOUCH_LOST.value | PxPairFlagEnum.eNOTIFY_CONTACT_POINTS.value;
        PxFilterData tmpFilterData = new PxFilterData(1, -1, reportContactFlags, 0);

        // create a small dynamic actor with sphere geometry, which will fall on the ground
        tmpPose.setP(data.getPos());
        tmpPose.setQ(data.getQuat());
        PxRigidDynamic sphere = physics.createRigidDynamic(tmpPose);

        for (Map.Entry<PxSphereGeometry, PxVec3> entry : data.getSphereGeometries().entrySet()) {
            PxShape tmpShape = physics.createShape(entry.getKey(), material, true, defaultShapeFlags);
            tmpShape.setSimulationFilterData(tmpFilterData);
            PxTransform tmpPose2 = new PxTransform(PxIDENTITYEnum.PxIdentity);
            tmpPose2.setP(entry.getValue());
            tmpShape.setLocalPose(tmpPose2);
            sphere.attachShape(tmpShape);
            sphereShapes.add(tmpShape);
            tmpPose2.destroy();
            entry.getValue().destroy();
            entry.getKey().destroy();
        }

        PxRigidBodyExt.updateMassAndInertia(sphere, data.getDensity());

        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        data.getPos().destroy();

        this.actor = sphere;
    }

    /**
     * 球体の座標空間を取得する
     *
     * @return 球体の座標空間
     */
    public PxTransform getPos() {
        return actor.getGlobalPose();
    }

    /**
     * 球体の座標空間をセットする
     *
     * @param transform 球体の座標空間
     */
    public void setPos(PxTransform transform) {
        actor.setGlobalPose(transform);
        transform.destroy();
    }

    /**
     * 球体を破壊する。このクラスを消す際に必ず呼ぶこと
     */
    public void release() {
        if (actor.isReleasable())
            actor.release();
        for (int i = 0; i < sphereShapes.size(); ) {
            sphereShapes.get(i).release();
            sphereShapes.remove(i);
        }
    }

    /**
     * 球体に力を加える
     *
     * @param vec3 球体に加える力
     */
    public void addForce(PxVec3 vec3, PxForceModeEnum mode) {
        if (actor.getRigidBodyFlags().isSet(PxRigidBodyFlagEnum.eKINEMATIC))
            return;

        actor.addForce(vec3, mode);
        vec3.destroy();
    }

    /**
     * 球体がスリープ中(更新停止中)か取得する
     */
    public boolean isSleeping() {
        return actor.isSleeping();
    }
} 