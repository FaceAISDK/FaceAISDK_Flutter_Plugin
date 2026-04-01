package com.faceAI.face_ai_sdk.SysCamera.search

import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.faceAI.face_ai_sdk.R
import com.faceAI.face_ai_sdk.base.utils.BitmapUtils

class ImageToast {

    /**
     * 接收 Base64 图 - (改名为 showBase64)
     */
    fun showBase64(context: Context, base64: String, tips: String): Toast {
        val bitmap = BitmapUtils.base64ToBitmap(base64)
        return showBitmap(context, bitmap, tips)
    }

    /**
     * 不需要图
     */
    fun show(context: Context, tips: String): Toast {
        return showBitmap(context, null, tips)
    }

    /**
     * 接收 Bitmap 图文并茂 - (改名为 showBitmap)
     */
    fun showBitmap(context: Context, bitmap: Bitmap?, tips: String): Toast {
        val toast = Toast(context)
        val view = View.inflate(context, R.layout.face_toast_tips, null)
        val image: ImageView = view.findViewById(R.id.toast_image)
        val text: TextView = view.findViewById(R.id.toast_text)

        if (bitmap == null) {
            image.visibility = View.GONE
        } else {
            image.visibility = View.VISIBLE
            Glide.with(context)
                .load(bitmap)
                .transform(RoundedCorners(44))
                .into(image)
        }

        text.text = tips
        @Suppress("DEPRECATION")
        toast.view = view
        toast.duration = Toast.LENGTH_SHORT
        toast.setGravity(Gravity.BOTTOM, 0, 166)
        toast.show()
        return toast
    }
}
