package com.ljrh.volumecontrol

import android.Manifest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ljrh.volumecontrol.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val audio get() = compose.activity.getSystemService(AudioManager::class.java)

    // 抓住方向反转、误改铃声，以及滑条只更新界面却没改变系统音量的问题。
    @Test
    fun sliderChangesRealMediaVolumeWithoutTouchingRingtone() = withRestoredVolume {
        val ringtone = audio.getStreamVolume(AudioManager.STREAM_RING)
        val target = minOf(9, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0)
        compose.onNodeWithTag("volumeSlider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(target.toFloat()) }
        compose.waitUntil { audio.getStreamVolume(AudioManager.STREAM_MUSIC) == target }
        assertEquals(ringtone, audio.getStreamVolume(AudioManager.STREAM_RING))
    }

    @Test
    fun sliderControlsRealVolumeAtBothBounds() = withRestoredVolume {
        val slider = compose.onNodeWithTag("volumeSlider")
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.waitUntil { audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 }
        slider.performSemanticsAction(SemanticsActions.SetProgress) {
            it(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat())
        }
        compose.waitUntil {
            audio.getStreamVolume(AudioManager.STREAM_MUSIC) ==
                audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }
    }

    @Test
    fun externalVolumeChangeAppearsOnScreen() = withRestoredVolume {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        compose.waitUntil(3_000) {
            compose.onAllNodes(hasText("已静音")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun flashlightCanBeTurnedOnAndOff() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            compose.activity.packageName, Manifest.permission.CAMERA
        )
        try {
            compose.onNodeWithContentDescription("开启手电筒").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasContentDescription("关闭手电筒"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("关闭手电筒").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasContentDescription("开启手电筒"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            val camera = compose.activity.getSystemService(CameraManager::class.java)
            camera.cameraIdList.firstOrNull {
                camera.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }?.let { camera.setTorchMode(it, false) }
        }
    }

    private fun withRestoredVolume(block: () -> Unit) {
        val original = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        try {
            block()
        } finally {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
        }
    }
}
