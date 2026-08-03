// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.app.PictureInPictureParams
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.view.Gravity
import android.view.InputDevice
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.libsdl.app.SDLActivity
import org.sharpemu.android.model.GameEntry
import org.sharpemu.android.overlay.OverlayLuminance
import org.sharpemu.android.ui.theme.AppearancePrefs
import org.sharpemu.android.ui.theme.appUiText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameActivity :
    SDLActivity(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    override fun getLibraries(): Array<String> = arrayOf("emucore")

    // SDLActivity extends plain Activity, which does NOT provide the ViewTree owners a ComposeView
    // needs. We implement Lifecycle/ViewModelStore/SavedStateRegistry ourselves and drive them from
    // the activity callbacks so the Compose on-screen gamepad overlay can be hosted on top of the
    // SDL surface.
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    private val viewModelStoreField = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = viewModelStoreField
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    private var lastGameplayRefreshRateHint = 0f
    private var lastGameplayRefreshModeId = 0

    private var overlayView: View? = null
    private var controlsToggle: View? = null
    // Set true once a physical controller has been seen, so the auto-hide toast is shown only once.
    private var physicalControllerNotified = false
    private var performanceWakeLock: PowerManager.WakeLock? = null
    private var debugPadReceiverRegistered = false
    private var pipSurfaceWidth = 0
    private var pipSurfaceHeight = 0

    private val debugPadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_DEBUG_PAD_PULSE) return
            when (intent.getStringExtra(EXTRA_DEBUG_PAD_BUTTON).orEmpty().lowercase(Locale.US)) {
                "cross", "x" -> {
                    Log.i(TAG, "debug pad pulse: cross")
                    pulseVirtualButton(GamePadBridge.BUTTON_SOUTH)
                }
                "options", "start" -> {
                    Log.i(TAG, "debug pad pulse: options")
                    pulseVirtualButton(GamePadBridge.BUTTON_START)
                }
                "select", "back" -> {
                    Log.i(TAG, "debug pad pulse: select")
                    pulseVirtualButton(GamePadBridge.BUTTON_BACK)
                }
                "dpad_up", "up" -> {
                    Log.i(TAG, "debug pad pulse: dpad_up")
                    pulseVirtualButton(GamePadBridge.BUTTON_DPAD_UP)
                }
                "dpad_down", "down" -> {
                    Log.i(TAG, "debug pad pulse: dpad_down")
                    pulseVirtualButton(GamePadBridge.BUTTON_DPAD_DOWN)
                }
                "dpad_left", "left" -> {
                    Log.i(TAG, "debug pad pulse: dpad_left")
                    pulseVirtualButton(GamePadBridge.BUTTON_DPAD_LEFT)
                }
                "dpad_right", "right" -> {
                    Log.i(TAG, "debug pad pulse: dpad_right")
                    pulseVirtualButton(GamePadBridge.BUTTON_DPAD_RIGHT)
                }
                "circle" -> {
                    Log.i(TAG, "debug pad pulse: circle")
                    pulseVirtualButton(GamePadBridge.BUTTON_EAST)
                }
                "square" -> {
                    Log.i(TAG, "debug pad pulse: square")
                    pulseVirtualButton(GamePadBridge.BUTTON_WEST)
                }
                "triangle" -> {
                    Log.i(TAG, "debug pad pulse: triangle")
                    pulseVirtualButton(GamePadBridge.BUTTON_NORTH)
                }
                "l1" -> {
                    Log.i(TAG, "debug pad pulse: l1")
                    pulseVirtualButton(GamePadBridge.BUTTON_LEFT_SHOULDER)
                }
                "r1" -> {
                    Log.i(TAG, "debug pad pulse: r1")
                    pulseVirtualButton(GamePadBridge.BUTTON_RIGHT_SHOULDER)
                }
            }
        }
    }

    private val inputManager: InputManager by lazy {
        getSystemService(Context.INPUT_SERVICE) as InputManager
    }

    private val highRefreshSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            scheduleHighRefreshRateHint()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            scheduleHighRefreshRateHint()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
    }

    // --- Adaptive on-screen-control contrast (see OverlayLuminance) ---
    // Every ~250 ms we PixelCopy the SDL surface into a tiny GRID_W x GRID_H bitmap (PixelCopy
    // downscales for us), compute per-cell luminance off the main thread, and publish it so the
    // Compose overlay re-tints each button to contrast whatever is behind it.
    private val luminanceUiHandler = Handler(Looper.getMainLooper())
    private var luminanceThread: HandlerThread? = null
    private var luminanceCbHandler: Handler? = null
    private var luminanceBitmap: Bitmap? = null
    private var luminanceSampling = false
    private val luminanceIntervalMs = 250L
    private val luminanceRunnable = object : Runnable {
        override fun run() {
            sampleBackgroundLuminance()
            luminanceUiHandler.postDelayed(this, luminanceIntervalMs)
        }
    }

    private fun startLuminanceSampling() {
        if (luminanceSampling) return
        luminanceSampling = true
        if (luminanceThread == null) {
            luminanceThread = HandlerThread("overlay-luminance").also { it.start() }
            luminanceCbHandler = Handler(luminanceThread!!.looper)
        }
        luminanceUiHandler.post(luminanceRunnable)
    }

    private fun stopLuminanceSampling() {
        luminanceSampling = false
        luminanceUiHandler.removeCallbacks(luminanceRunnable)
    }

    private fun sampleBackgroundLuminance() {
        if (!OverlayLuminance.enabled) return
        // Only sample while the overlay is actually shown (no point tinting hidden buttons).
        if (overlayView?.visibility != View.VISIBLE) return
        val surface: SurfaceView = mSurface ?: return
        if (surface.width <= 0 || surface.height <= 0 || !surface.holder.surface.isValid) return
        val w = OverlayLuminance.GRID_W
        val h = OverlayLuminance.GRID_H
        val bmp = luminanceBitmap
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { luminanceBitmap = it }
        val cb = luminanceCbHandler ?: return
        try {
            PixelCopy.request(surface, bmp, { result ->
                if (result != PixelCopy.SUCCESS) return@request
                val px = IntArray(w * h)
                bmp.getPixels(px, 0, w, 0, 0, w, h)
                val lum = FloatArray(w * h)
                for (i in px.indices) {
                    val c = px[i]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    // Rec.601 luma, normalized to 0..1.
                    lum[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                }
                OverlayLuminance.update(lum)
            }, cb)
        } catch (_: Throwable) {
            // Surface not ready / no buffer yet — skip this tick.
        }
    }

    // Reacts to controllers being plugged/unplugged at runtime by re-evaluating the on-screen
    // controls' visibility (and the native virtual-pad/physical-pad assignment is handled by SDL's
    // GAMEPAD_ADDED/REMOVED events independently).
    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refreshControlsVisibility()
        override fun onInputDeviceRemoved(deviceId: Int) = refreshControlsVisibility()
        override fun onInputDeviceChanged(deviceId: Int) = refreshControlsVisibility()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        savedStateController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        keepGameplayActivityVisible()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        applyBaselineGameScheduling()
        applyAndroidGamePerformanceHints()
        applyMaximumPerformanceMode()
        mSurface?.holder?.addCallback(highRefreshSurfaceCallback)
        scheduleHighRefreshRateHint()
        scheduleGameplayDisplayMode()
        registerDebugPadReceiver()

        // On-screen gamepad overlay (the PCSX2-Android touch controls, ported) drawn on top of the
        // SDL surface. It reads the SAME layout the virtual-control editor saves (ControlPrefs) so
        // the in-game buttons match the edited layout exactly, and forwards every input to the SDL
        // virtual gamepad (PadInput -> GamePadBridge), which the emulator treats as a controller.
        val renderDiagCaptureEnabled = intent.getBooleanExtra(EXTRA_RENDER_DIAG_CAPTURE_ENABLED, false)
        val overlay = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GameActivity)
            setViewTreeViewModelStoreOwner(this@GameActivity)
            setViewTreeSavedStateRegistryOwner(this@GameActivity)
            setContent {
                org.sharpemu.android.ui.theme.ShadTheme {
                    org.sharpemu.android.overlay.GameTouchControls()
                    if (renderDiagCaptureEnabled) {
                        org.sharpemu.android.overlay.RenderDiagCaptureButton()
                    }
                }
            }
        }
        overlayView = overlay
        // On-screen controls visibility follows the "Mostrar controles na tela" setting (Controles
        // screen). Tap the eye (👁) button at the bottom-right to toggle them at any time.
        val showOverlay = getSharedPreferences("PadOverlayPrefs", Context.MODE_PRIVATE)
            .getBoolean("show_touch_overlay", false)
        overlay.visibility = if (showOverlay) View.VISIBLE else View.GONE
        addContentView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        // Precompile progress overlay: a SEPARATE ComposeView, always VISIBLE (never gated by the
        // "show_touch_overlay" toggle above — that one hides the whole `overlay` View, which would
        // silently hide this too since it self-manages its own show/hide via AnimatedVisibility).
        val precompileOverlay = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GameActivity)
            setViewTreeViewModelStoreOwner(this@GameActivity)
            setViewTreeSavedStateRegistryOwner(this@GameActivity)
            setContent {
                org.sharpemu.android.ui.theme.ShadTheme {
                    org.sharpemu.android.overlay.PrecompileProgressOverlay(
                        AppearancePrefs.getAppLanguage(this@GameActivity),
                    )
                }
            }
        }
        addContentView(
            precompileOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        // Eye toggle to show/hide the on-screen controls: a discreet, icon-only silhouette pinned to
        // the very bottom edge and slightly rotated to tuck into the corner. The eye is "open" when
        // controls are shown and "crossed out" (diagonal slash) when hidden. Only the icon is
        // clickable. Not focusable so it never steals D-pad/controller navigation from the SDL surface.
        val density = resources.displayMetrics.density
        val eyeSize = (32 * density).toInt()
        val toggle = ImageView(this).apply {
            setImageResource(if (showOverlay) R.drawable.ic_eye else R.drawable.ic_eye_off)
            alpha = 0.4f
            rotation = -35f
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener {
                val show = overlay.visibility != View.VISIBLE
                overlay.visibility = if (show) View.VISIBLE else View.GONE
                setImageResource(if (show) R.drawable.ic_eye else R.drawable.ic_eye_off)
            }
        }
        controlsToggle = toggle
        val lp = FrameLayout.LayoutParams(eyeSize, eyeSize).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            val margin = (4 * density).toInt()
            rightMargin = margin
            bottomMargin = margin
        }
        addContentView(toggle, lp)

        refreshControlsVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateController.performSave(outState)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        inputManager.registerInputDeviceListener(deviceListener, null)
        refreshControlsVisibility()
        startLuminanceSampling()
        applyAndroidGamePerformanceHints()
        scheduleHighRefreshRateHint()
        scheduleGameplayDisplayMode()
    }

    // Keep gameplay locked to the landscape axis and let Android, not SDL's desktop display-mode
    // path, own system-bar/fullscreen state. This mirrors ARMSX2's Android split: orientation is an
    // Activity policy, while native SDL only receives the final Surface dimensions.
    private fun applyGameplayDisplayMode() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val mode = readFullscreenMode()
        val immersive = mode != FULLSCREEN_MODE_WINDOWED
        val borderless = mode == FULLSCREEN_MODE_BORDERLESS
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, !borderless)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            var flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            if (borderless) {
                flags = flags or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode =
                            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
            }
            window.decorView.systemUiVisibility = flags
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        Log.i(TAG, "Gameplay display mode applied: mode='$mode' immersive=$immersive borderless=$borderless")
    }

    private fun keepGameplayActivityVisible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }

    private fun scheduleGameplayDisplayMode() {
        applyGameplayDisplayMode()
        window.decorView.post { applyGameplayDisplayMode() }
        window.decorView.postDelayed({ applyGameplayDisplayMode() }, 250L)
        window.decorView.postDelayed({ applyGameplayDisplayMode() }, 1000L)
    }

    private fun scheduleHighRefreshRateHint() {
        applyHighRefreshRateHint()
        window.decorView.post { applyHighRefreshRateHint() }
        window.decorView.postDelayed({ applyHighRefreshRateHint() }, 250L)
        window.decorView.postDelayed({ applyHighRefreshRateHint() }, 1000L)
    }

    private fun applyHighRefreshRateHint() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        val currentDisplayRefreshRate = display?.refreshRate
        val mode = preferredGameplayDisplayMode()
        val refreshRate = mode?.refreshRate ?: 120f
        val modeId = mode?.modeId ?: 0
        if (lastGameplayRefreshRateHint == refreshRate && lastGameplayRefreshModeId == modeId) {
            return
        }
        val attributes = window.attributes.apply {
            preferredRefreshRate = refreshRate
            preferredDisplayModeId = modeId.takeIf { it != 0 } ?: preferredDisplayModeId
        }
        runCatching {
            attributes.javaClass.getField("preferredMinDisplayRefreshRate")
                .setFloat(attributes, refreshRate)
            attributes.javaClass.getField("preferredMaxDisplayRefreshRate")
                .setFloat(attributes, refreshRate)
        }
        window.attributes = attributes
        val surface = mSurface?.holder?.surface ?: return
        if (!surface.isValid) {
            return
        }
        runCatching {
            // Keep gameplay as a fixed high-rate source. Using DEFAULT here lets SurfaceFlinger
            // infer slow loading/video cadence as 24Hz on some Samsung devices, producing repeated
            // 120Hz<->24Hz mode changes that look like a JIT performance regression.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                surface.setFrameRate(
                    refreshRate,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ALWAYS,
                )
            } else {
                @Suppress("DEPRECATION")
                surface.setFrameRate(refreshRate, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
            }
            lastGameplayRefreshRateHint = refreshRate
            lastGameplayRefreshModeId = modeId
            Log.i(
                TAG,
                "Gameplay refresh-rate hint applied: refreshRate=$refreshRate modeId=${mode?.modeId} " +
                    "surface=${mSurface?.width}x${mSurface?.height} display=$currentDisplayRefreshRate",
            )
        }.onFailure {
            Log.w(TAG, "Failed to apply gameplay refresh-rate hint", it)
        }
    }

    private fun preferredGameplayDisplayMode(): android.view.Display.Mode? {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        return display?.supportedModes
            ?.filter { it.refreshRate >= 60f }
            ?.maxByOrNull { it.refreshRate }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            scheduleGameplayDisplayMode()
            scheduleHighRefreshRateHint()
            applyAndroidGamePerformanceHints()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        scheduleGameplayDisplayMode()
        scheduleHighRefreshRateHint()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPictureIfEnabled()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            overlayView?.visibility = View.GONE
            controlsToggle?.visibility = View.GONE
        } else {
            refreshControlsVisibility()
            restoreFullscreenAfterPip()
            scheduleGameplayDisplayMode()
        }
    }

    private fun enterPictureInPictureIfEnabled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode) {
            return
        }
        if (!readAndroidBoolSetting("picture_in_picture_enabled")) {
            return
        }
        val surface = mSurface ?: return
        val width = surface.width.takeIf { it > 0 } ?: return
        val height = surface.height.takeIf { it > 0 } ?: return
        pipSurfaceWidth = width
        pipSurfaceHeight = height
        // Keep the native rendering buffer at the fullscreen size while Android shrinks the Activity
        // into PiP. Letting SurfaceView resize to the tiny PiP window makes SDL/Vulkan rebuild for
        // that small size, which appears as random zoom/crop inside the PiP window and after return.
        surface.holder.setFixedSize(width, height)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(pipAspectRatio(width, height))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(false)
        }
        val params = builder.build()
        runCatching { enterPictureInPictureMode(params) }
    }

    private fun restoreFullscreenAfterPip() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val restore = {
            if (pipSurfaceWidth > 0 && pipSurfaceHeight > 0) {
                mSurface?.holder?.setFixedSize(pipSurfaceWidth, pipSurfaceHeight)
            }
            window.decorView.requestLayout()
            scheduleGameplayDisplayMode()
        }
        window.decorView.post(restore)
        window.decorView.postDelayed(restore, 250L)
    }

    private fun pipAspectRatio(width: Int, height: Int): Rational {
        var w = width.coerceAtLeast(1)
        var h = height.coerceAtLeast(1)
        // Android PiP accepts roughly 1:2.39..2.39:1. Clamp only extreme device/window ratios.
        if (w * 100 > h * 239) {
            w = 239
            h = 100
        } else if (h * 100 > w * 239) {
            w = 100
            h = 239
        }
        val divisor = gcd(w, h)
        return Rational(w / divisor, h / divisor)
    }

    private tailrec fun gcd(a: Int, b: Int): Int =
        if (b == 0) kotlin.math.abs(a).coerceAtLeast(1) else gcd(b, a % b)

    private fun readAndroidBoolSetting(key: String): Boolean {
        return readConfigValue("Android", key)
            ?.equals("true", ignoreCase = true) == true
    }

    private fun readFullscreenMode(): String {
        return readConfigValue("GPU", "full_screen_mode")
            ?: readConfigValue("GPU", "FullscreenMode")
            ?: FULLSCREEN_MODE_BORDERLESS
    }

    private fun readConfigValue(sectionName: String, keyName: String): String? {
        val appRoot = intent.getStringExtra(EXTRA_APP_ROOT).orEmpty().ifBlank {
            "${Environment.getExternalStorageDirectory().absolutePath}/Android/data/$packageName/files/SharpEmu"
        }
        val files = listOf(
            File(appRoot, "config.toml"),
            File(filesDir, "SharpEmu/config.toml"),
        )
        for (file in files) {
            if (!file.isFile) {
                continue
            }
            var section = ""
            var value: String? = null
            runCatching {
                file.forEachLine { raw ->
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) {
                        return@forEachLine
                    }
                    if (line.startsWith("[") && line.endsWith("]")) {
                        section = line.substring(1, line.length - 1)
                        return@forEachLine
                    }
                    val eq = line.indexOf('=')
                    if (section.equals(sectionName, ignoreCase = true) && eq > 0 &&
                        line.substring(0, eq).trim().equals(keyName, ignoreCase = true)) {
                        value = line.substring(eq + 1).trim().trim('"')
                    }
                }
            }
            if (value != null) {
                return value
            }
        }
        return null
    }

    override fun setOrientationBis(w: Int, h: Int, resizable: Boolean, hint: String?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        inputManager.unregisterInputDeviceListener(deviceListener)
        stopLuminanceSampling()
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        stopLuminanceSampling()
        luminanceThread?.quitSafely()
        luminanceThread = null
        luminanceCbHandler = null
        OverlayLuminance.clear()
        viewModelStoreField.clear()
        mSurface?.holder?.removeCallback(highRefreshSurfaceCallback)
        performanceWakeLock?.let { if (it.isHeld) it.release() }
        performanceWakeLock = null
        unregisterDebugPadReceiver()
        super.onDestroy()
    }

    private fun registerDebugPadReceiver() {
        if (debugPadReceiverRegistered) return
        val filter = IntentFilter(ACTION_DEBUG_PAD_PULSE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(debugPadReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(debugPadReceiver, filter)
        }
        debugPadReceiverRegistered = true
    }

    private fun unregisterDebugPadReceiver() {
        if (!debugPadReceiverRegistered) return
        runCatching { unregisterReceiver(debugPadReceiver) }
        debugPadReceiverRegistered = false
    }

    private fun pulseVirtualButton(button: Int, durationMs: Long = 180L) {
        GamePadBridge.setButton(button, true)
        Handler(Looper.getMainLooper()).postDelayed({
            GamePadBridge.setButton(button, false)
        }, durationMs)
    }

    private fun applyMaximumPerformanceMode() {
        val config = File(filesDir, "SharpEmu/config.toml")
        val enabled = runCatching {
            config.readLines().any {
                it.trim().equals("maximum_performance_mode = true", ignoreCase = true)
            }
        }.getOrDefault(false)
        if (!enabled) return

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android's sustained-performance mode may cap peak clocks to reduce heat. For this
            // setting we want the highest short/medium-term performance the OS allows.
            runCatching { window.setSustainedPerformanceMode(false) }
        }
        Log.i(TAG, "Maximum performance mode requested: wake-lock + thread priority, sustained cap disabled")
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        performanceWakeLock = power.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:maximum-performance",
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY) }
        scheduleThreadPriorityBoost(
            delaysMs = listOf(1000L, 3000L, 8000L, 15000L, 30000L),
            priority = Process.THREAD_PRIORITY_DISPLAY,
        )
    }

    private fun applyBaselineGameScheduling() {
        // Keep the game process in the latency-sensitive bucket even without the explicit
        // "maximum performance" toggle. This does not request wakelocks or sustained-mode changes;
        // it only nudges the scheduler priorities the same way other Android emulators do.
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY) }
        scheduleThreadPriorityBoost(
            delaysMs = listOf(250L, 1000L, 3000L, 8000L),
            priority = Process.THREAD_PRIORITY_DISPLAY,
        )
    }

    private fun applyAndroidGamePerformanceHints() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { window.setSustainedPerformanceMode(false) }
        }
        runCatching {
            window.javaClass
                .getMethod("setPreferMinimalPostProcessing", Boolean::class.javaPrimitiveType)
                .invoke(window, true)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        runCatching {
            val gameManager = getSystemService("game") ?: return@runCatching
            val gameStateClass = Class.forName("android.app.GameState")
            val mode = runCatching {
                gameStateClass.getField("MODE_GAMEPLAY_UNINTERRUPTIBLE").getInt(null)
            }.getOrElse {
                runCatching {
                    gameStateClass.getField("MODE_GAMEPLAY_INTERRUPTIBLE").getInt(null)
                }.getOrDefault(0)
            }
            val gameState = gameStateClass.constructors
                .firstOrNull { constructor ->
                    val types = constructor.parameterTypes
                    types.size == 2 &&
                        types[0] == Boolean::class.javaPrimitiveType &&
                        types[1] == Int::class.javaPrimitiveType
                }
                ?.newInstance(false, mode)
                ?: return@runCatching
            gameManager.javaClass.methods
                .firstOrNull { method ->
                    method.name == "setGameState" &&
                        method.parameterTypes.size == 1 &&
                        method.parameterTypes[0].name == "android.app.GameState"
                }
                ?.invoke(gameManager, gameState)
            val currentGameMode = runCatching {
                gameManager.javaClass.methods
                    .firstOrNull { method ->
                        method.name == "getGameMode" && method.parameterTypes.isEmpty()
                    }
                    ?.invoke(gameManager)
            }.getOrNull()
            Log.i(TAG, "Android game performance hints applied: stateMode=$mode currentGameMode=$currentGameMode")
        }.onFailure {
            Log.d(TAG, "Android game performance hints unavailable", it)
        }
    }

    private fun scheduleThreadPriorityBoost(delaysMs: List<Long>, priority: Int) {
        val boostThreads: () -> Unit = {
            File("/proc/self/task").listFiles().orEmpty().forEach { task ->
                val tid = task.name.toIntOrNull() ?: return@forEach
                runCatching { Process.setThreadPriority(tid, priority) }
            }
        }
        val handler = Handler(Looper.getMainLooper())
        delaysMs.forEach { delayMs -> handler.postDelayed(boostThreads, delayMs) }
    }

    // True when at least one real (non-virtual) game controller is connected.
    private fun hasPhysicalController(): Boolean {
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            val sources = device.sources
            val isGamepad = sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
            val isJoystick = sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
            if (isGamepad || isJoystick) {
                return true
            }
        }
        return false
    }

    // Hides the on-screen controls (and their toggle) while a physical controller is connected;
    // restores the toggle when none are. Posted to the overlay so it always runs on the UI thread.
    private fun refreshControlsVisibility() {
        val overlay = overlayView ?: return
        overlay.post {
            val uiText = appUiText(AppearancePrefs.getAppLanguage(this))
            val physical = hasPhysicalController()
            if (physical) {
                overlay.visibility = View.GONE
                controlsToggle?.visibility = View.GONE
                if (!physicalControllerNotified) {
                    physicalControllerNotified = true
                    Toast.makeText(
                        this,
                        uiText.text("Controle físico conectado — controles na tela desativados"),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } else {
                controlsToggle?.visibility = View.VISIBLE
                physicalControllerNotified = false
            }
        }
    }

    private fun logFile(): File = File(getExternalFilesDir(null), "SharpEmu/log/sharpemu_log.txt")

    // Copies the log to public Downloads (so it's reachable without adb) and opens a share sheet.
    private fun exportLogs() {
        val uiText = appUiText(AppearancePrefs.getAppLanguage(this))
        val src = logFile()
        if (!src.exists() || src.length() == 0L) {
            Toast.makeText(this, "${uiText.text("Log não encontrado")} (${src.absolutePath})", Toast.LENGTH_LONG)
                .show()
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = "sharpemu_log_$stamp.txt"

        // Save a copy to Downloads via MediaStore (no storage permission needed on Android 10+).
        var savedTo = ""
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        src.inputStream().use { it.copyTo(out) }
                    }
                    savedTo = "Downloads/$name"
                }
            } else {
                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dst = File(downloads, name)
                src.inputStream().use { input -> dst.outputStream().use { input.copyTo(it) } }
                savedTo = dst.absolutePath
            }
        }.onFailure {
            Toast.makeText(this, "${uiText.text("Falha ao salvar em Downloads")}: ${it.message}", Toast.LENGTH_LONG)
                .show()
        }

        if (savedTo.isNotEmpty()) {
            Toast.makeText(this, "${uiText.text("Log salvo em")} $savedTo", Toast.LENGTH_LONG).show()
        }

        // Offer to share the log to another app (Telegram, Drive, etc.).
        runCatching {
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", src)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, uiText.text("Exportar logs")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }.onFailure {
            Toast.makeText(this, "${uiText.text("Falha ao compartilhar")}: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun getMainFunction(): String = "SDL_main"

    override fun getArguments(): Array<String> {
        val gamePath = intent.getStringExtra(EXTRA_GAME_PATH).orEmpty()
        val appRoot = intent.getStringExtra(EXTRA_APP_ROOT).orEmpty().ifBlank {
            "${Environment.getExternalStorageDirectory().absolutePath}/Android/data/$packageName/files/SharpEmu"
        }
        val cpuBackend = intent.getStringExtra(EXTRA_CPU_BACKEND).orEmpty().ifBlank {
            "x64-aarch64-asmjit"
        }
        val cpuJitSelfCheck = intent.getStringOrBooleanExtra(EXTRA_CPU_JIT_SELF_CHECK)
            .orEmpty()
            .trim()
            .lowercase()
            .ifBlank { "false" }
        val cpuJitDetailedProfile = intent.getStringOrBooleanExtra(EXTRA_CPU_JIT_DETAILED_PROFILE)
            .orEmpty()
            .trim()
            .lowercase()
        val cpuJitDeepDebug = intent.getStringOrBooleanExtra(EXTRA_CPU_JIT_DEEP_DEBUG)
            .orEmpty()
            .trim()
            .lowercase()
        val cpuJitNativeDiagnostics = intent.getStringOrBooleanExtra(EXTRA_CPU_JIT_NATIVE_DIAGNOSTICS)
            .orEmpty()
            .trim()
            .lowercase()
        val cpuJitArm64DisallowFallback =
            intent.getStringOrBooleanExtra(EXTRA_CPU_JIT_ARM64_DISALLOW_FALLBACK)
                .orEmpty()
                .trim()
                .lowercase()
        val cpuX64Arm64GoldenDebug = intent.getStringOrBooleanExtra(EXTRA_CPU_X64ARM64_GOLDEN_DEBUG)
            .orEmpty()
            .trim()
            .lowercase()
        val cpuX64Arm64BootTurbo = intent.getStringOrBooleanExtra(EXTRA_CPU_X64ARM64_BOOT_TURBO)
            .orEmpty()
            .trim()
            .lowercase()
        // Note: --log-append is intentionally omitted so each game launch starts a fresh
        // sharpemu_log.txt. This keeps the exported log limited to a single run, which is essential for
        // measuring JIT coverage accurately (an appended log mixes old and new builds' diagnostics).
        val args = arrayListOf(
            "--android-app-root",
            appRoot,
            "--android-internal-data-root",
            filesDir.absolutePath,
            "--android-native-library-dir",
            applicationInfo.nativeLibraryDir,
            "--cpu-backend",
            cpuBackend,
        )
        if (cpuJitSelfCheck == "true" || cpuJitSelfCheck == "false") {
            args.add("--cpu-jit-self-check")
            args.add(cpuJitSelfCheck)
        }
        if (cpuJitDetailedProfile == "true" || cpuJitDetailedProfile == "false") {
            args.add("--cpu-jit-detailed-profile")
            args.add(cpuJitDetailedProfile)
        }
        if (cpuJitDeepDebug == "true" || cpuJitDeepDebug == "false") {
            args.add("--cpu-jit-deep-debug")
            args.add(cpuJitDeepDebug)
        }
        if (cpuJitNativeDiagnostics == "true" || cpuJitNativeDiagnostics == "false") {
            args.add("--cpu-jit-native-diagnostics")
            args.add(cpuJitNativeDiagnostics)
        }
        if (cpuJitArm64DisallowFallback == "true" || cpuJitArm64DisallowFallback == "false") {
            args.add("--cpu-jit-arm64-disallow-fallback")
            args.add(cpuJitArm64DisallowFallback)
        }
        if (cpuX64Arm64GoldenDebug == "true" || cpuX64Arm64GoldenDebug == "false") {
            args.add("--cpu-x64arm64-golden-debug")
            args.add(cpuX64Arm64GoldenDebug)
        }
        if (cpuX64Arm64BootTurbo == "true" || cpuX64Arm64BootTurbo == "false") {
            args.add("--cpu-x64arm64-boot-turbo")
            args.add(cpuX64Arm64BootTurbo)
        }
        // If a patch file was installed for this game (<appRoot>/patches/<id>.xml), apply it. The
        // core (OnGameLoaded -> ApplyPatchesFromXML) only applies the patches marked isEnabled.
        val gameId = intent.getStringExtra(EXTRA_GAME_ID).orEmpty()
        if (gameId.isNotBlank()) {
            val patch = File(appRoot, "patches/$gameId.xml")
            if (patch.exists()) {
                args.add("--patch")
                args.add(patch.absolutePath)
            }
        }
        args.add("--game")
        args.add(gamePath)
        Log.i(
            TAG,
            "Launching game from intent path='$gamePath' appRoot='$appRoot' cpuBackend='$cpuBackend' selfCheck='$cpuJitSelfCheck' detailedProfile='$cpuJitDetailedProfile' deepDebug='$cpuJitDeepDebug' nativeDiagnostics='$cpuJitNativeDiagnostics' arm64DisallowFallback='$cpuJitArm64DisallowFallback' x64Arm64Golden='$cpuX64Arm64GoldenDebug' x64Arm64BootTurbo='$cpuX64Arm64BootTurbo'",
        )
        return args.toTypedArray()
    }

    private fun Intent.getStringOrBooleanExtra(key: String): String? {
        if (!hasExtra(key)) {
            return null
        }
        return when (val value = extras?.get(key)) {
            is String -> value
            is Boolean -> value.toString()
            else -> value?.toString()
        }
    }

    companion object {
        private const val EXTRA_GAME_PATH = "org.sharpemu.android.extra.GAME_PATH"
        private const val EXTRA_GAME_TITLE = "org.sharpemu.android.extra.GAME_TITLE"
        private const val EXTRA_GAME_ID = "org.sharpemu.android.extra.GAME_ID"
        private const val EXTRA_APP_ROOT = "org.sharpemu.android.extra.APP_ROOT"
        private const val EXTRA_CPU_BACKEND = "org.sharpemu.android.extra.CPU_BACKEND"
        private const val EXTRA_CPU_JIT_SELF_CHECK =
            "org.sharpemu.android.extra.CPU_JIT_SELF_CHECK"
        private const val EXTRA_CPU_JIT_DETAILED_PROFILE =
            "org.sharpemu.android.extra.CPU_JIT_DETAILED_PROFILE"
        private const val EXTRA_CPU_JIT_DEEP_DEBUG =
            "org.sharpemu.android.extra.CPU_JIT_DEEP_DEBUG"
        private const val EXTRA_CPU_JIT_NATIVE_DIAGNOSTICS =
            "org.sharpemu.android.extra.CPU_JIT_NATIVE_DIAGNOSTICS"
        private const val EXTRA_CPU_JIT_ARM64_DISALLOW_FALLBACK =
            "org.sharpemu.android.extra.CPU_JIT_ARM64_DISALLOW_FALLBACK"
        private const val EXTRA_CPU_X64ARM64_GOLDEN_DEBUG =
            "org.sharpemu.android.extra.CPU_X64ARM64_GOLDEN_DEBUG"
        private const val EXTRA_CPU_X64ARM64_BOOT_TURBO =
            "org.sharpemu.android.extra.CPU_X64ARM64_BOOT_TURBO"
        private const val EXTRA_RENDER_DIAG_CAPTURE_ENABLED =
            "org.sharpemu.android.extra.RENDER_DIAG_CAPTURE_ENABLED"
        private const val ACTION_DEBUG_PAD_PULSE = "org.sharpemu.android.DEBUG_PAD_PULSE"
        private const val EXTRA_DEBUG_PAD_BUTTON = "button"
        private const val FULLSCREEN_MODE_WINDOWED = "Windowed"
        private const val FULLSCREEN_MODE_BORDERLESS = "Fullscreen (Borderless)"
        private const val TAG = "GameActivity"

        fun createIntent(
            context: Context,
            game: GameEntry,
            appRoot: String,
            cpuBackend: String,
            cpuJitDetailedProfile: String = "",
            cpuJitDeepDebug: String = "",
            cpuJitNativeDiagnostics: String = "",
            cpuX64Arm64BootTurbo: String = "",
            cpuJitArm64DisallowFallback: String = "",
            renderDiagCaptureEnabled: Boolean = false,
        ): Intent {
            return Intent(context, GameActivity::class.java).apply {
                // Keep the game Activity in the same Android task as the launcher UI. A separate
                // taskAffinity/NEW_TASK makes Android Recents show two SharpEmu entries.
                putExtra(EXTRA_GAME_PATH, game.path)
                putExtra(EXTRA_GAME_TITLE, game.title)
                putExtra(EXTRA_GAME_ID, game.id)
                putExtra(EXTRA_APP_ROOT, appRoot)
                putExtra(EXTRA_CPU_BACKEND, cpuBackend)
                putExtra(EXTRA_CPU_JIT_SELF_CHECK, "false")
                putExtra(EXTRA_CPU_JIT_DETAILED_PROFILE, cpuJitDetailedProfile)
                putExtra(EXTRA_CPU_JIT_DEEP_DEBUG, cpuJitDeepDebug)
                putExtra(EXTRA_CPU_JIT_NATIVE_DIAGNOSTICS, cpuJitNativeDiagnostics)
                putExtra(EXTRA_CPU_JIT_ARM64_DISALLOW_FALLBACK, cpuJitArm64DisallowFallback)
                putExtra(EXTRA_CPU_X64ARM64_GOLDEN_DEBUG, "false")
                putExtra(EXTRA_CPU_X64ARM64_BOOT_TURBO, cpuX64Arm64BootTurbo)
                putExtra(EXTRA_RENDER_DIAG_CAPTURE_ENABLED, renderDiagCaptureEnabled)
            }
        }
    }
}
