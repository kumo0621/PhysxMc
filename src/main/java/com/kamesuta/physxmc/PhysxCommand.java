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
 * コマンドを仮実装するクラス。CommandLib(<a href="https://github.com/TeamKun/CommandLib">...</a>)が1.20に対応し次第移行する
 */
public class PhysxCommand extends CommandBase implements Listener {
    
    private static final String commandName = "physxmc";
    private static final String resetArgument = "reset";

    /**
     * 引数のリスト
     */
    private static final List<String> arguments = List.of(resetArgument);
    
    public PhysxCommand() {
        super(commandName, 1, 1, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, @Nullable String[] arguments) {
        if(arguments[0].equals(resetArgument)){
            PhysxMc.rigidBlockDisplay.destroyAll();
            return true;
        }
        
        sendUsage(sender);
        return true;
    }

    @Override
    public void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/physxmc reset: 物理演算をリセットする"));
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
