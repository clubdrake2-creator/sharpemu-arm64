// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.core

// TODO(Phase 2, pending): this still targets a native "emucore" library the
// way shadPS4's C++ core did — SharpEmu's core is managed C#/.NET, so there
// is no such library to load. This whole object's implementation needs to be
// re-homed as a SharpEmu.Android (net10.0-android) bridge class exposing
// these same method names/JSON shapes via .NET-for-Android's JNI export
// mechanism, with the `external fun`s below repointed at the generated
// bindings instead of a hand-written native shim. Blocked on validating the
// net10.0-android "library embedded in an existing Gradle app" tooling path
// (workload install has failed repeatedly in the current dev environment —
// see the port plan's Phase 0).
object NativeBridge {
    init {
        System.loadLibrary("emucore")
    }

    external fun initialize(
        packageName: String,
        externalStorageRoot: String,
        internalDataRoot: String,
    ): String
    external fun getAppRoot(): String
    external fun getSettings(): String
    external fun updateSetting(section: String, key: String, value: String): Boolean
    external fun getGames(): String
    external fun getGameDetails(gameId: String): String
    external fun getTrophyInfo(gameId: String): String
    external fun getPatchInfo(gameId: String): String
    external fun setPatchEnabled(gameId: String, patchName: String, enabled: Boolean): Boolean
    external fun getEnabledCheats(gameId: String): String
    external fun setCheatEnabled(
        gameId: String,
        cheatName: String,
        enabled: Boolean,
        lines: String,
    ): Boolean
    external fun getGameSettings(gameId: String): String
    external fun updateGameSetting(gameId: String, section: String, key: String, value: String): Boolean
    external fun resetGameSetting(gameId: String, section: String, key: String): Boolean
    external fun addGameFolder(displayName: String, uri: String): Boolean
    external fun deleteGame(gameId: String, forceDeleteFolder: Boolean): Boolean
    external fun installPkg(
        displayName: String,
        fd: Int,
        sourcePath: String,
        installRootPath: String,
    ): String
    external fun getInstallProgress(): Int
    external fun getDeleteProgress(): Int
    external fun cancelInstallPkg()
    external fun getGpuDrivers(): String
    external fun selectGpuDriver(driverId: String): Boolean
    external fun installGpuDriver(displayName: String, fd: Int): String
    external fun installLsfgDll(displayName: String, fd: Int): String
}
