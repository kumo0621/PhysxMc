package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import physx.PxTopLevelFunctions;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;

import java.util.HashMap;
import java.util.Map;

import static com.kamesuta.physxmc.Physx.*;

public class PhysxWorld {
    
    private PxScene scene;
    private final Map<Chunk, PhysxTerrain> chunkTerrainMap = new HashMap<>();//チャンクごとに地形を生成して管理

    public void setUpScene() {
        scene = createScene();

        Chunk[] overWorldChunks = Bukkit.getWorlds().get(0).getLoadedChunks();
        for (Chunk overWorldChunk : overWorldChunks) {
            PhysxTerrain terrain = new PhysxTerrain(physics);
            scene.addActor(terrain.createTerrain(defaultMaterial, overWorldChunk));
            chunkTerrainMap.put(overWorldChunk, terrain);
        }
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

    public void destroyScene() {
        if (scene != null) {
            chunkTerrainMap.forEach((chunk, physxTerrain) -> {
                scene.removeActor(physxTerrain.getActor());
                physxTerrain.release();
            });
            chunkTerrainMap.clear();

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
}
