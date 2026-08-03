// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context

// Tracks which games' trophy "notifications" (in the dedicated Trophies screen, which behaves
// like a notification area) the user has dismissed or archived. Pure Android-side UI state --
// trophy unlock counts themselves come from the real per-game trophy save (see
// GameTrophySummary), this only tracks what the user has already acknowledged.
object TrophyNotificationPrefs {
    private const val PREFS = "trophy_notifications"
    private const val KEY_ARCHIVED = "archived_ids"
    private const val DISMISSED_COUNT_PREFIX = "dismissed_count_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getArchivedIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ARCHIVED, emptySet())?.toSet() ?: emptySet()

    fun isArchived(context: Context, gameId: String): Boolean =
        gameId in getArchivedIds(context)

    fun archive(context: Context, gameId: String) {
        val current = getArchivedIds(context).toMutableSet()
        current.add(gameId)
        prefs(context).edit().putStringSet(KEY_ARCHIVED, current).apply()
    }

    fun unarchive(context: Context, gameId: String) {
        val current = getArchivedIds(context).toMutableSet()
        current.remove(gameId)
        prefs(context).edit().putStringSet(KEY_ARCHIVED, current).apply()
    }

    // "Apagar": removes the game from the notification list until it earns a NEW trophy (i.e.
    // the unlocked count goes above what it was when dismissed).
    fun dismiss(context: Context, gameId: String, unlockedCountAtDismiss: Int) {
        prefs(context).edit().putInt(DISMISSED_COUNT_PREFIX + gameId, unlockedCountAtDismiss).apply()
    }

    fun getDismissedCount(context: Context, gameId: String): Int =
        prefs(context).getInt(DISMISSED_COUNT_PREFIX + gameId, 0)

    // True when this game should currently show as an active ("unseen") trophy notification:
    // has real unlocked trophies, isn't archived, and its unlocked count is above whatever
    // baseline the user last dismissed it at.
    fun isActiveNotification(context: Context, gameId: String, unlockedCount: Int): Boolean =
        unlockedCount > 0 &&
            !isArchived(context, gameId) &&
            unlockedCount > getDismissedCount(context, gameId)
}
