package com.kamesuta.physxmc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import physx.common.PxIDENTITYEnum;
import physx.common.PxQuat;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxActor;

import java.util.HashMap;
import java.util.Map;

/**
 * Physx世界でのプレイヤーの当たり判定を管理する
 */
public class PlayerTriggerHolder {

    private static final Map<Player, PhysxBox> playerCollisionList = new HashMap<>();

    public void update() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location loc = player.getLocation();
            if (playerCollisionList.get(player) == null) {
                playerCollisionList.put(player, createPlayerTriggerBox(loc, player));
            }
            PxTransform tmpPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
            PxVec3 vec3 = new PxVec3((float) loc.x(), (float) loc.y() + 0.9f, (float) loc.z());
            tmpPose.setP(vec3);
            vec3.destroy();
            playerCollisionList.get(player).setPos(tmpPose);
        });
    }
    
    private PhysxBox createPlayerTriggerBox(Location loc, Player player){
        PxVec3 pos = new PxVec3((float) loc.x(), (float) loc.y() + 0.9f, (float) loc.z());
        PxQuat rot = new PxQuat(PxIDENTITYEnum.PxIdentity);
        PxBoxGeometry geometry = new PxBoxGeometry(0.3f, 0.9f, 0.3f);//Steve is 1.8m tall and has 0.6m width
        PhysxBox box = PhysxMc.physxWorld.addBox(pos, rot, geometry, true);
        box.getActor().setName(player.getName());
        return box;
    }

    public void destroyAll() {
        playerCollisionList.forEach((player, physxBox) -> {
            PhysxMc.physxWorld.removeBox(physxBox);
        });
        playerCollisionList.clear();
    }
    
    public Player getPlayer(PxActor actor){
        for (Map.Entry<Player, PhysxBox> entry : playerCollisionList.entrySet()){
            if(entry.getValue().getActor().equals(actor))
                return entry.getKey();
        }
        return null;
    }
}
