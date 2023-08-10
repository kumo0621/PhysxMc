package com.kamesuta.physxmc.wrapper;

import com.kamesuta.physxmc.core.PhysxTerrain;
import com.kamesuta.physxmc.core.PhysxWorld;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.*;

import static com.kamesuta.physxmc.core.Physx.defaultMaterial;
import static com.kamesuta.physxmc.core.Physx.physics;

/**
 * マイクラとの連携機能を付け足したPhysxWorld
 */
public class IntegratedPhysxWorld extends PhysxWorld {

    /**
     * チャンクごとに地形を生成して管理
     */
    private final Map<Chunk, PhysxTerrain> chunkTerrainMap = new HashMap<>();

    /**
     * 次のtickで保持しておかなくてはいけない現在アクティブな物理オブジェクトが存在するチャンク
     */
    private final Set<Chunk> chunksToLoadNextTick = new HashSet<>();

    /**
     * 次の秒でリロードしておかなくてはいけない、構成ブロックに変更が加わったチャンク
     */
    private final Set<Chunk> chunksToReloadNextSecond = new HashSet<>();

    private boolean readyToUpdateChunks = false;
    private int tickCount = 0;

    /**
     * チャンクごとに物理エンジンの地形を作る
     *
     * @param chunk
     */
    public void loadChunkAsTerrain(Chunk chunk) {
        if (chunkTerrainMap.containsKey(chunk))
            return;

        PhysxTerrain terrain = new PhysxTerrain(physics, defaultMaterial, chunk);
        scene.addActor(terrain.getActor());
        chunkTerrainMap.put(chunk, terrain);
    }

    /**
     * チャンクごとに存在する地形を破壊する
     *
     * @param chunk
     */
    public void unloadChunkAsTerrain(Chunk chunk, boolean wakeOnLostTouch) {
        if (chunkTerrainMap.get(chunk) == null)
            return;

        scene.removeActor(chunkTerrainMap.get(chunk).getActor(), wakeOnLostTouch);
        chunkTerrainMap.get(chunk).release();

        chunkTerrainMap.remove(chunk);
    }

    /**
     * チャンクが物理地形として読み込まれているか
     */
    public boolean isChunkLoadedAsTerrain(Chunk chunk) {
        return chunkTerrainMap.get(chunk) != null;
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
     *
     * @param pos         座標
     * @param quat        回転
     * @param boxGeometries オブジェクトに含まれるそれぞれの箱の大きさと判定
     * @param display     表示用のBlockDisplay
     * @return 追加した箱オブジェクト
     */
    public DisplayedPhysxBox addBox(PxVec3 pos, PxQuat quat, Map<PxBoxGeometry, PxVec3> boxGeometries, BlockDisplay[] display) {
        DisplayedPhysxBox box = new DisplayedPhysxBox(physics, pos, quat, boxGeometries, display);
        scene.addActor(box.getActor());
        return box;
    }

    /**
     * 次のtickで地形をロードしておきたいチャンクを登録する（これに登録されていないチャンクは次のtickでアンロードされる）
     *
     * @param chunks 地形をロードしておきたいチャンク
     */
    public void registerChunksToLoadNextTick(Collection<Chunk> chunks) {
        chunksToLoadNextTick.addAll(chunks);
    }

    /**
     * 次のtickでロードするチャンクの登録が終わったことを登録する
     */
    public void setReadyToUpdateChunks() {
        readyToUpdateChunks = true;
    }

    /**
     * 次の秒で地形をリロードしておきたい変更の加わったチャンクを登録する
     */
    public void registerChunksToReloadNextSecond(Chunk chunk) {
        chunksToReloadNextSecond.add(chunk);
    }

    @Override
    public void tick() {
        super.tick();
        updateActiveChunks();

        tickCount++;
        if (tickCount % 20 == 0)
            reloadModifiedChunks();
    }

    /**
     * アクティブなオブジェクト付近のロードしておくチャンクをアップデートする。毎tick実行することで高速で動くオブジェクトに対応する
     */
    private void updateActiveChunks() {
        if (!readyToUpdateChunks)
            return;

        if (chunksToLoadNextTick.isEmpty())
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
            unloadChunkAsTerrain(chunk, false);
        }
        chunksToLoadNextTick.clear();
        readyToUpdateChunks = false;
    }

    /**
     * 構成ブロックに変更が加わったチャンクを(まとめて)リロードする
     */
    private void reloadModifiedChunks() {
        for (Chunk chunk : chunksToReloadNextSecond) {
            if (!isChunkLoadedAsTerrain(chunk))
                continue;

            unloadChunkAsTerrain(chunk, true);
            loadChunkAsTerrain(chunk);
        }
        chunksToReloadNextSecond.clear();
    }

    /**
     * Raycastして動的オブジェクトを検索する
     *
     * @param location 始点
     * @param distance 距離
     * @return 見つかった最初の動的オブジェクト
     */
    public PxRigidActor raycast(Location location, float distance) {
        PxVec3 origin = new PxVec3((float) location.x(), (float) location.y(), (float) location.z());
        Vector unitDir = location.getDirection();
        PxVec3 pxUnitDir = new PxVec3((float) unitDir.getX(), (float) unitDir.getY(), (float) unitDir.getZ());
        PxRaycastBuffer10 raycastHit = new PxRaycastBuffer10();
        PxHitFlags hitFlags = new PxHitFlags((short) PxHitFlagEnum.eDEFAULT.value);
        PxQueryFilterData filterData = new PxQueryFilterData(new PxQueryFlags((short) PxQueryFlagEnum.eDYNAMIC.value));
        boolean isHit = scene.raycast(origin, pxUnitDir, distance, raycastHit, hitFlags, filterData);
        origin.destroy();
        pxUnitDir.destroy();
        if (!isHit)
            return null;
        return raycastHit.getAnyHit(0).getActor();
    }
}
