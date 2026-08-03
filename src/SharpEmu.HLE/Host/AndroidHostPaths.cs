// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

namespace SharpEmu.HLE.Host;

/// <summary>
/// Android has no writable, portable "next to the executable" location the way
/// desktop <c>AppContext.BaseDirectory</c> resolution assumes — app data instead
/// lives under per-app external/internal storage roots that only the Android
/// process itself can discover (<c>Context.getExternalFilesDir</c>/<c>getFilesDir</c>).
/// The Android app bridge sets these two properties exactly once, at process
/// start, before any other SharpEmu code runs; every path resolver that
/// currently falls back to <c>AppContext.BaseDirectory</c> or a
/// <see cref="Environment.SpecialFolder"/> consults these first when set.
/// </summary>
public static class AndroidHostPaths
{
    /// <summary>
    /// <c>Android/data/&lt;package&gt;/files/SharpEmu</c> (falls back to
    /// <see cref="InternalFilesRoot"/> if that directory could not be created —
    /// see the Android bridge's <c>Initialize</c> method). Save data, screenshots,
    /// and anything the user might reasonably want to find with a file manager
    /// belong here.
    /// </summary>
    public static string? ExternalFilesRoot { get; set; }

    /// <summary>
    /// The app's private internal storage root. Caches and other data with no
    /// reason to be user-visible belong here.
    /// </summary>
    public static string? InternalFilesRoot { get; set; }
}
