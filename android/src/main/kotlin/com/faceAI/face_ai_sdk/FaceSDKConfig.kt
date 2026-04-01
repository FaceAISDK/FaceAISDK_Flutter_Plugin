package com.faceAI.face_ai_sdk

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import com.ai.face.faceSearch.search.Image2FaceFeature
import com.bumptech.glide.Glide
import com.faceAI.face_ai_sdk.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG
import com.faceAI.face_ai_sdk.base.utils.VoicePlayer
import com.tencent.mmkv.MMKV

/**
 * SDK 配置初始化
 */
object FaceSDKConfig {

    @JvmStatic
    var CACHE_FACE_LOG_DIR: String = ""    //本地保存某次人脸校验完成后的场景图目录，给三方插件使用

    //根据工信部人脸信息处理合规要求，不建议大规模缓存人脸图片，目前iOS&Android SDK都已经改为加密过后的人脸特征值
    @JvmStatic
    var CACHE_BASE_FACE_DIR: String = ""   //1：1 人脸识别人脸图片存储目录

    @JvmStatic
    var CACHE_SEARCH_FACE_DIR: String = "" //1：N 人脸识别搜索人脸图片存储目

    /**
     * 初始化人脸本地图片存储目录，请在Application onCreate中调用
     */
    @JvmStatic
    fun init(context: Context) {
        //使用MMKV保存简单1:1人脸特征保存key为faceID,value为特征值 （人脸搜索的人脸特征放在SDK内置数据库中管理）
        MMKV.initialize(context)

        //语音提示播报，现在都是播放录音文件。后期改为TTS吧
        VoicePlayer.getInstance().init(context)

        // 人脸图存储在App内部私有空间，SDK未做分区存储
        // Warming: 目前仅能存储在context.getCacheDir() 或者context.getFilesDir() 内部私有空间
        // https://developer.android.com/training/data-storage?hl=zh-cn
        CACHE_BASE_FACE_DIR = context.filesDir.path + "/FaceAI/Verify/"    //1:1 人脸识别目录
        CACHE_SEARCH_FACE_DIR = context.filesDir.path + "/FaceAI/Search/"  //人脸搜索人脸库目录
        CACHE_FACE_LOG_DIR = context.filesDir.path + "/FaceAI/Log/"  //使用场景图目录
    }

    /**
     * 清除所有的{人脸搜索识别}人脸特征值和本地缓存的图片
     */
    @JvmStatic
    fun clearAllFaceSearchData(context: Context) {
        //清除所有人脸搜索所有特征
        FaceSearchFeatureManger.getInstance(context).clearAllFaceFaceFeature()

        //删除所有缓存的人脸图
        Image2FaceFeature.getInstance(context).clearFaceImages(CACHE_SEARCH_FACE_DIR)
        Glide.get(context).clearMemory()
    }

    /**
     * 清除某个{人脸搜索识别}人脸特征值和本地缓存的图片
     */
    @JvmStatic
    fun deleteFaceSearchData(context: Context, faceID: String) {
        //清除所有人脸搜索所有特征
        FaceSearchFeatureManger.getInstance(context).deleteFaceFaceFeature(faceID)
        //删除FaceID对应缓存的裁剪好的人脸图
        Image2FaceFeature.getInstance(context).deleteFaceImage(CACHE_SEARCH_FACE_DIR + faceID)
    }

    /**
     * 删除1:1 人脸识别faceID 本地对应的图片和特征向量编码
     */
    @JvmStatic
    fun deleteFaceVerifyData(context: Context, faceID: String) {
        //1:1 的人脸特征清除
        MMKV.defaultMMKV().removeValueForKey(faceID)
        //如果缓存了图片也删除
        Image2FaceFeature.getInstance(context).deleteFaceImage(CACHE_BASE_FACE_DIR + faceID)
    }

    /**
     * 暂时还用SP，后期统一为MMKV
     */
    @JvmStatic
    fun setCameraID(context: Context, cameraID: Int) {
        val sharedPref = context.getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE)
        sharedPref.edit().putInt(FRONT_BACK_CAMERA_FLAG, cameraID).apply()
    }

    /**
     * 检测App 是否调试模式
     */
    @JvmStatic
    fun isDebugMode(mContext: Context): Boolean {
        //Debug 模式是打开状态
        return 0 != (mContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
    }
}
