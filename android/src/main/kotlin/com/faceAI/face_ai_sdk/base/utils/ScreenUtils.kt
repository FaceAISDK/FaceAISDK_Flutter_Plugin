package com.faceAI.face_ai_sdk.base.utils

import android.app.Activity
import android.content.Context
import android.view.Window
import android.view.WindowManager

object ScreenUtils {

    /**
     * 根据手机的分辨率从dp 到 px(像素)
     */
    @JvmStatic
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     */
    @JvmStatic
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * 隐藏状态栏
     *
     * 也就是设置全屏，一定要在setContentView之前调用，否则报错
     * 此方法Activity可以继承AppCompatActivity
     * 启动的时候状态栏会显示一下再隐藏，比如QQ的欢迎界面
     * 在配置文件中Activity加属性android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
     * 如加了以上配置Activity不能继承AppCompatActivity，会报错
     *
     * @param activity activity
     */
    @JvmStatic
    fun hideStatusBar(activity: Activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
}
