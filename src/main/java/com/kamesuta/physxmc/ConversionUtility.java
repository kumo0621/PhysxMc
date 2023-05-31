package com.kamesuta.physxmc;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ConversionUtility {

    public static Vector3f convertToEulerAngles(Quaternionf quaternion) {
        Vector3f euler = new Vector3f();
        quaternion.getEulerAnglesXYZ(euler);

        // ラジアンから度に変換
        euler.mul((float) (180.0 / Math.PI));

        return euler;
    }

    public static Quaternionf convertToQuaternion(double eulerX, double eulerY, double eulerZ) {
        Vector3f angles = new Vector3f((float) Math.toRadians(eulerX), (float) Math.toRadians(eulerY), (float) Math.toRadians(eulerZ));
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotationXYZ(angles.x, angles.y, angles.z);

        return quaternion;
    }

    public static float[] convertToYawPitch(double eulerX, double eulerY, double eulerZ) {
        double yaw = eulerY;
        double pitch = -eulerX;

        // 正規化
        yaw = normalizeAngle(yaw);
        pitch = normalizeAngle(pitch);

        return new float[]{(float) yaw, (float) pitch};
    }

    public static float[] convertToEulerAngles(double yaw, double pitch) {
        double eulerX = -pitch;
        double eulerY = yaw;
        double eulerZ = 0.0;

        // 正規化
        eulerX = normalizeAngle(eulerX);
        eulerY = normalizeAngle(eulerY);

        return new float[]{(float) eulerX, (float) eulerY, (float) eulerZ};
    }

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
