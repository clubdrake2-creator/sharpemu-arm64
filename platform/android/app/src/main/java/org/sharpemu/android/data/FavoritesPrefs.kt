// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context

// Persistent set of favorited game ids. Pure Android-side UI state (which games the user starred in
// the library), so it lives in SharedPreferences rather than the native emulator config.
object FavoritesPrefs {
    private const val PREFS = "favorites"
    private const val KEY = "ids"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun isFavorite(context: Context, gameId: String): Boolean =
        gameId.isNotBlank() && getFavorites(context).contains(gameId)

    // Toggles the favorite state of a game and returns the new state (true = now favorited).
    fun toggle(context: Context, gameId: String): Boolean {
        if (gameId.isBlank()) return false
        val current = getFavorites(context).toMutableSet()
        val nowFavorite = if (current.contains(gameId)) {
            current.remove(gameId); false
        } else {
            current.add(gameId); true
        }
        prefs(context).edit().putStringSet(KEY, current).apply()
        return nowFavorite
    }
}
