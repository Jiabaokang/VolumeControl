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
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ljrh.volumecontrol.R
import com.ljrh.volumecontrol.service.MediaVolumeTileService
import com.ljrh.volumecontrol.ui.theme.VolumeControlTheme
import com.ljrh.volumecontrol.viewmodel.TorchViewModel
import com.ljrh.volumecontrol.viewmodel.VolumeViewModel

class MainActivity : ComponentActivity() {
    private val volumeViewModel: VolumeViewModel by viewModels()
    private val torchViewModel: TorchViewModel by viewModels()
    private var cameraMessage by mutableStateOf<Int?>(null)
    private var cameraNeedsSettings by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            val volumeState by volumeViewModel.state.collectAsStateWithLifecycle()
            val torchState by torchViewModel.state.collectAsStateWithLifecycle()
            val feedback by volumeViewModel.feedback.collectAsStateWithLifecycle()
            val permission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    cameraMessage = null
                    cameraNeedsSettings = false
                    torchViewModel.toggle()
                } else {
                    cameraNeedsSettings = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                    cameraMessage = if (cameraNeedsSettings) R.string.camera_permission_settings
                        else R.string.camera_permission
                }
            }
            VolumeControlTheme {
                VolumeControlScreen(
                    volume = volumeState.volume,
                    torch = torchState,
                    volumeError = volumeState.error,
                    cameraMessage = cameraMessage,
                    cameraNeedsSettings = cameraNeedsSettings,
                    feedback = feedback,
                    onSetVolume = volumeViewModel::setValue,
                    onToggleTorch = {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED) {
                            cameraMessage = null
                            torchViewModel.toggle()
                        } else {
                            permission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:$packageName".toUri()))
                    },
                    onAddTile = ::addTile,
                    onCycleFeedback = volumeViewModel::cycleFeedback
                )
            }
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

    override fun onResume() {
        super.onResume()
        // 从设置页返回或首次授权后，清理已经过时的权限提示。
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            cameraMessage = null
            cameraNeedsSettings = false
        }
    }
}
