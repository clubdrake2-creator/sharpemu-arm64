// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.sharpemu.android.GamePadBridge

// Floating button shown during gameplay only when the "render_diag_capture_enabled" setting is on
// (GameActivity reads it once at launch and only composes this when true). Tapping it triggers
// the same capture the desktop keyboard shortcut does -- a screenshot (or RenderDoc capture if
// loaded) plus a short PM4 packet trace -- for reporting rendering bugs. Placed top-end so it
// never overlaps the touch-controls visibility toggle, which lives bottom-end.
@Composable
fun RenderDiagCaptureButton() {
    var justTriggered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = {
                    GamePadBridge.requestRenderDiagCapture()
                    justTriggered = true
                    scope.launch {
                        delay(600)
                        justTriggered = false
                    }
                },
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = "Capturar diagnóstico de renderização",
                    tint = if (justTriggered) Color(0xFF4CAF50) else Color.White,
                )
            }
        }
    }
}
