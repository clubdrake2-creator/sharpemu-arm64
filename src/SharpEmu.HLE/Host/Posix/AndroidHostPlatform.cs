// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

namespace SharpEmu.HLE.Host.Posix;

/// <summary>
/// Android/ARM64 host platform. Android's Bionic libc shares Linux's mmap/mprotect
/// ABI and SDL3 handles audio/input the same way it does on desktop Linux, so this
/// currently needs no overrides of <see cref="PosixHostPlatform"/> — it exists as a
/// distinct type (rather than reusing <see cref="PosixHostPlatform"/> directly) so
/// Android-specific behavior has an obvious, already-wired home the moment it's
/// needed, instead of requiring a later refactor of <see cref="HostPlatform.Create"/>.
/// </summary>
internal sealed class AndroidHostPlatform : PosixHostPlatform;
