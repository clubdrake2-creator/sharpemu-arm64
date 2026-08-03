// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File

// On-screen control (touch overlay) preferences, adapted from the PCSX2-Android control settings.
// These are Android-side UI/input preferences (not native emulator config). The in-game PadOverlay
// reads the same "PadOverlayPrefs" store for per-button layout/scale/opacity; the global knobs here
// (show overlay, idle opacity, haptics) live alongside them so the Controls settings screen and the
// overlay stay in sync.
object ControlPrefs {
    private const val PREFS = "PadOverlayPrefs"
    private const val KEY_SHOW_OVERLAY = "show_touch_overlay"
    private const val KEY_HAPTIC = "haptic_feedback"
    private const val KEY_OPACITY = "overlay_idle_opacity" // 0..100

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Whether the on-screen controls are shown by default when a game starts.
    fun getShowOverlay(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_OVERLAY, false)

    fun setShowOverlay(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_OVERLAY, enabled).apply()
    }

    fun getHaptic(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAPTIC, true)

    fun setHaptic(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    // Idle opacity (0..100) of the on-screen buttons. Default 30% matches the overlay's baseline.
    fun getOpacity(context: Context): Int =
        prefs(context).getInt(KEY_OPACITY, 30).coerceIn(10, 100)

    fun setOpacity(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_OPACITY, percent.coerceIn(10, 100)).apply()
    }

    // --- On-screen control behavior (adapted from PCSX2-Android) ---

    private const val KEY_CENTER_ANALOG = "center_analog_on_touch"
    private const val KEY_STICK_REGIONS = "stick_regions_enabled"
    private const val KEY_VIRTUAL_ORDER = "virtual_button_order"

    fun getCenterAnalogOnTouch(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CENTER_ANALOG, true)

    fun setCenterAnalogOnTouch(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CENTER_ANALOG, enabled).apply()
    }

    fun getStickRegionsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STICK_REGIONS, true)

    fun setStickRegionsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STICK_REGIONS, enabled).apply()
    }

    // Order/visibility of the virtual button groups (newline-separated group names).
    val defaultVirtualOrder = listOf(
        "D-Pad", "Botões de ação", "L1/L2", "R1/R2", "L3/R3", "Start/Select",
        "Analógico esquerdo", "Analógico direito",
    )

    fun getVirtualButtonOrder(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_VIRTUAL_ORDER, null) ?: return defaultVirtualOrder
        val list = raw.split('\n').filter { it.isNotBlank() }
        return if (list.isEmpty()) defaultVirtualOrder else list
    }

    fun setVirtualButtonOrder(context: Context, order: List<String>) {
        prefs(context).edit().putString(KEY_VIRTUAL_ORDER, order.joinToString("\n")).apply()
    }

    // --- Physical controllers (per slot 1..4): control type, auto-map flag, action bindings ---

    val controlTypes = listOf(
        "DualShock 4", "DualSense", "Digital Pad", "Analog Joystick", "Nenhum",
    )

    private fun slot(s: Int) = s.coerceIn(1, 4)

    fun getControlType(context: Context, s: Int): String =
        prefs(context).getString("ctype_slot${slot(s)}", if (slot(s) == 1) "DualShock 4" else "Nenhum")
            ?: "DualShock 4"

    fun setControlType(context: Context, s: Int, type: String) {
        prefs(context).edit().putString("ctype_slot${slot(s)}", type).apply()
    }

    fun getAutoMap(context: Context, s: Int): Boolean =
        prefs(context).getBoolean("automap_slot${slot(s)}", false)

    fun setAutoMap(context: Context, s: Int, enabled: Boolean) {
        prefs(context).edit().putBoolean("automap_slot${slot(s)}", enabled).apply()
    }

    fun getBindings(context: Context, s: Int): Map<String, String> {
        val raw = prefs(context).getString("bindings_slot${slot(s)}", "{}") ?: "{}"
        return runCatching {
            val j = JSONObject(raw)
            j.keys().asSequence().associateWith { j.optString(it) }
        }.getOrDefault(emptyMap())
    }

    fun setBinding(context: Context, s: Int, action: String, value: String) {
        val raw = prefs(context).getString("bindings_slot${slot(s)}", "{}") ?: "{}"
        val j = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        if (value.isBlank()) j.remove(action) else j.put(action, value)
        prefs(context).edit().putString("bindings_slot${slot(s)}", j.toString()).apply()
    }

    fun setBindings(context: Context, s: Int, bindings: Map<String, String>) {
        val j = JSONObject()
        bindings.forEach { (k, v) -> if (v.isNotBlank()) j.put(k, v) }
        prefs(context).edit().putString("bindings_slot${slot(s)}", j.toString()).apply()
    }

    // --- Virtual layout editor persistence (ported from PCSX2-Android Prefs) ---
    // Stored under id-keyed JSON so the visual editor is self-consistent. (Positions are normalized
    // 0..1 of the screen; scale/opacity are multipliers.)
    private const val KEY_VB_POSITIONS = "vb_positions"
    private const val KEY_VB_SCALE = "vb_scale"
    private const val KEY_VB_OPACITY = "vb_opacity"
    private const val KEY_VB_SCALE_OV = "vb_scale_overrides"
    private const val KEY_VB_OPACITY_OV = "vb_opacity_overrides"
    private const val KEY_VB_COLOR_OV = "vb_color_overrides"
    private const val KEY_VB_IMAGE_OV = "vb_image_overrides"
    private val DEFAULT_VB_SCALE_OVERRIDES = mapOf(
        "l1" to 1.2365673f,
        "l2" to 1.2365673f,
        "r1" to 1.2365673f,
        "r2" to 1.2365673f,
    )

    fun ensureVirtualLayoutMigration(context: Context) { /* no-op: positions are id-keyed already */ }

    fun getVirtualButtonPositions(context: Context): Map<String, Pair<Float, Float>> {
        val raw = prefs(context).getString(KEY_VB_POSITIONS, "{}") ?: "{}"
        return runCatching {
            val j = JSONObject(raw)
            j.keys().asSequence().mapNotNull { k ->
                val arr = j.optJSONArray(k) ?: return@mapNotNull null
                if (arr.length() < 2) return@mapNotNull null
                k to (arr.optDouble(0).toFloat() to arr.optDouble(1).toFloat())
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    fun setVirtualButtonPositions(context: Context, positions: Map<String, Pair<Float, Float>>) {
        val j = JSONObject()
        positions.forEach { (id, p) ->
            j.put(id, org.json.JSONArray().put(p.first.toDouble()).put(p.second.toDouble()))
        }
        prefs(context).edit().putString(KEY_VB_POSITIONS, j.toString()).apply()
    }

    fun getVirtualButtonScale(context: Context): Float =
        prefs(context).getFloat(KEY_VB_SCALE, 1.0f).coerceIn(0.6f, 1.6f)

    fun setVirtualButtonScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(KEY_VB_SCALE, scale.coerceIn(0.6f, 1.6f)).apply()
    }

    fun getVirtualButtonOpacity(context: Context): Float =
        prefs(context).getFloat(KEY_VB_OPACITY, 0.5f).coerceIn(0.15f, 1.0f)

    fun setVirtualButtonOpacity(context: Context, opacity: Float) {
        prefs(context).edit().putFloat(KEY_VB_OPACITY, opacity.coerceIn(0.15f, 1.0f)).apply()
    }

    // User-chosen tint for the on-screen controls, stored as a packed ARGB int. Defaults to opaque
    // white (0xFFFFFFFF), replacing the old adaptive luminance contrast: the buttons now always render
    // in this fixed color (picked from the in-editor color picker) instead of changing with the
    // background brightness. The same color is used by the in-game overlay and the layout editor.
    private const val KEY_VB_COLOR = "vb_color"
    const val DEFAULT_VB_COLOR: Int = 0xFFFFFFFF.toInt()

    fun getVirtualButtonColor(context: Context): Int =
        prefs(context).getInt(KEY_VB_COLOR, DEFAULT_VB_COLOR)

    fun setVirtualButtonColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_VB_COLOR, color).apply()
    }

    private fun readFloatMap(context: Context, key: String): Map<String, Float> {
        val raw = prefs(context).getString(key, "{}") ?: "{}"
        return runCatching {
            val j = JSONObject(raw)
            j.keys().asSequence().associateWith { j.optDouble(it).toFloat() }
        }.getOrDefault(emptyMap())
    }

    private fun writeFloatMap(context: Context, key: String, map: Map<String, Float>) {
        val j = JSONObject()
        map.forEach { (k, v) -> j.put(k, v.toDouble()) }
        prefs(context).edit().putString(key, j.toString()).apply()
    }

    fun getVirtualButtonScaleOverrides(context: Context): Map<String, Float> =
        readFloatMap(context, KEY_VB_SCALE_OV).ifEmpty { DEFAULT_VB_SCALE_OVERRIDES }

    fun setVirtualButtonScaleOverrides(context: Context, overrides: Map<String, Float>) =
        writeFloatMap(context, KEY_VB_SCALE_OV, overrides)

    fun getVirtualButtonOpacityOverrides(context: Context): Map<String, Float> =
        readFloatMap(context, KEY_VB_OPACITY_OV)

    fun setVirtualButtonOpacityOverrides(context: Context, overrides: Map<String, Float>) =
        writeFloatMap(context, KEY_VB_OPACITY_OV, overrides)

    private fun readIntMap(context: Context, key: String): Map<String, Int> {
        val raw = prefs(context).getString(key, "{}") ?: "{}"
        return runCatching {
            val j = JSONObject(raw)
            j.keys().asSequence().associateWith { j.optInt(it) }
        }.getOrDefault(emptyMap())
    }

    private fun writeIntMap(context: Context, key: String, map: Map<String, Int>) {
        val j = JSONObject()
        map.forEach { (k, v) -> if (k.isNotBlank()) j.put(k, v) }
        prefs(context).edit().putString(key, j.toString()).apply()
    }

    fun getVirtualButtonColorOverrides(context: Context): Map<String, Int> =
        readIntMap(context, KEY_VB_COLOR_OV)

    fun setVirtualButtonColorOverrides(context: Context, overrides: Map<String, Int>) =
        writeIntMap(context, KEY_VB_COLOR_OV, overrides)

    private fun readStringMap(context: Context, key: String): Map<String, String> {
        val raw = prefs(context).getString(key, "{}") ?: "{}"
        return runCatching {
            val j = JSONObject(raw)
            j.keys().asSequence().mapNotNull { k ->
                val value = j.optString(k)
                if (k.isBlank() || value.isBlank()) null else k to value
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun writeStringMap(context: Context, key: String, map: Map<String, String>) {
        val j = JSONObject()
        map.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) j.put(k, v) }
        prefs(context).edit().putString(key, j.toString()).apply()
    }

    private fun customButtonDir(context: Context): File =
        File(context.filesDir, "virtual_buttons").apply { mkdirs() }

    private fun safeButtonId(id: String): String =
        id.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { "button" }

    fun getVirtualButtonImageOverrides(context: Context): Map<String, String> =
        readStringMap(context, KEY_VB_IMAGE_OV).filterValues { File(it).isFile }

    fun setVirtualButtonImageOverride(context: Context, id: String, sourceUri: Uri): String? {
        val safeId = safeButtonId(id)
        val target = File(customButtonDir(context), "$safeId.png")
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            val overrides = getVirtualButtonImageOverrides(context).toMutableMap()
            overrides[id] = target.absolutePath
            writeStringMap(context, KEY_VB_IMAGE_OV, overrides)
            target.absolutePath
        }.getOrNull()
    }

    fun clearVirtualButtonImageOverride(context: Context, id: String) {
        val overrides = getVirtualButtonImageOverrides(context).toMutableMap()
        overrides.remove(id)?.let { runCatching { File(it).delete() } }
        writeStringMap(context, KEY_VB_IMAGE_OV, overrides)
    }

    // Default auto-map (PS4 action -> SDL-ish input token). Adapted from the PCSX2 template.
    val autoMapTemplate: Map<String, String> = mapOf(
        "left_stick_up" to "AxisY-", "left_stick_right" to "AxisX+",
        "left_stick_down" to "AxisY+", "left_stick_left" to "AxisX-",
        "right_stick_up" to "AxisRY-", "right_stick_right" to "AxisRX+",
        "right_stick_down" to "AxisRY+", "right_stick_left" to "AxisRX-",
        "dpad_up" to "DirectionUp", "dpad_right" to "DirectionRight",
        "dpad_down" to "DirectionDown", "dpad_left" to "DirectionLeft",
        "triangle" to "ButtonY", "circle" to "ButtonB", "cross" to "ButtonA", "square" to "ButtonX",
        "l1" to "ButtonL1", "l2" to "AxisLTrigger", "r1" to "ButtonR1", "r2" to "AxisRTrigger",
        "start" to "ButtonStart", "select" to "ButtonSelect",
        "left_stick_click" to "ButtonThumbLeft", "right_stick_click" to "ButtonThumbRight",
    )
}
