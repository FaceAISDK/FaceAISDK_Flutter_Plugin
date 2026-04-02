# SIGSEGV in checkModel when using FaceAISDK in Flutter plugin

## Environment
- Device: Samsung Galaxy A35 (SM-A356E), Android 16 (API 36)
- SDK Version: `io.github.faceaisdk:Android:2026.03.27`
- Flutter: 3.x (latest stable)
- AGP: 8.9.1
- Kotlin: 1.9.22
- compileSdk: 35, targetSdk: 35, minSdk: 24

## Problem

When integrating FaceAISDK into a Flutter plugin and launching `AddFaceFeatureActivity` via `startActivityForResult`, the app crashes with SIGSEGV in `checkModel` function of `libFaceAIModel.so`.

**The native demo app works perfectly on the same device.** Only the Flutter plugin integration crashes.

## Crash Log

```
F/libc: Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0
Cause: null pointer dereference

backtrace:
  #00 pc 0000000000007acc  libFaceAIModel.so
  #01 pc 0000000000007f40  libFaceAIModel.so (checkModel+52)
  #02 art_quick_generic_jni_trampoline
  ...
  #17 com.faceAI.face_ai_sdk.SysCamera.addFace.AddFaceFeatureActivity.onCreate+0
```

## Register Dump (at crash)
```
x0  valid_pointer    x1  0x0000000000000000 (NULL!)
x2  0x0000000000000020  x3  0x0000000000000010
```

`x1 = NULL` is the null pointer being dereferenced. Looks like `memcmp(ptr, NULL, 32)`.

## Debug Log (crash happens at BaseImageDispose constructor)

```
D/FaceAIDebug: === AddFaceFeatureActivity.onCreate START ===
D/FaceAIDebug: 1. super.onCreate done
D/FaceAIDebug: 2. hideSystemUI done
D/FaceAIDebug: 3. setContentView done
D/FaceAIDebug: 4. views initialized, addFaceType=FACE_VERIFY
D/FaceAIDebug: 5. intent parsed, faceID=g, mode=1, confirm=false
D/FaceAIDebug: 6. About to create BaseImageDispose  <-- CRASH HERE
I/DynamiteModule: Selected local version of com.google.mlkit.dynamite.face
D/nativeloader: Load libface_detector_v2_jni.so ... ok
D/nativeloader: Load libFaceAIModel.so ... ok
I/MMKV: loaded with 0 key-values
F/libc: Fatal signal 11 (SIGSEGV)
```

## What We Tried (all failed)

| Attempt | Result |
|---------|--------|
| `android:extractNativeLibs="true"` | Still crashes |
| `targetSdk=35` (same as demo) | Still crashes |
| Custom Application with `CameraXConfig.Provider` | Still crashes |
| Using `FaceApplication` from SDK as Application class | Still crashes |
| Pre-init `FaceAISDKEngine.getInstance()` in plugin init | Still crashes |
| `noCompress` for `.model`, `.bin`, `.param` files (all Stored in APK) | Still crashes |
| Verified all model files identical to demo APK (5,233,568 bytes) | Files match |
| Verified all assets present in APK | All present |

## How to Reproduce

1. Create a Flutter plugin with `io.github.faceaisdk:Android:2026.03.27` dependency
2. Copy `AddFaceFeatureActivity` source code from `faceAILib` module
3. Launch the Activity via `startActivityForResult` from the Flutter plugin
4. `BaseImageDispose` constructor triggers `checkModel` which crashes

## Code (Flutter plugin launches Activity)

```kotlin
val intent = Intent(currentActivity, AddFaceFeatureActivity::class.java).apply {
    putExtra(FaceVerificationActivity.USER_FACE_ID_KEY, faceId)
    putExtra(AddFaceFeatureActivity.ADD_FACE_IMAGE_TYPE_KEY, 
             AddFaceFeatureActivity.AddFaceImageTypeEnum.FACE_VERIFY.name)
    putExtra(AddFaceFeatureActivity.NEED_CONFIRM_ADD_FACE, false)
}
currentActivity.startActivityForResult(intent, ENROLL_REQUEST)
```

## Key Question

What does `checkModel` validate? The native demo app uses the exact same code and dependencies but doesn't crash. The only difference is that the Activity is launched from a Flutter context instead of a native Android context.

Is there any special initialization or configuration required for the SDK to work in a Flutter plugin environment? Does `checkModel` check the application package name, signing certificate, or something else that differs between a native app and a Flutter app?

## Reference
- GitHub Issue #95 requests Flutter plugin support
- Contact: FaceAISDK.Service@gmail.com
