package com.faceAI.face_ai_sdk.base.utils.performance.opengl

class OffscreenSurface(eglCore: EglCore, width: Int, height: Int) : EglSurfaceBase(eglCore) {

    init {
        createOffscreenSurface(width, height)
    }

    /**
     * Releases any resources associated with the surface.
     */
    fun release() {
        releaseEglSurface()
    }
}
