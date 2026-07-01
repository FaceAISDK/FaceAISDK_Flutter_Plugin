# face_aisdk_flutter_plugin

A Flutter plugin for FaceAISDK offline face recognition. Supports Platform View (Built-in UI mode).

## Features

- Offline Face Recognition
- Liveness Detection (Action, Silent, Color Flash)
- 1:1 Face Verification
- Platform View support for seamless UI integration

## Getting Started

### Android

Add the following to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

### iOS

Add the following to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access for face recognition.</string>
```

## Usage

```dart
import 'package:face_aisdk_flutter_plugin/face_aisdk_flutter_plugin.dart';

// Use the FaceAiSdkView widget
FaceAiSdkView(
  onViewCreated: (controller) {
    // Handle controller
  },
  creationParams: {
    'threshold': 0.8,
  },
)
```
