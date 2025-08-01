package com.kamesuta.physxmc.command;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.widget.MedalPusher;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ライブラリのコマンドを仮実装するクラス。CommandLib(<a href="https://github.com/TeamKun/CommandLib">...</a>)が1.20に対応し次第移行する
 */
public class PhysxCommand extends CommandBase implements Listener {

    private static final String commandName = "physxmc";
    private static final String resetArgument = "reset";
    private static final String debugArgument = "debugmode";
    private static final String densityArgument = "density";
    private static final String updateArgument = "updatecurrentchunk";
    private static final String summonArgument = "summon";

    private static final String gravityArgument = "gravity";
    private static final String coinArgument = "coin";
    private static final String pusherArgument = "pusher";
    private static final String ballArgument = "ball";
    private static final String rampArgument = "ramp";
    private static final String saveArgument = "save";

    /**
     * 引数のリスト
     */
    private static final List<String> arguments = List.of(resetArgument, debugArgument, densityArgument, updateArgument, summonArgument, gravityArgument, coinArgument, pusherArgument, ballArgument, rampArgument, saveArgument);

    public PhysxCommand() {
        super(commandName, 1, 8, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, @Nullable String[] arguments) {
        if (arguments[0].equals(resetArgument)) {
            PhysxMc.grabTool.forceClear();
            PhysxMc.displayedBoxHolder.destroyAll();
            return true;
        } else if (arguments[0].equals(debugArgument)) {
            PhysxSetting.setDebugMode(!PhysxSetting.isDebugMode());
            sender.sendMessage("デバッグモードを" + (PhysxSetting.isDebugMode() ? "有効" : "無効") + "にしました");
            return true;
        } else if (arguments[0].equals(densityArgument) && arguments[1] != null) {
            float density;
            try {
                density = Float.parseFloat(arguments[1]);
            } catch (NumberFormatException e) {
                sendUsage(sender);
                return true;
            }
            if (density > 0) {
                PhysxSetting.setDefaultDensity(density);
                sender.sendMessage("既定の密度を" + density + "にしました");
                return true;
            }
        } else if (arguments[0].equals(updateArgument)) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーしか実行できません");
                return true;
            }
            PhysxMc.physxWorld.registerChunksToReloadNextSecond(((Player) sender).getChunk());
            sender.sendMessage("プレイヤーが今いるチャンクをアップデートしました");
            return true;
        } else if (arguments[0].equals(summonArgument) && arguments[1] != null && arguments[2] != null && arguments[3] != null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーしか実行できません");
                return true;
            }
            float x, y, z;
            try {
                x = Float.parseFloat(arguments[1]);
                y = Float.parseFloat(arguments[2]);
                z = Float.parseFloat(arguments[3]);
            } catch (NumberFormatException e) {
                sendUsage(sender);
                return true;
            }
            
            // ブロックタイプを指定できるようにする（オプション）
            Material blockType = Material.COMMAND_BLOCK; // デフォルトはコマンドブロック
            if (arguments.length > 4 && arguments[4] != null) {
                try {
                    blockType = Material.valueOf(arguments[4].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("無効なブロック名です: " + arguments[4]);
                    return true;
                }
            }
            
            PhysxMc.displayedBoxHolder.createDisplayedBox(((Player) sender).getLocation(), new Vector(x, y, z), new ItemStack(blockType), List.of(new Vector()));
            sender.sendMessage("物理演算ブロックを生成しました (サイズ:" + x + "x" + y + "x" + z + ", ブロック:" + blockType + ")");
            return true;
        } else if (arguments[0].equals(gravityArgument) && arguments[1] != null && arguments[2] != null && arguments[3] != null) {
            float x, y, z;
            try {
                x = Float.parseFloat(arguments[1]);
                y = Float.parseFloat(arguments[2]);
                z = Float.parseFloat(arguments[3]);
            } catch (NumberFormatException e) {
                sendUsage(sender);
                return true;
            }
            PhysxMc.physxWorld.setGravity(new Vector(x, y, z));
            sender.sendMessage("重力を変更しました");
            return true;
        } else if (arguments[0].equals(coinArgument) && arguments[1] != null) {
            if (arguments[1].equals("enable")) {
                PhysxSetting.setCoinSystemEnabled(!PhysxSetting.isCoinSystemEnabled());
                sender.sendMessage("コイン投擲システムを" + (PhysxSetting.isCoinSystemEnabled() ? "有効" : "無効") + "にしました");
                return true;
            }
        } else if (arguments[0].equals(pusherArgument) && arguments[1] != null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーしか実行できません");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (arguments[1].equals("create") && arguments[2] != null && arguments[3] != null && arguments[4] != null && arguments[5] != null) {
                // /physxmc pusher create <height> <width> <length> <range> [material] [speed]
                try {
                    double height = Double.parseDouble(arguments[2]);
                    int width = Integer.parseInt(arguments[3]);
                    double length = Double.parseDouble(arguments[4]);
                    double range = Double.parseDouble(arguments[5]);
                    
                    Material material = Material.IRON_BLOCK; // デフォルト
                    if (arguments.length > 6 && arguments[6] != null) {
                        try {
                            material = Material.valueOf(arguments[6].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("無効なブロック名です: " + arguments[6]);
                            return true;
                        }
                    }
                    
                    double speed = PhysxSetting.getPusherSpeed(); // デフォルト速度
                    if (arguments.length > 7 && arguments[7] != null) {
                        try {
                            speed = Double.parseDouble(arguments[7]);
                            if (speed <= 0) {
                                sender.sendMessage("速度は正の値である必要があります");
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage("速度の数値が正しくありません");
                            return true;
                        }
                    }
                    
                    if (height <= 0 || width <= 0 || length <= 0 || range <= 0) {
                        sender.sendMessage("高さ、幅、長さ、移動範囲は正の値である必要があります");
                        return true;
                    }
                    
                    MedalPusher createdPusher = PhysxMc.pusherManager.createPusher(player.getLocation(), height, width, length, range, material, speed);
                    if (createdPusher != null) {
                        sender.sendMessage("プッシャーを作成しました (高さ:" + height + ", 幅:" + width + ", 長さ:" + length + ", 移動範囲:" + range + ", ブロック:" + material + ", 速度:" + speed + ")");
                    } else {
                        sender.sendMessage("プッシャーの作成に失敗しました。近くに既存のプッシャーがないか確認してください（最小距離: 2ブロック）");
                    }
                    return true;
                    
                } catch (NumberFormatException e) {
                    sender.sendMessage("数値が正しくありません");
                    return true;
                }
            } else if (arguments[1].equals("remove")) {
                // /physxmc pusher remove
                if (PhysxMc.pusherManager.removeNearestPusher(player.getLocation(), 10.0)) {
                    sender.sendMessage("近くのプッシャーを削除しました");
                } else {
                    sender.sendMessage("近くにプッシャーが見つかりません");
                }
                return true;
            } else if (arguments[1].equals("clear")) {
                // /physxmc pusher clear
                PhysxMc.pusherManager.destroyAll();
                sender.sendMessage("全てのプッシャーを削除しました");
                return true;
            } else if (arguments[1].equals("count")) {
                // /physxmc pusher count
                sender.sendMessage("現在のプッシャー数: " + PhysxMc.pusherManager.getPusherCount());
                return true;
            }
        } else if (arguments[0].equals(ballArgument) && arguments[1] != null && arguments[2] != null) {
            // プレイヤー用: /physxmc ball <radius> <density> [material]
            // コマンドブロック用: /physxmc ball <radius> <density> <x> <y> <z> [material]
            
            try {
                double radius = Double.parseDouble(arguments[1]);
                float density = Float.parseFloat(arguments[2]);
                
                if (radius <= 0) {
                    sender.sendMessage("半径は正の値である必要があります");
                    return true;
                }
                
                if (density <= 0) {
                    sender.sendMessage("密度は正の値である必要があります");
                    return true;
                }
                
                Location spawnLocation;
                Material material = Material.SLIME_BLOCK; // デフォルトはスライムブロック
                
                if (sender instanceof Player) {
                    // プレイヤー実行の場合：目の位置に召喚
                    Player player = (Player) sender;
                    spawnLocation = player.getEyeLocation();
                    
                    // マテリアル指定（プレイヤー実行時は4番目の引数）
                    if (arguments.length > 3 && arguments[3] != null) {
                        try {
                            material = Material.valueOf(arguments[3].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("無効なブロック名です: " + arguments[3]);
                            return true;
                        }
                    }
                } else if (sender instanceof BlockCommandSender) {
                    // コマンドブロック実行の場合：座標が必要
                    if (arguments.length < 6) {
                        sender.sendMessage("コマンドブロック実行時は座標が必要です: /physxmc ball <radius> <density> <x> <y> <z> [material]");
                        return true;
                    }
                    
                    try {
                        double x = Double.parseDouble(arguments[3]);
                        double y = Double.parseDouble(arguments[4]);
                        double z = Double.parseDouble(arguments[5]);
                        
                        spawnLocation = new Location(((BlockCommandSender) sender).getBlock().getWorld(), x, y, z);
                        
                        // マテリアル指定（コマンドブロック実行時は7番目の引数）
                        if (arguments.length > 6 && arguments[6] != null) {
                            try {
                                material = Material.valueOf(arguments[6].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage("無効なブロック名です: " + arguments[6]);
                                return true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("座標の数値が正しくありません");
                        return true;
                    }
                } else {
                    sender.sendMessage("このコマンドはプレイヤーまたはコマンドブロックから実行してください");
                    return true;
                }
                
                PhysxMc.displayedSphereHolder.createDisplayedSphere(spawnLocation, radius, material, density);
                sender.sendMessage("球体を召喚しました (半径:" + radius + ", 密度:" + density + ", マテリアル:" + material + ")");
                return true;
                
            } catch (NumberFormatException e) {
                sender.sendMessage("数値が正しくありません");
                return true;
            }
        } else if (arguments[0].equals(rampArgument) && arguments[1] != null) {
            // プレイヤー用: /physxmc ramp create <pitch> <width> <length> <thickness> [material]
            // コマンドブロック用: /physxmc ramp create <pitch> <width> <length> <thickness> <x> <y> <z> [material]

            if (arguments[1].equals("create") && arguments.length >= 6 && 
                arguments[2] != null && arguments[3] != null && arguments[4] != null && arguments[5] != null) {
                
                try {
                    double pitch = Double.parseDouble(arguments[2]);
                    double width = Double.parseDouble(arguments[3]);
                    double length = Double.parseDouble(arguments[4]);
                    double thickness = Double.parseDouble(arguments[5]);

                    if (width <= 0 || length <= 0 || thickness <= 0) {
                        sender.sendMessage("幅、長さ、厚みは正の値である必要があります");
                        return true;
                    }
                    
                    if (pitch < -90 || pitch > 90) {
                        sender.sendMessage("角度は-90から90度の範囲で指定してください");
                        return true;
                    }

                    Location spawnLocation;
                    Material material = Material.IRON_BLOCK; // デフォルト
                    
                    if (sender instanceof Player) {
                        // プレイヤー実行の場合：プレイヤーの位置に作成
                        Player player = (Player) sender;
                        spawnLocation = player.getLocation();
                        
                        // マテリアル指定（プレイヤー実行時は7番目の引数）
                        if (arguments.length > 6 && arguments[6] != null && !arguments[6].trim().isEmpty()) {
                            try {
                                material = Material.valueOf(arguments[6].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage("無効なブロック名です: " + arguments[6]);
                                return true;
                            }
                        }
                    } else if (sender instanceof BlockCommandSender) {
                        // コマンドブロック実行の場合：座標が必要
                        if (arguments.length < 9) {
                            sender.sendMessage("コマンドブロック実行時は座標が必要です: /physxmc ramp create <pitch> <width> <length> <thickness> <x> <y> <z> [material]");
                            return true;
                        }
                        
                        try {
                            double x = Double.parseDouble(arguments[6]);
                            double y = Double.parseDouble(arguments[7]);
                            double z = Double.parseDouble(arguments[8]);
                            
                            spawnLocation = new Location(((BlockCommandSender) sender).getBlock().getWorld(), x, y, z);
                            
                            // マテリアル指定（コマンドブロック実行時は10番目の引数）
                            if (arguments.length > 9 && arguments[9] != null && !arguments[9].trim().isEmpty()) {
                                try {
                                    material = Material.valueOf(arguments[9].toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    sender.sendMessage("無効なブロック名です: " + arguments[9]);
                                    return true;
                                }
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage("座標の数値が正しくありません");
                            return true;
                        }
                    } else {
                        sender.sendMessage("このコマンドはプレイヤーまたはコマンドブロックから実行してください");
                        return true;
                    }

                    DisplayedPhysxBox ramp = PhysxMc.rampManager.createRamp(spawnLocation, pitch, width, length, thickness, material);
                    if (ramp != null) {
                        sender.sendMessage("ランプを作成しました (角度:" + pitch + ", 幅:" + width + ", 長さ:" + length + ", 厚み:" + thickness + ", ブロック:" + material + ")");
                    } else {
                        sender.sendMessage("ランプの作成に失敗しました。サーバーログを確認してください。");
                    }
                    return true;

                } catch (NumberFormatException e) {
                    sender.sendMessage("数値が正しくありません: " + e.getMessage());
                    return true;
                }
            } else if (arguments.length >= 2 && arguments[1].equals("remove")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("removeコマンドはプレイヤーしか実行できません");
                    return true;
                }
                Player player = (Player) sender;
                if (PhysxMc.rampManager.removeNearestRamp(player.getLocation(), 10.0)) {
                    sender.sendMessage("近くのランプを削除しました");
                } else {
                    sender.sendMessage("近くにランプが見つかりません");
                }
                return true;
            } else if (arguments.length >= 2 && arguments[1].equals("clear")) {
                PhysxMc.rampManager.destroyAll();
                sender.sendMessage("全てのランプを削除しました");
                return true;
            } else if (arguments.length >= 2 && arguments[1].equals("count")) {
                sender.sendMessage("現在のランプ数: " + PhysxMc.rampManager.getRampCount());
                return true;
            }
        } else if (arguments[0].equals("payout")) {
            // payout関連コマンド（プレイヤーまたはコマンドブロック対応）

            if (arguments.length >= 2 && arguments[1].equals("start")) {
                // /physxmc payout start <taskId> <x> <y> <z> <intervalSeconds> [maxCoins]
                if (arguments.length >= 7) {
                    try {
                        String taskId = arguments[2];
                        double x = Double.parseDouble(arguments[3]);
                        double y = Double.parseDouble(arguments[4]);
                        double z = Double.parseDouble(arguments[5]);
                        double intervalSeconds = Double.parseDouble(arguments[6]);
                        int maxCoins = -1; // デフォルトは無制限
                        
                        if (arguments.length >= 8) {
                            maxCoins = Integer.parseInt(arguments[7]);
                        }
                        
                        // プレイヤーまたはコマンドブロックの世界を使用
                        org.bukkit.World world = null;
                        if (sender instanceof Player) {
                            world = ((Player) sender).getWorld();
                        } else {
                            // コマンドブロックの場合、実行者の世界を取得
                            world = org.bukkit.Bukkit.getWorlds().get(0); // デフォルトワールド
                        }
                        
                        Location location = new Location(world, x, y, z);
                        
                        boolean success = PhysxMc.medalPayoutSystem.startPayout(taskId, location, intervalSeconds, maxCoins);
                        if (success) {
                            sender.sendMessage("払い出しシステムを開始しました: " + taskId);
                            sender.sendMessage("位置: " + String.format("%.1f, %.1f, %.1f", x, y, z));
                            sender.sendMessage("間隔: " + intervalSeconds + "秒");
                            sender.sendMessage("最大枚数: " + (maxCoins > 0 ? maxCoins + "枚" : "無制限"));
                        } else {
                            sender.sendMessage("払い出しシステムの開始に失敗しました");
                        }
                        return true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("数値が正しくありません");
                        return true;
                    }
                } else {
                    sender.sendMessage("使用方法: /physxmc payout start <taskId> <x> <y> <z> <intervalSeconds> [maxCoins]");
                    return true;
                }
            } else if (arguments.length >= 2 && arguments[1].equals("stop")) {
                // /physxmc payout stop <taskId>
                if (arguments.length >= 3) {
                    String taskId = arguments[2];
                    boolean success = PhysxMc.medalPayoutSystem.stopPayout(taskId);
                    if (success) {
                        sender.sendMessage("払い出しシステムを停止しました: " + taskId);
                    } else {
                        sender.sendMessage("払い出しタスクが見つかりません: " + taskId);
                    }
                    return true;
                } else {
                    sender.sendMessage("使用方法: /physxmc payout stop <taskId>");
                    return true;
                }
            } else if (arguments.length >= 2 && arguments[1].equals("stopall")) {
                // /physxmc payout stopall
                PhysxMc.medalPayoutSystem.stopAllPayouts();
                sender.sendMessage("全ての払い出しシステムを停止しました");
                return true;
            } else if (arguments.length >= 2 && arguments[1].equals("list")) {
                // /physxmc payout list
                sender.sendMessage(PhysxMc.medalPayoutSystem.getAllTasksInfo());
                return true;
            } else if (arguments.length >= 2 && arguments[1].equals("info")) {
                // /physxmc payout info <taskId>
                if (arguments.length >= 3) {
                    String taskId = arguments[2];
                    sender.sendMessage(PhysxMc.medalPayoutSystem.getTaskInfo(taskId));
                    return true;
                } else {
                    sender.sendMessage("使用方法: /physxmc payout info <taskId>");
                    return true;
                }
            }
        } else if (arguments[0].equals(saveArgument)) {
            // /physxmc save - 現在のオブジェクトを全て保存（非同期実行）
            sender.sendMessage("オブジェクトの保存を開始します...");
            
            // 保存対象の数を事前に取得（finalにする）
            final int pusherCount = PhysxMc.pusherManager.getPusherCount();
            final int rampCount = PhysxMc.rampManager.getRampCount();
            // final int boxCount = PhysxMc.displayedBoxHolder.getAllBoxes().size();
            final int boxCount = 0; // 物理オブジェクト保存を無効化したため0に設定
            // スフィアは保存対象外
            
            // 非同期で保存処理を実行
            org.bukkit.scheduler.BukkitRunnable saveTask = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // プッシャーの保存
                        PhysxMc.pusherManager.savePushers();
                        
                        // ランプの保存
                        PhysxMc.rampManager.saveRamps();
                        
                        // 物理オブジェクトの保存を無効化（ネイティブクラッシュ防止）
                        // PhysxMc.physicsObjectManager.saveAll();
                        
                        final int totalSaved = pusherCount + rampCount + boxCount;
                        
                        // メインスレッドで結果を送信
                        org.bukkit.scheduler.BukkitRunnable resultTask = new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage("すべてのオブジェクトを保存しました:");
                                sender.sendMessage("- プッシャー: " + pusherCount + "個");
                                sender.sendMessage("- ランプ: " + rampCount + "個");
                                sender.sendMessage("- ボックス: 保存無効化（ネイティブクラッシュ防止）");
                                sender.sendMessage("- スフィア: 保存対象外");
                                sender.sendMessage("合計: " + totalSaved + "個のオブジェクトを保存");
                            }
                        };
                        resultTask.runTask(com.kamesuta.physxmc.PhysxMc.getPlugin(com.kamesuta.physxmc.PhysxMc.class));
                        
                    } catch (Exception e) {
                        // エラーもメインスレッドで送信
                        org.bukkit.scheduler.BukkitRunnable errorTask = new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                sender.sendMessage("保存中にエラーが発生しました: " + e.getMessage());
                            }
                        };
                        errorTask.runTask(com.kamesuta.physxmc.PhysxMc.getPlugin(com.kamesuta.physxmc.PhysxMc.class));
                    }
                }
            };
            saveTask.runTaskAsynchronously(com.kamesuta.physxmc.PhysxMc.getPlugin(com.kamesuta.physxmc.PhysxMc.class));
            return true;
        }

        sendUsage(sender);
        return true;
    }

    @Override
    public void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/physxmc reset: 物理オブジェクトをリセットする\n" +
                "/physxmc debugmode: 右クリックで持っているアイテムが投げられたり掴めたりするデバッグモードを有効/無効にする\n" +
                "/physxmc density {float型}: 召喚する物理オブジェクトの既定の密度を設定する\n" +
                "/physxmc updateCurrentChunk: プレイヤーが今いるチャンクの地形をリロードする\n" +
                "/physxmc summon {縦}　{高さ}　{横} [ブロック名]: 物理演算ブロックを召喚する\n" +
                "/physxmc gravity {x}　{y}　{z}: 重力の大きさを設定する\n" +
                "/physxmc coin enable: 鉄製のトラップドアを使ったコイン投擲システムを有効/無効にする\n" +
                "/physxmc pusher create {高さ} {幅} {長さ} {移動範囲} [ブロック名] [速度]: 指定サイズのプッシャーを作成する\n" +
                "/physxmc pusher remove: 近くのプッシャーを削除する\n" +
                "/physxmc pusher clear: 全てのプッシャーを削除する\n" +
                "/physxmc pusher count: プッシャーの数を表示する\n" +
                "/physxmc ball {半径} {密度} [マテリアル]: 指定サイズと重さの転がる球体を召喚する\n" +
                "/physxmc ball {半径} {密度} {x} {y} {z} [マテリアル]: コマンドブロック用の球体召喚\n" +
                "/physxmc ramp create {角度} {幅} {長さ} {厚み} [ブロック名]: 傾斜板を作成する\n" +
                "/physxmc ramp create {角度} {幅} {長さ} {厚み} {x} {y} {z} [ブロック名]: コマンドブロック用のランプ作成\n" +
                "/physxmc ramp remove: 近くのランプを削除する\n" +
                "/physxmc ramp clear: 全てのランプを削除する\n" +
                "/physxmc ramp count: ランプの数を表示する\n" +
                "/physxmc payout start <taskId> <x> <y> <z> <intervalSeconds> [maxCoins]: 指定座標にコイン払い出しを開始\n" +
                "/physxmc payout stop <taskId>: 指定タスクの払い出しを停止\n" +
                "/physxmc payout stopall: 全ての払い出しを停止\n" +
                "/physxmc payout list: アクティブなタスクを表示\n" +
                "/physxmc payout info <taskId>: タスクの詳細情報を表示\n" +
                "/physxmc save: 現在のすべてのオブジェクトをファイルに保存\n"));
    }

    @EventHandler
    public void AsyncTabCompleteEvent(AsyncTabCompleteEvent e) {
        if (e.getBuffer().startsWith("/" + commandName + " ")) {
            List<String> suggestions = new ArrayList<>();
            String pureBuffer = e.getBuffer().replace("/" + commandName + " ", "");
            arguments.forEach(s -> {
                if (s.startsWith(pureBuffer))
                    suggestions.add(s);
            });
            e.setCompletions(suggestions);
        }
    }
}
