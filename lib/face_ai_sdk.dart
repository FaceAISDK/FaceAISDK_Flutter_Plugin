import 'face_ai_sdk_platform_interface.dart';

class FaceAiSdk {
  Future<String?> getPlatformVersion() {
    return FaceAiSdkPlatform.instance.getPlatformVersion();
  }

  /// Initialize the FaceAI SDK. Must be called before any other method.
  ///
  /// [config] - Configuration map. Supported keys:
  ///   - `locale` (String) - iOS only. Sets UI language for face views.
  ///     Supported: "en" (default), "id", "zh-Hans".
  Future<String> initializeSDK(Map<String, dynamic> config) {
    return FaceAiSdkPlatform.instance.initializeSDK(config);
  }

  /// 1:1 Face Verification. At least one of [faceId] or [faceFeature] must be provided.
  ///
  /// [faceId] - Unique identifier for the face (used for MMKV lookup).
  /// [faceFeature] - Face feature string to verify against directly (skips MMKV lookup).
  /// [threshold] - Recognition threshold [0.75-0.95], default 0.85.
  /// [livenessType] - 0=NONE, 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT.
  /// [motionStepSize] - Number of motion steps [1-2].
  /// [motionTimeout] - Motion timeout in seconds [3-22].
  /// [motionTypes] - Comma-separated motion types: 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod.
  /// [allowRetry] - Whether to allow retry on timeout/failure. If false, finishes immediately.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, faceID, msg, similarity, livenessValue, faceImage.
  Future<Map<String, dynamic>> startVerification({
    String? faceId,
    String? faceFeature,
    double threshold = 0.85,
    int livenessType = 0,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3,4,5",
    bool allowRetry = true,
    String format = "filePath",
  }) {
    assert(
      (faceId != null && faceId.isNotEmpty) ||
          (faceFeature != null && faceFeature.isNotEmpty),
      'At least one of faceId or faceFeature must be provided',
    );
    return FaceAiSdkPlatform.instance.startVerification(
      faceId: faceId,
      faceFeature: faceFeature,
      threshold: threshold,
      livenessType: livenessType,
      motionStepSize: motionStepSize,
      motionTimeout: motionTimeout,
      motionTypes: motionTypes,
      allowRetry: allowRetry,
      format: format,
    );
  }

  /// Liveness detection only (no face matching).
  ///
  /// [livenessType] - 1=MOTION, 2=MOTION+COLOR, 3=COLOR, 4=SILENT.
  /// [motionStepSize] - Number of motion steps [1-2].
  /// [motionTimeout] - Motion timeout in seconds [3-22].
  /// [motionTypes] - Comma-separated motion types: 1=mouth, 2=smile, 3=blink, 4=shake, 5=nod.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, msg, livenessValue, faceImage.
  Future<Map<String, dynamic>> startLiveness({
    int livenessType = 1,
    int motionStepSize = 1,
    int motionTimeout = 10,
    String motionTypes = "1,2,3",
    String format = "filePath",
  }) {
    return FaceAiSdkPlatform.instance.startLiveness(
      livenessType: livenessType,
      motionStepSize: motionStepSize,
      motionTimeout: motionTimeout,
      motionTypes: motionTypes,
      format: format,
    );
  }

  /// Enroll a face for 1:1 verification.
  ///
  /// [faceId] - Unique identifier for the face being enrolled.
  /// [performanceMode] - Face detection mode: 2=ACCURATE (default), 1=FAST, 0=EASY, -1=NO_LIMIT.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, faceID, msg, faceFeature, faceImage.
  Future<Map<String, dynamic>> startEnroll({
    required String faceId,
    int performanceMode = 2,
    String format = "filePath",
  }) {
    return FaceAiSdkPlatform.instance.startEnroll(
      faceId: faceId,
      performanceMode: performanceMode,
      format: format,
    );
  }

  /// Add a face to the search database.
  ///
  /// [faceId] - Unique identifier for the face being added.
  /// [format] - Result image format: "base64" or "filePath".
  ///
  /// Returns Map with: code, faceID, msg, faceFeature, faceImage.
  Future<Map<String, dynamic>> addFace({
    required String faceId,
    String format = "base64",
  }) {
    return FaceAiSdkPlatform.instance.addFace(
      faceId: faceId,
      format: format,
    );
  }
}
