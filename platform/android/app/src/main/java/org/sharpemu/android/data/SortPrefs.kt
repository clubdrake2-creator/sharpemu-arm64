// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context

// How the home screen's game list is ordered (chosen from the "sort by" bottom sheet in Appearance
// settings). Independent of the always-on "last played game jumps to the front" behavior, which layers
// on top of whichever mode is chosen here.
enum class GameSortMode {
    NAME_ASC,
    NAME_DESC,
    RECENTLY_ADDED,
}

object SortPrefs {
    private const val PREFS = "sort"
    private const val KEY_MODE = "mode"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSortMode(context: Context): GameSortMode =
        runCatching {
            GameSortMode.valueOf(prefs(context).getString(KEY_MODE, null) ?: return GameSortMode.NAME_ASC)
        }.getOrDefault(GameSortMode.NAME_ASC)

    fun setSortMode(context: Context, mode: GameSortMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }
}
