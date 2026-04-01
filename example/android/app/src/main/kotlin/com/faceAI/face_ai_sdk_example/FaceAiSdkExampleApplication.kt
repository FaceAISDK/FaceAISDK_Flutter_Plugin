package com.faceAI.face_ai_sdk_example

import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.faceAI.face_ai_sdk.FaceSDKConfig
import io.flutter.app.FlutterApplication

class FaceAiSdkExampleApplication : FlutterApplication(), CameraXConfig.Provider {

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        FaceSDKConfig.init(this)
    }
}
