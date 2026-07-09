# face_aisdk_flutter_plugin

[English](#english) | [简体中文](#chinese)

<a name="english"></a>
A Flutter plugin for FaceAISDK offline face recognition. Supports Platform View (Built-in UI mode) and direct API calls.

## Features

- **Offline Face Recognition**: High-performance offline face identification and verification.
- **Liveness Detection**: Supports Action, Silent, and Color Flash liveness detection.
- **1:1 Face Verification**: Compare two faces for similarity.
- **Platform View Support**: Seamlessly integrate the camera preview and UI into your Flutter widget tree.
- **Face Management**: Register, delete, and check for face features locally.

## Getting Started

### Android
1. Add the following to your `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   ```
2. Ensure `minSdkVersion` is at least **21**.

### iOS
1. Add the following to your `Info.plist`:
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>We need camera access for face recognition.</string>
   ```
2. Ensure the deployment target is **iOS 15.0** or higher.

## Usage

### 1. Platform View (Built-in UI)
Use `FaceAiSdkView` to show a camera preview with built-in face recognition UI.

```dart
import 'package:face_aisdk_flutter_plugin/face_aisdk_flutter_plugin.dart';

FaceAiSdkView(
  onViewCreated: (FaceAiSdkController controller) {
    // You can use the controller to start or stop scanning
    // controller.startScan();
  },
  creationParams: {
    'threshold': 0.84,
  },
)
```

### 2. Direct API Calls
The `FaceAiSdkFlutterPlugin` class provides several static methods for face operations.

#### Face Verification (1:1 + Liveness)
```dart
final result = await FaceAiSdkFlutterPlugin.faceVerify(
  faceId: "user_123",
  threshold: 0.84,
  livenessType: 1, // 1: Action, 2: Action+Color, 3: Color, 4: Silent
  motionLivenessTypes: "1,2,3", // 1: Mouth, 2: Smile, 3: Blink, 4: Shake, 5: Nod
);

if (result?['code'] == FaceAiSdkResultCode.verifySuccess) {
  print("Face verified successfully");
}
```

#### Face Registration (via Camera)
```dart
final result = await FaceAiSdkFlutterPlugin.addFaceBySDKCamera(
  faceId: "new_user_456",
  addFacePerformanceMode: 1, // 1: Fast, 2: Precise
);
```

#### Liveness Detection
```dart
final result = await FaceAiSdkFlutterPlugin.livenessVerify(
  livenessType: 2,             // 1: Action, 2: Action+Color, 3: Color, 4: Silent
  motionLivenessTypes: "1,2,3", // 1: Mouth, 2: Smile, 3: Blink, 4: Shake, 5: Nod
  motionLivenessTimeOut: 7,    // Timeout in seconds [3, 10]
  motionLivenessSteps: 2,      // Actions required [1, 2]
  allowMultiFaces: true,       // Allow multiple faces (Android only)
);
```

### 3. Result Codes
Use `FaceAiSdkResultCode` to handle different outcomes:
- `verifySuccess` (1): Verification passed.
- `verifyFailed` (2): Verification failed.
- `motionLivenessSuccess` (3): Action liveness passed.
- `allLivenessSuccess` (10): All liveness checks passed.
- `cancel` (0): User cancelled.

---

<a name="chinese"></a>
# face_aisdk_flutter_plugin (简体中文)

适用于 FaceAISDK 离线人脸识别的 Flutter 插件。支持 Platform View（内置 UI 模式）和直接 API 调用。

## 特性

- **离线人脸识别**: 高性能离线人脸比对与识别。
- **活体检测**: 支持动作活体、静默活体及炫彩活体检测。
- **1:1 人脸比对**: 比较两张人脸的相似度。
- **支持 Platform View**: 将相机预览和 UI 无缝集成到 Flutter 组件树中。
- **人脸管理**: 本地注册、删除和检查人脸特征值。

## 入门指南

### Android
1. 在 `AndroidManifest.xml` 中添加：
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   ```
2. 确保 `minSdkVersion` 至少为 **21**。

### iOS
1. 在 `Info.plist` 中添加：
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>我们需要相机权限来进行人脸识别。</string>
   ```
2. 确保 Deployment Target 至少为 **iOS 15.0**。

## 使用示例

### 1. Platform View (内置 UI 模式)
使用 `FaceAiSdkView` 组件在 Flutter 中嵌入带有引导 UI 的相机界面。

```dart
import 'package:face_aisdk_flutter_plugin/face_aisdk_flutter_plugin.dart';

FaceAiSdkView(
  onViewCreated: (FaceAiSdkController controller) {
    // 可以通过控制器手动开始或停止扫描
    // controller.startScan();
  },
  creationParams: {
    'threshold': 0.84,
  },
)
```

### 2. API 调用模式
通过 `FaceAiSdkFlutterPlugin` 类直接调用功能接口。

#### 人脸核验 (1:1 + 活体检测)
```dart
final result = await FaceAiSdkFlutterPlugin.faceVerify(
  faceId: "user_123",
  threshold: 0.84,
  livenessType: 1, // 1: 动作, 2: 动作+炫彩, 3: 炫彩, 4: 静默
  motionLivenessTypes: "1,2,3", // 1: 张嘴, 2: 微笑, 3: 眨眼, 4: 摇头, 5: 点头
);

if (result?['code'] == FaceAiSdkResultCode.verifySuccess) {
  print("人脸核验成功");
}
```

#### 人脸采集注册 (通过相机)
```dart
final result = await FaceAiSdkFlutterPlugin.addFaceBySDKCamera(
  faceId: "user_123",
  addFacePerformanceMode: 1, // 1: 快速模式, 2: 精确模式
);
```

#### 纯活体检测
```dart
final result = await FaceAiSdkFlutterPlugin.livenessVerify(
  livenessType: 2,              // 1: 动作, 2: 动作+炫彩, 3: 炫彩, 4: 静默
  motionLivenessTypes: "1,2,3", // 1: 张嘴, 2: 微笑, 3: 眨眼, 4: 摇头, 5: 点头
  motionLivenessTimeOut: 7,     // 超时时间 [3, 10]
  motionLivenessSteps: 2,       // 动作步数 [1, 2]
  allowMultiFaces: true,        // 是否允许多人脸 (仅 Android)
);
```

### 3. 结果状态码
使用 `FaceAiSdkResultCode` 处理不同的返回结果：
- `verifySuccess` (1): 核验成功。
- `verifyFailed` (2): 核验失败。
- `motionLivenessSuccess` (3): 动作活体通过。
- `allLivenessSuccess` (10): 所有活体检测通过。
- `cancel` (0): 用户取消操作。
