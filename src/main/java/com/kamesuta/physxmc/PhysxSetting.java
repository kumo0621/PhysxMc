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
}
