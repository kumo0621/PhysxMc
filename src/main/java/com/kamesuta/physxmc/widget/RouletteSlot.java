package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.logging.Logger;

/**
 * ジャックポットルーレットの穴（スロット）
 */
public class RouletteSlot {
    
    @Getter
    private final int slotId;
    @Getter
    private final Location location;
    @Getter
    private final Material material;
    @Getter
    private final boolean isWinSlot;
    @Getter
    private final double angle; // 円盤上の角度
    
    private DisplayedPhysxBox slotBox; // スロットの物理オブジェクト
    private final Logger logger;
    
    // スロットのサイズ設定
    private static final double SLOT_WIDTH = 1.5;
    private static final double SLOT_DEPTH = 1.0;
    private static final double SLOT_HEIGHT = 0.5;
    private static final double DETECTION_RADIUS = 1.2; // ボール検出半径
    
    /**
     * ルーレットスロットを作成
     * @param slotId スロットID
     * @param location スロット位置
     * @param material スロット材質
     * @param isWinSlot 当たりスロットかどうか
     * @param angle 円盤上の角度
     */
    public RouletteSlot(int slotId, Location location, Material material, boolean isWinSlot, double angle) {
        this.slotId = slotId;
        this.location = location.clone();
        this.material = material;
        this.isWinSlot = isWinSlot;
        this.angle = angle;
        this.logger = PhysxMc.getPlugin(PhysxMc.class).getLogger();
        
        createSlot();
    }
    
    /**
     * スロットの物理オブジェクトを作成
     */
    private void createSlot() {
        Vector slotSize = new Vector(SLOT_WIDTH, SLOT_HEIGHT, SLOT_DEPTH);
        ItemStack slotItem = new ItemStack(material);
        List<Vector> offsets = List.of(new Vector()); // 単一のディスプレイ
        
        slotBox = PhysxMc.displayedBoxHolder.createDisplayedBox(
            location.clone(),
            slotSize,
            slotItem,
            offsets,
            1000.0f, // 重い密度で動かないように
            false    // プッシャーではない
        );
        
        if (slotBox != null) {
            slotBox.makeKinematic(true); // キネマティック制御で固定
            logger.info("スロット" + slotId + "を作成: " + (isWinSlot ? "当たり" : "通常") + 
                       " at " + String.format("%.1f,%.1f,%.1f", location.getX(), location.getY(), location.getZ()));
        } else {
            logger.warning("スロット" + slotId + "の作成に失敗しました");
        }
    }
    
    /**
     * 毎tick更新処理
     */
    public void update() {
        // 現在は特に更新処理はなし
        // 将来的にエフェクトや音声などを追加する可能性
    }
    
    /**
     * 指定された位置がスロット内にあるかチェック
     * @param checkLocation チェックする位置
     * @return スロット内にある場合true
     */
    public boolean isLocationInSlot(Location checkLocation) {
        if (!location.getWorld().equals(checkLocation.getWorld())) {
            return false;
        }
        
        double distance = location.distance(checkLocation);
        return distance <= DETECTION_RADIUS && checkLocation.getY() <= location.getY() + SLOT_HEIGHT;
    }
    
    /**
     * スロットが有効かどうか
     */
    public boolean isValid() {
        return slotBox != null && !slotBox.isDisplayDead();
    }
    
    /**
     * スロットを破壊
     */
    public void destroy() {
        if (slotBox != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(slotBox);
            slotBox = null;
        }
        logger.info("スロット" + slotId + "を破壊しました");
    }
    
    /**
     * 結果メッセージを取得
     * @param playerName プレイヤー名
     * @return 結果メッセージ
     */
    public String getResultMessage(String playerName) {
        if (isWinSlot) {
            return "🎉 " + playerName + "さんが当たり穴" + slotId + "に入りました！ジャックポット獲得！";
        } else {
            return "💔 " + playerName + "さんは穴" + slotId + "に入りました。残念！";
        }
    }
    
    /**
     * スロット情報を取得
     */
    public String getSlotInfo() {
        return String.format("スロット%d: %s (%.1f,%.1f,%.1f) 角度%.1f°", 
                           slotId, 
                           isWinSlot ? "当たり" : "通常",
                           location.getX(), 
                           location.getY(), 
                           location.getZ(),
                           Math.toDegrees(angle));
    }
}