package com.kamesuta.physxmc;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PhysxMc extends JavaPlugin implements Listener {

    public static Physx physx;
    public static PhysxWorld physxWorld;
    public static RigidItemDisplay rigidBlockDisplay;

    @Override
    public void onEnable() {
        try {
            PhysxLoader.loadPhysxOnAppClassloader();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        getServer().getPluginManager().registerEvents(new PhysxCommand(), this);

        physx = new Physx();
        physxWorld = new PhysxWorld();
        physxWorld.setUpScene();

        new BukkitRunnable() {
            @Override
            public void run() {
                physxWorld.tick();
                rigidBlockDisplay.update();
            }
        }.runTaskTimer(this, 1, 1);

        rigidBlockDisplay = new RigidItemDisplay();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            rigidBlockDisplay.create(event.getPlayer());
        }
    }

    @Override
    public void onDisable() {
        if (rigidBlockDisplay != null) {
            rigidBlockDisplay.destroyAll();
        }

        if (physx != null) {
            physxWorld.destroyScene();
            physx.terminate();
        }
    }
}
