# FaceAISDK Flutter Plugin Integration Design

## Overview

Integrate the native Android FaceAISDK into the existing `face_ai_sdk` Flutter plugin, enabling Flutter apps to call native face verification, liveness detection, enrollment, and face addition features via MethodChannel.

**Approach:** Activity Launcher — each Flutter method launches a native Android Activity via `startActivityForResult`, receives results back, and returns them to Dart as `Map<String, dynamic>`.

## Architecture

```
Flutter App (Dart)
    ↓ method call
FaceAiSdk (lib/face_ai_sdk.dart)
    ↓
FaceAiSdkPlatform (platform interface)
    ↓
MethodChannelFaceAiSdk (lib/face_ai_sdk_method_channel.dart)
    ↓ MethodChannel('face_ai_sdk')
FaceAiSdkPlugin.kt (Android)
    ↓ startActivityForResult
Native Activity (FaceVerificationActivity, LivenessDetectActivity, AddFaceFeatureActivity)
    ↓ onActivityResult
FaceAiSdkPlugin.kt
    ↓ result.success(map)
Flutter App (receives Map<String, dynamic>)
```

## Dart API

### Methods

```dart
class FaceAiSdk {
  /// Initialize the FaceAI SDK. Must be called before any other method.
  Future<String> initializeSDK(Map<String, dynamic> config);

  /// 1:1 Face Verification against a stored faceId
  Future<Map<String, dynamic>> startVerification({
    required String faceId,
    double threshold = 0.85,        // 0.75 - 0.95
    int livenessType = 0,           // 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
    int motionStepSize = 1,         // 1-2
    int motionTimeout = 10,         // 3-22 seconds
    String motionTypes = "1,2,3",   // 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod
    String format = "base64",       // "base64" or "filePath"
  });

  /// Liveness detection only (no face matching)
  Future<Map<String, dynamic>> startLiveness({
    int livenessType = 1,           // 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "base64",
  });

  /// Enroll a face for 1:1 verification
  Future<Map<String, dynamic>> startEnroll({
    required String faceId,
    String format = "base64",
  });

  /// Add a face to the search database
  Future<Map<String, dynamic>> addFace({
    required String faceId,
    String format = "base64",
  });
}
```

### Return Values

**startVerification:**
```dart
{
  "code": int,            // 0=cancelled, 1=success, 2=failed, 3=timeout
  "faceID": String,
  "msg": String,
  "similarity": double,
  "livenessValue": double,
  "faceImage": String?,   // base64 or filePath based on format param
}
```

**startLiveness:**
```dart
{
  "code": int,
  "msg": String,
  "livenessValue": double,
  "faceImage": String?,
}
```

**startEnroll / addFace:**
```dart
{
  "code": int,
  "faceID": String,
  "msg": String,
  "faceImage": String?,
}
```

## Android Plugin Implementation

### FaceAiSdkPlugin.kt

Implements: `FlutterPlugin`, `MethodCallHandler`, `ActivityAware`, `PluginRegistry.ActivityResultListener`

**Request codes:**
- `VERIFICATION_REQUEST = 1001`
- `LIVENESS_REQUEST = 1002`
- `ENROLL_REQUEST = 1003`
- `ADD_FACE_REQUEST = 1004`

**Key fields:**
- `private var activity: Activity? = null`
- `private var pendingResult: MethodChannel.Result? = null`
- `private var pendingFormat: String = "base64"`

**Method handling flow:**
1. `onMethodCall` receives call
2. Extracts arguments, builds Intent with extras
3. Calls `activity.startActivityForResult(intent, requestCode)`
4. `onActivityResult` processes result
5. Encodes face image per format param (base64 or save to file)
6. Calls `pendingResult?.success(resultMap)`

### Intent Extras Mapping

**startVerification → FaceVerificationActivity:**
- `USER_FACE_ID_KEY` → faceId
- `THRESHOLD_KEY` → threshold (Float)
- `FACE_LIVENESS_TYPE` → livenessType (Int)
- `MOTION_STEP_SIZE` → motionStepSize (Int)
- `MOTION_TIMEOUT` → motionTimeout (Int)
- `MOTION_LIVENESS_TYPES` → motionTypes (String)

**startLiveness → LivenessDetectActivity:**
- `FACE_LIVENESS_TYPE` → livenessType (Int)
- `MOTION_STEP_SIZE` → motionStepSize (Int)
- `MOTION_TIMEOUT` → motionTimeout (Int)
- `MOTION_LIVENESS_TYPES` → motionTypes (String)

**startEnroll → FaceVerificationActivity (enroll mode):**
- `USER_FACE_ID_KEY` → faceId
- Enroll mode flag or separate handling in result

**addFace → AddFaceFeatureActivity:**
- `USER_FACE_ID_KEY` → faceId

## Files to Copy from Native Project

### Source Code (faceAILib/src/main/java/com/faceAI/demo/)

**Core (2 files):**
- `FaceSDKConfig.java` — SDK initialization, storage paths, MMKV setup
- `FaceApplication.java` — CameraXConfig.Provider, app initialization

**Verification & Liveness — SysCamera/verify/ (3 files):**
- `FaceVerificationActivity.java` — 1:1 face verification + liveness
- `LivenessDetectActivity.java` — Standalone liveness detection
- `AbsAddFaceFromAlbumActivity.kt` — Album face enrollment base

**Add Face — SysCamera/addFace/ (1 file):**
- `AddFaceFeatureActivity.java` — Face feature extraction and storage

**Camera — SysCamera/camera/ (2 files):**
- `FaceCameraXFragment.java` — CameraX camera fragment
- `FaceCameraXBuilder.java` — Camera configuration builder

**Base & Utils — base/ (10 files):**
- `AbsBaseActivity.kt` — Base activity
- `SystemLanguageTTS.kt` — Text-to-speech prompts
- `utils/BitmapUtils.java`
- `utils/BrightnessUtil.java`
- `utils/ScreenUtils.java`
- `utils/VoicePlayer.java`
- `utils/fileUtils/MyFileUtils.java`
- `utils/fileUtils/ResultUtils.kt`
- `utils/performance/DevicePerformance.kt`
- `view/FaceVerifyCoverView.java`

**Performance/OpenGL — base/utils/performance/opengl/ (3 files):**
- `EglCore.java`
- `EglSurfaceBase.java`
- `OffscreenSurface.java`

**Total source files: 21**

### Resources (faceAILib/src/main/res/)

Copy ALL resources:
- `drawable/` + `drawable-anydpi/` — 27 files (icons, backgrounds, shapes)
- `layout/` + `layout-land/` — 34 files (all layouts for Activities, fragments, dialogs)
- `raw/` — 19 files (audio prompts, lottie animations)
- `values/` + `values-zh-rCN/` — 5 files (strings, colors, styles, attrs)
- `menu/` — 1 file
- `mipmap-*/` — 15 files (launcher icons)

**Total resource files: ~101**

Note: Copy ALL res files to avoid missing resource references. Some layouts used by Activities we need may reference drawables/strings that appear unrelated.

## Files to Modify in Flutter Plugin

### 1. `android/build.gradle`

Add repositories:
```gradle
repositories {
    mavenCentral()
    google()
    maven { url "https://s01.oss.sonatype.org/content/groups/public" }
    maven { url "https://jitpack.io" }
    maven { url 'https://repo1.maven.org/maven2/' }
}
```

Add dependencies:
```gradle
dependencies {
    api 'io.github.faceaisdk:Android:2026.03.27'
    implementation 'com.google.code.gson:gson:2.13.2'
    implementation 'com.tencent:mmkv:1.3.14'
    implementation 'pub.devrel:easypermissions:3.0.0'
    implementation 'com.airbnb.android:lottie:6.5.2'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    implementation 'androidx.appcompat:appcompat:1.6.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

Update build config:
- `compileSdk` → 35
- `minSdk` → 21
- Java/Kotlin → VERSION_17
- Enable `viewBinding`
- Add `abiFilters 'arm64-v8a'`

### 2. `android/src/main/AndroidManifest.xml`

Add Activity declarations for:
- `FaceVerificationActivity` (exported=false, singleTop, fullscreen theme)
- `LivenessDetectActivity` (exported=false, singleTop, fullscreen theme)
- `AddFaceFeatureActivity` (exported=false, fullscreen theme)

Add TTS query intent and permissions.

### 3. `android/src/main/kotlin/.../FaceAiSdkPlugin.kt`

Full rewrite to implement:
- `FlutterPlugin`
- `MethodCallHandler`
- `ActivityAware` (onAttachedToActivity, onDetachedFromActivity, etc.)
- `PluginRegistry.ActivityResultListener`

Handle methods: `initializeSDK`, `startVerification`, `startLiveness`, `startEnroll`, `addFace`, `getPlatformVersion`

### 4. `lib/face_ai_sdk.dart`

Update to expose all 5 methods with proper parameters and return types.

### 5. `lib/face_ai_sdk_platform_interface.dart`

Add abstract method signatures for new methods.

### 6. `lib/face_ai_sdk_method_channel.dart`

Add MethodChannel invocations for all methods with proper argument maps.

## Dependencies

### Required (from native project build.gradle)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `io.github.faceaisdk:Android` | 2026.03.27 | Core face AI engine |
| `com.tencent:mmkv` | 1.3.14 | Local key-value storage (used by FaceSDKConfig) |
| `com.google.code.gson:gson` | 2.13.2 | JSON serialization |
| `pub.devrel:easypermissions` | 3.0.0 | Runtime permission handling |
| `com.airbnb.android:lottie` | 6.5.2 | Lottie animations in UI |
| `com.github.bumptech.glide:glide` | 4.16.0 | Image loading |
| `androidx.appcompat:appcompat` | 1.6.0 | AndroidX |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Layout |

### NOT required (excluded features)

| Dependency | Reason |
|-----------|--------|
| `com.herohan:UVCAndroid` | UVC camera not needed |
| `com.github.CymChad:BaseRecyclerViewAdapterHelper` | Search list UI not needed |
| `com.github.javakam:file.*` | File picker for image compare not needed |
| `com.github.princekin-f:EasyFloat` | Floating window not needed |

## CameraX Configuration

The native app uses `FaceApplication` as a `CameraXConfig.Provider`. In a Flutter plugin context, we cannot control the host app's Application class. Options:

1. Document that host app must extend `FaceApplication` or implement `CameraXConfig.Provider`
2. Handle CameraX config in `initializeSDK` method
3. Use `ProcessCameraProvider.configureInstance()` in plugin initialization

Option 2/3 is preferred — keep it transparent to Flutter developers.

## Error Handling

- If Activity is null when method called → return error "Plugin not attached to Activity"
- If another operation is pending → return error "Another operation is in progress"
- If SDK not initialized → return error "SDK not initialized, call initializeSDK first"
- Activity cancelled by user → return `{code: 0, msg: "cancelled"}`
