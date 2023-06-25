package com.kamesuta.physxmc;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import physx.physics.PxPhysics;

/**
 * Minecraft世界で表示可能なPhysxBox
 */
public class DisplayedPhysxBox extends PhysxBox {
    
    /**
     * 表示用のItemDisplay
     */
    public ItemDisplay[] display;
    /**
     * スワップのフェーズ管理
     */
    public int swapPhase = 0;

    public DisplayedPhysxBox(PxPhysics physics, ItemDisplay[] display) {
        super(physics);
        this.display = display;
    }

    /**
     * スワップの1ティック前に呼ぶ
     * @param pos 新しい位置
     */
    public void preSwap(Location pos) {
        display[0].setVisibleByDefault(true);
    }

    /**
     * TPの移動が見えないようにスワップする
     * @param pos 新しい位置
     */
    public void swap(Location pos) {
        display[1].setVisibleByDefault(false);
        display[1].teleport(pos);

        ItemDisplay temp = display[1];
        display[1] = display[0];
        display[0] = temp;
    }
}
