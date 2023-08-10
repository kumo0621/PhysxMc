package com.kamesuta.physxmc.core;

import lombok.Getter;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.extensions.PxRigidBodyExt;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 物理演算システムで使う箱と、それにアタッチされた形状を格納するクラス
 */
public class PhysxBox {

    /**
     * 箱本体
     */
    @Getter
    private final PxRigidDynamic actor;

    /**
     * 箱にアタッチされた形状(接触判定)達
     */
    private final List<PxShape> boxShapes = new ArrayList<>();

    /**
     * 物理演算される箱を作る
     *
     */
    public PhysxBox(PxPhysics physics, PxMaterial defaultMaterial, BoxData data) {
        // create default simulation shape flags
        PxShapeFlags defaultShapeFlags;
        if (!data.isTrigger())
            defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        else
            defaultShapeFlags = new PxShapeFlags((byte) (PxShapeFlagEnum.eTRIGGER_SHAPE.value));//triggerはraycastに引っかからないようにする
        // create a few temporary objects used during setup
        PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        // create a box with contact reporting enabled
        int reportContactFlags = PxPairFlagEnum.eNOTIFY_TOUCH_FOUND.value | PxPairFlagEnum.eNOTIFY_TOUCH_LOST.value | PxPairFlagEnum.eNOTIFY_CONTACT_POINTS.value;
        PxFilterData tmpFilterData = new PxFilterData(1, -1, reportContactFlags, 0);

        // create a small dynamic actor with size 1x1x1, which will fall on the ground
        tmpPose.setP(data.getPos());
        tmpPose.setQ(data.getQuat());
        PxRigidDynamic box = physics.createRigidDynamic(tmpPose);

        for (Map.Entry<PxBoxGeometry, PxVec3> entry : data.getBoxGeometries().entrySet()) {
            PxShape tmpShape = physics.createShape(entry.getKey(), defaultMaterial, true, defaultShapeFlags);
            tmpShape.setSimulationFilterData(tmpFilterData);
            PxTransform tmpPose2 = new PxTransform(PxIDENTITYEnum.PxIdentity);
            tmpPose2.setP(entry.getValue());
            tmpShape.setLocalPose(tmpPose2);
            box.attachShape(tmpShape);
            boxShapes.add(tmpShape);
            tmpPose2.destroy();
            entry.getValue().destroy();
            entry.getKey().destroy();
        }

        PxRigidBodyExt.updateMassAndInertia(box, data.getDensity());

        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        data.getPos().destroy();

        this.actor = box;
    }

    /**
     * 箱の座標空間を取得する
     *
     * @return 箱の座標空間
     */
    public PxTransform getPos() {
        return actor.getGlobalPose();
    }

    /**
     * 箱の座標空間をセットする
     *
     * @param transform 箱の座標空間
     */
    public void setPos(PxTransform transform) {
        actor.setGlobalPose(transform);
        transform.destroy();
    }

    /**
     * 箱を破壊する。このクラスを消す際に必ず呼ぶこと
     */
    public void release() {
        if (actor.isReleasable())
            actor.release();
        for (int i = 0; i < boxShapes.size(); ) {
            boxShapes.get(i).release();
            boxShapes.remove(i);
        }
    }

    /**
     * 箱に力を加える
     *
     * @param vec3 箱に加える力
     */
    public void addForce(PxVec3 vec3, PxForceModeEnum mode) {
        if (actor.getRigidBodyFlags().isSet(PxRigidBodyFlagEnum.eKINEMATIC))
            return;

        actor.addForce(vec3, mode);
        vec3.destroy();
    }

    /**
     * Boxがスリープ中(更新停止中)か取得する
     */
    public boolean isSleeping() {
        return actor.isSleeping();
    }
}
