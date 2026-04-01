package com.faceAI.face_ai_sdk

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import com.faceAI.face_ai_sdk.FaceSDKConfig
import com.faceAI.face_ai_sdk.SysCamera.verify.FaceVerificationActivity
import com.faceAI.face_ai_sdk.SysCamera.verify.LivenessDetectActivity
import com.faceAI.face_ai_sdk.SysCamera.addFace.AddFaceFeatureActivity
import java.io.ByteArrayOutputStream
import java.io.File

class FaceAiSdkPlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware,
    PluginRegistry.ActivityResultListener {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var pendingResult: Result? = null
    private var pendingFormat: String = "base64"
    private var isSDKInitialized = false

    companion object {
        private const val VERIFICATION_REQUEST = 1001
        private const val LIVENESS_REQUEST = 1002
        private const val ENROLL_REQUEST = 1003
        private const val ADD_FACE_REQUEST = 1004
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "face_ai_sdk")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityBinding?.removeActivityResultListener(this)
        activity = null
        activityBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activityBinding?.removeActivityResultListener(this)
        activity = null
        activityBinding = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "initializeSDK" -> handleInitializeSDK(call, result)
            "startVerification" -> handleStartVerification(call, result)
            "startLiveness" -> handleStartLiveness(call, result)
            "startEnroll" -> handleStartEnroll(call, result)
            "addFace" -> handleAddFace(call, result)
            else -> result.notImplemented()
        }
    }

    private fun handleInitializeSDK(call: MethodCall, result: Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
            return
        }
        try {
            FaceSDKConfig.init(currentActivity.applicationContext)
            isSDKInitialized = true
            result.success("SDK initialized successfully")
        } catch (e: Exception) {
            result.error("INIT_FAILED", "Failed to initialize SDK: ${e.message}", null)
        }
    }

    private fun requireInitialized(result: Result): Boolean {
        if (!isSDKInitialized) {
            result.error("NOT_INITIALIZED", "SDK not initialized, call initializeSDK first", null)
            return false
        }
        return true
    }

    private fun handleStartVerification(call: MethodCall, result: Result) {
        val currentActivity = activity ?: run {
            result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
            return
        }
        if (!requireInitialized(result)) return
        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "Another operation is in progress", null)
            return
        }

        val faceId = call.argument<String>("faceId")
        if (faceId == null) {
            result.error("INVALID_ARGS", "faceId is required", null)
            return
        }

        pendingResult = result
        pendingFormat = call.argument<String>("format") ?: "base64"

        val intent = Intent(currentActivity, FaceVerificationActivity::class.java).apply {
            putExtra(FaceVerificationActivity.USER_FACE_ID_KEY, faceId)
            putExtra(FaceVerificationActivity.THRESHOLD_KEY, (call.argument<Double>("threshold") ?: 0.85).toFloat())
            putExtra(FaceVerificationActivity.FACE_LIVENESS_TYPE, call.argument<Int>("livenessType") ?: 0)
            putExtra(FaceVerificationActivity.MOTION_STEP_SIZE, call.argument<Int>("motionStepSize") ?: 1)
            putExtra(FaceVerificationActivity.MOTION_TIMEOUT, call.argument<Int>("motionTimeout") ?: 10)
            putExtra(FaceVerificationActivity.MOTION_LIVENESS_TYPES, call.argument<String>("motionTypes") ?: "1,2,3")
        }
        currentActivity.startActivityForResult(intent, VERIFICATION_REQUEST)
    }

    private fun handleStartLiveness(call: MethodCall, result: Result) {
        val currentActivity = activity ?: run {
            result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
            return
        }
        if (!requireInitialized(result)) return
        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "Another operation is in progress", null)
            return
        }

        pendingResult = result
        pendingFormat = call.argument<String>("format") ?: "base64"

        val intent = Intent(currentActivity, LivenessDetectActivity::class.java).apply {
            putExtra(LivenessDetectActivity.FACE_LIVENESS_TYPE, call.argument<Int>("livenessType") ?: 1)
            putExtra(LivenessDetectActivity.MOTION_STEP_SIZE, call.argument<Int>("motionStepSize") ?: 1)
            putExtra(LivenessDetectActivity.MOTION_TIMEOUT, call.argument<Int>("motionTimeout") ?: 10)
            putExtra(LivenessDetectActivity.MOTION_LIVENESS_TYPES, call.argument<String>("motionTypes") ?: "1,2,3")
        }
        currentActivity.startActivityForResult(intent, LIVENESS_REQUEST)
    }

    private fun handleStartEnroll(call: MethodCall, result: Result) {
        val currentActivity = activity ?: run {
            result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
            return
        }
        if (!requireInitialized(result)) return
        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "Another operation is in progress", null)
            return
        }

        val faceId = call.argument<String>("faceId")
        if (faceId == null) {
            result.error("INVALID_ARGS", "faceId is required", null)
            return
        }

        pendingResult = result
        pendingFormat = call.argument<String>("format") ?: "base64"

        val intent = Intent(currentActivity, AddFaceFeatureActivity::class.java).apply {
            putExtra(FaceVerificationActivity.USER_FACE_ID_KEY, faceId)
            putExtra(AddFaceFeatureActivity.ADD_FACE_IMAGE_TYPE_KEY, AddFaceFeatureActivity.AddFaceImageTypeEnum.FACE_VERIFY.name)
            putExtra(AddFaceFeatureActivity.NEED_CONFIRM_ADD_FACE, false)
        }
        currentActivity.startActivityForResult(intent, ENROLL_REQUEST)
    }

    private fun handleAddFace(call: MethodCall, result: Result) {
        val currentActivity = activity ?: run {
            result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
            return
        }
        if (!requireInitialized(result)) return
        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "Another operation is in progress", null)
            return
        }

        val faceId = call.argument<String>("faceId")
        if (faceId == null) {
            result.error("INVALID_ARGS", "faceId is required", null)
            return
        }

        pendingResult = result
        pendingFormat = call.argument<String>("format") ?: "base64"

        val intent = Intent(currentActivity, AddFaceFeatureActivity::class.java).apply {
            putExtra(FaceVerificationActivity.USER_FACE_ID_KEY, faceId)
            putExtra(AddFaceFeatureActivity.ADD_FACE_IMAGE_TYPE_KEY, AddFaceFeatureActivity.AddFaceImageTypeEnum.FACE_SEARCH.name)
            putExtra(AddFaceFeatureActivity.NEED_CONFIRM_ADD_FACE, false)
        }
        currentActivity.startActivityForResult(intent, ADD_FACE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode !in listOf(VERIFICATION_REQUEST, LIVENESS_REQUEST, ENROLL_REQUEST, ADD_FACE_REQUEST)) {
            return false
        }

        val result = pendingResult ?: return false
        pendingResult = null

        if (resultCode != Activity.RESULT_OK || data == null) {
            result.success(mapOf("code" to 0, "msg" to "cancelled"))
            return true
        }

        val faceImageStr = loadFaceLogImage(requestCode)

        when (requestCode) {
            VERIFICATION_REQUEST -> {
                val resultMap = hashMapOf<String, Any?>(
                    "code" to data.getIntExtra("code", 0),
                    "faceID" to (data.getStringExtra("faceID") ?: ""),
                    "msg" to (data.getStringExtra("msg") ?: ""),
                    "similarity" to data.getFloatExtra("similarity", 0f).toDouble(),
                    "livenessValue" to data.getFloatExtra("livenessValue", 0f).toDouble(),
                    "faceImage" to faceImageStr,
                )
                result.success(resultMap)
            }
            LIVENESS_REQUEST -> {
                val resultMap = hashMapOf<String, Any?>(
                    "code" to data.getIntExtra("code", 0),
                    "msg" to (data.getStringExtra("msg") ?: ""),
                    "livenessValue" to data.getFloatExtra("livenessValue", 0f).toDouble(),
                    "faceImage" to faceImageStr,
                )
                result.success(resultMap)
            }
            ENROLL_REQUEST, ADD_FACE_REQUEST -> {
                val resultMap = hashMapOf<String, Any?>(
                    "code" to data.getIntExtra("code", 0),
                    "faceID" to (data.getStringExtra("faceID") ?: ""),
                    "msg" to (data.getStringExtra("msg") ?: ""),
                    "faceImage" to faceImageStr,
                )
                result.success(resultMap)
            }
        }
        return true
    }

    private fun loadFaceLogImage(requestCode: Int): String? {
        val ctx = activity?.applicationContext ?: return null
        val logDir = ctx.filesDir.path + "/FaceAI/Log/"

        val fileName = when (requestCode) {
            VERIFICATION_REQUEST -> "verifyBitmap"
            LIVENESS_REQUEST -> "liveBitmap"
            else -> return null
        }
        val targetFile = File(logDir, fileName)
        if (!targetFile.exists()) return null

        return when (pendingFormat) {
            "filePath" -> targetFile.absolutePath
            else -> {
                val bitmap = BitmapFactory.decodeFile(targetFile.absolutePath) ?: return null
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()
                bitmap.recycle()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }
    }
}
