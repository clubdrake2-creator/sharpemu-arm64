// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context

// Persistent per-game "last launched" timestamp (epoch millis). Pure Android-side UI state (drives the
// relative "played X ago" label in the game info sheet), so it lives in SharedPreferences rather than
// the native emulator config -- the native side has no concept of "when was this game last launched".
object LastPlayedPrefs {
    private const val PREFS = "last_played"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Records "now" as the last-played time for a game. Called right before the game activity starts.
    fun markPlayedNow(context: Context, gameId: String) {
        if (gameId.isBlank()) return
        prefs(context).edit().putLong(gameId, System.currentTimeMillis()).apply()
    }

    // 0 means the game has never been launched from this library.
    fun getLastPlayed(context: Context, gameId: String): Long =
        if (gameId.isBlank()) 0L else prefs(context).getLong(gameId, 0L)
}
