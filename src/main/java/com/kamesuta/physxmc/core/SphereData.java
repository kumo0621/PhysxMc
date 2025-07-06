package com.kamesuta.physxmc.core;

import com.kamesuta.physxmc.PhysxSetting;
import lombok.Data;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxSphereGeometry;

import java.util.Map;

/**
 * PhysxSphereを作る時の引数データ
 */
@Data
public class SphereData {

    /**
     * 球体の位置
     */
    private final PxVec3 pos;

    /**
     * 球体の角度
     */
    private final PxQuat quat;

    /**
     * 球体内で定義された形状の大きさとそれぞれの球体本体に対するオフセット
     */
    private final Map<PxSphereGeometry, PxVec3> sphereGeometries;

    /**
     * トリガー(当たり判定検出用の球体)であるかどうか
     */
    private final boolean isTrigger;

    /**
     * 球体の密度
     */
    private final float density;

    public SphereData(PxVec3 pos, PxQuat quat) {
        this(pos, quat, new PxSphereGeometry(0.5f));
    }

    public SphereData(PxVec3 pos, PxQuat quat, PxSphereGeometry sphereGeometry) {
        this(pos, quat, Map.of(sphereGeometry, new PxVec3()));
    }

    public SphereData(PxVec3 pos, PxQuat quat, Map<PxSphereGeometry, PxVec3> sphereGeometries) {
        this(pos, quat, sphereGeometries, false);
    }

    public SphereData(PxVec3 pos, PxQuat quat, Map<PxSphereGeometry, PxVec3> sphereGeometries, boolean isTrigger) {
        this(pos, quat, sphereGeometries, isTrigger, PhysxSetting.getDefaultDensity());
    }

    public SphereData(PxVec3 pos, PxQuat quat, Map<PxSphereGeometry, PxVec3> sphereGeometries, boolean isTrigger, float density) {
        this.pos = pos;
        this.quat = quat;
        this.sphereGeometries = sphereGeometries;
        this.isTrigger = isTrigger;
        this.density = density;
    }
} 