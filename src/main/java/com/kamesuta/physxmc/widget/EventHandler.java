package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

import static com.kamesuta.physxmc.PhysxMc.displayedBoxHolder;

/**
 * Bukkitのイベントを受信するクラス
 */
public class EventHandler implements Listener {

    @org.bukkit.event.EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;

        // コイン投擲システムの処理
        if (PhysxSetting.isCoinSystemEnabled() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getItem() != null && event.getItem().getType() == Material.IRON_TRAPDOOR) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                    DisplayedPhysxBox coin = createCoin(event.getPlayer());
                    if (coin != null) {
                        coin.throwBox(event.getPlayer().getEyeLocation());
                        // 鉄製のトラップドアを1個消費
                        event.getItem().setAmount(event.getItem().getAmount() - 1);
                        return;
                    }
                }
            }
        }

        // デバッグモードの処理
        if (!PhysxSetting.isDebugMode())
            return;

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                DisplayedPhysxBox box = displayedBoxHolder.debugCreate(event.getPlayer());
                if (box != null) {
                    box.throwBox(event.getPlayer().getEyeLocation());
                    return;
                }
            }
            if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                if(event.getItem() == null && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.DIAMOND_BLOCK){
                    event.setCancelled(true);
                    List<Vector> offsets = scanOffsets(event.getInteractionPoint(), Material.DIAMOND_BLOCK);
                    displayedBoxHolder.getOffsetMap().put(event.getPlayer(), offsets);
                    event.getPlayer().sendMessage("連結する形状を記憶しました");
                }
            }
        }
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            if (PhysxMc.grabTool.isGrabbing(event.getPlayer())) {
                event.setCancelled(true);
                PhysxMc.grabTool.release(event.getPlayer());
            } else {
                if (event.getItem() != null && event.getItem().getType() == Material.STICK) {
                    DisplayedPhysxBox box = displayedBoxHolder.raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null) {
                        box.throwBox(event.getPlayer().getEyeLocation());
                    }
                    event.setCancelled(true);
                    return;
                }

                if (event.getItem() != null && event.getItem().getType() == Material.FLINT_AND_STEEL) {
                    DisplayedPhysxBox box = displayedBoxHolder.raycast(event.getPlayer().getEyeLocation(), 4);
                    if (box != null) {
                        displayedBoxHolder.destroySpecific(box);
                    }
                    event.setCancelled(true);
                    return;
                }

                if (PhysxMc.grabTool.tryGrab(event.getPlayer()))
                    event.setCancelled(true);
            }
        }
    }

    @org.bukkit.event.EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        displayedBoxHolder.executeExplosion(event.getLocation(), 6.9f);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        PhysxMc.grabTool.release(event.getPlayer());
    }
    
    /**
     * コインを作成する
     * @param player プレイヤー
     * @return 作成されたコイン
     */
    public DisplayedPhysxBox createCoin(org.bukkit.entity.Player player) {
        // 鉄製のトラップドアでコインを作成
        org.bukkit.inventory.ItemStack coinItem = new org.bukkit.inventory.ItemStack(Material.IRON_TRAPDOOR);
        float coinSize = PhysxSetting.getCoinSize();
        Vector scale = new Vector(coinSize, coinSize / 4.0 * 3.0, coinSize); // コインの形状（厚さを3倍に）
        List<Vector> offsets = List.of(new Vector()); // 単一のオブジェクト
        float coinDensity = PhysxSetting.getCoinDensity(); // 金の密度
        
        // コインの投擲位置をプレイヤーの胸の高さに設定（足元から1.5ブロック上）
        Location coinLocation = player.getLocation().clone();
        coinLocation.setY(coinLocation.getY() + 1.5);
        
        DisplayedPhysxBox coin = displayedBoxHolder.createDisplayedBox(
            coinLocation, 
            scale, 
            coinItem, 
            offsets,
            coinDensity  // コイン専用密度を指定（これによりコイン判定される）
        );
        
        return coin;
    }

    public static List<Vector> scanOffsets(Location location, Material material){
        List<Vector> offsets = new ArrayList<>();
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                for (int k = -5; k <= 5; k++) {
                    Block block = location.getBlock().getRelative(i,j,k);
                    if(block.getType() == material)
                        offsets.add(new Vector(i,j,k));
                }
            }
        }
        if(offsets.size() == 0)
            offsets.add(new Vector());
        
        return offsets;
    }
}
