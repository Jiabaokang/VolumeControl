package com.ljrh.volumecontrol.ui

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ljrh.volumecontrol.R
import com.ljrh.volumecontrol.data.MediaVolumeController
import com.ljrh.volumecontrol.data.MediaVolumeState
import com.ljrh.volumecontrol.data.TorchController
import com.ljrh.volumecontrol.service.MediaVolumeTileService
import com.ljrh.volumecontrol.ui.theme.VolumeControlTheme

class MainActivity : ComponentActivity() {
    private lateinit var volume: MediaVolumeController
    private lateinit var torch: TorchController
    private var volumeState by mutableStateOf(MediaVolumeState(0, 0, 1, false))
    private var volumeError by mutableStateOf(false)
    private var cameraMessage by mutableStateOf<Int?>(null)
    private var cameraNeedsSettings by mutableStateOf(false)
    private val handler = Handler(Looper.getMainLooper())
    private val refreshVolume = object : Runnable {
        override fun run() {
            volumeState = volume.read()
            handler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_MUSIC
        volume = MediaVolumeController(this)
        torch = TorchController(this)
        volumeState = volume.read()
        setContent {
            val permission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    cameraMessage = null
                    cameraNeedsSettings = false
                    torch.toggle()
                } else {
                    cameraNeedsSettings = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    cameraMessage = if (cameraNeedsSettings) R.string.camera_permission_settings
                        else R.string.camera_permission
                }
            }
            VolumeControlTheme {
                VolumeControlScreen(
                    volume = volumeState,
                    torch = torch.state,
                    volumeError = volumeError,
                    cameraMessage = cameraMessage,
                    cameraNeedsSettings = cameraNeedsSettings,
                    onAdjust = { direction -> changeVolume { volume.adjust(direction) } },
                    onSetVolume = { value -> changeVolume { volume.set(value) } },
                    onToggleTorch = {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED) {
                            cameraMessage = null
                            torch.toggle()
                        } else {
                            permission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:$packageName".toUri()))
                    },
                    onAddTile = ::addTile
                )
            }
        }
    }

    private fun changeVolume(change: () -> MediaVolumeState) {
        try {
            volumeState = change()
            volumeError = false
        } catch (_: SecurityException) {
            volumeError = true
        }
    }

    private fun addTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        try {
            getSystemService(StatusBarManager::class.java).requestAddTileService(
                ComponentName(this, MediaVolumeTileService::class.java),
                getString(R.string.volume_tile_label),
                Icon.createWithResource(this, R.drawable.ic_volume),
                mainExecutor
            ) { result ->
                val message = when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.tile_added
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.tile_already_added
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> R.string.tile_not_added
                    else -> R.string.tile_error
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.tile_error, Toast.LENGTH_LONG).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, R.string.tile_error, Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        torch.start()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraMessage = null
            cameraNeedsSettings = false
        }
        // 仅在前台读取系统音量，兼容耳机、系统面板等外部来源的变更。
        handler.removeCallbacks(refreshVolume)
        handler.post(refreshVolume)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshVolume)
        super.onPause()
    }

    override fun onStop() {
        torch.stop()
        super.onStop()
    }
}
