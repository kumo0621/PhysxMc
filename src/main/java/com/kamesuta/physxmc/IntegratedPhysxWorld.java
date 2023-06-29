package com.kamesuta.physxmc;

import org.bukkit.Chunk;
import org.bukkit.entity.ItemDisplay;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;

import java.util.*;

import static com.kamesuta.physxmc.Physx.defaultMaterial;
import static com.kamesuta.physxmc.Physx.physics;

/**
 * マイクラとの連携機能を付け足したPhysxWorld
 */
public class IntegratedPhysxWorld extends PhysxWorld {

    private final Map<Chunk, PhysxTerrain> chunkTerrainMap = new HashMap<>();//チャンクごとに地形を生成して管理
    private final List<Chunk> chunksToLoadNextTick = new ArrayList<>();//現在アクティブな物理オブジェクトが存在するチャンク
    
    private boolean readyToUpdateChunks = false;

    /**
     * チャンクごとに物理エンジンの地形を作る
     * @param chunk
     */
    public void loadChunkAsTerrain(Chunk chunk){
        if(chunkTerrainMap.containsKey(chunk))
            return;

        PhysxTerrain terrain = new PhysxTerrain(physics, defaultMaterial, chunk);
        scene.addActor(terrain.getActor());
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
    public DisplayedPhysxBox addBox(PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry, ItemDisplay[] display) {
        DisplayedPhysxBox box = new DisplayedPhysxBox(physics, pos, quat, boxGeometry, display);
        scene.addActor(box.getActor());
        return box;
    }

    /**
     * 次のtickで地形をロードしておきたいチャンクを登録する
     * @param chunks 地形をロードしておきたいチャンク
     */
    public void registerChunksToLoadNextTick(Collection<Chunk> chunks){
        chunksToLoadNextTick.addAll(chunks);
    }

    /**
     * 次のtickでロードするチャンクの登録が終わったことを登録する
     */
    public void setReadyToUpdateChunks(){
        readyToUpdateChunks = true;
    }

    @Override
    public void tick() {
        super.tick();
        updateChunks();
    }

    /**
     * ロードするチャンクをアップデートする
     */
    private void updateChunks(){
        if(!readyToUpdateChunks)
            return;
        
        if(chunksToLoadNextTick.isEmpty())
            return;
        
        for (Chunk chunk : chunksToLoadNextTick) {
            loadChunkAsTerrain(chunk);
        }
        Collection<Chunk> chunksToUnload = new ArrayList<>();
        for (Map.Entry<Chunk, PhysxTerrain> chunkPhysxTerrainEntry : chunkTerrainMap.entrySet()) {
            Chunk chunk = chunkPhysxTerrainEntry.getKey();
            if (!chunksToLoadNextTick.contains(chunk))
                chunksToUnload.add(chunk);
        }
        for (Chunk chunk : chunksToUnload) {
            unloadChunkAsTerrain(chunk);
        }
        chunksToLoadNextTick.clear();
        readyToUpdateChunks = false;
    }
}
