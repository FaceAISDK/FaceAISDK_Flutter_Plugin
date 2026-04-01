package com.faceAI.face_ai_sdk.base.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log

import com.ai.face.base.baseImage.FileStorage

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 优化后的图片处理工具类
 */
object BitmapUtils {

    private const val TAG = "BitmapUtils"

    /**
     * 保存压缩图（同步安全版）
     *
     * @param originBitmap   原图
     * @param pathName       路径目录
     * @param fileName       文件名
     * @return boolean       返回是否保存成功，方便上层做业务判断
     */
    @JvmStatic
    fun saveCompressBitmap2(originBitmap: Bitmap?, pathName: String, fileName: String): Boolean {
        // 1. Fail-Fast: 优先拦截无效 Bitmap，避免产生无用的空文件
        if (originBitmap == null || originBitmap.isRecycled) {
            Log.e("FaceAISDK", "Save Error: Bitmap is null or recycled.")
            return false
        }

        val file = FileStorage(pathName).createTempFile(fileName) ?: run {
            Log.e("FaceAISDK", "Save Error: Cannot create temp file.")
            return false
        }

        // 2. Try-With-Resources: 自动管理流的生命周期，绝对防止内存/句柄泄漏
        // 3. BufferedOutputStream: 增加 8KB 缓冲区，减少 I/O 阻塞时间
        return try {
            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos, 8192).use { bos ->
                    // 压缩并写入缓冲区
                    val success = originBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
                    // 确保缓冲区内最后剩余的数据被刷入磁盘
                    bos.flush()
                    success
                }
            }
        } catch (e: IOException) {
            Log.e("FaceAISDK", "Save Error: ${e.message}")
            false
        }
    }

    /**
     * 保存缩放后的图片，增加内存异常处理与副本回收优化
     */
    @JvmStatic
    fun saveCompressBitmap(srcBitmap: Bitmap?, pathName: String?, fileName: String?) {
        if (srcBitmap == null || srcBitmap.isRecycled || pathName.isNullOrEmpty() || fileName.isNullOrEmpty()) {
            Log.e(TAG, "Save Failed: srcBitmap is null, recycled, or invalid paths.")
            return
        }

        val srcWidth = srcBitmap.width
        val srcHeight = srcBitmap.height
        var targetWidth = srcWidth
        var targetHeight = srcHeight
        val MAX_SIDE = 640f // 目标最大边长

        // 计算等比缩放比例
        if (srcWidth > MAX_SIDE || srcHeight > MAX_SIDE) {
            val scale = Math.min(MAX_SIDE / srcWidth, MAX_SIDE / srcHeight)
            targetWidth = Math.round(srcWidth * scale)
            targetHeight = Math.round(srcHeight * scale)
        }

        val file = FileStorage(pathName).createTempFile(fileName)
        var finalBitmap: Bitmap? = null

        try {
            FileOutputStream(file).use { fos ->
                if (targetWidth != srcWidth || targetHeight != srcHeight) {
                    // 执行缩放，注意内存溢出风险
                    finalBitmap = Bitmap.createScaledBitmap(srcBitmap, targetWidth, targetHeight, true)
                } else {
                    finalBitmap = srcBitmap
                }

                // 写入文件，JPEG 90 质量平衡
                finalBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush() // 显式冲刷流
                Log.d(TAG, "Save Success: ${targetWidth}x$targetHeight to ${file.absolutePath}")
            }
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "Save Image Failed due to OOM")
        } catch (e: IOException) {
            Log.e(TAG, "Save Image Error: ${e.message}")
        } finally {
            // 内存管理：仅回收该方法内部创建的副本，不回收外部传入的 srcBitmap
            if (finalBitmap != null && finalBitmap !== srcBitmap) {
                finalBitmap!!.recycle()
            }
        }
    }

    /**
     * 文件转 Base64，引入预估容量与缓冲流优化
     */
    @JvmStatic
    fun bitmapToBase64(filepath: String?): String {
        if (filepath.isNullOrEmpty()) return ""

        val file = File(filepath)
        if (!file.exists() || !file.canRead()) {
            Log.e(TAG, "File not found or unreadable: $filepath")
            return ""
        }

        // Base64 编码后的长度约为原长度的 4/3，预设缓冲区大小减少扩容
        val initialCapacity = (file.length() * 1.34).toInt() + 1024

        try {
            BufferedInputStream(FileInputStream(file)).use { bis ->
                ByteArrayOutputStream(initialCapacity).use { baos ->
                    val buffer = ByteArray(16384) // 增大到 16K 缓冲区提升读速度
                    var len: Int
                    while (bis.read(buffer).also { len = it } != -1) {
                        baos.write(buffer, 0, len)
                    }

                    val fileBytes = baos.toByteArray()
                    return "data:image/jpg;base64," + Base64.encodeToString(fileBytes, Base64.NO_WRAP)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Read file to Base64 Error: ${e.message}")
        }

        return ""
    }

    /**
     * bitmapToBase64
     */
    @JvmStatic
    fun bitmapToBase64(bitmap: Bitmap?): String {
        if (bitmap == null || bitmap.isRecycled) return ""

        // 1. 修复一致性：JPEG 压缩应对应 image/jpeg 的前缀
        val prefix = "data:image/jpeg;base64,"
        var baos: ByteArrayOutputStream? = null

        try {
            // 2. 内存预分配合理化
            val initialCapacity = bitmap.allocationByteCount / 4
            baos = ByteArrayOutputStream(if (initialCapacity > 0) initialCapacity else 1024)

            // 3. 压缩策略
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val bitmapBytes = baos.toByteArray()

            // 4. Base64 编码优化
            val base64Content = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP)

            val sb = StringBuilder(prefix.length + base64Content.length)
            sb.append(prefix)
            sb.append(base64Content)

            return sb.toString()

        } catch (e: Exception) {
            Log.e("BitmapUtil", "BitmapToBase64 Error: ${e.message}")
            return ""
        } finally {
            if (baos != null) {
                try {
                    baos.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    /**
     * Base64 转为 Bitmap，增加 decode 质量控制
     */
    @JvmStatic
    fun base64ToBitmap(base64Data: String?): Bitmap? {
        if (base64Data.isNullOrEmpty()) return null

        try {
            var clearBase64Data = base64Data
            val commaIndex = base64Data.indexOf(",")
            if (commaIndex != -1) {
                clearBase64Data = base64Data.substring(commaIndex + 1)
            }

            val bytes = Base64.decode(clearBase64Data, Base64.NO_WRAP)

            // 优化：配置 Options 减少色彩开销（如果不需要 alpha 通道，可用 RGB_565）
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "base64ToBitmap: Invalid Base64 format")
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "base64ToBitmap: OOM while decoding")
        } catch (e: Exception) {
            Log.e(TAG, "base64ToBitmap Error: ${e.message}")
        }

        return null
    }
}
