package com.kamesuta.physxmc.core;

import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.physics.PxMaterial;
import physx.physics.PxPhysics;

/**
 * Physxのコアコンポーネントのホルダークラス。
 */
public class Physx {

    //PhysX's library version
    private static final int version = PxTopLevelFunctions.getPHYSICS_VERSION();

    //PhysX's foundation object
    private static final PxDefaultAllocator allocator;
    private static final PxDefaultErrorCallback errorCb;
    private static final PxFoundation foundation;

    //PhysX main physics object
    public static final PxTolerancesScale tolerances;
    public static final PxPhysics physics;

    //the CPU dispatcher, can be shared among multiple scenes
    public static final PxDefaultCpuDispatcher cpuDispatcher;

    //create default material
    public static final PxMaterial defaultMaterial;

    /**
     * コアコンポーネントを初期化する
     */
    static {
        allocator = new PxDefaultAllocator();
        errorCb = new PxDefaultErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);
        tolerances = new PxTolerancesScale();
        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);
        cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(4);
        defaultMaterial = physics.createMaterial(0.5f, 0.5f, 0.5f);
    }

    /**
     * プラグイン終了時にコアコンポーネントを破壊してメモリリークを防ぐ
     */
    public void terminate() {
        if (defaultMaterial != null) {
            defaultMaterial.release();
            tolerances.destroy();

            physics.release();

            foundation.release();
            errorCb.destroy();
            allocator.destroy();
        }
    }
}
