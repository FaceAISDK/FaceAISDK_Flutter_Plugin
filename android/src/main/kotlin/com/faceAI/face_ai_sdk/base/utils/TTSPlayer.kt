package com.faceAI.face_ai_sdk.base.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.annotation.StringRes
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * TTS 語音播報工具類，替代 VoicePlayer。
 * 優化點：
 * 1. 使用 Kotlin object 實現單例
 * 2. 配置 AudioAttributes 提升音頻通路質量
 * 3. 自動選擇當前語言下質量最高、延遲最低的 Voice
 * 4. 優先使用網絡高級語音模型（更具擬真感）
 * 5. 優化了 Pitch 和 SpeechRate 使得發音更加沉穩自然
 */
object TTSPlayer {

private const val TAG = "TTSPlayer"

/** 偏好的 TTS 引擎包名（Google TTS 質量最好） */
private const val PREFERRED_ENGINE = "com.google.android.tts"

@Volatile
private var mTTS: TextToSpeech? = null

@Volatile
private var mContext: Context? = null

private val mReady = AtomicBoolean(false)
private val mUtteranceId = AtomicInteger(0)

private val mPendingQueue = ConcurrentLinkedQueue<String>()

// 放慢語速，降低音調。去掉電子尖銳感，增加沉穩感
private var mSpeechRate = 0.92f
private var mPitch = 0.98f

/**
 * 初始化 TTS 引擎
 */
fun init(context: Context) {
    if (mTTS != null) return
            mContext = context.applicationContext

    val listener = TextToSpeech.OnInitListener { status -> onTTSInit(status) }

    mTTS = if (isEngineInstalled(PREFERRED_ENGINE)) {
        TextToSpeech(mContext, listener, PREFERRED_ENGINE)
    } else {
        TextToSpeech(mContext, listener)
    }

    mTTS?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}
        override fun onDone(utteranceId: String) {}
        override fun onError(utteranceId: String) {
            Log.e(TAG, "TTS utterance error: $utteranceId")
        }
    })
}

private fun onTTSInit(status: Int) {
    if (status != TextToSpeech.SUCCESS) {
        Log.e(TAG, "TTS engine init failed, status=$status")
        return
    }

    mTTS?.apply {
        val attrBuilder = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attrBuilder.setUsage(AudioAttributes.USAGE_ASSISTANT)
        } else {
            attrBuilder.setUsage(AudioAttributes.USAGE_MEDIA)
        }

        setAudioAttributes(attrBuilder.build())
        setSpeechRate(mSpeechRate)
        setPitch(mPitch)

        applyLocaleAndVoice()
    }

    mReady.set(true)
    drainPendingQueue()
    Log.i(TAG, "TTS engine initialized: ${mTTS?.defaultEngine}")
}

fun playTTS(@StringRes stringResId: Int) {
    val text = mContext?.getString(stringResId) ?: return
            playTTS(text)
}

fun playTTS(text: String?) {
    if (text.isNullOrEmpty()) return
    if (!mReady.get()) {
        mPendingQueue.offer(text)
        return
    }
    speak(text)
}

fun stop() {
    mPendingQueue.clear()
    mTTS?.stop()
}

fun release() {
    mReady.set(false)
    mPendingQueue.clear()
    mTTS?.apply {
        stop()
        shutdown()
    }
    mTTS = null
    mContext = null
}

fun setSpeechRate(rate: Float) {
    mSpeechRate = rate.coerceIn(0.5f, 2.0f)
    mTTS?.setSpeechRate(mSpeechRate)
}

fun setPitch(pitch: Float) {
    mPitch = pitch.coerceIn(0.5f, 2.0f)
    mTTS?.setPitch(mPitch)
}

// ==================== 內部邏輯 ====================

private fun speak(text: String) {
    val tts = mTTS ?: return

            // 每次發聲前重新應用最佳 Voice，防止引擎狀態被系統重置
            applyLocaleAndVoice()

    val id = "tts_${mUtteranceId.incrementAndGet()}"
    val params = Bundle().apply {
        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        // 強制允許網絡合成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true")
        }
    }

    tts.speak(text, TextToSpeech.QUEUE_ADD, params, id)
}

private fun applyLocaleAndVoice() {
    val tts = mTTS ?: return
            val currentLocale = Locale.getDefault()

    // 使用平滑降級策略協商最優 Locale
    val bestLocale = negotiateLocale(currentLocale)
    selectBestVoice(bestLocale)
}

private fun negotiateLocale(targetLocale: Locale): Locale {
    val tts = mTTS ?: return targetLocale

    // 嘗試直接設置
    if (isLanguageAvailable(tts.setLanguage(targetLocale))) return targetLocale

    // 中文平滑降級
    if (targetLocale.language == "zh") {
        if (isLanguageAvailable(tts.setLanguage(Locale.SIMPLIFIED_CHINESE))) return Locale.SIMPLIFIED_CHINESE
        if (isLanguageAvailable(tts.setLanguage(Locale.TRADITIONAL_CHINESE))) return Locale.TRADITIONAL_CHINESE
    }

    // 僅語言匹配
    val langOnlyLocale = Locale(targetLocale.language)
    if (isLanguageAvailable(tts.setLanguage(langOnlyLocale))) return langOnlyLocale

    // 默認回退到英文
    tts.setLanguage(Locale.US)
    return Locale.US
}

private fun isLanguageAvailable(ttsResultCode: Int): Boolean {
    return ttsResultCode >= TextToSpeech.LANG_AVAILABLE
}

/**
 * 選擇當前語言下最優的語音模型
 */
private fun selectBestVoice(targetLocale: Locale) {
    try {
        val voices = mTTS?.voices ?: return
                val targetLang = targetLocale.language

        val candidates = voices.filter { it.locale.language == targetLang }
        if (candidates.isEmpty()) return

                // 排序規則：1. 質量分數高 -> 2. 網絡語音優先 -> 3. 低延遲優先
                val bestVoice = candidates.sortedWith(
                compareByDescending<Voice> { it.quality }
                        .thenByDescending { it.isNetworkConnectionRequired }
                    .thenBy { it.latency }
            ).firstOrNull()

        bestVoice?.let {
            mTTS?.voice = it
            Log.i(TAG, "Selected voice: ${it.name} quality=${it.quality} network=${it.isNetworkConnectionRequired}")
        }
    } catch (e: Exception) {
        Log.w(TAG, "Voice selection failed, using default", e)
    }
}

private fun isEngineInstalled(enginePackage: String): Boolean {
    return try {
        mContext?.packageManager?.getPackageInfo(enginePackage, 0)
        true
    } catch (e: Exception) {
        false
    }
}

private fun drainPendingQueue() {
    while (mPendingQueue.isNotEmpty()) {
        mPendingQueue.poll()?.let { speak(it) }
    }
}
}