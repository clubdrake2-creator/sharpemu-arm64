// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.
//
// Ported from PCSX2-Android (VirtualControlLayoutEditorScreen.kt) and adapted for shadPS4: the
// PCSX2 `Prefs` calls are routed to shadPS4's `ControlPrefs`, the resource package is shadPS4's, and
// PS4 button labels are used. The visual on-screen layout editor (drag to move, per-button scale /
// opacity, grid, save/restore) is otherwise identical to the original.

package org.sharpemu.android.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateOffsetAsState
import android.widget.Toast
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.sharpemu.android.R
import org.sharpemu.android.data.ControlPrefs
import org.sharpemu.android.overlay.controlColorFilter
import org.sharpemu.android.ui.theme.AppearancePrefs
import org.sharpemu.android.ui.theme.appUiText
import kotlin.math.min
import kotlin.math.roundToInt

private enum class VirtualButtonType { SHOULDER, DPAD, FACE, SYSTEM, STICK, AUX }

private data class VirtualButtonSpec(
    val id: String,
    val label: String,
    val type: VirtualButtonType,
    val defaultX: Float,
    val defaultY: Float,
    val widthDp: Float,
    val heightDp: Float,
    val symbolColor: Color = Color(0xFF434343),
)

private fun sanitizeNormalized(value: Float, fallback: Float): Float {
    if (!value.isFinite()) return fallback
    return value.coerceIn(0f, 1f)
}

@Composable
fun VirtualControlLayoutEditorScreen(
    overlayContainerColor: Color,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    ForceLandscapeOrientation()
    BackHandler(onBack = onBack)

    remember {
        ControlPrefs.ensureVirtualLayoutMigration(context)
        0
    }

    val specs = remember {
        listOf(
            VirtualButtonSpec("l2", "L2", VirtualButtonType.SHOULDER, 0.09584009f, 0.13170458f, 138f, 72f),
            VirtualButtonSpec("l1", "L1", VirtualButtonType.SHOULDER, 0.10f, 0.26f, 138f, 72f),
            VirtualButtonSpec("r2", "R2", VirtualButtonType.SHOULDER, 0.89507091f, 0.14186218f, 138f, 72f),
            VirtualButtonSpec("r1", "R1", VirtualButtonType.SHOULDER, 0.89798176f, 0.25400078f, 138f, 72f),

            VirtualButtonSpec("dpad_up", "▲", VirtualButtonType.DPAD, 0.11f, 0.49f, 80f, 100f),
            VirtualButtonSpec("dpad_left", "◀", VirtualButtonType.DPAD, 0.063f, 0.57f, 100f, 80f),
            VirtualButtonSpec("dpad_right", "▶", VirtualButtonType.DPAD, 0.157f, 0.57f, 100f, 80f),
            VirtualButtonSpec("dpad_down", "▼", VirtualButtonType.DPAD, 0.11f, 0.65f, 80f, 100f),

            VirtualButtonSpec("left_stick", "L3", VirtualButtonType.STICK, 0.24f, 0.78f, 98f, 98f),
            VirtualButtonSpec("right_stick", "R3", VirtualButtonType.STICK, 0.90977818f, 0.55615997f, 98f, 98f),

            VirtualButtonSpec("triangle", "△", VirtualButtonType.FACE, 0.76f, 0.66f, 72f, 72f),
            VirtualButtonSpec("square", "□", VirtualButtonType.FACE, 0.71f, 0.77f, 72f, 72f),
            VirtualButtonSpec("circle", "○", VirtualButtonType.FACE, 0.81f, 0.77f, 72f, 72f),
            VirtualButtonSpec("cross", "✕", VirtualButtonType.FACE, 0.57019383f, 0.87845618f, 72f, 72f),

            VirtualButtonSpec("select", "-", VirtualButtonType.SYSTEM, 0.43f, 0.88f, 74f, 74f),
            VirtualButtonSpec("ps", "⌂", VirtualButtonType.SYSTEM, 0.50f, 0.88f, 74f, 74f),
            VirtualButtonSpec("start", "+", VirtualButtonType.SYSTEM, 0.76420736f, 0.89215344f, 74f, 74f),
        )
    }

    val savedPositions = remember { ControlPrefs.getVirtualButtonPositions(context) }
    val positions = remember {
        mutableStateMapOf<String, Pair<Float, Float>>().apply {
            specs.forEach { put(it.id, it.defaultX to it.defaultY) }
            savedPositions.forEach { (id, pos) ->
                if (containsKey(id)) this[id] = pos
            }
        }
    }

    var buttonScale by remember { mutableFloatStateOf(ControlPrefs.getVirtualButtonScale(context)) }
    var buttonOpacity by remember { mutableFloatStateOf(ControlPrefs.getVirtualButtonOpacity(context)) }
    // User-chosen control color (default white). Drives the live preview tint for every editor button
    // (including the D-pad arrows) and is persisted so the in-game overlay uses the same color.
    var controlColor by remember { mutableStateOf(Color(ControlPrefs.getVirtualButtonColor(context))) }
    val perButtonScaleOverrides = remember {
        mutableStateMapOf<String, Float>().apply {
            ControlPrefs.getVirtualButtonScaleOverrides(context).forEach { (id, value) ->
                if (id.isNotBlank() && value.isFinite()) put(id, value.coerceIn(0.35f, 2.0f))
            }
        }
    }
    val perButtonOpacityOverrides = remember {
        mutableStateMapOf<String, Float>().apply {
            ControlPrefs.getVirtualButtonOpacityOverrides(context).forEach { (id, value) ->
                if (id.isNotBlank() && value.isFinite()) put(id, value.coerceIn(0.20f, 2.0f))
            }
        }
    }
    val perButtonColorOverrides = remember {
        mutableStateMapOf<String, Int>().apply {
            ControlPrefs.getVirtualButtonColorOverrides(context).forEach { (id, value) ->
                if (id.isNotBlank()) put(id, value)
            }
        }
    }
    val perButtonImageOverrides = remember {
        mutableStateMapOf<String, String>().apply {
            ControlPrefs.getVirtualButtonImageOverrides(context).forEach { (id, value) ->
                if (id.isNotBlank() && value.isNotBlank()) put(id, value)
            }
        }
    }
    var controlsEnabled by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var perButtonMode by remember { mutableStateOf(false) }
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    val pngPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val selected = selectedButtonId
        if (uri == null || selected == null) return@rememberLauncherForActivityResult
        val path = ControlPrefs.setVirtualButtonImageOverride(context, selected, uri)
        if (path != null) {
            perButtonImageOverrides[selected] = path
            Toast.makeText(context, uiText.text("PNG do botão importado"), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, uiText.text("Não foi possível importar o PNG"), Toast.LENGTH_SHORT).show()
        }
    }

    var menuExpanded by remember { mutableStateOf(true) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    var menuReady by remember { mutableStateOf(false) }
    var animateMenuOffset by remember { mutableStateOf(false) }
    val blockerInteraction = remember { MutableInteractionSource() }

    val overlayColor = overlayContainerColor
    val overlayTextColor = contentColorFor(overlayColor)
    val overlaySecondaryTextColor = overlayTextColor.copy(alpha = 0.74f)
    val overlayAccent = MaterialTheme.colorScheme.primary
    val overlayAccentOn = MaterialTheme.colorScheme.onPrimary

    fun baseControlScale(spec: VirtualButtonSpec): Float = when {
        spec.id == "cross" -> 0.50f
        spec.id == "select" -> 0.50f
        spec.id == "start" -> 0.82f
        spec.type == VirtualButtonType.STICK -> 1.2f
        spec.type == VirtualButtonType.FACE -> 0.82f
        spec.type == VirtualButtonType.DPAD -> 0.782f
        spec.type == VirtualButtonType.SHOULDER -> 1.120f
        else -> 0.72f
    }

    val animatedMenuOffset by animateOffsetAsState(
        targetValue = menuOffset,
        animationSpec = if (animateMenuOffset) {
            tween(durationMillis = 170, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "virtual_overlay_offset",
        finishedListener = {
            animateMenuOffset = false
        },
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090B))
            .clickable(
                interactionSource = blockerInteraction,
                indication = null,
                onClick = {},
            )
    ) {
        val panelWidthDp = (maxWidth * 0.58f).coerceIn(460.dp, 760.dp)
        val panelHeightEstimateDp = 212.dp
        val panelMiniWidthDp = 84.dp
        val panelTopPaddingDp = 8.dp

        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current

        val panelWidthPx = with(density) { panelWidthDp.toPx() }
        val panelHeightPx = with(density) { panelHeightEstimateDp.toPx() }
        val panelMiniWidthPx = with(density) { panelMiniWidthDp.toPx() }
        val panelTopPaddingPx = with(density) { panelTopPaddingDp.toPx() }
        val initialPanelOffset = Offset(
            x = ((widthPx - panelWidthPx) / 2f).coerceAtLeast(0f),
            y = panelTopPaddingPx,
        )
        val initialMiniOffset = Offset(
            x = ((widthPx - panelMiniWidthPx) / 2f).coerceAtLeast(0f),
            y = panelTopPaddingPx,
        )

        val maxPanelY = (heightPx - panelHeightPx).coerceAtLeast(panelTopPaddingPx)

        LaunchedEffect(widthPx, panelWidthPx, panelHeightPx, panelTopPaddingPx) {
            if (!menuReady) {
                menuExpanded = true
                menuOffset = initialPanelOffset
                menuReady = true
            } else {
                menuOffset = Offset(
                    x = menuOffset.x.coerceIn(0f, (widthPx - panelWidthPx).coerceAtLeast(0f)),
                    y = menuOffset.y.coerceIn(panelTopPaddingPx, maxPanelY),
                )
            }
        }

        if (showGrid) {
            Canvas(Modifier.fillMaxSize()) {
                val columns = 40
                val rows = 20
                val majorEvery = 5
                for (i in 0..columns) {
                    val x = size.width * (i.toFloat() / columns.toFloat())
                    val isMajor = i % majorEvery == 0
                    drawLine(
                        color = Color.White.copy(alpha = if (isMajor) 0.18f else 0.09f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = if (isMajor) 2f else 1f,
                    )
                }
                for (i in 0..rows) {
                    val y = size.height * (i.toFloat() / rows.toFloat())
                    val isMajor = i % majorEvery == 0
                    drawLine(
                        color = Color.White.copy(alpha = if (isMajor) 0.18f else 0.09f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (isMajor) 2f else 1f,
                    )
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.24f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.24f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2f,
                )
            }
        }

        specs.forEach { spec ->
            val rawCurrent = positions[spec.id] ?: (spec.defaultX to spec.defaultY)
            val currentX = sanitizeNormalized(rawCurrent.first, spec.defaultX)
            val currentY = sanitizeNormalized(rawCurrent.second, spec.defaultY)
            if (currentX != rawCurrent.first || currentY != rawCurrent.second) {
                positions[spec.id] = currentX to currentY
            }
            val perScale = perButtonScaleOverrides[spec.id]?.coerceIn(0.35f, 2.0f) ?: 1.0f
            val perOpacity = perButtonOpacityOverrides[spec.id]?.coerceIn(0.20f, 2.0f) ?: 1.0f
            val controlScale = baseControlScale(spec) * perScale
            val itemWidthDp = (spec.widthDp * buttonScale * controlScale).dp
            val itemHeightDp = (spec.heightDp * buttonScale * controlScale).dp
            val itemWidthPx = with(density) { itemWidthDp.toPx() }
            val itemHeightPx = with(density) { itemHeightDp.toPx() }
            val fineTuneOffsetX = with(density) {
                when (spec.id) {
                    "right_stick" -> -4.dp.toPx()
                    else -> 0f
                }
            }
            val fineTuneOffsetY = 0f
            val xPx = (currentX * widthPx) - itemWidthPx / 2f + fineTuneOffsetX
            val yPx = (currentY * heightPx) - itemHeightPx / 2f + fineTuneOffsetY
            val resolvedAlpha = (buttonOpacity * perOpacity).coerceIn(0.05f, 1.0f)
            val baseAlpha = if (controlsEnabled) resolvedAlpha else resolvedAlpha * 0.4f
            val buttonColor = perButtonColorOverrides[spec.id]?.let { Color(it) } ?: controlColor
            val buttonTint = controlColorFilter(buttonColor)

            Box(
                modifier = Modifier
                    .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                    .size(itemWidthDp, itemHeightDp)
                    .clickable(
                        interactionSource = remember(spec.id) { MutableInteractionSource() },
                        indication = null,
                        enabled = perButtonMode,
                    ) {
                        selectedButtonId = spec.id
                    }
                    .pointerInput(spec.id, buttonScale) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val existingRaw = positions[spec.id] ?: (spec.defaultX to spec.defaultY)
                            val existing = sanitizeNormalized(existingRaw.first, spec.defaultX) to
                                sanitizeNormalized(existingRaw.second, spec.defaultY)
                            val nextX = (existing.first + (dragAmount.x / widthPx)).coerceIn(0.03f, 0.97f)
                            val nextY = (existing.second + (dragAmount.y / heightPx)).coerceIn(0.06f, 0.95f)
                            positions[spec.id] = nextX to nextY
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                VirtualButton(
                    spec = spec,
                    alpha = baseAlpha,
                    tint = buttonTint,
                    customImagePath = perButtonImageOverrides[spec.id],
                )
                if (perButtonMode && selectedButtonId == spec.id) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, overlayAccent, RoundedCornerShape(12.dp))
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(tween(150, easing = LinearEasing)) + scaleIn(
                animationSpec = tween(150, easing = FastOutSlowInEasing),
                initialScale = 0.96f,
            ),
            exit = fadeOut(tween(110, easing = LinearEasing)) + scaleOut(
                animationSpec = tween(110, easing = FastOutSlowInEasing),
                targetScale = 0.92f,
            ),
            modifier = Modifier.offset {
                IntOffset(animatedMenuOffset.x.roundToInt(), animatedMenuOffset.y.roundToInt())
            },
        ) {
            Column(
                modifier = Modifier
                    .width(panelWidthDp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(overlayColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .pointerInput(widthPx, panelWidthPx, panelHeightPx) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                animateMenuOffset = false
                                menuOffset = Offset(
                                    x = (menuOffset.x + drag.x).coerceIn(
                                        0f,
                                        (widthPx - panelWidthPx).coerceAtLeast(0f),
                                    ),
                                    y = (menuOffset.y + drag.y).coerceIn(
                                        panelTopPaddingPx,
                                        maxPanelY,
                                    ),
                                )
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(overlaySecondaryTextColor.copy(alpha = 0.65f))
                            .align(Alignment.TopCenter)
                    )
                    IconButton(
                        onClick = {
                            animateMenuOffset = false
                            menuOffset = initialPanelOffset
                            menuExpanded = false
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(overlayTextColor.copy(alpha = 0.12f)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = uiText.close,
                            tint = overlayTextColor,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selectedLabel = selectedButtonId
                        ?.let { id -> specs.firstOrNull { it.id == id }?.label ?: id }
                        ?: uiText.text("Todos")
                    Text(
                        text = "${uiText.text("Botão atual")}: $selectedLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = overlayTextColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = uiText.text("Selecionar todos"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = overlaySecondaryTextColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            selectedButtonId = null
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayCheckboxRow(
                        label = uiText.optionLabel("Enabled"),
                        checked = controlsEnabled,
                        onCheckedChange = { controlsEnabled = it },
                        textColor = overlayTextColor,
                    )
                    OverlayCheckboxRow(
                        label = uiText.text("Alternar"),
                        checked = perButtonMode,
                        onCheckedChange = {
                            perButtonMode = it
                            if (!it) selectedButtonId = null
                        },
                        textColor = overlayTextColor,
                    )
                    OverlayCheckboxRow(
                        label = uiText.text("Grade"),
                        checked = showGrid,
                        onCheckedChange = { showGrid = it },
                        textColor = overlayTextColor,
                    )
                }

                if (perButtonMode && selectedButtonId == null) {
                    Text(
                        text = uiText.text("Toque em um botão para editar somente ele."),
                        style = MaterialTheme.typography.bodySmall,
                        color = overlaySecondaryTextColor,
                    )
                }

                val selectedButton = if (perButtonMode) selectedButtonId else null
                val activeScale = selectedButton?.let { perButtonScaleOverrides[it] } ?: 1.0f
                val activeOpacity = selectedButton?.let { perButtonOpacityOverrides[it] } ?: 1.0f
                OverlaySliderRow(
                    label = uiText.text("Escala"),
                    valueText = if (selectedButton != null) {
                        "${(activeScale * 100f).roundToInt()}%"
                    } else {
                        "${(buttonScale * 100f).roundToInt()}%"
                    },
                    value = if (selectedButton != null) activeScale else buttonScale,
                    onValueChange = { newValue ->
                        if (selectedButton != null) {
                            perButtonScaleOverrides[selectedButton] = newValue.coerceIn(0.35f, 2.0f)
                        } else {
                            buttonScale = newValue.coerceIn(0.6f, 1.6f)
                        }
                    },
                    valueRange = if (selectedButton != null) 0.35f..2.0f else 0.6f..1.6f,
                    steps = if (selectedButton != null) 66 else 40,
                    textColor = overlayTextColor,
                )

                OverlaySliderRow(
                    label = uiText.text("Opacidade"),
                    valueText = if (selectedButton != null) {
                        "${(activeOpacity * 100f).roundToInt()}%"
                    } else {
                        "${(buttonOpacity * 100f).roundToInt()}%"
                    },
                    value = if (selectedButton != null) activeOpacity else buttonOpacity,
                    onValueChange = { newValue ->
                        if (selectedButton != null) {
                            perButtonOpacityOverrides[selectedButton] = newValue.coerceIn(0.20f, 2.0f)
                        } else {
                            buttonOpacity = newValue.coerceIn(0.15f, 1f)
                        }
                    },
                    valueRange = if (selectedButton != null) 0.20f..2.0f else 0.15f..1f,
                    steps = if (selectedButton != null) 72 else 34,
                    textColor = overlayTextColor,
                )

                // Control color picker: pick a swatch to recolor every on-screen button (and the editor
                // preview) live. Applied immediately and persisted so the in-game overlay matches.
                Text(
                    text = uiText.text("Cor dos botões"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = overlayTextColor,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val editingColor = selectedButton?.let { perButtonColorOverrides[it]?.let(::Color) }
                        ?: controlColor
                    ControlColorSwatches.forEach { swatch ->
                        val isSelected = swatch.toArgb() == editingColor.toArgb()
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) overlayAccent
                                    else Color.White.copy(alpha = 0.45f),
                                    shape = CircleShape,
                                )
                                .clickable(
                                    interactionSource = remember(swatch) { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (selectedButton != null) {
                                        perButtonColorOverrides[selectedButton] = swatch.toArgb()
                                        ControlPrefs.setVirtualButtonColorOverrides(
                                            context,
                                            perButtonColorOverrides.toMap(),
                                        )
                                    } else {
                                        controlColor = swatch
                                        ControlPrefs.setVirtualButtonColor(context, swatch.toArgb())
                                    }
                                },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedButton != null) {
                        IconButton(
                            onClick = { pngPicker.launch(arrayOf("image/png")) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(overlayAccent),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = uiText.text("Importar PNG"),
                                tint = overlayAccentOn,
                            )
                        }

                        IconButton(
                            onClick = {
                                ControlPrefs.clearVirtualButtonImageOverride(context, selectedButton)
                                perButtonImageOverrides.remove(selectedButton)
                                Toast.makeText(context, uiText.text("PNG original restaurado"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(overlayAccent),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ImageNotSupported,
                                contentDescription = uiText.text("Restaurar PNG original"),
                                tint = overlayAccentOn,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (selectedButton != null) {
                                specs.firstOrNull { it.id == selectedButton }?.let {
                                    positions[selectedButton] = it.defaultX to it.defaultY
                                }
                                perButtonScaleOverrides.remove(selectedButton)
                                perButtonOpacityOverrides.remove(selectedButton)
                                perButtonColorOverrides.remove(selectedButton)
                                ControlPrefs.setVirtualButtonPositions(context, positions.toMap())
                                ControlPrefs.setVirtualButtonScaleOverrides(
                                    context,
                                    perButtonScaleOverrides.toMap(),
                                )
                                ControlPrefs.setVirtualButtonOpacityOverrides(
                                    context,
                                    perButtonOpacityOverrides.toMap(),
                                )
                                ControlPrefs.setVirtualButtonColorOverrides(
                                    context,
                                    perButtonColorOverrides.toMap(),
                                )
                                Toast.makeText(context, uiText.text("Botão restaurado"), Toast.LENGTH_SHORT).show()
                            } else {
                                specs.forEach { positions[it.id] = it.defaultX to it.defaultY }
                                buttonScale = 1f
                                buttonOpacity = 0.5f
                                perButtonScaleOverrides.clear()
                                perButtonOpacityOverrides.clear()
                                perButtonColorOverrides.clear()
                                selectedButtonId = null
                                controlColor = Color(ControlPrefs.DEFAULT_VB_COLOR)
                                ControlPrefs.setVirtualButtonPositions(context, positions.toMap())
                                ControlPrefs.setVirtualButtonScale(context, buttonScale)
                                ControlPrefs.setVirtualButtonOpacity(context, buttonOpacity)
                                ControlPrefs.setVirtualButtonScaleOverrides(context, emptyMap())
                                ControlPrefs.setVirtualButtonOpacityOverrides(context, emptyMap())
                                ControlPrefs.setVirtualButtonColorOverrides(context, emptyMap())
                                ControlPrefs.setVirtualButtonColor(context, ControlPrefs.DEFAULT_VB_COLOR)
                                Toast.makeText(context, uiText.text("Layout restaurado"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(overlayAccent),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = uiText.restoreDefault,
                            tint = overlayAccentOn,
                        )
                    }

                    IconButton(
                        onClick = {
                            ControlPrefs.setVirtualButtonPositions(context, positions.toMap())
                            ControlPrefs.setVirtualButtonScale(context, buttonScale)
                            ControlPrefs.setVirtualButtonOpacity(context, buttonOpacity)
                            ControlPrefs.setVirtualButtonScaleOverrides(context, perButtonScaleOverrides.toMap())
                            ControlPrefs.setVirtualButtonOpacityOverrides(context, perButtonOpacityOverrides.toMap())
                            ControlPrefs.setVirtualButtonColor(context, controlColor.toArgb())
                            ControlPrefs.setVirtualButtonColorOverrides(context, perButtonColorOverrides.toMap())
                            // Keep the editor open after saving; just confirm with a popup so the user
                            // can keep tweaking the layout without re-entering the screen.
                            Toast.makeText(context, uiText.text("Layout dos controles salvo"), Toast.LENGTH_SHORT)
                                .show()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(overlayAccent),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = uiText.text("Salvar"),
                            tint = overlayAccentOn,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !menuExpanded,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.80f),
            exit = fadeOut(tween(90)) + scaleOut(tween(90), targetScale = 0.80f),
            modifier = Modifier.offset {
                IntOffset(initialMiniOffset.x.roundToInt(), initialMiniOffset.y.roundToInt())
            },
        ) {
            Box(
                modifier = Modifier
                    .size(width = panelMiniWidthDp, height = 50.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .clickable {
                        menuOffset = initialPanelOffset
                        animateMenuOffset = true
                        menuExpanded = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = uiText.text("Abrir menu"),
                    tint = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun OverlayCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
        )
    }
}

@Composable
private fun OverlaySliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.width(72.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps.coerceAtLeast(0),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun VirtualButton(
    spec: VirtualButtonSpec,
    alpha: Float,
    tint: ColorFilter,
    customImagePath: String?,
) {
    rememberCustomButtonBitmap(customImagePath)?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = spec.id,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().alpha(alpha),
            colorFilter = tint,
        )
        return
    }
    when (spec.type) {
        VirtualButtonType.SHOULDER -> ShoulderButton(id = spec.id, label = spec.label, alpha = alpha, tint = tint)
        VirtualButtonType.DPAD -> DPadButton(id = spec.id, alpha = alpha, tint = tint)
        VirtualButtonType.FACE -> FaceButton(id = spec.id, label = spec.label, alpha = alpha, tint = tint)
        VirtualButtonType.SYSTEM -> SystemButton(id = spec.id, label = spec.label, alpha = alpha, tint = tint)
        VirtualButtonType.STICK -> StickButton(id = spec.id, label = spec.label, alpha = alpha, tint = tint)
        VirtualButtonType.AUX -> AuxButton(id = spec.id, alpha = alpha, tint = tint)
    }
}

@Composable
private fun rememberCustomButtonBitmap(path: String?) = remember(path) {
    if (path.isNullOrBlank()) {
        null
    } else {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }
}

@Composable
private fun ShoulderButton(
    id: String,
    label: String,
    alpha: Float,
    tint: ColorFilter,
) {
    val drawableId = virtualControlDrawableId(id)
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = id,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().alpha(alpha),
            colorFilter = tint,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF2F2F2).copy(alpha = alpha * 0.62f))
                .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = Color(0xFF454B56),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DPadButton(
    id: String,
    alpha: Float,
    tint: ColorFilter,
) {
    val drawableId = virtualControlDrawableId(id) ?: return
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = id,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().alpha(alpha),
        colorFilter = tint,
    )
}

@Composable
private fun FaceButton(
    id: String,
    label: String,
    alpha: Float,
    tint: ColorFilter,
) {
    val drawableId = virtualControlDrawableId(id)
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = id,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().alpha(alpha),
            colorFilter = tint,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFF2F2F2).copy(alpha = alpha * 0.66f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = Color(0xFF454B56),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SystemButton(
    id: String,
    label: String,
    alpha: Float,
    tint: ColorFilter,
) {
    val drawableId = virtualControlDrawableId(id)
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = id,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().alpha(alpha),
            colorFilter = tint,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFF2F2F2).copy(alpha = alpha * 0.58f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = Color(0xFF454B56),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StickButton(
    id: String,
    label: String,
    alpha: Float,
    tint: ColorFilter,
) {
    val (bgDrawableId, stickDrawableId) = virtualStickDrawableIds(id)
    BoxWithConstraints(modifier = Modifier.fillMaxSize().alpha(alpha), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val sideDp = with(density) { min(maxWidth.toPx(), maxHeight.toPx()).toDp() }
        Image(
            painter = painterResource(id = bgDrawableId),
            contentDescription = "${id}_bg",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(sideDp),
            colorFilter = tint,
        )
        Image(
            painter = painterResource(id = stickDrawableId),
            contentDescription = id,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(sideDp * 0.60f)
                .offset {
                    if (id == "right_stick") {
                        val sidePx = with(density) { sideDp.toPx() }
                        IntOffset((-sidePx * 0.030f).roundToInt(), 0)
                    } else {
                        IntOffset.Zero
                    }
                },
            colorFilter = tint,
        )
    }
}

@Composable
private fun AuxButton(
    id: String,
    alpha: Float,
    tint: ColorFilter,
) {
    val drawableId = virtualControlDrawableId(id)
    if (drawableId != null) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = id,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().alpha(alpha),
            colorFilter = tint,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .border(2.dp, Color(0xFFC8F3AE).copy(alpha = alpha * 0.88f), RoundedCornerShape(3.dp)),
        )
    }
}

// Preset palette for the on-screen control color picker. White is first (the default); the rest span
// the hue wheel plus black, so the user can match the controls to any game. A SrcIn tint recolors the
// (light) button art to the chosen color, so every swatch reads true.
private val ControlColorSwatches: List<Color> = listOf(
    Color(0xFFFFFFFF), Color(0xFFFF5252), Color(0xFFFF7043), Color(0xFFFFB300),
    Color(0xFFFFEB3B), Color(0xFF9CCC65), Color(0xFF4CAF50), Color(0xFF26C6DA),
    Color(0xFF00B0FF), Color(0xFF2962FF), Color(0xFF7C4DFF), Color(0xFFE040FB),
    Color(0xFFFF4081), Color(0xFF8D6E63), Color(0xFF90A4AE), Color(0xFF000000),
)

private fun virtualControlDrawableId(id: String): Int? = when (id) {
    "dpad_up" -> R.drawable.dpad_top
    "dpad_left" -> R.drawable.dpad_left
    "dpad_right" -> R.drawable.dpad_right
    "dpad_down" -> R.drawable.dpad_bottom
    "triangle" -> R.drawable.triangle
    "square" -> R.drawable.square
    "circle" -> R.drawable.circle
    // start/cross art swapped (user request), matching the in-game GameTouchControls mapping.
    "cross" -> R.drawable.start
    "l1" -> R.drawable.l1
    "l2" -> R.drawable.l2
    "r1" -> R.drawable.r1
    "r2" -> R.drawable.r2
    "select" -> R.drawable.select
    "ps" -> R.drawable.ps
    "start" -> R.drawable.cross
    "left_stick_click" -> R.drawable.l3
    "right_stick_click" -> R.drawable.r3
    else -> null
}

private fun virtualStickDrawableIds(id: String): Pair<Int, Int> = when (id) {
    "left_stick" -> R.drawable.left_stick_background to R.drawable.left_stick
    "right_stick" -> R.drawable.right_stick_background to R.drawable.right_stick
    else -> R.drawable.left_stick_background to R.drawable.left_stick
}

@Composable
private fun ForceLandscapeOrientation() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            // Bug fix: this used to remap UNSPECIFIED (the app's normal "let it rotate freely"
            // state) to PORTRAIT, which left the whole app force-locked to portrait after
            // leaving this screen even with the device in landscape. Just restore whatever the
            // orientation actually was.
            activity.requestedOrientation = previous
        }
    }
}
