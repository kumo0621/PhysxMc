package com.kamesuta.physxmc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.kamesuta.physxmc.command.PhysxCommand;
import com.kamesuta.physxmc.core.Physx;
import com.kamesuta.physxmc.core.PhysxTerrain;
import com.kamesuta.physxmc.utils.BoundingBoxUtil;
import com.kamesuta.physxmc.utils.ConversionUtility;
import com.kamesuta.physxmc.utils.PhysxLoader;
import com.kamesuta.physxmc.widget.EventHandler;
import com.kamesuta.physxmc.widget.GrabTool;
import com.kamesuta.physxmc.widget.PlayerTriggerHolder;
import com.kamesuta.physxmc.widget.PusherManager;
import com.kamesuta.physxmc.wrapper.DisplayedBoxHolder;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import com.kamesuta.physxmc.wrapper.IntegratedPhysxWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import physx.physics.PxActor;

import java.util.ArrayList;
import java.util.List;

public final class PhysxMc extends JavaPlugin {

    public static Physx physx;
    public static IntegratedPhysxWorld physxWorld;
    public static DisplayedBoxHolder displayedBoxHolder;
    public static PlayerTriggerHolder playerTriggerHolder;
    public static PusherManager pusherManager;

    public static GrabTool grabTool;
    public ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        try {
            PhysxLoader.loadPhysxOnAppClassloader();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        physx = new Physx();
        physxWorld = new IntegratedPhysxWorld();
        physxWorld.setUpScene();
        displayedBoxHolder = new DisplayedBoxHolder();
        playerTriggerHolder = new PlayerTriggerHolder();
        pusherManager = new PusherManager();
        grabTool = new GrabTool();

        // コインの黒曜石接触検出を追加
        physxWorld.getSimCallback().contactReceivers.add(this::onCoinContact);

        new BukkitRunnable() {
            @Override
            public void run() {
                physxWorld.tick();
                displayedBoxHolder.update();
                playerTriggerHolder.update();
                pusherManager.update();
                grabTool.update();
            }
        }.runTaskTimer(this, 1, 1);

        getServer().getPluginManager().registerEvents(new PhysxCommand(), this);
        getServer().getPluginManager().registerEvents(new EventHandler(), this);

        initProtocolLib();
        try {
            BoundingBoxUtil.init();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        forceInit(PhysxTerrain.class);
    }

    private void initProtocolLib() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                BlockPosition pos = packet.getBlockPositionModifier().read(0);
                physxWorld.registerChunksToReloadNextSecond(event.getPlayer().getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getChunk());
            }
        });
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                List<Location> locations = new ArrayList<>();

                var sectionPos = packet.getSectionPositions().read(0);
                var shortLocations = packet.getShortArrays().read(0);

                for (short shortLocation : shortLocations) {
                    var loc = ConversionUtility.convertShortLocation(event.getPlayer().getWorld(), sectionPos, shortLocation);
                    locations.add(loc);
                }

                for (Location location : locations) {
                    physxWorld.registerChunksToReloadNextSecond(location.getChunk());
                }
            }
        });
    }

    @Override
    public void onDisable() {
        if (displayedBoxHolder != null) {
            displayedBoxHolder.destroyAll();
            playerTriggerHolder.destroyAll();
            pusherManager.destroyAll();
        }

        if (physx != null) {
            physxWorld.destroyScene();
            physx.terminate();
        }
    }

    /**
     * コインと地形の接触を検出する
     */
    private void onCoinContact(PxActor actor1, PxActor actor2, String event) {
        if (!"TOUCH_FOUND".equals(event)) {
            return;
        }

        // 動的オブジェクト（コイン）を特定
        DisplayedPhysxBox coin = displayedBoxHolder.getBox(actor1);
        if (coin == null) {
            coin = displayedBoxHolder.getBox(actor2);
        }

        // コインかどうかチェック
        if (coin == null || !coin.isCoin()) {
            return;
        }

        // コインの位置を取得
        Location coinLocation = coin.getLocation();
        
        // コインの下3マスまでブロックを確認
        boolean foundObsidian = false;
        for (int i = 1; i <= 3; i++) {
            Location belowLocation = coinLocation.clone().add(0, -i, 0);
            Material blockBelow = belowLocation.getBlock().getType();
            
            if (blockBelow == Material.OBSIDIAN) {
                foundObsidian = true;
                break;
            }
        }

        // 黒曜石が見つかった場合、コインをアイテム化
        if (foundObsidian) {
            // コインをアイテムとしてドロップ
            ItemStack droppedCoin = new ItemStack(Material.IRON_TRAPDOOR, 1);
            coinLocation.getWorld().dropItemNaturally(coinLocation, droppedCoin);
            
            // 物理オブジェクトを削除
            displayedBoxHolder.destroySpecific(coin);
            
            getLogger().info("コインが黒曜石に接触してアイテム化されました");
        }
    }

    /**
     * BukkitのOnDisableでエラーが出ないようにクラスを強制的にロードする
     */
    public static <T> Class<T> forceInit(Class<T> klass) {
        try {
            Class.forName(klass.getName(), true, klass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
        return klass;
    }
}
