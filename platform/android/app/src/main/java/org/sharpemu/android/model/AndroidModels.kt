// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.model

data class GameEntry(
    val id: String,
    val title: String,
    val path: String,
    val source: String,
    val icon: String = "",
    val version: String = "",
    val sizeBytes: Long = 0L,
)

data class SettingEntry(
    val section: String,
    val key: String,
    val type: String,
    val value: String,
    val locked: Boolean,
    // The setting's built-in default (from the native kSettings table). Used to show a "*" marker
    // when the current value differs from the default.
    val default: String = "",
) {
    // True when the current value differs from the built-in default (shown with a leading "*").
    val isModified: Boolean
        get() = default.isNotEmpty() && value != default
}

data class SettingsSnapshot(
    val root: String,
    val settings: List<SettingEntry>,
)

data class GpuDriverEntry(
    val id: String,
    val title: String,
    val active: Boolean,
    // Relevant info for installed drivers (vendor • version / description from the driver's meta.json).
    val subtitle: String = "",
)

// A GPU (Adreno/Turnip) driver available for download from the AdrenoToolsDrivers GitHub releases.
data class RemoteGpuDriver(
    val name: String,
    val downloadUrl: String,
)

data class PkgInstallResult(
    val ok: Boolean,
    val message: String,
    val path: String,
)

// One key/value from a game's param.sfo (isInt = the value was stored as a 32-bit integer).
data class SfoEntry(
    val key: String,
    val value: String,
    val isInt: Boolean,
)

// Full per-game details for the info sheet: firmware (from SYSTEM_VER), region code (from
// CONTENT_ID), content/title ids, app version and every param.sfo entry.
data class GameDetails(
    val firmware: String,
    val region: String,
    val contentId: String,
    val titleId: String,
    val appVer: String,
    val sfo: List<SfoEntry>,
)

// One trophy parsed from a game's trophy00.trp (icon = absolute path to the extracted PNG, or blank).
// unlocked comes from the real per-game trophy save (<HomeDir>/1000/trophy/<npCommId>.xml), written by
// sceNpTrophyUnlockTrophy as the game actually unlocks trophies during play.
data class TrophyEntry(
    val id: Int,
    val grade: String,
    val hidden: Boolean,
    val name: String,
    val detail: String,
    val icon: String,
    val unlocked: Boolean = false,
)

// Trophy listing result. available=false means no trophy data or the trophy key is missing; reason
// carries a user-facing explanation in that case.
data class TrophyInfo(
    val available: Boolean,
    val reason: String,
    val trophies: List<TrophyEntry>,
)

// Per-game trophy progress for the global Trophies overview screen. All numbers come from real trophy
// definitions + the game's own trophy save (see TrophyEntry.unlocked) -- nothing here is fabricated.
data class GameTrophySummary(
    val game: GameEntry,
    val trophies: List<TrophyEntry>,
) {
    val unlockedCount: Int get() = trophies.count { it.unlocked }
    val percent: Int get() = if (trophies.isEmpty()) 0 else (unlockedCount * 100) / trophies.size

    // Per-tier (Platinum/Gold/Silver/Bronze) unlocked/total counts, in that display order. Only tiers
    // the game actually has any trophies for are included.
    val tierCounts: List<Triple<String, Int, Int>> get() {
        val order = listOf("P", "G", "S", "B")
        return order.mapNotNull { grade ->
            val forGrade = trophies.filter { it.grade.equals(grade, ignoreCase = true) }
            if (forGrade.isEmpty()) null else Triple(grade, forGrade.count { it.unlocked }, forGrade.size)
        }
    }
}

// One togglable patch (a <Metadata> entry) from a game's installed patch XML.
data class PatchEntry(
    val name: String,
    val author: String,
    val appVer: String,
    val enabled: Boolean,
)

// Installed-patch listing for a game. available=false means no patch file is installed yet.
data class PatchInfo(
    val available: Boolean,
    val path: String,
    val patches: List<PatchEntry>,
)

// A patch file available in the online repo (GitHub ps4_cheats/PATCHES): display name + raw URL.
data class RepoPatch(
    val name: String,
    val downloadUrl: String,
)

// One cheat ("mod") from a game's cheat JSON. `lines` is the precomputed "ADDR=VALUE" patch payload
// (newline-separated) handed to the native toggler; `enabled` reflects the current applied state.
data class CheatEntry(
    val name: String,
    val type: String,
    val lines: String,
    val enabled: Boolean,
)

// Cheats listing for a game. available=false means no cheat file is installed; credits is optional.
data class CheatInfo(
    val available: Boolean,
    val credits: String,
    val cheats: List<CheatEntry>,
)
