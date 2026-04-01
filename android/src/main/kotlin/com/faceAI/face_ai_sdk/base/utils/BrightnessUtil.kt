package com.faceAI.face_ai_sdk.base.utils

import android.app.Activity

/**
 * 屏幕亮度检测工具类
 */
object BrightnessUtil {

    /**
     * 设置屏幕亮度
     * 0-1f
     */
    @JvmStatic
    fun setBrightness(activity: Activity, param: Float) {
        val localWindow = activity.window
        val attributes = localWindow.attributes
        attributes.screenBrightness = param
        localWindow.attributes = attributes
    }

    /**
     * 获取屏幕亮度
     */
    @JvmStatic
    fun getBrightness(activity: Activity): Float {
        val localWindow = activity.window
        val attributes = localWindow.attributes
        return attributes.screenBrightness
    }
}
