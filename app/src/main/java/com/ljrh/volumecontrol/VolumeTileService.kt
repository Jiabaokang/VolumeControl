package com.ljrh.volumecontrol

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings tiles cannot host a custom slider on Android 12, so tapping a tile
 * collapses the shade and opens a native dialog containing a real SeekBar.
 */
abstract class VolumeTileService : TileService() {
    private val volume by lazy { MediaVolumeController(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            updateVolumeTile()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
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
        openVolumeSlider()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openVolumeSlider() {
        val intent = Intent(this, VolumeSliderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.volume_error, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.volume_error, Toast.LENGTH_SHORT).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, R.string.volume_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateVolumeTile() {
        val tile = qsTile ?: return
        val current = volume.read()
        tile.label = getString(R.string.volume_tile_label)
        tile.subtitle = getString(R.string.volume_current, current.percent)
        tile.contentDescription = getString(R.string.volume_tile_content_description, current.percent)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_volume)
        // The tile opens a control dialog, so it is a one-shot action rather than a toggle.
        tile.state = if (current.fixed) Tile.STATE_UNAVAILABLE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        super.onDestroy()
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 500L
    }
}

/** 唯一的媒体音量磁贴：点击后收起面板并打开拖动式滑条。 */
class MediaVolumeTileService : VolumeTileService()
