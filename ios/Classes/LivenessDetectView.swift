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

    // callback: (code, liveness, faceImage?)
    let onDismiss: (Int, Float, UIImage?) -> Void

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
                        onDismiss(0, 0.0, nil)
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
                                onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.liveness, viewModel.faceVerifyResult.faceImage)
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

                let faceImage = viewModel.faceVerifyResult.faceImage

                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    withAnimation {
                        showToast = false
                    }
                    onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.liveness, faceImage)
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
