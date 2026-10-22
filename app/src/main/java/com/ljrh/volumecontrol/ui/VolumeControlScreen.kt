package com.ljrh.volumecontrol.ui

import android.media.AudioManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ljrh.volumecontrol.R
import com.ljrh.volumecontrol.data.MediaVolumeState
import com.ljrh.volumecontrol.data.TorchState
import com.ljrh.volumecontrol.viewmodel.SlideFeedback
import kotlin.math.roundToInt

/** 主界面：滑条是唯一调节入口，手电筒收在右上角。 */
@Composable
fun VolumeControlScreen(
    volume: MediaVolumeState,
    torch: TorchState,
    volumeError: Boolean,
    cameraMessage: Int?,
    cameraNeedsSettings: Boolean,
    feedback: SlideFeedback,
    onSetVolume: (Int) -> Unit,
    onToggleTorch: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddTile: () -> Unit,
    onCycleFeedback: () -> Unit
) {
    var showTileHelp by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    // 手电筒的异常与不可用状态只在需要解释时占一行，正常情况下完全隐藏。
    val torchNotice = cameraMessage ?: torch.errorRes ?: when {
        !torch.hasFlash -> R.string.flashlight_missing
        !torch.available -> R.string.flashlight_busy
        else -> null
    }
    val noticeIsError = cameraMessage != null || torch.errorRes != null
    val needsSettings = cameraNeedsSettings ||
        torch.errorRes == R.string.camera_permission_settings

    Scaffold(containerColor = colors.background) { insets ->
        Box(
            Modifier.fillMaxSize().padding(insets),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                AppHeader(torch, onToggleTorch)

                if (torchNotice != null) {
                    TorchMessage(torchNotice, noticeIsError, needsSettings, onOpenSettings)
                }

                Spacer(Modifier.height(28.dp))
                VolumeSection(volume, volumeError, feedback, onSetVolume, onCycleFeedback)

                Spacer(Modifier.height(18.dp))
                TileEntry(onClick = { showTileHelp = true })
                Spacer(Modifier.height(26.dp))
                Text(
                    text = stringResource(R.string.footer_note),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showTileHelp) {
        TileHelpDialog(
            onDismiss = { showTileHelp = false },
            onAddTile = {
                showTileHelp = false
                onAddTile()
            }
        )
    }
}

@Composable
private fun AppHeader(torch: TorchState, onToggleTorch: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_artwork),
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
        TorchControl(torch, onToggleTorch)
    }
}

/** 右上角常驻：开关在图标正上方，与左侧标题同一水平线。 */
@Composable
private fun TorchControl(torch: TorchState, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ready = torch.hasFlash && torch.available && !torch.pending
    val label = stringResource(
        if (torch.enabled) R.string.flashlight_on_description else R.string.flashlight_off_description
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CompactSwitch(
            checked = torch.enabled,
            enabled = ready,
            onCheckedChange = onToggle,
            contentDescription = label
        )
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier.size(30.dp).drawBehind {
                if (torch.enabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.primary.copy(alpha = 0.42f), Color.Transparent)
                        ),
                        radius = size.minDimension / 2f
                    )
                }
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_flashlight),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (torch.enabled) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CompactSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: () -> Unit,
    contentDescription: String
) {
    val colors = MaterialTheme.colorScheme
    val offset by animateDpAsState(if (checked) 21.dp else 3.dp, label = "switchThumb")
    val track by animateColorAsState(
        if (checked) colors.primary else colors.surfaceVariant, label = "switchTrack"
    )
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 22.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { onCheckedChange() }
            )
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(offset.roundToPx(), 0) }
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) colors.onPrimary else Color.White)
        )
    }
}

@Composable
private fun TorchMessage(
    messageRes: Int,
    isError: Boolean,
    needsSettings: Boolean,
    onOpenSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (isError) colors.error else colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flashlight),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(messageRes),
            modifier = Modifier.weight(1f),
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = tint
        )
        if (needsSettings) {
            Text(
                text = stringResource(R.string.open_app_settings),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun VolumeSection(
    volume: MediaVolumeState,
    hasError: Boolean,
    feedback: SlideFeedback,
    onSetVolume: (Int) -> Unit,
    onCycleFeedback: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val muted = volume.value == volume.minimum
    val onCard = colors.onPrimaryContainer
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(colors.primaryContainer, colors.surface),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.media_volume),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = onCard.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                StepPill(stringResource(R.string.volume_level, volume.value, volume.maximum))
            }

            Row(
                Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = volume.percent.toString(),
                    fontSize = 74.sp,
                    lineHeight = 78.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    color = onCard
                )
                Text(
                    text = "%",
                    modifier = Modifier.padding(start = 3.dp, bottom = 10.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = onCard.copy(alpha = 0.55f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.media_description),
                    fontSize = 11.5.sp,
                    color = onCard.copy(alpha = 0.65f)
                )
                if (muted) {
                    Spacer(Modifier.width(8.dp))
                    MutedBadge()
                }
            }

            Spacer(Modifier.height(24.dp))
            VolumeSlider(volume, feedback, onSetVolume)

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.volume_silent), // 轨道最左端对应静音
                    fontSize = 11.sp,
                    color = onCard.copy(alpha = 0.55f)
                )
                Spacer(Modifier.weight(1f))
                FeedbackChip(feedback, onCard.copy(alpha = 0.75f), onCycleFeedback)
            }

            val message = when {
                hasError -> R.string.volume_error
                volume.fixed -> R.string.volume_fixed
                else -> null
            }
            if (message != null) {
                Text(
                    text = stringResource(message),
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 11.5.sp,
                    color = if (hasError) colors.error else onCard.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StepPill(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 10.5.sp,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun MutedBadge() {
    val colors = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = colors.secondaryContainer) {
        Text(
            text = stringResource(R.string.volume_muted),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackChip(feedback: SlideFeedback, contentColor: Color, onClick: () -> Unit) {
    val label = stringResource(feedback.labelRes)
    val description = stringResource(R.string.volume_feedback_description, label)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.22f)),
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(feedback.iconRes),
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = contentColor
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

private val SlideFeedback.iconRes: Int
    get() = when (this) {
        SlideFeedback.Haptic -> R.drawable.ic_vibration
        SlideFeedback.Sound -> R.drawable.ic_volume
        SlideFeedback.Off -> R.drawable.ic_volume_off
    }

private val SlideFeedback.labelRes: Int
    get() = when (this) {
        SlideFeedback.Haptic -> R.string.volume_feedback_haptic
        SlideFeedback.Sound -> R.string.volume_feedback_sound
        SlideFeedback.Off -> R.string.volume_feedback_off
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSlider(
    volume: MediaVolumeState,
    feedback: SlideFeedback,
    onSetVolume: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val audioManager = LocalContext.current.getSystemService(AudioManager::class.java)
    // 逐档去重：每跨过一档才反馈一次，不跟随每个像素。
    var tickedStep by remember { mutableIntStateOf(volume.value) }
    val fraction = (volume.percent / 100f).coerceIn(0f, 1f)
    val sliderLabel = stringResource(R.string.media_volume)
    val percentLabel = stringResource(R.string.volume_percent, volume.percent)

    Slider(
        value = volume.value.toFloat(),
        onValueChange = { raw ->
            val next = raw.roundToInt().coerceIn(volume.minimum, volume.maximum)
            onSetVolume(next)
            if (next != tickedStep) {
                tickedStep = next
                when (feedback) {
                    SlideFeedback.Haptic ->
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                    SlideFeedback.Sound ->
                        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)

                    SlideFeedback.Off -> Unit
                }
            }
        },
        valueRange = volume.minimum.toFloat()..maxOf(volume.maximum, volume.minimum + 1).toFloat(),
        steps = (volume.maximum - volume.minimum - 1).coerceAtLeast(0),
        enabled = !volume.fixed && volume.maximum > volume.minimum,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("volumeSlider")
            .semantics {
                contentDescription = sliderLabel
                stateDescription = percentLabel
            },
        thumb = {
            Box(
                Modifier
                    .size(26.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(8.dp).background(colors.primary, CircleShape))
            }
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(colors.onPrimaryContainer.copy(alpha = 0.14f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF7D74FF), colors.primary))
                        )
                )
            }
        }
    )
}

@Composable
private fun TileEntry(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).background(colors.surfaceVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_quick_settings),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.primary
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.quick_settings_entry),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.quick_settings_entry_description),
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun TileHelpDialog(onDismiss: () -> Unit, onAddTile: () -> Unit) {
    val canRequestTile = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_quick_settings), null) },
        title = { Text(stringResource(R.string.quick_settings_help)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.quick_settings_instructions), lineHeight = 26.sp)
                Text(stringResource(R.string.quick_settings_tip))
            }
        },
        confirmButton = {
            if (canRequestTile) {
                // 系统只允许逐个请求添加，由用户在系统对话框确认。
                TextButton(onClick = onAddTile) {
                    Text(stringResource(R.string.quick_settings_add_tile))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
        dismissButton = if (canRequestTile) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
        } else null
    )
}
