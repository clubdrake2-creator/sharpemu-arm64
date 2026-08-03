// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.sharpemu.android.core.NativeBridge
import org.sharpemu.android.model.GameDetails
import org.sharpemu.android.model.GameEntry
import org.sharpemu.android.model.GpuDriverEntry
import org.sharpemu.android.model.CheatEntry
import org.sharpemu.android.model.CheatInfo
import org.sharpemu.android.model.PatchEntry
import org.sharpemu.android.model.PatchInfo
import org.sharpemu.android.model.RemoteGpuDriver
import org.sharpemu.android.model.RepoPatch
import org.sharpemu.android.model.SfoEntry
import org.sharpemu.android.model.PkgInstallResult
import org.sharpemu.android.model.SettingEntry
import org.sharpemu.android.model.SettingsSnapshot
import org.sharpemu.android.model.TrophyEntry
import org.sharpemu.android.model.TrophyInfo
import java.util.Locale

private val LatinAmericanSpanishCountries = setOf(
    "AR", "BO", "CL", "CO", "CR", "CU", "DO", "EC", "GT", "HN", "MX", "NI", "PA",
    "PE", "PR", "PY", "SV", "US", "UY", "VE",
)

private val SimplifiedChineseCountries = setOf("CN", "SG", "MY")

class EmulatorRepository(private val context: Context) {
    fun initialize(): String {
        // On Android 11+ (scoped storage) an app cannot create its own
        // /storage/emulated/0/Android/data/<pkg> directory through raw filesystem calls; only
        // the framework can provision it. The native InitializeAppHost() builds that path from
        // the external storage root below and calls create_directories() on it, which fails with
        // EACCES on a fresh install (the directory only "happened to exist" before because it
        // had been provisioned by an earlier install and survived reinstalls). Calling
        // getExternalFilesDir() here forces the framework to create
        // /storage/emulated/0/Android/data/<pkg>/files with the correct ownership first, so the
        // native directory creation succeeds on a clean install.
        context.getExternalFilesDir(null)
        val result = NativeBridge.initialize(
            context.packageName,
            Environment.getExternalStorageDirectory().absolutePath,
            context.filesDir.absolutePath,
        )
        return result
    }

    fun getAppRoot(): String {
        return NativeBridge.getAppRoot()
    }

    fun loadGames(): List<GameEntry> {
        val array = JSONArray(NativeBridge.getGames())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    GameEntry(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        path = item.optString("path"),
                        source = item.optString("source"),
                        icon = item.optString("icon"),
                        version = item.optString("version"),
                        sizeBytes = item.optLong("sizeBytes"),
                    ),
                )
            }
        }
    }

    fun loadSettings(): SettingsSnapshot {
        var root = JSONObject(NativeBridge.getSettings())
        if (applyDeviceConsoleLanguageIfNeeded(root)) {
            root = JSONObject(NativeBridge.getSettings())
        }
        val settings = root.getJSONArray("settings")
        return SettingsSnapshot(
            root = root.optString("root"),
            settings = buildList {
                for (index in 0 until settings.length()) {
                    val item = settings.getJSONObject(index)
                    add(
                        SettingEntry(
                            section = item.optString("section"),
                            key = item.optString("key"),
                            type = item.optString("type"),
                            value = item.optString("value"),
                            locked = item.optBoolean("locked"),
                            default = item.optString("default"),
                        ),
                    )
                }
            },
        )
    }

    fun updateSetting(setting: SettingEntry, value: String): Boolean {
        if (setting.section == "GPU" && setting.key == "full_screen_mode") {
            val fullScreen = (!value.equals("Windowed", ignoreCase = true)).toString()
            NativeBridge.updateSetting(setting.section, "full_screen", fullScreen)
        }
        return NativeBridge.updateSetting(setting.section, setting.key, value)
    }

    private fun applyDeviceConsoleLanguageIfNeeded(root: JSONObject): Boolean {
        val prefs = context.getSharedPreferences("AndroidConsoleLanguagePrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("device_language_applied", false)) {
            return false
        }
        val settings = root.optJSONArray("settings") ?: return false
        for (index in 0 until settings.length()) {
            val item = settings.optJSONObject(index) ?: continue
            if (item.optString("section") != "General" || item.optString("key") != "console_language") {
                continue
            }
            val value = item.optString("value")
            val default = item.optString("default")
            if (value.isNotBlank() && default.isNotBlank() && value != default) {
                prefs.edit().putBoolean("device_language_applied", true).apply()
                return false
            }
            val deviceLanguage = consoleLanguageForDeviceLocale().toString()
            val updated = NativeBridge.updateSetting("General", "console_language", deviceLanguage)
            if (updated) {
                prefs.edit().putBoolean("device_language_applied", true).apply()
            }
            return updated
        }
        return false
    }

    private fun consoleLanguageForDeviceLocale(): Int {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale ?: Locale.getDefault()
        }
        val language = locale.language.lowercase(Locale.US)
        val country = locale.country.uppercase(Locale.US)
        return when (language) {
            "ja" -> 0
            "en" -> if (country == "GB") 18 else 1
            "fr" -> if (country == "CA") 22 else 2
            "es" -> if (country in LatinAmericanSpanishCountries) 20 else 3
            "de" -> 4
            "it" -> 5
            "nl" -> 6
            "pt" -> if (country == "BR") 17 else 7
            "ru" -> 8
            "ko" -> 9
            "zh" -> if (country in SimplifiedChineseCountries) 11 else 10
            "fi" -> 12
            "sv" -> 13
            "da" -> 14
            "no", "nb" -> 15
            "pl" -> 16
            "tr" -> 19
            "ar" -> 21
            "cs" -> 23
            "hu" -> 24
            "el" -> 25
            "ro" -> 26
            "th" -> 27
            "vi" -> 28
            "id", "in" -> 29
            "uk" -> 30
            else -> 1
        }
    }

    // Full param.sfo-derived details for a game (firmware, region, content id, all SFO entries).
    fun loadGameDetails(gameId: String): GameDetails {
        val root = JSONObject(NativeBridge.getGameDetails(gameId))
        val sfoArray = root.optJSONArray("sfo")
        val sfo = buildList {
            if (sfoArray != null) {
                for (i in 0 until sfoArray.length()) {
                    val item = sfoArray.getJSONObject(i)
                    add(
                        SfoEntry(
                            key = item.optString("key"),
                            value = item.optString("value"),
                            isInt = item.optBoolean("isInt"),
                        ),
                    )
                }
            }
        }
        return GameDetails(
            firmware = root.optString("firmware"),
            region = root.optString("region"),
            contentId = root.optString("contentId"),
            titleId = root.optString("titleId"),
            appVer = root.optString("appVer"),
            sfo = sfo,
        )
    }

    // Trophy list (definitions + extracted icon paths) parsed from the game's trophy00.trp.
    fun loadTrophyInfo(gameId: String): TrophyInfo {
        val root = JSONObject(NativeBridge.getTrophyInfo(gameId))
        val arr = root.optJSONArray("trophies")
        val trophies = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        TrophyEntry(
                            id = item.optInt("id"),
                            grade = item.optString("grade"),
                            hidden = item.optBoolean("hidden"),
                            name = item.optString("name"),
                            detail = item.optString("detail"),
                            icon = item.optString("icon"),
                            unlocked = item.optBoolean("unlocked"),
                        ),
                    )
                }
            }
        }
        return TrophyInfo(
            available = root.optBoolean("available"),
            reason = root.optString("reason"),
            trophies = trophies,
        )
    }

    // Patches installed for a game (<appRoot>/patches/<id>.xml), with each patch's enabled state.
    fun loadPatchInfo(gameId: String): PatchInfo {
        val root = JSONObject(NativeBridge.getPatchInfo(gameId))
        val arr = root.optJSONArray("patches")
        val patches = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    add(
                        PatchEntry(
                            name = item.optString("name"),
                            author = item.optString("author"),
                            appVer = item.optString("appVer"),
                            enabled = item.optBoolean("enabled"),
                        ),
                    )
                }
            }
        }
        return PatchInfo(
            available = root.optBoolean("available"),
            path = root.optString("path"),
            patches = patches,
        )
    }

    fun setPatchEnabled(gameId: String, patchName: String, enabled: Boolean): Boolean =
        NativeBridge.setPatchEnabled(gameId, patchName, enabled)

    // Lists the patch XMLs available in the official shadps4 ps4_cheats repo (PATCHES folder).
    fun fetchRepoPatches(): List<RepoPatch> {
        val json = httpGet("https://api.github.com/repos/shadps4-emu/ps4_cheats/contents/PATCHES")
            ?: return emptyList()
        val arr = JSONArray(json)
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (item.optString("type") == "file" && item.optString("name").endsWith(".xml")) {
                    add(RepoPatch(item.optString("name"), item.optString("download_url")))
                }
            }
        }
    }

    // Downloads a repo patch XML and installs it as <appRoot>/patches/<id>.xml. Returns true on ok.
    fun downloadPatch(gameId: String, downloadUrl: String): Boolean {
        val xml = httpGet(downloadUrl) ?: return false
        return writePatchFile(gameId, xml.toByteArray())
    }

    // Imports a patch XML the user picked from device storage into <appRoot>/patches/<id>.xml.
    fun importPatch(gameId: String, uri: Uri): Boolean {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return false
        return writePatchFile(gameId, bytes)
    }

    private fun writePatchFile(gameId: String, bytes: ByteArray): Boolean = runCatching {
        val dir = java.io.File(getAppRoot(), "patches")
        dir.mkdirs()
        java.io.File(dir, "$gameId.xml").writeBytes(bytes)
        true
    }.getOrDefault(false)

    // --- Cheats ---

    // Downloads the cheat JSON for a game from the ps4_cheats repo CHEATS folder. Files are named
    // <TITLEID>_<version>.json; we take the first match for the title id. Returns true on success.
    fun downloadCheat(gameId: String): Boolean {
        val listJson =
            httpGet("https://api.github.com/repos/shadps4-emu/ps4_cheats/contents/CHEATS")
                ?: return false
        val arr = JSONArray(listJson)
        var url: String? = null
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val name = item.optString("name")
            if (name.startsWith("${gameId}_") && name.endsWith(".json")) {
                url = item.optString("download_url")
                break
            }
        }
        val downloadUrl = url ?: return false
        val json = httpGet(downloadUrl) ?: return false
        return runCatching {
            val dir = java.io.File(getAppRoot(), "cheats")
            dir.mkdirs()
            java.io.File(dir, "$gameId.json").writeText(json)
            true
        }.getOrDefault(false)
    }

    // Loads + parses the installed cheat JSON (cheats/<id>.json) into togglable cheats. Each mod's
    // memory entries become "ADDR=VALUE" lines (ADDR = offset + 0x400000, the module-base offset the
    // patch engine expects; VALUE = the "on" byte sequence). Enabled state comes from the native side.
    fun loadCheats(gameId: String): CheatInfo {
        val file = java.io.File(getAppRoot(), "cheats/$gameId.json")
        if (!file.exists()) return CheatInfo(available = false, credits = "", cheats = emptyList())
        val root = runCatching { JSONObject(file.readText()) }.getOrNull()
            ?: return CheatInfo(available = false, credits = "", cheats = emptyList())
        val enabledNames = runCatching {
            val a = JSONArray(NativeBridge.getEnabledCheats(gameId))
            buildSet { for (i in 0 until a.length()) add(a.getString(i)) }
        }.getOrDefault(emptySet())
        val mods = root.optJSONArray("mods")
        val cheats = buildList {
            if (mods != null) {
                for (i in 0 until mods.length()) {
                    val mod = mods.getJSONObject(i)
                    val name = mod.optString("name")
                    val memory = mod.optJSONArray("memory") ?: continue
                    val sb = StringBuilder()
                    for (j in 0 until memory.length()) {
                        val mem = memory.getJSONObject(j)
                        val offset = mem.optString("offset")
                        val on = mem.optString("on")
                        val addr = runCatching {
                            (offset.toLong(16) + 0x400000L).toString(16).uppercase()
                        }.getOrNull() ?: continue
                        if (on.isBlank()) continue
                        sb.append(addr).append('=').append(on.uppercase()).append('\n')
                    }
                    if (sb.isEmpty()) continue
                    add(
                        CheatEntry(
                            name = name,
                            type = mod.optString("type"),
                            lines = sb.toString(),
                            enabled = enabledNames.contains(name),
                        ),
                    )
                }
            }
        }
        val credits = root.optString("credits").ifBlank { root.optString("master") }
        return CheatInfo(available = true, credits = credits, cheats = cheats)
    }

    fun setCheatEnabled(gameId: String, cheat: CheatEntry, enabled: Boolean): Boolean =
        NativeBridge.setCheatEnabled(gameId, cheat.name, enabled, cheat.lines)

    private fun httpGet(url: String): String? = runCatching {
        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "SharpEmu-Android")
            setRequestProperty("Accept", "application/vnd.github.v3+json, */*")
        }
        conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    // Per-game configuration overrides (mirrors SharpEmu PC per-game config).
    fun loadGameSettings(gameId: String): SettingsSnapshot {
        val root = JSONObject(NativeBridge.getGameSettings(gameId))
        val settings = root.getJSONArray("settings")
        return SettingsSnapshot(
            root = root.optString("id"),
            settings = buildList {
                for (index in 0 until settings.length()) {
                    val item = settings.getJSONObject(index)
                    add(
                        SettingEntry(
                            section = item.optString("section"),
                            key = item.optString("key"),
                            type = item.optString("type"),
                            value = item.optString("value"),
                            locked = item.optBoolean("locked"),
                            default = item.optString("default"),
                        ),
                    )
                }
            },
        )
    }

    fun updateGameSetting(gameId: String, setting: SettingEntry, value: String): Boolean {
        return NativeBridge.updateGameSetting(gameId, setting.section, setting.key, value)
    }


     fun addGameFolder(uri: Uri): Boolean {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Game"
    val pathForNative = resolveTreeUriToPath(uri) ?: return false

    return NativeBridge.addGameFolder(name, pathForNative)
}

// Adds every PS4 game found under the picked tree. Covers the three cases the UI offers: the picked
// folder is itself a game (added as one), it is a parent folder that holds several game folders, or it
// is a root folder containing many game folders — all of them get added. It scans a few levels deep
// and stops descending as soon as a folder is recognised as a game, so nested layouts such as
// <root>/<region>/<game> also work. Returns how many games were added to the library.
fun addGameFolders(uri: Uri): Int {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
    val rootPath = resolveTreeUriToPath(uri) ?: return 0
    val games = mutableListOf<java.io.File>()
    collectGameFolders(java.io.File(rootPath), 0, games)
    var added = 0
    for (g in games) {
        if (NativeBridge.addGameFolder(g.name, g.absolutePath)) added++
    }
    return added
}

// A PS4 game folder is identified by its eboot.bin (or the sce_sys/param.sfo metadata).
private fun isPs4GameFolder(dir: java.io.File): Boolean =
    java.io.File(dir, "eboot.bin").exists() || java.io.File(dir, "sce_sys/param.sfo").exists()

private fun collectGameFolders(dir: java.io.File, depth: Int, out: MutableList<java.io.File>) {
    if (!dir.isDirectory || depth > 4) {
        return
    }
    if (isPs4GameFolder(dir)) {
        out.add(dir)
        return // it's a game — don't descend into its internal sce_sys/etc.
    }
    val children = dir.listFiles() ?: return
    for (child in children) {
        if (child.isDirectory) {
            collectGameFolders(child, depth + 1, out)
        }
    }
}

private fun resolveTreeUriToPath(uri: Uri): String? = runCatching {
    if (uri.authority != "com.android.externalstorage.documents") return null

    val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)

    // aparelhos retornam "raw:/storage/XXXX-XXXX/..."
    if (docId.startsWith("raw:", ignoreCase = true)) {
        val raw = docId.removePrefix("raw:")
        return raw.takeIf { java.io.File(it).exists() }
    }

    val parts = docId.split(":", limit = 2)
    val volume = parts[0]
    val relative = parts.getOrElse(1) { "" }.trimStart('/')

    val bases = buildList {
        if (volume.equals("primary", ignoreCase = true)) {
            add(android.os.Environment.getExternalStorageDirectory().absolutePath)
            add("/storage/emulated/0")
        } else {
            add("/storage/$volume")
            add("/mnt/media_rw/$volume")
        }
    }.distinct()

    for (base in bases) {
        val full = if (relative.isBlank()) base else java.io.File(base, relative).absolutePath
        if (java.io.File(full).exists()) return full
    }

    null
}.getOrNull()

private fun resolveSingleUriToPath(uri: Uri): String? = runCatching {
    if (uri.scheme == "file") {
        return uri.path?.takeIf { java.io.File(it).exists() }
    }
    if (uri.authority != "com.android.externalstorage.documents") return null

    val docId = android.provider.DocumentsContract.getDocumentId(uri)
    if (docId.startsWith("raw:", ignoreCase = true)) {
        val raw = docId.removePrefix("raw:")
        return raw.takeIf { java.io.File(it).exists() }
    }

    val parts = docId.split(":", limit = 2)
    val volume = parts[0]
    val relative = parts.getOrElse(1) { "" }.trimStart('/')
    val bases = buildList {
        if (volume.equals("primary", ignoreCase = true)) {
            add(android.os.Environment.getExternalStorageDirectory().absolutePath)
            add("/storage/emulated/0")
        } else {
            add("/storage/$volume")
            add("/mnt/media_rw/$volume")
        }
    }.distinct()

    for (base in bases) {
        val full = if (relative.isBlank()) base else java.io.File(base, relative).absolutePath
        if (java.io.File(full).exists()) return full
    }
    null
}.getOrNull()

   /* fun addGameFolder(uri: Uri): Boolean {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Game"
        // The native core reads games via raw filesystem paths (std::filesystem/fopen) and cannot
        // open a content:// SAF URI, so resolve the picked tree URI to its real on-disk path (needs
        // MANAGE_EXTERNAL_STORAGE, requested in MainActivity). Fall back to the URI string only if it
        // can't be mapped (e.g. a cloud DocumentsProvider).
        val pathForNative = resolveTreeUriToPath(uri) ?: uri.toString()
        return NativeBridge.addGameFolder(name, pathForNative)
    }

    // Converts a Storage Access Framework tree URI from the external-storage documents provider to an
    // absolute filesystem path (e.g. .../tree/primary%3AGames -> /storage/emulated/0/Games, or an SD
    // card volume -> /storage/<volume>/...). Returns null for providers we can't map or when the
    // resolved path doesn't exist on disk.
    private fun resolveTreeUriToPath(uri: Uri): String? = runCatching {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val parts = docId.split(":", limit = 2)
        val volume = parts[0]
        val relative = parts.getOrElse(1) { "" }
        val base = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            "/storage/$volume"
        }
        val full = if (relative.isEmpty()) base else "$base/$relative"
        if (java.io.File(full).exists()) full else null
    }.getOrNull() */

    fun installPkg(uri: Uri, installRootUri: Uri? = null): PkgInstallResult {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val installRootPath = installRootUri?.let { rootUri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    rootUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            resolveTreeUriToPath(rootUri)
                ?: return PkgInstallResult(false, "Cannot resolve selected install folder", "")
        }.orEmpty()
        val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "selected.pkg"
        val sourcePath = resolveSingleUriToPath(uri).orEmpty()
        val fd = if (sourcePath.isEmpty()) {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return PkgInstallResult(false, "Cannot open selected PKG", "")
            pfd.detachFd()
        } else {
            -1
        }
        // The native PKG install (copy + decrypt + extract via std::filesystem and miniz) is a deep
        // call chain. Coroutine dispatcher pool threads have a small stack and overflow inside it
        // (SIGSEGV in std::filesystem::path), so run the native call on a dedicated thread with a
        // large 16 MB stack and join it.
        return runNativeOnBigStack("pkg-install") {
            val result = JSONObject(NativeBridge.installPkg(name, fd, sourcePath, installRootPath))
            PkgInstallResult(
                ok = result.optBoolean("ok"),
                message = result.optString("message"),
                path = result.optString("path"),
            )
        }
    }

    // Runs `block` on a dedicated 16 MB-stack thread and returns its result, so heavy native work
    // doesn't overflow the small coroutine-pool thread stacks.
    private fun <T> runNativeOnBigStack(name: String, block: () -> T): T {
        var result: Result<T>? = null
        val worker = Thread(null, {
            result = runCatching(block)
        }, name, 16L * 1024 * 1024)
        worker.start()
        worker.join()
        return result!!.getOrThrow()
    }

    fun getInstallProgress(): Int = NativeBridge.getInstallProgress()

    fun cancelInstallPkg() = NativeBridge.cancelInstallPkg()

    fun getDeleteProgress(): Int = NativeBridge.getDeleteProgress()

    fun deleteGame(gameId: String, forceDeleteFolder: Boolean = false): Boolean =
        NativeBridge.deleteGame(gameId, forceDeleteFolder)

    fun loadGpuDrivers(): List<GpuDriverEntry> {
        val array = JSONArray(NativeBridge.getGpuDrivers())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id")
                add(
                    GpuDriverEntry(
                        id = id,
                        title = item.optString("title"),
                        active = item.optBoolean("active"),
                        subtitle = if (id == "system" || id.isBlank()) "" else readDriverMeta(id),
                    ),
                )
            }
        }
    }

    // Reads an installed driver's meta.json (AdrenoTools format, under filesDir/SharpEmu/gpu_drivers/
    // <id>) and builds a short "vendor • driverVersion" or description subtitle. Blank if unavailable.
    private fun readDriverMeta(id: String): String = runCatching {
        val meta = java.io.File(context.filesDir, "SharpEmu/gpu_drivers/$id/meta.json")
        if (!meta.exists()) return ""
        val j = JSONObject(meta.readText())
        val vendor = j.optString("vendor")
        val version = j.optString("driverVersion").ifBlank { j.optString("packageVersion") }
        val desc = j.optString("description")
        when {
            vendor.isNotBlank() && version.isNotBlank() -> "$vendor • v$version"
            version.isNotBlank() -> "v$version"
            desc.isNotBlank() -> desc
            else -> ""
        }
    }.getOrDefault("")

    fun selectGpuDriver(driver: GpuDriverEntry): Boolean {
        return NativeBridge.selectGpuDriver(driver.id)
    }

    // After installing/downloading a driver, reload the list and auto-activate the freshly installed
    // one (matched by the install path's folder name; falls back to the newest non-system driver).
    // Returns the refreshed list so the UI shows it as present + active.
    fun activateInstalledDriver(installPath: String): List<GpuDriverEntry> {
        val id = installPath.trimEnd('/').substringAfterLast('/')
        val drivers = loadGpuDrivers()
        val target = drivers.firstOrNull { it.id == id }
            ?: drivers.lastOrNull { it.id != "system" }
        if (target != null) {
            NativeBridge.selectGpuDriver(target.id)
        }
        return loadGpuDrivers()
    }

    // Lists downloadable Adreno/Turnip drivers from every configured channel (see
    // DriverChannelPrefs), merging the results. Each channel is an independent HTTP call, so one
    // dead/unreachable channel doesn't affect the others.
    fun fetchRemoteGpuDriversFromChannels(channels: List<String>): List<RemoteGpuDriver> =
        channels.flatMap { fetchRemoteGpuDrivers(it) }

    // Parses a GitHub "releases" URL (e.g. "https://github.com/OWNER/REPO/releases" or with a
    // trailing slash) into the GitHub API URL for that repo's releases, or null if it doesn't look
    // like a GitHub repo URL at all.
    private fun githubReleasesApiUrl(channelUrl: String): String? {
        val match = Regex("""github\.com/([^/]+)/([^/]+)""").find(channelUrl) ?: return null
        val (owner, repo) = match.destructured
        return "https://api.github.com/repos/$owner/${repo.removeSuffix("/")}/releases"
    }

    // Lists downloadable Adreno/Turnip drivers from one GitHub releases channel (the same source
    // PCSX2-Android's custom-driver downloader uses, generalized to any GitHub repo). Each release
    // asset ending in .zip is a driver package.
    fun fetchRemoteGpuDrivers(channelUrl: String): List<RemoteGpuDriver> {
        val apiUrl = githubReleasesApiUrl(channelUrl) ?: return emptyList()
        // Replicates PCSX2-Android's AdrenoDriverRepository.fetchRemoteDrivers exactly: read the
        // response code, fall back to errorStream on failure, and accept .zip or .so assets. Uses its
        // own connection (not the generic httpGet) so non-2xx responses are logged instead of silently
        // becoming an empty list.
        var connection: java.net.HttpURLConnection? = null
        return runCatching {
            connection = (java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "SharpEmu-Android/CustomDrivers")
            }
            val code = connection?.responseCode ?: -1
            if (code !in 200..299) {
                val err = connection?.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                android.util.Log.w("SharpEmu", "GPU driver fetch HTTP $code: ${err.take(200)}")
                return@runCatching emptyList<RemoteGpuDriver>()
            }
            val jsonText =
                connection?.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val releases = JSONArray(jsonText)
            val out = ArrayList<RemoteGpuDriver>()
            for (i in 0 until releases.length()) {
                val release = releases.optJSONObject(i) ?: continue
                val tag = release.optString("name").ifBlank { release.optString("tag_name") }
                val assets = release.optJSONArray("assets") ?: continue
                for (j in 0 until assets.length()) {
                    val asset = assets.optJSONObject(j) ?: continue
                    val name = asset.optString("name").trim()
                    val lower = name.lowercase()
                    if (!lower.endsWith(".zip") && !lower.endsWith(".so")) continue
                    val url = asset.optString("browser_download_url")
                    if (url.isBlank()) continue
                    out.add(RemoteGpuDriver(if (tag.isNotBlank()) "$tag — $name" else name, url))
                }
            }
            out
        }.getOrElse {
            android.util.Log.w("SharpEmu", "GPU driver fetch failed: ${it.message}")
            emptyList()
        }.also { connection?.disconnect() }
    }

    // Downloads a remote driver .zip and installs it via the native installer (same path as a
    // file-picked driver). Returns the install result.
    fun downloadGpuDriver(remote: RemoteGpuDriver): PkgInstallResult {
        val dir = java.io.File(context.cacheDir, "driver_dl").apply { mkdirs() }
        val safeName = remote.name.substringAfterLast(' ').ifBlank { "driver.zip" }
        val dest = java.io.File(dir, safeName)
        val ok = runCatching {
            (java.net.URL(remote.downloadUrl).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "SharpEmu-Android")
            }.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
            true
        }.getOrDefault(false)
        if (!ok || !dest.exists()) {
            return PkgInstallResult(false, "Falha ao baixar o driver", "")
        }
        val pfd = android.os.ParcelFileDescriptor.open(
            dest, android.os.ParcelFileDescriptor.MODE_READ_ONLY,
        )
        val fd = pfd.detachFd()
        return runNativeOnBigStack("driver-download-install") {
            val result = JSONObject(NativeBridge.installGpuDriver(safeName, fd))
            PkgInstallResult(
                ok = result.optBoolean("ok"),
                message = result.optString("message"),
                path = result.optString("path"),
            )
        }
    }

    // --- Save data management ---
    // PS4 saves live under <appRoot>/home/<userId>/savedata/<titleId>/. We operate across all user
    // home dirs so a save made under any local user is found.

    private fun userHomeDirs(): List<java.io.File> {
        val home = java.io.File(getAppRoot(), "home")
        return home.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: emptyList()
    }

    private fun saveDirsFor(gameId: String): List<java.io.File> =
        userHomeDirs()
            .map { java.io.File(it, "savedata/$gameId") }
            .filter { it.isDirectory && (it.listFiles()?.isNotEmpty() == true) }

    fun hasSaveData(gameId: String): Boolean = saveDirsFor(gameId).isNotEmpty()

    // Zips the game's save data and writes it to public Downloads via MediaStore. Returns the saved
    // location string, or null if there is nothing to export. Zip layout: "<userId>/<files...>".
    fun exportSaveData(gameId: String): String? {
        val dirs = saveDirsFor(gameId)
        if (dirs.isEmpty()) return null
        val name = "sharpemu_save_${gameId}.zip"
        return runCatching {
            val out = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                    )
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values,
                ) ?: return null
                context.contentResolver.openOutputStream(uri)?.let { it to "Downloads/$name" }
            } else {
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                )
                val dst = java.io.File(downloads, name)
                dst.outputStream() to dst.absolutePath
            } ?: return null
            java.util.zip.ZipOutputStream(out.first.buffered()).use { zip ->
                for (dir in dirs) {
                    val userId = dir.parentFile?.parentFile?.name ?: "1000"
                    dir.walkTopDown().filter { it.isFile }.forEach { f ->
                        val rel = f.relativeTo(dir).path.replace('\\', '/')
                        zip.putNextEntry(java.util.zip.ZipEntry("$userId/$rel"))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            out.second
        }.getOrNull()
    }

    // Restores a previously-exported save zip ("<userId>/<files...>") into the home save dirs.
    fun importSaveData(gameId: String, uri: Uri): Boolean = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        java.util.zip.ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val parts = entry.name.replace('\\', '/').split('/', limit = 2)
                    if (parts.size == 2) {
                        val (userId, rel) = parts
                        val dest = java.io.File(
                            java.io.File(getAppRoot(), "home/$userId/savedata/$gameId"), rel,
                        )
                        dest.parentFile?.mkdirs()
                        dest.outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        true
    }.getOrDefault(false)

    // Deletes only the game's save data (all user home dirs).
    fun deleteSaveData(gameId: String): Boolean = runCatching {
        saveDirsFor(gameId).forEach { it.deleteRecursively() }
        true
    }.getOrDefault(false)

    // Wipes ALL of a game's user-generated data: saves, trophy progress, and per-game caches —
    // everything the player has accumulated. Does NOT touch the installed game files themselves.
    fun deleteAllGameData(gameId: String): Boolean = runCatching {
        val root = getAppRoot()
        saveDirsFor(gameId).forEach { it.deleteRecursively() }
        listOf("game_data/$gameId", "cache/$gameId", "cache/trophy/$gameId", "custom_trophy/$gameId")
            .forEach { java.io.File(root, it).deleteRecursively() }
        true
    }.getOrDefault(false)

    // --- Shader cache management ---
    // SharpEmu's pipeline/shader cache (vk_pipeline_cache.bin + any shader dumps) lives under
    // <appRoot>/shader/ -- it is a single shared cache, not split per game (unlike savedata), so
    // these operate on the whole cache even though the UI surfaces them from a game's sheet (same
    // convenience pattern most emulator front-ends use for "manage shader cache").
    private fun shaderCacheDir(): java.io.File = java.io.File(getAppRoot(), "shader")

    fun hasShaderCache(): Boolean {
        val dir = shaderCacheDir()
        return dir.isDirectory && (dir.listFiles()?.isNotEmpty() == true)
    }

    // Zips the whole shader cache dir to public Downloads. Returns the saved location, or null if
    // there is nothing to export.
    fun exportShaderCache(): String? {
        val dir = shaderCacheDir()
        if (!dir.isDirectory || dir.listFiles()?.isEmpty() != false) return null
        val name = "sharpemu_shadercache.zip"
        return runCatching {
            val out = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                    )
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values,
                ) ?: return null
                context.contentResolver.openOutputStream(uri)?.let { it to "Downloads/$name" }
            } else {
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                )
                val dst = java.io.File(downloads, name)
                dst.outputStream() to dst.absolutePath
            } ?: return null
            java.util.zip.ZipOutputStream(out.first.buffered()).use { zip ->
                dir.walkTopDown().filter { it.isFile }.forEach { f ->
                    val rel = f.relativeTo(dir).path.replace('\\', '/')
                    zip.putNextEntry(java.util.zip.ZipEntry(rel))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            out.second
        }.getOrNull()
    }

    // Restores a previously-exported shader cache zip into the shader cache dir.
    fun importShaderCache(uri: Uri): Boolean = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        val dir = shaderCacheDir()
        java.util.zip.ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val dest = java.io.File(dir, entry.name)
                    dest.parentFile?.mkdirs()
                    dest.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        true
    }.getOrDefault(false)

    fun deleteShaderCache(): Boolean = runCatching {
        shaderCacheDir().deleteRecursively()
        true
    }.getOrDefault(false)

    fun installGpuDriver(uri: Uri): PkgInstallResult {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "turnip_driver.zip"
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return PkgInstallResult(false, "Cannot open selected driver", "")
        val fd = pfd.detachFd()
        return runNativeOnBigStack("driver-install") {
            val result = JSONObject(NativeBridge.installGpuDriver(name, fd))
            PkgInstallResult(
                ok = result.optBoolean("ok"),
                message = result.optString("message"),
                path = result.optString("path"),
            )
        }
    }

    fun installLsfgDll(uri: Uri): PkgInstallResult {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "Lossless.dll"
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return PkgInstallResult(false, "Cannot open selected Lossless.dll", "")
        val fd = pfd.detachFd()
        return runNativeOnBigStack("lsfg-dll-install") {
            val result = JSONObject(NativeBridge.installLsfgDll(name, fd))
            PkgInstallResult(
                ok = result.optBoolean("ok"),
                message = result.optString("message"),
                path = result.optString("path"),
            )
        }
    }
}
