# FaceAI SDK Flutter Plugin

A Flutter plugin for [FaceAISDK](https://github.com/FaceAISDK/FaceAISDK_Android) — on-device face verification, liveness detection, and face enrollment for Android.

**[Bahasa Indonesia](README_ID.md)**

## Features

- **Face Enrollment** — Register a face for 1:1 verification
- **Face Verification (1:1)** — Verify a live face against a stored face
- **Liveness Detection** — Anti-spoofing detection (motion, color flash, silent)
- **Add Face** — Add a face to the search database

All processing is on-device. No internet required.

## Requirements

- Android `minSdk >= 21`
- `arm64-v8a` device (64-bit ARM)
- Camera permission

## Installation

Add to your `pubspec.yaml`:

```yaml
dependencies:
  face_ai_sdk:
    git:
      url: https://github.com/your-repo/FaceAISDK_Flutter_Plugin.git
```

## Android Setup (Required)

### 1. AndroidManifest.xml

Add `extractNativeLibs="true"` to your `<application>` tag:

```xml
<application
    android:extractNativeLibs="true"
    ...>
```

### 2. build.gradle.kts

Add these configurations to `android/app/build.gradle.kts`:

```kotlin
android {
    // Java 17 required
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Prevent compression of SDK model files
    androidResources {
        noCompress += listOf("model", "bin", "param", "tfl")
    }

    // Sign with FaceAISDK-registered keystore
    signingConfigs {
        create("faceai") {
            storeFile = file("your-keystore-file")
            storePassword = "your-password"
            keyAlias = "your-alias"
            keyPassword = "your-password"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("faceai")
        }
        release {
            signingConfig = signingConfigs.getByName("faceai")
        }
    }
}
```

### 3. Signing Key Registration

FaceAISDK validates the app's **package name + signing certificate**. You must register your signing key with the SDK provider:

- Contact: FaceAISDK.Service@gmail.com
- GitHub: https://github.com/FaceAISDK/FaceAISDK_Android

For development/testing, you can use the demo keystore (`FaceAIPublic`) with `applicationId = "com.ai.face.Demo"`.

## Usage

### Initialize SDK

Must be called before any other method:

```dart
final faceAiSdk = FaceAiSdk();
await faceAiSdk.initializeSDK({});
```

### Enroll a Face

Capture and register a face for 1:1 verification:

```dart
final result = await faceAiSdk.startEnroll(
  faceId: "user_123",
  format: "base64",  // or "filePath"
);

if (result['code'] == 1) {
  print('Enrolled: ${result['faceID']}');
}
```

### Verify a Face (1:1)

Compare a live face against a stored face:

```dart
final result = await faceAiSdk.startVerification(
  faceId: "user_123",
  threshold: 0.85,          // 0.75 - 0.95
  livenessType: 1,          // 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
  motionStepSize: 1,        // 1-2 steps
  motionTimeout: 10,        // 3-22 seconds
  motionTypes: "1,2,3",     // 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod
  format: "base64",
);

if (result['code'] == 1) {
  print('Match! Similarity: ${result['similarity']}');
  print('Liveness: ${result['livenessValue']}');
}
```

### Liveness Detection

Detect if the face is a real person (no face matching):

```dart
final result = await faceAiSdk.startLiveness(
  livenessType: 1,          // 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
  motionStepSize: 1,
  motionTimeout: 10,
  motionTypes: "1,2,3",
  format: "base64",
);

if (result['code'] == 10) {
  print('Liveness score: ${result['livenessValue']}');
}
```

### Add Face to Search Database

Add a face for 1:N search:

```dart
final result = await faceAiSdk.addFace(
  faceId: "user_456",
  format: "base64",
);

if (result['code'] == 1) {
  print('Face added: ${result['faceID']}');
}
```

## Result Codes

| Code | Meaning |
|------|---------|
| 0 | Cancelled by user |
| 1 | Success |
| 2 | Verification failed (not same person) |
| 3 | Timeout |
| 4 | Timeout (exceeded retry limit) |
| 5 | No face detected repeatedly |
| 10 | Liveness detection completed |
| 11 | Silent liveness failed |

## Liveness Types

| Value | Type | Description |
|-------|------|-------------|
| 0 | NONE | No liveness detection |
| 1 | MOTION | Motion-based (open mouth, smile, blink, etc.) |
| 2 | MOTION + COLOR | Motion + color flash combined |
| 3 | COLOR_FLASH | Color flash only (not for bright environments) |
| 4 | SILENT | Passive silent liveness |

## Motion Types

Comma-separated string of motion IDs:

| ID | Motion |
|----|--------|
| 1 | Open mouth |
| 2 | Smile |
| 3 | Blink |
| 4 | Shake head |
| 5 | Nod head |

## Troubleshooting

### SIGSEGV crash in `checkModel`

The SDK validates your app's **package name + signing certificate**. If they don't match the registered values, the native code crashes. Make sure:

1. Your `applicationId` is registered with FaceAISDK
2. Your signing keystore is registered with FaceAISDK
3. `android:extractNativeLibs="true"` is set in AndroidManifest.xml
4. `noCompress` is configured for model files in build.gradle

### Camera not working

Ensure camera permission is granted at runtime. The plugin declares `<uses-permission android:name="android.permission.CAMERA" />` automatically.

## License

See [LICENSE](LICENSE) for details.

## Credits

- [FaceAISDK](https://github.com/FaceAISDK/FaceAISDK_Android) — Core face AI engine
