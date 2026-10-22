package com.ljrh.volumecontrol

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class TorchState(
    val hasFlash: Boolean = false,
    val available: Boolean = false,
    val enabled: Boolean = false,
    val pending: Boolean = false,
    val errorRes: Int? = null
)

/** 通过系统回调显示真实开关状态，也能同步系统面板对手电筒的操作。 */
class TorchController(context: Context) {
    private val camera = context.getSystemService(CameraManager::class.java)
    private var cameraId: String? = null
    private var observing = false
    var state by mutableStateOf(TorchState())
        private set

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) {
                state = TorchState(hasFlash = true, available = true, enabled = enabled)
            }
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) {
                state = state.copy(available = false, enabled = false, pending = false)
            }
        }
    }

    fun start() {
        if (observing) return
        try {
            val cameras = camera.cameraIdList.filter {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId = cameras.firstOrNull {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            } ?: cameras.firstOrNull()
            state = state.copy(hasFlash = cameraId != null, errorRes = null)
            camera.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
            observing = true
        } catch (_: CameraAccessException) {
            state = state.copy(errorRes = R.string.flashlight_error)
        } catch (_: SecurityException) {
            state = state.copy(errorRes = R.string.camera_permission_settings)
        }
    }

    fun toggle() {
        val id = cameraId ?: return
        if (!state.available || state.pending) return
        try {
            state = state.copy(pending = true, errorRes = null)
            camera.setTorchMode(id, !state.enabled)
        } catch (_: CameraAccessException) {
            state = state.copy(pending = false, errorRes = R.string.flashlight_error)
        } catch (_: SecurityException) {
            state = state.copy(pending = false, errorRes = R.string.camera_permission_settings)
        } catch (_: IllegalArgumentException) {
            state = state.copy(pending = false, errorRes = R.string.flashlight_missing)
        }
    }

    fun stop() {
        if (observing) camera.unregisterTorchCallback(callback)
        observing = false
    }
}
