import SwiftUI
import UIKit
import Combine
import FaceAISDK_Core

@objcMembers
public class FaceSDKSwiftManager: NSObject {

    // MARK: - 特征值管理
    public static func getiOSFaceFeature(_ faceId: String) -> String {
        return UserDefaults.standard.string(forKey: faceId) ?? ""
    }

    public static func isFaceFeatureExist(_ faceId: String) -> Bool {
        let feature = self.getiOSFaceFeature(faceId)
        return !feature.isEmpty
    }

    public static func deleteFaceFeature(_ faceId: String) {
        UserDefaults.standard.removeObject(forKey: faceId)
        UserDefaults.standard.removeObject(forKey: "\(faceId)_base64")
        UserDefaults.standard.synchronize()
    }

    public static func insertFaceFeature(_ faceId: String, _ feature: String, _ callback: @escaping (NSNumber, String) -> Void) {
        if feature.isEmpty || feature.count < 100 {
            callback(0, "特征值长度不正确")
            return
        }

        UserDefaults.standard.set(feature, forKey: faceId)
        UserDefaults.standard.synchronize()
        callback(1, "\(faceId)人脸数据插入成功")
    }

    public static func getFaceImageBase64(_ faceId: String) -> String {
        return UserDefaults.standard.string(forKey: "\(faceId)_base64") ?? ""
    }

    // MARK: - Base64 提取人脸特征
    public static func addFaceByBase64(_ faceId: String, _ base64Str: String, _ callback: @escaping (NSNumber, String, String) -> Void) {
        var cleanBase64 = base64Str
        if let idx = cleanBase64.range(of: "base64,")?.upperBound {
            cleanBase64 = String(cleanBase64[idx...])
        }

        guard let data = Data(base64Encoded: cleanBase64, options: .ignoreUnknownCharacters),
              let image = UIImage(data: data) else {
            callback(0, "", "图片Base64解析失败")
            return
        }

        DispatchQueue.main.async {
            let model = AddFaceByImageModel()
            let feature = model.getFaceFeature(faceUIImage: image)

            if !feature.isEmpty {
                UserDefaults.standard.set(feature, forKey: faceId)
                UserDefaults.standard.set(cleanBase64, forKey: "\(faceId)_base64")
                UserDefaults.standard.synchronize()
                callback(1, feature, "提取人脸特征成功")
            } else {
                callback(0, "", "未能提取到人脸特征")
            }
        }
    }

    // MARK: - 呼出相机录入人脸
    public static func showAddFaceByCamera(_ faceId: String, _ performanceMode: NSNumber, _ needConfirm: Bool, _ callback: @escaping (NSNumber, String) -> Void) {
        DispatchQueue.main.async {
            guard let topVC = self.getTopViewController() else {
                callback(0, "无法获取顶层控制器")
                return
            }

            var sdkView = AddFaceByCamera(
                faceID: faceId,
                addFacePerformanceMode: performanceMode.intValue,
                needShowConfirmDialog: needConfirm,
                onDismiss: { [weak topVC] (resultCode: Int, feature: String) in
                    let safeCode = NSNumber(value: resultCode)
                    DispatchQueue.main.async {
                        ScreenBrightnessHelper.shared.restoreBrightness()
                        topVC?.dismiss(animated: true) {
                            callback(safeCode, feature)
                        }
                    }
                }
            )
            sdkView.autoControlBrightness = true

            let hostingController = UIHostingController(rootView: sdkView)
            hostingController.modalPresentationStyle = .fullScreen
            topVC.present(hostingController, animated: true)
        }
    }

    // MARK: - 1:1 人脸识别
    public static func showFaceVerify(_ faceId: String, _ threshold: NSNumber, _ livenessType: NSNumber, _ motionLivenessTypes: String, _ motionLivenessTimeOut : NSNumber, _ motionLivenessSteps : NSNumber, _ callback: @escaping (NSNumber, NSNumber, NSNumber,String) -> Void) {
        DispatchQueue.main.async {
            guard let topVC = self.getTopViewController() else { return }

            ScreenBrightnessHelper.shared.maximizeBrightness()

            let sdkView = VerifyFaceView(
                faceID: faceId,
                threshold: threshold.floatValue,
                livenessType: livenessType.intValue,
                motionLiveness: motionLivenessTypes,
                motionLivenessTimeOut: motionLivenessTimeOut.intValue,
                motionLivenessSteps: motionLivenessSteps.intValue,
                onDismiss: { [weak topVC] (code: Int, similarity: Float, liveness: Float) in
                    DispatchQueue.main.async {
                        ScreenBrightnessHelper.shared.restoreBrightness()
                        topVC?.dismiss(animated: true) {
                            callback(NSNumber(value: code), NSNumber(value: similarity), NSNumber(value: liveness))
                        }
                    }
                }
            )

            let hostingController = UIHostingController(rootView: sdkView)
            hostingController.modalPresentationStyle = .fullScreen
            topVC.present(hostingController, animated: true)
        }
    }

    // MARK: - 活体检测
    public static func showLivenessVerify(_ livenessType: NSNumber, _ motionLivenessTypes: String, _ motionLivenessTimeOut : NSNumber, _ motionLivenessSteps : NSNumber,  _ callback: @escaping (NSNumber, NSNumber,String) -> Void) {
        DispatchQueue.main.async {
            guard let topVC = self.getTopViewController() else { return }

            ScreenBrightnessHelper.shared.maximizeBrightness()

            let sdkView = LivenessDetectView(
                livenessType: livenessType.intValue,
                motionLiveness: motionLivenessTypes,
                motionLivenessTimeOut: motionLivenessTimeOut.intValue,
                motionLivenessSteps: motionLivenessSteps.intValue,
                onDismiss: { [weak topVC] (code: Int, liveness: Float) in
                    DispatchQueue.main.async {
                        ScreenBrightnessHelper.shared.restoreBrightness()
                        topVC?.dismiss(animated: true) {
                            callback(NSNumber(value: code), NSNumber(value: liveness), "")
                        }
                    }
                }
            )

            let hostingController = UIHostingController(rootView: sdkView)
            hostingController.modalPresentationStyle = .fullScreen
            topVC.present(hostingController, animated: true)
        }
    }

    public static func goNativeDemoNavi() {
        DispatchQueue.main.async {
            guard let topVC = self.getTopViewController() else { return }
        }
    }

    // MARK: - 辅助方法
    private static func getTopViewController() -> UIViewController? {
        let windowScene = UIApplication.shared.connectedScenes
            .first { $0.activationState == .foregroundActive } as? UIWindowScene
        guard let keyWindow = windowScene?.windows.first(where: { $0.isKeyWindow }),
              let rootVC = keyWindow.rootViewController else {
            return nil
        }

        var topController = rootVC
        while let presentedViewController = topController.presentedViewController {
            topController = presentedViewController
        }
        return topController
    }
}
