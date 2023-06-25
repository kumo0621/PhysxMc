package com.kamesuta.physxmc;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PhysxMc extends JavaPlugin{

    public static Physx physx;
    public static IntegratedPhysxWorld physxWorld;
    public static DisplayedBoxHolder displayedBoxHolder;

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
                displayedBoxHolder.update();
            }
        }.runTaskTimer(this, 1, 1);

        displayedBoxHolder = new DisplayedBoxHolder();
    }

    @Override
    public void onDisable() {
        if (displayedBoxHolder != null) {
            displayedBoxHolder.destroyAll();
        }

        if (physx != null) {
            physxWorld.destroyScene();
            physx.terminate();
        }
    }
}
