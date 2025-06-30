package com.kamesuta.physxmc;

import lombok.Getter;
import lombok.Setter;

/**
 * 物理演算の設定
 */
public class PhysxSetting {

    /**
     * 箱を作る時の既定の密度
     */
    @Getter
    @Setter
    private static float defaultDensity = 1.0f;

    /**
     * プレイヤーがアイテムを持って右クリックすると物理オブジェクトが生成されるデバッグモードのフラグ
     */
    @Getter
    @Setter
    private static boolean debugMode = false;

    /**
     * デバッグモードでプレイヤーがアイテムを投げる速度の大きさ
     */
    @Getter
    @Setter
    private static float throwPower = 20f;

    /**
     * コイン投擲システムの有効/無効フラグ
     */
    @Getter
    @Setter
    private static boolean coinSystemEnabled = false;

    /**
     * コインの投擲力
     */
    @Getter
    @Setter
    private static float coinThrowPower = 15f;

    /**
     * コインのサイズ
     */
    @Getter
    @Setter
    private static float coinSize = 1.2f;

    /**
     * コインの密度（適度な重さ）
     */
    @Getter
    @Setter
    private static float coinDensity = 5.0f;

    /**
     * プッシャーの動作速度
     */
    @Getter
    @Setter
    private static double pusherSpeed = 0.02;
}
