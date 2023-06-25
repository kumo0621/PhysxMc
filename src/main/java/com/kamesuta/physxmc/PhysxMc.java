package com.kamesuta.physxmc;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PhysxMc extends JavaPlugin{

    public static Physx physx;
    public static IntegratedPhysxWorld physxWorld;
    public static RigidItemDisplay rigidBlockDisplay;

    @Override
    public void onEnable() {
        try {
            PhysxLoader.loadPhysxOnAppClassloader();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        getServer().getPluginManager().registerEvents(new PhysxCommand(), this);
        getServer().getPluginManager().registerEvents(new EventHandler(), this);

        physx = new Physx();
        physxWorld = new IntegratedPhysxWorld();
        physxWorld.setUpScene();

        new BukkitRunnable() {
            @Override
            public void run() {
                physxWorld.tick();
                rigidBlockDisplay.update();
            }
        }.runTaskTimer(this, 1, 1);

        rigidBlockDisplay = new RigidItemDisplay();
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
