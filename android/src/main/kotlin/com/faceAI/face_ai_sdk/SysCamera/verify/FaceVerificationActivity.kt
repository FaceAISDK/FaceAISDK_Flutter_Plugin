package com.faceAI.face_ai_sdk.SysCamera.verify

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector

import com.ai.face.base.baseImage.FaceEmbedding
import com.ai.face.core.engine.FaceAISDKEngine
import com.ai.face.core.utils.FaceAICameraType
import com.ai.face.faceVerify.verify.liveness.FaceLivenessType
import com.faceAI.face_ai_sdk.FaceSDKConfig
import com.faceAI.face_ai_sdk.R
import com.faceAI.face_ai_sdk.SysCamera.search.ImageToast
import com.faceAI.face_ai_sdk.base.AbsBaseActivity
import com.faceAI.face_ai_sdk.SysCamera.camera.FaceCameraXFragment
import com.faceAI.face_ai_sdk.base.utils.BitmapUtils
import com.ai.face.base.view.camera.CameraXBuilder
import com.ai.face.faceVerify.verify.FaceProcessBuilder
import com.ai.face.faceVerify.verify.FaceVerifyUtils
import com.ai.face.faceVerify.verify.ProcessCallBack
import com.ai.face.faceVerify.verify.VerifyStatus.ALIVE_DETECT_TYPE_ENUM
import com.ai.face.faceVerify.verify.VerifyStatus.VERIFY_DETECT_TIPS_ENUM
import com.ai.face.faceVerify.verify.liveness.MotionLivenessMode
import com.faceAI.face_ai_sdk.base.utils.VoicePlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.faceAI.face_ai_sdk.base.view.FaceVerifyCoverView
import com.faceAI.face_ai_sdk.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG
import com.faceAI.face_ai_sdk.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE
import com.faceAI.face_ai_sdk.FaceSDKConfig.CACHE_FACE_LOG_DIR
import com.tencent.mmkv.MMKV

/**
 * 1：1 的人脸识别 + 动作活体检测 接入演示D代码。正式接入集成需要你根据你的业务完善
 * 仅仅需要活体检测参考[LivenessDetectActivity]
 *
 * 移动考勤签到、App免密登录、刷脸授权、刷脸解锁。请熟悉Demo主流程后根据你的业务情况再改造
 * 摄像头管理源码开放了 [FaceCameraXFragment]
 * More: [人脸识别FaceAISDK](https://github.com/FaceAISDK/FaceAISDK_Android)
 *
 * @author FaceAISDK.Service@gmail.com
 */
class FaceVerificationActivity : AbsBaseActivity() {

    companion object {
        const val USER_FACE_ID_KEY = "USER_FACE_ID_KEY"   //1:1 face verify ID KEY
        const val USER_FACE_FEATURE = "USER_FACE_FEATURE" //1:1 face verify feature passed directly
        const val THRESHOLD_KEY = "THRESHOLD_KEY"           //人脸识别通过的阈值
        const val FACE_LIVENESS_TYPE = "FACE_LIVENESS_TYPE"   //活体检测的类型
        const val MOTION_STEP_SIZE = "MOTION_STEP_SIZE"   //动作活体的步骤数
        const val MOTION_TIMEOUT = "MOTION_TIMEOUT"   //动作活体超时数据
        const val MOTION_LIVENESS_TYPES = "MOTION_LIVENESS_TYPES" //动作活体种类
        const val ALLOW_RETRY = "ALLOW_RETRY" //timeout时是否允许retry
    }

    private var faceID: String? = null //你的业务系统中可以唯一定义一个账户的ID，手机号/身份证号等
    private var passedFaceFeature: String? = null //从Flutter直接传递的人脸特征值
    private var verifyThreshold = 0.85f //1:1 人脸识别对比通过的阈值，根据使用场景自行调整
    private var motionStepSize = 1 //动作活体的个数
    private var motionTimeOut = motionStepSize * 3 + 1  //动作超时秒，低端机可以设置长一点
    private var motionLivenessTypes = "1,2,3,4,5" //动作活体种类用英文","隔开； 1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头
    private var faceLivenessType = FaceLivenessType.MOTION  //活体检测类型.20251220  新加 MOTION_COLOR_FLASH炫彩活体
    private var allowRetry = true //timeout时是否允许retry
    private val faceVerifyUtils = FaceVerifyUtils()
    private lateinit var tipsTextView: TextView
    private lateinit var secondTipsTextView: TextView
    private lateinit var faceCoverView: FaceVerifyCoverView
    private lateinit var cameraXFragment: FaceCameraXFragment  //摄像头管理源码，可自行管理摄像头

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI() //炫彩活体全屏显示各种颜色
        setContentView(R.layout.activity_face_verification)
        tipsTextView = findViewById(R.id.tips_view)
        secondTipsTextView = findViewById(R.id.second_tips_view) //次要提示
        faceCoverView = findViewById(R.id.face_cover)
        findViewById<View>(R.id.back).setOnClickListener { finishFaceVerify(0, R.string.face_verify_result_cancel) }

        getIntentParams() //接收三方插件传递的参数，原生开发可以忽略裁剪掉

        initCameraX()
        initFaceVerifyFeature()
    }

    /**
     * 初始化摄像头
     */
    private fun initCameraX() {
        val sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
        val cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, CameraSelector.LENS_FACING_FRONT)
        val degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, windowManager.defaultDisplay.rotation)

        val cameraXBuilder = CameraXBuilder.Builder()
            .setCameraLensFacing(cameraLensFacing) //前后摄像头
            .setLinearZoom(0f)          //焦距范围[0f,1.0f]，根据应用场景自行适当调整焦距（摄像头需支持变焦）炫彩活体请设置为0f
            .setRotation(degree)        //画面旋转角度
            .setCameraSizeHigh(false) //高分辨率远距离也可以工作，但是性能速度会下降.部分定制设备不支持请工程师调试好
            .create()

        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_camerax, cameraXFragment).commit()
    }

    /**
     * 初始化人脸识别底图 人脸特征值
     * //人脸图片和人脸特征向量不方便传递，以及相关法律法规不允许明文传输。注意数据迁移
     */
    private fun initFaceVerifyFeature() {
        // 优先使用Flutter直接传递的人脸特征值
        if (!passedFaceFeature.isNullOrEmpty()) {
            initFaceVerificationParam(passedFaceFeature!!)
        } else {
            //老的数据是float[] 需要转换为faceFeatureOld才能在新版本中使用
            val faceEmbeddingOld = FaceEmbedding.loadEmbedding(baseContext, faceID)
            val faceFeatureOld = FaceAISDKEngine.getInstance(this).faceArray2Feature(faceEmbeddingOld)

            //从本地MMKV读取人脸特征值(2025.11.23版本使用MMKV，老的人脸数据请做好迁移)
            val faceFeature = MMKV.defaultMMKV().decodeString(faceID)
            if (!faceFeature.isNullOrEmpty()) {
                initFaceVerificationParam(faceFeature)
            } else if (!faceFeatureOld.isNullOrEmpty()) {
                initFaceVerificationParam(faceFeatureOld)
                MMKV.defaultMMKV().encode(faceID, faceFeatureOld) //从老的数据迁移到新的MMKV
            } else {
                //根据你的业务进行提示去录入人脸 提取特征，服务器有提前同步到本地
                Toast.makeText(baseContext, "FaceFeature isEmpty ! ", Toast.LENGTH_LONG).show()
            }
        }

        // 去Path 路径读取有没有faceID 对应的处理好的人脸Bitmap，不需要可删除
        if (!faceID.isNullOrEmpty()) {
            val faceFilePath = FaceSDKConfig.CACHE_BASE_FACE_DIR + faceID
            val baseBitmap = BitmapFactory.decodeFile(faceFilePath)
            if (baseBitmap != null) {
                Glide.with(baseContext).load(baseBitmap)
                    .transform(RoundedCorners(33))
                    .into(findViewById<ImageView>(R.id.base_face))
            }
        }
    }

    /**
     * 初始化认证引擎，仅仅需要活体检测参考[LivenessDetectActivity]
     *
     * @param faceFeature 1:1 人脸识别对比的底片特征
     */
    private fun initFaceVerificationParam(faceFeature: String) {
        //建议老的低配设备减少活体检测步骤，加长活体检测 人脸对比时间。
        val faceProcessBuilder = FaceProcessBuilder.Builder(this)
            .setThreshold(verifyThreshold)          //阈值设置，范围限 [0.75,0.95] ,低配摄像头可适量放低，默认0.85
            .setFaceFeature(faceFeature)            //1:1 人脸识别对比的底片人脸特征值字符串
            .setCameraType(FaceAICameraType.SYSTEM_CAMERA)  //相机类型，目前分为3种
            .setCompareDurationTime(3000)           //人脸识别对比时间[3000,6000] 毫秒。相似度低会持续识别比对的时间
            .setLivenessType(faceLivenessType)      //活体检测可以炫彩&动作活体组合，炫彩活体不能在强光下使用
            .setLivenessDetectionMode(MotionLivenessMode.FAST)    //硬件配置低或不需太严格用FAST快速模式，否则用精确模式
            .setMotionLivenessStepSize(motionStepSize)            //随机动作活体的步骤个数[1-2]，SILENT_MOTION和MOTION 才有效
            .setMotionLivenessTimeOut(motionTimeOut)              //动作活体检测，支持设置超时时间 [3,22] 秒 。API 名字0410 修改
            .setMotionLivenessTypes(motionLivenessTypes)          //动作活体种类。1 张张嘴,2 微笑,3 眨眨眼,4 摇摇头,5 点点头
            .setStopVerifyNoFaceRealTime(true)      //没检测到人脸是否立即停止，还是出现过人脸后检测到无人脸停止.(默认false，为后者)
            .setProcessCallBack(object : ProcessCallBack() {
                /**
                 * 1:1 人脸识别 活体检测 对比结束
                 *
                 * @param isMatched     true匹配成功（大于setThreshold）； false 与底片不是同一人
                 * @param similarity    与底片匹配的相似度值
                 * @param livenessValue 静默&炫彩活体分数，仅动作活体可以忽略判断(不同设备的情况可能不一样，建议大于0.75为真人)
                 * @param bitmap        识别完成的时候人脸实时图，金融级别应用可以再次和自己的服务器二次校验
                 */
                override fun onVerifyMatched(isMatched: Boolean, similarity: Float, livenessValue: Float, bitmap: Bitmap) {
                    showVerifyResult(isMatched, similarity, livenessValue, bitmap)
                }

                override fun onColorFlash(color: Int) {
                    faceCoverView.setFlashColor(color) //设置炫彩颜色，不能在室外强光环境使用
                }

                //人脸识别，活体检测过程中的各种提示
                override fun onProcessTips(code: Int) {
                    showFaceVerifyTips(code)
                }

                /**
                 * 动作活体超时倒计时百分比，注意适配低端机反应慢要多点时间
                 */
                override fun onTimeCountDown(percent: Float) {
                    faceCoverView.setProgress(percent)
                }

                override fun onFailed(code: Int, message: String) {
                    Toast.makeText(baseContext, "onFailed error!：$message", Toast.LENGTH_LONG).show()
                }
            }).create()

        faceVerifyUtils.setDetectorParams(faceProcessBuilder)

        cameraXFragment.setOnAnalyzerListener(object : FaceCameraXFragment.onAnalyzeData {
            override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
                //防止在识别过程中关闭页面导致Crash
                if (!isDestroyed && !isFinishing) {
                    //默认演示CameraX的 imageProxy 传入SDK，也支持NV21，Bitmap 类型，你也可以自己管理相机
                    faceVerifyUtils.goVerifyWithImageProxy(imageProxy)
                }
            }
        })
    }

    /**
     * 1:1 人脸识别是否通过
     *
     * 动作活体要有动作配合，必须先动作匹配通过再1：1 匹配
     */
    private var retryTime = 0
    private fun showVerifyResult(isVerifyMatched: Boolean, similarity: Float, livenessValue: Float, bitmap: Bitmap) {
        BitmapUtils.saveCompressBitmap(bitmap, CACHE_FACE_LOG_DIR, "verifyBitmap")  //保存场景图给三方插件使用

        if (isVerifyMatched && livenessValue > 0.8) {
            //2. 相似度>verifyThreshold，并且livenessValue>0.8
            VoicePlayer.getInstance().addPayList(VoicePlayer.localized(baseContext, R.raw.verify_success))
            ImageToast().show(applicationContext, getString(R.string.face_verify_success))
            Handler(Looper.getMainLooper()).postDelayed({
                finishFaceVerify(1, R.string.face_verify_result_success, similarity, livenessValue)
            }, 999)
        } else {
            val code: Int = if (isVerifyMatched) {
                11 //SILENT_LIVENESS_FAILED
            } else {
                2 //VERIFY_FAILED
            }
            VoicePlayer.getInstance().addPayList(VoicePlayer.localized(baseContext, R.raw.verify_failed))
            if (allowRetry) {
                AlertDialog.Builder(this@FaceVerificationActivity)
                    .setTitle(R.string.face_verify_failed_title)
                    .setMessage(R.string.face_verify_failed)
                    .setCancelable(false)
                    .setPositiveButton(R.string.know) { _, _ ->
                        finishFaceVerify(code, R.string.face_verify_result_failed, similarity, livenessValue)
                    }
                    .setNegativeButton(R.string.retry) { _, _ -> faceVerifyUtils.retryVerify() }
                    .show()
            } else {
                faceVerifyUtils.destroyProcess()
                finishFaceVerify(code, R.string.face_verify_result_failed, similarity, livenessValue)
            }
        }
    }

    /**
     * 根据业务和设计师UI交互修改你的 UI，Demo 仅供参考
     *
     * 添加声音提示和动画提示定制也在这里根据返回码进行定制
     * 制作自定义声音：https://www.minimax.io/audio/text-to-speech
     */
    private fun showFaceVerifyTips(actionCode: Int) {
        if (!isDestroyed && !isFinishing) {
            when (actionCode) {
                //检测到多人脸
                VERIFY_DETECT_TIPS_ENUM.FACE_TOO_MANY -> {
                    //防止一真一假人脸作弊,每帧画面检测
                }

                // 动作活体检测完成了
                ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_SUCCESS -> {
                    setMainTips(R.string.keep_face_visible)
                }

                // 动作活体检测超时
                ALIVE_DETECT_TYPE_ENUM.MOTION_LIVE_TIMEOUT -> {
                    if (allowRetry) {
                        AlertDialog.Builder(this)
                            .setMessage(R.string.motion_liveness_detection_time_out)
                            .setCancelable(false)
                            .setPositiveButton(R.string.retry) { _, _ ->
                                retryTime++
                                if (retryTime > 1) {
                                    finishFaceVerify(4, R.string.face_verify_result_timeout)
                                } else {
                                    faceVerifyUtils.retryVerify()
                                }
                            }.show()
                    } else {
                        faceVerifyUtils.destroyProcess()
                        finishFaceVerify(4, R.string.face_verify_result_timeout)
                    }
                }

                // 人脸识别处理中
                VERIFY_DETECT_TIPS_ENUM.ACTION_PROCESS -> {
                    setMainTips(R.string.face_verifying)
                }

                ALIVE_DETECT_TYPE_ENUM.OPEN_MOUSE -> {
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.open_mouse))
                    setMainTips(R.string.repeat_open_close_mouse)
                }

                ALIVE_DETECT_TYPE_ENUM.SMILE -> {
                    setMainTips(R.string.motion_smile)
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.smile))
                }

                ALIVE_DETECT_TYPE_ENUM.BLINK -> {
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.blink))
                    setMainTips(R.string.motion_blink_eye)
                }

                ALIVE_DETECT_TYPE_ENUM.SHAKE_HEAD -> {
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.shake_head))
                    setMainTips(R.string.motion_shake_head)
                }

                ALIVE_DETECT_TYPE_ENUM.NOD_HEAD -> {
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.nod_head))
                    setMainTips(R.string.motion_node_head)
                }

                // 人脸识别活体检测过程切换到后台防止作弊
                VERIFY_DETECT_TIPS_ENUM.PAUSE_VERIFY -> {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.face_verify_pause)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            finishFaceVerify(5, R.string.face_verify_result_no_face_multi_time)
                        }.show()
                }

                //多次没有人脸
                VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY -> {
                    setMainTips(R.string.no_face_or_repeat_switch_screen)
                    AlertDialog.Builder(this)
                        .setMessage(R.string.stop_verify_tips)
                        .setCancelable(false)
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            finishFaceVerify(5, R.string.face_verify_result_no_face_multi_time)
                        }.show()
                }

                // ------------   以下是setSecondTips    -----------------
                VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE -> {
                    setSecondTips(R.string.far_away_tips)
                }

                //人脸太小靠近一点摄像头。炫彩活体检测强制要求靠近屏幕才能把光线打在脸上
                VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL -> {
                    setSecondTips(R.string.come_closer_tips)
                }

                //检测到正常的人脸，尺寸大小OK
                VERIFY_DETECT_TIPS_ENUM.FACE_SIZE_FIT -> {
                    setSecondTips(0)
                }

                VERIFY_DETECT_TIPS_ENUM.ACTION_NO_FACE -> {
                    setSecondTips(R.string.no_face_detected_tips)
                }

                //炫彩活体检测需要人脸更加靠近屏幕摄像头才能通过检测
                VERIFY_DETECT_TIPS_ENUM.COLOR_FLASH_NEED_CLOSER_CAMERA -> {
                    setSecondTips(R.string.color_flash_need_closer_camera)
                }

                //炫彩活体通过
                ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_SUCCESS -> {
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.face_camera))
                    setMainTips(R.string.keep_face_visible)
                }

                ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIVE_FAILED -> {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.color_flash_liveness_failed)
                        .setCancelable(false)
                        .setPositiveButton(R.string.retry) { _, _ ->
                            retryTime++
                            if (retryTime > 1) {
                                finishFaceVerify(8, R.string.color_flash_liveness_failed)
                            } else {
                                faceVerifyUtils.retryVerify()
                            }
                        }.show()
                }

                ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_LIGHT_HIGH -> {
                    val inflater = LayoutInflater.from(this)
                    val dialogView = inflater.inflate(R.layout.dialog_light_warning, null)
                    AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .setPositiveButton(R.string.retry) { _, _ ->
                            retryTime++
                            if (retryTime > 1) {
                                finishFaceVerify(9, R.string.color_flash_light_high)
                            } else {
                                faceVerifyUtils.retryVerify()
                            }
                        }.show()
                }

                ALIVE_DETECT_TYPE_ENUM.COLOR_FLASH_START -> {
                    VoicePlayer.getInstance().play(VoicePlayer.localized(baseContext, R.raw.closer_to_screen))
                }
            }
        }
    }

    /**
     * 主要提示
     */
    private fun setMainTips(resId: Int) {
        tipsTextView.setText(resId)
    }

    /**
     * 第二行提示
     */
    private fun setSecondTips(resId: Int) {
        if (resId == 0) {
            secondTipsTextView.text = ""
            secondTipsTextView.visibility = View.INVISIBLE
        } else {
            secondTipsTextView.visibility = View.VISIBLE
            secondTipsTextView.setText(resId)
        }
    }

    /**
     * 退出页面，释放资源
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishFaceVerify(0, R.string.face_verify_result_cancel)
    }

    /**
     * 资源释放
     */
    override fun onDestroy() {
        super.onDestroy()
        faceVerifyUtils.destroyProcess()
    }

    /**
     * 暂停识别，防止切屏识别，如果你需要退后台不能识别的话
     */
    override fun onStop() {
        super.onStop()
        faceVerifyUtils.pauseProcess()
    }

    // ************************** 下面代码是为了兼容三方插件，原生开放可以忽略   ***********************************

    /**
     * 获取UNI,RN,Flutter三方插件传递的参数,以便在原生代码中生效
     */
    private fun getIntentParams() {
        val intent = intent ?: return
        if (intent.hasExtra(USER_FACE_ID_KEY)) {
            faceID = intent.getStringExtra(USER_FACE_ID_KEY)
        }

        if (intent.hasExtra(USER_FACE_FEATURE)) {
            passedFaceFeature = intent.getStringExtra(USER_FACE_FEATURE)
            Log.d("FaceVerification", "faceFeature received from Flutter, length=${passedFaceFeature?.length ?: 0}")
        } else {
            Log.d("FaceVerification", "faceFeature NOT received from Flutter, will use MMKV lookup")
        }

        if (intent.hasExtra(THRESHOLD_KEY)) {
            verifyThreshold = intent.getFloatExtra(THRESHOLD_KEY, 0.85f)
        }

        if (intent.hasExtra(FACE_LIVENESS_TYPE)) {
            val type = intent.getIntExtra(FACE_LIVENESS_TYPE, 1)
            // 1.动作活体  2.动作+炫彩活体 3.炫彩活体(不能强光环境使用) 4.静默活体检测
            faceLivenessType = when (type) {
                1 -> FaceLivenessType.MOTION
                2 -> FaceLivenessType.COLOR_FLASH_MOTION
                3 -> FaceLivenessType.COLOR_FLASH
                4 -> FaceLivenessType.SILENT_LIVE
                else -> FaceLivenessType.NONE
            }
        }

        if (intent.hasExtra(MOTION_STEP_SIZE)) {
            motionStepSize = intent.getIntExtra(MOTION_STEP_SIZE, 2)
        }
        if (intent.hasExtra(MOTION_TIMEOUT)) {
            motionTimeOut = intent.getIntExtra(MOTION_TIMEOUT, 9)
        }
        if (intent.hasExtra(MOTION_LIVENESS_TYPES)) {
            motionLivenessTypes = intent.getStringExtra(MOTION_LIVENESS_TYPES) ?: motionLivenessTypes
        }
        if (intent.hasExtra(ALLOW_RETRY)) {
            allowRetry = intent.getBooleanExtra(ALLOW_RETRY, true)
        }
    }

    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     */
    private fun finishFaceVerify(code: Int, msgStrRes: Int) {
        finishFaceVerify(code, msgStrRes, 0f, 0f)
    }

    /**
     * 识别结束返回结果, 为了给uniApp UTS插件，RN，Flutter统一的交互返回格式
     */
    private fun finishFaceVerify(code: Int, msgStrRes: Int, similarity: Float, livenessValue: Float) {
        val intent = Intent().putExtra("code", code)
            .putExtra("faceID", faceID)
            .putExtra("msg", getString(msgStrRes))
            .putExtra("livenessValue", livenessValue)
            .putExtra("similarity", similarity)
        setResult(RESULT_OK, intent)
        finish()
    }
}
