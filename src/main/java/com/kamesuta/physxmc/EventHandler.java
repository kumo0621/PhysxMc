package com.kamesuta.physxmc;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

public class EventHandler implements Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && PhysxSetting.isDebugMode()) {
            displayedBoxHolder.debugCreate(event.getPlayer());
        }
    }
}
