package com.kamesuta.physxmc.widget;

import com.kamesuta.physxmc.PhysxMc;
import com.kamesuta.physxmc.wrapper.DisplayedPhysxBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 傾斜板(ランプ)を管理するクラス
 */
public class RampManager {

    private final List<DisplayedPhysxBox> ramps = new ArrayList<>();

    /**
     * ランプを生成してシーンに追加
     *
     * @param location   生成位置 (プレイヤー位置基準)
     * @param pitchDeg   傾斜角度(度)
     * @param width      幅(ブロック数)
     * @param length     長さ(ブロック数)
     * @param thickness  厚み(ブロック数)
     * @param material   表面ブロック
     * @return 生成されたDisplayedPhysxBox
     */
    public DisplayedPhysxBox createRamp(Location location, double pitchDeg, double width, double length, double thickness, Material material) {
        // ランプ中心位置をコピーしてピッチのみ設定
        Location center = location.clone();
        center.setPitch((float) pitchDeg);

        Vector scale = new Vector(width, thickness, length);
        ItemStack item = new ItemStack(material);

        DisplayedPhysxBox ramp = PhysxMc.displayedBoxHolder.createDisplayedBox(center, scale, item, List.of(new Vector()));
        // 動かないランプとして設定
        ramp.makeKinematic(true);
        ramps.add(ramp);
        return ramp;
    }

    /**
     * すべてのランプを更新(無効化されたものを除去)
     */
    public void update() {
        Iterator<DisplayedPhysxBox> iterator = ramps.iterator();
        while (iterator.hasNext()) {
            DisplayedPhysxBox ramp = iterator.next();
            if (ramp.isDisplayDead()) {
                PhysxMc.displayedBoxHolder.destroySpecific(ramp);
                iterator.remove();
            }
        }
    }

    /**
     * 付近のランプを削除
     *
     * @param location    基準位置
     * @param maxDistance 最大距離
     * @return 削除できたか
     */
    public boolean removeNearestRamp(Location location, double maxDistance) {
        DisplayedPhysxBox nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (DisplayedPhysxBox ramp : ramps) {
            double distance = ramp.getLocation().distance(location);
            if (distance < nearestDistance && distance <= maxDistance) {
                nearestDistance = distance;
                nearest = ramp;
            }
        }

        if (nearest != null) {
            PhysxMc.displayedBoxHolder.destroySpecific(nearest);
            ramps.remove(nearest);
            return true;
        }
        return false;
    }

    /**
     * 全ランプを削除
     */
    public void destroyAll() {
        for (DisplayedPhysxBox ramp : ramps) {
            PhysxMc.displayedBoxHolder.destroySpecific(ramp);
        }
        ramps.clear();
    }

    /**
     * ランプ数取得
     */
    public int getRampCount() {
        return ramps.size();
    }
} 