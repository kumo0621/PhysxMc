package com.kamesuta.physxmc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

public class GrabTool {

    private final Map<Player, DisplayedPhysxBox> grabbedPlayerMap = new HashMap<>();
    private final Map<Player, Quaternionf> originalRotationMap = new HashMap<>();
    
    public GrabTool(){
        
    }

    public boolean tryGrab(Player player){
        DisplayedPhysxBox box = displayedBoxHolder.raycast(player.getEyeLocation(), 10);
        if(box == null || grabbedPlayerMap.containsValue(box)){
            return false;
        }
        
        grabbedPlayerMap.put(player, box);
        
        Quaternionf boxQuat = box.getQuat();
        Quaternionf playerQuat = new Quaternionf();
        Location eyeLocation = player.getEyeLocation().clone();
        playerQuat.rotateY((float) Math.toRadians(eyeLocation.getYaw()));
        playerQuat.rotateX((float) Math.toRadians(eyeLocation.getPitch()));
        
        playerQuat.mul(boxQuat);
        
        originalRotationMap.put(player, playerQuat);
        box.makeKinematic(true);
        return true;
    }

    public void release(Player player){
        if(!isGrabbing(player))
            return;
        
        if(grabbedPlayerMap.get(player) != null)
            grabbedPlayerMap.get(player).makeKinematic(false);
        grabbedPlayerMap.remove(player);
        originalRotationMap.remove(player);
    }
    
    public boolean isGrabbing(Player player){
        return grabbedPlayerMap.containsKey(player);
    }
    
    public void update(){
        for (Map.Entry<Player, DisplayedPhysxBox> entry : grabbedPlayerMap.entrySet()){
            if(entry.getValue() == null || !originalRotationMap.containsKey(entry.getKey()))
                return;
            
            Location eyeLocation = entry.getKey().getEyeLocation().clone();
            Vector playerDir = eyeLocation.getDirection().clone().normalize().multiply(3);
            eyeLocation.add(playerDir);
            
            //元のブロックの回転を追加でかける
            Quaternionf quat = new Quaternionf();
            quat.rotateY((float) -Math.toRadians(eyeLocation.getYaw()));
            quat.rotateX((float) Math.toRadians(eyeLocation.getPitch()));
            
            Quaternionf originalRotation = originalRotationMap.get(entry.getKey());
            quat.mul(originalRotation);
            
            entry.getValue().moveKinematic(eyeLocation.toVector(), quat);
        }
    }
    
    public void forceClear(){
        grabbedPlayerMap.clear();
        originalRotationMap.clear();
    }
}
