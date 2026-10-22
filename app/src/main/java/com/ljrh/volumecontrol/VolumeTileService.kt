package com.ljrh.volumecontrol

import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

abstract class VolumeTileService : TileService() {
    protected abstract val direction: Int
    protected abstract val labelRes: Int
    private val volume by lazy { MediaVolumeController(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            updateVolumeTile()
            handler.postDelayed(this, 500)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        handler.removeCallbacks(refresh)
        handler.post(refresh)
    }

    override fun onStopListening() {
        handler.removeCallbacks(refresh)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        try {
            volume.adjust(direction)
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.volume_error, Toast.LENGTH_SHORT).show()
        }
        updateVolumeTile()
    }

    private fun updateVolumeTile() {
        val tile = qsTile ?: return
        val current = volume.read()
        val detail = when {
            current.fixed -> getString(R.string.volume_fixed)
            direction == AudioManager.ADJUST_RAISE && !current.canRaise -> getString(R.string.volume_max)
            direction == AudioManager.ADJUST_LOWER && !current.canLower -> getString(R.string.volume_min)
            else -> getString(R.string.volume_current, current.percent)
        }
        tile.label = getString(labelRes)
        tile.subtitle = detail
        tile.contentDescription = "${tile.label}，$detail"
        // 这是一次性动作，不是开关；到达边界时仍保留可点击的磁贴。
        tile.state = if (current.fixed) Tile.STATE_UNAVAILABLE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }
}

class VolumeUpTileService : VolumeTileService() {
    override val direction = AudioManager.ADJUST_RAISE
    override val labelRes = R.string.volume_up
}

class VolumeDownTileService : VolumeTileService() {
    override val direction = AudioManager.ADJUST_LOWER
    override val labelRes = R.string.volume_down
}
