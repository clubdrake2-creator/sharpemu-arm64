// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.overlay

import org.sharpemu.android.GamePadBridge

// On-screen control input model, ported from RPCS3-Android's pad overlay. The overlay widgets
// (PadOverlayButton/Dpad/Stick) accumulate a digital bitmask (two words, matching the PS3 cellpad
// layout RPCS3 uses) plus four analog axes (0..255, 128 = centre). PadInput.send() then translates
// that state into shadPS4's existing SDL virtual gamepad bridge (GamePadBridge), which is already
// wired into the emulator's controller pipeline — so every input reaches the game.

enum class Digital1Flags(val bit: Int) {
    None(0),
    CELL_PAD_CTRL_SELECT(1 shl 0),
    CELL_PAD_CTRL_L3(1 shl 1),
    CELL_PAD_CTRL_R3(1 shl 2),
    CELL_PAD_CTRL_START(1 shl 3),
    CELL_PAD_CTRL_UP(1 shl 4),
    CELL_PAD_CTRL_RIGHT(1 shl 5),
    CELL_PAD_CTRL_DOWN(1 shl 6),
    CELL_PAD_CTRL_LEFT(1 shl 7),
    CELL_PAD_CTRL_PS(1 shl 8),
}

enum class Digital2Flags(val bit: Int) {
    None(0),
    CELL_PAD_CTRL_L2(1 shl 0),
    CELL_PAD_CTRL_R2(1 shl 1),
    CELL_PAD_CTRL_L1(1 shl 2),
    CELL_PAD_CTRL_R1(1 shl 3),
    CELL_PAD_CTRL_TRIANGLE(1 shl 4),
    CELL_PAD_CTRL_CIRCLE(1 shl 5),
    CELL_PAD_CTRL_CROSS(1 shl 6),
    CELL_PAD_CTRL_SQUARE(1 shl 7),
}

// Shared mutable pad state the overlay widgets write into. digital[0]/[1] are the two cellpad words;
// the stick fields are 0..255 with 128 ≈ centre (RPCS3's convention).
data class State(
    val digital: IntArray = IntArray(2),
    var leftStickX: Int = 127,
    var leftStickY: Int = 127,
    var rightStickX: Int = 127,
    var rightStickY: Int = 127,
)

object PadInput {
    // Maps the 0..255 (128 centre) overlay axis to SDL's -32768..32767 range.
    private fun axis(v: Int): Int =
        (((v - 128).toFloat() / 127f) * 32767f).toInt().coerceIn(-32768, 32767)

    private fun has(word: Int, flag: Int) = (word and flag) != 0

    // Pushes the full pad state to the SDL virtual gamepad. Called after every touch event; the
    // GamePadBridge calls are cheap, so sending the whole state each time keeps the code simple and
    // avoids missed transitions.
    fun send(state: State) {
        val d0 = state.digital[0]
        val d1 = state.digital[1]

        GamePadBridge.setButton(GamePadBridge.BUTTON_DPAD_UP, has(d0, Digital1Flags.CELL_PAD_CTRL_UP.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_DPAD_DOWN, has(d0, Digital1Flags.CELL_PAD_CTRL_DOWN.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_DPAD_LEFT, has(d0, Digital1Flags.CELL_PAD_CTRL_LEFT.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_DPAD_RIGHT, has(d0, Digital1Flags.CELL_PAD_CTRL_RIGHT.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_BACK, has(d0, Digital1Flags.CELL_PAD_CTRL_SELECT.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_START, has(d0, Digital1Flags.CELL_PAD_CTRL_START.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_GUIDE, has(d0, Digital1Flags.CELL_PAD_CTRL_PS.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_LEFT_STICK, has(d0, Digital1Flags.CELL_PAD_CTRL_L3.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_RIGHT_STICK, has(d0, Digital1Flags.CELL_PAD_CTRL_R3.bit))

        GamePadBridge.setButton(GamePadBridge.BUTTON_NORTH, has(d1, Digital2Flags.CELL_PAD_CTRL_TRIANGLE.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_EAST, has(d1, Digital2Flags.CELL_PAD_CTRL_CIRCLE.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_SOUTH, has(d1, Digital2Flags.CELL_PAD_CTRL_CROSS.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_WEST, has(d1, Digital2Flags.CELL_PAD_CTRL_SQUARE.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_LEFT_SHOULDER, has(d1, Digital2Flags.CELL_PAD_CTRL_L1.bit))
        GamePadBridge.setButton(GamePadBridge.BUTTON_RIGHT_SHOULDER, has(d1, Digital2Flags.CELL_PAD_CTRL_R1.bit))

        // L2/R2 are digital on the overlay but analog triggers in SDL: pressed = full, released = rest.
        GamePadBridge.setAxis(GamePadBridge.AXIS_LEFT_TRIGGER,
            if (has(d1, Digital2Flags.CELL_PAD_CTRL_L2.bit)) 32767 else -32768)
        GamePadBridge.setAxis(GamePadBridge.AXIS_RIGHT_TRIGGER,
            if (has(d1, Digital2Flags.CELL_PAD_CTRL_R2.bit)) 32767 else -32768)

        GamePadBridge.setAxis(GamePadBridge.AXIS_LEFT_X, axis(state.leftStickX))
        GamePadBridge.setAxis(GamePadBridge.AXIS_LEFT_Y, axis(state.leftStickY))
        GamePadBridge.setAxis(GamePadBridge.AXIS_RIGHT_X, axis(state.rightStickX))
        GamePadBridge.setAxis(GamePadBridge.AXIS_RIGHT_Y, axis(state.rightStickY))
    }
}
