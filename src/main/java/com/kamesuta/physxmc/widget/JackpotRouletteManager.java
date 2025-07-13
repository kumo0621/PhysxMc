package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxSphere;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ジャックポットルーレットを管理するクラス
 */
public class JackpotRouletteManager {
    
    private final List<JackpotRoulette> roulettes = new ArrayList<>();
    private final Map<UUID, BallTracker> ballTrackers = new HashMap<>(); // ボールの追跡
    private final Logger logger;
    
    // デフォルト設定
    private static final double DEFAULT_RADIUS = 4.0;
    private static final double DEFAULT_ROTATION_SPEED = 0.02; // rad/tick
    private static final Material DEFAULT_DISK_MATERIAL = Material.QUARTZ_BLOCK;
    private static final Material DEFAULT_WIN_SLOT_MATERIAL = Material.EMERALD_BLOCK;
    private static final Material DEFAULT_NORMAL_SLOT_MATERIAL = Material.IRON_BLOCK;
    
    // ボール追跡時間（秒）
    private static final int BALL_TRACKING_TIME = 30;
    
    public JackpotRouletteManager() {
        this.logger = Bukkit.getLogger();
    }
    
    /**
     * 新しいジャックポットルーレットを作成
     * @param location 中心位置
     * @param radius 半径
     * @param rotationSpeed 回転速度
     * @param diskMaterial 円盤材質
     * @param winSlotMaterial 当たり穴材質
     * @param normalSlotMaterial 通常穴材質
     * @return 作成されたルーレット
     */
    public JackpotRoulette createRoulette(Location location, double radius, double rotationSpeed,
                                         Material diskMaterial, Material winSlotMaterial, Material normalSlotMaterial) {
        // 重複チェック：3ブロック以内に既存のルーレットがないか確認
        double minDistance = 6.0; // 半径を考慮した最小距離
        for (JackpotRoulette existingRoulette : roulettes) {
            double distance = existingRoulette.getCenterLocation().distance(location);
            if (distance < minDistance) {
                logger.warning("ルーレット作成失敗: " + String.format("%.1f", distance) + 
                             "ブロック先に既存のルーレットがあります（最小距離: " + minDistance + "ブロック）");
                return null;
            }
        }
        
        JackpotRoulette roulette = new JackpotRoulette(location, radius, rotationSpeed, 
                                                      diskMaterial, winSlotMaterial, normalSlotMaterial);
        roulettes.add(roulette);
        logger.info("ジャックポットルーレットを作成しました: " + roulettes.size() + "個目");
        return roulette;
    }
    
    /**
     * デフォルト設定でルーレットを作成
     * @param location 中心位置
     * @return 作成されたルーレット
     */
    public JackpotRoulette createDefaultRoulette(Location location) {
        return createRoulette(location, DEFAULT_RADIUS, DEFAULT_ROTATION_SPEED,
                            DEFAULT_DISK_MATERIAL, DEFAULT_WIN_SLOT_MATERIAL, DEFAULT_NORMAL_SLOT_MATERIAL);
    }
    
    /**
     * ボールを投入
     * @param player プレイヤー
     * @param location 投入位置（最寄りのルーレットを検索）
     * @return 投入成功した場合true
     */
    public boolean launchBall(Player player, Location location) {
        // 最寄りのルーレットを検索
        JackpotRoulette nearestRoulette = findNearestRoulette(location, 10.0);
        if (nearestRoulette == null) {
            player.sendMessage("近くにジャックポットルーレットがありません（10ブロック以内）");
            return false;
        }
        
        // ボールを投入
        DisplayedPhysxSphere ball = nearestRoulette.launchBall(player);
        if (ball != null) {
            // ボール追跡を開始
            BallTracker tracker = new BallTracker(ball, player, nearestRoulette);
            ballTrackers.put(java.util.UUID.randomUUID(), tracker);
            
            player.sendMessage("🎯 ジャックポットチャレンジ開始！ボールの行方を見守りましょう...");
            return true;
        } else {
            player.sendMessage("ボールの投入に失敗しました");
            return false;
        }
    }
    
    /**
     * 毎tick更新処理
     */
    public void update() {
        // ルーレットの更新
        Iterator<JackpotRoulette> rouletteIterator = roulettes.iterator();
        while (rouletteIterator.hasNext()) {
            JackpotRoulette roulette = rouletteIterator.next();
            if (roulette.isValid()) {
                roulette.update();
            } else {
                roulette.destroy();
                rouletteIterator.remove();
            }
        }
        
        // ボール追跡の更新
        Iterator<Map.Entry<UUID, BallTracker>> ballIterator = ballTrackers.entrySet().iterator();
        while (ballIterator.hasNext()) {
            Map.Entry<UUID, BallTracker> entry = ballIterator.next();
            BallTracker tracker = entry.getValue();
            
            if (tracker.update()) {
                // 追跡完了（結果確定または時間切れ）
                ballIterator.remove();
            }
        }
    }
    
    /**
     * 最寄りのルーレットを検索
     * @param location 基準位置
     * @param maxDistance 最大距離
     * @return 最寄りのルーレット、なければnull
     */
    public JackpotRoulette findNearestRoulette(Location location, double maxDistance) {
        JackpotRoulette nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (JackpotRoulette roulette : roulettes) {
            double distance = roulette.getCenterLocation().distance(location);
            if (distance < nearestDistance && distance <= maxDistance) {
                nearestDistance = distance;
                nearest = roulette;
            }
        }
        
        return nearest;
    }
    
    /**
     * 指定した位置に最も近いルーレットを削除
     * @param location 基準位置
     * @param maxDistance 最大距離
     * @return 削除されたかどうか
     */
    public boolean removeNearestRoulette(Location location, double maxDistance) {
        JackpotRoulette nearest = findNearestRoulette(location, maxDistance);
        if (nearest != null) {
            nearest.destroy();
            roulettes.remove(nearest);
            logger.info("ジャックポットルーレットを削除しました");
            return true;
        }
        return false;
    }
    
    /**
     * 全てのルーレットを破壊
     */
    public void destroyAll() {
        for (JackpotRoulette roulette : roulettes) {
            roulette.destroy();
        }
        roulettes.clear();
        
        // 追跡中のボールもクリア
        ballTrackers.clear();
        
        logger.info("全てのジャックポットルーレットを破壊しました");
    }
    
    /**
     * ルーレットの数を取得
     */
    public int getRouletteCount() {
        return roulettes.size();
    }
    
    /**
     * 追跡中のボール数を取得
     */
    public int getTrackingBallCount() {
        return ballTrackers.size();
    }
    
    /**
     * ボール追跡クラス
     */
    private class BallTracker {
        private final DisplayedPhysxSphere ball;
        private final Player player;
        private final JackpotRoulette roulette;
        private final long startTime;
        private boolean resultNotified = false;
        
        public BallTracker(DisplayedPhysxSphere ball, Player player, JackpotRoulette roulette) {
            this.ball = ball;
            this.player = player;
            this.roulette = roulette;
            this.startTime = System.currentTimeMillis();
        }
        
        /**
         * ボール追跡の更新
         * @return 追跡完了した場合true
         */
        public boolean update() {
            // ボールが無効になった場合
            if (ball == null || ball.isDisplayDead()) {
                if (!resultNotified && player.isOnline()) {
                    player.sendMessage("🚫 ボールが消失しました。再度お試しください。");
                }
                return true; // 追跡終了
            }
            
            // 時間切れチェック
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedTime > BALL_TRACKING_TIME) {
                if (!resultNotified && player.isOnline()) {
                    player.sendMessage("⏰ 時間切れです。ボールを回収します。");
                }
                PhysxMc.displayedSphereHolder.destroySpecific(ball); // ボールを削除
                return true; // 追跡終了
            }
            
            // スロット判定
            if (!resultNotified) {
                Location ballLocation = ball.getLocation();
                RouletteSlot slot = roulette.checkBallInSlot(ballLocation);
                
                if (slot != null) {
                    // 結果通知
                    resultNotified = true;
                    String resultMessage = slot.getResultMessage(player.getName());
                    
                    if (player.isOnline()) {
                        player.sendMessage(resultMessage);
                    }
                    
                    // 全プレイヤーに通知
                    Bukkit.broadcastMessage(resultMessage);
                    
                    // 少し待ってからボールを削除
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ball != null && !ball.isDisplayDead()) {
                                PhysxMc.displayedSphereHolder.destroySpecific(ball);
                            }
                        }
                    }.runTaskLater(PhysxMc.getPlugin(PhysxMc.class), 60L); // 3秒後
                    
                    logger.info("ジャックポット結果: " + player.getName() + " -> スロット" + slot.getSlotId() + 
                               " (" + (slot.isWinSlot() ? "当たり" : "ハズレ") + ")");
                    
                    return true; // 追跡終了
                }
            }
            
            return false; // 追跡継続
        }
    }
}