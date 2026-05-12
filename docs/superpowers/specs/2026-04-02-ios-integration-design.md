# iOS Integration Design for FaceAI Flutter Plugin

## Summary

Integrate the native iOS FaceAI SDK into the Flutter plugin by copying and adapting SwiftUI views from the standalone iOS app (`FaceAISDK_iOS`). The iOS implementation will mirror the existing Android pattern: Flutter calls a method via MethodChannel, the plugin presents a native fullscreen modal, and returns results via FlutterResult callback.

## Decisions

- **Presentation style**: UIHostingController presented as fullscreen modal (option A — matches Android's Activity-based pattern)
- **Dependency**: `FaceAISDK_Core` pod, tag `2026.04.27`
- **Storage**: UserDefaults for face features (same as native iOS app)
- **Scope**: Only implement methods already in Dart API (`initializeSDK`, `startVerification`, `startLiveness`, `startEnroll`, `addFace`) — no new Dart API methods

## Architecture

```
Flutter (Dart) → MethodChannel("face_ai_sdk") → FaceAiSdkPlugin.swift
    ↓
FaceAiSdkPlugin.swift (router) → present UIHostingController
    ↓
SwiftUI Views (adapted from native app):
  - VerifyFaceView → startVerification
  - LivenessDetectView → startLiveness
  - AddFaceByCamera → startEnroll & addFace
    ↓
Callback/closure → FlutterResult → Dart
```

## Files to Create/Modify

### Modified Files

1. **`ios/Classes/FaceAiSdkPlugin.swift`** — Main plugin entry point
   - Handle all method calls: `initializeSDK`, `startVerification`, `startLiveness`, `startEnroll`, `addFace`, `getPlatformVersion`
   - Get root ViewController from Flutter engine
   - Present SwiftUI views via UIHostingController as fullscreen modal
   - Return results via FlutterResult

2. **`ios/face_ai_sdk.podspec`** — Add FaceAISDK_Core dependency

### New Files (copied & adapted from native iOS app)

3. **`ios/Classes/VerifyFaceView.swift`** — Face verification + liveness view
   - Adapted from native: replace NavigationLink/onDismiss with closure callback
   - Standalone view (no NavigationView wrapper)

4. **`ios/Classes/LivenessDetectView.swift`** — Liveness detection only view
   - Adapted from native: closure callback for results

5. **`ios/Classes/AddFaceByCamera.swift`** — Face enrollment view
   - Used for both `startEnroll` and `addFace`
   - Adapted callback to return faceFeature and faceImage

6. **`ios/Classes/ScreenBrightnessHelper.swift`** — Screen brightness utility (copy as-is)

7. **`ios/Classes/CustomToastView.swift`** — Toast UI component (copy as-is)

## Adaptations from Native to Plugin

1. **Callback pattern**: Native uses `onDismiss: { code, similarity, liveness in }` → Plugin wraps in closure that calls `FlutterResult` with dictionary
2. **Image handling**: Convert `UIImage` to base64 string or save to temp file and return path, based on `format` parameter ("base64" or "filePath")
3. **UserDefaults**: Kept for face feature storage, consistent with native app
4. **No NavigationView**: Views presented as fullscreen modals, not in a navigation stack
5. **Thread safety**: Ensure FlutterResult called on main thread

## Result Formats (consistent with Android)

### startVerification
```swift
["code": Int, "faceID": String, "msg": String, "similarity": Double, "livenessValue": Double, "faceImage": String]
```

### startLiveness
```swift
["code": Int, "msg": String, "livenessValue": Double, "faceImage": String]
```

### startEnroll & addFace
```swift
["code": Int, "faceID": String, "msg": String, "faceFeature": String, "faceImage": String]
```

### initializeSDK
```swift
"initialized" // String
```

## Podspec Dependency

```ruby
s.dependency 'FaceAISDK_Core'
# Source specified via Podfile in example app or consumer app
```

Note: CocoaPods podspec `s.dependency` does not support `:git` or `:tag` — the consuming app's Podfile must specify the git source.

## iOS Requirements

- iOS deployment target: 13.0 (from podspec) — note: FaceAISDK_Core requires 15.5+, so effective minimum is 15.5
- Swift 5.0+
- Camera permission (NSCameraUsageDescription in consumer app's Info.plist)
