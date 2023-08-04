package com.kamesuta.physxmc;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

public class EventHandler implements Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && PhysxSetting.isDebugMode()) {
            DisplayedPhysxBox box = displayedBoxHolder.debugCreate(event.getPlayer());
            if (box == null)
                return;
            box.throwBox(event.getPlayer().getEyeLocation());
        }
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) && PhysxSetting.isDebugMode()) {
            DisplayedPhysxBox box = displayedBoxHolder.raycast(event.getPlayer().getEyeLocation(), 4);
            if (box == null)
                return;
            box.throwBox(event.getPlayer().getEyeLocation());
        }
    }

    @org.bukkit.event.EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        displayedBoxHolder.executeExplosion(event.getLocation(), 6.9f);
    }
}
