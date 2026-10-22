package com.ljrh.volumecontrol.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ljrh.volumecontrol.data.MediaVolumeController
import com.ljrh.volumecontrol.data.MediaVolumeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** 主页面与弹窗共用的音量界面状态。 */
data class VolumeUiState(
    val volume: MediaVolumeState,
    val error: Boolean = false
)

/** 拖动滑条跨档时的反馈方式，选择结果跨启动保留。 */
enum class SlideFeedback {
    Haptic,
    Sound,
    Off;

    fun next(): SlideFeedback = entries[(ordinal + 1) % entries.size]
}

/**
 * 持有媒体音量状态并与系统对齐。
 *
 * 轮询写进 [state] 的上游：界面订阅时每 400 ms 读取真实音量，退订 1 秒后上游被取消，
 * 界面不必再自己开关 Handler。拖动滑条会直接写 [volume]，不必等下一次轮询。
 */
class VolumeViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = MediaVolumeController(application)
    private val preferences =
        application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val volume = MutableStateFlow(controller.read())
    private val error = MutableStateFlow(false)

    private val _feedback = MutableStateFlow(
        SlideFeedback.entries.getOrElse(
            preferences.getInt(KEY_FEEDBACK, SlideFeedback.Haptic.ordinal)
        ) { SlideFeedback.Haptic }
    )

    /** 滑条反馈方式，默认震动。 */
    val feedback: StateFlow<SlideFeedback> = _feedback.asStateFlow()

    private val poller = flow {
        while (true) {
            volume.value = controller.read()
            emit(Unit)
            delay(REFRESH_INTERVAL_MS)
        }
    }

    val state: StateFlow<VolumeUiState> = combine(volume, error, poller) { current, failed, _ ->
        VolumeUiState(current, failed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = VolumeUiState(controller.read())
    )

    fun setValue(value: Int) = update { controller.set(value) }

    /** 震动 → 声音 → 关闭，循环切换。 */
    fun cycleFeedback() {
        val next = _feedback.value.next()
        _feedback.value = next
        preferences.edit { putInt(KEY_FEEDBACK, next.ordinal) }
    }

    private fun update(change: () -> MediaVolumeState) {
        try {
            volume.value = change()
            error.value = false
        } catch (_: SecurityException) {
            error.value = true
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 400L
        const val STOP_TIMEOUT_MS = 1_000L
        const val PREFERENCES_NAME = "volume_control"
        const val KEY_FEEDBACK = "slide_feedback"
    }
}
