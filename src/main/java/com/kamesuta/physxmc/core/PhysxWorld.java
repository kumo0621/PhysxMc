package com.kamesuta.physxmc.core;

import lombok.Getter;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.util.Vector;
import physx.PxTopLevelFunctions;
import physx.common.PxQuat;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.physics.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kamesuta.physxmc.core.Physx.*;

/**
 * Physxのシーン管理クラス
 */
public class PhysxWorld {

    /**
     * シーン本体
     */
    protected PxScene scene;

    @Getter
    protected SimulationCallback simCallback;

    /**
     * チャンクごとに地形を作ってシーンに挿入する
     */
    public void setUpScene() {
        simCallback = new SimulationCallback();
        scene = createScene();
    }

    /**
     * シーン本体を作る
     *
     * @return
     */
    public PxScene createScene() {
        // create a physics scene
        PxVec3 tmpVec = new PxVec3(0f, -19.62f, 0f);
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setSimulationEventCallback(simCallback);
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
     *
     * @param pos  座標
     * @param quat 回転
     * @return 箱オブジェクト
     */
    public PhysxBox addBox(PxVec3 pos, PxQuat quat) {
        PhysxBox box = new PhysxBox(physics, defaultMaterial, pos, quat);
        scene.addActor(box.getActor());
        return box;
    }

    /**
     * シーンに箱オブジェクトを追加する
     *
     * @param pos         座標
     * @param quat        回転
     * @param boxGeometry 箱の大きさ
     * @return 追加した箱オブジェクト
     */
    public PhysxBox addBox(PxVec3 pos, PxQuat quat, PxBoxGeometry boxGeometry) {
        PhysxBox box = new PhysxBox(physics, defaultMaterial, pos, quat, boxGeometry);
        scene.addActor(box.getActor());
        return box;
    }

    public PhysxBox addBox(PxVec3 pos, PxQuat quat, Map<PxBoxGeometry, PxVec3> boxGeometries, boolean isTrigger) {
        PhysxBox box = new PhysxBox(physics, defaultMaterial, pos, quat, boxGeometries, isTrigger);
        scene.addActor(box.getActor());
        return box;
    }

    /**
     * シーンから箱オブジェクトを取り除いて箱を削除する
     *
     * @param box 　箱オフジェクト
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

    /**
     * シーンの重力を設定する
     *
     * @param gravity
     */
    public void setGravity(Vector gravity) {
        PxVec3 pxGravity = new PxVec3((float) gravity.getX(), (float) gravity.getY(), (float) gravity.getZ());
        scene.setGravity(pxGravity);
        pxGravity.destroy();
    }

    /**
     * 衝突や重なりイベントを検知するためのクラス
     */
    public static class SimulationCallback extends PxSimulationEventCallbackImpl {

        /**
         * 衝突イベントを受信したいメソッドをここに登録する
         */
        public List<TriConsumer<PxActor, PxActor, String>> contactReceivers = new ArrayList<>();

        /**
         * 重なりイベントを受信したいメソッドをここに登録する
         */
        public List<TriConsumer<PxActor, PxActor, String>> triggerReceivers = new ArrayList<>();

        @Override
        public void onContact(PxContactPairHeader pairHeader, PxContactPair pairs, int nbPairs) {
            PxActor actor0 = pairHeader.getActors(0);
            PxActor actor1 = pairHeader.getActors(1);

            //入れるとなぜかクラッシュする
//            if(actor0.getType().equals(PxActorTypeEnum.eRIGID_STATIC) || actor1.getType().equals(PxActorTypeEnum.eRIGID_STATIC)) 
//                return;

            for (int i = 0; i < nbPairs; i++) {
                PxContactPair pair = PxContactPair.arrayGet(pairs.getAddress(), i);
                PxPairFlags events = pair.getEvents();
                String event;
                if (events.isSet(PxPairFlagEnum.eNOTIFY_TOUCH_FOUND)) {
                    event = "TOUCH_FOUND";
                } else if (events.isSet(PxPairFlagEnum.eNOTIFY_TOUCH_LOST)) {
                    event = "TOUCH_LOST";
                } else {
                    event = "OTHER";
                }
                contactReceivers.forEach(pxActorPxActorStringTriConsumer -> pxActorPxActorStringTriConsumer.accept(actor0, actor1, event));
            }
        }

        @Override
        public void onTrigger(PxTriggerPair pairs, int count) {
            for (int i = 0; i < count; i++) {
                PxTriggerPair pair = PxTriggerPair.arrayGet(pairs.getAddress(), i);
                PxActor actor0 = pair.getTriggerActor();
                PxActor actor1 = pair.getOtherActor();

                PxPairFlagEnum status = pair.getStatus();
                String event;
                if (status == PxPairFlagEnum.eNOTIFY_TOUCH_FOUND) {
                    event = "TRIGGER_ENTER";
                } else if (status == PxPairFlagEnum.eNOTIFY_TOUCH_LOST) {
                    event = "TRIGGER_EXIT";
                } else {
                    event = "OTHER";
                }
                triggerReceivers.forEach(pxActorPxActorStringTriConsumer -> pxActorPxActorStringTriConsumer.accept(actor0, actor1, event));
            }
        }
    }
}
