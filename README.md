# FaceAI SDK Flutter Plugin

A Flutter plugin for FaceAISDK — on-device face verification, liveness detection, and face enrollment for Android and iOS.

**[Bahasa Indonesia](README_ID.md)**

## Features

- **Face Enrollment** — Register a face for 1:1 verification
- **Face Verification (1:1)** — Verify a live face against a stored face
- **Liveness Detection** — Anti-spoofing detection (motion, color flash, silent)
- **Add Face** — Add a face to the search database

All processing is on-device. No internet required.

## Requirements

| Platform | Requirement |
|----------|-------------|
| Android | `minSdk >= 24`, `armeabi-v7a` or `arm64-v8a` device |
| iOS | iOS 15.5+, physical device only (no simulator) |
| Both | Camera permission |

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

## iOS Setup (Required)

### 1. Podfile

Set the platform to iOS 15.5+ and add the `FaceAISDK_Core` pod in your `ios/Podfile`:

```ruby
platform :ios, '15.5'

target 'Runner' do
  use_frameworks!

  flutter_install_all_ios_pods File.dirname(File.realpath(__FILE__))

  pod 'FaceAISDK_Core', :git => 'https://github.com/FaceAISDK/FaceAISDK_Core.git', :tag => '2026.03.27'
end
```

Add this `post_install` block to fix `BUILD_LIBRARY_FOR_DISTRIBUTION` ABI mismatch:

```ruby
post_install do |installer|
  installer.pods_project.targets.each do |target|
    flutter_additional_ios_build_settings(target)

    target.build_configurations.each do |config|
      config.build_settings['BUILD_LIBRARY_FOR_DISTRIBUTION'] = 'YES'
      config.build_settings['OTHER_SWIFT_FLAGS'] ||= '$(inherited)'
      config.build_settings['OTHER_SWIFT_FLAGS'] += ' -Xfrontend -enable-library-evolution'
    end
  end
end
```

Then run:

```bash
cd ios && pod install
```

### 2. Info.plist

Add camera permission to `ios/Runner/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera access is required for face verification and liveness detection.</string>
```

### 3. Build Settings

The plugin requires a **physical device** — simulator builds are not supported (`EXCLUDED_ARCHS[sdk=iphonesimulator*]` excludes `i386` and `arm64`).

## Usage

### Initialize SDK

Must be called before any other method:

```dart
final faceAiSdk = FaceAiSdk();
await faceAiSdk.initializeSDK({
  'locale': 'en',  // iOS only: UI language — "en" (default), "id", "zh-Hans"
});
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
  faceId: "user_123",        // face ID for stored lookup (faceId or faceFeature required)
  faceFeature: null,          // or pass face feature string directly
  threshold: 0.85,            // 0.75 - 0.95
  livenessType: 1,            // 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT
  motionStepSize: 1,          // 1-2 steps
  motionTimeout: 10,          // 3-22 seconds
  motionTypes: "1,2,3",       // 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod
  allowRetry: true,           // allow retry on timeout/failure
  format: "base64",           // "base64" or "filePath"
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
### Camera not working

Ensure camera permission is granted at runtime. The plugin declares `<uses-permission android:name="android.permission.CAMERA" />` automatically on Android. On iOS, add `NSCameraUsageDescription` to `Info.plist`.

### iOS: `BUILD_LIBRARY_FOR_DISTRIBUTION` / ABI mismatch errors

`FaceAISDK_Core` is a pre-compiled binary. All pods must use a consistent `BUILD_LIBRARY_FOR_DISTRIBUTION` setting. Add the `post_install` block from the iOS Setup section to your Podfile.

### iOS: `No such module 'FaceAISDK_Core'`

Make sure you added the `FaceAISDK_Core` pod to your Podfile and ran `pod install`. The module is not available on CocoaPods trunk — it must be referenced via the Git URL.

## License

See [LICENSE](LICENSE) for details.

## Credits

- [FaceAISDK Android](https://github.com/FaceAISDK/FaceAISDK_Android) — Core face AI engine (Android)
- [FaceAISDK_Core](https://github.com/FaceAISDK/FaceAISDK_Core) — Core face AI engine (iOS)
