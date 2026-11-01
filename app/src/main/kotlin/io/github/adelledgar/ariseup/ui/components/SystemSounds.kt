package io.github.adelledgar.ariseup.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Sonidos del Sistema sintetizados al vuelo (sin archivos de audio). */
object SystemSounds {
    @Volatile var enabled = true

    private const val RATE = 44_100

    /** Notificación: dos notas limpias. */
    fun ding() = play(listOf(Note(1318.5, 0.09), Note(1975.5, 0.22)))

    /** Advertencia: tono grave que se repite. */
    fun alert() = play(listOf(Note(220.0, 0.16), Note(0.0, 0.05), Note(220.0, 0.16), Note(0.0, 0.05), Note(196.0, 0.3)))

    /** Subida de nivel: arpegio ascendente. */
    fun levelUp() = play(listOf(Note(523.3, 0.08), Note(659.3, 0.08), Note(784.0, 0.08), Note(1046.5, 0.35)))

    private data class Note(val freq: Double, val seconds: Double)

    private fun play(notes: List<Note>) {
        if (!enabled) return
        thread(isDaemon = true) {
            runCatching {
                val pcm = synth(notes)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((pcm.size * 1000L / RATE) + 100)
                track.release()
            }
        }
    }

    private fun synth(notes: List<Note>): ShortArray {
        val out = ArrayList<Short>()
        for (n in notes) {
            val count = (n.seconds * RATE).toInt()
            for (i in 0 until count) {
                val t = i.toDouble() / RATE
                val env = (1 - exp(-t * 300)) * exp(-t * 6)
                val s = if (n.freq == 0.0) 0.0 else (sin(2 * PI * n.freq * t) + 0.3 * sin(4 * PI * n.freq * t)) * env
                out += (s * 0.35 * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return out.toShortArray()
    }
}
