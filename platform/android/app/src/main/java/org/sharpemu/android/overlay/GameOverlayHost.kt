// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.sharpemu.android.overlay

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.ui.platform.ComposeView
import org.sharpemu.android.R
import org.sharpemu.android.ui.theme.AppearancePrefs
import org.sharpemu.android.ui.theme.ShadTheme

/**
 * Attaches the SAME touch-controls / precompile-progress Compose overlay GameActivity always drew
 * on top of the render surface — the only thing that changed is *who* hosts the Activity. SDL3 has
 * to own the Android window itself (see SharpEmu.Android's GameActivity.cs, which extends the
 * SDLActivity Java class this project's SDL3 dependency bundles), so the real GameActivity is now a
 * C# class rather than Kotlin — but attaching Compose content on top of an arbitrary host Activity
 * needs nothing Kotlin-specific, so this stays exactly the same UI code, just callable instead of
 * self-hosting.
 */
object GameOverlayHost {
    /** Returned so the caller (GameActivity.cs) can wire up its own eye-toggle / lifecycle hooks. */
    class Handle(val touchOverlay: View, val precompileOverlay: View)

    private const val PREFS_NAME = "PadOverlayPrefs"
    private const val PREF_SHOW = "show_touch_overlay"

    fun attach(activity: Activity, renderDiagCaptureEnabled: Boolean): Handle {
        val touchOverlay = ComposeView(activity).apply {
            setContent {
                ShadTheme {
                    GameTouchControls()
                    if (renderDiagCaptureEnabled) {
                        RenderDiagCaptureButton()
                    }
                }
            }
        }
        val showOverlay = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_SHOW, false)
        touchOverlay.visibility = if (showOverlay) View.VISIBLE else View.GONE
        activity.addContentView(
            touchOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        // Always visible, independent of the toggle above — see PrecompileProgressOverlay's own
        // AnimatedVisibility for why it manages its own show/hide instead.
        val precompileOverlay = ComposeView(activity).apply {
            setContent {
                ShadTheme {
                    PrecompileProgressOverlay(AppearancePrefs.getAppLanguage(activity))
                }
            }
        }
        activity.addContentView(
            precompileOverlay,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )

        attachEyeToggle(activity, touchOverlay, showOverlay)
        return Handle(touchOverlay, precompileOverlay)
    }

    private fun attachEyeToggle(activity: Activity, overlay: View, initiallyShown: Boolean) {
        val density = activity.resources.displayMetrics.density
        val eyeSize = (32 * density).toInt()
        val toggle = ImageView(activity).apply {
            setImageResource(if (initiallyShown) R.drawable.ic_eye else R.drawable.ic_eye_off)
            alpha = 0.4f
            rotation = -35f
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener {
                val show = overlay.visibility != View.VISIBLE
                overlay.visibility = if (show) View.VISIBLE else View.GONE
                setImageResource(if (show) R.drawable.ic_eye else R.drawable.ic_eye_off)
                activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(PREF_SHOW, show)
                    .apply()
            }
        }
        val lp = FrameLayout.LayoutParams(eyeSize, eyeSize).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            val margin = (4 * density).toInt()
            rightMargin = margin
            bottomMargin = margin
        }
        activity.addContentView(toggle, lp)
    }

    /** True when at least one real (non-virtual) game controller is connected — hides the overlay. */
    fun hasPhysicalController(): Boolean {
        for (id in android.view.InputDevice.getDeviceIds()) {
            val device = android.view.InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            val sources = device.sources
            val isGamepad = sources and android.view.InputDevice.SOURCE_GAMEPAD == android.view.InputDevice.SOURCE_GAMEPAD
            val isJoystick = sources and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK
            if (isGamepad || isJoystick) return true
        }
        return false
    }
}
