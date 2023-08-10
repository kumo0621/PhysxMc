package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.core.PhysxBox;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.apache.logging.log4j.util.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxActor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Physx世界でのプレイヤーの当たり判定を管理する
 */
public class PlayerTriggerHolder {

    /**
     * プレイヤーと当たり判定のマップ
     */
    private static final Map<Player, PhysxBox> playerCollisionList = new HashMap<>();

    /**
     * プレイヤーのトリガーが箱に接触した時呼ばれるイベント
     */
    public List<BiConsumer<Player, DisplayedPhysxBox>> playerTriggerReceivers = new ArrayList<>();

    public PlayerTriggerHolder() {
        PhysxMc.physxWorld.getSimCallback().triggerReceivers.add(this::onPlayerEnterBox);
    }

    public void update() {
        updatePlayerTriggerBox();
        removeOfflinePlayerTriggerBox();
    }

    /**
     * プレイヤーの位置に基づいて当たり判定の座標をアップデートする
     */
    private void updatePlayerTriggerBox(){
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location loc = player.getLocation();
            if (playerCollisionList.get(player) == null) {
                playerCollisionList.put(player, createPlayerTriggerBox(loc, player));
            }
            PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
            PxVec3 vec3 = new PxVec3((float) loc.x(), (float) loc.y() + 0.9f, (float) loc.z());//steve is 1.8m tall
            tmpPose.setP(vec3);
            vec3.destroy();
            playerCollisionList.get(player).setPos(tmpPose);
        });
    }

    /**
     * オフラインのプレイヤーの当たり判定を削除する
     */
    private void removeOfflinePlayerTriggerBox(){
        playerCollisionList.forEach((player, physxBox) -> {
            if (!player.isOnline() && physxBox != null) {
                PhysxMc.physxWorld.removeBox(physxBox);
                playerCollisionList.put(player, null);
            }
        });
    }

    /**
     * プレイヤーの当たり判定を新たに作成する
     * @param loc 座標
     * @param player プレイヤー
     * @return 作った当たり判定
     */
    private PhysxBox createPlayerTriggerBox(Location loc, Player player) {
        PxVec3 pos = new PxVec3((float) loc.x(), (float) loc.y() + 0.9f, (float) loc.z());
        PxQuat rot = new PxQuat(PxIDENTITYEnum.PxIdentity);
        PxBoxGeometry geometry = new PxBoxGeometry(0.3f, 0.9f, 0.3f);//Steve is 1.8m tall and has 0.6m width
        PhysxBox box = PhysxMc.physxWorld.addBox(pos, rot, Map.of(geometry, new PxVec3()), true);
        box.getActor().setName(player.getName());
        return box;
    }

    /**
     * 全ての当たり判定を削除
     */
    public void destroyAll() {
        playerCollisionList.forEach((player, physxBox) -> {
            if(physxBox != null)
                PhysxMc.physxWorld.removeBox(physxBox);
        });
        playerCollisionList.clear();
    }

    /**
     * 当たり判定本体からプレイヤーを取得
     * @param actor
     * @return
     */
    public Player getPlayer(PxActor actor) {
        for (Map.Entry<Player, PhysxBox> entry : playerCollisionList.entrySet()) {
            if (entry.getValue() == null)
                continue;
            if (entry.getValue().getActor().equals(actor))
                return entry.getKey();
        }
        return null;
    }

    /**
     * プレイヤーの当たり判定と他オブジェクトが接触したイベントを受信し、扱いやすい型に変換してイベントを発行する
     * @param actor1 オブジェクト
     * @param actor2 オブジェクト
     * @param event イベントの種類
     */
    public void onPlayerEnterBox(PxActor actor1, PxActor actor2, String event) {
        if (!event.equals("TRIGGER_ENTER"))
            return;

        Player player;
        DisplayedPhysxBox box;

        player = getPlayer(actor1);
        if (player != null) {
            box = PhysxMc.displayedBoxHolder.getBox(actor2);
            if (box != null) {
                Player finalPlayer = player;
                DisplayedPhysxBox finalBox = box;
                playerTriggerReceivers.forEach(playerDisplayedPhysxBoxBiConsumer -> playerDisplayedPhysxBoxBiConsumer.accept(finalPlayer, finalBox));
            }
        }

        player = getPlayer(actor2);
        if (player != null) {
            box = PhysxMc.displayedBoxHolder.getBox(actor1);
            if (box != null) {
                Player finalPlayer = player;
                DisplayedPhysxBox finalBox = box;
                playerTriggerReceivers.forEach(playerDisplayedPhysxBoxBiConsumer -> playerDisplayedPhysxBoxBiConsumer.accept(finalPlayer, finalBox));
            }
        }
    }
}
