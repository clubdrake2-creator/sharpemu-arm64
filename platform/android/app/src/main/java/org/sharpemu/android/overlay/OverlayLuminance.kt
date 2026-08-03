// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.
//
// Adaptive on-screen-control contrast: a low-resolution luminance grid of the game surface, sampled
// periodically (GameActivity PixelCopies the SDL SurfaceView into it). The Compose touch overlay
// (GameTouchControls) reads the luminance under each button's screen region and tints the button to
// contrast it — bright background -> dark button, dark background -> light button — so every control
// stays readable regardless of what is drawn behind it.

package org.sharpemu.android.overlay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

object OverlayLuminance {
    // Grid resolution the game frame is downscaled to. Small = cheap to copy and read every tick;
    // 32x18 keeps the 16:9 aspect and is plenty to localize bright/dark regions per button.
    const val GRID_W = 32
    const val GRID_H = 18

    // Per-cell luminance in 0..1, row-major (GRID_H rows of GRID_W). Null until the first sample.
    // Backed by Compose state so the overlay recomposes (re-tints) whenever a new frame is sampled.
    var grid by mutableStateOf<FloatArray?>(null)
        private set

    // Master switch for the adaptive-contrast feature. Off by default: the on-screen controls now use
    // a fixed, user-chosen color (ControlPrefs.getVirtualButtonColor, default white) instead of
    // tinting against the background luminance.
    var enabled by mutableStateOf(false)

    fun update(values: FloatArray) {
        grid = values
    }

    fun clear() {
        grid = null
    }

    // Average luminance (0..1) of the background under a normalized rect (all args 0..1). Returns
    // null when no frame has been sampled yet, so the caller can fall back to the default art.
    fun luminanceAt(nx: Float, ny: Float, nw: Float, nh: Float): Float? {
        val g = grid ?: return null
        val cx0 = (nx.coerceIn(0f, 1f) * (GRID_W - 1)).toInt()
        val cy0 = (ny.coerceIn(0f, 1f) * (GRID_H - 1)).toInt()
        val cx1 = ((nx + nw).coerceIn(0f, 1f) * (GRID_W - 1)).roundToInt().coerceIn(cx0, GRID_W - 1)
        val cy1 = ((ny + nh).coerceIn(0f, 1f) * (GRID_H - 1)).roundToInt().coerceIn(cy0, GRID_H - 1)
        var sum = 0f
        var n = 0
        for (cy in cy0..cy1) {
            val row = cy * GRID_W
            for (cx in cx0..cx1) {
                sum += g[row + cx]
                n++
            }
        }
        return if (n > 0) sum / n else null
    }
}
