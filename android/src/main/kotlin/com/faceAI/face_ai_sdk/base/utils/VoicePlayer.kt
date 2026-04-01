package com.faceAI.face_ai_sdk.base.utils

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 播报提示语音,播放自己录制的语音文件
 * 仅供参考，建议用户自己玩耍
 */
class VoicePlayer private constructor() {
    private var mMediaPlayer: MediaPlayer? = null
    private var mContext: Context? = null
    private val mAudioList: MutableList<Int> = CopyOnWriteArrayList()

    companion object {
        @JvmStatic
        fun getInstance(): VoicePlayer = Factory.INSTANCE
    }

    private object Factory {
        val INSTANCE = VoicePlayer()
    }

    /**
     * 新加一个开关是否可以打开声音播放
     */
    fun init(context: Context) {
        mContext = context.applicationContext
    }

    fun play(id: Int, onCompletionListener: MediaPlayer.OnCompletionListener?) {
        if (mContext == null) {
            return
        }

        stop()
        mMediaPlayer = MediaPlayer.create(mContext, id)
        if (mMediaPlayer != null) {
            if (onCompletionListener != null) {
                mMediaPlayer!!.setOnCompletionListener(onCompletionListener)
            }
            mMediaPlayer!!.start()
        }
    }

    @Synchronized
    fun addPayList(@RawRes rawId: Int) {
        if (mContext == null) {
            return
        }
        if (mAudioList.isEmpty()) {
            stop()
            mAudioList.add(rawId)
            mMediaPlayer = MediaPlayer.create(mContext, mAudioList[0])
            if (mAudioList.isNotEmpty()) {
                val completionListener = object : MediaPlayer.OnCompletionListener {
                    override fun onCompletion(mediaPlayer: MediaPlayer) {
                        if (mAudioList.isEmpty()) {
                            return
                        }
                        mAudioList.removeAt(0)
                        if (mMediaPlayer!!.isPlaying) {
                            mMediaPlayer!!.stop()
                        }
                        mMediaPlayer!!.setOnCompletionListener(null)
                        mMediaPlayer!!.release()
                        if (mAudioList.isNotEmpty()) {
                            mMediaPlayer = MediaPlayer.create(mContext, mAudioList[0])
                            mMediaPlayer!!.setOnCompletionListener(this)
                            mMediaPlayer!!.start()
                        }
                    }
                }
                mMediaPlayer!!.setOnCompletionListener(completionListener)
            }
            mMediaPlayer!!.start()
        } else {
            mAudioList.add(rawId)
        }
    }

    fun stop() {
        try {
            if (mMediaPlayer != null) {
                if (mMediaPlayer!!.isPlaying) {
                    mMediaPlayer!!.stop()
                }
                mAudioList.clear()
                mMediaPlayer!!.release()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun play(id: Int) {
        play(id, null)
    }
}
