// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using Android.Content.PM;
using Org.Libsdl.App;
using Org.Sharpemu.Android.Overlay;

namespace SharpEmu.Android;

/// <summary>
/// The real "in-game" Activity: extends the SDLActivity Java class ppy.SDL3-CS bundles for Android
/// (the same upstream SDL3 Android glue shadPS4/KytyPS5 vendor directly) — SDL3 has to own the
/// Android window itself (verified: <c>SDL_CreateWindowWithProperties</c> has no external-window
/// property for Android the way it does for Win32/Cocoa/Wayland/X11), so this can't be a Kotlin
/// class the way the rest of the UI is. The actual gameplay-screen Compose overlay (touch controls,
/// precompile progress) is still 100% the Kotlin/Compose code from platform/android/app — see
/// <see cref="GameOverlayHost"/> — attached on top of this Activity's SDL surface from here instead
/// of self-hosted, which is the only thing that changed about it.
///
/// <c>SDLActivity.main()</c> (bound here as <see cref="Main"/>) is explicitly documented as overridable by derived classes: the
/// default implementation dlsym's a native "SDL_main" symbol out of a compiled .so, which does not
/// exist here (SharpEmu's core is managed C#, not a native library) — overriding it directly runs
/// SharpEmu's entry point on the same dedicated "SDLThread" SDLActivity already runs it on for the
/// native case, calling SDL3 the same way desktop's SdlHostWindow.cs does (ppy.SDL3-CS P/Invoke
/// bindings), with no native shim needed anywhere in this design.
/// </summary>
[Activity(
    Name = "org.sharpemu.android.GameActivity",
    Process = ":game",
    AlwaysRetainTaskState = true,
    ConfigurationChanges = ConfigChanges.LayoutDirection | ConfigChanges.Locale | ConfigChanges.FontScale |
        ConfigChanges.Orientation | ConfigChanges.UiMode | ConfigChanges.ScreenLayout |
        ConfigChanges.ScreenSize | ConfigChanges.SmallestScreenSize | ConfigChanges.Keyboard |
        ConfigChanges.KeyboardHidden | ConfigChanges.Navigation,
    Exported = true,
    LaunchMode = global::Android.Content.PM.LaunchMode.SingleTop,
    ScreenOrientation = global::Android.Content.PM.ScreenOrientation.SensorLandscape,
    SupportsPictureInPicture = true)]
public sealed class GameActivity : SDLActivity
{
    protected override string[] GetLibraries() => ["SDL3"];

    protected override void Main()
    {
        var gamePath = Intent?.GetStringExtra("org.sharpemu.android.extra.GAME_PATH") ?? string.Empty;
        var titleId = Intent?.GetStringExtra("org.sharpemu.android.extra.GAME_ID") ?? string.Empty;
        var appRoot = Intent?.GetStringExtra("org.sharpemu.android.extra.APP_ROOT") ?? string.Empty;

        if (string.IsNullOrWhiteSpace(gamePath))
        {
            global::Android.Util.Log.Error("SharpEmu", "GameActivity.Main: no game path in the launch Intent");
            return;
        }

        global::Android.Util.Log.Info("SharpEmu", $"GameActivity.Main: starting emulation path='{gamePath}' titleId='{titleId}' appRoot='{appRoot}'");
        GameSession.RunOnCurrentThread(gamePath, titleId, appRoot);
    }

    protected override void OnCreate(global::Android.OS.Bundle? savedInstanceState)
    {
        base.OnCreate(savedInstanceState);

        var renderDiagCaptureEnabled =
            Intent?.GetBooleanExtra("org.sharpemu.android.extra.RENDER_DIAG_CAPTURE_ENABLED", false) ?? false;
        // GameOverlayHost is a Kotlin `object` too — see EmulatorBridgeHolder.Instance.Instance's
        // comment in SharpEmuApplication.cs for why this goes through a generated Instance accessor.
        GameOverlayHost.Instance.Attach(this, renderDiagCaptureEnabled);
    }

    protected override void OnDestroy()
    {
        GameSession.Stop();
        base.OnDestroy();
    }
}
