package com.kamesuta.physxmc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PhysxMc extends JavaPlugin{

    public static Physx physx;
    public static IntegratedPhysxWorld physxWorld;
    public static DisplayedBoxHolder displayedBoxHolder;
    public ProtocolManager protocolManager;

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
        displayedBoxHolder = new DisplayedBoxHolder();

        new BukkitRunnable() {
            @Override
            public void run() {
                physxWorld.tick();
                displayedBoxHolder.update();
            }
        }.runTaskTimer(this, 1, 1);
        
        initProtocolLib();
    }
    
    private void initProtocolLib(){
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                physxWorld.registerChunksToReloadNextSecond(event.getPlayer().getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getChunk());
            }
        });
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
