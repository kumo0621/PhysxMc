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
import com.kamesuta.physxmc.widget.MedalPayoutSystem;
import com.kamesuta.physxmc.widget.PhysicsObjectManager;
import com.kamesuta.physxmc.widget.PusherManager;
import com.kamesuta.physxmc.widget.RampManager;
import com.kamesuta.physxmc.wrapper.DisplayedBoxHolder;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import com.kamesuta.physxmc.wrapper.DisplayedSphereHolder;
import com.kamesuta.physxmc.wrapper.IntegratedPhysxWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import physx.physics.PxActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PhysxMc extends JavaPlugin {

    public static Physx physx;
    public static IntegratedPhysxWorld physxWorld;
    public static DisplayedBoxHolder displayedBoxHolder;
    public static DisplayedSphereHolder displayedSphereHolder;
    public static PlayerTriggerHolder playerTriggerHolder;
    public static PusherManager pusherManager;
    public static PhysicsObjectManager physicsObjectManager;
    public static RampManager rampManager;
    public static MedalPayoutSystem medalPayoutSystem;

    public static GrabTool grabTool;
    public ProtocolManager protocolManager;

    // 衝突した鉄ブロックに対するレッドストーン信号パルスを管理
    private final Map<Location, BukkitTask> redstonePulseTasks = new java.util.HashMap<>();
    
    // 自動保存のカウンター（5分間隔 = 6000 ticks）
    private int autoSaveCounter = 0;
    private static final int AUTO_SAVE_INTERVAL = 6000; // 5分間隔

    @Override
    public void onEnable() {
        try {
            getLogger().info("PhysxMcプラグインを開始しています...");
            
            // PhysXライブラリの読み込み
            getLogger().info("PhysXライブラリを読み込み中...");
            PhysxLoader.loadPhysxOnAppClassloader();
            getLogger().info("PhysXライブラリの読み込み完了");

            // 各コンポーネントの初期化
            getLogger().info("物理エンジンを初期化中...");
            physx = new Physx();
            physxWorld = new IntegratedPhysxWorld();
            physxWorld.setUpScene();
            
            getLogger().info("マネージャーを初期化中...");
            displayedBoxHolder = new DisplayedBoxHolder();
            displayedSphereHolder = new DisplayedSphereHolder();
            playerTriggerHolder = new PlayerTriggerHolder();
            pusherManager = new PusherManager(getDataFolder());
            physicsObjectManager = new PhysicsObjectManager(getDataFolder());
            rampManager = new RampManager(getDataFolder());
            medalPayoutSystem = new MedalPayoutSystem();
            grabTool = new GrabTool();
            
            getLogger().info("PhysxMcプラグインの初期化完了");
        } catch (Throwable e) {
            getLogger().severe("PhysxMcプラグインの初期化に失敗しました: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PhysxMcプラグインの初期化に失敗", e);
        }
        
        // データを読み込み（5秒後に実行してワールドとPhysXが完全に読み込まれてから）
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    getLogger().info("物理オブジェクトの復元を開始します...");
                    
                    // PhysXシーンが準備できているか確認
                    if (physxWorld == null) {
                        getLogger().warning("PhysXワールドが準備できていません。復元をスキップします。");
                        return;
                    }
                    
                    // 段階的復元を実装（サーバー負荷軽減）
                    new BukkitRunnable() {
                        private int stage = 0;
                        
                        @Override
                        public void run() {
                            try {
                                switch (stage) {
                                    case 0:
                                        getLogger().info("ボックスとスフィアの復元を開始...");
                                        physicsObjectManager.loadAll();
                                        getLogger().info("ボックスとスフィアの復元完了");
                                        break;
                                    case 1:
                                        getLogger().info("プッシャーの復元を開始...");
                                        pusherManager.loadPushers();
                                        getLogger().info("プッシャーの復元完了");
                                        break;
                                    case 2:
                                        getLogger().info("ランプの復元を開始...");
                                        rampManager.loadRamps();
                                        getLogger().info("ランプの復元完了");
                                        
                                        // 復元後の状態をログ出力
                                        getLogger().info("物理オブジェクトの復元が完了しました。");
                                        getLogger().info("復元結果:");
                                        getLogger().info("- ボックス数: " + displayedBoxHolder.getAllBoxes().size());
                                        getLogger().info("- スフィア数: " + (displayedSphereHolder != null ? "実装待ち" : "0"));
                                        getLogger().info("- プッシャー数: " + pusherManager.getPusherCount());
                                        getLogger().info("- ランプ数: " + rampManager.getRampCount());
                                        
                                        this.cancel();
                                        return;
                                }
                                stage++;
                            } catch (Exception e) {
                                getLogger().severe("物理オブジェクトの復元中にエラーが発生しました (ステージ " + stage + "): " + e.getMessage());
                                e.printStackTrace();
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(PhysxMc.this, 0L, 20L); // 1秒間隔で段階的に実行
                    
                } catch (Exception e) {
                    getLogger().severe("物理オブジェクトの復元スケジューラーの開始に失敗しました: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskLater(this, 100L); // 5秒後に実行（より安全な待機時間）

        // コインの黒曜石接触検出を追加
        physxWorld.getSimCallback().contactReceivers.add(this::onCoinContact);

        // 球体と鉄ブロックの接触検出を追加
        physxWorld.getSimCallback().contactReceivers.add(this::onSphereContact);

        new BukkitRunnable() {
            private int tickCounter = 0;
            
            @Override
            public void run() {
                tickCounter++;
                
                // 物理シミュレーション - 毎ティック実行（必須）
                physxWorld.tick();
                
                // 表示更新 - 2ティックに1回実行（パフォーマンス改善）
                if (tickCounter % 2 == 0) {
                    displayedBoxHolder.update();
                    displayedSphereHolder.update();
                    playerTriggerHolder.update();
                    pusherManager.update();
                    rampManager.update();
                    grabTool.update();
                }
                
                // 自動保存処理を無効化（クラッシュ防止のため）
                // autoSaveCounter++;
                // if (autoSaveCounter >= AUTO_SAVE_INTERVAL) {
                //     autoSaveCounter = 0;
                //     // 同期で自動保存を実行（onDisableと同じ安全な処理）
                //     try {
                //         getLogger().info("自動保存を開始...");
                //         if (pusherManager != null) {
                //             pusherManager.savePushers();
                //         }
                //         if (physicsObjectManager != null) {
                //             physicsObjectManager.saveAll();
                //         }
                //         if (rampManager != null) {
                //             rampManager.saveRamps();
                //         }
                //         getLogger().info("自動保存が完了しました");
                //     } catch (Exception e) {
                //         getLogger().severe("自動保存中にエラーが発生しました: " + e.getMessage());
                //         e.printStackTrace();
                //     }
                // }
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
        // 自動保存カウンターをリセットして即座に保存を実行
        autoSaveCounter = AUTO_SAVE_INTERVAL;
        getLogger().info("プラグイン停止中: 最終データ保存を開始...");
        
        // 同期で最終保存を実行（非同期だとプラグイン停止前に完了しない可能性）
        try {
            if (pusherManager != null) {
                pusherManager.savePushers();
            }
            if (physicsObjectManager != null) {
                physicsObjectManager.saveAll();
            }
            if (rampManager != null) {
                rampManager.saveRamps();
            }
            if (medalPayoutSystem != null) {
                medalPayoutSystem.stopAllPayouts();
            }
            getLogger().info("最終データ保存完了");
        } catch (Exception e) {
            getLogger().severe("最終データ保存中にエラーが発生しました: " + e.getMessage());
        }
        
        // 物理シミュレーションを停止してからオブジェクトを削除
        if (physx != null && physxWorld != null) {
            physxWorld.destroyScene(); // 先にシーンを破棄
        }

        // その後でオブジェクトを削除
        if (displayedBoxHolder != null) {
            displayedBoxHolder.destroyAll();
        }
        
        if (displayedSphereHolder != null) {
            displayedSphereHolder.destroyAll();
        }
        
        if (playerTriggerHolder != null) {
            playerTriggerHolder.destroyAll();
        }
            
        if (pusherManager != null) {
            pusherManager.destroyAll();
        }

        if (rampManager != null) {
            rampManager.destroyAll();
        }

        // 最後にPhysXを終了
        if (physx != null) {
            physx.terminate();
        }
        
        getLogger().info("プラグイン停止完了");
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
     * 球体が鉄ブロックに接触した時の処理
     */
    private void onSphereContact(PxActor actor1, PxActor actor2, String event) {
        if (!"TOUCH_FOUND".equals(event)) {
            return;
        }

        // 衝突したオブジェクトのどちらかが球体か確認
        var sphere = displayedSphereHolder.getSphere(actor1);
        if (sphere == null) {
            sphere = displayedSphereHolder.getSphere(actor2);
        }
        if (sphere == null) {
            return; // 球体と無関係
        }

        // 球体の位置と半径を取得
        Location center = sphere.getLocation();
        double radius = sphere.getRadius();

        org.bukkit.World world = center.getWorld();
        if (world == null) {
            return;
        }

        int blockRadius = (int) Math.ceil(radius + 0.5); // ブロック中心判定用

        for (int dx = -blockRadius; dx <= blockRadius; dx++) {
            for (int dy = -blockRadius; dy <= blockRadius; dy++) {
                for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                    int bx = center.getBlockX() + dx;
                    int by = center.getBlockY() + dy;
                    int bz = center.getBlockZ() + dz;

                    org.bukkit.block.Block block = world.getBlockAt(bx, by, bz);
                    if (block.getType() != org.bukkit.Material.IRON_BLOCK) {
                        continue;
                    }

                    // ブロック中心と球体中心の距離で接触を近似判定
                    double distSq = center.clone().add(0.5, 0.5, 0.5).distanceSquared(
                            new org.bukkit.Location(world, bx + 0.5, by + 0.5, bz + 0.5));
                    if (distSq > (radius + 0.5) * (radius + 0.5)) {
                        continue;
                    }

                    triggerRedstonePulse(block);
                }
            }
        }
    }

    /**
     * 指定ブロックをREDSTONE_BLOCKに置き換え、1秒後に元に戻す
     */
    private void triggerRedstonePulse(org.bukkit.block.Block block) {
        Location locKey = block.getLocation();
        // 既にパルス中なら無視
        if (redstonePulseTasks.containsKey(locKey)) {
            return;
        }

        org.bukkit.block.data.BlockData originalData = block.getBlockData();
        // 物理更新を伴って置換して隣接ブロックへ信号を通知
        block.setType(org.bukkit.Material.REDSTONE_BLOCK, true);

        // 1秒（20tick）後に元のブロックへ戻すタスク
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // ブロックがまだREDSTONE_BLOCKなら元に戻す
                if (block.getType() == org.bukkit.Material.REDSTONE_BLOCK) {
                    block.setBlockData(originalData, true);
                }
                redstonePulseTasks.remove(locKey);
            }
        }.runTaskLater(this, 20L);

        redstonePulseTasks.put(locKey, task);
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
