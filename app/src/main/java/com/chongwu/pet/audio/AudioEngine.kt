package com.chongwu.pet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.*

/**
 * 程序化音效引擎 - 无需外部音频文件
 * 用数学公式合成小羊叫声、环境音等
 */
class AudioEngine(private val context: Context) {
    
    private val sampleRate = 22050
    private var isPlaying = false
    
    // 音效缓存
    private var bleatBuffer: ShortArray? = null
    private var ambientBuffer: ShortArray? = null
    private var happyBuffer: ShortArray? = null
    private var startleBuffer: ShortArray? = null
    
    fun init() {
        // 预生成声音缓冲
        bleatBuffer = generateBleat(0.6f)
        happyBuffer = generateHappy(0.4f)
        startleBuffer = generateStartle(0.3f)
        ambientBuffer = generateAmbient(3.0f)
    }
    
    /** 咩咩叫 */
    fun playBleat() {
        bleatBuffer?.let { playSound(it, 0.8f) }
    }
    
    /** 开心叫声 */
    fun playHappy() {
        happyBuffer?.let { playSound(it, 0.6f) }
    }
    
    /** 惊吓叫声 */
    fun playStartle() {
        startleBuffer?.let { playSound(it, 0.9f) }
    }
    
    /** 环境氛围音 */
    fun playAmbient() {
        ambientBuffer?.let { playSoundLooped(it, 0.15f) }
    }
    
    /** 吃草音 */
    fun playEat() {
        playNoise(0.3f, 0.15f)
    }
    
    /** 蹦跳音 */
    fun playHop() {
        playTone(600f, 0.05f, 0.3f)
    }
    
    /** 生成咩咩叫声 */
    private fun generateBleat(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            // 咩~ 声：颤音 + 滑音
            val freq = 400f + 200f * (t / durationSec) + 30f * sin(2 * PI * 8f * t).toFloat()
            val envelope = exp(-3f * t / durationSec).toFloat()
            val sample = sin(2 * PI * freq * t).toFloat() * envelope * 0.7f +
                         sin(2 * PI * freq * 2f * t).toFloat() * envelope * 0.3f
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
    
    /** 开心叫声 */
    private fun generateHappy(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 600f + 100f * sin(2 * PI * 12f * t).toFloat()
            val envelope = sin(PI * t / durationSec).toFloat()
            val sample = (sin(2 * PI * freq * t).toFloat() * 0.6f +
                         sin(2 * PI * freq * 1.5f * t).toFloat() * 0.4f) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
        return buffer
    }
    
    /** 惊吓叫声 */
    private fun generateStartle(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 800f + 400f * (1f - t / durationSec)
            val envelope = exp(-5f * t / durationSec).toFloat()
            val sample = sin(2 * PI * freq * t).toFloat() * envelope +
                        Math.random().toFloat() * envelope * 0.3f
            buffer[i] = (sample.toFloat() * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        return buffer
    }
    
    /** 环境氛围 */
    private fun generateAmbient(durationSec: Float): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        
        // 风声+虫鸣
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            // 风声（噪声滤波）
            val noise = Math.random().toFloat() * 2f - 1f
            val wind = noise * 0.3f + 
                      sin(2 * PI * 200f * t + sin(2 * PI * 0.3f * t)).toFloat() * 0.05f
            // 虫鸣
            val cricket = sin(2 * PI * 4000f * t).toFloat() * 
                         (0.5f + 0.5f * sin(2 * PI * 4f * t).toFloat()) * 0.1f
            val sample = (wind + cricket) * 0.3f
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
    
    /** 播放纯音 */
    private fun playTone(freq: Float, durationSec: Float, volume: Float) {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-4f * t / durationSec).toFloat()
            val sample = sin(2 * PI * freq * t).toFloat() * envelope
            buffer[i] = (sample * Short.MAX_VALUE * volume).toInt().toShort()
        }
        playSound(buffer, volume)
    }
    
    /** 播放噪声（吃草等） */
    private fun playNoise(volume: Float, durationSec: Float) {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val envelope = exp(-3f * t / durationSec).toFloat()
            val noise = (Math.random().toFloat() * 2f - 1f)
            val sample = noise * envelope * 0.5f +
                        sin(2 * PI * 2000f * t).toFloat() * envelope * 0.3f
            buffer[i] = (sample * Short.MAX_VALUE * volume).toInt().toShort()
        }
        playSound(buffer, volume)
    }
    
    private fun playSound(buffer: ShortArray, volume: Float) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            track.write(buffer, 0, buffer.size)
            track.setVolume(volume)
            track.play()
            
            // 播放完毕后释放
            Thread {
                try {
                    Thread.sleep((buffer.size.toFloat() / sampleRate * 1000 + 100).toLong())
                    track.release()
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {
            // 音频不可用时静默处理
        }
    }
    
    private fun playSoundLooped(buffer: ShortArray, volume: Float): AudioTrack? {
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            track.write(buffer, 0, buffer.size)
            track.setVolume(volume)
            track.setLoopPoints(0, buffer.size / 2, -1)
            track.play()
            track
        } catch (_: Exception) { null }
    }
    
    fun release() {
        bleatBuffer = null
        happyBuffer = null
        startleBuffer = null
        ambientBuffer = null
    }
}
