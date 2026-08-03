// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context

// Persisted list of GitHub "releases" URLs the GPU driver downloader pulls from. Pure Android-side
// UI state (which channels the user wants to check for drivers), not a native emulator setting.
// Ships with two default channels; the user can add more (or, in principle, remove the defaults --
// nothing hardcodes them as un-removable beyond seeding them on first run).
object DriverChannelPrefs {
    private const val PREFS = "driver_channels"
    private const val KEY_CHANNELS = "urls"

    val defaultChannels = listOf(
        "https://github.com/K11MCH1/AdrenoToolsDrivers/releases",
        "https://github.com/MrPurple666/purple-turnip/releases",
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getChannels(context: Context): List<String> {
        val p = prefs(context)
        if (!p.contains(KEY_CHANNELS)) {
            // First run: seed with the defaults so they show up as real, removable/editable
            // channels rather than being specially hardcoded in the UI.
            p.edit().putStringSet(KEY_CHANNELS, defaultChannels.toSet()).apply()
            return defaultChannels
        }
        return p.getStringSet(KEY_CHANNELS, emptySet())?.toList().orEmpty()
    }

    fun addChannel(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val current = getChannels(context).toMutableSet()
        current.add(trimmed)
        prefs(context).edit().putStringSet(KEY_CHANNELS, current).apply()
    }

    fun removeChannel(context: Context, url: String) {
        val current = getChannels(context).toMutableSet()
        current.remove(url)
        prefs(context).edit().putStringSet(KEY_CHANNELS, current).apply()
    }
}
