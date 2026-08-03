// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using Org.Sharpemu.Android.Core;

namespace SharpEmu.Android;

[Application]
public sealed class SharpEmuApplication : global::Android.App.Application
{
    public SharpEmuApplication(IntPtr handle, global::Android.Runtime.JniHandleOwnership transfer)
        : base(handle, transfer)
    {
    }

    public override void OnCreate()
    {
        base.OnCreate();

        // Installs the C# implementation before any Activity from the Kotlin/Compose UI library
        // runs — every EmulatorBridgeHolder.require() call in that library resolves to this from
        // here on. See EmulatorBridge's doc comment (platform/android/app) for why this indirection
        // is needed instead of a direct reference.
        //
        // EmulatorBridgeHolder is a Kotlin `object` (singleton) — Java.Interop binds its members as
        // instance members reached through a generated static Instance accessor (mirroring the JVM
        // "INSTANCE" static field Kotlin itself compiles the singleton down to), not as plain static
        // members directly on the class. Its own "bridge" property is exposed as Bridge here.
        EmulatorBridgeHolder.Instance.Bridge = new EmulatorBridgeImpl();
    }
}
