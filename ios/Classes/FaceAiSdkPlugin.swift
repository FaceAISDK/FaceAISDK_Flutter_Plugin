import Flutter
import UIKit
import SwiftUI
import FaceAISDK_Core

public class FaceAiSdkPlugin: NSObject, FlutterPlugin {
    private var isSDKInitialized = false
    private var pendingResult: FlutterResult? = nil
    private var presentedHostingController: UIViewController? = nil

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
                onDismiss: { [weak self] code, similarity, liveness, faceImage in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImageStr = self.convertUIImage(faceImage, format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "faceID": faceId ?? "",
                        "msg": self.messageForCode(code),
                        "similarity": Double(similarity),
                        "livenessValue": Double(liveness),
                        "faceImage": faceImageStr ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                    self.dismissPresentedView()
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
                onDismiss: { [weak self] code, liveness, faceImage in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImageStr = self.convertUIImage(faceImage, format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "msg": self.messageForCode(code),
                        "livenessValue": Double(liveness),
                        "faceImage": faceImageStr ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                    self.dismissPresentedView()
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
                onDismiss: { [weak self] code, faceFeature, faceImage in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImageStr = self.convertUIImage(faceImage, format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "faceID": faceId,
                        "msg": code == 1 ? "success" : "cancelled",
                        "faceFeature": faceFeature ?? "",
                        "faceImage": faceImageStr ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                    self.dismissPresentedView()
                },
                autoControlBrightness: false,
                needConfirmAddFace: false
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
                onDismiss: { [weak self] code, faceFeature, faceImage in
                    guard let self = self else { return }
                    ScreenBrightnessHelper.shared.restoreBrightness()

                    let faceImageStr = self.convertUIImage(faceImage, format: format)

                    let resultMap: [String: Any?] = [
                        "code": code,
                        "faceID": faceId,
                        "msg": code == 1 ? "success" : "cancelled",
                        "faceFeature": faceFeature ?? "",
                        "faceImage": faceImageStr ?? ""
                    ]

                    if let pending = self.pendingResult {
                        self.pendingResult = nil
                        pending(resultMap)
                    }
                    self.dismissPresentedView()
                },
                autoControlBrightness: false,
                needConfirmAddFace: false
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

        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        let hostingController = UIHostingController(rootView: view)
        hostingController.modalPresentationStyle = .fullScreen
        self.presentedHostingController = hostingController

        topVC.present(hostingController, animated: true)
    }

    // MARK: - Dismiss Presented View

    private func dismissPresentedView() {
        DispatchQueue.main.async { [weak self] in
            self?.presentedHostingController?.dismiss(animated: true) {
                self?.presentedHostingController = nil
            }
        }
    }

    // MARK: - Helper: Convert UIImage to base64 or filePath

    private func convertUIImage(_ image: UIImage?, format: String) -> String? {
        guard let image = image else { return nil }
        guard let jpegData = image.jpegData(compressionQuality: 0.9) else { return nil }

        if format == "filePath" {
            let tempDir = NSTemporaryDirectory()
            let filePath = (tempDir as NSString).appendingPathComponent("face_\(Int(Date().timeIntervalSince1970 * 1000)).jpg")
            do {
                try jpegData.write(to: URL(fileURLWithPath: filePath))
                return filePath
            } catch {
                return nil
            }
        }

        return jpegData.base64EncodedString()
    }

    // MARK: - Helper: Message for Code

    private func messageForCode(_ code: Int) -> String {
        let key = "Face_Tips_Code_\(code)"
        let defaultValue = "Code \(code)"
        return NSLocalizedString(key, value: defaultValue, comment: "")
    }
}
