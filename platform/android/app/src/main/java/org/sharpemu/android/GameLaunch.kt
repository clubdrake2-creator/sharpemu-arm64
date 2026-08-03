// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.sharpemu.android

import android.content.Context
import android.content.Intent
import org.sharpemu.android.model.GameEntry

/**
 * Builds the launch [Intent] for the real GameActivity — implemented in C# (SharpEmu.Android's
 * GameActivity.cs, which extends the SDLActivity Java class bundled with SharpEmu's SDL3
 * dependency; SDL3 has to own the Android window itself, so this can't be a Kotlin class the way
 * it used to be — see GameOverlayHost's doc comment). Named by class rather than by
 * `GameActivity::class.java`, since that type now lives in a different module this one cannot
 * depend on (only the app can depend on this library, never the reverse).
 */
object GameLaunch {
    private const val GAME_ACTIVITY_CLASS = "org.sharpemu.android.GameActivity"

    const val EXTRA_GAME_PATH = "org.sharpemu.android.extra.GAME_PATH"
    const val EXTRA_GAME_TITLE = "org.sharpemu.android.extra.GAME_TITLE"
    const val EXTRA_GAME_ID = "org.sharpemu.android.extra.GAME_ID"
    const val EXTRA_APP_ROOT = "org.sharpemu.android.extra.APP_ROOT"
    const val EXTRA_RENDER_DIAG_CAPTURE_ENABLED = "org.sharpemu.android.extra.RENDER_DIAG_CAPTURE_ENABLED"

    fun createIntent(
        context: Context,
        game: GameEntry,
        appRoot: String,
        renderDiagCaptureEnabled: Boolean = false,
    ): Intent =
        Intent().apply {
            setClassName(context.packageName, GAME_ACTIVITY_CLASS)
            // Keep the game Activity in the same Android task as the launcher UI (see
            // AndroidManifest.xml's alwaysRetainTaskState on GameActivity) — a separate
            // taskAffinity/NEW_TASK would make Android Recents show two SharpEmu entries.
            putExtra(EXTRA_GAME_PATH, game.path)
            putExtra(EXTRA_GAME_TITLE, game.title)
            putExtra(EXTRA_GAME_ID, game.id)
            putExtra(EXTRA_APP_ROOT, appRoot)
            putExtra(EXTRA_RENDER_DIAG_CAPTURE_ENABLED, renderDiagCaptureEnabled)
        }
}
