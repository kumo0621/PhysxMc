package com.kamesuta.physxmc;

import lombok.Getter;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.extensions.PxRigidBodyExt;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

/**
 * 物理演算システムで使う箱と、それにアタッチされた形状を格納するクラス
 */
public class PhysxBox {

    private final PxPhysics physics;

    @Getter
    private PxRigidDynamic actor;
    private PxShape boxShape;

    public PhysxBox(PxPhysics physics) {
        this.physics = physics;
    }

    /**
     * 物理演算される箱を作る
     * @param defaultMaterial　箱のマテリアル
     * @param pos 箱の位置
     * @param quat 箱の角度
     * @return 箱のオブジェクト
     */
    public PxRigidDynamic createBox(PxMaterial defaultMaterial, PxVec3 pos, PxQuat quat) {
        return createBox(defaultMaterial, pos, quat, new PxBoxGeometry(0.5f, 0.5f, 0.5f));// PxBoxGeometry uses half-sizes
    }

    public PxRigidDynamic createBox(PxMaterial defaultMaterial, PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry) {
        return createBox(defaultMaterial, pos, quat, boxGeometry, PhysxSetting.getDefaultDensity());
    }

    /**
     * 物理演算される箱を作る
     * @param defaultMaterial　箱のマテリアル
     * @param pos 箱の位置
     * @param quat 箱の角度
     * @param boxGeometry 箱の大きさ (1/2)
     * @param density 箱の密度
     * @return 箱のオブジェクト
     */
    public PxRigidDynamic createBox(PxMaterial defaultMaterial, PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry, float density) {
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

        PxRigidBodyExt.updateMassAndInertia(box, density);

        defaultShapeFlags.destroy();
        tmpFilterData.destroy();
        tmpPose.destroy();
        boxGeometry.destroy();
        pos.destroy();

        this.actor = box;
        return box;
    }

    /**
     * 箱の座標空間を取得する
     * @return 箱の座標空間
     */
    public PxTransform getPos() {
        return actor.getGlobalPose();
    }

    /**
     * 箱の座標空間をセットする
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
        actor.release();
        boxShape.release();
    }

    /**
     * 箱に力を加える
     * @param vec3 箱に加える力
     */
    public void addForce(PxVec3 vec3) {
        actor.addForce(vec3, PxForceModeEnum.eFORCE);
        vec3.destroy();
    }
}
