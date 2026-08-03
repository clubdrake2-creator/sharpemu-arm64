// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.overlay

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter

// Recolors an on-screen control's "hollow" art (solid regions + intentionally transparent cut-outs)
// to the user-chosen color while keeping the art's own transparency: a SrcIn tint paints every opaque
// pixel with the chosen color and leaves the cut-outs transparent, so the design is preserved exactly.
//
// Opacity is handled entirely by the per-button `Modifier.alpha()` driven by the opacity slider —
// there is intentionally NO separate dimming for the idle (deactivated) state, so a control always
// renders at the same opacity whether pressed or not. At the slider's maximum it therefore looks like
// the "activated" state (as solid as the source art's solid regions), and gets uniformly translucent
// as the slider is lowered.
fun controlColorFilter(color: Color): ColorFilter = ColorFilter.tint(color)
