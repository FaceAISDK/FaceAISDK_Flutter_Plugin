package com.faceAI.face_ai_sdk.SysCamera.camera

import android.view.Surface

/**
 * 参数构造，后期需要添加更多的配置
 *
 *
 * 2022.07.30 SZ
 */
class FaceCameraXBuilder private constructor(builder: Builder) {
    val linearZoom: Float = builder.linearZoom
    val cameraLensFacing: Int = builder.cameraLensFacing //默认是前置摄像头
    val rotation: Int = builder.rotation //默认是前置摄像头
    val cameraSizeHigh: Boolean = builder.cameraSizeHigh //是否高分辨率

    class Builder {
        var linearZoom: Float = 0f    //默认的大小
        var cameraLensFacing: Int = 0 //默认是前置摄像头
        var cameraSizeHigh: Boolean = false //摄像头分辨率
        var rotation: Int = Surface.ROTATION_0 //默认是前置摄像头

        fun setCameraLensFacing(cameraLensFacing: Int): Builder {
            this.cameraLensFacing = cameraLensFacing
            return this
        }

        fun setRotation(rotation: Int): Builder {
            this.rotation = rotation
            return this
        }

        fun setCameraSizeHigh(cameraSizeHigh: Boolean): Builder {
            this.cameraSizeHigh = cameraSizeHigh
            return this
        }

        fun setLinearZoom(linearZoom: Float): Builder {
            this.linearZoom = linearZoom
            return this
        }

        fun create(): FaceCameraXBuilder { // 构建，返回一个新对象
            return FaceCameraXBuilder(this)
        }
    }
}
