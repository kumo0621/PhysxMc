package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.ItemDisplay;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;

import java.util.HashMap;
import java.util.Map;

import static com.kamesuta.physxmc.Physx.defaultMaterial;
import static com.kamesuta.physxmc.Physx.physics;

/**
 * マイクラとの連携機能を付け足したPhysxWorld
 */
public class IntegratedPhysxWorld extends PhysxWorld {

    private final Map<Chunk, PhysxTerrain> chunkTerrainMap = new HashMap<>();//チャンクごとに地形を生成して管理

    /**
     * チャンクごとに地形を作ってシーンに挿入する
     */
    @Override
    public void setUpScene() {
        scene = createScene();

        Chunk[] overWorldChunks = Bukkit.getWorlds().get(0).getLoadedChunks();
        for (Chunk overWorldChunk : overWorldChunks) {
            loadChunkAsTerrain(overWorldChunk);
        }
    }

    /**
     * チャンクごとに物理エンジンの地形を作る
     * @param chunk
     */
    public void loadChunkAsTerrain(Chunk chunk){
        if(chunkTerrainMap.containsKey(chunk))
            return;

        PhysxTerrain terrain = new PhysxTerrain(physics);
        scene.addActor(terrain.createTerrain(defaultMaterial, chunk));
        chunkTerrainMap.put(chunk, terrain);
    }

    /**
     * チャンクごとに存在する地形を破壊する
     * @param chunk
     */
    public void unloadChunkAsTerrain(Chunk chunk){
        if(chunkTerrainMap.get(chunk) == null)
            return;

        scene.removeActor(chunkTerrainMap.get(chunk).getActor());
        chunkTerrainMap.get(chunk).release();

        chunkTerrainMap.remove(chunk);
    }

    /**
     * シーンオブジェクトを破壊する
     */
    @Override
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

    /**
     * シーンにMinecraft世界で表示可能な箱オブジェクトを追加する
     * @param pos 座標
     * @param quat 回転
     * @param boxGeometry 箱の大きさ
     * @param display 表示用のItemDisplay
     * @return 追加した箱オブジェクト
     */
    public DisplayedPhysxBox addBox(PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry, ItemDisplay display) {
        DisplayedPhysxBox box = new DisplayedPhysxBox(physics, pos, quat, boxGeometry, display);
        scene.addActor(box.getActor());
        return box;
    }
}
