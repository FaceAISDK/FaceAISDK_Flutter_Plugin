# iOS Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all FaceAI SDK method channel handlers on iOS by copying and adapting native SwiftUI views, so that `initializeSDK`, `startVerification`, `startLiveness`, `startEnroll`, and `addFace` work on iOS identically to Android.

**Architecture:** Flutter Dart calls MethodChannel → `FaceAiSdkPlugin.swift` routes to the correct SwiftUI view → view is presented as fullscreen modal via `UIHostingController` → result returned via `FlutterResult` closure.

**Tech Stack:** Swift 5.0, SwiftUI, FaceAISDK_Core (CocoaPods), Flutter iOS plugin API

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `ios/face_ai_sdk.podspec` | Modify | Add FaceAISDK_Core dependency, bump iOS to 15.5 |
| `example/ios/Podfile` | Modify | Add FaceAISDK_Core git source |
| `ios/Classes/ScreenBrightnessHelper.swift` | Create | Screen brightness utility (copy from native) |
| `ios/Classes/CustomToastView.swift` | Create | Toast UI component (copy from native) |
| `ios/Classes/AddFaceByCamera.swift` | Create | Face enrollment view (adapt from native) |
| `ios/Classes/VerifyFaceView.swift` | Create | Face verification + liveness view (adapt from native) |
| `ios/Classes/LivenessDetectView.swift` | Create | Liveness detection view (adapt from native) |
| `ios/Classes/FaceAiSdkPlugin.swift` | Modify | Main plugin — route all method calls, present views, return results |

---

### Task 1: Update podspec and Podfile for FaceAISDK_Core dependency

**Files:**
- Modify: `ios/face_ai_sdk.podspec`
- Modify: `example/ios/Podfile`

- [ ] **Step 1: Update podspec**

Edit `ios/face_ai_sdk.podspec` to add the FaceAISDK_Core dependency and raise iOS minimum to 15.5:

```ruby
Pod::Spec.new do |s|
  s.name             = 'face_ai_sdk'
  s.version          = '1.0.1'
  s.summary          = 'FaceAI SDK Flutter Plugin for iOS'
  s.description      = <<-DESC
FaceAI SDK Flutter Plugin - Face verification, liveness detection, and face enrollment.
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'FaceAISDK_Core'
  s.platform = :ios, '15.5'

  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
```

- [ ] **Step 2: Update example Podfile**

Edit `example/ios/Podfile` to add the FaceAISDK_Core git source. Add this near the top (after the `ENV` line) and set the platform:

```ruby
platform :ios, '15.5'

# FaceAISDK_Core source
pod 'FaceAISDK_Core', :git => 'https://github.com/FaceAISDK/FaceAISDK_Core.git', :tag => '2026.05.28.xcode15'
```

The `pod 'FaceAISDK_Core'` line goes inside the `target 'Runner' do` block, after `flutter_install_all_ios_pods`. The platform line replaces the commented-out one at the top.

Full `example/ios/Podfile`:

```ruby
platform :ios, '15.5'

# CocoaPods analytics sends network stats synchronously affecting flutter build latency.
ENV['COCOAPODS_DISABLE_STATS'] = 'true'

project 'Runner', {
  'Debug' => :debug,
  'Profile' => :release,
  'Release' => :release,
}

def flutter_root
  generated_xcode_build_settings_path = File.expand_path(File.join('..', 'Flutter', 'Generated.xcconfig'), __FILE__)
  unless File.exist?(generated_xcode_build_settings_path)
    raise "#{generated_xcode_build_settings_path} must exist. If you're running pod install manually, make sure flutter pub get is executed first"
  end

  File.foreach(generated_xcode_build_settings_path) do |line|
    matches = line.match(/FLUTTER_ROOT\=(.*)/)
    return matches[1].strip if matches
  end
  raise "FLUTTER_ROOT not found in #{generated_xcode_build_settings_path}. Try deleting Generated.xcconfig, then run flutter pub get"
end

require File.expand_path(File.join('packages', 'flutter_tools', 'bin', 'podhelper'), flutter_root)

flutter_ios_podfile_setup

target 'Runner' do
  use_frameworks!

  flutter_install_all_ios_pods File.dirname(File.realpath(__FILE__))

  pod 'FaceAISDK_Core', :git => 'https://github.com/FaceAISDK/FaceAISDK_Core.git', :tag => '2026.05.28.xcode15'

  target 'RunnerTests' do
    inherit! :search_paths
  end
end

post_install do |installer|
  installer.pods_project.targets.each do |target|
    flutter_additional_ios_build_settings(target)
  end
end
```

- [ ] **Step 3: Commit**

```bash
git add ios/face_ai_sdk.podspec example/ios/Podfile
git commit -m "feat(ios): add FaceAISDK_Core dependency and bump iOS to 15.5"
```

---

### Task 2: Copy utility files (ScreenBrightnessHelper, CustomToastView)

**Files:**
- Create: `ios/Classes/ScreenBrightnessHelper.swift`
- Create: `ios/Classes/CustomToastView.swift`

- [ ] **Step 1: Create ScreenBrightnessHelper.swift**

Copy from `/Users/mac-kantor/Documents/GitHub/FaceAISDK_iOS/FaceAISDK_iOS/ScreenBrightnessHelper.swift` to `ios/Classes/ScreenBrightnessHelper.swift`. No changes needed — copy as-is.

```swift
import UIKit

public class ScreenBrightnessHelper {

    public static let shared = ScreenBrightnessHelper()

    private var originalBrightness: CGFloat?
    private var wasIdleTimerDisabled: Bool = false
    private var isMaximized = false

    private init() {}

    public func maximizeBrightness() {
        runOnMain { [weak self] in
            guard let self = self else { return }

            if !self.isMaximized {
                self.originalBrightness = self.getCurrentBrightness()
                self.wasIdleTimerDisabled = UIApplication.shared.isIdleTimerDisabled
                self.isMaximized = true
            }

            self.setBrightness(1.0)
            UIApplication.shared.isIdleTimerDisabled = true
        }
    }

    public func restoreBrightness() {
        runOnMain { [weak self] in
            guard let self = self else { return }

            guard self.isMaximized, let original = self.originalBrightness else { return }

            self.setBrightness(original)
            UIApplication.shared.isIdleTimerDisabled = self.wasIdleTimerDisabled

            self.isMaximized = false
            self.originalBrightness = nil
        }
    }

    private func getCurrentBrightness() -> CGFloat {
        if #available(iOS 15.0, *) {
            let scene = UIApplication.shared.connectedScenes
                .filter { $0.activationState == .foregroundActive }
                .compactMap { $0 as? UIWindowScene }
                .first
            return scene?.screen.brightness ?? UIScreen.main.brightness
        } else {
            return UIScreen.main.brightness
        }
    }

    private func setBrightness(_ value: CGFloat) {
        if #available(iOS 15.0, *) {
            if let scene = UIApplication.shared.connectedScenes
                .filter({ $0.activationState == .foregroundActive })
                .compactMap({ $0 as? UIWindowScene })
                .first {
                scene.screen.brightness = value
            } else {
                UIScreen.main.brightness = value
            }
        } else {
            UIScreen.main.brightness = value
        }
    }

    private func runOnMain(_ block: @escaping () -> Void) {
        if Thread.isMainThread {
            block()
        } else {
            DispatchQueue.main.async {
                block()
            }
        }
    }
}
```

- [ ] **Step 2: Create CustomToastView.swift**

Copy from `/Users/mac-kantor/Documents/GitHub/FaceAISDK_iOS/FaceAISDK_iOS/CustomToastView.swift` to `ios/Classes/CustomToastView.swift`.

The native app uses `Color.faceMain` (a custom color from Assets.xcassets). Since we don't have that asset in the plugin, replace it with a hardcoded color. Check the native app's `faceMain.colorset` for the actual color value, or use a sensible default like `Color(red: 0.2, green: 0.6, blue: 1.0)`.

```swift
import Foundation
import SwiftUI

enum ToastStyle {
    case success
    case failure
    var backgroundColor: Color {
        switch self {
        case .success: return Color(red: 0.2, green: 0.6, blue: 1.0)
        case .failure: return Color.yellow
        }
    }
}

struct CustomToastView: View {
    let message: String
    let style: ToastStyle
    var body: some View {
        VStack {
            HStack {
                Text(message)
                    .foregroundColor(.white)
                    .font(.system(size: 19).bold())
                    .padding(.vertical, 14)
                    .padding(.horizontal, 22)
            }
            .background(style.backgroundColor)
            .cornerRadius(25)
            .shadow(radius: 5)
        }
    }
}
```

**Important:** We also need a `Color.faceMain` extension since the SwiftUI views from the native app reference it. Add this at the top of `CustomToastView.swift` (or create a separate file). For simplicity, add it here:

```swift
// Add before the ToastStyle enum
extension Color {
    static let faceMain = Color(red: 0.2, green: 0.6, blue: 1.0)
}
```

- [ ] **Step 3: Commit**

```bash
git add ios/Classes/ScreenBrightnessHelper.swift ios/Classes/CustomToastView.swift
git commit -m "feat(ios): add ScreenBrightnessHelper and CustomToastView utilities"
```

---

### Task 3: Create AddFaceByCamera.swift (adapted for Flutter plugin)

**Files:**
- Create: `ios/Classes/AddFaceByCamera.swift`

This view is used for both `startEnroll` and `addFace` method calls. The key adaptation: instead of using `@Environment(\.dismiss)` and `NavigationLink`, we use a completion closure that the plugin calls with the result dictionary.

- [ ] **Step 1: Create AddFaceByCamera.swift**

Copy from `/Users/mac-kantor/Documents/GitHub/FaceAISDK_iOS/FaceAISDK_iOS/addFace/AddFaceByCamera.swift` and adapt:

1. Set `autoControlBrightness = false` (plugin manages brightness externally)
2. Replace `dismiss()` calls with a `dismissAction` closure provided by the plugin
3. Keep `onDismiss` callback signature: `(Int, String?) -> Void`
4. Include the `ConfirmAddFaceDialog` struct
5. Include the `FaceCameraSize` computed property

```swift
import SwiftUI
import AVFoundation
import FaceAISDK_Core

@MainActor
var FaceCameraSize: CGFloat {
    14 * min(UIScreen.main.bounds.width, UIScreen.main.bounds.height) / 20
}

public struct AddFaceByCamera: View {
    let faceID: String
    let onDismiss: (Int, String?) -> Void
    var autoControlBrightness: Bool = true
    var dismissAction: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @StateObject private var viewModel: AddFaceByCameraModel = AddFaceByCameraModel()

    private func localizedTip(for code: Int) -> String {
        let key = "Face_Tips_Code_\(code)"
        let defaultValue = "Add Face Tips Code=\(code)"
        return NSLocalizedString(key, value: defaultValue, comment: "")
    }

    private func close() {
        if let dismissAction = dismissAction {
            dismissAction()
        } else {
            dismiss()
        }
    }

    public var body: some View {
        ZStack {
            VStack(spacing: 20) {
                HStack {
                    Button(action: {
                        onDismiss(0, nil)
                        close()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.black)
                            .padding(10)
                            .background(Color.gray.opacity(0.1))
                            .clipShape(Circle())
                    }
                    Spacer()
                }
                .padding(.horizontal, 2)
                .padding(.top, 10)

                Text(localizedTip(for: viewModel.sdkInterfaceTips.code))
                    .font(.system(size: 19).bold())
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .foregroundColor(.white)
                    .background(Color.faceMain)
                    .cornerRadius(20)

                ZStack {
                    FaceAICameraView(session: viewModel.captureSession, cameraSize: FaceCameraSize)
                        .aspectRatio(1.0, contentMode: .fit)
                        .clipShape(Circle())
                        .background(Circle().fill(Color.white))
                        .overlay(Circle().stroke(Color.gray, lineWidth: 1))

                    if viewModel.readyConfirmFace {
                        Color.black.opacity(0.3)
                            .clipShape(Circle())

                        ConfirmAddFaceDialog(
                            viewModel: viewModel,
                            cameraSize: FaceCameraSize,
                            onConfirm: {
                                UserDefaults.standard.set(viewModel.faceFeatureBySDKCamera, forKey: faceID)

                                if FaceImageManager.saveFaceImage(faceName: faceID, faceImage: viewModel.croppedFaceImage) {
                                    print("saveFaceImage success")
                                }

                                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                                    onDismiss(1, viewModel.faceFeatureBySDKCamera)
                                    close()
                                }
                            }
                        )
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .frame(width: FaceCameraSize, height: FaceCameraSize)
                .animation(.easeInOut(duration: 0.25), value: viewModel.readyConfirmFace)

                Spacer()
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white.ignoresSafeArea())
            .navigationBarBackButtonHidden(true)
            .navigationBarHidden(true)
            .onAppear {
                if autoControlBrightness {
                    ScreenBrightnessHelper.shared.maximizeBrightness()
                }
                viewModel.initAddFace()
            }
            .onDisappear {
                if autoControlBrightness {
                    ScreenBrightnessHelper.shared.restoreBrightness()
                }
                viewModel.stopAddFace()
            }
        }
    }
}

struct ConfirmAddFaceDialog: View {
    let viewModel: AddFaceByCameraModel
    let cameraSize: CGFloat
    let onConfirm: () -> Void

    var body: some View {
        VStack(alignment: .center, spacing: 15) {
            Text("Confirm Add Face")
                .font(.system(size: 19, weight: .semibold))
                .foregroundColor(Color.faceMain)
                .padding(.top, 16)

            Image(uiImage: viewModel.croppedFaceImage)
                .resizable()
                .scaledToFill()
                .frame(width: 130, height: 130)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                )
                .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)

            Text("Ensure face is clear")
                .font(.system(size: 15))
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            HStack(spacing: 12) {
                Button(action: {
                    viewModel.reInit()
                }) {
                    Text("Retry")
                        .font(.system(size: 16, weight: .medium))
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(Color.gray.opacity(0.6))
                        .foregroundColor(.primary)
                        .cornerRadius(8)
                }

                Button(action: {
                    onConfirm()
                }) {
                    Text("Confirm")
                        .font(.system(size: 16, weight: .bold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(Color.faceMain)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 18)
            .padding(.top, 5)
        }
        .frame(width: cameraSize * 1.11)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.2), radius: 10, x: 0, y: 5)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ios/Classes/AddFaceByCamera.swift
git commit -m "feat(ios): add AddFaceByCamera view for enrollment and addFace"
```

---

### Task 4: Create VerifyFaceView.swift (adapted for Flutter plugin)

**Files:**
- Create: `ios/Classes/VerifyFaceView.swift`

Key adaptations:
1. Set `autoControlBrightness = false` for plugin use
2. Add `dismissAction` closure for programmatic dismissal from UIHostingController
3. Add `passedFaceFeature` parameter — when provided, use it directly instead of looking up UserDefaults by faceID (this supports the Dart `faceFeature` parameter)
4. Keep `onDismiss` callback: `(Int, Float, Float) -> Void` (code, similarity, liveness)

- [ ] **Step 1: Create VerifyFaceView.swift**

```swift
import SwiftUI
import FaceAISDK_Core

struct VerifyFaceView: View {
    @StateObject private var viewModel: VerifyFaceModel = VerifyFaceModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showLightHighDialog = false
    @State private var showToast = false
    @State private var toastViewTips: String = ""

    var autoControlBrightness: Bool = true
    var dismissAction: (() -> Void)? = nil

    let faceID: String
    let threshold: Float
    let livenessType: Int
    let motionLiveness: String
    let motionLivenessTimeOut: Int
    let motionLivenessSteps: Int

    // Optional: pass face feature directly (from Flutter faceFeature param)
    var passedFaceFeature: String? = nil

    let onDismiss: (Int, Float, Float) -> Void

    private func localizedTip(for code: Int) -> String {
        let key = "Face_Tips_Code_\(code)"
        let defaultValue = "VerifyFace Tips Code=\(code)"
        return NSLocalizedString(key, value: defaultValue, comment: "")
    }

    private func close() {
        if let dismissAction = dismissAction {
            dismissAction()
        } else {
            dismiss()
        }
    }

    var body: some View {
        ZStack {
            VStack {
                HStack {
                    Button(action: {
                        onDismiss(0, 0.0, 0.0)
                        close()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.black)
                            .padding(10)
                            .background(Color.gray.opacity(0.1))
                            .clipShape(Circle())
                    }
                    Spacer()
                }
                .padding(.horizontal, 2)
                .padding(.top, 10)

                Text(localizedTip(for: viewModel.sdkInterfaceTips.code))
                    .font(.system(size: 20).bold())
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .foregroundColor(.white)
                    .background(Color.faceMain)
                    .cornerRadius(20)

                Text(localizedTip(for: viewModel.sdkInterfaceTipsExtra.code))
                    .font(.system(size: 20).bold())
                    .padding(.bottom, 6)
                    .frame(minHeight: 30)
                    .foregroundColor(.black)

                FaceAICameraView(session: viewModel.captureSession, cameraSize: FaceCameraSize)
                    .frame(width: FaceCameraSize, height: FaceCameraSize)
                    .padding(.vertical, 8)
                    .aspectRatio(1.0, contentMode: .fit)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.gray, lineWidth: 1))

                Spacer()
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(viewModel.colorFlash.ignoresSafeArea())
            .navigationBarBackButtonHidden(true)
            .navigationBarHidden(true)

            if showToast {
                let similarity = String(format: "%.2f", viewModel.faceVerifyResult.similarity)
                let displayTips = toastViewTips.isEmpty ? viewModel.faceVerifyResult.tips : toastViewTips
                let displayMessage = (toastViewTips.isEmpty) ? "\(displayTips)" : displayTips
                let isSuccess = viewModel.faceVerifyResult.similarity > threshold && viewModel.faceVerifyResult.liveness > 0.8
                let toastStyle: ToastStyle = isSuccess ? .success : .failure

                VStack {
                    Spacer()
                    CustomToastView(
                        message: displayMessage,
                        style: toastStyle
                    )
                    .padding(.bottom, 77)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(1)
            }

            if showLightHighDialog {
                ZStack {
                    VStack(spacing: 22) {
                        Text(viewModel.faceVerifyResult.tips)
                            .font(.system(size: 16).bold())
                            .fontWeight(.semibold)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.black)
                            .padding(.horizontal, 25)

                        Button(action: {
                            withAnimation {
                                showLightHighDialog = false
                                onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.similarity, viewModel.faceVerifyResult.liveness)
                                close()
                            }
                        }) {
                            Text("Confirm")
                                .font(.system(size: 18).bold())
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.faceMain)
                                .cornerRadius(10)
                        }
                        .padding(.horizontal, 30)
                    }
                    .padding(.vertical, 22)
                    .background(Color.white)
                    .cornerRadius(20)
                    .shadow(color: Color.black.opacity(0.2), radius: 20, x: 0, y: 10)
                    .padding(.horizontal, 30)
                }
                .zIndex(2)
                .transition(.scale(scale: 0.8).combined(with: .opacity))
            }
        }
        .onAppear {
            if autoControlBrightness {
                ScreenBrightnessHelper.shared.maximizeBrightness()
            }

            withAnimation(.easeInOut(duration: 0.3)) {
                UIScreen.main.brightness = 1.0
            }

            // Use passedFaceFeature if available, otherwise look up from UserDefaults
            let faceFeature: String?
            if let passed = passedFaceFeature, !passed.isEmpty {
                faceFeature = passed
            } else {
                faceFeature = UserDefaults.standard.string(forKey: faceID)
            }

            guard let feature = faceFeature else {
                toastViewTips = "No Face Feature for key: \(faceID)"
                showToast = true

                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    showToast = false
                    onDismiss(6, 0.0, 0.0)
                    close()
                }
                return
            }

            viewModel.initFaceAISDK(
                faceIDFeature: feature,
                threshold: threshold,
                livenessType: livenessType,
                onlyLiveness: false,
                motionLiveness: motionLiveness,
                motionLivenessTimeOut: motionLivenessTimeOut,
                motionLivenessSteps: motionLivenessSteps
            )
        }
        .onChange(of: viewModel.faceVerifyResult.code) { newValue in
            toastViewTips = ""

            if newValue == VerifyResultCode.COLOR_LIVENESS_LIGHT_TOO_HIGH {
                withAnimation {
                    showLightHighDialog = true
                }
            } else {
                showToast = true

                if FaceImageManager.saveFaceImage(faceName: faceID, faceImage: viewModel.faceVerifyResult.faceImage) {
                    print("saveFaceImage success")
                }

                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    withAnimation {
                        showToast = false
                    }
                    onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.similarity, viewModel.faceVerifyResult.liveness)
                    close()
                }
            }
        }
        .onDisappear {
            if autoControlBrightness {
                ScreenBrightnessHelper.shared.restoreBrightness()
            }
            viewModel.stopFaceVerify()
        }
        .animation(.easeInOut(duration: 0.3), value: showToast)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ios/Classes/VerifyFaceView.swift
git commit -m "feat(ios): add VerifyFaceView for face verification + liveness"
```

---

### Task 5: Create LivenessDetectView.swift (adapted for Flutter plugin)

**Files:**
- Create: `ios/Classes/LivenessDetectView.swift`

Key adaptations: same pattern — `dismissAction` closure, `autoControlBrightness` defaults.

- [ ] **Step 1: Create LivenessDetectView.swift**

```swift
import SwiftUI
import AVFoundation
import FaceAISDK_Core

struct LivenessDetectView: View {
    @StateObject private var viewModel: VerifyFaceModel = VerifyFaceModel()
    @State private var showToast = false
    @State private var showLightHighDialog = false
    @Environment(\.dismiss) private var dismiss

    var autoControlBrightness: Bool = true
    var dismissAction: (() -> Void)? = nil

    let livenessType: Int
    let motionLiveness: String
    let motionLivenessTimeOut: Int
    let motionLivenessSteps: Int

    let onDismiss: (Int, Float) -> Void

    private func localizedTip(for code: Int) -> String {
        let key = "Face_Tips_Code_\(code)"
        let defaultValue = "LivenessDetect Tips Code=\(code)"
        return NSLocalizedString(key, value: defaultValue, comment: "")
    }

    private func close() {
        if let dismissAction = dismissAction {
            dismissAction()
        } else {
            dismiss()
        }
    }

    var body: some View {
        ZStack {
            VStack {
                HStack {
                    Button(action: {
                        onDismiss(0, 0.0)
                        close()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.black)
                            .padding(10)
                            .background(Color.gray.opacity(0.1))
                            .clipShape(Circle())
                    }
                    Spacer()
                }
                .padding(.horizontal, 10)
                .padding(.top, 10)

                Text(localizedTip(for: viewModel.sdkInterfaceTips.code))
                    .font(.system(size: 20).bold())
                    .padding(.horizontal, 20)
                    .padding(.vertical, 9)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .background(Color.faceMain)
                    .cornerRadius(20)

                Text(localizedTip(for: viewModel.sdkInterfaceTipsExtra.code))
                    .font(.system(size: 20).bold())
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 8)
                    .frame(minHeight: 30)
                    .foregroundColor(.black)

                FaceAICameraView(session: viewModel.captureSession, cameraSize: FaceCameraSize)
                    .frame(width: FaceCameraSize, height: FaceCameraSize)
                    .aspectRatio(1.0, contentMode: .fit)
                    .padding(.vertical, 8)
                    .clipShape(Circle())

                Spacer()
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(viewModel.colorFlash.ignoresSafeArea())
            .navigationBarBackButtonHidden(true)
            .navigationBarHidden(true)

            if showToast {
                let isSuccess = viewModel.faceVerifyResult.liveness > 0.8
                let toastStyle: ToastStyle = isSuccess ? .success : .failure

                VStack {
                    Spacer()
                    CustomToastView(
                        message: "\(viewModel.faceVerifyResult.tips)",
                        style: toastStyle
                    )
                    .padding(.bottom, 77)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .zIndex(1)
            }

            if showLightHighDialog {
                ZStack {
                    VStack(spacing: 22) {
                        Text(viewModel.faceVerifyResult.tips)
                            .font(.system(size: 16).bold())
                            .fontWeight(.semibold)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.black)
                            .padding(.horizontal, 25)

                        Button(action: {
                            withAnimation {
                                showLightHighDialog = false
                                onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.liveness)
                                close()
                            }
                        }) {
                            Text("Confirm")
                                .font(.system(size: 18).bold())
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.faceMain)
                                .cornerRadius(10)
                        }
                        .padding(.horizontal, 30)
                    }
                    .padding(.vertical, 22)
                    .background(Color.white)
                    .cornerRadius(20)
                    .shadow(color: Color.black.opacity(0.2), radius: 20, x: 0, y: 10)
                    .padding(.horizontal, 30)
                }
                .zIndex(2)
                .transition(.scale(scale: 0.8).combined(with: .opacity))
            }
        }
        .onAppear {
            if autoControlBrightness {
                ScreenBrightnessHelper.shared.maximizeBrightness()
            }

            withAnimation(.easeInOut(duration: 0.3)) {
                UIScreen.main.brightness = 1.0
            }

            viewModel.initFaceAISDK(
                faceIDFeature: "",
                livenessType: livenessType,
                onlyLiveness: true,
                motionLiveness: motionLiveness,
                motionLivenessTimeOut: motionLivenessTimeOut,
                motionLivenessSteps: motionLivenessSteps
            )
        }
        .onChange(of: viewModel.faceVerifyResult.code) { newValue in
            if newValue == VerifyResultCode.COLOR_LIVENESS_LIGHT_TOO_HIGH {
                withAnimation {
                    showLightHighDialog = true
                }
            } else {
                showToast = true

                if FaceImageManager.saveFaceImage(faceName: "Liveness", faceImage: viewModel.faceVerifyResult.faceImage) {
                    // saved
                }

                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    withAnimation {
                        showToast = false
                    }
                    onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.liveness)
                    close()
                }
            }
        }
        .onDisappear {
            if autoControlBrightness {
                ScreenBrightnessHelper.shared.restoreBrightness()
            }
            viewModel.stopFaceVerify()
        }
        .animation(.easeInOut(duration: 0.3), value: showToast)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add ios/Classes/LivenessDetectView.swift
git commit -m "feat(ios): add LivenessDetectView for liveness detection"
```

---

### Task 6: Implement FaceAiSdkPlugin.swift (main plugin with all method handlers)

**Files:**
- Modify: `ios/Classes/FaceAiSdkPlugin.swift`

This is the core task. The plugin must:
1. Route all method channel calls
2. Extract arguments from Flutter
3. Present SwiftUI views via UIHostingController as fullscreen modal
4. Manage brightness externally (since views run with `autoControlBrightness = false`)
5. Convert face images to base64 or file path
6. Return results as dictionaries matching the Android format

- [ ] **Step 1: Rewrite FaceAiSdkPlugin.swift**

Replace the entire contents of `ios/Classes/FaceAiSdkPlugin.swift` with:

```swift
import Flutter
import UIKit
import SwiftUI
import FaceAISDK_Core

public class FaceAiSdkPlugin: NSObject, FlutterPlugin {
    private var isSDKInitialized = false
    private var pendingResult: FlutterResult? = nil

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "face_ai_sdk", binaryMessenger: registrar.messenger())
        let instance = FaceAiSdkPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "getPlatformVersion":
            result("iOS " + UIDevice.current.systemVersion)
        case "initializeSDK":
            handleInitializeSDK(result: result)
        case "startVerification":
            handleStartVerification(call: call, result: result)
        case "startLiveness":
            handleStartLiveness(call: call, result: result)
        case "startEnroll":
            handleStartEnroll(call: call, result: result)
        case "addFace":
            handleAddFace(call: call, result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }

    // MARK: - Initialize SDK

    private func handleInitializeSDK(result: @escaping FlutterResult) {
        isSDKInitialized = true
        result("SDK initialized successfully")
    }

    // MARK: - Start Verification

    private func handleStartVerification(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard isSDKInitialized else {
            result(FlutterError(code: "NOT_INITIALIZED", message: "SDK not initialized, call initializeSDK first", details: nil))
            return
        }
        guard pendingResult == nil else {
            result(FlutterError(code: "ALREADY_ACTIVE", message: "Another operation is in progress", details: nil))
            return
        }

        let args = call.arguments as? [String: Any] ?? [:]
        let faceId = args["faceId"] as? String
        let faceFeature = args["faceFeature"] as? String
        let threshold = Float(args["threshold"] as? Double ?? 0.85)
        let livenessType = args["livenessType"] as? Int ?? 0
        let motionStepSize = args["motionStepSize"] as? Int ?? 1
        let motionTimeout = args["motionTimeout"] as? Int ?? 10
        let motionTypes = args["motionTypes"] as? String ?? "1,2,3"
        let format = args["format"] as? String ?? "base64"

        if (faceId == nil || faceId!.isEmpty) && (faceFeature == nil || faceFeature!.isEmpty) {
            result(FlutterError(code: "INVALID_ARGS", message: "At least one of faceId or faceFeature is required", details: nil))
            return
        }

        pendingResult = result

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            ScreenBrightnessHelper.shared.maximizeBrightness()

            let verifyView = VerifyFaceView(
                autoControlBrightness: false,
                faceID: faceId ?? "",
                threshold: threshold,
                livenessType: livenessType,
                motionLiveness: motionTypes,
                motionLivenessTimeOut: motionTimeout,
                motionLivenessSteps: motionStepSize,
                passedFaceFeature: faceFeature,
                onDismiss: { [weak self] code, similarity, liveness in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImage = self.loadFaceImage(faceName: faceId ?? "verify", format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "faceID": faceId ?? "",
                        "msg": self.messageForCode(code),
                        "similarity": Double(similarity),
                        "livenessValue": Double(liveness),
                        "faceImage": faceImage ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                }
            )

            self.presentSwiftUIView(verifyView)
        }
    }

    // MARK: - Start Liveness

    private func handleStartLiveness(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard isSDKInitialized else {
            result(FlutterError(code: "NOT_INITIALIZED", message: "SDK not initialized, call initializeSDK first", details: nil))
            return
        }
        guard pendingResult == nil else {
            result(FlutterError(code: "ALREADY_ACTIVE", message: "Another operation is in progress", details: nil))
            return
        }

        let args = call.arguments as? [String: Any] ?? [:]
        let livenessType = args["livenessType"] as? Int ?? 1
        let motionStepSize = args["motionStepSize"] as? Int ?? 1
        let motionTimeout = args["motionTimeout"] as? Int ?? 10
        let motionTypes = args["motionTypes"] as? String ?? "1,2,3"
        let format = args["format"] as? String ?? "base64"

        pendingResult = result

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            ScreenBrightnessHelper.shared.maximizeBrightness()

            let livenessView = LivenessDetectView(
                autoControlBrightness: false,
                livenessType: livenessType,
                motionLiveness: motionTypes,
                motionLivenessTimeOut: motionTimeout,
                motionLivenessSteps: motionStepSize,
                onDismiss: { [weak self] code, liveness in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImage = self.loadFaceImage(faceName: "Liveness", format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "msg": self.messageForCode(code),
                        "livenessValue": Double(liveness),
                        "faceImage": faceImage ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                }
            )

            self.presentSwiftUIView(livenessView)
        }
    }

    // MARK: - Start Enroll

    private func handleStartEnroll(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard isSDKInitialized else {
            result(FlutterError(code: "NOT_INITIALIZED", message: "SDK not initialized, call initializeSDK first", details: nil))
            return
        }
        guard pendingResult == nil else {
            result(FlutterError(code: "ALREADY_ACTIVE", message: "Another operation is in progress", details: nil))
            return
        }

        let args = call.arguments as? [String: Any] ?? [:]
        guard let faceId = args["faceId"] as? String, !faceId.isEmpty else {
            result(FlutterError(code: "INVALID_ARGS", message: "faceId is required", details: nil))
            return
        }
        let format = args["format"] as? String ?? "base64"

        pendingResult = result

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            ScreenBrightnessHelper.shared.maximizeBrightness()

            let addFaceView = AddFaceByCamera(
                faceID: faceId,
                onDismiss: { [weak self] code, faceFeature in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImage = self.loadFaceImage(faceName: faceId, format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "faceID": faceId,
                        "msg": code == 1 ? "success" : "cancelled",
                        "faceFeature": faceFeature ?? "",
                        "faceImage": faceImage ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                },
                autoControlBrightness: false
            )

            self.presentSwiftUIView(addFaceView)
        }
    }

    // MARK: - Add Face

    private func handleAddFace(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard isSDKInitialized else {
            result(FlutterError(code: "NOT_INITIALIZED", message: "SDK not initialized, call initializeSDK first", details: nil))
            return
        }
        guard pendingResult == nil else {
            result(FlutterError(code: "ALREADY_ACTIVE", message: "Another operation is in progress", details: nil))
            return
        }

        let args = call.arguments as? [String: Any] ?? [:]
        guard let faceId = args["faceId"] as? String, !faceId.isEmpty else {
            result(FlutterError(code: "INVALID_ARGS", message: "faceId is required", details: nil))
            return
        }
        let format = args["format"] as? String ?? "base64"

        pendingResult = result

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            ScreenBrightnessHelper.shared.maximizeBrightness()

            let addFaceView = AddFaceByCamera(
                faceID: faceId,
                onDismiss: { [weak self] code, faceFeature in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImage = self.loadFaceImage(faceName: faceId, format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "faceID": faceId,
                        "msg": code == 1 ? "success" : "cancelled",
                        "faceFeature": faceFeature ?? "",
                        "faceImage": faceImage ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                },
                autoControlBrightness: false
            )

            self.presentSwiftUIView(addFaceView)
        }
    }

    // MARK: - Present SwiftUI View

    private func presentSwiftUIView<V: View>(_ view: V) {
        guard let rootVC = UIApplication.shared.delegate?.window??.rootViewController ??
              UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .flatMap({ $0.windows })
                .first(where: { $0.isKeyWindow })?.rootViewController else {
            pendingResult?(FlutterError(code: "NO_VIEW_CONTROLLER", message: "Cannot find root view controller", details: nil))
            pendingResult = nil
            return
        }

        // Find the topmost presented view controller
        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        let hostingController = UIHostingController(rootView: view)
        hostingController.modalPresentationStyle = .fullScreen

        topVC.present(hostingController, animated: true)
    }

    // MARK: - Helper: Load Face Image

    private func loadFaceImage(faceName: String, format: String) -> String? {
        guard let base64 = FaceImageManager.faceImageToBase64(fileName: faceName) else {
            return nil
        }

        if format == "filePath" {
            // Save base64 data to temp file and return path
            guard let imageData = Data(base64Encoded: base64) else { return nil }
            let tempDir = NSTemporaryDirectory()
            let filePath = (tempDir as NSString).appendingPathComponent("\(faceName)_\(Int(Date().timeIntervalSince1970)).jpg")
            do {
                try imageData.write(to: URL(fileURLWithPath: filePath))
                return filePath
            } catch {
                return nil
            }
        }

        return base64
    }

    // MARK: - Helper: Message for Code

    private func messageForCode(_ code: Int) -> String {
        let key = "Face_Tips_Code_\(code)"
        let defaultValue = "Code \(code)"
        return NSLocalizedString(key, value: defaultValue, comment: "")
    }
}
```

- [ ] **Step 2: Verify the `dismissAction` wiring**

The SwiftUI views have a `dismissAction` closure but in the current approach we use `@Environment(\.dismiss)` which works when the view is presented via `UIHostingController.present()`. However, `dismiss()` may not work reliably in all cases for a programmatically presented `UIHostingController`.

We need to wire `dismissAction` to dismiss the hosting controller. Update the `presentSwiftUIView` method and the view construction calls.

**Problem:** SwiftUI structs are value types — we can't pass a reference to the hosting controller to the view before creating it. Instead, we should handle dismissal in the `onDismiss` callback inside the plugin.

**Solution:** After `onDismiss` fires and we send the Flutter result, dismiss the hosting controller from the plugin side. Update `presentSwiftUIView` to store the hosting controller reference, and dismiss it in each `onDismiss` handler.

Update `FaceAiSdkPlugin.swift`:

Add a property:
```swift
private var presentedHostingController: UIViewController? = nil
```

Update `presentSwiftUIView`:
```swift
private func presentSwiftUIView<V: View>(_ view: V) {
    guard let rootVC = UIApplication.shared.delegate?.window??.rootViewController ??
          UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap({ $0.windows })
            .first(where: { $0.isKeyWindow })?.rootViewController else {
        pendingResult?(FlutterError(code: "NO_VIEW_CONTROLLER", message: "Cannot find root view controller", details: nil))
        pendingResult = nil
        return
    }

    var topVC = rootVC
    while let presented = topVC.presentedViewController {
        topVC = presented
    }

    let hostingController = UIHostingController(rootView: view)
    hostingController.modalPresentationStyle = .fullScreen
    self.presentedHostingController = hostingController

    topVC.present(hostingController, animated: true)
}
```

Add a dismiss helper:
```swift
private func dismissPresentedView() {
    DispatchQueue.main.async { [weak self] in
        self?.presentedHostingController?.dismiss(animated: true) {
            self?.presentedHostingController = nil
        }
    }
}
```

Then in each `onDismiss` handler, after sending the result, call `self.dismissPresentedView()`. For example in verification:
```swift
onDismiss: { [weak self] code, similarity, liveness in
    guard let self = self else { return }
    ScreenBrightnessHelper.shared.restoreBrightness()

    let faceImage = self.loadFaceImage(faceName: faceId ?? "verify", format: format)

    let resultMap: [String: Any?] = [
        "code": code,
        "faceID": faceId ?? "",
        "msg": self.messageForCode(code),
        "similarity": Double(similarity),
        "livenessValue": Double(liveness),
        "faceImage": faceImage ?? ""
    ]

    if let pending = self.pendingResult {
        self.pendingResult = nil
        pending(resultMap)
    }
    self.dismissPresentedView()
}
```

Apply the same pattern to all four `onDismiss` handlers (verification, liveness, enroll, addFace).

- [ ] **Step 3: Commit**

```bash
git add ios/Classes/FaceAiSdkPlugin.swift
git commit -m "feat(ios): implement all method channel handlers in FaceAiSdkPlugin"
```

---

### Task 7: Run pod install and verify build

**Files:** No file changes — build verification only.

- [ ] **Step 1: Run pod install in example app**

```bash
cd /Users/mac-kantor/Documents/GitHub/FaceAISDK_Flutter_Plugin/example/ios
pod install
```

Expected: Pods installed successfully, including FaceAISDK_Core.

- [ ] **Step 2: Build the iOS example app**

```bash
cd /Users/mac-kantor/Documents/GitHub/FaceAISDK_Flutter_Plugin/example
flutter build ios --no-codesign
```

Expected: Build succeeds. If there are compilation errors, fix them in the relevant Swift files.

- [ ] **Step 3: Fix any compilation issues found**

Common issues to watch for:
- `Color.faceMain` not found → ensure the `Color` extension is in `CustomToastView.swift`
- `FaceCameraSize` duplicate symbol → ensure it's only defined once (in `AddFaceByCamera.swift`)
- `VerifyResultCode` not accessible → it comes from `FaceAISDK_Core`, ensure `import FaceAISDK_Core` is in each file
- `FaceImageManager` (note: original uses this spelling, not "Manager") → comes from `FaceAISDK_Core`
- SwiftUI view init parameter ordering issues → match exact parameter order from struct definitions

- [ ] **Step 4: Commit any fixes**

```bash
git add ios/Classes/
git commit -m "fix(ios): resolve compilation issues"
```

---

### Task 8: Verify the Dart API works end-to-end (manual test checklist)

This is a manual verification task. Run the example app on an iOS device (simulator won't have camera).

- [ ] **Step 1: Verify initializeSDK**

In the example app, call `initializeSDK`. Expected: returns "SDK initialized successfully".

- [ ] **Step 2: Verify startEnroll**

Call `startEnroll(faceId: "testUser")`. Expected: camera opens fullscreen, can capture face, returns code 1 with faceFeature.

- [ ] **Step 3: Verify startVerification**

After enrollment, call `startVerification(faceId: "testUser", livenessType: 1)`. Expected: camera opens, performs liveness + face match, returns code/similarity/liveness.

- [ ] **Step 4: Verify startLiveness**

Call `startLiveness(livenessType: 1)`. Expected: camera opens, liveness detection runs, returns code/liveness.

- [ ] **Step 5: Verify addFace**

Call `addFace(faceId: "searchUser")`. Expected: camera opens, captures face, returns code/faceFeature.

- [ ] **Step 6: Verify cancellation**

In any view, press the back button. Expected: returns code 0 (cancelled).
