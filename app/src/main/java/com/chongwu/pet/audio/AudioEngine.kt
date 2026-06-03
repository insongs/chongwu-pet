package com.chongwu.pet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

class AudioEngine(private val context: Context) {

    private val sampleRate = 22050

    private var bleatBuffer: ShortArray? = null
    private var happyBuffer: ShortArray? = null
    private var startleBuffer: ShortArray? = null
    private var ambientBuffer: ShortArray? = null
    private var bubbleBuffer: ShortArray? = null
    private var waterBuffer: ShortArray? = null

    fun init() {
        bleatBuffer = genBleat(0.6f)
        happyBuffer = genHappy(0.4f)
        startleBuffer = genStartle(0.3f)
        ambientBuffer = genAmbient(3.0f)
        bubbleBuffer = genBubble(0.15f)
        waterBuffer = genWaterAmbient(4.0f)
    }

    fun playBleat() { bleatBuffer?.let { play(it, 0.8f) } }
    fun playHappy() { happyBuffer?.let { play(it, 0.6f) } }
    fun playStartle() { startleBuffer?.let { play(it, 0.9f) } }
    fun playEat() { playNoise(0.3f, 0.15f) }
    fun playHop() { playTone(600f, 0.05f, 0.3f) }
    fun playBubble() { bubbleBuffer?.let { play(it, 0.4f) } }
    fun playWaterAmbient() { waterBuffer?.let { playLooped(it, 0.1f) } }

    private fun genBleat(dur: Float): ShortArray {
        val n = (sampleRate * dur).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val freq = 400f + 200f * (t / dur) + 30f * sin(2 * PI * 8f * t)
            val env = exp(-3f * t / dur)
            val s = sin(2 * PI * freq * t) * env * 0.7f + sin(2 * PI * freq * 2f * t) * env * 0.3f
            (s * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun genHappy(dur: Float): ShortArray {
        val n = (sampleRate * dur).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val freq = 600f + 100f * sin(2 * PI * 12f * t)
            val env = sin(PI * t / dur)
            val s = (sin(2 * PI * freq * t) * 0.6f + sin(2 * PI * freq * 1.5f * t) * 0.4f) * env
            (s * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
    }

    private fun genStartle(dur: Float): ShortArray {
        val n = (sampleRate * dur).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val freq = 800f + 400f * (1f - t / dur)
            val env = exp(-5f * t / dur)
            val s = sin(2 * PI * freq * t) * env + Math.random().toFloat() * env * 0.3f
            (s * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
    }

    private fun genAmbient(dur: Float): ShortArray {
        val n = (sampleRate * dur).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val noise = Math.random().toFloat() * 2f - 1f
            val wind = noise * 0.3f + sin(2 * PI * 200f * t + sin(2 * PI * 0.3f * t)) * 0.05f
            val cricket = sin(2 * PI * 4000f * t) * (0.5f + 0.5f * sin(2 * PI * 4f * t)) * 0.1f
            ((wind + cricket) * 0.3f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** 吐泡泡音效 */
    private fun genBubble(dur: Float): ShortArray {
        val n = (sampleRate * dur).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val freq = 800f + 1200f * (1f - t / dur)
            val env = exp(-8f * t / dur)
            val s = sin(2 * PI * freq * t) * env * 0.4f + sin(2 * PI * freq * 1.3f * t) * env * 0.3f
            (s * Short.MAX_VALUE * 0.5f).toInt().toShort()
        }
    }

    /** 水下环境音 */
    private fun genWaterAmbient(dur: Float): ShortArray {
        val n = (sampleRate * dur).toInt()
        return ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val noise = Math.random().toFloat() * 2f - 1f
            val filtered = noise * 0.2f + sin(2 * PI * 100f * t + noise * 0.5f) * 0.1f
            val hum = sin(2 * PI * 60f * t) * 0.05f
            ((filtered + hum) * 0.4f * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun playTone(freq: Float, dur: Float, vol: Float) {
        val n = (sampleRate * dur).toInt()
        val buf = ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val s = sin(2 * PI * freq * t) * exp(-4f * t / dur)
            (s * Short.MAX_VALUE * vol).toInt().toShort()
        }
        play(buf, vol)
    }

    private fun playNoise(vol: Float, dur: Float) {
        val n = (sampleRate * dur).toInt()
        val buf = ShortArray(n) { i ->
            val t = i.toFloat() / sampleRate
            val env = exp(-3f * t / dur)
            val s = (Math.random().toFloat() * 2f - 1f) * env * 0.5f + sin(2 * PI * 2000f * t) * env * 0.3f
            (s * Short.MAX_VALUE * vol).toInt().toShort()
        }
        play(buf, vol)
    }

    private fun play(buf: ShortArray, vol: Float) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC).build()
            track.write(buf, 0, buf.size)
            track.setVolume(vol)
            track.play()
            Thread {
                try { Thread.sleep((buf.size.toFloat() / sampleRate * 1000 + 100).toLong()); track.release() }
                catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }

    private fun playLooped(buf: ShortArray, vol: Float): AudioTrack? {
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC).build()
            track.write(buf, 0, buf.size)
            track.setVolume(vol)
            track.setLoopPoints(0, buf.size / 2, -1)
            track.play()
            track
        } catch (_: Exception) { null }
    }

    fun release() {
        bleatBuffer = null; happyBuffer = null; startleBuffer = null
        ambientBuffer = null; bubbleBuffer = null; waterBuffer = null
    }
}
