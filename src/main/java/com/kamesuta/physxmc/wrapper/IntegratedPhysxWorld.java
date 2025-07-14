package com.kamesuta.physxmc.wrapper;

import com.kamesuta.physxmc.core.BoxData;
import com.kamesuta.physxmc.core.SphereData;
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
    private final Map<Chunk, IntegratedPhysxTerrain> chunkTerrainMap = new HashMap<>();

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

        IntegratedPhysxTerrain terrain = new IntegratedPhysxTerrain(physics, defaultMaterial, chunk);
        scene.addActor(terrain.getActor());
        chunkTerrainMap.put(chunk, terrain);
    }

    /**
     * チャンクごとに存在する地形を破壊する
     *
     * @param chunk　チャンク
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
     * @param display       表示用のBlockDisplay
     * @return 追加した箱オブジェクト
     */
    public DisplayedPhysxBox addBox(BoxData data, Map<BlockDisplay[], Vector> display) {
        return addBox(data, display, false);
    }

    /**
     * シーンにMinecraft世界で表示可能な箱オブジェクトを追加する（コインフラグ指定可能）
     *
     * @param display       表示用のBlockDisplay
     * @param isCoin        コインかどうか
     * @return 追加した箱オブジェクト
     */
    public DisplayedPhysxBox addBox(BoxData data, Map<BlockDisplay[], Vector> display, boolean isCoin) {
        return addBox(data, display, isCoin, false);
    }

    /**
     * シーンにMinecraft世界で表示可能な箱オブジェクトを追加する（コイン・プッシャーフラグ指定可能）
     *
     * @param display       表示用のBlockDisplay
     * @param isCoin        コインかどうか
     * @param isPusher      プッシャーの一部かどうか
     * @return 追加した箱オブジェクト
     */
    public DisplayedPhysxBox addBox(BoxData data, Map<BlockDisplay[], Vector> display, boolean isCoin, boolean isPusher) {
        try {
            DisplayedPhysxBox box = new DisplayedPhysxBox(physics, data, display, isCoin, isPusher);
            
            if (box == null) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: コンストラクタがnullを返しました");
                return null;
            }
            
            if (box.getActor() == null) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: アクターがnullです");
                return null;
            }
            
            if (scene == null) {
                org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成失敗: PhysXシーンがnullです");
                return null;
            }
            
            scene.addActor(box.getActor());
            org.bukkit.Bukkit.getLogger().info("DisplayedPhysxBoxをシーンに追加しました");
            return box;
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("DisplayedPhysxBox作成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * シーンにMinecraft世界で表示可能な球体オブジェクトを追加する
     *
     * @param data          球体データ
     * @param display       表示用のBlockDisplay
     * @param radius        半径
     * @return 追加した球体オブジェクト
     */
    public DisplayedPhysxSphere addSphere(SphereData data, Map<BlockDisplay[], Vector> display, double radius) {
        DisplayedPhysxSphere sphere = new DisplayedPhysxSphere(physics, data, display, radius);
        scene.addActor(sphere.getActor());
        return sphere;
    }

    /**
     * シーンから箱オブジェクトを削除する
     *
     * @param box 削除する箱オブジェクト
     */
    public void removeBox(DisplayedPhysxBox box) {
        scene.removeActor(box.getActor());
        box.release();
    }

    /**
     * シーンから球体オブジェクトを削除する
     *
     * @param sphere 削除する球体オブジェクト
     */
    public void removeSphere(DisplayedPhysxSphere sphere) {
        scene.removeActor(sphere.getActor());
        sphere.release();
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
        for (Map.Entry<Chunk, IntegratedPhysxTerrain> chunkPhysxTerrainEntry : chunkTerrainMap.entrySet()) {
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

    /**
     * Raycastして球体を検索する
     *
     * @param location 始点
     * @param distance 距離
     * @return 見つかった最初の球体
     */
    public DisplayedPhysxSphere raycastSphere(Location location, float distance) {
        PxRigidActor actor = raycast(location, distance);
        if (actor == null)
            return null;
        
        // 球体のホルダーから該当する球体を検索
        return com.kamesuta.physxmc.PhysxMc.displayedSphereHolder.getSphere(actor);
    }
}
