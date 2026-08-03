// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.sharpemu.android.core

import android.view.Surface

/**
 * The contract between this module (the Kotlin/Compose UI — game library, settings, controller
 * config, the in-game touch overlay) and SharpEmu's managed C#/.NET emulator core.
 *
 * Unlike shadPS4/KytyPS5, whose Android ports link a native C++ core straight into this module's
 * own .so and expose it via hand-written JNI, SharpEmu's core is managed C#/.NET: this module
 * builds as a plain Android library (.aar) with no native code of its own, and the real installed
 * app is the separate SharpEmu.Android (net10.0-android) project, which references this .aar via
 * the AndroidLibrary MSBuild item and implements this interface directly — Java.Interop generates
 * the Kotlin<->C# call glue automatically, so there is no hand-written JNI on either side.
 *
 * [EmulatorBridgeHolder] carries the live implementation from the app to this library at runtime
 * (see its doc comment for why a holder, not a constructor parameter, is needed here).
 */
interface EmulatorBridge {
    // --- Lifecycle -----------------------------------------------------------------------
    fun initialize(packageName: String, externalStorageRoot: String, internalDataRoot: String): String
    fun getAppRoot(): String

    // --- Settings --------------------------------------------------------------------------
    fun getSettings(): String
    fun updateSetting(section: String, key: String, value: String): Boolean

    // --- Game library ------------------------------------------------------------------------
    fun getGames(): String
    fun getGameDetails(gameId: String): String
    fun addGameFolder(displayName: String, uri: String): Boolean
    fun deleteGame(gameId: String, forceDeleteFolder: Boolean): Boolean

    // --- Trophies / patches / cheats ----------------------------------------------------------
    fun getTrophyInfo(gameId: String): String
    fun getPatchInfo(gameId: String): String
    fun setPatchEnabled(gameId: String, patchName: String, enabled: Boolean): Boolean
    fun getEnabledCheats(gameId: String): String
    fun setCheatEnabled(gameId: String, cheatName: String, enabled: Boolean, lines: String): Boolean

    // --- Per-game settings -------------------------------------------------------------------
    fun getGameSettings(gameId: String): String
    fun updateGameSetting(gameId: String, section: String, key: String, value: String): Boolean
    fun resetGameSetting(gameId: String, section: String, key: String): Boolean

    // --- PKG install -------------------------------------------------------------------------
    fun installPkg(displayName: String, fd: Int, sourcePath: String, installRootPath: String): String
    fun getInstallProgress(): Int
    fun getDeleteProgress(): Int
    fun cancelInstallPkg()

    // --- GPU driver management (Adreno/Turnip-style custom driver packages) ------------------
    fun getGpuDrivers(): String
    fun selectGpuDriver(driverId: String): Boolean
    fun installGpuDriver(displayName: String, fd: Int): String
    fun installLsfgDll(displayName: String, fd: Int): String

    // --- Virtual gamepad (touch overlay -> emulated pad; SDL_GamepadButton/Axis ordering) -----
    fun setPadButton(button: Int, pressed: Boolean)
    fun setPadAxis(axis: Int, value: Int)
    fun requestRenderDiagCapture()

    // --- Emulation -----------------------------------------------------------------------------
    /**
     * Starts emulating [gamePath] rendering into [surface] (GameActivity's SurfaceView — SharpEmu
     * wraps its native ANativeWindow with SDL3's external-window creation, the same SDL3 dependency
     * already used on desktop, rather than SDL owning the Activity/Java surface lifecycle the way
     * shadPS4/KytyPS5's SDLActivity-based GameActivity does). Runs on a background thread; returns
     * immediately. [args] carries the same CLI-style options the desktop build accepts
     * (--cpu-engine, trace flags, etc. — always interpreter-only on Android, enforced app-side).
     */
    fun startEmulation(surface: Surface, gamePath: String, titleId: String, args: Map<String, String>)

    /** Called when the SurfaceView is destroyed (Activity finishing, or app backgrounded far enough) — must not block. */
    fun surfaceDestroyed()

    /** Requests the running game stop; GameActivity calls this before finishing so save data can flush. */
    fun stopEmulation()
}
