// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

// Shared counter of currently-open dialogs/bottom sheets. The root app content applies a light blur
// while this is > 0, so whatever is visible behind a dialog/sheet is softly blurred (the dialog/sheet
// draws in a separate window above the activity, so blurring the root content shows through its scrim).
// `OverlayBlurEffect()` is dropped into each dialog/sheet's content to bump the counter for its
// lifetime. Kept as a top-level object so screens in other files (e.g. ControllerConfigScreen) share it.
object OverlayBlur {
    var openCount by mutableIntStateOf(0)
        private set

    fun acquire() { openCount++ }
    fun release() { if (openCount > 0) openCount-- }
}

// Place inside a dialog/sheet content lambda: increments the overlay counter while composed AND
// removes the host window's dim (the dark translucent scrim drawn behind a dialog). We blur the
// content behind instead, so the extra dark layer is unwanted. ModalBottomSheet draws its own scrim
// via `scrimColor` (pass Color.Transparent at the call site) rather than the window dim.
@Composable
fun OverlayBlurEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        OverlayBlur.acquire()
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.setDimAmount(0f)
        onDispose { OverlayBlur.release() }
    }
}
