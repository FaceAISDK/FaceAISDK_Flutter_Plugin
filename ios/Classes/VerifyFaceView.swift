import SwiftUI
import FaceAISDK_Core

struct VerifyFaceView: View {
    @StateObject private var viewModel: VerifyFaceModel = VerifyFaceModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showLightHighDialog = false
    @State private var showToast = false
    @State private var toastViewTips: String = ""
    @State private var timerProgress: CGFloat = 0
    @State private var timerSecondsLeft: Int = 0
    @State private var countdownTimer: Timer? = nil

    var autoControlBrightness: Bool = true
    var dismissAction: (() -> Void)? = nil

    let faceID: String
    let threshold: Float
    let livenessType: Int
    let motionLiveness: String
    let motionLivenessTimeOut: Int
    let motionLivenessSteps: Int

    var passedFaceFeature: String? = nil

    // callback: (code, similarity, liveness, faceImage?)
    let onDismiss: (Int, Float, Float, UIImage?) -> Void

    private var toastOverlay: some View {
        let isSuccess = viewModel.faceVerifyResult.similarity > threshold && viewModel.faceVerifyResult.liveness > 0.8
        let displayMessage: String = {
            if !toastViewTips.isEmpty {
                return toastViewTips
            } else if isSuccess {
                return FaceAILocalization.localizedTip(for: 62)
            } else {
                return FaceAILocalization.localizedTip(for: 63)
            }
        }()
        let toastStyle: ToastStyle = isSuccess ? .success : .failure

        return VStack {
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

    private func localizedTip(for code: Int) -> String {
        return FaceAILocalization.localizedTip(for: code, defaultPrefix: "VerifyFace")
    }

    private func close() {
        stopTimer()
        if let dismissAction = dismissAction {
            dismissAction()
        } else {
            dismiss()
        }
    }

    private func startTimer() {
        let total = motionLivenessTimeOut
        guard total > 0 else { return }
        timerSecondsLeft = total
        timerProgress = 0

        let interval: TimeInterval = 0.1
        var elapsed: Double = 0

        countdownTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { timer in
            elapsed += interval
            let progress = CGFloat(elapsed / Double(total))
            if progress >= 1.0 {
                timerProgress = 1.0
                timerSecondsLeft = 0
                timer.invalidate()
            } else {
                timerProgress = progress
                timerSecondsLeft = total - Int(elapsed)
            }
        }
    }

    private func stopTimer() {
        countdownTimer?.invalidate()
        countdownTimer = nil
    }

    var body: some View {
        ZStack {
            VStack {
                HStack {
                    Button(action: {
                        onDismiss(0, 0.0, 0.0, nil)
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

                ZStack {
                    FaceAICameraView(session: viewModel.captureSession, cameraSize: FaceCameraSize)
                        .frame(width: FaceCameraSize, height: FaceCameraSize)
                        .aspectRatio(1.0, contentMode: .fit)
                        .clipShape(Circle())

                    // Circular timer track (background)
                    Circle()
                        .stroke(Color.gray.opacity(0.2), lineWidth: 4)
                        .frame(width: FaceCameraSize + 8, height: FaceCameraSize + 8)

                    // Circular timer progress
                    Circle()
                        .trim(from: 0, to: timerProgress)
                        .stroke(
                            timerProgress > 0.7 ? Color.red : Color.faceMain,
                            style: StrokeStyle(lineWidth: 4, lineCap: .round)
                        )
                        .frame(width: FaceCameraSize + 8, height: FaceCameraSize + 8)
                        .rotationEffect(.degrees(-90))
                        .animation(.linear(duration: 0.1), value: timerProgress)

                    // Timer seconds label
                    VStack {
                        Spacer()
                        Text("\(timerSecondsLeft)s")
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(
                                Capsule()
                                    .fill(timerProgress > 0.7 ? Color.red : Color.faceMain)
                            )
                            .offset(y: 6)
                    }
                    .frame(width: FaceCameraSize + 8, height: FaceCameraSize + 8)
                }
                .padding(.vertical, 8)

                Spacer()
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(viewModel.colorFlash.ignoresSafeArea())
            .navigationBarBackButtonHidden(true)
            .navigationBarHidden(true)

            if showToast {
                toastOverlay
            }

            if showLightHighDialog {
                ZStack {
                    VStack(spacing: 22) {
                        Text(FaceAILocalization.localizedTip(for: viewModel.faceVerifyResult.code, defaultPrefix: "VerifyFace"))
                            .font(.system(size: 16).bold())
                            .fontWeight(.semibold)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.black)
                            .padding(.horizontal, 25)

                        if let uiImage = UIImage(named: "light_too_high", in: FaceAILocalization.bundle, compatibleWith: nil) {
                            Image(uiImage: uiImage)
                                .resizable()
                                .scaledToFit()
                                .frame(maxHeight: 120)
                                .padding(.horizontal, 1)
                        }

                        Button(action: {
                            withAnimation {
                                showLightHighDialog = false
                                onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.similarity, viewModel.faceVerifyResult.liveness, viewModel.faceVerifyResult.faceImage)
                                close()
                            }
                        }) {
                            Text(FaceAILocalization.localized("Confirm"))
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

            let faceFeature: String?
            if let passed = passedFaceFeature, !passed.isEmpty {
                faceFeature = passed
            } else {
                faceFeature = UserDefaults.standard.string(forKey: faceID)
            }

            guard let feature = faceFeature else {
                toastViewTips = FaceAILocalization.localized("No Face Feature")
                showToast = true

                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    showToast = false
                    onDismiss(6, 0.0, 0.0, nil)
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
            startTimer()
        }
        .onChange(of: viewModel.faceVerifyResult.code) { newValue in
            stopTimer()
            toastViewTips = ""

            if newValue == VerifyResultCode.COLOR_LIVENESS_LIGHT_TOO_HIGH {
                withAnimation {
                    showLightHighDialog = true
                }
            } else {
                showToast = true

                let faceImage = viewModel.faceVerifyResult.faceImage

                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    withAnimation {
                        showToast = false
                    }
                    onDismiss(viewModel.faceVerifyResult.code, viewModel.faceVerifyResult.similarity, viewModel.faceVerifyResult.liveness, faceImage)
                    close()
                }
            }
        }
        .onDisappear {
            stopTimer()
            if autoControlBrightness {
                ScreenBrightnessHelper.shared.restoreBrightness()
            }
            viewModel.stopFaceVerify()
        }
        .animation(.easeInOut(duration: 0.3), value: showToast)
    }
}
