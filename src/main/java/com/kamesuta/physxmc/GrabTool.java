package com.kamesuta.physxmc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

public class GrabTool {

    private final Map<Player, DisplayedPhysxBox> grabbedPlayerMap = new HashMap<>();
    
    public GrabTool(){
        
    }

    public boolean tryGrab(Player player){
        DisplayedPhysxBox box = displayedBoxHolder.raycast(player.getEyeLocation(), 10);
        if(box == null || grabbedPlayerMap.containsValue(box)){
            return false;
        }
        
        grabbedPlayerMap.put(player, box);
        box.makeKinematic(true);
        return true;
    }

    public void release(Player player){
        if(!isGrabbing(player))
            return;
        
        if(grabbedPlayerMap.get(player) != null)
            grabbedPlayerMap.get(player).makeKinematic(false);
        grabbedPlayerMap.remove(player);
    }
    
    public boolean isGrabbing(Player player){
        return grabbedPlayerMap.containsKey(player);
    }
    
    public void update(){
        for (Map.Entry<Player, DisplayedPhysxBox> entry : grabbedPlayerMap.entrySet()){
            if(entry.getValue() == null)
                return;
            
            Location eyeLocation = entry.getKey().getEyeLocation().clone();
            Vector playerDir = eyeLocation.getDirection().clone().normalize().multiply(3);
            eyeLocation.add(playerDir);
            
            entry.getValue().moveKinematic(eyeLocation);
        }
    }
    
    public void forceClear(){
        grabbedPlayerMap.clear();
    }
}
