// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using Android.Content.PM;
using Android.Graphics;
using AndroidX.Core.View;
using AndroidX.Lifecycle;
using AndroidX.SavedState;
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
public sealed class GameActivity : SDLActivity, ILifecycleOwner, IViewModelStoreOwner, ISavedStateRegistryOwner
{
    // SDLActivity extends plain android.app.Activity, not ComponentActivity — so unlike a normal
    // Compose host, nothing here automatically publishes a ViewTreeLifecycleOwner/ViewModelStoreOwner/
    // SavedStateRegistryOwner up the view tree. GameOverlayHost.Attach's ComposeView.setContent walks
    // up looking for one and throws IllegalStateException ("ViewTreeLifecycleOwner not found") without
    // this — confirmed by an on-device FATAL EXCEPTION crash the moment a game launched. This manually
    // reproduces the plumbing ComponentActivity normally provides for free.
    private readonly LifecycleRegistry _lifecycleRegistry;
    private readonly SavedStateRegistryController _savedStateRegistryController;
    private readonly ViewModelStore _viewModelStore = new();

    public GameActivity()
    {
        _lifecycleRegistry = new LifecycleRegistry(this);
        _savedStateRegistryController = SavedStateRegistryController.Create(this);
    }

    public Lifecycle Lifecycle => _lifecycleRegistry;

    public ViewModelStore ViewModelStore => _viewModelStore;

    public SavedStateRegistry SavedStateRegistry => _savedStateRegistryController.SavedStateRegistry;

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
        try
        {
            GameSession.RunOnCurrentThread(gamePath, titleId, appRoot);
        }
        catch (Exception exception)
        {
            // Main() runs on SDLActivity's own native-spawned "SDLThread", not a normal JNI-attached
            // managed thread — the runtime's usual unhandled-exception marshaling back to Java doesn't
            // reliably work from here, so catch and log explicitly rather than risk losing the real
            // exception behind an opaque native abort.
            global::Android.Util.Log.Error("SharpEmu", $"GameActivity.Main: emulation crashed: {exception}");
        }
    }

    protected override void OnCreate(global::Android.OS.Bundle? savedInstanceState)
    {
        RequestedOrientation = global::Android.Content.PM.ScreenOrientation.SensorLandscape;
        _savedStateRegistryController.PerformRestore(savedInstanceState);
        base.OnCreate(savedInstanceState);
        RequestedOrientation = global::Android.Content.PM.ScreenOrientation.SensorLandscape;
        _lifecycleRegistry.HandleLifecycleEvent(Lifecycle.Event.OnCreate);

        var decorView = Window!.DecorView;
        ViewTreeLifecycleOwner.Set(decorView, this);
        ViewTreeViewModelStoreOwner.Set(decorView, this);
        ViewTreeSavedStateRegistryOwner.Set(decorView, this);

        ApplyImmersiveFullscreen();

        var renderDiagCaptureEnabled =
            Intent?.GetBooleanExtra("org.sharpemu.android.extra.RENDER_DIAG_CAPTURE_ENABLED", false) ?? false;
        // GameOverlayHost is a Kotlin `object` too — see EmulatorBridgeHolder.Instance.Instance's
        // comment in SharpEmuApplication.cs for why this goes through a generated Instance accessor.
        GameOverlayHost.Instance.Attach(this, renderDiagCaptureEnabled);
    }

    // SDLActivity's own setOrientationBis (below) derives an orientation guess from the width/height
    // SDL's native window-creation code reports — on Android that guess landed in portrait on-device
    // (the SDL window is created before the real landscape-locked SurfaceView geometry is settled),
    // which is what made gameplay render sideways/small in the corner. Forcing landscape here
    // unconditionally, the same way shadPS4's Android GameActivity.kt does (see its setOrientationBis
    // override and the "orientation is an Activity policy, native SDL only receives the final Surface
    // dimensions" comment there), removes SDL's own orientation guess from the equation entirely.
    public override void SetOrientationBis(int w, int h, bool resizable, string? hint)
    {
        RequestedOrientation = global::Android.Content.PM.ScreenOrientation.SensorLandscape;
    }

    public override void OnWindowFocusChanged(bool hasFocus)
    {
        base.OnWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            ApplyImmersiveFullscreen();
        }
    }

    // Mirrors shadPS4's GameActivity.kt applyGameplayDisplayMode: fullscreen/system-bar state is
    // owned directly by the Activity's own Window/WindowInsetsController, not by SDL's
    // SDL_SetWindowFullscreen path (SdlHostWindow.cs's ApplyConfiguredMode) — that path is built for
    // desktop windowing and, per shadPS4's own comment, is not the reliable place to drive Android's
    // fullscreen/immersive state from. This makes the SurfaceView SDL renders into occupy the entire
    // screen (not the area left over after the status/navigation bars), which is what made the
    // gameplay surface look small and pinned to the top instead of centered/full-bleed.
    private void ApplyImmersiveFullscreen()
    {
        if (Window is null)
        {
            return;
        }

        Window.SetStatusBarColor(Color.Transparent);
        Window.SetNavigationBarColor(Color.Transparent);
        WindowCompat.SetDecorFitsSystemWindows(Window, false);
        var controller = new WindowInsetsControllerCompat(Window, Window.DecorView);
        controller.SystemBarsBehavior = WindowInsetsControllerCompat.BehaviorShowTransientBarsBySwipe;
        controller.Hide(WindowInsetsCompat.Type.SystemBars());
    }

    protected override void OnStart()
    {
        base.OnStart();
        _lifecycleRegistry.HandleLifecycleEvent(Lifecycle.Event.OnStart);
    }

    protected override void OnResume()
    {
        base.OnResume();
        RequestedOrientation = global::Android.Content.PM.ScreenOrientation.SensorLandscape;
        _lifecycleRegistry.HandleLifecycleEvent(Lifecycle.Event.OnResume);
        ApplyImmersiveFullscreen();
    }

    protected override void OnPause()
    {
        _lifecycleRegistry.HandleLifecycleEvent(Lifecycle.Event.OnPause);
        base.OnPause();
    }

    protected override void OnStop()
    {
        _lifecycleRegistry.HandleLifecycleEvent(Lifecycle.Event.OnStop);
        base.OnStop();
    }

    protected override void OnDestroy()
    {
        GameSession.Stop();
        _lifecycleRegistry.HandleLifecycleEvent(Lifecycle.Event.OnDestroy);
        _viewModelStore.Clear();
        base.OnDestroy();
    }
}
