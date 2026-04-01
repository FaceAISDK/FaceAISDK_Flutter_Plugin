# FaceAISDK Flutter Plugin Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate native Android FaceAISDK into the Flutter plugin so Flutter apps can call face verification, liveness detection, enrollment, and face addition via MethodChannel.

**Architecture:** Activity Launcher pattern — Flutter calls MethodChannel methods, the Kotlin plugin launches native Activities via `startActivityForResult`, and returns results as `Map<String, dynamic>` to Dart. Native source code and resources are copied from the `faceAILib` module into the plugin's Android directory.

**Tech Stack:** Flutter/Dart, Kotlin, Java, Android CameraX, FaceAISDK (io.github.faceaisdk:Android:2026.03.27), MMKV, MethodChannel

---

## File Structure

**Files to modify:**
- `android/build.gradle` — Add dependencies, repositories, update SDK/Java versions
- `android/src/main/AndroidManifest.xml` — Register Activities, add permissions
- `android/src/main/kotlin/com/faceAI/face_ai_sdk/FaceAiSdkPlugin.kt` — Full rewrite with ActivityAware
- `lib/face_ai_sdk.dart` — Updated public API with all methods
- `lib/face_ai_sdk_platform_interface.dart` — New method signatures
- `lib/face_ai_sdk_method_channel.dart` — New MethodChannel calls

**Files to create (copied from native):**
- `android/src/main/java/com/faceAI/demo/` — 21 source files (Java/Kotlin)
- `android/src/main/res/` — ~101 resource files (layouts, drawables, raw, values)

---

### Task 1: Update android/build.gradle

**Files:**
- Modify: `android/build.gradle`

- [ ] **Step 1: Update build.gradle with repositories, dependencies, and build config**

Replace the full contents of `android/build.gradle` with:

```gradle
group = "com.faceAI.face_ai_sdk"
version = "1.0-SNAPSHOT"

buildscript {
    ext.kotlin_version = "1.9.22"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.9.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://s01.oss.sonatype.org/content/groups/public" }
        maven { url "https://jitpack.io" }
        maven { url 'https://repo1.maven.org/maven2/' }
    }
}

apply plugin: "com.android.library"
apply plugin: "kotlin-android"

android {
    namespace = "com.faceAI.face_ai_sdk"

    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        main.java.srcDirs += "src/main/kotlin"
        main.java.srcDirs += "src/main/java"
        test.java.srcDirs += "src/test/kotlin"
    }

    defaultConfig {
        minSdk = 21
        ndk {
            abiFilters 'arm64-v8a'
        }
    }

    buildFeatures {
        viewBinding true
    }

    dependencies {
        api 'io.github.faceaisdk:Android:2026.03.27'

        implementation 'com.google.code.gson:gson:2.13.2'
        implementation 'com.tencent:mmkv:1.3.14'
        implementation 'pub.devrel:easypermissions:3.0.0'
        implementation 'com.airbnb.android:lottie:6.5.2'
        implementation 'com.github.bumptech.glide:glide:4.16.0'
        implementation 'androidx.appcompat:appcompat:1.6.0'
        implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

        testImplementation("org.jetbrains.kotlin:kotlin-test")
        testImplementation("org.mockito:mockito-core:5.0.0")
    }

    testOptions {
        unitTests.all {
            useJUnitPlatform()
            testLogging {
               events "passed", "skipped", "failed", "standardOut", "standardError"
               outputs.upToDateWhen {false}
               showStandardStreams = true
            }
        }
    }
}

def camera_version = "1.4.2"
configurations.configureEach {
    resolutionStrategy {
        force "androidx.camera:camera-core:$camera_version",
                "androidx.camera:camera-camera2:$camera_version",
                "androidx.camera:camera-lifecycle:$camera_version",
                "androidx.camera:camera-view:$camera_version"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/build.gradle
git commit -m "build: update android build.gradle with FaceAISDK dependencies and config"
```

---

### Task 2: Copy native source code files

**Files:**
- Create: `android/src/main/java/com/faceAI/demo/` (21 files total)

The source path prefix is: `C:\Users\merez\Downloads\ABDM\Compressed\FaceAISDK_Android-publish-new\FaceAISDK_Android-publish\faceAILib\src\main\java\com\faceAI\demo\`
The destination path prefix is: `android/src/main/java/com/faceAI/demo/`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p android/src/main/java/com/faceAI/demo/SysCamera/verify
mkdir -p android/src/main/java/com/faceAI/demo/SysCamera/addFace
mkdir -p android/src/main/java/com/faceAI/demo/SysCamera/camera
mkdir -p android/src/main/java/com/faceAI/demo/base/utils/fileUtils
mkdir -p android/src/main/java/com/faceAI/demo/base/utils/performance/opengl
mkdir -p android/src/main/java/com/faceAI/demo/base/view
```

- [ ] **Step 2: Copy core files (2 files)**

```bash
NATIVE_SRC="/c/Users/merez/Downloads/ABDM/Compressed/FaceAISDK_Android-publish-new/FaceAISDK_Android-publish/faceAILib/src/main/java/com/faceAI/demo"
PLUGIN_DEST="android/src/main/java/com/faceAI/demo"

cp "$NATIVE_SRC/FaceSDKConfig.java" "$PLUGIN_DEST/"
cp "$NATIVE_SRC/FaceApplication.java" "$PLUGIN_DEST/"
```

- [ ] **Step 3: Copy SysCamera/verify files (3 files)**

```bash
cp "$NATIVE_SRC/SysCamera/verify/FaceVerificationActivity.java" "$PLUGIN_DEST/SysCamera/verify/"
cp "$NATIVE_SRC/SysCamera/verify/LivenessDetectActivity.java" "$PLUGIN_DEST/SysCamera/verify/"
cp "$NATIVE_SRC/SysCamera/verify/AbsAddFaceFromAlbumActivity.kt" "$PLUGIN_DEST/SysCamera/verify/"
```

- [ ] **Step 4: Copy SysCamera/addFace files (1 file)**

```bash
cp "$NATIVE_SRC/SysCamera/addFace/AddFaceFeatureActivity.java" "$PLUGIN_DEST/SysCamera/addFace/"
```

- [ ] **Step 5: Copy SysCamera/camera files (2 files)**

```bash
cp "$NATIVE_SRC/SysCamera/camera/FaceCameraXFragment.java" "$PLUGIN_DEST/SysCamera/camera/"
cp "$NATIVE_SRC/SysCamera/camera/FaceCameraXBuilder.java" "$PLUGIN_DEST/SysCamera/camera/"
```

- [ ] **Step 6: Copy base files (2 files)**

```bash
cp "$NATIVE_SRC/base/AbsBaseActivity.kt" "$PLUGIN_DEST/base/"
cp "$NATIVE_SRC/base/SystemLanguageTTS.kt" "$PLUGIN_DEST/base/"
```

- [ ] **Step 7: Copy base/utils files (6 files)**

```bash
cp "$NATIVE_SRC/base/utils/BitmapUtils.java" "$PLUGIN_DEST/base/utils/"
cp "$NATIVE_SRC/base/utils/BrightnessUtil.java" "$PLUGIN_DEST/base/utils/"
cp "$NATIVE_SRC/base/utils/ScreenUtils.java" "$PLUGIN_DEST/base/utils/"
cp "$NATIVE_SRC/base/utils/VoicePlayer.java" "$PLUGIN_DEST/base/utils/"
cp "$NATIVE_SRC/base/utils/fileUtils/MyFileUtils.java" "$PLUGIN_DEST/base/utils/fileUtils/"
cp "$NATIVE_SRC/base/utils/fileUtils/ResultUtils.kt" "$PLUGIN_DEST/base/utils/fileUtils/"
```

- [ ] **Step 8: Copy base/utils/performance and opengl files (4 files)**

```bash
cp "$NATIVE_SRC/base/utils/performance/DevicePerformance.kt" "$PLUGIN_DEST/base/utils/performance/"
cp "$NATIVE_SRC/base/utils/performance/opengl/EglCore.java" "$PLUGIN_DEST/base/utils/performance/opengl/"
cp "$NATIVE_SRC/base/utils/performance/opengl/EglSurfaceBase.java" "$PLUGIN_DEST/base/utils/performance/opengl/"
cp "$NATIVE_SRC/base/utils/performance/opengl/OffscreenSurface.java" "$PLUGIN_DEST/base/utils/performance/opengl/"
```

- [ ] **Step 9: Copy base/view files (1 file)**

```bash
cp "$NATIVE_SRC/base/view/FaceVerifyCoverView.java" "$PLUGIN_DEST/base/view/"
```

- [ ] **Step 10: Copy SysCamera/search helper files needed by verify Activities**

The `FaceVerificationActivity` imports `ImageToast` from search package:

```bash
mkdir -p android/src/main/java/com/faceAI/demo/SysCamera/search
cp "$NATIVE_SRC/SysCamera/search/ImageToast.java" "$PLUGIN_DEST/SysCamera/search/"
```

- [ ] **Step 11: Create FaceAISettingsActivity stub**

The Activities import `FRONT_BACK_CAMERA_FLAG` and `SYSTEM_CAMERA_DEGREE` constants from `FaceAISettingsActivity`. Since we're not copying that full Activity, create a stub with just the constants.

Create file `android/src/main/java/com/faceAI/demo/FaceAISettingsActivity.kt`:

```kotlin
package com.faceAI.demo

/**
 * Stub containing camera setting constants used by Activities.
 * Full settings UI is not included in the Flutter plugin.
 */
object FaceAISettingsActivity {
    const val FRONT_BACK_CAMERA_FLAG = "cameraFlag"
    const val SYSTEM_CAMERA_DEGREE = "cameraDegree"
}
```

- [ ] **Step 12: Commit**

```bash
git add android/src/main/java/
git commit -m "feat: copy native FaceAISDK source files into plugin"
```

---

### Task 3: Copy native resource files

**Files:**
- Create: `android/src/main/res/` (~101 resource files)

- [ ] **Step 1: Copy all resource directories**

```bash
NATIVE_RES="/c/Users/merez/Downloads/ABDM/Compressed/FaceAISDK_Android-publish-new/FaceAISDK_Android-publish/faceAILib/src/main/res"
PLUGIN_RES="android/src/main/res"

# Copy all resource directories
cp -r "$NATIVE_RES/drawable" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/drawable-anydpi" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/layout" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/layout-land" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/raw" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/values" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/values-zh-rCN" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/menu" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/mipmap-hdpi" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/mipmap-mdpi" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/mipmap-xhdpi" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/mipmap-xxhdpi" "$PLUGIN_RES/"
cp -r "$NATIVE_RES/mipmap-xxxhdpi" "$PLUGIN_RES/"
```

- [ ] **Step 2: Commit**

```bash
git add android/src/main/res/
git commit -m "feat: copy native FaceAISDK resources into plugin"
```

---

### Task 4: Update AndroidManifest.xml

**Files:**
- Modify: `android/src/main/AndroidManifest.xml`

- [ ] **Step 1: Update the manifest**

Replace `android/src/main/AndroidManifest.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.faceAI.face_ai_sdk">

    <queries>
        <intent>
            <action android:name="android.intent.action.TTS_SERVICE" />
        </intent>
    </queries>

    <uses-permission android:name="com.android.vending.CHECK_LICENSE" />

    <application>
        <activity
            android:name="com.faceAI.demo.SysCamera.verify.FaceVerificationActivity"
            android:exported="false"
            android:launchMode="singleTop"
            android:theme="@style/Theme.FaceAISDK.Fullscreen" />

        <activity
            android:name="com.faceAI.demo.SysCamera.verify.LivenessDetectActivity"
            android:exported="false"
            android:launchMode="singleTop"
            android:theme="@style/Theme.FaceAISDK.Fullscreen" />

        <activity
            android:name="com.faceAI.demo.SysCamera.addFace.AddFaceFeatureActivity"
            android:exported="false"
            android:theme="@style/FaceAITheme" />
    </application>

</manifest>
```

- [ ] **Step 2: Commit**

```bash
git add android/src/main/AndroidManifest.xml
git commit -m "feat: register FaceAISDK Activities in plugin manifest"
```

---

### Task 5: Rewrite FaceAiSdkPlugin.kt

**Files:**
- Modify: `android/src/main/kotlin/com/faceAI/face_ai_sdk/FaceAiSdkPlugin.kt`

- [ ] **Step 1: Rewrite the plugin with ActivityAware and result handling**

Replace `android/src/main/kotlin/com/faceAI/face_ai_sdk/FaceAiSdkPlugin.kt` with:

```kotlin
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
import com.faceAI.demo.FaceSDKConfig
import com.faceAI.demo.SysCamera.verify.FaceVerificationActivity
import com.faceAI.demo.SysCamera.verify.LivenessDetectActivity
import com.faceAI.demo.SysCamera.addFace.AddFaceFeatureActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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

    // region ActivityAware
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
    // endregion

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

    private fun handleStartVerification(call: MethodCall, result: Result) {
        val currentActivity = activity ?: run {
            result.error("NO_ACTIVITY", "Plugin not attached to an Activity", null)
            return
        }
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

        if (data == null) {
            result.success(mapOf("code" to 0, "msg" to "cancelled"))
            return true
        }

        val faceImageStr = loadFaceLogImage()

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

    /**
     * Load the face log image saved by the Activity.
     * Returns base64 string or file path based on pendingFormat.
     */
    private fun loadFaceLogImage(): String? {
        val ctx = activity?.applicationContext ?: return null
        val logDir = ctx.filesDir.path + "/FaceAI/Log/"

        // Try to find the most recent saved bitmap
        val verifyFile = File(logDir, "verifyBitmap")
        val liveFile = File(logDir, "liveBitmap")
        val targetFile = when {
            verifyFile.exists() -> verifyFile
            liveFile.exists() -> liveFile
            else -> return null
        }

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
```

- [ ] **Step 2: Commit**

```bash
git add android/src/main/kotlin/com/faceAI/face_ai_sdk/FaceAiSdkPlugin.kt
git commit -m "feat: rewrite FaceAiSdkPlugin with ActivityAware and all method handlers"
```

---

### Task 6: Update Dart platform interface

**Files:**
- Modify: `lib/face_ai_sdk_platform_interface.dart`

- [ ] **Step 1: Update platform interface with new method signatures**

Replace `lib/face_ai_sdk_platform_interface.dart` with:

```dart
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'face_ai_sdk_method_channel.dart';

abstract class FaceAiSdkPlatform extends PlatformInterface {
  FaceAiSdkPlatform() : super(token: _token);

  static final Object _token = Object();

  static FaceAiSdkPlatform _instance = MethodChannelFaceAiSdk();

  static FaceAiSdkPlatform get instance => _instance;

  static set instance(FaceAiSdkPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }

  Future<String> initializeSDK(Map<String, dynamic> config) {
    throw UnimplementedError('initializeSDK() has not been implemented.');
  }

  Future<Map<String, dynamic>> startVerification({
    required String faceId,
    double threshold = 0.85,
    int livenessType = 0,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  }) {
    throw UnimplementedError('startVerification() has not been implemented.');
  }

  Future<Map<String, dynamic>> startLiveness({
    int livenessType = 1,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  }) {
    throw UnimplementedError('startLiveness() has not been implemented.');
  }

  Future<Map<String, dynamic>> startEnroll({
    required String faceId,
    String format = "base64",
  }) {
    throw UnimplementedError('startEnroll() has not been implemented.');
  }

  Future<Map<String, dynamic>> addFace({
    required String faceId,
    String format = "base64",
  }) {
    throw UnimplementedError('addFace() has not been implemented.');
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add lib/face_ai_sdk_platform_interface.dart
git commit -m "feat: update platform interface with all FaceAISDK methods"
```

---

### Task 7: Update Dart method channel

**Files:**
- Modify: `lib/face_ai_sdk_method_channel.dart`

- [ ] **Step 1: Update method channel with all method invocations**

Replace `lib/face_ai_sdk_method_channel.dart` with:

```dart
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'face_ai_sdk_platform_interface.dart';

class MethodChannelFaceAiSdk extends FaceAiSdkPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('face_ai_sdk');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String> initializeSDK(Map<String, dynamic> config) async {
    final result = await methodChannel.invokeMethod<String>(
      'initializeSDK',
      {'config': config},
    );
    return result ?? 'Failed to initialize';
  }

  @override
  Future<Map<String, dynamic>> startVerification({
    required String faceId,
    double threshold = 0.85,
    int livenessType = 0,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  }) async {
    final result =
        await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'startVerification',
      {
        'faceId': faceId,
        'threshold': threshold,
        'livenessType': livenessType,
        'motionStepSize': motionStepSize,
        'motionTimeout': motionTimeout,
        'motionTypes': motionTypes,
        'format': format,
      },
    );
    return Map<String, dynamic>.from(result ?? {});
  }

  @override
  Future<Map<String, dynamic>> startLiveness({
    int livenessType = 1,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  }) async {
    final result =
        await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'startLiveness',
      {
        'livenessType': livenessType,
        'motionStepSize': motionStepSize,
        'motionTimeout': motionTimeout,
        'motionTypes': motionTypes,
        'format': format,
      },
    );
    return Map<String, dynamic>.from(result ?? {});
  }

  @override
  Future<Map<String, dynamic>> startEnroll({
    required String faceId,
    String format = "base64",
  }) async {
    final result =
        await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'startEnroll',
      {
        'faceId': faceId,
        'format': format,
      },
    );
    return Map<String, dynamic>.from(result ?? {});
  }

  @override
  Future<Map<String, dynamic>> addFace({
    required String faceId,
    String format = "base64",
  }) async {
    final result =
        await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'addFace',
      {
        'faceId': faceId,
        'format': format,
      },
    );
    return Map<String, dynamic>.from(result ?? {});
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add lib/face_ai_sdk_method_channel.dart
git commit -m "feat: update method channel with all FaceAISDK method invocations"
```

---

### Task 8: Update Dart public API

**Files:**
- Modify: `lib/face_ai_sdk.dart`

- [ ] **Step 1: Update public API**

Replace `lib/face_ai_sdk.dart` with:

```dart
import 'face_ai_sdk_platform_interface.dart';

class FaceAiSdk {
  Future<String?> getPlatformVersion() {
    return FaceAiSdkPlatform.instance.getPlatformVersion();
  }

  /// Initialize the FaceAI SDK. Must be called before any other method.
  Future<String> initializeSDK(Map<String, dynamic> config) {
    return FaceAiSdkPlatform.instance.initializeSDK(config);
  }

  /// 1:1 Face Verification against a stored faceId.
  ///
  /// [faceId] - Unique identifier for the face to verify against.
  /// [threshold] - Recognition threshold [0.75-0.95], default 0.85.
  /// [livenessType] - 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT.
  /// [motionStepSize] - Number of motion steps [1-2].
  /// [motionTimeout] - Motion timeout in seconds [3-22].
  /// [motionTypes] - Comma-separated motion types: 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, faceID, msg, similarity, livenessValue, faceImage.
  Future<Map<String, dynamic>> startVerification({
    required String faceId,
    double threshold = 0.85,
    int livenessType = 0,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  }) {
    return FaceAiSdkPlatform.instance.startVerification(
      faceId: faceId,
      threshold: threshold,
      livenessType: livenessType,
      motionStepSize: motionStepSize,
      motionTimeout: motionTimeout,
      motionTypes: motionTypes,
      format: format,
    );
  }

  /// Liveness detection only (no face matching).
  ///
  /// [livenessType] - 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT.
  /// [motionStepSize] - Number of motion steps [1-2].
  /// [motionTimeout] - Motion timeout in seconds [3-22].
  /// [motionTypes] - Comma-separated motion types: 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, msg, livenessValue, faceImage.
  Future<Map<String, dynamic>> startLiveness({
    int livenessType = 1,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  }) {
    return FaceAiSdkPlatform.instance.startLiveness(
      livenessType: livenessType,
      motionStepSize: motionStepSize,
      motionTimeout: motionTimeout,
      motionTypes: motionTypes,
      format: format,
    );
  }

  /// Enroll a face for 1:1 verification.
  ///
  /// [faceId] - Unique identifier for the face being enrolled.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, faceID, msg, faceImage.
  Future<Map<String, dynamic>> startEnroll({
    required String faceId,
    String format = "base64",
  }) {
    return FaceAiSdkPlatform.instance.startEnroll(
      faceId: faceId,
      format: format,
    );
  }

  /// Add a face to the search database.
  ///
  /// [faceId] - Unique identifier for the face being added.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, faceID, msg, faceImage.
  Future<Map<String, dynamic>> addFace({
    required String faceId,
    String format = "base64",
  }) {
    return FaceAiSdkPlatform.instance.addFace(
      faceId: faceId,
      format: format,
    );
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add lib/face_ai_sdk.dart
git commit -m "feat: update public Dart API with all FaceAISDK methods"
```

---

### Task 9: Fix compilation issues

After copying source files, there may be import issues from removed dependencies (UVC, search, etc.). This task resolves them.

**Files:**
- Modify: Various copied Java/Kotlin files (as needed)

- [ ] **Step 1: Build the plugin to identify issues**

```bash
cd example && flutter build apk --debug 2>&1 | head -100
```

- [ ] **Step 2: Fix any import errors in copied source files**

Common fixes needed:
- Remove unused imports referencing UVC classes
- Remove unused imports referencing search classes not copied
- Fix any resource references to layouts/drawables that weren't copied

For each compilation error, either:
1. Remove the offending import if it's unused
2. Add missing files if they're required dependencies
3. Stub out minimal interfaces if the dependency is minor

- [ ] **Step 3: Re-build to verify**

```bash
cd example && flutter build apk --debug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit fixes**

```bash
git add -A
git commit -m "fix: resolve compilation issues in copied native source files"
```

---

### Task 10: Update example app configuration

**Files:**
- Modify: `example/android/app/build.gradle` (if needed for minSdk/compileSdk alignment)
- Modify: `example/android/app/src/main/AndroidManifest.xml` (camera permission)

- [ ] **Step 1: Add camera permission to example app manifest**

The example app needs camera permission. Add to `example/android/app/src/main/AndroidManifest.xml` inside `<manifest>`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
```

- [ ] **Step 2: Ensure example app Application class initializes CameraX**

Check if the example app needs a custom Application class that implements `CameraXConfig.Provider`. If the default Flutter Application doesn't support CameraX, create one or document the requirement.

The simplest approach: in `initializeSDK`, call `ProcessCameraProvider.configureInstance(...)` if not already configured. This is handled in the plugin code via `FaceSDKConfig.init()`.

- [ ] **Step 3: Build and verify the example app compiles**

```bash
cd example && flutter build apk --debug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add example/
git commit -m "feat: update example app configuration for FaceAISDK"
```

---

### Task 11: Smoke test on device

- [ ] **Step 1: Run on a connected Android device/emulator**

```bash
cd example && flutter run
```

Note: Face AI SDK requires arm64-v8a, so this must run on a physical device or arm64 emulator.

- [ ] **Step 2: Test initializeSDK**

From the example app, call `FaceAiSdk().initializeSDK({})` and verify it returns "SDK initialized successfully".

- [ ] **Step 3: Test startEnroll**

Call `FaceAiSdk().startEnroll(faceId: "test_user")` and verify:
- The native AddFaceFeatureActivity opens
- Camera shows and detects a face
- Result returns to Flutter with code 1 on success

- [ ] **Step 4: Test startVerification**

Call `FaceAiSdk().startVerification(faceId: "test_user")` and verify:
- The native FaceVerificationActivity opens
- Camera shows, face is compared against enrolled face
- Result returns with similarity score

- [ ] **Step 5: Test startLiveness**

Call `FaceAiSdk().startLiveness(livenessType: 1)` and verify:
- The native LivenessDetectActivity opens
- Motion liveness detection works
- Result returns with livenessValue

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "feat: complete FaceAISDK Flutter plugin integration"
```
