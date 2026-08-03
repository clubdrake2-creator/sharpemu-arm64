// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.core

// Thin forwarder to EmulatorBridgeHolder.instance, kept under the same name/method surface as the
// shadPS4 original (which called straight into a native "emucore" library) so the rest of the UI
// code — EmulatorRepository and everything downstream of it — did not need to change at all. See
// EmulatorBridge's doc comment for why the real implementation lives on the C# side instead.
object NativeBridge {
    private val bridge get() = EmulatorBridgeHolder.require()

    fun initialize(packageName: String, externalStorageRoot: String, internalDataRoot: String): String =
        bridge.initialize(packageName, externalStorageRoot, internalDataRoot)
    fun getAppRoot(): String = bridge.getAppRoot()
    fun getSettings(): String = bridge.getSettings()
    fun updateSetting(section: String, key: String, value: String): Boolean =
        bridge.updateSetting(section, key, value)
    fun getGames(): String = bridge.getGames()
    fun getGameDetails(gameId: String): String = bridge.getGameDetails(gameId)
    fun getTrophyInfo(gameId: String): String = bridge.getTrophyInfo(gameId)
    fun getPatchInfo(gameId: String): String = bridge.getPatchInfo(gameId)
    fun setPatchEnabled(gameId: String, patchName: String, enabled: Boolean): Boolean =
        bridge.setPatchEnabled(gameId, patchName, enabled)
    fun getEnabledCheats(gameId: String): String = bridge.getEnabledCheats(gameId)
    fun setCheatEnabled(gameId: String, cheatName: String, enabled: Boolean, lines: String): Boolean =
        bridge.setCheatEnabled(gameId, cheatName, enabled, lines)
    fun getGameSettings(gameId: String): String = bridge.getGameSettings(gameId)
    fun updateGameSetting(gameId: String, section: String, key: String, value: String): Boolean =
        bridge.updateGameSetting(gameId, section, key, value)
    fun resetGameSetting(gameId: String, section: String, key: String): Boolean =
        bridge.resetGameSetting(gameId, section, key)
    fun addGameFolder(displayName: String, uri: String): Boolean = bridge.addGameFolder(displayName, uri)
    fun deleteGame(gameId: String, forceDeleteFolder: Boolean): Boolean =
        bridge.deleteGame(gameId, forceDeleteFolder)
    fun installPkg(displayName: String, fd: Int, sourcePath: String, installRootPath: String): String =
        bridge.installPkg(displayName, fd, sourcePath, installRootPath)
    fun getInstallProgress(): Int = bridge.getInstallProgress()
    fun getDeleteProgress(): Int = bridge.getDeleteProgress()
    fun cancelInstallPkg() = bridge.cancelInstallPkg()
    fun getGpuDrivers(): String = bridge.getGpuDrivers()
    fun selectGpuDriver(driverId: String): Boolean = bridge.selectGpuDriver(driverId)
    fun installGpuDriver(displayName: String, fd: Int): String = bridge.installGpuDriver(displayName, fd)
    fun installLsfgDll(displayName: String, fd: Int): String = bridge.installLsfgDll(displayName, fd)
}
