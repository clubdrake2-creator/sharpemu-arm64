// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android

import android.util.Log

// Bridge to the on-screen touch controls' native virtual gamepad. Button/axis constants mirror
// SDL_GamepadButton / SDL_GamepadAxis ordering.
object GamePadBridge {
    private const val TAG = "GamePadBridge"

    // SDL_GamepadButton
    const val BUTTON_SOUTH = 0   // Cross
    const val BUTTON_EAST = 1    // Circle
    const val BUTTON_WEST = 2    // Square
    const val BUTTON_NORTH = 3   // Triangle
    const val BUTTON_BACK = 4    // Share / Select
    const val BUTTON_GUIDE = 5   // PS button
    const val BUTTON_START = 6   // Options / Start
    const val BUTTON_LEFT_STICK = 7
    const val BUTTON_RIGHT_STICK = 8
    const val BUTTON_LEFT_SHOULDER = 9   // L1
    const val BUTTON_RIGHT_SHOULDER = 10 // R1
    const val BUTTON_DPAD_UP = 11
    const val BUTTON_DPAD_DOWN = 12
    const val BUTTON_DPAD_LEFT = 13
    const val BUTTON_DPAD_RIGHT = 14

    // SDL_GamepadAxis
    const val AXIS_LEFT_X = 0
    const val AXIS_LEFT_Y = 1
    const val AXIS_RIGHT_X = 2
    const val AXIS_RIGHT_Y = 3
    const val AXIS_LEFT_TRIGGER = 4  // L2
    const val AXIS_RIGHT_TRIGGER = 5 // R2

    init {
        runCatching { System.loadLibrary("emucore") }
            .onFailure { Log.e(TAG, "Failed to load libemucore for virtual gamepad", it) }
    }

    external fun nativeSetButton(button: Int, pressed: Boolean)
    external fun nativeSetAxis(axis: Int, value: Int)
    external fun nativeRequestRenderDiagCapture()

    fun setButton(button: Int, pressed: Boolean) {
        runCatching { nativeSetButton(button, pressed) }
            .onFailure { Log.e(TAG, "nativeSetButton failed button=$button pressed=$pressed", it) }
    }

    fun setAxis(axis: Int, value: Int) {
        runCatching { nativeSetAxis(axis, value) }
            .onFailure { Log.e(TAG, "nativeSetAxis failed axis=$axis value=$value", it) }
    }

    // Triggers the same render-diagnostics capture as the desktop keyboard shortcut: a
    // screenshot (or RenderDoc capture if loaded) plus a short PM4 packet trace, used to report
    // rendering bugs. Shown as an on-screen button only when render_diag_capture_enabled is on.
    fun requestRenderDiagCapture() {
        runCatching { nativeRequestRenderDiagCapture() }
            .onFailure { Log.e(TAG, "nativeRequestRenderDiagCapture failed", it) }
    }
}
