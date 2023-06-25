package com.kamesuta.physxmc;

import physx.PxTopLevelFunctions;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;

import static com.kamesuta.physxmc.Physx.*;

/**
 * Physxのシーン管理クラス
 */
public class PhysxWorld {
    
    protected PxScene scene;

    /**
     * チャンクごとに地形を作ってシーンに挿入する
     */
    public void setUpScene() {
        scene = createScene();
    }

    /**
     * シーン本体を作る
     * @return
     */
    public PxScene createScene() {
        // create a physics scene
        PxVec3 tmpVec = new PxVec3(0f, -19.62f, 0f);
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(tmpVec);
        sceneDesc.setCpuDispatcher(cpuDispatcher);
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        PxScene scene = physics.createScene(sceneDesc);

        tmpVec.destroy();
        sceneDesc.destroy();

        return scene;
    }

    /**
     * シーンオブジェクトを破壊する
     */
    public void destroyScene() {
        if (scene != null) {
            scene.release();
        }
    }

    /**
     * シーンに箱オブジェクトを追加する
     * @param pos 座標
     * @param quat 回転
     * @return 箱オブジェクト
     */
    public PhysxBox addBox(PxVec3 pos, PxQuat quat) {
        PhysxBox box = new PhysxBox(physics);
        scene.addActor(box.createBox(defaultMaterial, pos, quat));
        return box;
    }

    /**
     * シーンに箱オブジェクトを追加する
     * @param pos 座標
     * @param quat 回転
     * @param boxGeometry 箱の大きさ
     * @return 追加した箱オブジェクト
     */
    public PhysxBox addBox(PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry) {
        PhysxBox box = new PhysxBox(physics);
        scene.addActor(box.createBox(defaultMaterial, pos, quat, boxGeometry));
        return box;
    }

    /**
     * シーンから箱オブジェクトを取り除く
     * @param box　箱オフジェクト
     */
    public void removeBox(PhysxBox box) {
        scene.removeActor(box.getActor());
        box.release();
    }

    /**
     * シーンの時間を経過させる
     */
    public void tick() {
        scene.simulate(3f / 60f); // 1 second = 60 frame = 20tick
        scene.fetchResults(true);
    }
}
