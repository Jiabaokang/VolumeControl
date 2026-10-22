package com.ljrh.volumecontrol

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.ljrh.volumecontrol.ui.theme.VolumeControlTheme
import kotlin.math.roundToInt

/** 供快捷设置磁贴使用的轻量悬浮音量窗口。 */
class VolumeSliderActivity : ComponentActivity() {
    private lateinit var volume: MediaVolumeController
    private var volumeState by mutableStateOf(MediaVolumeState(0, 0, 1, false))
    private var volumeError by mutableStateOf(false)
    private val handler = Handler(Looper.getMainLooper())
    private val refreshVolume = object : Runnable {
        override fun run() {
            volumeState = volume.read()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setFinishOnTouchOutside(true)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply { dimAmount = 0.35f }
        volume = MediaVolumeController(this)
        volumeState = volume.read()
        setContent {
            VolumeControlTheme {
                VolumeSliderPopup(
                    volume = volumeState,
                    hasError = volumeError,
                    onSetVolume = { value -> changeVolume { volume.set(value) } },
                    onClose = ::finish
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshVolume)
        handler.post(refreshVolume)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshVolume)
        super.onPause()
    }

    private fun changeVolume(change: () -> MediaVolumeState) {
        try {
            volumeState = change()
            volumeError = false
        } catch (_: SecurityException) {
            volumeError = true
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshVolume)
        super.onDestroy()
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 400L
    }
}

@Composable
private fun VolumeSliderPopup(
    volume: MediaVolumeState,
    hasError: Boolean,
    onSetVolume: (Int) -> Unit,
    onClose: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sliderDescription = stringResource(R.string.media_volume)
    val percentDescription = stringResource(R.string.volume_percent, volume.percent)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colors.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_volume),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = colors.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.media_volume),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.volume_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = volume.percent.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%",
                    modifier = Modifier.padding(start = 4.dp, bottom = 5.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.volume_muted).takeIf {
                        volume.value == volume.minimum
                    }.orEmpty(),
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
            Slider(
                value = volume.value.toFloat(),
                onValueChange = { onSetVolume(it.roundToInt()) },
                valueRange = volume.minimum.toFloat()..maxOf(volume.maximum, volume.minimum + 1).toFloat(),
                steps = (volume.maximum - volume.minimum - 1).coerceAtLeast(0),
                enabled = !volume.fixed && volume.maximum > volume.minimum,
                modifier = Modifier.fillMaxWidth().testTag("quickVolumeSlider")
                    .semantics {
                        contentDescription = sliderDescription
                        stateDescription = percentDescription
                    }
            )
            Text(
                text = stringResource(R.string.volume_level, volume.value, volume.maximum),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (hasError || volume.fixed) {
                Text(
                    text = stringResource(if (hasError) R.string.volume_error else R.string.volume_fixed),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }
        }
    }
}
