import SwiftUI
import AVFoundation
import FaceAISDK_Core

@MainActor
var FaceCameraSize: CGFloat {
    15 * min(UIScreen.main.bounds.width, UIScreen.main.bounds.height) / 20
}

public struct AddFaceByCamera: View {
    let faceID: String
    // callback: (code, faceFeature?, faceImage?)
    let onDismiss: (Int, String?, UIImage?) -> Void
    var autoControlBrightness: Bool = true
    var dismissAction: (() -> Void)? = nil
    var needConfirmAddFace: Bool = true

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

    private func saveFaceAndFinish() {
        UserDefaults.standard.set(viewModel.faceFeatureBySDKCamera, forKey: faceID)

        if FaceImageManger.saveFaceImage(faceName: faceID, faceImage: viewModel.croppedFaceImage) {
            print("saveFaceImage success")
        }

        let faceImage = viewModel.croppedFaceImage

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            onDismiss(1, viewModel.faceFeatureBySDKCamera, faceImage)
            close()
        }
    }

    public var body: some View {
        ZStack {
            VStack(spacing: 20) {
                HStack {
                    Button(action: {
                        onDismiss(0, nil, nil)
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
                        if needConfirmAddFace {
                            Color.black.opacity(0.3)
                                .clipShape(Circle())

                            ConfirmAddFaceDialog(
                                viewModel: viewModel,
                                cameraSize: FaceCameraSize,
                                onConfirm: {
                                    saveFaceAndFinish()
                                }
                            )
                            .transition(.scale.combined(with: .opacity))
                        }
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
            .onChange(of: viewModel.readyConfirmFace) { ready in
                if ready && !needConfirmAddFace {
                    saveFaceAndFinish()
                }
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
