package com.ljrh.volumecontrol

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

data class MediaVolumeState(
    val value: Int,
    val minimum: Int,
    val maximum: Int,
    val fixed: Boolean
) {
    val percent: Int get() = if (maximum > minimum) {
        ((value - minimum) * 100f / (maximum - minimum)).roundToInt().coerceIn(0, 100)
    } else 0
    val canRaise: Boolean get() = !fixed && value < maximum
    val canLower: Boolean get() = !fixed && value > minimum
}

/** APP 和磁贴共同操作媒体流，避免误改来电、闹钟或勿扰模式。 */
class MediaVolumeController(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)

    fun read() = MediaVolumeState(
        value = audio.getStreamVolume(AudioManager.STREAM_MUSIC),
        minimum = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC),
        maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
        fixed = audio.isVolumeFixed
    )

    fun adjust(direction: Int): MediaVolumeState {
        val current = read()
        if ((direction == AudioManager.ADJUST_RAISE && current.canRaise) ||
            (direction == AudioManager.ADJUST_LOWER && current.canLower)) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
        return read()
    }

    fun set(value: Int): MediaVolumeState {
        val current = read()
        if (!current.fixed) {
            audio.setStreamVolume(
                AudioManager.STREAM_MUSIC, value.coerceIn(current.minimum, current.maximum), 0
            )
        }
        return read()
    }
}
