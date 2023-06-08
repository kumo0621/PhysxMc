package com.kamesuta.physxmc;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 角度変換用のユーティリティクラス
 */
public class ConversionUtility {

    /**
     * クォータニオンをオイラー角に変換
     * @param quaternion クォータニオン
     * @return オイラー角
     */
    public static Vector3f convertToEulerAngles(Quaternionf quaternion) {
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesXYZ(euler);
        
        euler.mul((float) (180.0 / Math.PI));

        return euler;
    }

    /**
     * オイラー角をクォータニオンに変換
     * @return クォータニオン
     */
    public static Quaternionf convertToQuaternion(double eulerX, double eulerY, double eulerZ) {
        Vector3f angles = new Vector3f((float) Math.toRadians(eulerX), (float) Math.toRadians(eulerY), (float) Math.toRadians(eulerZ));
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotationXYZ(angles.x, angles.y, angles.z);

        return quaternion;
    }

    /**
     * オイラー角をyaw/pitchに変換
     * @return yaw/pitch
     */
    public static float[] convertToYawPitch(double eulerX, double eulerY, double eulerZ) {
        double yaw = eulerY;
        double pitch = -eulerX;
        
        yaw = normalizeAngle(yaw);
        pitch = normalizeAngle(pitch);

        return new float[]{(float) yaw, (float) pitch};
    }

    /**
     * yaw/pitchをオイラー角に変換
     * @return オイラー角
     */
    public static Vector3f convertToEulerAngles(double yaw, double pitch) {
        double eulerX = -pitch;
        double eulerY = yaw;
        double eulerZ = 0.0;
        
        eulerX = normalizeAngle(eulerX);
        eulerY = normalizeAngle(eulerY);

        return new Vector3f((float) eulerX, (float) eulerY, (float) eulerZ);
    }

    /**
     * 角度を正規化
     * @return 正規化された角度
     */
    public static double normalizeAngle(double angle) {
        angle %= 360.0;

        if (angle < -180.0) {
            angle += 360.0;
        } else if (angle > 180.0) {
            angle -= 360.0;
        }

        return angle;
    }
}
