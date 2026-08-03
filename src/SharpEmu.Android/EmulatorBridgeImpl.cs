// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using Android.Views;
using Org.Sharpemu.Android.Core;
using SharpEmu.HLE.Host;

namespace SharpEmu.Android;

/// <summary>
/// Implements the Kotlin-defined <c>EmulatorBridge</c> interface (from platform/android/app,
/// bound into this project via the AndroidLibrary reference) — this is the entire contact surface
/// between the Kotlin/Compose UI and SharpEmu's managed core. <see cref="SharpEmuApplication"/>
/// installs this as <c>EmulatorBridgeHolder.Instance.Bridge</c> before any Activity in the library runs.
/// </summary>
public sealed class EmulatorBridgeImpl : Java.Lang.Object, IEmulatorBridge
{
    public string Initialize(string packageName, string externalStorageRoot, string internalDataRoot)
    {
        var externalRoot = System.IO.Path.Combine(externalStorageRoot, "Android", "data", packageName, "files", "SharpEmu");
        try
        {
            System.IO.Directory.CreateDirectory(externalRoot);
            AndroidHostPaths.ExternalFilesRoot = externalRoot;
        }
        catch
        {
            // Scoped-storage provisioning failed (shouldn't happen — the Kotlin side already calls
            // Context.getExternalFilesDir(null) before this, forcing the OS to create the parent
            // directory tree with the right ownership) — fall back to internal storage.
        }

        var internalRoot = System.IO.Path.Combine(internalDataRoot, "SharpEmu");
        System.IO.Directory.CreateDirectory(internalRoot);
        AndroidHostPaths.InternalFilesRoot = internalRoot;

        return AndroidHostPaths.ExternalFilesRoot ?? internalRoot;
    }

    // Kotlin's no-arg getXxx() functions bind to C# properties (Java Bean convention), not methods —
    // only parameterized getters (GetGameDetails(id), etc.) stay methods.
    public string AppRoot => AndroidHostPaths.ExternalFilesRoot ?? AndroidHostPaths.InternalFilesRoot ?? string.Empty;

    // --- Settings / library / trophies / patches / cheats / GPU drivers ---------------------
    // TODO: real implementations — port from shadPS4's Core::Android::* (android_app_host.cpp) once
    // SharpEmu has the equivalent config/game-scan/trophy/patch/cheat subsystems wired for Android.
    // Stubbed with empty-but-valid JSON so the Kotlin UI doesn't crash while this is built out.
    public string Settings => "{}";
    public bool UpdateSetting(string section, string key, string value) => false;
    public string Games => "[]";
    public string GetGameDetails(string gameId) => "{}";
    public bool AddGameFolder(string displayName, string uri) => false;
    public bool DeleteGame(string gameId, bool forceDeleteFolder) => false;
    public string GetTrophyInfo(string gameId) => "{}";
    public string GetPatchInfo(string gameId) => "{}";
    public bool SetPatchEnabled(string gameId, string patchName, bool enabled) => false;
    public string GetEnabledCheats(string gameId) => "[]";
    public bool SetCheatEnabled(string gameId, string cheatName, bool enabled, string lines) => false;
    public string GetGameSettings(string gameId) => "{}";
    public bool UpdateGameSetting(string gameId, string section, string key, string value) => false;
    public bool ResetGameSetting(string gameId, string section, string key) => false;
    public string InstallPkg(string displayName, int fd, string sourcePath, string installRootPath) => "{}";
    public int InstallProgress => 0;
    public int DeleteProgress => 0;
    public void CancelInstallPkg() { }
    public string GpuDrivers => "[]";
    public bool SelectGpuDriver(string driverId) => false;
    public string InstallGpuDriver(string displayName, int fd) => "{}";
    public string InstallLsfgDll(string displayName, int fd) => "{}";

    // --- Virtual gamepad -----------------------------------------------------------------------
    public void SetPadButton(int button, bool pressed) => GameSession.SetPadButton(button, pressed);
    public void SetPadAxis(int axis, int value) => GameSession.SetPadAxis(axis, value);
    public void RequestRenderDiagCapture() => GameSession.RequestRenderDiagCapture();

    // --- Emulation -----------------------------------------------------------------------------
    // Actual emulation is started from GameActivity.Main() (SDLActivity's own dedicated thread),
    // not from this Surface-ready callback — GameActivity extends SDLActivity directly (SDL3 has to
    // own the Android window itself; see GameActivity's doc comment), so by the time Main() runs,
    // SDL already has a valid window/surface of its own. This callback exists on the interface for
    // API symmetry with the original NativeBridge design and is intentionally a no-op here.
    public void StartEmulation(Surface surface, string gamePath, string titleId, IDictionary<string, string> args) { }

    public void SurfaceDestroyed() { }

    public void StopEmulation() => GameSession.Stop();
}
