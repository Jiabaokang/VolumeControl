package com.ljrh.volumecontrol

import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun VolumeControlScreen(
    volume: MediaVolumeState,
    torch: TorchState,
    volumeError: Boolean,
    cameraMessage: Int?,
    cameraNeedsSettings: Boolean,
    onAdjust: (Int) -> Unit,
    onSetVolume: (Int) -> Unit,
    onToggleTorch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddTile: (Boolean) -> Unit
) {
    var showTileHelp by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Scaffold(containerColor = colors.background) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_launcher_artwork), null,
                        Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)))
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.app_name), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                    }
                }
                VolumeCard(volume, volumeError, onAdjust, onSetVolume)
                FlashlightCard(torch, cameraMessage, cameraNeedsSettings, onToggleTorch, onOpenSettings)
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(painterResource(R.drawable.ic_quick_settings), null, Modifier.size(20.dp), tint = colors.primary)
                            Text(stringResource(R.string.quick_settings), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Text(stringResource(R.string.quick_settings_description), style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // 系统只允许逐个请求添加，由用户在系统对话框确认。
                            OutlinedButton(onClick = { onAddTile(true) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.quick_settings_add_up))
                            }
                            OutlinedButton(onClick = { onAddTile(false) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.quick_settings_add_down))
                            }
                        } else {
                            Text(stringResource(R.string.quick_settings_instructions),
                                style = MaterialTheme.typography.bodyMedium, lineHeight = 25.sp,
                                color = colors.onSurfaceVariant)
                        }
                        TextButton(onClick = { showTileHelp = true }, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(R.string.quick_settings_help))
                        }
                    }
                }
                Text(stringResource(R.string.footer_note), modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
    if (showTileHelp) {
        AlertDialog(
            onDismissRequest = { showTileHelp = false },
            icon = { Icon(painterResource(R.drawable.ic_quick_settings), null) },
            title = { Text(stringResource(R.string.quick_settings_help)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.quick_settings_instructions), lineHeight = 27.sp)
                    Text(stringResource(R.string.quick_settings_tip))
                }
            },
            confirmButton = { TextButton(onClick = { showTileHelp = false }) { Text(stringResource(R.string.close)) } }
        )
    }
}

@Composable
private fun VolumeCard(
    volume: MediaVolumeState,
    hasError: Boolean,
    onAdjust: (Int) -> Unit,
    onSetVolume: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sliderLabel = stringResource(R.string.media_volume)
    val percentLabel = stringResource(R.string.volume_percent, volume.percent)
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(
        containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer
    )) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_volume), null, Modifier.size(28.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.media_volume), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.media_description), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.padding(top = 16.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
                Text(volume.percent.toString(), fontSize = 70.sp, lineHeight = 76.sp, fontWeight = FontWeight.Bold)
                Text("%", Modifier.padding(start = 4.dp, bottom = 10.dp), fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                if (volume.value == volume.minimum) {
                    Text(stringResource(R.string.volume_muted), Modifier.padding(bottom = 12.dp),
                        style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(stringResource(R.string.volume_level, volume.value, volume.maximum), style = MaterialTheme.typography.bodySmall)
            Slider(
                value = volume.value.toFloat(),
                onValueChange = { onSetVolume(it.roundToInt()) },
                valueRange = volume.minimum.toFloat()..maxOf(volume.maximum, volume.minimum + 1).toFloat(),
                steps = (volume.maximum - volume.minimum - 1).coerceAtLeast(0),
                enabled = !volume.fixed && volume.maximum > volume.minimum,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("volumeSlider")
                    .semantics { contentDescription = sliderLabel; stateDescription = percentLabel }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                VolumeButton(false, volume.canLower, Modifier.weight(1f)) { onAdjust(AudioManager.ADJUST_LOWER) }
                VolumeButton(true, volume.canRaise, Modifier.weight(1f)) { onAdjust(AudioManager.ADJUST_RAISE) }
            }
            Text(stringResource(when {
                hasError -> R.string.volume_error
                volume.fixed -> R.string.volume_fixed
                else -> R.string.volume_hint
            }), Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall,
                color = if (hasError) colors.error else colors.onPrimaryContainer)
        }
    }
}

@Composable
private fun VolumeButton(increase: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val label = stringResource(if (increase) R.string.volume_up_description else R.string.volume_down_description)
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(68.dp).semantics { contentDescription = label },
        shape = RoundedCornerShape(22.dp),
        colors = if (increase) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(if (increase) "+" else "−", fontSize = 34.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (increase) R.string.volume_up else R.string.volume_down),
            style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun FlashlightCard(
    torch: TorchState,
    cameraMessage: Int?,
    needsSettings: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(if (torch.enabled) R.string.flashlight_on_description else R.string.flashlight_off_description)
    val status = when {
        torch.errorRes != null -> torch.errorRes
        !torch.hasFlash -> R.string.flashlight_missing
        !torch.available -> R.string.flashlight_busy
        torch.enabled -> R.string.flashlight_on
        else -> R.string.flashlight_off
    }
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(
        containerColor = if (torch.enabled) colors.secondaryContainer else colors.surface
    )) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(
                    if (torch.enabled) colors.onSecondaryContainer.copy(alpha = 0.10f) else colors.surfaceVariant,
                    CircleShape
                ), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_flashlight), null, Modifier.size(26.dp),
                        tint = if (torch.enabled) colors.onSecondaryContainer else colors.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.flashlight), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(stringResource(status), style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
                Switch(checked = torch.enabled, onCheckedChange = { onToggle() },
                    enabled = torch.hasFlash && torch.available && !torch.pending,
                    modifier = Modifier.semantics { contentDescription = label })
            }
            if (cameraMessage != null) {
                Text(stringResource(cameraMessage), style = MaterialTheme.typography.bodySmall, color = colors.error)
            }
            if (needsSettings) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.open_app_settings))
                }
            }
        }
    }
}
