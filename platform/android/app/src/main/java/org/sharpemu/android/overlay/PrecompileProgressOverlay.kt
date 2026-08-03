// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.sharpemu.android.PrecompileBridge
import org.sharpemu.android.ui.theme.AppLanguage
import org.sharpemu.android.ui.theme.appUiText

// Blocking boot-time precompile (rpcs3-style — see Core::Cpu::PrecompileBeforeBoot / linker.cpp)
// runs on the native SDL_main thread before the game's entry point executes; this polls the same
// atomic-int progress the install/delete dialogs use (Core::Cpu::SetPrecompileProgress, -1 = idle,
// 0..100 = percent) so the wait is visible and intentional instead of a silent stutter. Composed
// unconditionally alongside GameTouchControls; hides itself whenever no precompile is running.
@Composable
fun PrecompileProgressOverlay(language: AppLanguage) {
    var progress by remember { mutableStateOf(-1) }
    LaunchedEffect(Unit) {
        while (true) {
            progress = PrecompileBridge.getProgress()
            delay(100)
        }
    }
    val uiText = appUiText(language)
    val visible = progress in 0..99
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
            ) {
                Text(
                    text = uiText.text("Otimizando o jogo..."),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = "${progress.coerceIn(0, 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
