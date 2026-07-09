package com.faceaisdk.flutter

import android.app.Activity
import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

import com.faceAI.demo.FaceSDKConfig
import com.faceAI.demo.SysCamera.verify.FaceVerificationActivity
import com.faceAI.demo.SysCamera.addFace.AddFaceFeatureActivity
import com.faceAI.demo.SysCamera.verify.LivenessDetectActivity
import com.faceAI.demo.SysCamera.addFace.AddFaceByImageTestDemo

class FaceAiSdkPlugin: FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  private lateinit var channel : MethodChannel
  private lateinit var context: android.content.Context
  private var activity: Activity? = null
  private var pendingResult: MethodChannel.Result? = null
  private var currentFaceId: String? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    FaceSDKConfig.init(context)

    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "face_aisdk_flutter_plugin")
    channel.setMethodCallHandler(this)

    flutterPluginBinding.platformViewRegistry.registerViewFactory(
      "com.faceaisdk/view",
      FaceAiSdkViewFactory(flutterPluginBinding.binaryMessenger)
    )
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    activity?.let { FaceSDKConfig.init(it) }
    android.util.Log.d("FaceAiSdkPlugin", "onMethodCall: ${call.method}, arguments: ${call.arguments}")

    when (call.method) {
      "addFaceBySDKCamera" -> {
        val faceId = call.argument<String>("faceId")
        currentFaceId = faceId
        val performanceMode = call.argument<Int>("addFacePerformanceMode") ?: 0
        val needShowConfirmDialog = call.argument<Boolean>("needShowConfirmDialog") ?: true

        val intent = Intent(activity, AddFaceFeatureActivity::class.java)
         intent.putExtra("USER_FACE_ID_KEY", faceId)
        intent.putExtra(AddFaceFeatureActivity.ADD_FACE_IMAGE_TYPE_KEY,"FACE_VERIFY" )
        intent.putExtra(AddFaceFeatureActivity.NEED_CONFIRM_ADD_FACE, needShowConfirmDialog)

        pendingResult = result
        activity?.startActivityForResult(intent, 10086)
      }
      "faceVerify" -> {
        val faceId = call.argument<String>("faceId")
        currentFaceId = faceId
        val threshold = call.argument<Double>("threshold") ?: 0.84
        val livenessType = call.argument<Int>("livenessType") ?: 1
        val motionLivenessTypes = call.argument<String>("motionLivenessTypes") ?: "1,2,3,4,5"
        val motionLivenessTimeOut = call.argument<Int>("motionLivenessTimeOut") ?: 7
        val motionLivenessSteps = call.argument<Int>("motionLivenessSteps") ?: 2
        val allowMultiFaces = call.argument<Boolean>("allowMultiFaces") ?: false

        val intent = Intent(activity, FaceVerificationActivity::class.java)
        intent.putExtra(FaceVerificationActivity.USER_FACE_ID_KEY, faceId)
        intent.putExtra(FaceVerificationActivity.THRESHOLD_KEY, threshold.toFloat())
        intent.putExtra(FaceVerificationActivity.FACE_LIVENESS_TYPE, livenessType)
        intent.putExtra(FaceVerificationActivity.MOTION_LIVENESS_TYPES, motionLivenessTypes)
        intent.putExtra(FaceVerificationActivity.MOTION_TIMEOUT, motionLivenessTimeOut)
        intent.putExtra(FaceVerificationActivity.MOTION_STEP_SIZE, motionLivenessSteps)
        intent.putExtra(FaceVerificationActivity.ALLOW_MULTI_FACES, allowMultiFaces)

        pendingResult = result
        activity?.startActivityForResult(intent, 10087)
      }
      "livenessVerify" -> {
        val livenessType = call.argument<Int>("livenessType") ?: 1
        val motionLivenessTypes = call.argument<String>("motionLivenessTypes") ?: "1,2,3,4,5"
        val motionLivenessTimeOut = call.argument<Int>("motionLivenessTimeOut") ?: 7
        val motionLivenessSteps = call.argument<Int>("motionLivenessSteps") ?: 2
        val allowMultiFaces = call.argument<Boolean>("allowMultiFaces") ?: false

        val intent = Intent(activity, LivenessDetectActivity::class.java)
        intent.putExtra(FaceVerificationActivity.FACE_LIVENESS_TYPE, livenessType)
        intent.putExtra(FaceVerificationActivity.MOTION_LIVENESS_TYPES, motionLivenessTypes)
        intent.putExtra(FaceVerificationActivity.MOTION_TIMEOUT, motionLivenessTimeOut)
        intent.putExtra(FaceVerificationActivity.MOTION_STEP_SIZE, motionLivenessSteps)
        intent.putExtra(FaceVerificationActivity.ALLOW_MULTI_FACES, allowMultiFaces)

        pendingResult = result
        activity?.startActivityForResult(intent, 10088)
      }
      "addFaceBySDKImage" -> {
        val faceId = call.argument<String>("faceId")
        val imageBase64 = call.argument<String>("imageBase64")
        if (faceId != null && imageBase64 != null && activity != null) {
            try {
                AddFaceByImageTestDemo.addFaceByImageFunc(activity!!, faceId, imageBase64)
                result.success(mapOf("code" to 1, "message" to "Success"))
            } catch (e: Exception) {
                result.error("SDK_ERROR", e.message, null)
            }
        } else {
            result.error("INVALID_ARGUMENT", "faceId or imageBase64 is null", null)
        }
      }
      "deleteFaceFeature" -> {
        val faceId = call.argument<String>("faceId")
        if (faceId != null && activity != null) {
            FaceSDKConfig.deleteFaceVerifyData(activity!!, faceId)
            result.success(mapOf("code" to 1, "msg" to "Success"))
        } else {
            result.error("INVALID_ARGUMENT", "faceId is null", null)
        }
      }
      "switchCamera" -> {
        val cameraId = call.argument<Int>("cameraId") ?: 0
        if (activity != null) {
            FaceSDKConfig.setCameraID(activity!!, cameraId)
            result.success(null)
        } else {
            result.error("NO_ACTIVITY", "Activity is null", null)
        }
      }
      "insertFaceFeature" -> {
        val faceId = call.argument<String>("faceId")
        val feature = call.argument<String>("feature")
        // FaceSDKConfig.insertFaceFeature(activity!!, faceId, feature)
        result.success(mapOf("code" to 1, "msg" to "Success"))
      }
      "getFaceFeature" -> {
        val faceId = call.argument<String>("faceId")
        // val feature = FaceSDKConfig.getFaceFeature(activity!!, faceId)
        result.success(mapOf("code" to 1, "faceFeature" to "mock_feature"))
      }
      "isFaceExist" -> {
        val faceId = call.argument<String>("faceId")
        if (faceId != null) {
            val path = "${activity?.getExternalFilesDir(null)}/face/baseFace/${faceId}.jpg"
            result.success(java.io.File(path).exists())
        } else {
            result.error("INVALID_ARGUMENT", "faceId is null", null)
        }
      }
      "getFaceImageBase64" -> {
        val faceId = call.argument<String>("faceId")
        if (faceId != null && activity != null) {
             val path = "${activity?.getExternalFilesDir(null)}/face/baseFace/${faceId}.jpg"
             val base64 = com.faceAI.demo.base.utils.BitmapUtils.bitmapToBase64(path)
             result.success(base64)
        } else {
            result.error("INVALID_ARGUMENT", "faceId is null", null)
        }
      }
      "goNativeDemoNavi" -> {
        val intent = Intent(activity, Class.forName("com.faceAI.demo.FaceAINaviActivity"))
        activity?.startActivity(intent)
        result.success(null)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (requestCode == 10086 || requestCode == 10087 || requestCode == 10088) {
        val code = data?.getIntExtra("code", 0) ?: 0
        var faceFeature = data?.getStringExtra("faceFeature") ?: ""
        var similarity = data?.getFloatExtra("similarity", 0f) ?: 0f
        var livenessValue = data?.getFloatExtra("livenessValue", 0f) ?: 0f
        val msg = data?.getStringExtra("message") ?: ""
        var faceBase64 = ""

        when (requestCode) {
            10086 -> { // REQ_CODE_ADD_FACE
                faceFeature = data?.getStringExtra("faceFeature") ?: ""
                if (code != 0) {
                    faceBase64 = com.faceAI.demo.base.utils.BitmapUtils.bitmapToBase64(FaceSDKConfig.CACHE_BASE_FACE_DIR + currentFaceId)
                }
            }
            10087 -> { // REQ_CODE_VERIFY
                similarity = data?.getFloatExtra("similarity", 0f) ?: 0f
                livenessValue = data?.getFloatExtra("livenessValue", 0f) ?: 0f
                if (code == 1) {
                    faceBase64 = com.faceAI.demo.base.utils.BitmapUtils.bitmapToBase64(FaceSDKConfig.CACHE_FACE_LOG_DIR + "verifyBitmap")
                }
            }
            10088 -> { // REQ_CODE_LIVENESS
                livenessValue = data?.getFloatExtra("livenessValue", 0f) ?: 0f
                if (code == 10) {
                    faceBase64 = com.faceAI.demo.base.utils.BitmapUtils.bitmapToBase64(FaceSDKConfig.CACHE_FACE_LOG_DIR + "liveBitmap")
                }
            }
        }
        
        val resultMap = mapOf(
            "code" to code,
            "message" to msg,
            "similarity" to similarity.toDouble(),
            "livenessValue" to livenessValue.toDouble(),
            "faceBase64" to faceBase64,
            "faceFeature" to faceFeature
        )
        android.util.Log.d("FaceAiSdkPlugin", "onActivityResult: requestCode=$requestCode, result=$resultMap")
        pendingResult?.success(resultMap)
        pendingResult = null
        return true
    }
    return false
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}
