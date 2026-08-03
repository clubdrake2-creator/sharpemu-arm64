// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.
//
// In-game on-screen gamepad, ported verbatim from PCSX2-Android's TouchControlsOverlay (its
// EmulationScreen.kt) and adapted for shadPS4: it reads the SAME layout the virtual-control editor
// saves (ControlPrefs positions/scale/opacity/overrides) and uses the identical TouchSpecs +
// baseControlScale, so the in-game buttons match the edited layout exactly. Input is dispatched
// through shadPS4's existing virtual-gamepad bridge (PadInput -> GamePadBridge) instead of PCSX2's
// native pad, so every press reaches the emulator.

package org.sharpemu.android.overlay

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.sharpemu.android.R
import org.sharpemu.android.data.ControlPrefs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class TouchControlType { SHOULDER, DPAD, FACE, SYSTEM, STICK, AUX }

private data class TouchControlSpec(
    val id: String,
    val label: String,
    val type: TouchControlType,
    val defaultX: Float,
    val defaultY: Float,
    val widthDp: Float,
    val heightDp: Float,
)

// Identical to the editor's spec list so the in-game layout matches the edited layout exactly.
private val TouchSpecs = listOf(
    TouchControlSpec("l2", "L2", TouchControlType.SHOULDER, 0.09584009f, 0.13170458f, 138f, 72f),
    TouchControlSpec("l1", "L1", TouchControlType.SHOULDER, 0.10f, 0.26f, 138f, 72f),
    TouchControlSpec("r2", "R2", TouchControlType.SHOULDER, 0.89507091f, 0.14186218f, 138f, 72f),
    TouchControlSpec("r1", "R1", TouchControlType.SHOULDER, 0.89798176f, 0.25400078f, 138f, 72f),
    TouchControlSpec("dpad_up", "â–²", TouchControlType.DPAD, 0.11f, 0.49f, 80f, 100f),
    TouchControlSpec("dpad_left", "â—€", TouchControlType.DPAD, 0.063f, 0.57f, 100f, 80f),
    TouchControlSpec("dpad_right", "â–¶", TouchControlType.DPAD, 0.157f, 0.57f, 100f, 80f),
    TouchControlSpec("dpad_down", "â–¼", TouchControlType.DPAD, 0.11f, 0.65f, 80f, 100f),
    TouchControlSpec("left_stick", "L3", TouchControlType.STICK, 0.24f, 0.78f, 98f, 98f),
    TouchControlSpec("right_stick", "R3", TouchControlType.STICK, 0.90977818f, 0.55615997f, 98f, 98f),
    TouchControlSpec("triangle", "â–³", TouchControlType.FACE, 0.76f, 0.66f, 72f, 72f),
    TouchControlSpec("square", "â–¡", TouchControlType.FACE, 0.71f, 0.77f, 72f, 72f),
    TouchControlSpec("circle", "â—‹", TouchControlType.FACE, 0.81f, 0.77f, 72f, 72f),
    TouchControlSpec("cross", "âœ•", TouchControlType.FACE, 0.57019383f, 0.87845618f, 72f, 72f),
    TouchControlSpec("select", "-", TouchControlType.SYSTEM, 0.43f, 0.88f, 74f, 74f),
    TouchControlSpec("ps", "âŒ‚", TouchControlType.SYSTEM, 0.50f, 0.88f, 74f, 74f),
    TouchControlSpec("start", "+", TouchControlType.SYSTEM, 0.76420736f, 0.89215344f, 74f, 74f),
)

// Maps a button id to (digital word index, flag bit) in shadPS4's pad State.
private val ButtonBits: Map<String, Pair<Int, Int>> = mapOf(
    "dpad_up" to (0 to Digital1Flags.CELL_PAD_CTRL_UP.bit),
    "dpad_down" to (0 to Digital1Flags.CELL_PAD_CTRL_DOWN.bit),
    "dpad_left" to (0 to Digital1Flags.CELL_PAD_CTRL_LEFT.bit),
    "dpad_right" to (0 to Digital1Flags.CELL_PAD_CTRL_RIGHT.bit),
    "select" to (0 to Digital1Flags.CELL_PAD_CTRL_SELECT.bit),
    // start/cross click functions intentionally swapped (user request) â€” kept in sync with the
    // swapped art in touchControlDrawableId: the "start" slot fires Cross, the "cross" slot fires Start.
    "start" to (1 to Digital2Flags.CELL_PAD_CTRL_CROSS.bit),
    "ps" to (0 to Digital1Flags.CELL_PAD_CTRL_PS.bit),
    "left_stick_click" to (0 to Digital1Flags.CELL_PAD_CTRL_L3.bit),
    "right_stick_click" to (0 to Digital1Flags.CELL_PAD_CTRL_R3.bit),
    "triangle" to (1 to Digital2Flags.CELL_PAD_CTRL_TRIANGLE.bit),
    "circle" to (1 to Digital2Flags.CELL_PAD_CTRL_CIRCLE.bit),
    "cross" to (0 to Digital1Flags.CELL_PAD_CTRL_START.bit),
    "square" to (1 to Digital2Flags.CELL_PAD_CTRL_SQUARE.bit),
    "l1" to (1 to Digital2Flags.CELL_PAD_CTRL_L1.bit),
    "l2" to (1 to Digital2Flags.CELL_PAD_CTRL_L2.bit),
    "r1" to (1 to Digital2Flags.CELL_PAD_CTRL_R1.bit),
    "r2" to (1 to Digital2Flags.CELL_PAD_CTRL_R2.bit),
)

@Composable
fun GameTouchControls(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val savedPositions = remember { ControlPrefs.getVirtualButtonPositions(context) }
    val scale = remember { ControlPrefs.getVirtualButtonScale(context) }
    val opacity = remember { ControlPrefs.getVirtualButtonOpacity(context) }
    val scaleOverrides = remember { ControlPrefs.getVirtualButtonScaleOverrides(context) }
    val opacityOverrides = remember { ControlPrefs.getVirtualButtonOpacityOverrides(context) }
    val colorOverrides = remember { ControlPrefs.getVirtualButtonColorOverrides(context) }
    val imageOverrides = remember { ControlPrefs.getVirtualButtonImageOverrides(context) }
    val centerAnalog = remember { ControlPrefs.getCenterAnalogOnTouch(context) }
    // Fixed, user-chosen control color (default white). Replaces the old adaptive luminance tint: every
    // button is recolored to this single color via a SrcIn tint, so the controls keep a consistent
    // look regardless of the background. Picked in the layout editor's color picker.
    val controlColor = remember { Color(ControlPrefs.getVirtualButtonColor(context)) }

    // Shared pad state pushed to the SDL virtual gamepad after every change.
    val padState = remember { State() }
    fun setBit(word: Int, flag: Int, on: Boolean) {
        padState.digital[word] =
            if (on) padState.digital[word] or flag else padState.digital[word] and flag.inv()
    }
    fun onButtonChanged(id: String, pressed: Boolean) {
        val wf = ButtonBits[id] ?: return
        setBit(wf.first, wf.second, pressed)
        PadInput.send(padState)
    }
    fun onStick(left: Boolean, axis: Offset) {
        val x = (128 + axis.x * 127f).roundToInt().coerceIn(0, 255)
        val y = (128 + axis.y * 127f).roundToInt().coerceIn(0, 255)
        if (left) {
            padState.leftStickX = x; padState.leftStickY = y
        } else {
            padState.rightStickX = x; padState.rightStickY = y
        }
        PadInput.send(padState)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        TouchSpecs.forEach { spec ->
            val position = savedPositions[spec.id] ?: (spec.defaultX to spec.defaultY)
            val normX = position.first.coerceIn(0f, 1f)
            val normY = position.second.coerceIn(0f, 1f)
            val baseControlScale = when {
                spec.id == "cross" -> 0.50f
                spec.id == "select" -> 0.50f
                spec.id == "start" -> 0.82f
                spec.type == TouchControlType.STICK -> 1.2f
                spec.type == TouchControlType.FACE -> 0.82f
                spec.type == TouchControlType.DPAD -> 0.782f
                spec.type == TouchControlType.SHOULDER -> 1.120f
                else -> 0.72f
            }
            val perButtonScale = scaleOverrides[spec.id]?.coerceIn(0.35f, 2.0f) ?: 1.0f
            val perButtonOpacity = opacityOverrides[spec.id]?.coerceIn(0.20f, 2.0f) ?: 1.0f
            val controlScale = baseControlScale * perButtonScale
            val widthDp = (spec.widthDp * scale * controlScale).dp
            val heightDp = (spec.heightDp * scale * controlScale).dp
            val widthControlPx = with(density) { widthDp.toPx() }
            val heightControlPx = with(density) { heightDp.toPx() }
            val fineTuneOffsetX = with(density) {
                when (spec.id) {
                    "right_stick" -> -4.dp.toPx()
                    else -> 0f
                }
            }
            val fineTuneOffsetY = 0f
            val offsetX = (widthPx * normX - widthControlPx / 2f + fineTuneOffsetX).roundToInt()
            val offsetY = (heightPx * normY - heightControlPx / 2f + fineTuneOffsetY).roundToInt()
            val resolvedAlpha = (opacity * perButtonOpacity).coerceIn(0.05f, 1.0f)

            // Fixed user color for every control (default white). The adaptive luminance tint was
            // removed; controls now render in the single color picked in the layout editor.
            val resolvedColor = colorOverrides[spec.id]?.let { Color(it) } ?: controlColor
            val adaptiveTint: ColorFilter? = remember(resolvedColor) { controlColorFilter(resolvedColor) }
            val customImagePath = imageOverrides[spec.id]

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .size(widthDp, heightDp),
                contentAlignment = Alignment.Center,
            ) {
                when (spec.type) {
                    TouchControlType.STICK -> {
                        val left = spec.id == "left_stick"
                        TouchStickControl(
                            id = spec.id,
                            label = spec.label,
                            alpha = resolvedAlpha,
                            centerOnTouch = centerAnalog,
                            tint = adaptiveTint,
                            customImagePath = customImagePath,
                            onStickChanged = { onStick(left, it) },
                            onStickClickChanged = { pressed ->
                                onButtonChanged(if (left) "left_stick_click" else "right_stick_click", pressed)
                            },
                        )
                    }
                    else -> TouchButtonControl(
                        spec = spec,
                        alpha = resolvedAlpha,
                        tint = adaptiveTint,
                        customImagePath = customImagePath,
                        onPressedChanged = { onButtonChanged(spec.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TouchButtonControl(
    spec: TouchControlSpec,
    alpha: Float,
    tint: ColorFilter?,
    customImagePath: String?,
    onPressedChanged: (Boolean) -> Unit,
) {
    var pressed by remember(spec.id) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(spec.id) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPressedChanged(true)
                        tryAwaitRelease()
                        pressed = false
                        onPressedChanged(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        TouchControlVisual(
            spec = spec,
            alpha = alpha,
            pressed = pressed,
            knob = Offset.Zero,
            tint = tint,
            customImagePath = customImagePath,
        )
    }
}

@Composable
private fun TouchStickControl(
    id: String,
    label: String,
    alpha: Float,
    centerOnTouch: Boolean,
    tint: ColorFilter?,
    customImagePath: String?,
    onStickChanged: (Offset) -> Unit,
    onStickClickChanged: (Boolean) -> Unit,
) {
    var axis by remember(id) { mutableStateOf(Offset.Zero) }
    var sizePx by remember(id) { mutableStateOf(IntSize.Zero) }
    var centerShift by remember(id) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(id) { mutableStateOf(false) }
    var isPressing by remember(id) { mutableStateOf(false) }
    var longPressActive by remember(id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun resetStick() {
        isDragging = false
        isPressing = false
        axis = Offset.Zero
        if (centerOnTouch) centerShift = Offset.Zero
        if (longPressActive) {
            onStickClickChanged(false)
            longPressActive = false
        }
        onStickChanged(Offset.Zero)
    }

    fun updateStick(position: Offset) {
        val width = sizePx.width.toFloat().coerceAtLeast(1f)
        val height = sizePx.height.toFloat().coerceAtLeast(1f)
        val radius = (min(width, height) * 0.40f).coerceAtLeast(1f)
        if (centerOnTouch && centerShift == Offset.Zero) {
            val center = Offset(width / 2f, height / 2f)
            val maxShift = min(width, height) * 0.36f
            val rawShift = position - center
            centerShift = Offset(
                rawShift.x.coerceIn(-maxShift, maxShift),
                rawShift.y.coerceIn(-maxShift, maxShift),
            )
        }
        val center = Offset(width / 2f, height / 2f) + centerShift
        var dx = (position.x - center.x) / radius
        var dy = (position.y - center.y) / radius
        val len = hypot(dx, dy)
        if (len > 1f) {
            dx /= len
            dy /= len
        }
        axis = Offset(dx.coerceIn(-1f, 1f), dy.coerceIn(-1f, 1f))
        onStickChanged(axis)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(id, centerOnTouch) {
                detectDragGestures(
                    onDragStart = { position ->
                        if (longPressActive) {
                            onStickClickChanged(false)
                            longPressActive = false
                        }
                        isDragging = true
                        isPressing = true
                        updateStick(position)
                    },
                    onDrag = { change, _ ->
                        updateStick(change.position)
                        change.consume()
                    },
                    onDragEnd = { resetStick() },
                    onDragCancel = { resetStick() },
                )
            }
            .pointerInput(id) {
                detectTapGestures(
                    onTap = {
                        scope.launch {
                            onStickClickChanged(true)
                            delay(60)
                            onStickClickChanged(false)
                        }
                    },
                    onLongPress = {
                        if (!isDragging && !longPressActive) {
                            longPressActive = true
                            onStickClickChanged(true)
                        }
                    },
                    onPress = {
                        isPressing = true
                        val released = tryAwaitRelease()
                        isPressing = false
                        if (longPressActive) {
                            onStickClickChanged(false)
                            longPressActive = false
                        } else if (!released) {
                            onStickClickChanged(false)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        TouchControlVisual(
            spec = TouchControlSpec(id, label, TouchControlType.STICK, 0f, 0f, 100f, 100f),
            alpha = alpha,
            pressed = isDragging || isPressing || longPressActive,
            knob = axis,
            hideStickOuter = isDragging,
            onSizeChanged = { sizePx = it },
            tint = tint,
            customImagePath = customImagePath,
        )
    }
}

@Composable
private fun TouchControlVisual(
    spec: TouchControlSpec,
    alpha: Float,
    pressed: Boolean,
    knob: Offset,
    hideStickOuter: Boolean = false,
    onSizeChanged: (IntSize) -> Unit = {},
    tint: ColorFilter? = null,
    customImagePath: String? = null,
) {
    // The opacity slider is the single source of truth for control opacity, so at its maximum the
    // buttons render fully solid (100% opaque). Press feedback is a subtle shrink instead of an alpha
    // dip, which would otherwise stop the controls from ever reaching full solidity.
    val drawableAlpha = alpha
    val pressScale = if (pressed) 0.90f else 1.0f
    rememberCustomButtonBitmap(customImagePath)?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = spec.id,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().scale(pressScale).alpha(drawableAlpha),
            colorFilter = tint,
        )
        return
    }
    when (spec.type) {
        TouchControlType.SHOULDER,
        TouchControlType.DPAD,
        TouchControlType.FACE,
        TouchControlType.SYSTEM,
        TouchControlType.AUX -> {
            val drawableId = touchControlDrawableId(spec.id)
            if (drawableId != null) {
                Image(
                    painter = painterResource(id = drawableId),
                    contentDescription = spec.id,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().scale(pressScale).alpha(drawableAlpha),
                    colorFilter = tint,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pressScale)
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F2).copy(alpha = drawableAlpha))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = spec.label,
                        color = Color(0xFF454B56),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        TouchControlType.STICK -> {
            val density = LocalDensity.current
            val (bgDrawableId, stickDrawableId) = touchStickDrawableIds(spec.id)
            val knobX = knob.x.coerceIn(-1f, 1f)
            val knobY = knob.y.coerceIn(-1f, 1f)
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().alpha(drawableAlpha),
                contentAlignment = Alignment.Center,
            ) {
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                onSizeChanged(IntSize(widthPx.roundToInt(), heightPx.roundToInt()))
                // The knob keeps a CONSTANT size whether idle or dragging (it never shrinks). The
                // outer ring is shown only when NOT dragging â€” while the user moves the stick the ring
                // disappears and just the (full-size) knob slides with the axis.
                val knobFraction = 0.60f
                val minSide = min(widthPx, heightPx)
                val knobSizePx = minSide * knobFraction
                val moveRadius = ((minSide * 0.5f) - (knobSizePx * 0.5f)).coerceAtLeast(0f)
                val idleBiasX = if (spec.id == "right_stick" && knobX == 0f && knobY == 0f) {
                    -minSide * 0.030f
                } else {
                    0f
                }
                val idleBiasY = if (spec.id == "right_stick" && knobX == 0f && knobY == 0f) {
                    0f
                } else {
                    0f
                }
                if (!hideStickOuter) {
                    Image(
                        painter = painterResource(id = bgDrawableId),
                        contentDescription = "${spec.id}_bg",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = tint,
                    )
                }
                Image(
                    painter = painterResource(id = stickDrawableId),
                    contentDescription = spec.id,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize(knobFraction)
                        .offset {
                            IntOffset(
                                (knobX * moveRadius + idleBiasX).roundToInt(),
                                (knobY * moveRadius + idleBiasY).roundToInt(),
                            )
                        },
                    colorFilter = tint,
                )
            }
        }
    }
}

private fun touchControlDrawableId(id: String): Int? = when (id) {
    "dpad_up" -> R.drawable.dpad_top
    "dpad_left" -> R.drawable.dpad_left
    "dpad_right" -> R.drawable.dpad_right
    "dpad_down" -> R.drawable.dpad_bottom
    "triangle" -> R.drawable.triangle
    "square" -> R.drawable.square
    "circle" -> R.drawable.circle
    // start/cross intentionally swapped (user request): the cross slot shows the start art and vice
    // versa, kept in sync with the swapped click mapping in ButtonBits.
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

private fun touchStickDrawableIds(id: String): Pair<Int, Int> = when (id) {
    "left_stick" -> R.drawable.left_stick_background to R.drawable.left_stick
    "right_stick" -> R.drawable.right_stick_background to R.drawable.right_stick
    else -> R.drawable.left_stick_background to R.drawable.left_stick
}

@Composable
private fun rememberCustomButtonBitmap(path: String?) = remember(path) {
    if (path.isNullOrBlank()) {
        null
    } else {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }
}

// Builds a color filter that tints a (grayscale) control so it contrasts the background luminance
// behind it: a bright background keeps the art as-is (it is already dark), a dark background inverts
// the art (light body, dark symbol) so it stays visible. Full contrast is preserved at both ends;
// only a very narrow mid-gray band (~0.47..0.53) crossfades through low contrast. Returns null (no
// filter -> original art) for a bright background or before any frame has been sampled.
private fun adaptiveControlColorFilter(luminance: Float?): ColorFilter? {
    if (luminance == null) return null
    // t = invert amount: 1 for a dark background (-> light button), 0 for a bright background.
    val t = (((0.5f - luminance) / 0.06f) + 0.5f).coerceIn(0f, 1f)
    if (t <= 0.001f) return null
    val s = 1f - 2f * t // per-channel scale: 1 -> -1
    val o = 255f * t // per-channel offset: 0 -> 255 (full RGB inversion at t=1)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                s, 0f, 0f, 0f, o,
                0f, s, 0f, 0f, o,
                0f, 0f, s, 0f, o,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
