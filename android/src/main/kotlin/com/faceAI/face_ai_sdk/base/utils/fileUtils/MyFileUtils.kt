package com.faceAI.face_ai_sdk.base.utils.fileUtils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

object MyFileUtils {

    class FileMetaData {
        var displayName: String? = null
        var size: Long = 0
        var mimeType: String? = null
        var path: String? = null

        override fun toString(): String {
            return "name : $displayName ; size : $size ; path : $path ; mime : $mimeType"
        }
    }

    @JvmStatic
    fun getFileMetaData(context: Context, uri: Uri): FileMetaData? {
        val fileMetaData = FileMetaData()

        if ("file".equals(uri.scheme, ignoreCase = true)) {
            val file = File(uri.path!!)
            fileMetaData.displayName = file.name
            fileMetaData.size = file.length()
            fileMetaData.path = file.path

            return fileMetaData
        } else {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(uri, null, null, null, null)
            fileMetaData.mimeType = contentResolver.getType(uri)

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    fileMetaData.displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                    if (!cursor.isNull(sizeIndex))
                        fileMetaData.size = cursor.getLong(sizeIndex)
                    else
                        fileMetaData.size = -1

                    try {
                        fileMetaData.path = cursor.getString(cursor.getColumnIndexOrThrow("_data"))
                    } catch (e: Exception) {
                        // DO NOTHING, _data does not exist
                    }

                    return fileMetaData
                }
            } catch (e: Exception) {
                Log.e("FileUtils", e.toString())
            } finally {
                cursor?.close()
            }

            return null
        }
    }
}
