import Foundation
import Combine
import UIKit
import SwiftUI
import CoreVideo
import AVFoundation
import VisionKit
import Vision
import MLKitFaceDetection
import MLKitVision

/**
 *  使用SDK相机添加人脸照片，提示人脸角度摆正
 *
 */
public class AddFaceByCameraModel:NSObject, ObservableObject,@unchecked Sendable,
                            AVCaptureVideoDataOutputSampleBufferDelegate {
    
    @Published public var faceFeatureBySDKCamera: String = ""

    @Published public var croppedFaceImage: UIImage = UIImage() //可以让用户确认的图
    @Published public var originFaceImage: UIImage = UIImage()  //原图
    
    @Published public var sdkInterfaceTips:SDKInterfaceTips=SDKInterfaceTips()
    
    @Published public var readyConfirmFace: Bool = false
    
    public let captureSession = AVCaptureSession()  //视频流采集
    
    private let sessionQueue = DispatchQueue(label: "FaceAI Session Queue")
    private let processingQueue = DispatchQueue(label: "FaceAI Processing Queue", qos: .userInitiated)
    
    private var videoAnalysisOutput = AVCaptureVideoDataOutput()
    private var isSessionConfigured: Bool = false
    private var faceAngelOKTime: Int = 0
    
    //是否正在处理
    private var isDisposing: Bool = false
    private var lastVerifyTime: Int = 0
    private var frameCounter: Int = 0
    private let frameSkipCount = 3      //每3帧处理1帧，减少处理频率
    private var lastTipsCode: Int = SDKInterfaceTips().code
    
    //检测摄像头的方向
    private let faceOrientation = UIUtils.imageOrientation(fromDevicePosition: .front)
    private let faceDetector = FaceAIDetector.instance.getFaceDetector()
    
    private let registry = AppRegistry()
    
    /**
     结束添加人脸
     */
    public func stopAddFace() {
        weak var weakSelf = self
        sessionQueue.async {
            guard let strongSelf = weakSelf else {

                return
            }
            if strongSelf.captureSession.isRunning {
                strongSelf.captureSession.stopRunning()
            }
        }
        processingQueue.async { [weak self] in
            self?.isDisposing = false
        }
    }
    
    
    /**
     初始化添加人脸
     */
    public func initAddFace() {
        weak var weakSelf = self
        
        sessionQueue.async {
            guard let strongSelf = weakSelf else {
                print("Self is nil!")
                NSLog("===> %@ Error: %@", "UTS", "Self is nil")
                return
            }
            
            if !strongSelf.isSessionConfigured {
                strongSelf.captureSession.beginConfiguration()
                defer { strongSelf.captureSession.commitConfiguration() }
                
                // 使用较低分辨率提高性能
                strongSelf.captureSession.sessionPreset = AVCaptureSession.Preset.vga640x480
                
                if strongSelf.captureSession.inputs.isEmpty {
                    guard let videoDevice = AVCaptureDevice.default(.builtInWideAngleCamera,
                                                                    for: .video,
                                                                    position: .front),
                          let videoDeviceInput = try? AVCaptureDeviceInput(device: videoDevice),
                          strongSelf.captureSession.canAddInput(videoDeviceInput) else {
                        print("Failed to add capture session input.")
                        return
                    }
                    strongSelf.captureSession.addInput(videoDeviceInput)
                }
                
                if !strongSelf.captureSession.outputs.contains(strongSelf.videoAnalysisOutput) {
                    strongSelf.videoAnalysisOutput.videoSettings = [
                        (kCVPixelBufferPixelFormatTypeKey as String): kCVPixelFormatType_32BGRA
                    ]
                    strongSelf.videoAnalysisOutput.alwaysDiscardsLateVideoFrames = true
                    strongSelf.videoAnalysisOutput.setSampleBufferDelegate(strongSelf, queue: strongSelf.processingQueue)
                    
                    guard strongSelf.captureSession.canAddOutput(strongSelf.videoAnalysisOutput) else {
                        print("Failed to add capture session output.")
                        return
                    }
                    strongSelf.captureSession.addOutput(strongSelf.videoAnalysisOutput)
                }
                
                strongSelf.isSessionConfigured = true
            }
            
            if !strongSelf.captureSession.isRunning {
                strongSelf.captureSession.startRunning()
            }
            strongSelf.resetDetectionState(clearPublished: false)
        }
        
    }
    
    
    /**
     * 分析帧数据
     */
    public func captureOutput(_ output: AVCaptureOutput,
                              didOutput sampleBuffer: CMSampleBuffer,
                              from connection: AVCaptureConnection) {
        
        // 跳过一些帧以减少处理频率
        frameCounter += 1
        if frameCounter % frameSkipCount != 0 {
            return
        }
        
        detect2AddFace(faceAIBuffer: sampleBuffer)
    }
    
    /**
     *  重新初始化
     */
    public func reInit(){
        resetDetectionState(clearPublished: true)
    }
    
    private func resetDetectionState(clearPublished: Bool) {
        processingQueue.async { [weak self] in
            guard let self = self else { return }
            self.faceAngelOKTime = 0
            self.isDisposing = false
            self.frameCounter = 0
            self.lastTipsCode = SDKInterfaceTips().code
            //延迟1秒开始检测添加人脸环节
            self.lastVerifyTime = 500 + Int(Date().timeIntervalSince1970 * 1000)
            
            if clearPublished {
                DispatchQueue.main.async { [weak self] in
                    guard let self = self else { return }
                    self.faceFeatureBySDKCamera = ""
                    self.croppedFaceImage = UIImage()
                    self.originFaceImage = UIImage()
                    self.readyConfirmFace = false
                    self.sdkInterfaceTips = SDKInterfaceTips()
                }
            }
        }
    }
    
    private func updateSDKInterfaceTips(code: Int, message: String) {
        guard lastTipsCode != code else { return }
        lastTipsCode = code
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.sdkInterfaceTips.code = code
            self.sdkInterfaceTips.message = message
        }
    }
    
    private func publishFaceAddSuccess(feature: String, croppedImage: UIImage, originImage: UIImage) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.faceFeatureBySDKCamera = feature
            self.croppedFaceImage = croppedImage
            self.originFaceImage = originImage
            self.readyConfirmFace = true
        }
    }
    
    private func publishFaceAddFailure() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.faceFeatureBySDKCamera = ""
            self.readyConfirmFace = false
        }
    }
    
    /**
     * 实时检测摄像头人脸看看是否可以添加人脸
     *
     */
    private func detect2AddFace(faceAIBuffer: CMSampleBuffer) {
        
        if readyConfirmFace || isDisposing {
            return
        }
        
        let nowTime = Int(Date().timeIntervalSince1970 * 1000)
        
        if nowTime - lastVerifyTime < 111 { //判断人脸
            return
        }
        
        lastVerifyTime = nowTime
        isDisposing = true
        
        // 使用优化的一次性检测和对齐方法
        FaceAIDetector.instance.detectAndAlignFace(mode:FaceAIDetector.DetectorMode.addFace,sampleBuffer: faceAIBuffer) {
           [weak self] face, alignedImage,originImage in
            self?.processingQueue.async { [weak self] in
            guard let self = self, let detectedFace = face ,let alignedImage = alignedImage,let originImage = originImage else {
                self?.updateSDKInterfaceTips(code: FaceTipsCode.NO_FACE_DETECTED, message: "未检测到人脸")
                self?.isDisposing = false
                return
            }
            
            
            // 检查人脸大小
            if detectedFace.frame.size.width < 266 {
                self.updateSDKInterfaceTips(code: FaceTipsCode.COME_CLOSER, message: "人脸太小了")
                self.isDisposing = false

                return
            }
            
            
            if let currentID = Bundle.main.bundleIdentifier {
                let isValid = registry.isBundleIDRegistered(currentID)
                
                if isValid {
                    print("✅ CurrentID (\(currentID)) Registered")
                } else {
                    let version = ProcessInfo.processInfo.operatingSystemVersion
                    if version.majorVersion > 26 {
                        self.updateSDKInterfaceTips(code: FaceTipsCode.UPDATE_FACE_SDK, message: "FaceAISDK Version Too Old")
                        self.isDisposing = false
                        return
                    }
                }
            }

            
            //检测要采集的人脸角度是否是正脸
            if self.isFaceAngelAligned(mlkitFace: detectedFace) {
                
                //停留1秒太久了，优化一下为0.8秒
                let nowTime = Int(Date().timeIntervalSince1970 * 1000) //毫秒

                if self.faceAngelOKTime == 0 {
                    self.faceAngelOKTime = nowTime
                    self.updateSDKInterfaceTips(code: FaceTipsCode.KEEP_FACE_FRONTAL, message: "录入中，保持正脸不晃动")
                    self.isDisposing = false
                } else if nowTime - self.faceAngelOKTime > 800 {
                    
                    // 假设这是在子线程进行耗时的特征提取
                    let feature = FaceAISDKEngine.shared.croppedImage2Feature(image: alignedImage)
                    
                    if !feature.isEmpty , !readyConfirmFace {
                        self.updateSDKInterfaceTips(code: FaceTipsCode.CONFIRM_ADD_FACE, message: "人脸采集确认中")
                        self.publishFaceAddSuccess(feature: feature, croppedImage: alignedImage, originImage: originImage)
                    } else {
                        self.updateSDKInterfaceTips(code: FaceTipsCode.FACE_CROP_FAILED, message: "特征提取失败，请重试")
                        self.publishFaceAddFailure()
                        self.isDisposing = false
                    }
                    
                } else {
                    self.isDisposing = false
                }
                
            } else {
                self.isDisposing = false
                self.faceAngelOKTime = 0
            }
            }
        }
        
    }
    
    
    /**
     *  人脸角度是否正常，提示人脸摆正角度
     *
     */
    private func isFaceAngelAligned(mlkitFace: Face) -> Bool {
        let angelLeft = -9.0, angelRight = 9.0, angelTop = 9.0, angelBottom = -9.0, angleZ_N = -6.0, angleZ_P = 6.0
        let openEye = 0.1;
        
        let leftEyeOpen = mlkitFace.leftEyeOpenProbability
        let rightEyeOpen = mlkitFace.rightEyeOpenProbability
        
        if leftEyeOpen < openEye, rightEyeOpen < openEye {
            updateSDKInterfaceTips(code: FaceTipsCode.NO_CLOSE_EYE, message: "不要闭眼睛")
            return false
        } else if mlkitFace.headEulerAngleY < angelLeft {
            updateSDKInterfaceTips(code: FaceTipsCode.TURN_FACE_RIGHT, message: "向右摆正头部")
            return false
        } else if mlkitFace.headEulerAngleY > angelRight {
            updateSDKInterfaceTips(code: FaceTipsCode.TURN_FACE_LEFT, message: "向左摆正头部")
            return false
        } else if mlkitFace.headEulerAngleZ < angleZ_N {
            updateSDKInterfaceTips(code: FaceTipsCode.NO_TILT_FACE, message: "不要歪头")
            return false
        } else if mlkitFace.headEulerAngleZ > angleZ_P {
            updateSDKInterfaceTips(code: FaceTipsCode.NO_TILT_FACE, message: "不要歪头")
            return false
        } else if mlkitFace.headEulerAngleX < angelBottom {
            updateSDKInterfaceTips(code: FaceTipsCode.TURN_FACE_TOP, message: "向上摆正头部")
            return false
        } else if mlkitFace.headEulerAngleX > angelTop {
            updateSDKInterfaceTips(code: FaceTipsCode.TURN_FACE_BOTTOM, message: "向下摆正头部")
            return false
        } else {
            return true
        }
    }
    
 
}
