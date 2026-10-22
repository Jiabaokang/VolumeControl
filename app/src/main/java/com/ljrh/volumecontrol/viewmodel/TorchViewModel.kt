package com.ljrh.volumecontrol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ljrh.volumecontrol.data.TorchController
import com.ljrh.volumecontrol.data.TorchState
import kotlinx.coroutines.flow.StateFlow

/** 闪光灯状态来自系统回调，直接透传 [TorchController] 的状态流。 */
class TorchViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = TorchController(application)
    val state: StateFlow<TorchState> = controller.state

    init {
        controller.start()
    }

    fun toggle() = controller.toggle()

    override fun onCleared() {
        controller.stop()
    }
}
