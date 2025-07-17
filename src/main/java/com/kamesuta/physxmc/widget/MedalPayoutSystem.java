package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.PhysxSetting;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * メダルゲームの払い出し機構
 * 指定座標に一定間隔で鉄のトラップドア（コイン）を生成する
 */
public class MedalPayoutSystem {
    
    private final Logger logger;
    private final ConcurrentHashMap<String, PayoutTask> activeTasks = new ConcurrentHashMap<>();
    
    public MedalPayoutSystem() {
        this.logger = Bukkit.getLogger();
    }
    
    /**
     * 払い出しタスクの情報を保持するクラス
     */
    private static class PayoutTask {
        final Location location;
        final double intervalSeconds;
        final int maxCoins;
        final AtomicInteger coinsSpawned;
        final BukkitTask task;
        
        PayoutTask(Location location, double intervalSeconds, int maxCoins, BukkitTask task) {
            this.location = location;
            this.intervalSeconds = intervalSeconds;
            this.maxCoins = maxCoins;
            this.coinsSpawned = new AtomicInteger(0);
            this.task = task;
        }
    }
    
    /**
     * 払い出しシステムを開始
     * @param taskId タスクID（一意）
     * @param location 生成位置
     * @param intervalSeconds 生成間隔（秒）
     * @param maxCoins 最大生成数（-1で無制限）
     * @return 開始できたかどうか
     */
    public boolean startPayout(String taskId, Location location, double intervalSeconds, int maxCoins) {
        if (activeTasks.containsKey(taskId)) {
            logger.warning("払い出しタスク既に存在: " + taskId);
            return false;
        }
        
        if (intervalSeconds <= 0) {
            logger.warning("払い出し間隔は正の値である必要があります: " + intervalSeconds);
            return false;
        }
        
        // 20tick = 1秒なので、intervalSeconds * 20 = tick間隔
        long tickInterval = Math.max(1L, Math.round(intervalSeconds * 20.0));
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                PayoutTask payoutTask = activeTasks.get(taskId);
                if (payoutTask == null) {
                    cancel();
                    return;
                }
                
                // 最大数に達した場合は停止
                if (maxCoins > 0 && payoutTask.coinsSpawned.get() >= maxCoins) {
                    stopPayout(taskId);
                    return;
                }
                
                // コインを生成
                DisplayedPhysxBox coin = createCoin(location);
                if (coin != null) {
                    int spawned = payoutTask.coinsSpawned.incrementAndGet();
                    logger.info("コイン払い出し: " + taskId + " (" + spawned + "/" + 
                               (maxCoins > 0 ? maxCoins : "無制限") + ")");
                } else {
                    logger.warning("コイン生成に失敗: " + taskId);
                }
            }
        }.runTaskTimer(PhysxMc.getPlugin(PhysxMc.class), 0L, tickInterval);
        
        PayoutTask payoutTask = new PayoutTask(location, intervalSeconds, maxCoins, task);
        activeTasks.put(taskId, payoutTask);
        
        logger.info("払い出しシステム開始: " + taskId + " @ " + 
                   String.format("%.1f,%.1f,%.1f", location.getX(), location.getY(), location.getZ()) +
                   " 間隔:" + intervalSeconds + "秒");
        
        return true;
    }
    
    /**
     * 払い出しシステムを停止
     * @param taskId タスクID
     * @return 停止できたかどうか
     */
    public boolean stopPayout(String taskId) {
        PayoutTask payoutTask = activeTasks.remove(taskId);
        if (payoutTask == null) {
            logger.warning("払い出しタスクが見つかりません: " + taskId);
            return false;
        }
        
        payoutTask.task.cancel();
        logger.info("払い出しシステム停止: " + taskId + " (生成済み: " + payoutTask.coinsSpawned.get() + "個)");
        return true;
    }
    
    /**
     * 全ての払い出しシステムを停止
     */
    public void stopAllPayouts() {
        for (String taskId : activeTasks.keySet()) {
            stopPayout(taskId);
        }
        logger.info("全ての払い出しシステムを停止しました");
    }
    
    /**
     * アクティブなタスク数を取得
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }
    
    /**
     * タスクの状態を取得
     */
    public String getTaskInfo(String taskId) {
        PayoutTask task = activeTasks.get(taskId);
        if (task == null) {
            return "タスクが見つかりません: " + taskId;
        }
        
        return String.format("タスク: %s | 位置: %.1f,%.1f,%.1f | 間隔: %.1f秒 | 生成済み: %d/%s",
                taskId, task.location.getX(), task.location.getY(), task.location.getZ(),
                task.intervalSeconds, task.coinsSpawned.get(),
                task.maxCoins > 0 ? String.valueOf(task.maxCoins) : "無制限");
    }
    
    /**
     * 全タスクの状態を取得
     */
    public String getAllTasksInfo() {
        if (activeTasks.isEmpty()) {
            return "アクティブなタスクがありません";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("アクティブなタスク (").append(activeTasks.size()).append("個):\n");
        for (String taskId : activeTasks.keySet()) {
            sb.append("- ").append(getTaskInfo(taskId)).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * コインを生成する（EventHandlerのcreateCoitメソッドと同じ実装）
     * @param location 生成位置
     * @return 生成されたコイン
     */
    private DisplayedPhysxBox createCoin(Location location) {
        try {
            // 鉄製のトラップドアでコインを作成
            ItemStack coinItem = new ItemStack(Material.IRON_TRAPDOOR);
            float coinSize = PhysxSetting.getCoinSize();
            Vector scale = new Vector(coinSize, coinSize / 4.0 * 3.0, coinSize); // コインの形状（厚さを3倍に）
            List<Vector> offsets = List.of(new Vector()); // 単一のオブジェクト
            float coinDensity = PhysxSetting.getCoinDensity(); // 金の密度
            
            DisplayedPhysxBox coin = PhysxMc.displayedBoxHolder.createDisplayedBox(
                location,
                scale,
                coinItem,
                offsets,
                coinDensity  // コイン専用密度を指定（これによりコイン判定される）
            );
            
            return coin;
        } catch (Exception e) {
            logger.severe("コイン生成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}