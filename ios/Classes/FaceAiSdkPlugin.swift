import Flutter
import UIKit

public class FaceAiSdkPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "face_aisdk_flutter_plugin", binaryMessenger: registrar.messenger())
    let instance = FaceAiSdkPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)

    let factory = FaceAiSdkViewFactory(messenger: registrar.messenger())
    registrar.register(factory, withId: "com.faceaisdk/view")
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let args = call.arguments as? [String: Any]
    print("FaceAiSdkPlugin handle method: \(call.method), args: \(String(describing: args))")

    switch call.method {
    case "addFaceBySDKCamera":
      let faceId = args?["faceId"] as? String ?? ""
      let performanceMode = args?["addFacePerformanceMode"] as? NSNumber ?? 0

      // 兼容多种 Bool 传递方式
      let needConfirm = (args?["needShowConfirmDialog"] as? NSNumber)?.boolValue
                       ?? (args?["needShowConfirmDialog"] as? Bool)
                       ?? false

      FaceSDKSwiftManager.showAddFaceByCamera(faceId, performanceMode, needConfirm) { code, feature in
          var faceBase64 = ""
          if code.intValue != 0 {
              faceBase64 = FaceSDKSwiftManager.getFaceImageBase64(faceId)
          }
          let res: [String: Any] = ["code": code, "faceFeature": feature, "faceBase64": faceBase64]
          print("FaceAiSdkPlugin addFaceBySDKCamera result: \(res)")
          result(res)
      }

    case "addFaceBySDKImage":
      let faceId = args?["faceId"] as? String ?? ""
      let imageBase64 = args?["imageBase64"] as? String ?? ""
      FaceSDKSwiftManager.addFaceByBase64(faceId, imageBase64) { code, feature, msg in
          let res: [String: Any] = ["code": code, "faceFeature": feature, "message": msg]
          print("FaceAiSdkPlugin addFaceBySDKImage result: \(res)")
          result(res)
      }

    case "faceVerify":
      let faceId = args?["faceId"] as? String ?? ""
      let threshold = args?["threshold"] as? NSNumber ?? 0.84
      let livenessType = args?["livenessType"] as? NSNumber ?? 1
      let motionLivenessTypes = args?["motionLivenessTypes"] as? String ?? "1,2,3,4,5"
      let motionLivenessTimeOut = args?["motionLivenessTimeOut"] as? NSNumber ?? 7
      let motionLivenessSteps = args?["motionLivenessSteps"] as? NSNumber ?? 2
      FaceSDKSwiftManager.showFaceVerify(faceId, threshold, livenessType, motionLivenessTypes, motionLivenessTimeOut, motionLivenessSteps) { code, similarity, liveness in
          var faceBase64 = ""
          if code.intValue == 1 {
              faceBase64 = FaceSDKSwiftManager.getFaceImageBase64("verifyBitmap")
          }
          let res: [String: Any] = [
              "code": code,
              "similarity": similarity,
              "livenessValue": liveness,
              "faceBase64": faceBase64
          ]
          print("FaceAiSdkPlugin faceVerify result: \(res)")
          result(res)
      }

    case "livenessVerify":
      let livenessType = args?["livenessType"] as? NSNumber ?? 1
      let motionLivenessTypes = args?["motionLivenessTypes"] as? String ?? "1,2,3,4,5"
      let motionLivenessTimeOut = args?["motionLivenessTimeOut"] as? NSNumber ?? 7
      let motionLivenessSteps = args?["motionLivenessSteps"] as? NSNumber ?? 2
      FaceSDKSwiftManager.showLivenessVerify(livenessType, motionLivenessTypes, motionLivenessTimeOut, motionLivenessSteps) { code, liveness in
          var faceBase64 = ""
          if code.intValue == 10 {
              faceBase64 = FaceSDKSwiftManager.getFaceImageBase64("liveBitmap")
          }
          let res: [String: Any] = [
              "code": code,
              "livenessValue": liveness,
              "faceBase64": faceBase64
          ]
          print("FaceAiSdkPlugin livenessVerify result: \(res)")
          result(res)
      }

    case "deleteFaceFeature":
      let faceId = args?["faceId"] as? String ?? ""
      FaceSDKSwiftManager.deleteFaceFeature(faceId)
      print("FaceAiSdkPlugin deleteFaceFeature faceId: \(faceId)")
      result(true)

    case "insertFaceFeature":
      let faceId = args?["faceId"] as? String ?? ""
      let feature = args?["feature"] as? String ?? ""
      FaceSDKSwiftManager.insertFaceFeature(faceId, feature) { code, msg in
          print("FaceAiSdkPlugin insertFaceFeature result: \(code), msg: \(msg)")
          result(code.boolValue)
      }

    case "getFaceFeature":
      let faceId = args?["faceId"] as? String ?? ""
      result(FaceSDKSwiftManager.getiOSFaceFeature(faceId))

    case "isFaceExist":
      let faceId = args?["faceId"] as? String ?? ""
      let exists = FaceSDKSwiftManager.isFaceFeatureExist(faceId)
      print("FaceAiSdkPlugin isFaceExist: \(exists)")
      result(exists)

    case "getFaceImageBase64":
      let faceId = args?["faceId"] as? String ?? ""
      result(FaceSDKSwiftManager.getFaceImageBase64(faceId))

    case "goNativeDemoNavi":
      FaceSDKSwiftManager.goNativeDemoNavi()
      result(nil)

    default:
      result(FlutterMethodNotImplemented)
    }
  }
}
