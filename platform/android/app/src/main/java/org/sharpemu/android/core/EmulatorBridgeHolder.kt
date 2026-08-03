// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.sharpemu.android.core

/**
 * Carries the live [EmulatorBridge] implementation from the app to this library at runtime.
 *
 * This module (a plain Android library — see [EmulatorBridge]'s doc comment) cannot depend on the
 * app project that consumes it (SharpEmu.Android): Gradle library modules never see the classes of
 * whatever eventually depends on them, only the reverse. So the app can't just be referenced by
 * type from here. Instead, the app's [Application.OnCreate] sets [bridge] to its C# [EmulatorBridge]
 * implementation — visible to it because .NET-for-Android's Java.Interop binds this Kotlin
 * interface into a C#-implementable type when the app references this module's .aar — before any
 * Activity in this module runs. Every call site in the UI code goes through [bridge], not a direct
 * reference.
 *
 * Named [bridge] rather than "instance" deliberately: this is itself a Kotlin `object` (singleton),
 * and Java.Interop's binding of a Kotlin object's own singleton access already uses an "Instance"
 * name on the C# side — a same-named property here would collide with (and be shadowed by) that.
 */
object EmulatorBridgeHolder {
    @Volatile
    var bridge: EmulatorBridge? = null

    fun require(): EmulatorBridge =
        bridge ?: error("EmulatorBridgeHolder.bridge not set — SharpEmu.Android's Application.OnCreate must run first.")
}
