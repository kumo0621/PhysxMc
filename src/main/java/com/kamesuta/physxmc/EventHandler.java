package com.kamesuta.physxmc;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import static com.kamesuta.physxmc.PhysxMc.physxWorld;
import static com.kamesuta.physxmc.PhysxMc.rigidBlockDisplay;

public class EventHandler implements Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && PhysxSetting.isDebugMode()) {
            rigidBlockDisplay.debugCreate(event.getPlayer());
        }
    }

    @org.bukkit.event.EventHandler
    public void onChunkLoad(ChunkLoadEvent event){
        physxWorld.loadChunkAsTerrain(event.getChunk());
    }

    @org.bukkit.event.EventHandler
    public void onChunkUnload(ChunkUnloadEvent event){
        physxWorld.unloadChunkAsTerrain(event.getChunk());
    }
}
