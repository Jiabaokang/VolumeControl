package com.ljrh.volumecontrol

import android.media.AudioManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VolumeSliderActivityInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<VolumeSliderActivity>()

    private val audio get() = compose.activity.getSystemService(AudioManager::class.java)

    @Test
    fun popupSliderChangesTheRealMediaStreamAndRestoresIt() {
        val original = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minimum = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val target = if (original < maximum) minOf(original + 5, maximum)
            else maxOf(original - 5, minimum)
        try {
            compose.onNodeWithTag("quickVolumeSlider")
                .performSemanticsAction(SemanticsActions.SetProgress) { it(target.toFloat()) }
            compose.waitUntil(5_000) {
                audio.getStreamVolume(AudioManager.STREAM_MUSIC) == target
            }
            assertEquals(target, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
        } finally {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
        }
    }
}
