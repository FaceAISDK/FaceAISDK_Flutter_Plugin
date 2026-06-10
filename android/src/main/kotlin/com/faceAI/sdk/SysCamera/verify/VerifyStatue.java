package com.faceAI.sdk.SysCamera.verify;

/**
 * Result codes for face verification and liveness detection.
 */
public class VerifyStatue {
    public static final int DEFAULT = -1;
    public static final int VERIFY_SUCCESS = 1;
    public static final int VERIFY_FAILED = 2;
    public static final int NO_BASE_FACE_FEATURE = 3;
    public static final int MOTION_LIVENESS_TIMEOUT = 4;
    public static final int SILENT_LIVENESS_FAILED = 5;
    public static final int NOT_ALLOW_MULTI_FACES = 6;
    public static final int NO_FACE_MULTI = 7;
    public static final int COLOR_LIVENESS_FAILED = 8;
    public static final int COLOR_LIVENESS_LIGHT_TOO_HIGH = 9;
    public static final int ALL_LIVENESS_SUCCESS = 10;
}
