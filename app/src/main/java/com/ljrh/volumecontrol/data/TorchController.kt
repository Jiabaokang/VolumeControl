package com.ljrh.volumecontrol.data

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.ljrh.volumecontrol.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val _state = MutableStateFlow(TorchState())
    val state: StateFlow<TorchState> = _state.asStateFlow()

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) {
                _state.value = TorchState(hasFlash = true, available = true, enabled = enabled)
            }
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) {
                _state.value = _state.value.copy(available = false, enabled = false, pending = false)
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
            _state.value = _state.value.copy(hasFlash = cameraId != null, errorRes = null)
            camera.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
            observing = true
        } catch (_: CameraAccessException) {
            _state.value = _state.value.copy(errorRes = R.string.flashlight_error)
        } catch (_: SecurityException) {
            _state.value = _state.value.copy(errorRes = R.string.camera_permission_settings)
        }
    }

    fun toggle() {
        val id = cameraId ?: return
        if (!_state.value.available || _state.value.pending) return
        try {
            _state.value = _state.value.copy(pending = true, errorRes = null)
            camera.setTorchMode(id, !_state.value.enabled)
        } catch (_: CameraAccessException) {
            _state.value = _state.value.copy(pending = false, errorRes = R.string.flashlight_error)
        } catch (_: SecurityException) {
            _state.value = _state.value.copy(pending = false, errorRes = R.string.camera_permission_settings)
        } catch (_: IllegalArgumentException) {
            _state.value = _state.value.copy(pending = false, errorRes = R.string.flashlight_missing)
        }
    }

    fun stop() {
        if (observing) camera.unregisterTorchCallback(callback)
        observing = false
    }
}
