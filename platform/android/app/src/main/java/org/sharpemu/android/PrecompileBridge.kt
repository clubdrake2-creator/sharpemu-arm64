// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android

// Bridge to the boot-time blocking precompile progress (rpcs3-style — see
// Core::Cpu::SetPrecompileProgress / linker.cpp). The native function lives in libemucore.so,
// which GameActivity (an SDLActivity) already loads, so no extra loadLibrary is needed here —
// same reasoning as GamePadBridge. NativeBridge loads libemucore.so too; that
// library runs in MainActivity's default process, while the precompile itself only ever runs in
// GameActivity's separate ":game" process (see AndroidManifest.xml).
object PrecompileBridge {
    external fun nativeGetPrecompileProgress(): Int

    // 0..100, or -1 when no precompile is running (either finished, or the active CPU backend
    // doesn't have one — CpuBackend::PrecompileBeforeBoot is a no-op default).
    fun getProgress(): Int = runCatching { nativeGetPrecompileProgress() }.getOrDefault(-1)
}
