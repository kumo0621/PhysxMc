package com.kamesuta.physxmc.command;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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

    /**
     * 引数のリスト
     */
    private static final List<String> arguments = List.of(resetArgument, debugArgument, densityArgument, updateArgument, summonArgument, gravityArgument, coinArgument, pusherArgument);

    public PhysxCommand() {
        super(commandName, 1, 6, false);
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
            PhysxMc.displayedBoxHolder.createDisplayedBox(((Player) sender).getLocation(), new Vector(x, y, z), new ItemStack(Material.COMMAND_BLOCK), List.of(new Vector()));
            sender.sendMessage("テストブロックを生成しました");
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
            
            if (arguments[1].equals("create") && arguments[2] != null && arguments[3] != null && arguments[4] != null) {
                // /physxmc pusher create <height> <width> <range> [material]
                try {
                    int height = Integer.parseInt(arguments[2]);
                    int width = Integer.parseInt(arguments[3]);
                    double range = Double.parseDouble(arguments[4]);
                    
                    Material material = Material.IRON_BLOCK; // デフォルト
                    if (arguments.length > 5 && arguments[5] != null) {
                        try {
                            material = Material.valueOf(arguments[5].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("無効なブロック名です: " + arguments[5]);
                            return true;
                        }
                    }
                    
                    if (height <= 0 || width <= 0 || range <= 0) {
                        sender.sendMessage("高さ、幅、伸び範囲は正の値である必要があります");
                        return true;
                    }
                    
                    PhysxMc.pusherManager.createPusher(player.getLocation(), height, width, range, material);
                    sender.sendMessage("プッシャーを作成しました (高さ:" + height + ", 幅:" + width + ", 伸び範囲:" + range + ", ブロック:" + material + ")");
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
                "/physxmc summon {縦}　{高さ}　{横}: テストオブジェクトを1個召喚する\n" +
                "/physxmc gravity {x}　{y}　{z}: 重力の大きさを設定する\n" +
                "/physxmc coin enable: 鉄製のトラップドアを使ったコイン投擲システムを有効/無効にする\n" +
                "/physxmc pusher create {高さ} {幅} {伸び範囲} [ブロック名]: 壁が伸び縮みするプッシャーを作成する\n" +
                "/physxmc pusher remove: 近くのプッシャーを削除する\n" +
                "/physxmc pusher clear: 全てのプッシャーを削除する\n" +
                "/physxmc pusher count: プッシャーの数を表示する\n"));
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
