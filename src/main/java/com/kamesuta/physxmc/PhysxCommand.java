package com.kamesuta.physxmc;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ライブラリのコマンドを仮実装するクラス。CommandLib(<a href="https://github.com/TeamKun/CommandLib">...</a>)が1.20に対応し次第移行する
 */
public class PhysxCommand extends CommandBase implements Listener {
    
    private static final String commandName = "physxmc";
    private static final String resetArgument = "reset";
    private static final String debugArgument = "debug";
    private static final String densityArgument = "density";

    /**
     * 引数のリスト
     */
    private static final List<String> arguments = List.of(resetArgument, debugArgument, densityArgument);
    
    public PhysxCommand() {
        super(commandName, 1, 2, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, @Nullable String[] arguments) {
        if(arguments[0].equals(resetArgument)){
            PhysxMc.rigidBlockDisplay.destroyAll();
            return true;
        }
        else if(arguments[0].equals(debugArgument)){
            PhysxSetting.setDebugMode(!PhysxSetting.isDebugMode());
            sender.sendMessage("デバッグモードを" + (PhysxSetting.isDebugMode() ? "有効" : "無効") + "にしました");
            return true;
        }
        else if(arguments[0].equals(densityArgument) && arguments[1] != null){
            float density;
            try{
                density = Float.parseFloat(arguments[1]);
            }
            catch (NumberFormatException e){
                sendUsage(sender);
                return true;
            }
            if(density > 0){
                PhysxSetting.setDefaultDensity(density);
                sender.sendMessage("既定の密度を" + density + "にしました");
                return true;
            }
        }
        
        sendUsage(sender);
        return true;
    }

    @Override
    public void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/physxmc reset: 物理演算をリセットする\n" +
                "/physxmc debug: 右クリックでアイテムが投げられるデバッグモードを有効/無効にする\n" + 
                "/physxmc density {float型} 召喚する物理オブジェクトの既定の密度を設定する"));
    }
    
    @EventHandler
    public void AsyncTabCompleteEvent(AsyncTabCompleteEvent e) {
        if (e.getBuffer().startsWith("/" + commandName + " ")) {
            List<String> suggestions = new ArrayList<>();
            String pureBuffer = e.getBuffer().replace("/" + commandName + " ", "");
            arguments.forEach(s -> {
                if(s.startsWith(pureBuffer))
                    suggestions.add(s);
            });
            e.setCompletions(suggestions);
        }
    }
}
