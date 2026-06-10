package com.faceAI.sdk.SysCamera.verify;

import static com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM.*;
import static com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM.*;
import static com.faceAI.sdk.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.sdk.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.sdk.SysCamera.verify.VerifyStatue.*;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;

import com.ai.face.core.utils.FaceAICameraType;
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType;
import com.faceAI.sdk.FaceSDKConfig;
import com.faceAI.sdk.R;
import com.faceAI.sdk.SysCamera.search.ImageToast;
import com.faceAI.sdk.base.AbsBaseActivity;
import com.faceAI.sdk.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.sdk.base.utils.BitmapUtils;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.faceVerify.verify.FaceProcessBuilder;
import com.ai.face.faceVerify.verify.FaceVerifyUtils;
import com.ai.face.faceVerify.verify.ProcessCallBack;
import com.faceAI.sdk.base.utils.TTSPlayer;
import com.faceAI.sdk.base.view.FaceCoverView;
import com.tencent.mmkv.MMKV;

/**
 * 1：1 的人脸识别 + 动作活体检测 接入演示代码
 */
public class FaceVerificationActivity extends AbsBaseActivity {
    public static final String USER_FACE_ID_KEY = "USER_FACE_ID_KEY";
    public static final String THRESHOLD_KEY = "THRESHOLD_KEY";
    public static final String FACE_LIVENESS_TYPE = "FACE_LIVENESS_TYPE";
    public static final String MOTION_STEP_SIZE = "MOTION_STEP_SIZE";
    public static final String MOTION_TIMEOUT = "MOTION_TIMEOUT";
    public static final String MOTION_LIVENESS_TYPES = "MOTION_LIVENESS_TYPES";
    public static final String ALLOW_MULTI_FACES = "ALLOW_MULTI_FACES";

    public static final String USER_FACE_FEATURE = "USER_FACE_FEATURE";

    private boolean allowMultiFaces = true;

    private String faceID;
    private String faceFeature;
    private float verifyThreshold = 0.84f;

    private FaceLivenessType faceLivenessType = FaceLivenessType.MOTION;

    private int motionStepSize = 2;
    private int motionTimeOut = motionStepSize * 3;
    private String motionLivenessTypes = "1,2,3,4,5";

    private final FaceVerifyUtils faceVerifyUtils = new FaceVerifyUtils();
    private FaceCoverView faceCoverView;
    private FaceCameraXFragment cameraXFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_face_verification);

        faceCoverView = findViewById(R.id.face_cover);
        findViewById(R.id.back).setOnClickListener(v -> finishFaceVerify(DEFAULT, R.string.face_verify_result_cancel));

        getIntentParams();

        initCameraX();
        initFaceVerifyFeature();
    }

    private void initCameraX() {
        MMKV mmkv = MMKV.defaultMMKV();
        int cameraLensFacing = mmkv.decodeInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT);
        int degree = mmkv.decodeInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing)
                .setLinearZoom(0f)
                .setRotation(degree)
                .setCameraSizeHigh(false)
                .create();

        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment).commit();
    }

    private void initFaceVerifyFeature() {
//        String faceFeature = MMKV.defaultMMKV().decodeString(faceID);
        if (!TextUtils.isEmpty(faceFeature)) {
            initFaceVerificationParam(faceFeature);
        } else {
            TTSPlayer.getInstance().playTTS(R.string.no_face_feature);
            Toast.makeText(getBaseContext(), R.string.no_face_feature, Toast.LENGTH_LONG).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finishFaceVerify(NO_BASE_FACE_FEATURE, R.string.no_face_feature, 0, 0);
            }, 1111);
        }
    }

    private void initFaceVerificationParam(String faceFeature) {
        FaceProcessBuilder faceProcessBuilder = new FaceProcessBuilder.Builder(this)
                .setThreshold(verifyThreshold)
                .setFaceFeature(faceFeature)
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
                .setCompareDurationTime(3000)
                .setLivenessType(faceLivenessType)
                .setMotionLivenessStepSize(motionStepSize)
                .setMotionLivenessTimeOut(motionTimeOut)
                .setMotionLivenessTypes(motionLivenessTypes)
                .setStopVerifyNoFaceRealTime(true)
                .setProcessCallBack(new ProcessCallBack() {
                    @Override
                    public void onVerifyMatched(boolean isMatched, float similarity, float livenessValue, Bitmap bitmap) {
                        showVerifyResult(isMatched, similarity, livenessValue, bitmap);
                    }

                    @Override
                    public void onColorFlash(int color) {
                        faceCoverView.setFlashColor(color);
                    }

                    @Override
                    public void onProcessTips(int code) {
                        showFaceVerifyTips(code);
                    }

                    @Override
                    public void onTimeCountDown(float percent) {
                        faceCoverView.setProgress(percent);
                    }

                    @Override
                    public void onFailed(int code, String message) {
                        Toast.makeText(getBaseContext(), "onFailed error!：" + message, Toast.LENGTH_LONG).show();
                    }

                }).create();

        faceVerifyUtils.setDetectorParams(faceProcessBuilder);

        cameraXFragment.setOnAnalyzerListener(imageProxy -> {
            if (!isDestroyed() && !isFinishing()) {
                faceVerifyUtils.goVerifyWithImageProxy(imageProxy);
            }
        });
    }

    private int retryTime = 0;

    private void showVerifyResult(boolean isVerifyMatched, float similarity, float livenessValue, Bitmap bitmap) {
        BitmapUtils.saveCompressBitmap(bitmap, FaceSDKConfig.CACHE_FACE_LOG_DIR, "verifyBitmap");

        if (isVerifyMatched && (livenessValue > 0.8 || faceLivenessType.equals(FaceLivenessType.NONE))) {
            TTSPlayer.getInstance().playTTS(R.string.face_verify_success);
            new ImageToast().show(getApplicationContext(), getString(R.string.face_verify_success));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finishFaceVerify(VERIFY_SUCCESS, R.string.face_verify_result_success, similarity, livenessValue);
            }, 500);
        } else {
            int code;
            if (isVerifyMatched) {
                code = SILENT_LIVENESS_FAILED;
            } else {
                code = VERIFY_FAILED;
            }
            TTSPlayer.getInstance().playTTS(R.string.face_verify_failed);

            new AlertDialog.Builder(FaceVerificationActivity.this).setTitle(R.string.face_verify_failed_title).setMessage(R.string.face_verify_failed).setCancelable(false).setPositiveButton(R.string.know, (dialogInterface, i) -> {
                finishFaceVerify(code, R.string.face_verify_result_failed, similarity, livenessValue);
            }).setNegativeButton(R.string.retry, (dialog, which) -> faceVerifyUtils.retryVerify()).show();
        }
    }

    private void showFaceVerifyTips(int actionCode) {
        if (!isDestroyed() && !isFinishing()) {
            switch (actionCode) {
                case FACE_TOO_MANY:
                    if (!allowMultiFaces) {
                        Toast.makeText(this, R.string.multiple_faces_tips, Toast.LENGTH_LONG).show();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            finishFaceVerify(NOT_ALLOW_MULTI_FACES, R.string.multiple_faces_tips);
                        }, 999);
                    }
                    break;
                case MOTION_LIVE_SUCCESS:
                    setMainTips(R.string.keep_face_visible);
                    break;
                case MOTION_LIVE_TIMEOUT:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.motion_liveness_detection_time_out)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(MOTION_LIVENESS_TIMEOUT, R.string.face_verify_result_timeout);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;
                case ACTION_PROCESS:
                    setMainTips(R.string.face_verifying);
                    break;
                case OPEN_MOUSE:
                    TTSPlayer.getInstance().playTTS(R.string.repeat_open_close_mouse);
                    setMainTips(R.string.repeat_open_close_mouse);
                    break;
                case SMILE:
                    setMainTips(R.string.motion_smile);
                    TTSPlayer.getInstance().playTTS(R.string.motion_smile);
                    break;
                case BLINK:
                    TTSPlayer.getInstance().playTTS(R.string.motion_blink_eye);
                    setMainTips(R.string.motion_blink_eye);
                    break;
                case SHAKE_HEAD:
                    TTSPlayer.getInstance().playTTS(R.string.motion_shake_head);
                    setMainTips(R.string.motion_shake_head);
                    break;
                case NOD_HEAD:
                    TTSPlayer.getInstance().playTTS(R.string.motion_node_head);
                    setMainTips(R.string.motion_node_head);
                    break;
                case PAUSE_VERIFY:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.face_verify_pause)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                finishFaceVerify(NO_FACE_MULTI, R.string.face_verify_result_no_face_multi_time);
                            }).show();
                    break;
                case NO_FACE_REPEATEDLY:
                    setMainTips(R.string.no_face_or_repeat_switch_screen);
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.stop_verify_tips)
                            .setCancelable(false)
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                finishFaceVerify(NO_FACE_MULTI, R.string.face_verify_result_no_face_multi_time);
                            }).show();
                    break;
                case FACE_TOO_LARGE:
                    setSecondTips(R.string.far_away_tips);
                    break;
                case FACE_TOO_SMALL:
                    setSecondTips(R.string.come_closer_tips);
                    break;
                case FACE_SIZE_FIT:
                    setSecondTips(0);
                    break;
                case ACTION_NO_FACE:
                    setSecondTips(R.string.no_face_detected_tips);
                    break;
                case COLOR_FLASH_NEED_CLOSER_CAMERA:
                    setSecondTips(R.string.color_flash_need_closer_camera);
                    TTSPlayer.getInstance().playTTS(R.string.color_flash_need_closer_camera, TTSPlayer.PlayMode.DROP_IF_BUSY);
                    break;
                case COLOR_FLASH_LIVE_SUCCESS:
                    TTSPlayer.getInstance().playTTS(R.string.keep_face_visible);
                    setMainTips(R.string.keep_face_visible);
                    break;
                case COLOR_FLASH_LIVE_FAILED:
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.color_flash_liveness_failed)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(COLOR_LIVENESS_FAILED, R.string.color_flash_liveness_failed);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;
                case COLOR_FLASH_LIGHT_HIGH:
                    LayoutInflater inflater = LayoutInflater.from(this);
                    View dialogView = inflater.inflate(R.layout.dialog_light_warning, null);
                    new AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry, (dialogInterface, i) -> {
                                retryTime++;
                                if (retryTime > 1) {
                                    finishFaceVerify(COLOR_LIVENESS_LIGHT_TOO_HIGH, R.string.color_flash_light_high);
                                } else {
                                    faceVerifyUtils.retryVerify();
                                }
                            }).show();
                    break;
            }
        }
    }

    private void setMainTips(int resId) {
        faceCoverView.setTipsText(resId);
    }

    private void setSecondTips(int resId) {
        faceCoverView.setSecondTipsText(resId);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishFaceVerify(DEFAULT, R.string.face_verify_result_cancel);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceVerifyUtils.destroyProcess();
    }

    protected void onStop() {
        super.onStop();
        faceVerifyUtils.pauseProcess();
    }

    private void getIntentParams() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(USER_FACE_ID_KEY)) {
                faceID = intent.getStringExtra(USER_FACE_ID_KEY);
            }
            if (intent.hasExtra(USER_FACE_FEATURE)) {
                faceFeature = intent.getStringExtra(USER_FACE_FEATURE);
            }
            if (faceID == null && faceFeature == null) {
                Toast.makeText(this, R.string.input_face_id_tips, Toast.LENGTH_LONG).show();
            }
            if (intent.hasExtra(THRESHOLD_KEY)) {
                verifyThreshold = intent.getFloatExtra(THRESHOLD_KEY, 0.85f);
            }
            if (intent.hasExtra(ALLOW_MULTI_FACES)) {
                allowMultiFaces = intent.getBooleanExtra(ALLOW_MULTI_FACES, true);
            }
            if (intent.hasExtra(FACE_LIVENESS_TYPE)) {
                int type = intent.getIntExtra(FACE_LIVENESS_TYPE, 1);
                switch (type) {
                    case 1: faceLivenessType = FaceLivenessType.MOTION; break;
                    case 2: faceLivenessType = FaceLivenessType.COLOR_FLASH_MOTION; break;
                    case 3: faceLivenessType = FaceLivenessType.COLOR_FLASH; break;
                    case 4: faceLivenessType = FaceLivenessType.SILENT_LIVE; break;
                    default: faceLivenessType = FaceLivenessType.NONE;
                }
            }
            if (intent.hasExtra(MOTION_STEP_SIZE)) {
                motionStepSize = intent.getIntExtra(MOTION_STEP_SIZE, 2);
            }
            if (intent.hasExtra(MOTION_TIMEOUT)) {
                motionTimeOut = intent.getIntExtra(MOTION_TIMEOUT, 9);
            }
            if (intent.hasExtra(MOTION_LIVENESS_TYPES)) {
                motionLivenessTypes = intent.getStringExtra(MOTION_LIVENESS_TYPES);
            }
        }
    }

    private void finishFaceVerify(int code, int msgStrRes) {
        finishFaceVerify(code, msgStrRes, 0f, 0f);
    }

    private void finishFaceVerify(int code, int msgStrRes, float similarity, float livenessValue) {
        Intent intent = new Intent().putExtra("code", code)
                .putExtra("faceID", faceID)
                .putExtra("msg", getString(msgStrRes))
                .putExtra("livenessValue", livenessValue)
                .putExtra("similarity", similarity);
        setResult(RESULT_OK, intent);
        finish();
    }
}
