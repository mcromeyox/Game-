package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural Real-time Audio & Haptic Engine for PUBG-Style Combat and Vehicle Simulator.
 * Synthesizes dynamic engine RPM, drift squeals, nitro whoosh, gunshots, explosions, and UI sounds.
 */
class GameSoundEngine(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050
    private var isMuted = false

    // Cached sound buffers
    private val soundBuffers = ConcurrentHashMap<String, ShortArray>()

    init {
        pregenerateSounds()
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    private fun pregenerateSounds() {
        scope.launch {
            // M416 gunshot
            soundBuffers["gun_m416"] = generateGunshot(durationMs = 90, noiseFactor = 0.7f, baseFreq = 180f)
            // AKM gunshot (heavier)
            soundBuffers["gun_akm"] = generateGunshot(durationMs = 120, noiseFactor = 0.8f, baseFreq = 120f)
            // AWM gunshot (high power sniper)
            soundBuffers["gun_awm"] = generateGunshot(durationMs = 240, noiseFactor = 0.9f, baseFreq = 80f)
            // Pan clank (metallic)
            soundBuffers["pan_clang"] = generatePanClang()
            // Explosion
            soundBuffers["explosion"] = generateExplosion()
            // Horn
            soundBuffers["horn"] = generateHorn()
            // Drift squeal
            soundBuffers["drift"] = generateDriftSqueal()
            // Nitro boost
            soundBuffers["nitro"] = generateNitroWhoosh()
            // Reload click
            soundBuffers["reload"] = generateReloadClick()
            // Heal sound
            soundBuffers["heal"] = generateHealChime()
            // Victory fanfare
            soundBuffers["victory"] = generateVictoryFanfare()
        }
    }

    fun playGunshot(weaponType: String) {
        if (isMuted) return
        val key = when (weaponType.uppercase()) {
            "AKM", "GROZA" -> "gun_akm"
            "AWM", "KAR98K" -> "gun_awm"
            "PAN" -> "pan_clang"
            else -> "gun_m416"
        }
        playSound(key)
        vibrate(durationMs = if (key == "gun_awm") 80 else 40, amplitude = 180)
    }

    fun playExplosion() {
        if (isMuted) return
        playSound("explosion")
        vibrate(durationMs = 250, amplitude = 255)
    }

    fun playHorn() {
        if (isMuted) return
        playSound("horn")
    }

    fun playNitro() {
        if (isMuted) return
        playSound("nitro")
        vibrate(durationMs = 100, amplitude = 120)
    }

    fun playDrift() {
        if (isMuted) return
        playSound("drift")
    }

    fun playReload() {
        if (isMuted) return
        playSound("reload")
    }

    fun playHeal() {
        if (isMuted) return
        playSound("heal")
    }

    fun playVictory() {
        if (isMuted) return
        playSound("victory")
        vibratePattern(longArrayOf(0, 100, 100, 150, 100, 300))
    }

    fun playCrash() {
        if (isMuted) return
        playSound("explosion")
        vibrate(durationMs = 150, amplitude = 220)
    }

    private fun playSound(key: String) {
        val buffer = soundBuffers[key] ?: return
        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                // Let track play then release
                Thread.sleep((buffer.size * 1000L / sampleRate) + 50)
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(timings: LongArray) {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(timings, -1)
                }
            }
        } catch (_: Exception) {}
    }

    // Audio generators
    private fun generateGunshot(durationMs: Int, noiseFactor: Float, baseFreq: Float): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val envelope = (1.0 - progress) * (1.0 - progress) * (1.0 - progress)
            val freq = baseFreq * (1.0 - progress * 0.7)
            phase += 2.0 * Math.PI * freq / sampleRate
            val tone = sin(phase)
            val noise = (Random.nextFloat() * 2.0 - 1.0)
            val sample = (tone * (1f - noiseFactor) + noise * noiseFactor) * envelope * Short.MAX_VALUE * 0.9
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generatePanClang(): ShortArray {
        val totalSamples = (sampleRate * 0.35).toInt()
        val buffer = ShortArray(totalSamples)
        var p1 = 0.0
        var p2 = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = Math.exp(-progress * 9.0)
            p1 += 2.0 * Math.PI * 1450.0 / sampleRate
            p2 += 2.0 * Math.PI * 2280.0 / sampleRate
            val sample = (sin(p1) * 0.6 + sin(p2) * 0.4) * env * Short.MAX_VALUE * 0.85
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateExplosion(): ShortArray {
        val totalSamples = (sampleRate * 0.5).toInt()
        val buffer = ShortArray(totalSamples)
        var prevNoise = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = Math.exp(-progress * 5.0)
            val raw = (Random.nextDouble() * 2.0 - 1.0)
            // Low-pass filter for deep boom
            prevNoise = prevNoise * 0.85 + raw * 0.15
            val subBass = sin(2.0 * Math.PI * (60.0 * (1.0 - progress * 0.5)) * i / sampleRate) * 0.5
            val sample = (prevNoise * 0.7 + subBass) * env * Short.MAX_VALUE * 0.95
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHorn(): ShortArray {
        val totalSamples = (sampleRate * 0.28).toInt()
        val buffer = ShortArray(totalSamples)
        var p1 = 0.0
        var p2 = 0.0
        for (i in 0 until totalSamples) {
            p1 += 2.0 * Math.PI * 440.0 / sampleRate
            p2 += 2.0 * Math.PI * 554.0 / sampleRate // Dual-tone horn chord
            val env = if (i < 500) i / 500.0 else if (i > totalSamples - 1000) (totalSamples - i) / 1000.0 else 1.0
            val sample = (sin(p1) * 0.5 + sin(p2) * 0.5) * env * Short.MAX_VALUE * 0.6
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateDriftSqueal(): ShortArray {
        val totalSamples = (sampleRate * 0.2).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = sin(progress * Math.PI)
            val freq = 1600.0 + sin(i * 0.05) * 300.0
            phase += 2.0 * Math.PI * freq / sampleRate
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.3
            val sample = (sin(phase) * 0.7 + noise) * env * Short.MAX_VALUE * 0.5
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateNitroWhoosh(): ShortArray {
        val totalSamples = (sampleRate * 0.4).toInt()
        val buffer = ShortArray(totalSamples)
        var lpf = 0.0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = sin(progress * Math.PI)
            val raw = (Random.nextDouble() * 2.0 - 1.0)
            lpf = lpf * 0.7 + raw * 0.3
            val whoosh = sin(2.0 * Math.PI * (250.0 + progress * 600.0) * i / sampleRate) * 0.4
            val sample = (lpf * 0.6 + whoosh) * env * Short.MAX_VALUE * 0.8
            buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateReloadClick(): ShortArray {
        val totalSamples = (sampleRate * 0.15).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = Math.exp(-progress * 25.0)
            val click = sin(2.0 * Math.PI * 3200.0 * i / sampleRate)
            buffer[i] = (click * env * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        return buffer
    }

    private fun generateHealChime(): ShortArray {
        val totalSamples = (sampleRate * 0.3).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val env = (1.0 - progress)
            val chime = sin(2.0 * Math.PI * 880.0 * (1.0 + progress * 0.5) * i / sampleRate)
            buffer[i] = (chime * env * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        return buffer
    }

    private fun generateVictoryFanfare(): ShortArray {
        val totalSamples = (sampleRate * 0.8).toInt()
        val buffer = ShortArray(totalSamples)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C-E-G-C Major arpeggio
        val noteLength = totalSamples / notes.size
        for (i in 0 until totalSamples) {
            val noteIdx = (i / noteLength).coerceIn(0, notes.size - 1)
            val noteProgress = (i % noteLength).toDouble() / noteLength
            val env = (1.0 - noteProgress * 0.5)
            val tone = sin(2.0 * Math.PI * notes[noteIdx] * i / sampleRate)
            buffer[i] = (tone * env * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
        return buffer
    }
}
