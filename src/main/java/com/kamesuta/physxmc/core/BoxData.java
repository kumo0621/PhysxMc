package com.kamesuta.physxmc.core;

import com.kamesuta.physxmc.PhysxSetting;
import lombok.Data;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;

import java.util.Map;

/**
 * PhysxBoxを作る時の引数データ
 */
@Data
public class BoxData{

    /**
     * 箱の位置
     */
    private final PxVec3 pos;

    /**
     * 箱の角度
     */
    private final PxQuat quat;

    /**
     * 箱内で定義された形状の大きさ (1/2)とそれぞれの箱本体に対するオフセット
     */
    private final Map<PxBoxGeometry, PxVec3> boxGeometries;

    /**
     * トリガー(当たり判定検出用の箱)であるかどうか
     */
    private final boolean isTrigger;

    /**
     * 箱の密度
     */
    private final float density;

    public BoxData(PxVec3 pos, PxQuat quat) {
        this(pos, quat, new PxBoxGeometry(0.5f, 0.5f, 0.5f));// PxBoxGeometry uses half-sizes
    }

    public BoxData(PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry) {
        this(pos, quat, Map.of(boxGeometry, new PxVec3()));
    }

    public BoxData(PxVec3 pos, PxQuat quat, Map<PxBoxGeometry, PxVec3> boxGeometries) {
        this(pos, quat, boxGeometries, false);
    }

    public BoxData(PxVec3 pos, PxQuat quat, Map<PxBoxGeometry, PxVec3> boxGeometries, boolean isTrigger) {
        this(pos, quat, boxGeometries, isTrigger, PhysxSetting.getDefaultDensity());
    }

    public BoxData(PxVec3 pos, PxQuat quat, Map<PxBoxGeometry, PxVec3> boxGeometries, boolean isTrigger, float density) {
        this.pos = pos;
        this.quat = quat;
        this.boxGeometries = boxGeometries;
        this.isTrigger = isTrigger;
        this.density = density;
    }
}
