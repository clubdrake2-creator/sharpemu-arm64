// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.ui.theme

import android.content.Context
import android.os.Build
import java.util.Locale

// Theme mode chosen by the user in the Appearance settings section.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage { PORTUGUESE, ENGLISH, SPANISH }

// Home library layout (portrait). GRID = 2-column cards with the title below the cover; LIST = rows;
// GRID_COMPACT = denser 3-column grid whose title is overlaid on the bottom of the cover (no caption
// line below). In landscape the home always switches to the console-style carousel home regardless of
// this setting.
enum class ViewMode { GRID, LIST, GRID_COMPACT }

// Small persistent store for the app's visual appearance preferences (theme mode and Material You
// dynamic colors). These are pure Android-side UI preferences, so they live in SharedPreferences
// rather than in the native emulator config.
object AppearancePrefs {
    private const val PREFS = "appearance"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_MATERIAL_YOU = "material_you"
    private const val KEY_USE_GAME_BACKGROUND = "use_game_background"
    private const val KEY_APP_LANGUAGE = "app_language"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): ThemeMode =
        when (prefs(context).getString(KEY_MODE, ThemeMode.LIGHT.name)) {
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.LIGHT
        }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getMaterialYou(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MATERIAL_YOU, false)

    fun setMaterialYou(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MATERIAL_YOU, enabled).apply()
    }

    fun getUseGameBackground(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_GAME_BACKGROUND, false)

    fun setUseGameBackground(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_GAME_BACKGROUND, enabled).apply()
    }

    fun getAppLanguage(context: Context): AppLanguage {
        val saved = prefs(context).getString(KEY_APP_LANGUAGE, null)
        if (saved != null) {
            return runCatching { AppLanguage.valueOf(saved) }.getOrDefault(AppLanguage.ENGLISH)
        }
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale ?: Locale.getDefault()
        }
        return when (locale.language.lowercase(Locale.US)) {
            "pt" -> AppLanguage.PORTUGUESE
            "es" -> AppLanguage.SPANISH
            else -> AppLanguage.ENGLISH
        }
    }

    fun setAppLanguage(context: Context, language: AppLanguage) {
        prefs(context).edit().putString(KEY_APP_LANGUAGE, language.name).apply()
    }

    private const val KEY_VIEW_MODE = "view_mode"

    fun getViewMode(context: Context): ViewMode =
        when (prefs(context).getString(KEY_VIEW_MODE, ViewMode.GRID.name)) {
            ViewMode.LIST.name -> ViewMode.LIST
            ViewMode.GRID_COMPACT.name -> ViewMode.GRID_COMPACT
            else -> ViewMode.GRID
        }

    fun setViewMode(context: Context, mode: ViewMode) {
        prefs(context).edit().putString(KEY_VIEW_MODE, mode.name).apply()
    }
}
