package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxMaterial;
import physx.physics.PxPhysics;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;

import java.util.HashMap;
import java.util.Map;

public class Physx {

    //PhysX's library version
    private static final int version = PxTopLevelFunctions.getPHYSICS_VERSION();

    //PhysX's foundation object
    private static final PxDefaultAllocator allocator;
    private static final PxDefaultErrorCallback errorCb;
    private static final PxFoundation foundation;

    //PhysX main physics object
    private static final PxTolerancesScale tolerances;
    private static final PxPhysics physics;

    //the CPU dispatcher, can be shared among multiple scenes
    private static final PxDefaultCpuDispatcher cpuDispatcher;

    //create default material
    private static final PxMaterial defaultMaterial;

    static {
        allocator = new PxDefaultAllocator();
        errorCb = new PxDefaultErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);
        tolerances = new PxTolerancesScale();
        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);
        cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(4);
        defaultMaterial = physics.createMaterial(0.5f, 0.5f, 0.5f);
    }

    private PxScene scene;
    private final Map<Chunk, PhysxGround> chunkGroundMap = new HashMap<>();//チャンクごとに地形を生成して管理

    public void setUpScene() {
        scene = createScene();

        Chunk[] overWorldChunks = Bukkit.getWorlds().get(0).getLoadedChunks();
        for (Chunk overWorldChunk : overWorldChunks) {
            PhysxGround ground = new PhysxGround(physics);
            scene.addActor(ground.createGround(defaultMaterial, overWorldChunk));
            chunkGroundMap.put(overWorldChunk, ground);
        }
    }

    public void destroyScene() {
        if (scene != null) {
            chunkGroundMap.forEach((chunk, physxGround) -> {
                scene.removeActor(physxGround.getActor());
                physxGround.release();
            });
            chunkGroundMap.clear();

            scene.release();
        }
    }

    public PhysxBox addBox(PxVec3 pos, PxQuat quat) {
        PhysxBox box = new PhysxBox(physics);
        scene.addActor(box.createBox(defaultMaterial, pos, quat));
        return box;
    }

    public PhysxBox addBox(PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry) {
        PhysxBox box = new PhysxBox(physics);
        scene.addActor(box.createBox(defaultMaterial, pos, quat, boxGeometry));
        return box;
    }

    public void removeBox(PhysxBox box) {
        scene.removeActor(box.getActor());
        box.release();
    }

    public void tick() {
        scene.simulate(3f / 60f); // 1 second = 60 frame = 20tick
        scene.fetchResults(true);
    }

    public PxScene createScene() {
        // create a physics scene
        PxVec3 tmpVec = new PxVec3(0f, -19.62f, 0f);
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(tmpVec);
        sceneDesc.setCpuDispatcher(cpuDispatcher);
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        PxScene scene = physics.createScene(sceneDesc);

        tmpVec.destroy();
        sceneDesc.destroy();

        return scene;
    }

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
