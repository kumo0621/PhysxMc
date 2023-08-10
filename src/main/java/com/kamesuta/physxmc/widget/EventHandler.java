package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

/**
 * Bukkitのイベントを受信するクラス
 */
public class EventHandler implements Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !PhysxSetting.isDebugMode())
            return;

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                DisplayedPhysxBox box = displayedBoxHolder.debugCreate(event.getPlayer());
                if (box != null) {
                    box.throwBox(event.getPlayer().getEyeLocation());
                    return;
                }
            }
        }
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            if (PhysxMc.grabTool.isGrabbing(event.getPlayer())) {
                event.setCancelled(true);
                PhysxMc.grabTool.release(event.getPlayer());
            } else {
                if (event.getItem() != null && event.getItem().getType() == Material.STICK) {
                    DisplayedPhysxBox box = displayedBoxHolder.raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null) {
                        box.throwBox(event.getPlayer().getEyeLocation());
                    }
                    event.setCancelled(true);
                    return;
                }

                if (event.getItem() != null && event.getItem().getType() == Material.FLINT_AND_STEEL) {
                    DisplayedPhysxBox box = displayedBoxHolder.raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null) {
                        displayedBoxHolder.destroySpecific(box);
                    }
                    event.setCancelled(true);
                    return;
                }

                if (PhysxMc.grabTool.tryGrab(event.getPlayer()))
                    event.setCancelled(true);
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        displayedBoxHolder.executeExplosion(event.getLocation(), 6.9f);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        PhysxMc.grabTool.release(event.getPlayer());
    }
}
