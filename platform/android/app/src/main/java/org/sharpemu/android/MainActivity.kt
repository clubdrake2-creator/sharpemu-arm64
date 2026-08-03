// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import java.text.SimpleDateFormat
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.drag as pointerDrag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.mutableStateMapOf
import org.sharpemu.android.data.FoldersPrefs
import org.sharpemu.android.data.GameFolder
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import org.sharpemu.android.data.ControlPrefs
import org.sharpemu.android.data.EmulatorRepository
import org.sharpemu.android.data.FavoritesPrefs
import org.sharpemu.android.ui.theme.AppearancePrefs
import org.sharpemu.android.ui.theme.AppLanguage
import org.sharpemu.android.ui.theme.AppUiText
import org.sharpemu.android.ui.theme.ShadTheme
import org.sharpemu.android.ui.theme.ThemeMode
import org.sharpemu.android.ui.theme.appUiText
import org.sharpemu.android.model.CheatEntry
import org.sharpemu.android.model.CheatInfo
import org.sharpemu.android.model.GameDetails
import org.sharpemu.android.model.GameEntry
import org.sharpemu.android.model.GpuDriverEntry
import org.sharpemu.android.model.PatchInfo
import org.sharpemu.android.model.RemoteGpuDriver
import org.sharpemu.android.model.RepoPatch
import org.sharpemu.android.model.TrophyInfo
import org.sharpemu.android.model.SettingEntry
import org.sharpemu.android.model.SettingsSnapshot

class MainActivity : ComponentActivity() {
    // Legacy (Android 10) runtime READ/WRITE storage permission request. On Android 11+ the much
    // broader MANAGE_EXTERNAL_STORAGE is granted from a system settings screen instead (see below).
    private val legacyStoragePermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { /* re-checked on next resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureStoragePermission()
        setContent {
            SharpEmuAndroidApp()
        }
    }

    // The native emulator core reads game files (eboot.bin, sce_sys/param.sfo, icon0.png) through
    // raw filesystem paths, which cannot open a content:// SAF URI — so the app needs broad file
    // access or games appear with no cover and won't launch. Request it appropriately for the OS
    // version: "All files access" (MANAGE_EXTERNAL_STORAGE) on Android 11+, or the legacy
    // READ/WRITE_EXTERNAL_STORAGE runtime permissions on Android 10.
    private fun ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            legacyStoragePermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return
            }
            // Open the per-app "Allow access to manage all files" screen; fall back to the global
            // list if the per-app deep link isn't available on this device.
            val opened = runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"),
                    ),
                )
                true
            }.getOrDefault(false)
            if (!opened) {
                runCatching {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            val needed = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ).filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                legacyStoragePermissionLauncher.launch(needed.toTypedArray())
            }
        }
    }
}

private enum class Screen {
    Home,
    Settings,
    SettingsCategory,
    Controls,
    ControllerConfig,
    VirtualButtons,
    GpuDrivers,
    GameSettings,
    Logs,
    SfoViewer,
    TrophyViewer,
    Patches,
    Trophies,
    DriverChannels,
}

// Best-effort "when was this game added to the library" signal for the "Recently added" sort mode.
// There is no dedicated "added at" timestamp tracked anywhere (native or Android-side), so this uses
// the game folder's own last-modified time as a real (not fabricated) proxy -- it reflects the moment
// the game's files were extracted/copied onto the device, which for freshly added games is exactly
// when they were added.
private fun gameAddedAt(game: org.sharpemu.android.model.GameEntry): Long =
    runCatching { java.io.File(game.path).lastModified() }.getOrDefault(0L)

// Shared ordering for both the home screen's game list and a folder's contents: applies the chosen
// sort mode (name A-Z/Z-A or recently added), keeping favorited games first within that order.
private fun orderGames(
    games: List<org.sharpemu.android.model.GameEntry>,
    favorites: Set<String>,
    sortMode: org.sharpemu.android.data.GameSortMode,
): List<org.sharpemu.android.model.GameEntry> {
    val sorted = when (sortMode) {
        org.sharpemu.android.data.GameSortMode.NAME_ASC ->
            games.sortedBy { it.title.lowercase(java.util.Locale.getDefault()) }
        org.sharpemu.android.data.GameSortMode.NAME_DESC ->
            games.sortedByDescending { it.title.lowercase(java.util.Locale.getDefault()) }
        org.sharpemu.android.data.GameSortMode.RECENTLY_ADDED ->
            games.sortedByDescending { gameAddedAt(it) }
    }
    return sorted.sortedByDescending { it.id.isNotBlank() && favorites.contains(it.id) }
}

private const val INSTALL_NOTIFICATION_CHANNEL_ID = "pkg_install"
private const val INSTALL_NOTIFICATION_ID = 2001
// Separate, higher-importance channel just for the "finished" alert: the ongoing progress
// notification stays IMPORTANCE_LOW (no peek banner while it's just ticking percentages), but the
// completion alert needs IMPORTANCE_HIGH so Android shows it as a heads-up/expanded banner over
// whatever the user is doing -- which Android itself already suppresses when the notification
// shade is already open, so no manual "is the shade open" check is needed here.
private const val INSTALL_COMPLETE_NOTIFICATION_CHANNEL_ID = "pkg_install_complete"
private const val INSTALL_COMPLETE_NOTIFICATION_ID = 2002
private const val ACTION_CANCEL_INSTALL = "org.sharpemu.android.action.CANCEL_INSTALL"

// Live PKG-install state: the game/file name, 0..100 progress, and whether it is in background.
private data class InstallProgressState(
    val name: String,
    val progress: Int,
    val background: Boolean = false,
)

private fun installNotificationManager(context: Context): NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

private fun ensureInstallNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    val channel = NotificationChannel(
        INSTALL_NOTIFICATION_CHANNEL_ID,
        uiText.text("Instalação de PKG"),
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = uiText.text("Progresso de instalação de jogos")
        setSound(null, null)
    }
    installNotificationManager(context).createNotificationChannel(channel)
}

private fun canPostInstallNotification(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun installCancelPendingIntent(context: Context): PendingIntent {
    val intent = Intent(ACTION_CANCEL_INSTALL).setPackage(context.packageName)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(context, 0, intent, flags)
}

private fun showInstallNotification(context: Context, name: String, progress: Int) {
    if (!canPostInstallNotification(context)) {
        return
    }
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    ensureInstallNotificationChannel(context)
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, INSTALL_NOTIFICATION_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        android.app.Notification.Builder(context)
    }
    val notification = builder
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(uiText.text("Instalando jogo"))
        .setContentText("$name - $progress%")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setProgress(100, progress.coerceIn(0, 100), false)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            uiText.text("Cancelar"),
            installCancelPendingIntent(context),
        )
        .build()
    installNotificationManager(context).notify(INSTALL_NOTIFICATION_ID, notification)
}

private fun clearInstallNotification(context: Context) {
    installNotificationManager(context).cancel(INSTALL_NOTIFICATION_ID)
}

private fun ensureInstallCompleteNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    val channel = NotificationChannel(
        INSTALL_COMPLETE_NOTIFICATION_CHANNEL_ID,
        uiText.text("Instalação concluída"),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = uiText.text("Avisa quando uma instalação em segundo plano termina")
    }
    installNotificationManager(context).createNotificationChannel(channel)
}

// Posted once a background install batch finishes (success or failure), on a HIGH-importance
// channel so it pops up as a heads-up banner instead of sitting silently in the shade -- exactly
// the case the ongoing progress notification (IMPORTANCE_LOW, no peek) doesn't cover.
private fun showInstallCompleteNotification(context: Context, message: String) {
    if (!canPostInstallNotification(context)) {
        return
    }
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    ensureInstallCompleteNotificationChannel(context)
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, INSTALL_COMPLETE_NOTIFICATION_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        android.app.Notification.Builder(context)
    }
    val notification = builder
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(uiText.text("Instalação concluída"))
        .setContentText(message)
        .setAutoCancel(true)
        .setPriority(android.app.Notification.PRIORITY_HIGH)
        .build()
    installNotificationManager(context).notify(INSTALL_COMPLETE_NOTIFICATION_ID, notification)
}

@Composable
private fun SharpEmuAndroidApp() {
    val context = LocalContext.current
    val repository = remember { EmulatorRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Short transient messages are shown as toasts (snackbars were removed from the app UI).
    fun toast(msg: String) =
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()

    var screen by remember { mutableStateOf(Screen.Home) }
    // Simple navigation back-stack so the app-bar back button AND the Android back button/gesture
    // return to the exact screen the user came from (e.g. a settings sub-screen back to its section).
    val backStack = remember { androidx.compose.runtime.mutableStateListOf<Screen>() }
    fun navTo(target: Screen) {
        if (target != screen) {
            backStack.add(screen)
            screen = target
        }
    }
    fun navBack() {
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Home
    }
    androidx.activity.compose.BackHandler(enabled = screen != Screen.Home) { navBack() }
    var games by remember { mutableStateOf(emptyList<GameEntry>()) }
    var gamesLoading by remember { mutableStateOf(true) }
    // Home screen scroll/selection state, hoisted here (instead of `remember`'d inside HomeScreen
    // itself) so it survives navigating to Settings/a game and back, and screen rotation --
    // `Crossfade` disposes the non-current branch on every screen switch, which would otherwise
    // reset any state `remember`'d inside HomeScreen back to its initial value each time.
    val homeGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val homeListState = rememberLazyListState()
    val homeCarouselSelected = remember { mutableStateOf(0) }
    val homeCarouselState = rememberLazyListState()
    // Home library layout (Grid / List / Grid compact), edited in Appearance settings.
    var viewMode by remember { mutableStateOf(AppearancePrefs.getViewMode(context)) }
    // Home library game order (Name A-Z/Z-A/Recently added), edited in Appearance settings.
    var sortMode by remember {
        mutableStateOf(org.sharpemu.android.data.SortPrefs.getSortMode(context))
    }
    var settings by remember {
        mutableStateOf(SettingsSnapshot(root = "", settings = emptyList()))
    }
    var gpuDrivers by remember { mutableStateOf(emptyList<GpuDriverEntry>()) }

    // Blocking PKG-install dialog state (non-null while installing) and the per-game settings screen
    // target + its loaded snapshot.
    var installState by remember { mutableStateOf<InstallProgressState?>(null) }
    var keepScreenOnDuringInstall by remember { mutableStateOf(false) }
    var showCancelInstallConfirm by remember { mutableStateOf(false) }
    var installInBackground by remember { mutableStateOf(false) }
    var settingsGame by remember { mutableStateOf<GameEntry?>(null) }
    var gameSettings by remember { mutableStateOf(SettingsSnapshot(root = "", settings = emptyList())) }

    // Selected global-settings category (for the per-category detail screen).
    var settingsCategory by remember { mutableStateOf("") }

    // Selected physical-controller slot (1..4) for the controller config screen.
    var controllerSlot by remember { mutableStateOf(1) }

    // SFO viewer target game + its loaded details (firmware/region/all param.sfo entries).
    var sfoGame by remember { mutableStateOf<GameEntry?>(null) }
    var sfoDetails by remember { mutableStateOf<GameDetails?>(null) }

    // Game currently being uninstalled (null when idle) + its live deletion progress 0..100,
    // polled the same way PKG install progress is (see onDeleteGame below).
    var deletingGame by remember { mutableStateOf<GameEntry?>(null) }
    var deleteProgress by remember { mutableStateOf(0) }

    // Trophy viewer target game + its loaded trophy listing (null while loading). onlyUnlocked
    // is true when opened from the Trophies notification area (show what was just earned, not
    // the game's full trophy list) and false from the per-game info sheet (show everything).
    var trophyGame by remember { mutableStateOf<GameEntry?>(null) }
    var trophyInfo by remember { mutableStateOf<TrophyInfo?>(null) }
    var trophyViewerOnlyUnlocked by remember { mutableStateOf(false) }
    fun openTrophyViewer(game: GameEntry, onlyUnlocked: Boolean = false) {
        trophyGame = game
        trophyInfo = null
        trophyViewerOnlyUnlocked = onlyUnlocked
        scope.launch {
            val t = withContext(Dispatchers.IO) { repository.loadTrophyInfo(game.id) }
            trophyInfo = t
        }
        navTo(Screen.TrophyViewer)
    }

    // Global Trophies overview (one row per game with real trophy data), opened from the home app bar.
    var trophiesOverview by remember {
        mutableStateOf<List<org.sharpemu.android.model.GameTrophySummary>>(emptyList())
    }
    var trophiesOverviewLoading by remember { mutableStateOf(false) }
    fun loadTrophiesOverview() {
        trophiesOverviewLoading = true
        scope.launch {
            val summaries = withContext(Dispatchers.IO) {
                games.mapNotNull { g ->
                    val info = repository.loadTrophyInfo(g.id)
                    if (info.available && info.trophies.isNotEmpty()) {
                        org.sharpemu.android.model.GameTrophySummary(g, info.trophies)
                    } else {
                        null
                    }
                }
            }
            trophiesOverview = summaries
            trophiesOverviewLoading = false
        }
    }

    // Cheats/Patches target game + its installed-patch and cheat listings.
    var patchesGame by remember { mutableStateOf<GameEntry?>(null) }
    var patchInfo by remember { mutableStateOf<PatchInfo?>(null) }
    var cheatInfo by remember { mutableStateOf<CheatInfo?>(null) }
    fun reloadPatchInfo(id: String) {
        scope.launch { patchInfo = withContext(Dispatchers.IO) { repository.loadPatchInfo(id) } }
    }
    fun reloadCheatInfo(id: String) {
        scope.launch { cheatInfo = withContext(Dispatchers.IO) { repository.loadCheats(id) } }
    }

    // Appearance preferences (theme mode + Material You) drive the theme below and are edited in
    // the Appearance settings section.
    var themeMode by remember { mutableStateOf(AppearancePrefs.getThemeMode(context)) }
    var materialYou by remember { mutableStateOf(AppearancePrefs.getMaterialYou(context)) }
    var useGameBackground by remember { mutableStateOf(AppearancePrefs.getUseGameBackground(context)) }
    var appLanguage by remember { mutableStateOf(AppearancePrefs.getAppLanguage(context)) }
    val uiText = remember(appLanguage) { appUiText(appLanguage) }

    fun refreshGames() {
        gamesLoading = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { repository.loadGames() }
            games = loaded
            gamesLoading = false
            // Warm the icon cache in the background so scrolling the home grid right after the
            // library loads doesn't stutter decoding every cover for the first time.
            precacheGameIcons(loaded)
        }
    }

    fun refreshSettings() {
        settings = repository.loadSettings()
        gpuDrivers = repository.loadGpuDrivers()
    }

    DisposableEffect(context, repository) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action == ACTION_CANCEL_INSTALL) {
                    repository.cancelInstallPkg()
                    toast(uiText.text("Cancelando instalação"))
                }
            }
        }
        val filter = IntentFilter(ACTION_CANCEL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    DisposableEffect(context, installState != null && keepScreenOnDuringInstall) {
        val window = (context as? android.app.Activity)?.window
        if (installState != null && keepScreenOnDuringInstall) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            repository.initialize()
        }
        refreshSettings()
        refreshGames()
    }

    // Keeps the trophy notification badge (home app bar) accurate without requiring the user to
    // open the Trophies screen first -- reloads whenever the game list changes (install/remove)...
    LaunchedEffect(games) {
        if (games.isNotEmpty()) {
            loadTrophiesOverview()
        }
    }
    // ...and also on every resume, since a trophy is typically unlocked while playing a game in a
    // separate activity -- the game list itself doesn't change just because a trophy popped, so the
    // effect above alone would leave the badge stale until the next install/uninstall.
    val trophyLifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(trophyLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && games.isNotEmpty()) {
                loadTrophiesOverview()
            }
        }
        trophyLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { trophyLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    ShadTheme(darkTheme = darkTheme, dynamicColor = materialYou) {
        // Inverted look: the app background uses the cards' tone (surfaceVariant) while the cards
        // themselves use the lighter background tone (set per-card below).
        val overlayBlur by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (org.sharpemu.android.ui.OverlayBlur.openCount > 0) 10.dp else 0.dp,
            label = "overlayBlur",
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .blur(overlayBlur),
            color = appBg(),
        ) {
            // Crossfade gives every screen navigation a fade transition (e.g. opening a settings
            // category detail screen fades in, as requested).
            androidx.compose.animation.Crossfade(
                targetState = screen,
                animationSpec = androidx.compose.animation.core.tween(280),
                label = "screen",
            ) { current ->
            when (current) {
                Screen.Home -> HomeScreen(
                    games = games,
                    loading = gamesLoading,
                    viewMode = viewMode,
                    sortMode = sortMode,
                    useGameBackground = useGameBackground,
                    uiText = uiText,
                    onOpenSettings = {
                        refreshSettings()
                        navTo(Screen.Settings)
                    },
                    onAddFolder = { uri, _ ->
                        scope.launch {
                            // Accepts a single game folder, a folder holding several games, or a root
                            // folder containing many game folders — all games found are added.
                            val count = withContext(Dispatchers.IO) {
                                repository.addGameFolders(uri)
                            }
                            refreshGames()
                            toast(
                                when {
                                    count <= 0 -> "Nenhum jogo encontrado na pasta"
                                    count == 1 -> "Jogo adicionado"
                                    else -> "$count jogos adicionados"
                                },
                            )
                        }
                    },
                    onInstallPkgs = { uris, installRootUri ->
                        // Install the selected PKG(s) one after another. A single selection behaves
                        // exactly as before; a multi-selection installs each in turn, showing
                        // "name (i/total)" progress, and reports a summary at the end.
                        scope.launch {
                            val total = uris.size
                            var okCount = 0
                            var failCount = 0
                            var lastMessage = ""
                            installInBackground = false
                            showCancelInstallConfirm = false
                            for (index in uris.indices) {
                                val uri = uris[index]
                                val name = runCatching {
                                    androidx.documentfile.provider.DocumentFile
                                        .fromSingleUri(context, uri)?.name
                                }.getOrNull() ?: "PKG"
                                val label = if (total > 1) "$name (${index + 1}/$total)" else name
                                installState = InstallProgressState(label, 0, installInBackground)
                                if (installInBackground) {
                                    showInstallNotification(context, label, 0)
                                }
                                // Poll the native install progress while the (blocking) install runs.
                                val poller = launch {
                                    while (true) {
                                        val p = withContext(Dispatchers.IO) {
                                            repository.getInstallProgress()
                                        }
                                        if (p in 0..100) {
                                            installState = installState?.copy(progress = p)
                                            if (installState?.background == true) {
                                                showInstallNotification(context, label, p)
                                            }
                                        }
                                        kotlinx.coroutines.delay(100)
                                    }
                                }
                                val result = withContext(Dispatchers.IO) {
                                    repository.installPkg(uri, installRootUri)
                                }
                                poller.cancel()
                                if (result.ok) {
                                    okCount++
                                } else {
                                    failCount++
                                }
                                lastMessage = result.message
                                refreshGames()
                                if (lastMessage.contains("cancel", ignoreCase = true)) {
                                    break
                                }
                            }
                            clearInstallNotification(context)
                            val wasBackground = installState?.background == true
                            installState = null
                            showCancelInstallConfirm = false
                            installInBackground = false
                            keepScreenOnDuringInstall = false
                            val summary = if (total > 1) {
                                if (failCount == 0) {
                                    "$okCount de $total PKGs instalados"
                                } else {
                                    "$okCount instalados, $failCount falharam: $lastMessage"
                                }
                            } else {
                                lastMessage
                            }
                            if (wasBackground) {
                                showInstallCompleteNotification(context, summary)
                            }
                            toast(summary)
                        }
                    },
                    onGameSelected = { game ->
                        val appRoot = repository.getAppRoot()
                        val renderDiagCaptureEnabled = settings.settings.firstOrNull {
                            it.section == "Android" && it.key == "render_diag_capture_enabled"
                        }?.value == "true"
                        context.startActivity(
                            GameLaunch.createIntent(context, game, appRoot, renderDiagCaptureEnabled),
                        )
                    },
                    onOpenGameSettings = { game ->
                        settingsGame = game
                        gameSettings = repository.loadGameSettings(game.id)
                        navTo(Screen.GameSettings)
                    },
                    onDeleteGame = { game, deleteEntireFolder ->
                        scope.launch {
                            deletingGame = game
                            deleteProgress = 0
                            val poller = launch {
                                while (true) {
                                    val p = withContext(Dispatchers.IO) { repository.getDeleteProgress() }
                                    if (p in 0..100) {
                                        deleteProgress = p
                                    }
                                    kotlinx.coroutines.delay(100)
                                }
                            }
                            val ok = withContext(Dispatchers.IO) {
                                repository.deleteGame(game.id, deleteEntireFolder)
                            }
                            poller.cancel()
                            deletingGame = null
                            refreshGames()
                            toast(if (ok) "Jogo removido" else "Falha ao remover o jogo")
                        }
                    },
                    onCreateShortcut = { game ->
                        val appRoot = repository.getAppRoot()
                        val msg = createGameShortcut(context, game, appRoot)
                        toast(msg)
                    },
                    loadGameDetails = { id -> repository.loadGameDetails(id) },
                    onOpenSfoViewer = { game ->
                        sfoGame = game
                        sfoDetails = null
                        scope.launch {
                            val d = withContext(Dispatchers.IO) { repository.loadGameDetails(game.id) }
                            sfoDetails = d
                        }
                        navTo(Screen.SfoViewer)
                    },
                    onOpenTrophyViewer = { game -> openTrophyViewer(game) },
                    onOpenPatches = { game ->
                        patchesGame = game
                        patchInfo = null
                        cheatInfo = null
                        reloadPatchInfo(game.id)
                        reloadCheatInfo(game.id)
                        navTo(Screen.Patches)
                    },
                    onOpenTrophies = {
                        loadTrophiesOverview()
                        navTo(Screen.Trophies)
                    },
                    showTrophiesButton = settings.settings.firstOrNull {
                        it.section == "General" && it.key == "trophy_key"
                    }?.value?.isNotBlank() ?: false,
                    trophyNotificationCount = trophiesOverview.count {
                        org.sharpemu.android.data.TrophyNotificationPrefs.isActiveNotification(
                            context, it.game.id, it.unlockedCount,
                        )
                    },
                    homeGridState = homeGridState,
                    homeListState = homeListState,
                    homeCarouselSelected = homeCarouselSelected,
                    homeCarouselState = homeCarouselState,
                )

                Screen.Logs -> LogsScreen(onBack = { navBack() })

                Screen.Settings -> SettingsScreen(
                    snapshot = settings,
                    gpuDrivers = gpuDrivers,
                    themeMode = themeMode,
                    materialYou = materialYou,
                    useGameBackground = useGameBackground,
                    appLanguage = appLanguage,
                    uiText = uiText,
                    onThemeModeChange = {
                        themeMode = it
                        AppearancePrefs.setThemeMode(context, it)
                    },
                    onMaterialYouChange = {
                        materialYou = it
                        AppearancePrefs.setMaterialYou(context, it)
                    },
                    onUseGameBackgroundChange = {
                        useGameBackground = it
                        AppearancePrefs.setUseGameBackground(context, it)
                    },
                    onAppLanguageChange = {
                        appLanguage = it
                        AppearancePrefs.setAppLanguage(context, it)
                    },
                    onBack = {
                        refreshGames()
                        navBack()
                    },
                    onSettingChanged = { setting, value ->
                        val updated = repository.updateSetting(setting, value)
                        if (updated) {
                            val linkedFullScreenValue =
                                if (setting.section == "GPU" && setting.key == "full_screen_mode") {
                                    (!value.equals("Windowed", ignoreCase = true)).toString()
                                } else {
                                    null
                                }
                            settings = settings.copy(
                                settings = settings.settings.map {
                                    if (it.section == setting.section && it.key == setting.key) {
                                        it.copy(value = value)
                                    } else if (linkedFullScreenValue != null &&
                                        it.section == "GPU" && it.key == "full_screen") {
                                        it.copy(value = linkedFullScreenValue)
                                    } else {
                                        it
                                    }
                                },
                            )
                        }
                    },
                    onOpenGpuDrivers = {
                        refreshSettings()
                        navTo(Screen.GpuDrivers)
                    },
                    onOpenCategory = { category ->
                        if (category == "Input") {
                            navTo(Screen.Controls)
                        } else {
                            settingsCategory = category
                            navTo(Screen.SettingsCategory)
                        }
                    },
                    onOpenDriverChannels = { navTo(Screen.DriverChannels) },
                )

                Screen.SettingsCategory -> SettingsCategoryScreen(
                    category = settingsCategory,
                    settings = settings.settings.filter {
                        uiSettingsSection(it) == settingsCategory &&
                            !(it.section == "Android" && it.key == "vulkan_driver")
                    },
                    onBack = { navBack() },
                    onSettingChanged = { setting, value ->
                        val updated = repository.updateSetting(setting, value)
                        if (updated) {
                            val linkedFullScreenValue =
                                if (setting.section == "GPU" && setting.key == "full_screen_mode") {
                                    (!value.equals("Windowed", ignoreCase = true)).toString()
                                } else {
                                    null
                                }
                            settings = settings.copy(
                                settings = settings.settings.map {
                                    if (it.section == setting.section && it.key == setting.key) {
                                        it.copy(value = value)
                                    } else if (linkedFullScreenValue != null &&
                                        it.section == "GPU" && it.key == "full_screen") {
                                        it.copy(value = linkedFullScreenValue)
                                    } else {
                                        it
                                    }
                                },
                            )
                        }
                    },
                    themeMode = themeMode,
                    materialYou = materialYou,
                    useGameBackground = useGameBackground,
                    appLanguage = appLanguage,
                    uiText = uiText,
                    onThemeModeChange = {
                        themeMode = it
                        AppearancePrefs.setThemeMode(context, it)
                    },
                    onMaterialYouChange = {
                        materialYou = it
                        AppearancePrefs.setMaterialYou(context, it)
                    },
                    onUseGameBackgroundChange = {
                        useGameBackground = it
                        AppearancePrefs.setUseGameBackground(context, it)
                    },
                    onAppLanguageChange = {
                        appLanguage = it
                        AppearancePrefs.setAppLanguage(context, it)
                    },
                    gpuDriverValue = settings.settings.firstOrNull {
                        it.section == "Android" && it.key == "vulkan_driver"
                    }?.value ?: "system",
                    onOpenGpuDrivers = {
                        refreshSettings()
                        navTo(Screen.GpuDrivers)
                    },
                    onInstallLsfgDll = { uri ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                repository.installLsfgDll(uri)
                            }
                            toast(result.message.ifBlank {
                                if (result.ok) "LSFG instalado" else "Falha ao instalar LSFG"
                            })
                        }
                    },
                    viewMode = viewMode,
                    onViewModeChange = {
                        viewMode = it
                        AppearancePrefs.setViewMode(context, it)
                    },
                    sortMode = sortMode,
                    onSortModeChange = {
                        sortMode = it
                        org.sharpemu.android.data.SortPrefs.setSortMode(context, it)
                    },
                    onOpenLogs = { navTo(Screen.Logs) },
                )

                Screen.Controls -> ControlsScreen(
                    inputSettings = settings.settings.filter { it.section == "Input" },
                    onBack = { navBack() },
                    onSettingChanged = { setting, value ->
                        val updated = repository.updateSetting(setting, value)
                        if (updated) {
                            settings = settings.copy(
                                settings = settings.settings.map {
                                    if (it.section == setting.section && it.key == setting.key) {
                                        it.copy(value = value)
                                    } else {
                                        it
                                    }
                                },
                            )
                        }
                    },
                    onOpenVirtualButtons = { navTo(Screen.VirtualButtons) },
                    onOpenControllerConfig = { slot ->
                        controllerSlot = slot
                        navTo(Screen.ControllerConfig)
                    },
                )

                Screen.ControllerConfig -> org.sharpemu.android.ui.screens.ControllerConfigScreen(
                    slot = controllerSlot,
                    onBack = { navBack() },
                )

                Screen.VirtualButtons -> org.sharpemu.android.ui.screens.VirtualControlLayoutEditorScreen(
                    overlayContainerColor = MaterialTheme.colorScheme.surface,
                    onBack = { navBack() },
                )

                Screen.GpuDrivers -> GpuDriversScreen(
                    drivers = gpuDrivers,
                    onBack = { navBack() },
                    onSelectDriver = { driver ->
                        scope.launch {
                            val selected = withContext(Dispatchers.IO) {
                                repository.selectGpuDriver(driver)
                            }
                            refreshSettings()
                            toast(if (selected) "Driver Vulkan selecionado" else "Falha ao selecionar driver")
                        }
                    },
                    onInstallDriver = { uri ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                repository.installGpuDriver(uri)
                            }
                            if (result.ok) {
                                gpuDrivers = withContext(Dispatchers.IO) {
                                    repository.activateInstalledDriver(result.path)
                                }
                            }
                            refreshSettings()
                            toast(result.message)
                        }
                    },
                    onFetchRemote = {
                        val channels = org.sharpemu.android.data.DriverChannelPrefs.getChannels(context)
                        repository.fetchRemoteGpuDriversFromChannels(channels)
                    },
                    onDownloadDriver = { remote ->
                        scope.launch {
                            toast("${uiText.text("Baixando")} ${remote.name}…")
                            val result = withContext(Dispatchers.IO) {
                                repository.downloadGpuDriver(remote)
                            }
                            if (result.ok) {
                                // Downloaded driver shows up in the list and is activated automatically.
                                gpuDrivers = withContext(Dispatchers.IO) {
                                    repository.activateInstalledDriver(result.path)
                                }
                            }
                            refreshSettings()
                            toast(result.message)
                        }
                    },
                )

                Screen.GameSettings -> {
                    val game = settingsGame
                    if (game == null) {
                        navBack()
                    } else {
                        GameSettingsScreen(
                            game = game,
                            snapshot = gameSettings,
                                    onBack = { navBack() },
                            onSettingChanged = { setting, value ->
                                val ok = repository.updateGameSetting(game.id, setting, value)
                                if (ok) {
                                    gameSettings = gameSettings.copy(
                                        settings = gameSettings.settings.map {
                                            if (it.section == setting.section && it.key == setting.key) {
                                                it.copy(value = value)
                                            } else {
                                                it
                                            }
                                        },
                                    )
                                }
                            },
                        )
                    }
                }

                Screen.SfoViewer -> {
                    val game = sfoGame
                    if (game == null) {
                        navBack()
                    } else {
                        SfoViewerScreen(
                            game = game,
                            details = sfoDetails,
                            onBack = { navBack() },
                        )
                    }
                }

                Screen.TrophyViewer -> {
                    val game = trophyGame
                    if (game == null) {
                        navBack()
                    } else {
                        TrophyViewerScreen(
                            game = game,
                            info = trophyInfo,
                            onlyUnlocked = trophyViewerOnlyUnlocked,
                            onBack = { navBack() },
                        )
                    }
                }

                Screen.Trophies -> TrophiesOverviewScreen(
                    summaries = trophiesOverview,
                    loading = trophiesOverviewLoading,
                    uiText = uiText,
                    onBack = { navBack() },
                    onOpenGame = { game -> openTrophyViewer(game, onlyUnlocked = true) },
                )

                Screen.DriverChannels -> DriverChannelsScreen(onBack = { navBack() })

                Screen.Patches -> {
                    val game = patchesGame
                    if (game == null) {
                        navBack()
                    } else {
                        PatchesScreen(
                            game = game,
                            patchInfo = patchInfo,
                            cheatInfo = cheatInfo,
                                    onBack = { navBack() },
                            onTogglePatch = { name, enabled ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        repository.setPatchEnabled(game.id, name, enabled)
                                    }
                                    reloadPatchInfo(game.id)
                                }
                            },
                            onFetchRepo = { repository.fetchRepoPatches() },
                            onDownloadPatch = { repoPatch ->
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        repository.downloadPatch(game.id, repoPatch.downloadUrl)
                                    }
                                    reloadPatchInfo(game.id)
                                    toast(if (ok) uiText.text("Patch baixado") else uiText.text("Falha ao baixar patch"))
                                }
                            },
                            onImportPatch = { uri ->
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        repository.importPatch(game.id, uri)
                                    }
                                    reloadPatchInfo(game.id)
                                    toast(if (ok) uiText.text("Patch importado") else uiText.text("Falha ao importar patch"))
                                }
                            },
                            onToggleCheat = { cheat, enabled ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        repository.setCheatEnabled(game.id, cheat, enabled)
                                    }
                                    reloadCheatInfo(game.id)
                                }
                            },
                            onDownloadCheat = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        repository.downloadCheat(game.id)
                                    }
                                    reloadCheatInfo(game.id)
                                    toast(if (ok) uiText.text("Cheats baixados") else uiText.text("Nenhum cheat encontrado para este jogo"))
                                }
                            },
                        )
                    }
                }
            }
            } // Crossfade

            // Blocking install dialog shown over everything while a PKG installs, unless the user
            // sends the progress to the Android notification panel.
            installState?.takeIf { !it.background }?.let { state ->
                InstallProgressDialog(
                    name = state.name,
                    progress = state.progress,
                    keepScreenOn = keepScreenOnDuringInstall,
                    onKeepScreenOnChange = { keepScreenOnDuringInstall = it },
                    onCancelRequest = { showCancelInstallConfirm = true },
                    onInstallInBackground = {
                        installInBackground = true
                        installState = state.copy(background = true)
                        showInstallNotification(context, state.name, state.progress)
                        toast(uiText.text("Instalação em segundo plano"))
                    },
                )
            }
            if (showCancelInstallConfirm) {
                ConfirmCancelInstallDialog(
                    onDismiss = { showCancelInstallConfirm = false },
                    onConfirm = {
                        showCancelInstallConfirm = false
                        repository.cancelInstallPkg()
                        toast(uiText.text("Cancelando instalação"))
                    },
                )
            }
            deletingGame?.let { game ->
                DeleteProgressDialog(name = game.title, progress = deleteProgress)
            }
        }
    }
}

// Non-dismissable dialog shown while a game (and optionally its folder) is being uninstalled,
// with a real percentage polled from the native side (GetDeleteProgress) as files are removed.
@Composable
private fun DeleteProgressDialog(name: String, progress: Int) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    org.sharpemu.android.ui.OverlayBlurEffect()
    AlertDialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text(uiText.text("Desinstalando jogo")) },
        text = {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        },
        confirmButton = {},
    )
}

// Non-dismissable dialog shown while a PKG is being installed.
@Composable
private fun InstallProgressDialog(
    name: String,
    progress: Int,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onCancelRequest: () -> Unit,
    onInstallInBackground: () -> Unit,
) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    org.sharpemu.android.ui.OverlayBlurEffect()
    AlertDialog(
        onDismissRequest = {}, // non-dismissable: install must complete first
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text(uiText.text("Instalando jogo")) },
        text = {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onKeepScreenOnChange(!keepScreenOn) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = keepScreenOn,
                        onClick = { onKeepScreenOnChange(!keepScreenOn) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiText.text("Manter a tela ligada"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onInstallInBackground) {
                Text(uiText.text("Instalar em segundo plano"))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelRequest) {
                Text(uiText.text("Cancelar"))
            }
        },
    )
}

@Composable
private fun ConfirmCancelInstallDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    org.sharpemu.android.ui.OverlayBlurEffect()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiText.text("Cancelar instalação?")) },
        text = {
            Text(uiText.text("A pasta parcialmente extraída será apagada."))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(uiText.text("Cancelar instalação"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(uiText.text("Voltar"))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    games: List<GameEntry>,
    loading: Boolean,
    viewMode: org.sharpemu.android.ui.theme.ViewMode,
    sortMode: org.sharpemu.android.data.GameSortMode,
    useGameBackground: Boolean,
    uiText: AppUiText,
    onOpenSettings: () -> Unit,
    onAddFolder: (Uri, Boolean) -> Unit,
    onInstallPkgs: (List<Uri>, Uri?) -> Unit,
    onGameSelected: (GameEntry) -> Unit,
    onOpenGameSettings: (GameEntry) -> Unit,
    onDeleteGame: (GameEntry, Boolean) -> Unit,
    onCreateShortcut: (GameEntry) -> Unit,
    loadGameDetails: (String) -> GameDetails,
    onOpenSfoViewer: (GameEntry) -> Unit,
    onOpenTrophyViewer: (GameEntry) -> Unit,
    onOpenPatches: (GameEntry) -> Unit,
    onOpenTrophies: () -> Unit,
    showTrophiesButton: Boolean,
    trophyNotificationCount: Int,
    // Hoisted from SharpEmuAndroidApp so scroll/selection position survives navigating away and back
    // (Crossfade disposes/recreates this whole composable on every screen switch) and rotation.
    homeGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    homeCarouselSelected: androidx.compose.runtime.MutableState<Int>,
    homeCarouselState: androidx.compose.foundation.lazy.LazyListState,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    // The home is a console-style carousel in landscape and the chosen grid/list layout in portrait.
    val isLandscape = LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var menuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var infoGame by remember { mutableStateOf<GameEntry?>(null) }
    var folderBatchMode by remember { mutableStateOf(false) }
    var showFolderBatchDialog by remember { mutableStateOf(false) }
    var showPkgInstallSheet by remember { mutableStateOf(false) }
    var pkgInstallRootUri by remember { mutableStateOf<Uri?>(null) }
    // Bumped whenever a favorite is toggled, so the list re-sorts (favorites first) live.
    var favoritesTick by remember { mutableStateOf(0) }
    val favorites = remember(favoritesTick, games) { FavoritesPrefs.getFavorites(context) }
    // Records the launch timestamp used to show a relative "last played" label in the game info sheet.
    val launchGame: (GameEntry) -> Unit = { game ->
        org.sharpemu.android.data.LastPlayedPrefs.markPlayedNow(context, game.id)
        onGameSelected(game)
    }
    // Home-screen game folders (drag-and-drop grouping). Bumped to reload from prefs after any change.
    var foldersTick by remember { mutableStateOf(0) }
    val folders = remember(foldersTick) { FoldersPrefs.getFolders(context) }
    fun reloadFolders() { foldersTick++ }
    var openFolder by remember { mutableStateOf<GameFolder?>(null) }
    var optionsFolder by remember { mutableStateOf<GameFolder?>(null) }

    // The search field collapses behind the app bar while scrolling down and reappears scrolling up.
    var searchVisible by remember { mutableStateOf(true) }
    val scrollHider = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y < -3f) searchVisible = false
                else if (available.y > 3f) searchVisible = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    // In landscape the console home goes immersive (status + navigation bars hidden); rotating back to
    // portrait restores them. Tied to orientation so it toggles automatically as the phone is turned.
    val immersiveView = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(isLandscape) {
        val window = (immersiveView.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        if (isLandscape) {
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        if (it != null) {
            onAddFolder(it, folderBatchMode)
            if (folderBatchMode) {
                showFolderBatchDialog = true
            }
        } else {
            folderBatchMode = false
        }
    }
    // Multi-select: the user can pick a single PKG or many at once; they install one after another.
    val pkgPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onInstallPkgs(uris, pkgInstallRootUri)
        }
        pkgInstallRootUri = null
    }
    val pkgInstallFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) {
        if (it != null) {
            pkgInstallRootUri = it
            showPkgInstallSheet = false
            pkgPicker.launch(arrayOf("application/octet-stream", "*/*"))
        } else {
            pkgInstallRootUri = null
        }
    }

    val filteredGames = remember(games, searchQuery, favorites, sortMode) {
        val matched = if (searchQuery.isBlank()) {
            games
        } else {
            games.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.id.contains(searchQuery, ignoreCase = true)
            }
        }
        orderGames(matched, favorites, sortMode)
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(if (menuOpen) 10.dp else 0.dp),
            containerColor = appBg(),
            topBar = {
                // The landscape console home draws its own top bar (clock + search/settings icons), so
                // the standard app header is only shown in portrait.
                if (!isLandscape) {
                    HomeHeader(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onOpenSettings = onOpenSettings,
                        onOpenTrophies = onOpenTrophies,
                        showTrophiesButton = showTrophiesButton,
                        trophyNotificationCount = trophyNotificationCount,
                        searchVisible = searchVisible,
                        uiText = uiText,
                    )
                }
            },
        ) { padding ->
            val gridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp)
            if (loading) {
                // Material 3 Expressive contained loading indicator while the game list is read.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    AppLoadingIndicator(modifier = Modifier.size(72.dp))
                }
            } else if (filteredGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isBlank()) uiText.text("Nenhum jogo instalado")
                            else "${uiText.text("Nenhum resultado para")} \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (isLandscape) {
                // Landscape: the Nintendo-Switch-style console home — a centered horizontal carousel of
                // games and folders with the focused one framed by an animated border. Fills the whole
                // screen (its own top bar replaces the app header) and ignores the scaffold padding.
                val groupedPaths = remember(folders) { folders.flatMap { it.gamePaths }.toSet() }
                val gamesByPath = remember(games) { games.associateBy { it.path } }
                val switchItems = remember(filteredGames, folders, searchQuery, groupedPaths) {
                    if (searchQuery.isNotBlank()) {
                        filteredGames.map { HomeItem.GameItem(it) }
                    } else {
                        folders.sortedByDescending { it.favorite }.map { HomeItem.FolderItem(it) } +
                            filteredGames.filterNot { groupedPaths.contains(it.path) }
                                .map { HomeItem.GameItem(it) }
                    }
                }
                SwitchHomeScreen(
                    items = switchItems,
                    gamesByPath = gamesByPath,
                    searchQuery = searchQuery,
                    useGameBackground = useGameBackground,
                    uiText = uiText,
                    onSearchChange = { searchQuery = it },
                    onOpenGame = launchGame,
                    onOpenFolder = { openFolder = it },
                    onGameInfo = { infoGame = it },
                    onDismissInfo = { infoGame = null },
                    onFolderOptions = { optionsFolder = it },
                    onOpenSettings = onOpenSettings,
                    onOpenTrophies = onOpenTrophies,
                    showTrophiesButton = showTrophiesButton,
                    trophyNotificationCount = trophyNotificationCount,
                    onAddClick = { menuOpen = true },
                    onCreateFolder = { a, b ->
                        FoldersPrefs.createFolder(context, a.path, b.path)
                        reloadFolders()
                    },
                    onAddToFolder = { fid, g ->
                        FoldersPrefs.addGameToFolder(context, fid, g.path)
                        reloadFolders()
                    },
                    selectedState = homeCarouselSelected,
                    listState = homeCarouselState,
                )
            } else {
                // Drag-and-drop folders work in every view mode (list, compact grid, normal grid). A
                // tile dragged onto another game makes a folder; dragged onto a folder, joins it. When
                // searching, folders are bypassed and a flat list of matching games is shown.
                val groupedPaths = remember(folders) {
                    folders.flatMap { it.gamePaths }.toSet()
                }
                val gamesByPath = remember(games) { games.associateBy { it.path } }
                val homeItems = remember(filteredGames, folders, searchQuery, groupedPaths) {
                    if (searchQuery.isNotBlank()) {
                        filteredGames.map { HomeItem.GameItem(it) }
                    } else {
                        val folderItems = folders.sortedByDescending { it.favorite }
                            .map { HomeItem.FolderItem(it) }
                        val ungrouped = filteredGames
                            .filterNot { groupedPaths.contains(it.path) }
                            .map { HomeItem.GameItem(it) }
                        folderItems + ungrouped
                    }
                }
                HomeContentWithFolders(
                    viewMode = viewMode,
                    items = homeItems,
                    gamesByPath = gamesByPath,
                    padding = padding,
                    gridPadding = gridPadding,
                    scrollHider = scrollHider,
                    onLaunchGame = launchGame,
                    onGameInfo = { infoGame = it },
                    onDismissInfo = { infoGame = null },
                    onOpenFolder = { openFolder = it },
                    onFolderOptions = { optionsFolder = it },
                    onCreateFolder = { a, b ->
                        FoldersPrefs.createFolder(context, a.path, b.path)
                        reloadFolders()
                    },
                    onAddToFolder = { fid, g ->
                        FoldersPrefs.addGameToFolder(context, fid, g.path)
                        reloadFolders()
                    },
                    listState = homeListState,
                    gridState = homeGridState,
                )
            }
        }
        // Same add-games actions in both orientations. In portrait they hang off the floating FAB; in
        // landscape the FAB is hidden and the console home's discreet "Adicionar" button opens the very
        // same expanding menu (here with showFab = false) over the blurred backdrop.
        val addMenuItems = listOf(
            ExpressiveFabItem(uiText.text("Pasta/raiz de jogos"), Icons.Default.Folder) {
                folderBatchMode = false
                folderPicker.launch(null)
            },
            ExpressiveFabItem("Várias pastas", Icons.Default.FolderOpen) {
                folderBatchMode = true
                folderPicker.launch(null)
            },
            ExpressiveFabItem(uiText.text("Instalar PKG(s)"), Icons.Default.Add) {
                showPkgInstallSheet = true
            },
        )
        FabBackdrop(expanded = menuOpen, onDismiss = { menuOpen = false })
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            ExpressiveFabMenu(
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
                items = addMenuItems,
                showFab = !isLandscape,
            )
        }
    }

    if (showPkgInstallSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { showPkgInstallSheet = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Text(
                uiText.text("Instalar PKG"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text(uiText.text("Pasta padrão do app")) },
                supportingContent = {
                    Text(uiText.text("Instala no armazenamento interno usado atualmente pelo emulador."))
                },
                leadingContent = { Icon(Icons.Default.InstallMobile, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    pkgInstallRootUri = null
                    showPkgInstallSheet = false
                    pkgPicker.launch(arrayOf("application/octet-stream", "*/*"))
                },
            )
            ListItem(
                headlineContent = { Text(uiText.text("Pasta selecionada")) },
                supportingContent = { Text(uiText.text("Escolha uma pasta e instale todos os PKGs selecionados nela.")) },
                leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    pkgInstallFolderPicker.launch(null)
                },
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showFolderBatchDialog) {
        AlertDialog(
            onDismissRequest = {
                showFolderBatchDialog = false
                folderBatchMode = false
            },
            title = { Text(uiText.text("Adicionar outra pasta?")) },
            text = {
                Text(
                    uiText.text("A pasta selecionada foi enviada para a biblioteca. Selecione outra pasta de jogo ou encerre a fila."),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFolderBatchDialog = false
                    folderPicker.launch(null)
                }) {
                    Text(uiText.text("Selecionar outra"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFolderBatchDialog = false
                    folderBatchMode = false
                }) {
                    Text(uiText.text("Concluir"))
                }
            },
        )
    }

    // Long-press info bottom sheet: cover, id, version, size, path, and a per-game settings button.
    infoGame?.let { game ->
        GameInfoBottomSheet(
            game = game,
            onDismiss = { infoGame = null },
            onOpenSettings = {
                infoGame = null
                onOpenGameSettings(game)
            },
            onDelete = { deleteEntireFolder ->
                infoGame = null
                onDeleteGame(game, deleteEntireFolder)
            },
            onCreateShortcut = {
                infoGame = null
                onCreateShortcut(game)
            },
            loadGameDetails = loadGameDetails,
            onOpenSfoViewer = {
                infoGame = null
                onOpenSfoViewer(game)
            },
            onOpenTrophyViewer = {
                infoGame = null
                onOpenTrophyViewer(game)
            },
            onOpenPatches = {
                infoGame = null
                onOpenPatches(game)
            },
            onFavoriteChanged = { favoritesTick++ },
            // Only when this game is currently inside a folder: offer to pull it back to the home grid.
            onRemoveFromFolder = if (folders.any { it.gamePaths.contains(game.path) }) {
                {
                    FoldersPrefs.removeGameFromFolder(context, game.path)
                    reloadFolders()
                    infoGame = null
                }
            } else {
                null
            },
        )
    }

    // Folder contents: tap a folder to open a blurred sheet of the games inside (favorites first).
    openFolder?.let { folder ->
        val current = folders.firstOrNull { it.id == folder.id }
        if (current == null) {
            LaunchedEffect(folder.id) { openFolder = null }
        } else {
            val gamesByPath = remember(games) { games.associateBy { it.path } }
            val folderGames = remember(current, games, favorites, sortMode) {
                orderGames(
                    current.gamePaths.mapNotNull { gamesByPath[it] },
                    favorites,
                    sortMode,
                )
            }
            val availableFolderGames = remember(current, games, folders) {
                val grouped = folders.flatMap { it.gamePaths }.toSet()
                games.filterNot { grouped.contains(it.path) || current.gamePaths.contains(it.path) }
            }
            FolderContentsSheet(
                folder = current,
                games = folderGames,
                availableGames = availableFolderGames,
                viewMode = viewMode,
                uiText = uiText,
                onDismiss = { openFolder = null },
                onLaunch = { launchGame(it) },
                onGameInfo = { infoGame = it },
                onOptions = { optionsFolder = current },
                onAddGames = { selectedPaths ->
                    FoldersPrefs.addGamesToFolder(context, current.id, selectedPaths)
                    reloadFolders()
                },
            )
        }
    }

    // Folder long-press: options sheet to rename, favorite, see size/regions, or remove the folder.
    optionsFolder?.let { folder ->
        val current = folders.firstOrNull { it.id == folder.id }
        if (current == null) {
            LaunchedEffect(folder.id) { optionsFolder = null }
        } else {
            val gamesByPath = remember(games) { games.associateBy { it.path } }
            val folderGames = remember(current, games) {
                current.gamePaths.mapNotNull { gamesByPath[it] }
            }
            FolderOptionsSheet(
                folder = current,
                games = folderGames,
                loadGameDetails = loadGameDetails,
                onDismiss = { optionsFolder = null },
                onRename = { FoldersPrefs.rename(context, current.id, it); reloadFolders() },
                onToggleFavorite = { FoldersPrefs.toggleFavorite(context, current.id); reloadFolders() },
                onRemove = {
                    FoldersPrefs.removeFolder(context, current.id)
                    reloadFolders()
                    optionsFolder = null
                    openFolder = null
                },
            )
        }
    }
}

// Formats a byte count as a human-readable size (e.g. "1.4 GB").
private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) "${bytes} B" else String.format(java.util.Locale.US, "%.1f %s", value, units[unit])
}

// Modal bottom sheet with all of a game's information plus a button to its per-game settings.
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GameInfoBottomSheet(
    game: GameEntry,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: (deleteEntireFolder: Boolean) -> Unit,
    onCreateShortcut: () -> Unit,
    loadGameDetails: (String) -> GameDetails,
    onOpenSfoViewer: () -> Unit,
    onOpenTrophyViewer: () -> Unit,
    onOpenPatches: () -> Unit,
    onFavoriteChanged: () -> Unit = {},
    // Non-null only when the game currently lives inside a home-screen folder. Invoking it removes the
    // game from the folder (returning it to the home grid) and closes the sheet.
    onRemoveFromFolder: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    val scope = rememberCoroutineScope()
    val repo = remember { EmulatorRepository(context.applicationContext) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteEntireFolder by remember { mutableStateOf(false) }
    var hasSaves by remember(game.id) { mutableStateOf(false) }
    var hasShaders by remember { mutableStateOf(false) }
    LaunchedEffect(game.id) {
        hasSaves = withContext(Dispatchers.IO) { repo.hasSaveData(game.id) }
        hasShaders = withContext(Dispatchers.IO) { repo.hasShaderCache() }
    }
    fun toast(msg: String) =
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    val importSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { repo.importSaveData(game.id, uri) }
                hasSaves = withContext(Dispatchers.IO) { repo.hasSaveData(game.id) }
                toast(if (ok) "Save importado" else "Falha ao importar save")
            }
        }
    }
    val importShaderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { repo.importShaderCache(uri) }
                hasShaders = withContext(Dispatchers.IO) { repo.hasShaderCache() }
                toast(if (ok) "Shaders importados" else "Falha ao importar shaders")
            }
        }
    }
    var favorite by remember(game.id) {
        mutableStateOf(FavoritesPrefs.isFavorite(context, game.id))
    }
    // Firmware/region come from the game's param.sfo, read off the main thread.
    val details by androidx.compose.runtime.produceState<GameDetails?>(
        initialValue = null,
        key1 = game.id,
    ) {
        value = withContext(Dispatchers.IO) { runCatching { loadGameDetails(game.id) }.getOrNull() }
    }
    val cover = rememberGameIcon(game.icon)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cover != null) {
                        Image(
                            bitmap = cover,
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = game.source.uppercase(java.util.Locale.US),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = remember(game.id) {
                            relativeLastPlayedLabel(
                                context,
                                org.sharpemu.android.data.LastPlayedPrefs.getLastPlayed(context, game.id),
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Favorite toggle (persisted in FavoritesPrefs, Android-side only).
                IconButton(onClick = {
                    favorite = FavoritesPrefs.toggle(context, game.id)
                    onFavoriteChanged()
                }) {
                Icon(
                    if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = uiText.text("Favoritar"),
                        tint = if (favorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            GameInfoRow("ID", game.id.ifBlank { "—" })
            GameInfoRow(uiText.text("Versão"), game.version.ifBlank { "—" })
            GameInfoRow(uiText.text("Região"), details?.region?.let { regionLabel(it) } ?: "…")
            GameInfoRow(
                "Firmware",
                details?.firmware?.takeIf { it.isNotBlank() }?.let { "$it" } ?: "…",
            )
            GameInfoRow(uiText.text("Tamanho"), formatSize(game.sizeBytes))
            GameInfoRow(uiText.text("Local"), game.path.ifBlank { "—" })
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))

            // All quick actions fit in exactly 2 rows of 4 equal-width cells. SFO (row 1, col 1) and
            // Compatibility (row 2, col 1) are text-only cells; the rest are icons.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetGridIcon(Icons.Default.Description, "SFO viewer") { onOpenSfoViewer() }
                SheetGridIcon(Icons.Default.Settings, uiText.text("Configurações do jogo")) { onOpenSettings() }
                SheetGridIcon(Icons.Default.AddToHomeScreen, uiText.text("Criar atalho")) { onCreateShortcut() }
                SheetGridIcon(Icons.Default.FolderOpen, uiText.text("Abrir pasta")) { openGameFolder(context, game) }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetGridIcon(Icons.Default.VerifiedUser, uiText.text("Compatibilidade")) {
                    openCompatibility(context, game)
                }
                SheetGridIcon(Icons.Default.ContentCopy, uiText.text("Copiar informações")) {
                    copyGameInfo(context, game)
                }
                SheetGridIcon(Icons.Default.EmojiEvents, uiText.text("Troféus")) { onOpenTrophyViewer() }
                SheetGridIcon(Icons.Default.Tune, uiText.text("Cheats / Patches")) { onOpenPatches() }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                uiText.text("Saves"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            // All save actions fit in a single row: Import/Export keep their labels; Delete is icon-only
            // (no label) so everything fits without wrapping.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetWideButton(Icons.Default.Download, uiText.text("Importar")) {
                    importSaveLauncher.launch(arrayOf("application/zip", "*/*"))
                }
                SheetWideButton(Icons.Default.Upload, uiText.text("Exportar"), enabled = hasSaves) {
                    scope.launch {
                        val path = withContext(Dispatchers.IO) { repo.exportSaveData(game.id) }
                        toast(if (path != null) "Save exportado: $path" else "Nenhum save para exportar")
                    }
                }
                SheetIconButton(
                    Icons.Default.Delete,
                    uiText.text("Apagar save"),
                    destructive = true,
                    enabled = hasSaves,
                ) {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { repo.deleteSaveData(game.id) }
                        hasSaves = withContext(Dispatchers.IO) { repo.hasSaveData(game.id) }
                        toast(if (ok) "Save apagado" else "Falha ao apagar save")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                uiText.text("Shaders"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            // Same layout/style as the Saves row above -- shadPS4's shader/pipeline cache is a single
            // shared cache (not per-game), but managing it from a game's sheet is the same convenience
            // pattern most emulator front-ends use.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SheetWideButton(Icons.Default.Download, uiText.text("Importar")) {
                    importShaderLauncher.launch(arrayOf("application/zip", "*/*"))
                }
                SheetWideButton(Icons.Default.Upload, uiText.text("Exportar"), enabled = hasShaders) {
                    scope.launch {
                        val path = withContext(Dispatchers.IO) { repo.exportShaderCache() }
                        toast(if (path != null) "Shaders exportados: $path" else "Nenhum shader para exportar")
                    }
                }
                SheetIconButton(
                    Icons.Default.Delete,
                    uiText.text("Apagar shaders"),
                    destructive = true,
                    enabled = hasShaders,
                ) {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { repo.deleteShaderCache() }
                        hasShaders = withContext(Dispatchers.IO) { repo.hasShaderCache() }
                        toast(if (ok) "Shaders apagados" else "Falha ao apagar shaders")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))
            // Shown only when the game is inside a folder: pull it back out onto the home screen.
            onRemoveFromFolder?.let { remove ->
                SheetAction(Icons.Default.FolderOff, uiText.text("Remover da pasta")) { remove() }
            }
            SheetAction(Icons.Default.DeleteSweep, uiText.text("Desinstalar jogo"), destructive = true) {
                deleteEntireFolder = false
                confirmDelete = true
            }
        }
    }

    if (confirmDelete) {
        org.sharpemu.android.ui.OverlayBlurEffect()
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text(uiText.text("Desinstalar jogo")) },
            text = {
                Column {
                    Text(
                        "Remover \"${game.title}\"? Os arquivos do jogo instalado serão apagados do " +
                            "dispositivo. Esta ação não pode ser desfeita.",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable { deleteEntireFolder = !deleteEntireFolder }
                            .padding(vertical = 6.dp),
                    ) {
                        RoundCheckTile(checked = deleteEntireFolder)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            uiText.text("Apagar também a pasta inteira do jogo"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete(deleteEntireFolder)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(uiText.text("Desinstalar"))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(uiText.text("Cancelar")) }
            },
        )
    }
}

// Round, tile-shaped checkbox indicator (filled circle with a checkmark when checked) -- used
// instead of the default square Material Checkbox where a fully round selection tile is wanted.
@Composable
private fun RoundCheckTile(checked: Boolean, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .then(
                if (!checked) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outline,
                        androidx.compose.foundation.shape.CircleShape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(size * 0.7f),
            )
        }
    }
}

// Equal-width grid cell with only a centered text label (used for SFO / Compatibility so they sit in
// the same 4-per-row grid as the icon buttons).
@Composable
private fun androidx.compose.foundation.layout.RowScope.SheetGridText(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Equal-width grid cell with a centered icon (the quick-action buttons), matching SheetGridText's size.
@Composable
private fun androidx.compose.foundation.layout.RowScope.SheetGridIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Wide action button: a rounded tonal container with the icon and label side by side (label to the
// right of the icon). Used for the save actions (Import/Export).
@Composable
private fun androidx.compose.foundation.layout.RowScope.SheetWideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val container = if (destructive) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (destructive) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = content.copy(alpha = if (enabled) 1f else 0.5f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = content.copy(alpha = if (enabled) 1f else 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Material 3 switch with the expressive check-icon thumb (shown filled when ON). Centralizes the look
// so every toggle in the app uses the same expressive switch.
@Composable
private fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

// App-wide loading spinner: a Material 3 "contained" loading indicator — a circular progress indicator
// inside a rounded tonal container, matching https://m3.material.io/components/loading-indicator
// visually. The real ContainedLoadingIndicator composable doesn't exist yet in ANY published
// compose-bom as of this check (verified by inspecting material3-android 1.4.0's classes.jar
// directly: only LoadingIndicatorTokens exists, no LoadingIndicatorKt) -- material3-expressive
// hasn't shipped the actual component publicly yet, so this mirrors its look until it does.
@Composable
private fun AppLoadingIndicator(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

// Icon-only square action button used in the game info sheet's quick-action grid.
@Composable
private fun SheetIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val container = if (destructive) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (destructive) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container.copy(alpha = if (enabled) 1f else 0.4f),
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = content.copy(alpha = if (enabled) 1f else 0.5f),
            )
        }
    }
}

// Icon + label vertical action button (used for the save import/export/delete row).
@Composable
private fun SheetLabeledButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val content = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = (if (destructive) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant).copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = content.copy(alpha = if (enabled) 1f else 0.5f),
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content.copy(alpha = if (enabled) 1f else 0.5f),
        )
    }
}

@Composable
private fun GameInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

// Maps a region code (derived natively from CONTENT_ID) to a flag emoji + localized name.
private fun regionLabel(code: String): String = when (code.uppercase(java.util.Locale.US)) {
    "US" -> "🇺🇸 América do Norte"
    "EU" -> "🇪🇺 Europa"
    "JP" -> "🇯🇵 Japão"
    "ASIA" -> "🇭🇰 Ásia"
    "KR" -> "🇰🇷 Coreia"
    "INT" -> "🌍 Internacional"
    else -> "🌍 Mundial"
}

// A single tappable action row in the game info bottom sheet (icon + label).
@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

// Tries to open the game's install folder in a file manager. Under Android scoped storage there is
// no universal "open this folder" intent (and games live under Android/data, which many file
// managers can't browse), so we attempt a DocumentsUI intent and, if nothing handles it, copy the
// path to the clipboard and show it so the user can paste it into their file manager.
private fun openGameFolder(context: Context, game: GameEntry) {
    val file = java.io.File(game.path)
    val folder = if (file.isFile) (file.parentFile ?: file) else file
    val path = folder.absolutePath
    // Targets the "primary" ExternalStorageProvider that ships as part of AOSP's DocumentsUI /
    // Files app on every Android device (not a 3rd-party file manager that may or may not be
    // installed) -- the same content:// document-tree convention Android itself uses to open a
    // folder from a path under /storage/emulated/0. `resource/folder` (the previous attempt) isn't
    // a real Android MIME type, so essentially nothing ever handled it.
    val opened = runCatching {
        val externalRoot = "/storage/emulated/0/"
        val relative = if (path.startsWith(externalRoot)) {
            path.removePrefix(externalRoot)
        } else {
            path.trimStart('/')
        }
        val docId = "primary:$relative"
        val treeUri = Uri.parse(
            "content://com.android.externalstorage.documents/document/" + Uri.encode(docId),
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(treeUri, "vnd.android.document/directory")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
    if (!opened) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("game folder", path))
        android.widget.Toast.makeText(
            context,
            "Nenhum gerenciador abriu a pasta. Caminho copiado:\n$path",
            android.widget.Toast.LENGTH_LONG,
        ).show()
    }
}

// Copies a human-readable summary of the game (mirrors desktop's "Copy Info") to the clipboard.
private fun copyGameInfo(context: Context, game: GameEntry) {
    val info = buildString {
        appendLine("Título: ${game.title}")
        appendLine("ID: ${game.id}")
        appendLine("Versão: ${game.version}")
        appendLine("Fonte: ${game.source}")
        appendLine("Tamanho: ${formatSize(game.sizeBytes)}")
        append("Local: ${game.path}")
    }
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cb.setPrimaryClip(ClipData.newPlainText("shadPS4 game info", info))
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    android.widget.Toast.makeText(context, uiText.text("Informações copiadas"), android.widget.Toast.LENGTH_SHORT)
        .show()
}

// Opens the official shadPS4 game-compatibility tracker filtered by this game's title id (the same
// database the desktop UI surfaces).
private fun openCompatibility(context: Context, game: GameEntry) {
    val query = game.id.ifBlank { game.title }
    val url = "https://github.com/shadps4-emu/shadps4-game-compatibility/issues?q=" +
        Uri.encode("is:issue $query")
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        android.widget.Toast.makeText(
            context, "Não foi possível abrir o navegador", android.widget.Toast.LENGTH_SHORT,
        ).show()
    }
}

// Pins a launcher shortcut that boots straight into this game (icon = the game's cover). Returns a
// status message for the snackbar.
private fun createGameShortcut(
    context: Context,
    game: GameEntry,
    appRoot: String,
): String {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        return "A tela inicial não suporta atalhos fixados"
    }
    val launch = GameLaunch.createIntent(context, game, appRoot)
        .setAction(Intent.ACTION_VIEW)
    val bmp = runCatching { android.graphics.BitmapFactory.decodeFile(game.icon) }.getOrNull()
    val icon = if (bmp != null) {
        IconCompat.createWithBitmap(bmp)
    } else {
        IconCompat.createWithResource(context, org.sharpemu.android.R.drawable.ic_rpcs3_foreground)
    }
    val id = "game_" + game.id.ifBlank { game.path.hashCode().toString() }
    val shortcut = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(game.title.ifBlank { "Jogo" })
        .setLongLabel(game.title.ifBlank { "Jogo" })
        .setIcon(icon)
        .setIntent(launch)
        .build()
    return if (ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)) {
        "Atalho criado na tela inicial"
    } else {
        "Falha ao criar atalho"
    }
}

// A single action in the expressive FAB menu.
private data class ExpressiveFabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun FabBackdrop(
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = expanded,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(140),
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(100),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // No dim layer — the content behind is blurred instead (user asked to drop the dark
                // translucent scrim). Kept clickable so tapping outside still closes the menu.
                .clickable(onClick = onDismiss),
        )
    }
}

// Material 3 Expressive-style FAB menu: tapping the FAB reveals a dimming scrim and a vertical stack
// of labeled tonal buttons that spring in/out, while the FAB itself morphs from "+" to "×". This
// replaces the plain DropdownMenu with the expressive expanding-menu pattern (labeled small FABs).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<ExpressiveFabItem>,
    showFab: Boolean = true,
) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    // FAB rotates 45° (+ -> ×) when open.
    val fabRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        ),
        label = "fabRotation",
    )
    Column(horizontalAlignment = Alignment.End) {
        items.forEachIndexed { index, item ->
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                // Each item springs up from the FAB, slightly staggered, newest (closest to FAB) last.
                enter = androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(120),
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.6f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f),
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                    ),
                ) + androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                ),
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(90),
                ) + androidx.compose.animation.scaleOut(
                    targetScale = 0.6f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f),
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(bottom = 14.dp),
                ) {
                    // Label chip to the left of the small FAB (expressive labeled menu item).
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 3.dp,
                        shadowElevation = 3.dp,
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    FloatingActionButton(
                        onClick = {
                            onExpandedChange(false)
                            item.onClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(item.icon, contentDescription = item.label)
                    }
                }
            }
        }
        // The main FAB: large, rounded, morphs + -> × when expanded. Hidden when showFab is false (the
        // landscape console home triggers this menu from its own discreet "Adicionar" button instead).
        if (showFab) {
            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(if (expanded) 22.dp else 18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (expanded) uiText.close else uiText.text("Adicionar jogo"),
                    modifier = Modifier.graphicsLayer { rotationZ = fabRotation },
                )
            }
        }
    }
}

// Home top bar styled after the pcsx2 library header: a large title with a spaced subtitle, a
// settings icon, and a rounded search field.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrophies: () -> Unit,
    showTrophiesButton: Boolean,
    trophyNotificationCount: Int,
    searchVisible: Boolean,
    uiText: AppUiText,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appBg())
            .statusBarsPadding()
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "rpPS4",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp,
                )
                Text(
                    text = "ANDROID UNOFFICIAL",
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (showTrophiesButton) {
                IconButton(onClick = onOpenTrophies) {
                    androidx.compose.material3.BadgedBox(
                        badge = {
                            if (trophyNotificationCount > 0) {
                                androidx.compose.material3.Badge(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                ) {
                                    Text(trophyNotificationCount.coerceAtMost(99).toString())
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = uiText.text("Troféus"),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = uiText.settings,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        // The search field slides up behind the app-bar row when scrolling down and back out when
        // scrolling up (expand/shrink towards the top = it disappears behind the title row above it).
        androidx.compose.animation.AnimatedVisibility(
            visible = searchVisible,
            enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) +
                androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) +
                androidx.compose.animation.fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    placeholder = {
                        Text(uiText.text("Pesquisar"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBg(),
                        unfocusedContainerColor = cardBg(),
                        focusedBorderColor = cardBg(),
                        unfocusedBorderColor = cardBg(),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

// Game tile styled after the pcsx2 card item: the game's real icon0.png cover (decoded from the
// installed game folder) with the title and id below it. Falls back to a gamepad placeholder
// when the icon is missing.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GameTile(game: GameEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    val cover = rememberGameIcon(game.icon)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(appBg()),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = game.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = game.id.ifBlank { game.source },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------------------------------
// Home-screen game folders (drag-and-drop grouping)
// ---------------------------------------------------------------------------------------------------

private sealed interface HomeItem {
    data class FolderItem(val folder: GameFolder) : HomeItem
    data class GameItem(val game: GameEntry) : HomeItem
}

// Folder keys come from FoldersPrefs ("folder_<millis>"); game keys are the game path. The "folder_"
// prefix lets a drop target tell a folder from a game.
private fun homeItemKey(item: HomeItem): String = when (item) {
    is HomeItem.FolderItem -> item.folder.id
    is HomeItem.GameItem -> item.game.path
}

private data class DragInfo(
    val game: GameEntry,
    val start: Offset,
    val pointer: Offset,
    val hoverKey: String?,
    // The long press only "picks up" the tile once the finger has actually moved past a small
    // threshold. Until then nothing visually lifts, so a plain hold reads as "about to open the
    // sheet"; the first significant movement flips this true and the tile starts following the finger.
    val started: Boolean = false,
    // The dragged tile's width in px, captured ONCE from itemBounds at drag-start (when the tile is
    // guaranteed to be composed/visible, since the finger is on it). The ghost preview must NOT look
    // this up again from itemBounds on every recomposition: once auto-scroll or a manual scroll
    // carries the source tile out of the lazy list/grid's composed viewport, its itemBounds entry
    // goes stale (or is missing entirely for a row never composed), which used to make the ghost
    // render with a wrong width or not render at all until the scroll position happened to bring the
    // original tile back into view.
    val width: Float = 0f,
)

// ===================================================================================================
// Landscape console home (Nintendo-Switch-style), adapted from github.com/kirankunigiri/nintendo-
// switch-web-ui. A centered horizontal carousel of games and folders on a dark backdrop: the focused
// item's title shows in blue above it, and the focused tile is framed by an animated cyan<->blue
// border (the Switch selection glow). Driven by a connected controller (D-pad to move, A to open, Y for
// info) or by touch (tap to focus, tap again to open, long-press for info). The original's user-profile
// row and bottom option-button bar are intentionally omitted; search and settings live as icons in the
// top-right corner, and the carousel is centered vertically.
// ===================================================================================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SwitchHomeScreen(
    items: List<HomeItem>,
    gamesByPath: Map<String, GameEntry>,
    searchQuery: String,
    useGameBackground: Boolean,
    uiText: AppUiText,
    onSearchChange: (String) -> Unit,
    onOpenGame: (GameEntry) -> Unit,
    onOpenFolder: (GameFolder) -> Unit,
    onGameInfo: (GameEntry) -> Unit,
    onDismissInfo: () -> Unit,
    onFolderOptions: (GameFolder) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrophies: () -> Unit,
    showTrophiesButton: Boolean,
    trophyNotificationCount: Int,
    onAddClick: () -> Unit,
    onCreateFolder: (GameEntry, GameEntry) -> Unit,
    onAddToFolder: (String, GameEntry) -> Unit,
    // Hoisted to SharpEmuAndroidApp (survives Crossfade screen switches and rotation) so the
    // selected/focused item and scroll position aren't lost when the user opens Settings/a game
    // and comes back to Home, or rotates the phone into landscape.
    selectedState: androidx.compose.runtime.MutableState<Int>,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    // Follow the app's selected theme (light/dark/Material You) instead of a fixed dark palette.
    val titleColor = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground
    val count = items.size
    var selected by selectedState
    LaunchedEffect(count) { if (selected > count - 1) selected = (count - 1).coerceAtLeast(0) }
    var searchExpanded by remember { mutableStateOf(false) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    var drag by remember { mutableStateOf<DragInfo?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val longPressTimeoutMs = LocalViewConfiguration.current.longPressTimeoutMillis
    val moveThresholdPx = with(density) { 18.dp.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    var horizontalAutoScroll by remember { mutableStateOf(0f) }

    LaunchedEffect(horizontalAutoScroll) {
        while (horizontalAutoScroll != 0f) {
            listState.scrollBy(horizontalAutoScroll)
            delay(16)
        }
    }
    // Clicking/focusing an item brings it to the left edge (where the first item starts); the item that
    // was there scrolls off to the right. animateScrollToItem aligns the item to the viewport start.
    LaunchedEffect(selected, count) {
        if (count > 0) listState.animateScrollToItem(selected.coerceIn(0, count - 1))
    }

    val selectedItem = items.getOrNull(selected)
    val selectedBackground = rememberGameBackground((selectedItem as? HomeItem.GameItem)?.game)
    val selectedTitle = when (selectedItem) {
        is HomeItem.FolderItem -> selectedItem.folder.name.ifBlank { "Sem nome" }
        is HomeItem.GameItem -> selectedItem.game.title
        null -> ""
    }
    val openSelected = {
        when (val it = items.getOrNull(selected)) {
            is HomeItem.FolderItem -> onOpenFolder(it.folder)
            is HomeItem.GameItem -> onOpenGame(it.game)
            null -> {}
        }
    }
    val infoSelected = {
        when (val it = items.getOrNull(selected)) {
            is HomeItem.FolderItem -> onFolderOptions(it.folder)
            is HomeItem.GameItem -> onGameInfo(it.game)
            null -> {}
        }
    }

    // Request focus so a connected controller's D-pad/buttons drive the carousel immediately.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg())
            .focusRequester(focusRequester)
            .focusable()
            .onGloballyPositioned { containerOrigin = it.boundsInWindow().topLeft }
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) {
                    false
                } else when (ev.key) {
                    Key.DirectionLeft, Key.SystemNavigationLeft -> {
                        if (selected > 0) selected -= 1
                        true
                    }
                    Key.DirectionRight, Key.SystemNavigationRight -> {
                        if (selected < count - 1) selected += 1
                        true
                    }
                    Key.ButtonA, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        openSelected()
                        true
                    }
                    Key.ButtonY -> {
                        infoSelected()
                        true
                    }
                    else -> false
                }
            },
    ) {
        if (useGameBackground) {
            androidx.compose.animation.Crossfade(
                targetState = selectedBackground,
                animationSpec = androidx.compose.animation.core.tween(260),
                label = "game-background",
                modifier = Modifier.fillMaxSize(),
            ) { background ->
                if (background != null) {
                    Image(
                        bitmap = background,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f)),
                    )
                }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // Tiles centered on the screen, a touch smaller than the band height.
            val tileSize = maxHeight * 0.58f - 10.dp

            // The carousel (game/folder list), centered vertically in the middle of the screen.
            LazyRow(
                state = listState,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(tileSize),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                itemsIndexed(items, key = { _, it -> homeItemKey(it) }) { index, item ->
                    val key = homeItemKey(item)
                    val draggingThis = drag?.game?.path == key
                    val isTarget = drag != null && drag?.hoverKey == key && !draggingThis
                    SwitchTile(
                        item = item,
                        gamesByPath = gamesByPath,
                        size = tileSize,
                        selected = index == selected,
                        highlighted = isTarget,
                        modifier = Modifier
                            .onGloballyPositioned { itemBounds[key] = it.boundsInWindow() }
                            .graphicsLayer {
                                alpha = if (draggingThis && drag?.started == true) 0.3f else 1f
                            },
                        onClick = { if (index == selected) openSelected() else selected = index },
                        onLongClick = {
                            selected = index
                            when (item) {
                                is HomeItem.FolderItem -> onFolderOptions(item.folder)
                                is HomeItem.GameItem -> onGameInfo(item.game)
                            }
                        },
                        onDragStart = if (item is HomeItem.GameItem) {
                            { local ->
                                selected = index
                                val bounds = itemBounds[key]
                                val origin = bounds?.topLeft ?: Offset.Zero
                                val abs = origin + local
                                drag = DragInfo(item.game, abs, abs, null, width = bounds?.width ?: 0f)
                                onGameInfo(item.game)
                            }
                        } else {
                            null
                        },
                        onDragMove = if (item is HomeItem.GameItem) {
                            { amount ->
                                val ds = drag
                                val cur = (ds?.pointer ?: Offset.Zero) + amount
                                val started = ds?.started == true ||
                                    (ds != null && (cur - ds.start).getDistance() > moveThresholdPx)
                                if (started && ds?.started == false) {
                                    // Landscape uses the same long-press affordance as portrait:
                                    // hold opens info, dragging converts it into a folder gesture.
                                    onDismissInfo()
                                }
                                if (started) {
                                    val edge = with(density) { 72.dp.toPx() }
                                    horizontalAutoScroll = when {
                                        cur.x - containerOrigin.x < edge -> -36f
                                        screenWidthPx - (cur.x - containerOrigin.x) < edge -> 36f
                                        else -> 0f
                                    }
                                } else {
                                    horizontalAutoScroll = 0f
                                }
                                val hover = if (started) {
                                    itemBounds.entries
                                        .firstOrNull { (k, r) -> k != key && r.contains(cur) }?.key
                                } else {
                                    null
                                }
                                drag = ds?.copy(pointer = cur, hoverKey = hover, started = started)
                            }
                        } else {
                            null
                        },
                        onDragEnd = if (item is HomeItem.GameItem) {
                            {
                                val ds = drag
                                drag = null
                                horizontalAutoScroll = 0f
                                if (ds != null && ds.started) {
                                    val hk = ds.hoverKey
                                    when {
                                        hk == null -> {}
                                        hk.startsWith("folder_") -> onAddToFolder(hk, item.game)
                                        else -> gamesByPath[hk]?.let { onCreateFolder(item.game, it) }
                                    }
                                }
                            }
                        } else {
                            null
                        },
                        onDragCancel = if (item is HomeItem.GameItem) {
                            {
                                drag = null
                                horizontalAutoScroll = 0f
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            // Focused item's title, floating just above the centered carousel.
            Text(
                text = selectedTitle,
                color = titleColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .offset(y = -(tileSize / 2) - 25.dp)
                    .padding(start = 44.dp, end = 44.dp),
            )

            // Top bar pinned to the top: settings/search only; the clock is intentionally omitted in
            // landscape so the console home stays focused on the library.
            // search field (and back to an icon when the keyboard is dismissed or the search submitted).
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !searchExpanded,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = uiText.text("Buscar"),
                                tint = onBg,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        if (showTrophiesButton) {
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = onOpenTrophies) {
                                androidx.compose.material3.BadgedBox(
                                    badge = {
                                        if (trophyNotificationCount > 0) {
                                            androidx.compose.material3.Badge(
                                                containerColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                                            ) {
                                                Text(trophyNotificationCount.coerceAtMost(99).toString())
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = uiText.text("Troféus"),
                                        tint = onBg,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = uiText.settings,
                                tint = onBg,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = searchExpanded,
                    enter = androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End) +
                        androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End) +
                        androidx.compose.animation.fadeOut(),
                ) {
                    SwitchSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchChange,
                        onCollapse = { searchExpanded = false },
                        uiText = uiText,
                    )
                }
            }

            // Bottom controls: divider, the open hint, and the discreet add-games button (the portrait
            // FAB's actions; tapping it opens the same menu over a blurred backdrop).
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SwitchButtonHint(
                        drawable = R.drawable.cross,
                        if (selectedItem is HomeItem.FolderItem) "Abrir pasta" else "Abrir",
                    )
                    Spacer(Modifier.width(20.dp))
                    SwitchAddButton(onClick = onAddClick)
                }
            }
        }
        drag?.takeIf { it.started }?.let { ds ->
            val local = ds.pointer - containerOrigin
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = local.x - 58.dp.toPx()
                        translationY = local.y - 58.dp.toPx()
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.92f
                    }
                    .size(116.dp),
            ) {
                FolderGameCover(ds.game, 22.dp, Modifier.fillMaxSize())
            }
        }
    }
}

// The search field shown when the Switch top-bar search icon is tapped: same look as the portrait
// home's field (rounded, card-coloured, "Pesquisar"). Auto-focuses (opening the keyboard); submitting
// the search or dismissing the keyboard clears focus, which collapses it back to an icon.
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SwitchSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onCollapse: () -> Unit,
    uiText: AppUiText,
) {
    val fr = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var everFocused by remember { mutableStateOf(false) }
    // When the virtual keyboard is dismissed, drop focus — which collapses the field back to an icon.
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible) {
            focusManager.clearFocus()
        }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        placeholder = { Text(uiText.text("Pesquisar"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cardBg(),
            unfocusedContainerColor = cardBg(),
            focusedBorderColor = cardBg(),
            unfocusedBorderColor = cardBg(),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(fr)
            .onFocusChanged { st ->
                if (st.isFocused) {
                    everFocused = true
                } else if (everFocused) {
                    onCollapse()
                }
            },
    )
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
}

// A single carousel tile: a square cover (game) or 2x2 mini-cover folder, framed by an animated
// cyan<->blue border when it is the focused item.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SwitchTile(
    item: HomeItem,
    gamesByPath: Map<String, GameEntry>,
    size: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
) {
    val transition = rememberInfiniteTransition(label = "switchBorder")
    val borderColor by transition.animateColor(
        initialValue = Color(0xFF62EFDF),
        targetValue = Color(0xFF1B98C6),
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "switchBorderColor",
    )
    Box(
        modifier = modifier
            .size(size)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(
                if (onDragStart != null && onDragMove != null && onDragEnd != null && onDragCancel != null) {
                    Modifier.pointerInput(homeItemKey(item)) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(it) },
                            onDrag = { change, amount -> change.consume(); onDragMove(amount) },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // The cover is inset a little so the focus frame has room to draw around it without resizing
        // the slot. A small inset keeps the covers large (the user asked for noticeably bigger tiles).
        val inset = size * 0.03f
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(inset)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (item is HomeItem.FolderItem) {
                        folderBg()
                    } else {
                        appBg()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (item) {
                is HomeItem.GameItem -> {
                    val cover = rememberGameIcon(item.game.icon)
                    if (cover != null) {
                        Image(
                            bitmap = cover,
                            contentDescription = item.game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxSize(0.4f),
                        )
                    }
                }
                is HomeItem.FolderItem -> {
                    val covers = item.folder.gamePaths.mapNotNull { gamesByPath[it] }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(size * 0.06f),
                        verticalArrangement = Arrangement.spacedBy(size * 0.04f),
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(size * 0.04f),
                            ) {
                                for (col in 0 until 2) {
                                    val g = covers.getOrNull(row * 2 + col)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    ) {
                                        if (g != null) {
                                            FolderGameCover(g, size * 0.12f, Modifier.fillMaxSize())
                                        } else {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(size * 0.12f))
                                                    .background(appBg()),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Focus frame at the slot edge, a gap (= inset) away from the cover.
        if (selected || highlighted) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        size * 0.022f,
                        if (highlighted) MaterialTheme.colorScheme.primary else borderColor,
                        RoundedCornerShape(26.dp),
                    ),
            )
        }
    }
}

// Small circular controller-button hint (e.g. "A  Abrir") shown on the bottom controls row.
@Composable
private fun SwitchButtonHint(drawable: Int, label: String) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(drawable),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
    }
}

// The discreet landscape "add games" control beside the open hint. Tapping it opens the very same
// expanding menu the portrait FAB shows (over a blurred backdrop) — here it just triggers that menu.
@Composable
private fun SwitchAddButton(onClick: () -> Unit) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.triangle),
            contentDescription = uiText.text("Adicionar jogos"),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            uiText.text("Adicionar"),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            fontSize = 14.sp,
        )
    }
}

// Live HH:mm clock, refreshed periodically. Mirrors the Switch home's status-bar time.
@Composable
private fun rememberClockHm(): String {
    var now by remember { mutableStateOf(java.time.LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = java.time.LocalTime.now()
            kotlinx.coroutines.delay(10_000)
        }
    }
    return "%02d:%02d".format(now.hour, now.minute)
}


// The default 2-column home grid with drag-and-drop folders. Long-pressing a game tile opens its info
// sheet immediately; if the finger then moves past a small threshold the sheet is dismissed and the
// tile is picked up to drag — onto another game to create a folder, or onto a folder to add it there.
// A floating ghost of the cover follows the finger and the hovered drop target scales up.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HomeContentWithFolders(
    viewMode: org.sharpemu.android.ui.theme.ViewMode,
    items: List<HomeItem>,
    gamesByPath: Map<String, GameEntry>,
    padding: PaddingValues,
    gridPadding: PaddingValues,
    scrollHider: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    onLaunchGame: (GameEntry) -> Unit,
    onGameInfo: (GameEntry) -> Unit,
    onDismissInfo: () -> Unit,
    onOpenFolder: (GameFolder) -> Unit,
    onFolderOptions: (GameFolder) -> Unit,
    onCreateFolder: (GameEntry, GameEntry) -> Unit,
    onAddToFolder: (String, GameEntry) -> Unit,
    // Hoisted to SharpEmuAndroidApp (survives Crossfade screen switches and rotation) so the scroll
    // position isn't lost when the user opens Settings/a game and comes back to Home.
    listState: androidx.compose.foundation.lazy.LazyListState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
) {
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    var containerHeightPx by remember { mutableStateOf(0f) }
    var drag by remember { mutableStateOf<DragInfo?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val longPressTimeoutMs = LocalViewConfiguration.current.longPressTimeoutMillis
    val moveThresholdPx = with(density) { 18.dp.toPx() }
    val edgeScrollPx = with(density) { 82.dp.toPx() }
    val isList = viewMode == org.sharpemu.android.ui.theme.ViewMode.LIST
    val columns = if (viewMode == org.sharpemu.android.ui.theme.ViewMode.GRID_COMPACT) 3 else 2
    var verticalAutoScroll by remember { mutableStateOf(0f) }

    LaunchedEffect(verticalAutoScroll, isList) {
        while (verticalAutoScroll != 0f) {
            if (isList) {
                listState.scrollBy(verticalAutoScroll)
            } else {
                gridState.scrollBy(verticalAutoScroll)
            }
            delay(16)
        }
    }

    // One home cell: tracks its window bounds (for drag hit-testing), scales/dims while being
    // dragged, and renders either a folder tile or a draggable game tile in the active view mode.
    // Sharing it keeps the drag-to-create-folder behaviour identical across list/compact/normal.
    val cell: @Composable (HomeItem) -> Unit = { item ->
        val key = homeItemKey(item)
        val draggingThis = drag?.game?.path == key
        val isTarget = drag != null && drag?.hoverKey == key && !draggingThis
        val targetScale by animateFloatAsState(if (isTarget) 1.12f else 1f, label = "folderTarget")
        Box(
            modifier = Modifier
                .onGloballyPositioned { itemBounds[key] = it.boundsInWindow() }
                .graphicsLayer {
                    scaleX = targetScale
                    scaleY = targetScale
                    alpha = if (draggingThis && drag?.started == true) 0.3f else 1f
                },
        ) {
            when (item) {
                is HomeItem.FolderItem ->
                    if (isList) {
                        FolderListRow(
                            folder = item.folder,
                            gamesByPath = gamesByPath,
                            highlighted = isTarget,
                            onClick = { onOpenFolder(item.folder) },
                            onLongClick = { onFolderOptions(item.folder) },
                        )
                    } else {
                        FolderTile(
                            folder = item.folder,
                            gamesByPath = gamesByPath,
                            highlighted = isTarget,
                            onClick = { onOpenFolder(item.folder) },
                            onLongClick = { onFolderOptions(item.folder) },
                        )
                    }
                is HomeItem.GameItem -> {
                    val game = item.game
                    GameTileDraggable(
                        viewMode = viewMode,
                        game = game,
                        highlighted = isTarget,
                        onClick = { onLaunchGame(game) },
                        enableDrag = false,
                        onDragStart = { local ->
                            val bounds = itemBounds[key]
                            val origin = bounds?.topLeft ?: Offset.Zero
                            val abs = origin + local
                            drag = DragInfo(game, abs, abs, null, width = bounds?.width ?: 0f)
                            // The long press itself opens the game's info sheet right away (no
                            // hold-and-release needed). If the finger then moves to drag, onDragMove
                            // dismisses the sheet and the tile is picked up instead.
                            onGameInfo(game)
                        },
                        onDragMove = { amount ->
                            val ds = drag
                            val cur = (ds?.pointer ?: Offset.Zero) + amount
                            // Only once the finger has travelled past the threshold do we "pick up"
                            // the tile and track drop targets. The first time that happens we dismiss
                            // the info sheet the long press opened (the gesture is now a folder drag).
                            val started = ds?.started == true ||
                                (ds != null && (cur - ds.start).getDistance() > moveThresholdPx)
                            if (started && ds?.started == false) {
                                onDismissInfo()
                            }
                            if (started) {
                                val y = cur.y - containerOrigin.y
                                verticalAutoScroll = when {
                                    y < edgeScrollPx -> -42f
                                    containerHeightPx - y < edgeScrollPx -> 42f
                                    else -> 0f
                                }
                            } else {
                                verticalAutoScroll = 0f
                            }
                            val hover = if (started) {
                                itemBounds.entries
                                    .firstOrNull { (k, r) -> k != key && r.contains(cur) }?.key
                            } else {
                                null
                            }
                            drag = ds?.copy(pointer = cur, hoverKey = hover, started = started)
                        },
                        onDragEnd = {
                            val ds = drag
                            drag = null
                            verticalAutoScroll = 0f
                            // The sheet was already opened in onDragStart, so a plain hold needs
                            // nothing here. Only a real drag triggers a folder action.
                            if (ds != null && ds.started) {
                                val hk = ds.hoverKey
                                when {
                                    hk == null -> {}
                                    hk.startsWith("folder_") -> onAddToFolder(hk, game)
                                    else -> gamesByPath[hk]?.let { onCreateFolder(game, it) }
                                }
                            }
                        },
                        onDragCancel = {
                            drag = null
                            verticalAutoScroll = 0f
                        },
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .onGloballyPositioned {
                containerOrigin = it.boundsInWindow().topLeft
                containerHeightPx = it.size.height.toFloat()
            }
            .pointerInput(items, viewMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downWindow = containerOrigin + down.position
                    val downItem = items.firstOrNull { candidate ->
                        candidate is HomeItem.GameItem &&
                            itemBounds[homeItemKey(candidate)]?.contains(downWindow) == true
                    } as? HomeItem.GameItem ?: return@awaitEachGesture
                    var longPressCancelled = false
                    val longPress = withTimeoutOrNull(longPressTimeoutMs) {
                        var latest = down
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: run {
                                    longPressCancelled = true
                                    return@withTimeoutOrNull null
                                }
                            if (!change.pressed || change.isConsumed) {
                                longPressCancelled = true
                                return@withTimeoutOrNull null
                            }
                            latest = change
                        }
                        @Suppress("UNREACHABLE_CODE")
                        latest
                    } ?: if (longPressCancelled) {
                        return@awaitEachGesture
                    } else {
                        down
                    }
                    val startWindow = containerOrigin + longPress.position
                    val downWidth = itemBounds[homeItemKey(downItem)]?.width ?: 0f
                    drag = DragInfo(downItem.game, startWindow, startWindow, null, width = downWidth)
                    onGameInfo(downItem.game)
                    val sourceKey = downItem.game.path
                    var dismissedInfoForDrag = false
                    pointerDrag(longPress.id) { change ->
                        val ds = drag ?: return@pointerDrag
                        val cur = containerOrigin + change.position
                        val started = ds.started ||
                            (cur - ds.start).getDistance() > moveThresholdPx
                        if (started && !dismissedInfoForDrag) {
                            onDismissInfo()
                            dismissedInfoForDrag = true
                        }
                        verticalAutoScroll = if (started) {
                            val y = cur.y - containerOrigin.y
                            when {
                                y < edgeScrollPx -> -42f
                                containerHeightPx - y < edgeScrollPx -> 42f
                                else -> 0f
                            }
                        } else {
                            0f
                        }
                        val hover = if (started) {
                            itemBounds.entries
                                .firstOrNull { (k, r) -> k != sourceKey && r.contains(cur) }?.key
                        } else {
                            null
                        }
                        drag = ds.copy(pointer = cur, hoverKey = hover, started = started)
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                    }
                    val ds = drag
                    drag = null
                    verticalAutoScroll = 0f
                    if (ds != null && ds.started) {
                        val hk = ds.hoverKey
                        when {
                            hk == null -> {}
                            hk.startsWith("folder_") -> onAddToFolder(hk, downItem.game)
                            else -> gamesByPath[hk]?.let { onCreateFolder(downItem.game, it) }
                        }
                    }
                }
            },
    ) {
        if (isList) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollHider),
                contentPadding = gridPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { homeItemKey(it) }) { cell(it) }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollHider),
                contentPadding = gridPadding,
                horizontalArrangement = Arrangement.spacedBy(if (columns == 3) 10.dp else 14.dp),
                verticalArrangement = Arrangement.spacedBy(if (columns == 3) 10.dp else 14.dp),
            ) {
                items(items, key = { homeItemKey(it) }) { cell(it) }
            }
        }
        // Floating ghost cover that follows the finger once a tile is actually picked up (finger moved
        // past the threshold); a plain hold shows nothing. In list mode the rows are full-width, so the
        // ghost uses a fixed cover size instead of the row width. Width comes from `ds.width`,
        // captured once at drag-start -- NOT re-read from itemBounds here, since auto-scroll/manual
        // scroll during the drag routinely carries the source tile's row out of the lazy grid/list's
        // composed viewport, which used to make this whole block disappear (itemBounds[ds.game.path]
        // becomes null) until the scroll happened to bring the original tile back into view.
        drag?.takeIf { it.started }?.let { ds ->
            val fallbackWidthPx = with(density) { 116.dp.toPx() }
            val ghostWidthPx = if (isList) {
                with(density) { 96.dp.toPx() }
            } else {
                ds.width.takeIf { it > 0f } ?: fallbackWidthPx
            }
            val wDp = with(density) { ghostWidthPx.toDp() }
            val local = ds.pointer - containerOrigin
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = local.x - ghostWidthPx / 2f
                        translationY = local.y - ghostWidthPx / 2f
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.92f
                    }
                    .width(wDp),
            ) {
                FolderGameCover(
                    game = ds.game,
                    corner = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }
        }
    }
}

// A game tile that can be dragged (after a long press) onto another tile/folder. Visually identical to
// GameTile; tapping launches the game, and the highlighted ring shows when it is a hovered drop target.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GameTileDraggable(
    viewMode: org.sharpemu.android.ui.theme.ViewMode,
    game: GameEntry,
    highlighted: Boolean,
    onClick: () -> Unit,
    enableDrag: Boolean = true,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    // Same tap/long-press-drag gesture in every view mode; only the visual differs. A tap launches the
    // game, a long press picks the tile up so it can be dragged onto another tile/folder.
    val gesture = Modifier
        .clickable(onClick = onClick)
        .then(
            if (enableDrag) {
                Modifier.pointerInput(game.path) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart(it) },
                        onDrag = { change, amount -> change.consume(); onDragMove(amount) },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                    )
                }
            } else {
                Modifier
            },
        )
    when (viewMode) {
        org.sharpemu.android.ui.theme.ViewMode.LIST -> GameListVisual(game, highlighted, gesture)
        org.sharpemu.android.ui.theme.ViewMode.GRID_COMPACT -> GameCompactVisual(game, highlighted, gesture)
        else -> GameGridVisual(game, highlighted, gesture)
    }
}

// Normal-grid game visual: a square cover with the title and id beneath. `gesture` carries the
// tap/long-press-drag behaviour; `highlighted` rings the cover when it is a hovered drop target.
@Composable
private fun GameGridVisual(game: GameEntry, highlighted: Boolean, gesture: Modifier) {
    val cover = rememberGameIcon(game.icon)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(gesture),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(appBg())
                .then(
                    if (highlighted) {
                        Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(22.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = game.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(iterations = Int.MAX_VALUE),
        )
        Text(
            text = game.id.ifBlank { game.source },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Just a game's cover in a rounded box (used for the drag ghost and the mini covers inside folders).
@Composable
private fun FolderGameCover(
    game: GameEntry,
    corner: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val cover = rememberGameIcon(game.icon)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(appBg()),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Default.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}

// A folder tile on the home screen: a container in the search-bar colour (cardBg) holding up to four
// mini covers in a 2x2 grid, with the folder name below. Tap opens its contents; long-press opens its
// options. This deliberately keeps the game long-press behaviour intact for games (handled elsewhere).
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(
    folder: GameFolder,
    gamesByPath: Map<String, GameEntry>,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val covers = folder.gamePaths.mapNotNull { gamesByPath[it] }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(folderBg())
                .then(
                    if (highlighted) {
                        Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(22.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(10.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                for (row in 0 until 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        for (col in 0 until 2) {
                            val g = covers.getOrNull(row * 2 + col)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                if (g != null) {
                                    FolderGameCover(g, 18.dp, Modifier.fillMaxSize())
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(appBg()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (folder.favorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = folder.name.ifBlank { "Sem nome" },
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${folder.gamePaths.size} jogos",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

// Tap a folder -> blurred sheet listing the games inside (favorites first). Each game keeps the home
// behaviour: tap launches, long-press opens its info sheet. The games are laid out the same way as the
// current home view mode (list rows / compact grid / normal grid). Scrolls vertically for big folders.
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderContentsSheet(
    folder: GameFolder,
    games: List<GameEntry>,
    availableGames: List<GameEntry>,
    viewMode: org.sharpemu.android.ui.theme.ViewMode,
    uiText: AppUiText,
    onDismiss: () -> Unit,
    onLaunch: (GameEntry) -> Unit,
    onGameInfo: (GameEntry) -> Unit,
    onOptions: () -> Unit,
    onAddGames: (List<String>) -> Unit,
) {
    var showAddGames by remember(folder.id) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text(
                    folder.name.ifBlank { "Sem nome" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (folder.favorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { showAddGames = true }) {
                    Icon(Icons.Default.Add, contentDescription = uiText.text("Adicionar jogos"))
                }
            }
            // Games are shown the same way as the active home view mode. Inside a folder there is no
            // drag-to-create, so the tiles just carry tap (launch) + long-press (info) via this gesture.
            val tapGesture: (GameEntry) -> Modifier = { g ->
                Modifier.combinedClickable(
                    onClick = { onLaunch(g) },
                    onLongClick = { onGameInfo(g) },
                )
            }
            if (viewMode == org.sharpemu.android.ui.theme.ViewMode.LIST) {
                games.forEach { g ->
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        GameListVisual(g, highlighted = false, gesture = tapGesture(g))
                    }
                }
            } else {
                val cols = if (viewMode == org.sharpemu.android.ui.theme.ViewMode.GRID_COMPACT) 3 else 2
                games.chunked(cols).forEach { rowGames ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for (g in rowGames) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (cols == 3) {
                                    GameCompactVisual(g, highlighted = false, gesture = tapGesture(g))
                                } else {
                                    GameGridVisual(g, highlighted = false, gesture = tapGesture(g))
                                }
                            }
                        }
                        repeat(cols - rowGames.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showAddGames) {
        AddGamesToFolderSheet(
            availableGames = availableGames,
            uiText = uiText,
            onDismiss = { showAddGames = false },
            onAdd = { paths ->
                onAddGames(paths)
                showAddGames = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGamesToFolderSheet(
    availableGames: List<GameEntry>,
    uiText: AppUiText,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                uiText.text("Adicionar jogos"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            if (availableGames.isEmpty()) {
                Text(
                    uiText.text("Nenhum jogo disponível"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                availableGames.forEach { game ->
                    val checked = selected.contains(game.path)
                    ListItem(
                        headlineContent = {
                            Text(game.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(game.id.ifBlank { game.source }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            val cover = rememberGameIcon(game.icon)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cover != null) {
                                    Image(
                                        bitmap = cover,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null)
                                }
                            }
                        },
                        trailingContent = { RadioButton(selected = checked, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selected = if (checked) selected - game.path else selected + game.path
                            },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onAdd(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiText.text("Adicionar"))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// Long-press a folder -> options: rename, favorite, total size + regions of the games, and remove (all
// games return to the home screen). Scrolls vertically so nothing is cut off.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderOptionsSheet(
    folder: GameFolder,
    games: List<GameEntry>,
    loadGameDetails: (String) -> GameDetails,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var name by remember(folder.id) { mutableStateOf(folder.name) }
    var favorite by remember(folder.id) { mutableStateOf(folder.favorite) }
    val totalSize = remember(games) { games.sumOf { it.sizeBytes } }
    val regions by androidx.compose.runtime.produceState(emptyList<String>(), folder.id) {
        value = withContext(Dispatchers.IO) {
            games.mapNotNull {
                runCatching { loadGameDetails(it.id).region }.getOrNull()
                    ?.takeIf { r -> r.isNotBlank() }
            }.map { regionLabel(it) }.distinct()
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    uiText.text("Pasta"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { favorite = !favorite; onToggleFavorite() }) {
                    Icon(
                        if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = uiText.text("Favoritar"),
                        tint = if (favorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onRename(it.trim())
                },
                label = { Text(uiText.text("Nome da pasta")) },
                placeholder = { Text(uiText.text("Sem nome")) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBg(),
                    unfocusedContainerColor = cardBg(),
                    focusedBorderColor = cardBg(),
                    unfocusedBorderColor = cardBg(),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            GameInfoRow(uiText.text("Jogos"), "${games.size}")
            GameInfoRow(uiText.text("Tamanho total"), formatSize(totalSize))
            GameInfoRow(uiText.text("Regiões"), if (regions.isEmpty()) "…" else regions.joinToString(", "))
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onRemove),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        uiText.text("Remover"),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Compact-grid game visual (3 columns): the cover with the title overlaid on a gradient strip across
// the bottom — no caption line, so more games fit. `gesture` carries tap/long-press-drag.
@Composable
private fun GameCompactVisual(game: GameEntry, highlighted: Boolean, gesture: Modifier) {
    val cover = rememberGameIcon(game.icon)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(appBg())
            .then(
                if (highlighted) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .then(gesture),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Default.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                modifier = Modifier.size(40.dp),
            )
        }
        // Title overlaid on a dark gradient at the bottom of the cover for legibility.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = game.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
    }
}

// List-view game visual: an 88dp cover, a marquee title (20sp), the serial/id and the size. `gesture`
// carries tap/long-press-drag; the cover gains a ring when it is a hovered drop target.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GameListVisual(game: GameEntry, highlighted: Boolean, gesture: Modifier) {
    val cover = rememberGameIcon(game.icon)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(gesture),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(appBg())
                .then(
                    if (highlighted) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = game.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )
            Text(
                text = game.id.ifBlank { game.source },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Text(
                text = formatSize(game.sizeBytes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
    }
}

// Folder as a list row: an 88dp 2x2 mini-cover thumbnail (search-bar colour), the folder name and the
// game count. Tap opens the folder; long-press opens its options. Mirrors the game list row layout.
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderListRow(
    folder: GameFolder,
    gamesByPath: Map<String, GameEntry>,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val covers = folder.gamePaths.mapNotNull { gamesByPath[it] }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(folderBg())
                .then(
                    if (highlighted) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(6.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                for (row in 0 until 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        for (col in 0 until 2) {
                            val g = covers.getOrNull(row * 2 + col)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                if (g != null) {
                                    FolderGameCover(g, 12.dp, Modifier.fillMaxSize())
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(appBg()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = folder.name.ifBlank { "Sem nome" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (folder.favorite) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = "${folder.gamePaths.size} jogos",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
    }
}

// In-memory cache for decoded game icon/cover bitmaps, keyed by file path. Without this, every
// composable that shows the same icon (home grid, game info sheet, Trophies screen, shortcuts...)
// re-decodes the same PNG/JPG from disk on every recomposition -- wasteful and a source of jank
// when scrolling. Sized by actual bitmap byte count (not entry count) so a handful of large cover
// images don't starve the cache the way a fixed entry-count limit would.
private object GameIconCache {
    private const val MAX_BYTES = 48 * 1024 * 1024 // 48 MB of decoded bitmaps
    private val cache = object : android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: androidx.compose.ui.graphics.ImageBitmap): Int =
            value.asAndroidBitmap().byteCount
    }

    fun get(path: String): androidx.compose.ui.graphics.ImageBitmap? = cache.get(path)

    fun getOrDecode(path: String): androidx.compose.ui.graphics.ImageBitmap? {
        cache.get(path)?.let { return it }
        val decoded = runCatching {
            android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap()
        }.getOrNull() ?: return null
        cache.put(path, decoded)
        return decoded
    }
}

// Decodes (or reuses the cached decode of) a game's icon/cover off the main thread. See
// GameIconCache -- repeated calls for the same path after the first are instant.
@Composable
private fun rememberGameIcon(iconPath: String): androidx.compose.ui.graphics.ImageBitmap? {
    if (iconPath.isBlank()) return null
    GameIconCache.get(iconPath)?.let { return it }
    return androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = iconPath,
    ) {
        value = withContext(Dispatchers.IO) { GameIconCache.getOrDecode(iconPath) }
    }.value
}

// Warms GameIconCache for every game's icon in the background, off the main thread, so scrolling
// the home grid right after the library loads doesn't stutter decoding icons for the first time.
// Cheap no-op for anything already cached (e.g. re-warming after a refresh).
private suspend fun precacheGameIcons(games: List<GameEntry>) {
    withContext(Dispatchers.IO) {
        for (game in games) {
            if (game.icon.isNotBlank()) {
                GameIconCache.getOrDecode(game.icon)
            }
        }
    }
}

@Composable
private fun rememberGameBackground(game: GameEntry?): androidx.compose.ui.graphics.ImageBitmap? {
    val path = remember(game?.path, game?.icon) {
        if (game == null) {
            ""
        } else {
            val root = java.io.File(game.path)
            listOf(
                java.io.File(root, "sce_sys/pic1.png"),
                java.io.File(root, "sce_sys/pic0.png"),
                java.io.File(game.icon),
            ).firstOrNull { it.isFile }?.absolutePath.orEmpty()
        }
    }
    if (path.isBlank()) return null
    return androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = path,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}

// === PlayStation 4 console home (landscape PS4 view mode) ==========================================
// Recreates the PS4 home screen (github.com/d4v1-sudo/Ps4-UI): a blue wave backdrop, a top status bar
// (info message, player name, trophy glyph and a live clock) and a horizontal row of large game tiles.
// The focused tile grows with a 3px white border, drops a translucent "Start" bar and shows the game
// title to its right — like a real PS4. Tap a tile to focus it, tap again to launch; long-press opens
// the game info sheet. Landscape only; portrait keeps the normal grid (handled by the caller).
@Composable
private fun Ps4HomeScreen(
    games: List<GameEntry>,
    onLaunch: (GameEntry) -> Unit,
    onInfo: (GameEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    val ps4Blue = Color(0xFF0E6CC4)
    val ps4WaveCyan = Color(0xFF00AAFF)
    val settingsIndex = games.size
    var selected by remember(games.size) { mutableStateOf(0) }
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        runCatching { listState.animateScrollToItem(selected.coerceIn(0, settingsIndex)) }
    }

    var clock by remember { mutableStateOf(ps4ClockNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = ps4ClockNow()
            kotlinx.coroutines.delay(15_000)
        }
    }

    Box(Modifier.fillMaxSize().background(ps4Blue)) {
        // Wave backdrop: large translucent discs anchored to the lower-left, echoing the PS4 curve.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(ps4WaveCyan.copy(alpha = 0.16f), radius = h * 1.15f,
                center = Offset(w * 0.10f, h * 1.10f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = h * 0.92f,
                center = Offset(w * 0.02f, h * 1.18f))
            drawCircle(Color(0xFF0A4E91).copy(alpha = 0.35f), radius = h * 0.7f,
                center = Offset(0f, h * 1.25f))
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 26.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (games.isEmpty()) "Nenhum jogo instalado"
                    else "${games.size} jogos na biblioteca",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(uiText.text("Player"), color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.width(22.dp))
                Icon(Icons.Default.EmojiEvents, null, tint = Color.White,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(22.dp))
                Text(clock, color = Color.White, fontSize = 22.sp)
            }

            Spacer(Modifier.weight(1f))

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                verticalAlignment = Alignment.Bottom,
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(games, key = { _, g -> g.path }) { index, game ->
                    Ps4GameTile(
                        game = game,
                        selected = index == selected,
                        onClick = { if (selected == index) onLaunch(game) else selected = index },
                        onLongClick = { onInfo(game) },
                    )
                }
                item(key = "ps4-settings-tile") {
                    Ps4SystemTile(
                        icon = Icons.Default.Settings,
                        title = uiText.settings,
                        action = uiText.text("Abrir"),
                        selected = selected == settingsIndex,
                        onClick = {
                            if (selected == settingsIndex) onOpenSettings()
                            else selected = settingsIndex
                        },
                    )
                }
            }

            Spacer(Modifier.weight(1.4f))
        }
    }
}

private fun ps4ClockNow(): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Ps4GameTile(
    game: GameEntry,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val cover = rememberGameIcon(game.icon)
    val tileW by animateDpAsState(if (selected) 140.dp else 108.dp, label = "ps4TileW")
    val coverH by animateDpAsState(if (selected) 140.dp else 108.dp, label = "ps4CoverH")
    Row(verticalAlignment = Alignment.Bottom) {
        Column(
            Modifier
                .width(tileW)
                .then(if (selected) Modifier.border(BorderStroke(3.dp, Color.White)) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            Box(
                Modifier.width(tileW).height(coverH).background(Color(0x33000000)),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover,
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.SportsEsports, null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            if (selected) {
                Text(
                    "Start",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().background(Color(0x33FFFFFF))
                        .padding(vertical = 5.dp),
                )
            }
        }
        if (selected) {
            Text(
                game.title,
                color = Color.White,
                fontSize = 18.sp,
                maxLines = 2,
                modifier = Modifier.padding(start = 16.dp).widthIn(max = 280.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Ps4SystemTile(
    icon: ImageVector,
    title: String,
    action: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tileW by animateDpAsState(if (selected) 140.dp else 108.dp, label = "ps4SysW")
    val coverH by animateDpAsState(if (selected) 140.dp else 108.dp, label = "ps4SysH")
    Row(verticalAlignment = Alignment.Bottom) {
        Column(
            Modifier
                .width(tileW)
                .then(if (selected) Modifier.border(BorderStroke(3.dp, Color.White)) else Modifier)
                .clickable(onClick = onClick),
        ) {
            Box(
                Modifier.width(tileW).height(coverH).background(Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
            if (selected) {
                Text(
                    action,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().background(Color(0x33FFFFFF))
                        .padding(vertical = 5.dp),
                )
            }
        }
        if (selected) {
            Text(title, color = Color.White, fontSize = 18.sp,
                modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    snapshot: SettingsSnapshot,
    gpuDrivers: List<GpuDriverEntry>,
    themeMode: ThemeMode,
    materialYou: Boolean,
    useGameBackground: Boolean,
    appLanguage: AppLanguage,
    uiText: AppUiText,
    onThemeModeChange: (ThemeMode) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onUseGameBackgroundChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onSettingChanged: (SettingEntry, String) -> Unit,
    onOpenGpuDrivers: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenDriverChannels: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = { Text(uiText.settings) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        // Categories shown as navigation cards. "Aparência" is a synthetic category (theme/Material
        // You) handled specially in the detail screen; the rest come from the native settings table.
        // The Android/vulkan_driver entry is excluded here (custom drivers live inside the GPU
        // category detail screen).
        val grouped = snapshot.settings
            .filterNot { it.section == "Android" && it.key == "vulkan_driver" }
            .filterNot(::isHiddenGlobalSettingInUi)
            .groupBy(::uiSettingsSection)
        val sections = grouped.entries.toList()
        // Full ordered category list: Aparência first, then each native section. Each carries a
        // localized display name and a meaningful description of what it contains (not just "N opções").
        data class Cat(val title: String, val displayTitle: String, val subtitle: String)
        val categories = buildList {
            add(Cat("Aparência", uiText.appearance, uiText.categoryDescription("Aparência")))
            sections.forEach { (section, entries) ->
                val modifiedCount = entries.count { it.isModified }
                val base = uiText.categoryDescription(section)
                val subtitle = if (modifiedCount > 0) {
                    "$base • ${uiText.text("Alteradas")}: $modifiedCount"
                } else {
                    base
                }
                add(Cat(section, uiText.categoryTitle(section), subtitle))
                // Right below the GPU category card, as part of the very same grouped list (no
                // separate header/section of its own) -- just another entry the user can tap.
                if (section == "GPU") {
                    add(
                        Cat(
                            "gpu-driver-download-channel",
                            uiText.text("GPU driver download channel"),
                            uiText.text("Escolha de onde os drivers de GPU são baixados"),
                        ),
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(categories, key = { _, c -> "cat-${c.title}" }) { index, cat ->
                val top = if (index == 0) 15.dp else 5.dp
                val bottom = if (index == categories.lastIndex) 15.dp else 5.dp
                CategoryNavCard(
                    title = cat.title,
                    displayTitle = cat.displayTitle,
                    subtitle = cat.subtitle,
                    shape = RoundedCornerShape(
                        topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                    ),
                    onClick = {
                        if (cat.title == "gpu-driver-download-channel") {
                            onOpenDriverChannels()
                        } else {
                            onOpenCategory(cat.title)
                        }
                    },
                )
            }
        }
    }
}

// Navigation card for a settings category (title + subtitle + chevron), styled like the other
// settings cards (Material 3 surface, grouped rounded corners).
@Composable
private fun CategoryNavCard(
    title: String,
    displayTitle: String,
    subtitle: String,
    shape: Shape,
    onClick: () -> Unit,
) {
    val (icon, color) = sectionVisual(title)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionIconBadge(icon, color)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Detail screen for a single settings category: app bar (same color as the app background) with back
// button + category title, and the category's cards. Special categories: "Aparência" shows the theme
// + Material You cards; "GPU" also shows the custom-drivers nav card on top of the GPU settings.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryScreen(
    category: String,
    settings: List<SettingEntry>,
    onBack: () -> Unit,
    onSettingChanged: (SettingEntry, String) -> Unit,
    themeMode: ThemeMode,
    materialYou: Boolean,
    useGameBackground: Boolean,
    appLanguage: AppLanguage,
    uiText: AppUiText,
    onThemeModeChange: (ThemeMode) -> Unit,
    onMaterialYouChange: (Boolean) -> Unit,
    onUseGameBackgroundChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    gpuDriverValue: String,
    onOpenGpuDrivers: () -> Unit,
    onInstallLsfgDll: (Uri) -> Unit,
    viewMode: org.sharpemu.android.ui.theme.ViewMode,
    onViewModeChange: (org.sharpemu.android.ui.theme.ViewMode) -> Unit,
    sortMode: org.sharpemu.android.data.GameSortMode,
    onSortModeChange: (org.sharpemu.android.data.GameSortMode) -> Unit,
    onOpenLogs: () -> Unit,
) {
    val isAppearance = category == "Aparência"
    val isGpu = category == "GPU"
    val isVulkan = category == "Vulkan"
    val isLog = category == "Log"
    val isAudio = category == "Audio"
    val isAndroid = category == "Android"
    val visibleSettings = remember(category, settings) { settings.filterNot(::isHiddenGlobalSettingInUi) }
    val groupedGeneralSettings = remember(visibleSettings) {
        if (category == "General") visibleSettings.groupBy(::generalSettingGroup) else emptyMap()
    }
    val groupedLogSettings = remember(visibleSettings) {
        if (category == "Log") visibleSettings.groupBy(::logSettingGroup) else emptyMap()
    }
    val groupedAudioSettings = remember(visibleSettings) {
        if (category == "Audio") visibleSettings.groupBy(::audioSettingGroup) else emptyMap()
    }
    val groupedGpuSettings = remember(visibleSettings) {
        if (category == "GPU") visibleSettings.groupBy(::gpuSettingGroup) else emptyMap()
    }
    val groupedVulkanSettings = remember(visibleSettings) {
        if (category == "Vulkan") visibleSettings.groupBy(::vulkanSettingGroup) else emptyMap()
    }
    val groupedAndroidSettings = remember(visibleSettings) {
        if (category == "Android") visibleSettings.groupBy(::androidSettingGroup) else emptyMap()
    }
    var showViewModeSheet by remember { mutableStateOf(false) }
    var showAppLanguageSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val lsfgDllPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            onInstallLsfgDll(uri)
        }
    }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = { Text(uiText.categoryTitle(category)) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (isAppearance) {
                item(key = "appearance-header-theme") {
                    Text(
                        uiText.theme,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                    )
                }
                val themeOptions = listOf(
                    Triple(ThemeMode.SYSTEM, uiText.systemTheme, Icons.Default.PhoneAndroid),
                    Triple(ThemeMode.LIGHT, uiText.lightTheme, Icons.Default.LightMode),
                    Triple(ThemeMode.DARK, uiText.darkTheme, Icons.Default.DarkMode),
                )
                itemsIndexed(themeOptions, key = { _, t -> "theme-${t.first}" }) { idx, (mode, label, icon) ->
                    val top = if (idx == 0) 15.dp else 5.dp
                    val bottom = if (idx == themeOptions.lastIndex) 15.dp else 5.dp
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeChange(mode) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(icon, Color(0xFF6750A4))
                            Spacer(Modifier.width(14.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            AppSwitch(
                                checked = themeMode == mode,
                                onCheckedChange = { if (it) onThemeModeChange(mode) },
                            )
                        }
                    }
                }
                item(key = "appearance-header-color") {
                    Text(
                        uiText.colors,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                    )
                }
                item(key = "appearance-material-you") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.Palette, Color(0xFFD81B60))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.materialYou,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    uiText.materialYouDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AppSwitch(checked = materialYou, onCheckedChange = onMaterialYouChange)
                        }
                    }
                }
                item(key = "appearance-header-language") {
                    Text(
                        uiText.language,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                    )
                }
                item(key = "appearance-app-language") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAppLanguageSheet = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.Language, Color(0xFF3949AB))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.language,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    appUiText(appLanguage).languageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item(key = "appearance-header-view") {
                    Text(
                        uiText.library,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                    )
                }
                item(key = "appearance-view-mode") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 15.dp, topEnd = 15.dp, bottomStart = 5.dp, bottomEnd = 5.dp,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showViewModeSheet = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.GridView, Color(0xFF00897B))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.viewMode,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    viewModeLabel(viewMode, uiText),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item(key = "appearance-sort-by") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(5.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSortSheet = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.Sort, Color(0xFF00897B))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.text("Ordenar por"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    sortModeLabel(sortMode, uiText),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            AppSettingInfoButton(
                                title = uiText.text("Ordenar por"),
                                description = uiText.text(
                                    "Define a ordem dos jogos na tela inicial e dentro das pastas.",
                                ),
                                closeText = uiText.close,
                            )
                        }
                    }
                }
                item(key = "appearance-game-background") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 5.dp, topEnd = 5.dp, bottomStart = 15.dp, bottomEnd = 15.dp,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.Wallpaper, Color(0xFF00897B))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.useGameBackground,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            AppSettingInfoButton(
                                title = uiText.useGameBackground,
                                description = uiText.useGameBackgroundDescription,
                                closeText = uiText.close,
                            )
                            AppSwitch(
                                checked = useGameBackground,
                                onCheckedChange = onUseGameBackgroundChange,
                            )
                        }
                    }
                }
            }
            if (isLog) {
                item(key = "log-header-file") {
                    Text(
                        uiText.text("Arquivo de registro"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                    )
                }
                item(key = "log-view-logs") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 15.dp,
                            topEnd = 15.dp,
                            bottomStart = if (groupedLogSettings["Arquivo"].isNullOrEmpty()) 15.dp else 5.dp,
                            bottomEnd = if (groupedLogSettings["Arquivo"].isNullOrEmpty()) 15.dp else 5.dp,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLogs() }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.Description, Color(0xFF5E35B1))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.text("Ver logs"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    uiText.text("Abrir, exportar ou apagar o sharpemu_log.txt"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (isGpu && isAdrenoDevice()) {
                item(key = "gpu-header-driver") {
                    Text(
                        uiText.text("Driver e recursos"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                    )
                }
                item(key = "gpu-custom-drivers") {
                    GpuDriverNavCard(
                        activeDriver = gpuDriverValue,
                        onClick = onOpenGpuDrivers,
                        // Bottom corners must match the "gpu-lsfg-dll" card's top corners (5.dp,
                        // set right below) so the two cards read as one seamless grouped block --
                        // this card previously stayed at a fixed 15.dp/15.dp/15.dp/15.dp regardless,
                        // which is why the border between them looked inconsistent.
                        shape = RoundedCornerShape(
                            topStart = 15.dp,
                            topEnd = 15.dp,
                            bottomStart = 5.dp,
                            bottomEnd = 5.dp,
                        ),
                    )
                }
            }
            if (isGpu) {
                if (!isAdrenoDevice()) {
                    item(key = "gpu-header-driver") {
                        Text(
                            uiText.text("Driver e recursos"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                        )
                    }
                }
                item(key = "gpu-lsfg-dll") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = if (isAdrenoDevice()) 5.dp else 15.dp,
                            topEnd = if (isAdrenoDevice()) 5.dp else 15.dp,
                            bottomStart = 15.dp,
                            bottomEnd = 15.dp,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { lsfgDllPicker.launch(arrayOf("*/*")) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionIconBadge(Icons.Default.AutoAwesome, Color(0xFF00897B))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiText.text("Selecionar Lossless.dll"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    uiText.text("Extrai os shaders SPIR-V do LSFG para o frame generation nativo"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (category == "General") {
                GeneralSettingsGroupOrder.forEach { group ->
                    val entries = groupedGeneralSettings[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "general-header-$group") {
                            Text(
                                uiText.text(group),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                            )
                        }
                        itemsIndexed(entries, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                            val top = if (idx == 0) 15.dp else 5.dp
                            val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                            SettingCard(
                                setting = setting,
                                uiText = uiText,
                                shape = RoundedCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                ),
                                onChanged = { onSettingChanged(setting, it) },
                            )
                        }
                    }
                }
            } else if (isGpu) {
                GpuSettingsGroupOrder.forEach { group ->
                    val entries = groupedGpuSettings[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "gpu-header-$group") {
                            Text(
                                uiText.text(group),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                            )
                        }
                        itemsIndexed(entries, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                            val top = if (idx == 0) 15.dp else 5.dp
                            val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                            SettingCard(
                                setting = setting,
                                uiText = uiText,
                                shape = RoundedCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                ),
                                onChanged = { onSettingChanged(setting, it) },
                            )
                        }
                    }
                }
            } else if (isVulkan) {
                VulkanSettingsGroupOrder.forEach { group ->
                    val entries = groupedVulkanSettings[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "vulkan-header-$group") {
                            Text(
                                uiText.text(group),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                            )
                        }
                        itemsIndexed(entries, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                            val top = if (idx == 0) 15.dp else 5.dp
                            val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                            SettingCard(
                                setting = setting,
                                uiText = uiText,
                                shape = RoundedCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                ),
                                onChanged = { onSettingChanged(setting, it) },
                            )
                        }
                    }
                }
            } else if (isAndroid) {
                AndroidSettingsGroupOrder.forEach { group ->
                    val entries = groupedAndroidSettings[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "android-header-$group") {
                            Text(
                                uiText.text(group),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                            )
                        }
                        itemsIndexed(entries, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                            val top = if (idx == 0) 15.dp else 5.dp
                            val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                            SettingCard(
                                setting = setting,
                                uiText = uiText,
                                shape = RoundedCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                ),
                                onChanged = { onSettingChanged(setting, it) },
                            )
                        }
                    }
                }
            } else if (isLog) {
                LogSettingsGroupOrder.forEach { group ->
                    val entries = groupedLogSettings[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        if (group != "Arquivo") {
                            item(key = "log-header-$group") {
                                Text(
                                    uiText.text(group),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                                )
                            }
                        }
                        itemsIndexed(entries, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                            val top = if (group == "Arquivo") 5.dp else if (idx == 0) 15.dp else 5.dp
                            val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                            SettingCard(
                                setting = setting,
                                uiText = uiText,
                                shape = RoundedCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                ),
                                onChanged = { onSettingChanged(setting, it) },
                            )
                        }
                    }
                }
            } else if (isAudio) {
                AudioSettingsGroupOrder.forEach { group ->
                    val entries = groupedAudioSettings[group].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "audio-header-$group") {
                            Text(
                                uiText.text(group),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
                            )
                        }
                        itemsIndexed(entries, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                            val top = if (idx == 0) 15.dp else 5.dp
                            val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                            SettingCard(
                                setting = setting,
                                uiText = uiText,
                                shape = RoundedCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                ),
                                onChanged = { onSettingChanged(setting, it) },
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(visibleSettings, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                    val top = if (idx == 0) 15.dp else 5.dp
                    val bottom = if (idx == visibleSettings.lastIndex) 15.dp else 5.dp
                    SettingCard(
                        setting = setting,
                        uiText = uiText,
                        shape = RoundedCornerShape(
                            topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                        ),
                        onChanged = { onSettingChanged(setting, it) },
                    )
                }
            }
        }
    }

    if (showViewModeSheet) {
        ViewModeSheet(
            current = viewMode,
            uiText = uiText,
            onSelect = {
                onViewModeChange(it)
                showViewModeSheet = false
            },
            onDismiss = { showViewModeSheet = false },
        )
    }
    if (showAppLanguageSheet) {
        AppLanguageSheet(
            current = appLanguage,
            uiText = uiText,
            onSelect = {
                onAppLanguageChange(it)
                showAppLanguageSheet = false
            },
            onDismiss = { showAppLanguageSheet = false },
        )
    }
    if (showSortSheet) {
        SortBySheet(
            current = sortMode,
            uiText = uiText,
            onSelect = {
                onSortModeChange(it)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

private fun viewModeLabel(mode: org.sharpemu.android.ui.theme.ViewMode, uiText: AppUiText): String = when (mode) {
    org.sharpemu.android.ui.theme.ViewMode.GRID -> uiText.grid
    org.sharpemu.android.ui.theme.ViewMode.LIST -> uiText.list
    org.sharpemu.android.ui.theme.ViewMode.GRID_COMPACT -> uiText.compactGrid
}

private fun sortModeLabel(mode: org.sharpemu.android.data.GameSortMode, uiText: AppUiText): String = when (mode) {
    org.sharpemu.android.data.GameSortMode.NAME_ASC -> uiText.text("Nome (A-Z)")
    org.sharpemu.android.data.GameSortMode.NAME_DESC -> uiText.text("Nome (Z-A)")
    org.sharpemu.android.data.GameSortMode.RECENTLY_ADDED -> uiText.text("Adicionados recentemente")
}

// "Last played" relative-time label for the game info sheet (e.g. "Jogado há 3 horas" / "Played 3
// hours ago" / "Jugado hace 3 horas"), hand-translated per language -- not run through the automatic
// per-token key fallback, since it needs real grammar (singular/plural, word order) around a number.
private fun relativeLastPlayedLabel(context: Context, lastPlayedMillis: Long): String {
    val language = AppearancePrefs.getAppLanguage(context)
    if (lastPlayedMillis <= 0L) {
        return when (language) {
            AppLanguage.PORTUGUESE -> "Nunca jogado"
            AppLanguage.SPANISH -> "Nunca jugado"
            AppLanguage.ENGLISH -> "Never played"
        }
    }
    val diffMs = (System.currentTimeMillis() - lastPlayedMillis).coerceAtLeast(0L)
    val minutes = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L
    val weeks = days / 7L
    val months = days / 30L
    val years = days / 365L
    fun pt(n: Long, singular: String, plural: String) = "Jogado há " + (if (n == 1L) "1 $singular" else "$n $plural")
    fun es(n: Long, singular: String, plural: String) = "Jugado hace " + (if (n == 1L) "1 $singular" else "$n $plural")
    fun en(n: Long, singular: String, plural: String) =
        (if (n == 1L) "Played 1 $singular ago" else "Played $n $plural ago")
    return when {
        minutes < 1L -> when (language) {
            AppLanguage.PORTUGUESE -> "Jogado agora"
            AppLanguage.SPANISH -> "Jugado ahora"
            AppLanguage.ENGLISH -> "Played just now"
        }
        hours < 1L -> when (language) {
            AppLanguage.PORTUGUESE -> pt(minutes, "minuto", "minutos")
            AppLanguage.SPANISH -> es(minutes, "minuto", "minutos")
            AppLanguage.ENGLISH -> en(minutes, "minute", "minutes")
        }
        days < 1L -> when (language) {
            AppLanguage.PORTUGUESE -> pt(hours, "hora", "horas")
            AppLanguage.SPANISH -> es(hours, "hora", "horas")
            AppLanguage.ENGLISH -> en(hours, "hour", "hours")
        }
        weeks < 1L -> when (language) {
            AppLanguage.PORTUGUESE -> pt(days, "dia", "dias")
            AppLanguage.SPANISH -> es(days, "día", "días")
            AppLanguage.ENGLISH -> en(days, "day", "days")
        }
        months < 1L -> when (language) {
            AppLanguage.PORTUGUESE -> pt(weeks, "semana", "semanas")
            AppLanguage.SPANISH -> es(weeks, "semana", "semanas")
            AppLanguage.ENGLISH -> en(weeks, "week", "weeks")
        }
        years < 1L -> when (language) {
            AppLanguage.PORTUGUESE -> pt(months, "mês", "meses")
            AppLanguage.SPANISH -> es(months, "mes", "meses")
            AppLanguage.ENGLISH -> en(months, "month", "months")
        }
        else -> when (language) {
            AppLanguage.PORTUGUESE -> pt(years, "ano", "anos")
            AppLanguage.SPANISH -> es(years, "año", "años")
            AppLanguage.ENGLISH -> en(years, "year", "years")
        }
    }
}

// Bottom sheet to pick how the home library (and folder contents) is ordered. Default is Name (A-Z);
// whichever game was most recently launched still always jumps to the front regardless of this choice.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBySheet(
    current: org.sharpemu.android.data.GameSortMode,
    uiText: AppUiText,
    onSelect: (org.sharpemu.android.data.GameSortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
            Text(
                uiText.text("Ordenar por"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
            val options = listOf(
                Triple(
                    org.sharpemu.android.data.GameSortMode.NAME_ASC,
                    uiText.text("Nome (A-Z)"),
                    Icons.Default.Sort,
                ),
                Triple(
                    org.sharpemu.android.data.GameSortMode.NAME_DESC,
                    uiText.text("Nome (Z-A)"),
                    Icons.Default.Sort,
                ),
                Triple(
                    org.sharpemu.android.data.GameSortMode.RECENTLY_ADDED,
                    uiText.text("Adicionados recentemente"),
                    Icons.Default.Sort,
                ),
            )
            options.forEach { (mode, label, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    RadioButton(selected = current == mode, onClick = { onSelect(mode) })
                }
            }
        }
    }
}

// Bottom sheet to pick the home library layout (Grid / List / Grid compact).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeSheet(
    current: org.sharpemu.android.ui.theme.ViewMode,
    uiText: AppUiText,
    onSelect: (org.sharpemu.android.ui.theme.ViewMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
            Text(
                uiText.viewMode,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
            val options = listOf(
                Triple(org.sharpemu.android.ui.theme.ViewMode.GRID, uiText.grid, Icons.Default.GridView),
                Triple(org.sharpemu.android.ui.theme.ViewMode.LIST, uiText.list, Icons.Default.ViewList),
                Triple(
                    org.sharpemu.android.ui.theme.ViewMode.GRID_COMPACT,
                    uiText.compactGrid,
                    Icons.Default.Apps,
                ),
            )
            options.forEach { (mode, label, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    RadioButton(selected = current == mode, onClick = { onSelect(mode) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageSheet(
    current: AppLanguage,
    uiText: AppUiText,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    org.sharpemu.android.ui.OverlayBlurEffect()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Transparent,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
            Text(
                uiText.language,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            )
            AppLanguage.entries.forEach { language ->
                val label = appUiText(language).languageName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(language) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    RadioButton(selected = current == language, onClick = { onSelect(language) })
                }
            }
        }
    }
}

// Controls screen (opened from the Input category): the on-screen touch-control options adapted from
// the PCSX2-Android control settings (show overlay, idle opacity, haptics) plus all the emulator's
// current Input settings. Per-button position/scale/opacity is edited live in-game via the overlay's
// edit mode (PadOverlay), so it isn't duplicated here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsScreen(
    inputSettings: List<SettingEntry>,
    onBack: () -> Unit,
    onSettingChanged: (SettingEntry, String) -> Unit,
    onOpenVirtualButtons: () -> Unit,
    onOpenControllerConfig: (Int) -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var showOverlay by remember { mutableStateOf(ControlPrefs.getShowOverlay(context)) }
    var haptic by remember { mutableStateOf(ControlPrefs.getHaptic(context)) }
    var centerAnalog by remember { mutableStateOf(ControlPrefs.getCenterAnalogOnTouch(context)) }
    var stickRegions by remember { mutableStateOf(ControlPrefs.getStickRegionsEnabled(context)) }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = { Text(uiText.text("Controles")) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // --- On-screen controls (touch overlay) ---
            item(key = "ctl-header-overlay") {
                Text(
                    uiText.text("Controles na tela"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
                )
            }
            item(key = "ctl-show-overlay") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 15.dp, topEnd = 15.dp, bottomStart = 5.dp, bottomEnd = 5.dp,
                    ),
                    colors = CardDefaults.cardColors(containerColor = cardBg()),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiText.text("Mostrar controles na tela"),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                uiText.text("Exibe o gamepad virtual ao iniciar um jogo"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppSwitch(
                            checked = showOverlay,
                            onCheckedChange = {
                                showOverlay = it
                                ControlPrefs.setShowOverlay(context, it)
                            },
                        )
                    }
                }
            }
            item(key = "ctl-haptic") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(5.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg()),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            uiText.text("Vibrar ao tocar"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        AppSwitch(
                            checked = haptic,
                            onCheckedChange = {
                                haptic = it
                                ControlPrefs.setHaptic(context, it)
                            },
                        )
                    }
                }
            }
            item(key = "ctl-center-analog") {
                ControlSwitchCard(
                    title = uiText.text("Centralizar analógico ao toque"),
                    subtitle = uiText.text("O analógico nasce onde você toca"),
                    checked = centerAnalog,
                    shape = RoundedCornerShape(5.dp),
                    onChange = {
                        centerAnalog = it
                        ControlPrefs.setCenterAnalogOnTouch(context, it)
                    },
                )
            }
            item(key = "ctl-stick-regions") {
                ControlSwitchCard(
                    title = uiText.text("Regiões de analógico (stick regions)"),
                    subtitle = uiText.text("Define áreas fixas para os analógicos"),
                    checked = stickRegions,
                    shape = RoundedCornerShape(5.dp),
                    onChange = {
                        stickRegions = it
                        ControlPrefs.setStickRegionsEnabled(context, it)
                    },
                )
            }
            item(key = "ctl-virtual-order") {
                Card(
                    onClick = onOpenVirtualButtons,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 5.dp, topEnd = 5.dp, bottomStart = 15.dp, bottomEnd = 15.dp,
                    ),
                    colors = CardDefaults.cardColors(containerColor = cardBg()),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiText.text("Ordem dos botões virtuais"),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                uiText.text("Reordenar e mostrar/ocultar grupos de botões"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item(key = "ctl-note") {
                Text(
                    uiText.text(
                        "Dica: dentro do jogo, toque no botão de overlay e use o modo de edição para mover e redimensionar cada botão individualmente.",
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }

            // --- Physical controllers (up to 4 slots) ---
            item(key = "ctl-header-physical") {
                Text(
                    uiText.text("Controle físico"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 2.dp),
                )
            }
            items((1..4).toList(), key = { "ctl-slot-$it" }) { slotNum ->
                val type = ControlPrefs.getControlType(context, slotNum)
                val top = if (slotNum == 1) 15.dp else 5.dp
                val bottom = if (slotNum == 4) 15.dp else 5.dp
                Card(
                    onClick = { onOpenControllerConfig(slotNum) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                    ),
                    colors = CardDefaults.cardColors(containerColor = cardBg()),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${uiText.text("Controle")} $slotNum",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                type,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // --- Current emulator Input settings ---
            if (inputSettings.isNotEmpty()) {
                item(key = "ctl-header-input") {
                    Text(
                        uiText.text("Entrada"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                    )
                }
                itemsIndexed(inputSettings, key = { _, s -> "${s.section}.${s.key}" }) { idx, setting ->
                    val top = if (idx == 0) 15.dp else 5.dp
                    val bottom = if (idx == inputSettings.lastIndex) 15.dp else 5.dp
                    SettingCard(
                        setting = setting,
                        uiText = uiText,
                        shape = RoundedCornerShape(
                            topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                        ),
                        onChanged = { onSettingChanged(setting, it) },
                    )
                }
            }
        }
    }
}

// Reusable switch card for the Controls screen (title + subtitle + Switch).
@Composable
private fun ControlSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    shape: Shape,
    onChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AppSwitch(checked = checked, onCheckedChange = onChange)
        }
    }
}

// Per-game settings screen: mirrors the general settings layout (grouped, rounded SettingCards) but
// edits this game's per-game override config (like shadPS4 PC's per-game configuration). Appearance
// and the GPU-driver picker are global-only and intentionally not shown here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameSettingsScreen(
    game: GameEntry,
    snapshot: SettingsSnapshot,
    onBack: () -> Unit,
    onSettingChanged: (SettingEntry, String) -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = {
                    Column {
                        Text(uiText.text("Configurações do jogo"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            game.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        // Per-game config covers only the emulator settings (skip the Android vulkan_driver entry,
        // which is a global device-driver choice).
        val grouped = snapshot.settings
            .filterNot { it.section == "Android" && it.key == "vulkan_driver" }
            .groupBy { it.section }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val sections = grouped.entries.toList()
            sections.forEachIndexed { index, (section, entries) ->
                item(key = "gs-header-$section") {
                    Text(
                        uiText.categoryTitle(section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )
                }
                items(entries, key = { "gs-${it.section}.${it.key}" }) { setting ->
                    val idx = entries.indexOf(setting)
                    val top = if (idx == 0) 15.dp else 5.dp
                    val bottom = if (idx == entries.lastIndex) 15.dp else 5.dp
                    SettingCard(
                        setting = setting,
                        uiText = uiText,
                        shape = RoundedCornerShape(
                            topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                        ),
                        onChanged = { onSettingChanged(setting, it) },
                    )
                }
                if (index < sections.size - 1) {
                    item(key = "gs-divider-$section") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

// Path to the emulator log file.
private fun shadLogFile(context: Context): java.io.File =
    java.io.File(context.getExternalFilesDir(null), "SharpEmu/log/sharpemu_log.txt")

// Saves the log to public Downloads (via MediaStore, no permission needed on A10+) and opens a share
// sheet so the user can send it to another app. Returns a status message.
private fun exportShadLog(context: Context): String {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    val src = shadLogFile(context)
    if (!src.exists() || src.length() == 0L) return uiText.text("Nenhum log para exportar")
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val name = "sharpemu_log_$stamp.txt"
    var savedTo = ""
    runCatching {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(
                android.provider.MediaStore.Downloads.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_DOWNLOADS,
            )
        }
        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values,
        )
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            }
            savedTo = "Downloads/$name"
        }
    }
    // Offer to share the log with another app (Telegram, Drive, etc.).
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", src,
        )
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(share, uiText.text("Exportar logs")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
    return if (savedTo.isNotEmpty()) "${uiText.text("Log salvo em")} $savedTo"
    else uiText.text("Compartilhando log…")
}

// Logs screen: shows the current sharpemu_log.txt and lets the user export it (share / save to Downloads)
// or clear it. Reached from the home app bar's logs button.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var content by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    LaunchedEffect(reloadKey) {
        content = withContext(Dispatchers.IO) {
            runCatching {
                val f = shadLogFile(context)
                if (f.exists()) f.readText().takeLast(200_000) else ""
            }.getOrDefault("")
        }
    }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBg()),
                title = { Text(uiText.text("Logs")) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = {
                        val msg = exportShadLog(context)
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG)
                            .show()
                    }) {
                        Icon(Icons.Default.Download, contentDescription = uiText.text("Exportar logs"))
                    }
                    IconButton(onClick = {
                        val f = shadLogFile(context)
                        val ok = runCatching { if (f.exists()) f.delete() else true }
                            .getOrDefault(false)
                        reloadKey++
                        android.widget.Toast.makeText(
                            context,
                            if (ok) uiText.text("Logs apagados") else uiText.text("Falha ao apagar logs"),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = uiText.text("Apagar logs"))
                    }
                },
            )
        },
    ) { padding ->
        if (content.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    uiText.text("Nenhum log disponível"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

// SFO viewer: lists every param.sfo key/value of a game (mirrors the desktop "SFO Viewer"). The
// details are loaded by the caller; while null a spinner is shown.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SfoViewerScreen(
    game: GameEntry,
    details: GameDetails?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = {
                    Column {
                        Text(uiText.text("SFO Viewer"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            game.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        when {
            details == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }

            details.sfo.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    uiText.text("param.sfo não encontrado para este jogo"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(details.sfo, key = { it.key }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBg(),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                entry.key,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                entry.value.ifBlank { "—" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Maps a trophy grade letter (ttype) to a label + accent color (matches the PS4 trophy tiers).
private fun trophyGrade(grade: String): Pair<String, Color> = when (grade.uppercase(java.util.Locale.US)) {
    "P" -> "Platina" to Color(0xFF7FB3D5)
    "G" -> "Ouro" to Color(0xFFD4AC0D)
    "S" -> "Prata" to Color(0xFFAAB7B8)
    "B" -> "Bronze" to Color(0xFFB9770E)
    else -> grade to Color.Gray
}

// Trophy viewer: lists a game's trophies parsed from trophy00.trp (icons + name + detail + grade),
// mirroring the desktop Trophy Viewer. Earned status requires the game to have run, so trophies are
// shown as definitions. info == null while loading.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrophyViewerScreen(
    game: GameEntry,
    info: TrophyInfo?,
    onBack: () -> Unit,
    onlyUnlocked: Boolean = false,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    val displayedTrophies = remember(info, onlyUnlocked) {
        val all = info?.trophies.orEmpty()
        if (onlyUnlocked) all.filter { it.unlocked } else all
    }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = {
                    Column {
                        Text(uiText.text("Troféus"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            game.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        when {
            info == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                AppLoadingIndicator(modifier = Modifier.size(72.dp))
            }

            !info.available || displayedTrophies.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        info.reason.ifBlank {
                            if (onlyUnlocked) {
                                uiText.text("Nenhum troféu desbloqueado ainda")
                            } else {
                                uiText.text("Nenhum troféu encontrado")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(displayedTrophies, key = { it.id }) { trophy ->
                    val (gradeLabel, gradeColor) = trophyGrade(trophy.grade)
                    val icon = rememberGameIcon(trophy.icon)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBg(),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = trophy.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = gradeColor,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    trophy.name.ifBlank { "Troféu ${trophy.id}" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (trophy.detail.isNotBlank()) {
                                    Text(
                                        trophy.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = gradeColor.copy(alpha = 0.18f),
                                    ) {
                                        Text(
                                            gradeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = gradeColor,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 2.dp,
                                            ),
                                        )
                                    }
                                    if (trophy.hidden) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "oculto",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global trophy overview: behaves like a notification area, not a game library. Only shows games
// with at least one REALLY unlocked trophy (see GameTrophySummary.unlockedCount, sourced from the
// game's own trophy save). Each row shows the game icon, its (marquee-scrolling) title, a progress
// bar for the percentage of trophies unlocked, and a compact count per tier (Platinum/Gold/Silver/
// Bronze). Tapping a row opens that game's UNLOCKED trophies only (TrophyViewerScreen, onlyUnlocked
// = true) -- this is "what did I just earn", not the game's full trophy list. Long-pressing a row
// offers to dismiss it (comes back once a new trophy is earned) or archive it (hidden until the
// user switches to the Archived view from the app bar).
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TrophiesOverviewScreen(
    summaries: List<org.sharpemu.android.model.GameTrophySummary>,
    loading: Boolean,
    uiText: AppUiText,
    onBack: () -> Unit,
    onOpenGame: (GameEntry) -> Unit,
) {
    val context = LocalContext.current
    var showArchived by remember { mutableStateOf(false) }
    var notifTick by remember { mutableStateOf(0) }
    var confirmTarget by remember { mutableStateOf<org.sharpemu.android.model.GameTrophySummary?>(null) }
    val earned = remember(summaries) { summaries.filter { it.unlockedCount > 0 } }
    val visible = remember(earned, showArchived, notifTick) {
        earned.filter { summary ->
            val archived = org.sharpemu.android.data.TrophyNotificationPrefs.isArchived(context, summary.game.id)
            if (showArchived) {
                archived
            } else {
                !archived && org.sharpemu.android.data.TrophyNotificationPrefs.isActiveNotification(
                    context, summary.game.id, summary.unlockedCount,
                )
            }
        }
    }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBg()),
                title = { Text(uiText.text("Troféus")) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = { showArchived = !showArchived }) {
                        Icon(
                            if (showArchived) Icons.Default.Notifications else Icons.Default.Archive,
                            contentDescription = uiText.text(if (showArchived) "Notificações" else "Arquivados"),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                AppLoadingIndicator(modifier = Modifier.size(72.dp))
            }

            visible.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiText.text(
                            if (showArchived) "Nenhum troféu arquivado" else "Nenhuma novidade de troféu",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(visible, key = { _, s -> s.game.id }) { index, summary ->
                    val top = if (index == 0) 15.dp else 5.dp
                    val bottom = if (index == visible.lastIndex) 15.dp else 5.dp
                    val icon = rememberGameIcon(summary.game.icon)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onOpenGame(summary.game) },
                                onLongClick = { confirmTarget = summary },
                            ),
                        shape = RoundedCornerShape(
                            topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = summary.game.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = summary.game.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .width(110.dp)
                                    .basicMarquee(iterations = Int.MAX_VALUE),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { summary.percent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${summary.percent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                summary.tierCounts.forEach { (grade, unlocked, total) ->
                                    val (_, gradeColor) = trophyGrade(grade)
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = gradeColor,
                                        modifier = Modifier.size(16.dp).padding(start = 6.dp),
                                    )
                                    Text(
                                        "$unlocked/$total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmTarget?.let { summary ->
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            title = { Text(summary.game.title) },
            text = { Text(uiText.text("Apagar remove até o próximo troféu conquistado; arquivar guarda numa lista separada.")) },
            confirmButton = {
                TextButton(onClick = {
                    org.sharpemu.android.data.TrophyNotificationPrefs.archive(context, summary.game.id)
                    notifTick++
                    confirmTarget = null
                }) { Text(uiText.text("Arquivar")) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmTarget = null }) { Text(uiText.text("Cancelar")) }
                    TextButton(onClick = {
                        org.sharpemu.android.data.TrophyNotificationPrefs.dismiss(
                            context, summary.game.id, summary.unlockedCount,
                        )
                        notifTick++
                        confirmTarget = null
                    }) { Text(uiText.text("Apagar")) }
                }
            },
        )
    }
}

// Shared empty-state for the Patches/Cheats tabs (icon + centered message).
@Composable
private fun PatchesEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// Cheats/Patches manager: lists the patches installed for a game (<appRoot>/patches/<id>.xml) with
// per-patch switches, and lets the user download a patch from the official shadps4 ps4_cheats repo or
// import one from device storage. Enabled patches are applied by the core at launch (--patch).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatchesScreen(
    game: GameEntry,
    patchInfo: PatchInfo?,
    cheatInfo: CheatInfo?,
    onBack: () -> Unit,
    onTogglePatch: (String, Boolean) -> Unit,
    onFetchRepo: () -> List<RepoPatch>,
    onDownloadPatch: (RepoPatch) -> Unit,
    onImportPatch: (Uri) -> Unit,
    onToggleCheat: (CheatEntry, Boolean) -> Unit,
    onDownloadCheat: () -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var tab by remember { mutableStateOf(0) }
    var showDownload by remember { mutableStateOf(false) }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) onImportPatch(it)
    }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = {
                    Column {
                        Text(uiText.text("Cheats / Patches"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            game.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = appBg(),
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(uiText.text("Patches")) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(uiText.text("Cheats")) })
            }

            if (tab == 0) {
                // --- Patches tab ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { showDownload = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(uiText.text("Baixar do repositório"))
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            importPicker.launch(arrayOf("text/xml", "application/xml", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(uiText.text("Importar .xml"))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (patchInfo != null && patchInfo.available && patchInfo.patches.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(patchInfo.patches, key = { it.name }) { patch ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBg(),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            patch.name.ifBlank { "Patch" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        val sub = buildList {
                                            if (patch.author.isNotBlank()) add(patch.author)
                                            if (patch.appVer.isNotBlank()) add("v${patch.appVer}")
                                        }.joinToString(" • ")
                                        if (sub.isNotBlank()) {
                                            Text(
                                                sub,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    AppSwitch(
                                        checked = patch.enabled,
                                        onCheckedChange = { onTogglePatch(patch.name, it) },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    PatchesEmptyState(
                        uiText.text("Nenhum patch instalado. Baixe do repositório oficial ou importe um arquivo .xml."),
                    )
                }
            } else {
                // --- Cheats tab ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onDownloadCheat,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(uiText.text("Baixar cheats do repositório"))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                when {
                    cheatInfo == null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { androidx.compose.material3.CircularProgressIndicator() }

                    !cheatInfo.available || cheatInfo.cheats.isEmpty() -> PatchesEmptyState(
                        uiText.text("Nenhum cheat instalado. Toque em \"Baixar cheats do repositório\" para buscar cheats deste jogo."),
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(cheatInfo.cheats, key = { it.name }) { cheat ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBg(),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleCheat(cheat, !cheat.enabled) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = cheat.enabled,
                                        onCheckedChange = { onToggleCheat(cheat, it) },
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        cheat.name.ifBlank { "Cheat" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        if (cheatInfo.credits.isNotBlank()) {
                            item(key = "credits") {
                                Text(
                                    "${uiText.text("Créditos")}: ${cheatInfo.credits}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDownload) {
        var repoList by remember { mutableStateOf<List<RepoPatch>?>(null) }
        var query by remember { mutableStateOf(game.title) }
        LaunchedEffect(Unit) {
            repoList = withContext(Dispatchers.IO) { runCatching { onFetchRepo() }.getOrNull() }
        }
        org.sharpemu.android.ui.OverlayBlurEffect()
        AlertDialog(
            onDismissRequest = { showDownload = false },
            title = { Text(uiText.text("Baixar patch")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text(uiText.text("Filtrar por nome")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    val list = repoList
                    when {
                        list == null -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) { androidx.compose.material3.CircularProgressIndicator() }

                        else -> {
                            val norm = query.filter { it.isLetterOrDigit() }.lowercase()
                            val filtered = if (norm.isBlank()) list else list.filter {
                                it.name.filter { c -> c.isLetterOrDigit() }.lowercase().contains(norm)
                            }
                            if (filtered.isEmpty()) {
                                Text(
                                    uiText.text("Nenhum patch encontrado no repositório."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                                    items(filtered, key = { it.downloadUrl }) { rp ->
                                        ListItem(
                                            headlineContent = { Text(rp.name.removeSuffix(".xml")) },
                                            colors = ListItemDefaults.colors(
                                                containerColor = Color.Transparent,
                                            ),
                                            modifier = Modifier.clickable {
                                                onDownloadPatch(rp)
                                                showDownload = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownload = false }) { Text(uiText.close) }
            },
        )
    }
}

// Appearance: theme mode selector (System / Light / Dark) shown via a bottom sheet picker.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeCard(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    shape: Shape,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    fun label(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> uiText.systemTheme
        ThemeMode.LIGHT -> uiText.lightTheme
        ThemeMode.DARK -> uiText.darkTheme
    }
    var sheetOpen by remember { mutableStateOf(false) }
    Card(
        onClick = { sheetOpen = true },
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(uiText.theme, style = MaterialTheme.typography.bodyLarge)
                Text(
                    label(selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Text(
                uiText.theme,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ThemeMode.values().forEach { mode ->
                ListItem(
                    headlineContent = { Text(label(mode)) },
                    trailingContent = {
                        RadioButton(
                            selected = mode == selected,
                            onClick = {
                                onSelected(mode)
                                sheetOpen = false
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onSelected(mode)
                        sheetOpen = false
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// Appearance: Material You (dynamic color) toggle.
@Composable
private fun MaterialYouCard(enabled: Boolean, onToggle: (Boolean) -> Unit, shape: Shape) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(uiText.materialYou, style = MaterialTheme.typography.bodyLarge)
                Text(
                    uiText.materialYouDescription,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            AppSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

// Generic navigation card (title + subtitle + chevron) used to open sub-screens from settings.
@Composable
private fun NavCard(title: String, subtitle: String, shape: Shape, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

// Custom Vulkan drivers (Turnip) only apply to Adreno (Qualcomm) GPUs. We show the option when the
// device is positively Qualcomm (HARDWARE is "qcom" on every Qualcomm Android device, plus SOC fields
// on API 31+), hide it only on a positively-identified non-Adreno SoC, and otherwise show it — so a
// real Adreno device (e.g. Snapdragon 855 / Adreno 640) is never wrongly hidden.
private fun isAdrenoDevice(): Boolean {
    val info = buildString {
        append(android.os.Build.HARDWARE).append(' ')
        append(android.os.Build.BOARD).append(' ')
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            append(android.os.Build.SOC_MANUFACTURER).append(' ')
            append(android.os.Build.SOC_MODEL)
        }
    }.lowercase()
    if (info.contains("qcom") || info.contains("qualcomm") || info.contains("qti") ||
        info.contains("snapdragon") || info.contains("msm") || info.contains("sdm") ||
        Regex("\\bsm\\d").containsMatchIn(info)) {
        return true
    }
    val nonAdreno = listOf("exynos", "samsung", "mediatek", "mtk", "mt6", "kirin", "hisilicon",
        "tensor", "google", "unisoc", "spreadtrum", "tegra", "nvidia")
    if (nonAdreno.any { info.contains(it) }) return false
    return true // unknown SoC: show rather than risk hiding on an Adreno device.
}

// Entry card in the settings list that navigates to the dedicated GPU drivers screen.
@Composable
private fun GpuDriverNavCard(
    activeDriver: String,
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(15.dp),
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    // A non-blank activeDriver means the user picked a custom driver (as opposed to the default
    // "Sistema" one) -- shown as an explicit checkmark badge, not just the subtitle text, so it
    // reads at a glance that a selection was made.
    val isCustomDriverSelected = activeDriver.isNotBlank()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(uiText.settingTitle("vulkan_driver"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    activeDriver.ifBlank { uiText.text("Sistema") },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCustomDriverSelected) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = uiText.text("Driver selecionado"),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// Grouped rounded-corner shape for a card at `index` within a `size`-long group.
private fun ccGroupedShape(index: Int, size: Int): RoundedCornerShape {
    if (size <= 1) return RoundedCornerShape(15.dp)
    val top = if (index == 0) 15.dp else 5.dp
    val bottom = if (index == size - 1) 15.dp else 5.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

// Section title above a group of driver cards.
@Composable
private fun DriverSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp),
    )
}

// A driver row: title + optional subtitle (vendor/version for installed) + selection radio.
@Composable
private fun DriverCard(
    driver: GpuDriverEntry,
    shape: Shape,
    onSelectDriver: (GpuDriverEntry) -> Unit,
) {
    Card(
        onClick = { onSelectDriver(driver) },
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardBg()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(driver.title, style = MaterialTheme.typography.bodyLarge)
                if (driver.subtitle.isNotBlank()) {
                    Text(
                        driver.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RadioButton(selected = driver.active, onClick = { onSelectDriver(driver) })
        }
    }
}

// Dedicated screen listing every installed GPU driver. The user can pick which one is active,
// and a bottom floating action button installs a new driver from a file.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuDriversScreen(
    drivers: List<GpuDriverEntry>,
    onBack: () -> Unit,
    onSelectDriver: (GpuDriverEntry) -> Unit,
    onInstallDriver: (Uri) -> Unit,
    onFetchRemote: () -> List<RemoteGpuDriver>,
    onDownloadDriver: (RemoteGpuDriver) -> Unit,
) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var fabOpen by remember { mutableStateOf(false) }
    var showDownload by remember { mutableStateOf(false) }
    val driverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            onInstallDriver(it)
        }
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.blur(if (fabOpen) 10.dp else 0.dp),
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBg(),
                ),
                title = { Text(uiText.text("Drivers de GPU")) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
        floatingActionButton = {
            /*
            // Expandable FAB (same style as the home screen): Baixar (from the AdrenoToolsDrivers
            // repo) and Instalar (from a local .zip) — mirrors PCSX2's custom-driver download/install.
            ExpressiveFabMenu(
                expanded = fabOpen,
                onExpandedChange = { fabOpen = it },
                items = listOf(
                    ExpressiveFabItem("Baixar", Icons.Default.Download) { showDownload = true },
                    ExpressiveFabItem("Instalar", Icons.Default.InstallMobile) {
                        driverPicker.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                                "all-files",
                            ),
                        )
                    },
                ),
            )
            */
        },
    ) { padding ->
        // Use the native list directly (it already reports the correct "active" flag for the system
        // entry — previously the UI synthesized its own system entry, which broke the selection state).
        val systemDrivers = drivers.filter { it.id == "system" }
        val installedDrivers = drivers.filter { it.id != "system" && it.id.isNotBlank() }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "h-system") { DriverSectionHeader(uiText.text("Driver de sistema")) }
            itemsIndexed(systemDrivers, key = { _, d -> "sys-${d.id}" }) { idx, driver ->
                DriverCard(driver, ccGroupedShape(idx, systemDrivers.size), onSelectDriver)
            }
            item(key = "h-installed") { DriverSectionHeader(uiText.text("Drivers instalados")) }
            if (installedDrivers.isEmpty()) {
                item(key = "no-installed") {
                    Text(
                        uiText.text("Nenhum driver instalado. Use o botão flutuante para baixar ou instalar."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(installedDrivers, key = { _, d -> "inst-${d.id}" }) { idx, driver ->
                    DriverCard(driver, ccGroupedShape(idx, installedDrivers.size), onSelectDriver)
                }
            }
        }
    }
        FabBackdrop(expanded = fabOpen, onDismiss = { fabOpen = false })
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            ExpressiveFabMenu(
                expanded = fabOpen,
                onExpandedChange = { fabOpen = it },
                items = listOf(
                    ExpressiveFabItem(uiText.text("Baixar"), Icons.Default.Download) { showDownload = true },
                    ExpressiveFabItem(uiText.text("Instalar"), Icons.Default.InstallMobile) {
                        driverPicker.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                                "*/*",
                            ),
                        )
                    },
                ),
            )
        }
    }

    if (showDownload) {
        var remote by remember { mutableStateOf<List<RemoteGpuDriver>?>(null) }
        var query by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            remote = withContext(Dispatchers.IO) { runCatching { onFetchRemote() }.getOrNull() }
        }
        // Drivers already installed (matched by a loose "does its name appear in the remote
        // listing's name" check, since the remote name includes the release tag + asset filename
        // and there's no exact id to compare against) are hidden from this list -- no point
        // offering to download something the user already has.
        val installedTitles = remember(drivers) {
            drivers.filter { it.id != "system" && it.id.isNotBlank() }.map { it.title.lowercase() }
        }
        val filtered = remember(remote, query, installedTitles) {
            remote.orEmpty()
                .filterNot { rd -> installedTitles.any { rd.name.lowercase().contains(it) } }
                .filter { rd -> query.isBlank() || rd.name.contains(query, ignoreCase = true) }
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { showDownload = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    uiText.text("Baixar driver de GPU"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(uiText.text("Filtrar por nome")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                when {
                    remote == null -> Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) { androidx.compose.material3.CircularProgressIndicator() }

                    filtered.isEmpty() -> Text(
                        uiText.text("Nenhum driver encontrado (verifique a conexão)."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )

                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        itemsIndexed(filtered, key = { _, rd -> rd.downloadUrl }) { index, rd ->
                            val top = if (index == 0) 15.dp else 5.dp
                            val bottom = if (index == filtered.lastIndex) 15.dp else 5.dp
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDownloadDriver(rd)
                                        showDownload = false
                                    },
                                shape = RoundedCornerShape(
                                    topStart = top, topEnd = top,
                                    bottomStart = bottom, bottomEnd = bottom,
                                ),
                                colors = CardDefaults.cardColors(containerColor = cardBg()),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        rd.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extracts "owner/repo" from a GitHub releases channel URL for a compact card title, falling back
// to the raw URL if it doesn't parse as one.
private fun driverChannelTitle(url: String): String =
    Regex("""github\.com/([^/]+)/([^/]+)""").find(url)
        ?.let { "${it.groupValues[1]}/${it.groupValues[2].removeSuffix("/")}" }
        ?: url

// Lists the configured GPU driver download channels (see DriverChannelPrefs) as cards, with a
// floating "add channel" button (bottom-left, per the request) that opens a blurred sheet with a
// GitHub URL field pre-filled with "https://github.com/". The driver download sheet in
// GpuDriversScreen pulls from every channel listed here.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverChannelsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var tick by remember { mutableStateOf(0) }
    val channels = remember(tick) { org.sharpemu.android.data.DriverChannelPrefs.getChannels(context) }
    var showAdd by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = appBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appBg()),
                title = { Text(uiText.text("Canais de downloads")) },
                navigationIcon = { CircleBackButton(onClick = onBack) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = uiText.text("Adicionar"))
            }
        },
    ) { padding ->
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    uiText.text("Nenhum canal configurado."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(channels, key = { _, url -> url }) { index, url ->
                    val top = if (index == 0) 15.dp else 5.dp
                    val bottom = if (index == channels.lastIndex) 15.dp else 5.dp
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom,
                        ),
                        colors = CardDefaults.cardColors(containerColor = cardBg()),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    driverChannelTitle(url),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { removeTarget = url }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = uiText.text("Remover"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        var url by remember { mutableStateOf("https://github.com/") }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(url, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { showAdd = false }) { Text(uiText.text("Cancelar")) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        org.sharpemu.android.data.DriverChannelPrefs.addChannel(context, url)
                        tick++
                        showAdd = false
                    }) { Text(uiText.text("Adicionar")) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    removeTarget?.let { url ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text(driverChannelTitle(url)) },
            text = { Text(uiText.text("Remover este canal de downloads?")) },
            confirmButton = {
                TextButton(onClick = {
                    org.sharpemu.android.data.DriverChannelPrefs.removeChannel(context, url)
                    tick++
                    removeTarget = null
                }) { Text(uiText.text("Remover")) }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text(uiText.text("Cancelar")) }
            },
        )
    }
}

// String settings whose value is one of a fixed set of choices. These are presented in a bottom
// sheet with selectable list items instead of a free text field. The stored value IS the chosen
// string (mirrors the desktop combo boxes in settings_dialog_imgui).
private val selectableSettingOptions: Map<String, List<String>> = mapOf(
    // Android UI exposes these backends (native side also restricts/normalizes to just these,
    // see android_app_host.cpp's NormalizeValue) -- "x64-aarch64-asmjit" is shown as "aarch64",
    // "x64-arm64-backend" as "LLVM", and "x64-aarch64-dynarec" as "Dynarec" in the UI (see
    // AppUiText.optionLabel).
    "cpu_backend" to listOf(
        "auto",
        "x64-aarch64-asmjit",
        "x64-arm64-backend",
        "x64-interpreter",
        "x64-aarch64-dynarec",
    ),
    "fps_overlay_position" to listOf(
        "top_left", "top_center", "top_right", "bottom_left", "bottom_center", "bottom_right",
    ),
    "ram_overlay_position" to listOf(
        "top_left", "top_center", "top_right", "bottom_left", "bottom_center", "bottom_right",
    ),
    "gpu_overlay_position" to listOf(
        "top_left", "top_center", "top_right", "bottom_left", "bottom_center", "bottom_right",
    ),
    "cpu_usage_position" to listOf(
        "top_left", "top_center", "top_right", "bottom_left", "bottom_center", "bottom_right",
    ),
    "gpu_usage_position" to listOf(
        "top_left", "top_center", "top_right", "bottom_left", "bottom_center", "bottom_right",
    ),
    "full_screen_mode" to listOf("Windowed", "Fullscreen", "Fullscreen (Borderless)"),
    "present_mode" to listOf("Mailbox", "Fifo", "Immediate"),
    "frame_generation_backend" to listOf("LSFG", "FSR3"),
    "trophy_notification_side" to listOf("left", "right", "top", "bottom"),
    "upscale_quality_mode" to listOf(
        "Quality", "Balanced", "Performance", "UltraPerformance", "Custom",
    ),
)

// Integer settings that are really labeled enums: the stored value is an int, shown to the user as a
// label. Each entry is (label -> stored int), ordered for display. Values match the desktop combos
// (settings_dialog_imgui.h) and the native enums (GpuReadbacksMode, console language map). Rendered
// as a bottom-sheet picker instead of a meaningless numeric slider.
private val intEnumSettingOptions: Map<String, List<Pair<String, Int>>> = mapOf(
    "audio_backend" to listOf("SDL" to 0, "OpenAL" to 1),
    // Labels show the actual resolution at each percentage (relative to 1920x1080, the common
    // target output resolution) so the effect of each step is concrete instead of an abstract
    // number -- this setting scales the game's real render resolution, not just a display filter.
    "android_performance_resolution_scale" to listOf(
        "35% (672x378)" to 35,
        "50% (960x540)" to 50,
        "67% (1286x724)" to 67,
        "75% (1440x810)" to 75,
        "85% (1632x918)" to 85,
        "100% (1920x1080)" to 100,
        "115% (2208x1242)" to 115,
        "125% (2400x1350)" to 125,
        "150% (2880x1620)" to 150,
    ),
    "readbacks_mode" to listOf("Disabled" to 0, "Relaxed" to 1, "Precise" to 2),
    "cursor_state" to listOf("Never" to 0, "Idle" to 1, "Always" to 2),
    "console_language" to listOf(
        "Japanese" to 0,
        "English (United States)" to 1,
        "English (United Kingdom)" to 18,
        "French (France)" to 2,
        "French (Canada)" to 22,
        "Spanish (Spain)" to 3,
        "Spanish (Latin America)" to 20,
        "German" to 4,
        "Italian" to 5,
        "Dutch" to 6,
        "Portuguese (Portugal)" to 7,
        "Portuguese (Brazil)" to 17,
        "Russian" to 8,
        "Korean" to 9,
        "Traditional Chinese" to 10,
        "Simplified Chinese" to 11,
        "Finnish" to 12,
        "Swedish" to 13,
        "Danish" to 14,
        "Norwegian (Bokmaal)" to 15,
        "Polish" to 16,
        "Turkish" to 19,
        "Arabic" to 21,
        "Czech" to 23,
        "Hungarian" to 24,
        "Greek" to 25,
        "Romanian" to 26,
        "Thai" to 27,
        "Vietnamese" to 28,
        "Indonesian" to 29,
        "Ukrainian" to 30,
    ),
)

// True when the active Material 3 scheme is dark (surface is a dark tone).
@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

// App background and card colors. Light: background = surfaceVariant (slightly tinted), cards =
// surface (lighter). Dark: SWAPPED — background = surface (deeper) and cards = surfaceVariant
// (lighter than the background), so cards read as elevated Material 3 Expressive surfaces instead of
// looking black-on-black.
@Composable
private fun appBg(): Color =
    if (isDarkScheme()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun cardBg(): Color =
    if (isDarkScheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

@Composable
private fun folderBg(): Color =
    if (isDarkScheme()) Color.Black else Color.White

// Per-setting icon (chosen by key), tinted in a circle with its section's accent color, so every
// option row carries a meaningful badge like the controller-mapping screen.
private fun settingIcon(setting: SettingEntry): androidx.compose.ui.graphics.vector.ImageVector {
    val k = setting.key
    return when {
        k.contains("volume") -> Icons.Default.VolumeUp
        k.contains("fps") || k.contains("vblank") || k.contains("frame_generation") -> Icons.Default.Speed
        k.contains("video_codec") || k.contains("av_player") -> Icons.Default.Movie
        k.contains("language") -> Icons.Default.Language
        k.contains("trophy") -> Icons.Default.EmojiEvents
        k.contains("cpu") -> Icons.Default.Memory
        k.contains("backend") -> Icons.Default.Tune
        k.contains("screen") || k.contains("window") || k.contains("resolution") -> Icons.Default.AspectRatio
        k.contains("fsr") || k.contains("rcas") || k.contains("shader") -> Icons.Default.AutoAwesome
        k.contains("full_screen") || k.contains("present") -> Icons.Default.Fullscreen
        k.contains("audio") || k.contains("mic") || k.contains("output") -> Icons.Default.VolumeUp
        k.contains("motion") || k.contains("cursor") || k.contains("pad") -> Icons.Default.SportsEsports
        k.contains("log") || k.contains("filter") -> Icons.Default.Description
        k.contains("vk") || k.contains("vulkan") || k.contains("gpu_id") -> Icons.Default.Layers
        k.contains("memory") || k.contains("dmem") -> Icons.Default.Memory
        k.contains("debug") || k.contains("dump") -> Icons.Default.BugReport
        setting.type == "bool" -> Icons.Default.ToggleOn
        else -> Icons.Default.Tune
    }
}

@Composable
private fun SettingLeadingBadge(setting: SettingEntry) {
    SectionIconBadge(settingIcon(setting), sectionVisual(setting.section).second)
}

// A settings icon inside a small colored circle (matches the controller-mapping screen's badges).
@Composable
private fun SectionIconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

// Per-section icon + unique accent color for the category cards/headers.
private fun sectionVisual(section: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> =
    when (section) {
        "Aparência" -> Icons.Default.Palette to Color(0xFFD81B60)
        "CPU" -> Icons.Default.DeveloperBoard to Color(0xFF00796B)
        "General" -> Icons.Default.Tune to Color(0xFF3949AB)
        "GPU" -> Icons.Default.Memory to Color(0xFF00897B)
        "Vulkan" -> Icons.Default.Layers to Color(0xFFE53935)
        "Audio" -> Icons.Default.VolumeUp to Color(0xFFFB8C00)
        "Input" -> Icons.Default.SportsEsports to Color(0xFF6D4C41)
        "Log" -> Icons.Default.Description to Color(0xFF546E7A)
        "Debug" -> Icons.Default.BugReport to Color(0xFF8E24AA)
        "Android" -> Icons.Default.PhoneAndroid to Color(0xFF43A047)
        "gpu-driver-download-channel" -> Icons.Default.Download to Color(0xFF00897B)
        else -> Icons.Default.Settings to Color(0xFF5E35B1)
    }

// Localized display name for a native settings section.
private fun categoryDisplayName(section: String): String = when (section) {
    "CPU" -> "CPU"
    "General" -> "Geral"
    "GPU" -> "Gráficos (GPU)"
    "Vulkan" -> "Vulkan"
    "Audio" -> "Áudio"
    "Input" -> "Controles"
    "Log" -> "Registro (Log)"
    "Debug" -> "Depuração"
    "Android" -> "Android"
    else -> section
}

private fun uiSettingsSection(setting: SettingEntry): String =
    if (setting.section == "General" && setting.key.startsWith("cpu_")) "CPU" else setting.section

private fun isHiddenGlobalSettingInUi(setting: SettingEntry): Boolean =
    (setting.section == "General" && setting.key in HiddenGeneralSettingKeys) ||
        setting.key in HiddenSettingKeys

private val HiddenGeneralSettingKeys = setOf(
    "shad_net_enabled",
    "shadnet_server",
    "discord_rpc_enabled",
    "big_picture_scale",
    // CPU section is restricted to just backend choice (cpu_backend) + self-check
    // (cpu_jit_self_check) -- every other CPU diagnostic/tuning toggle stays available natively
    // (desktop settings dialog, config file) but isn't surfaced in the Android UI.
    "cpu_interpreter_trace",
    "cpu_interpreter_max_instructions",
    "cpu_jit_detailed_profile",
    "cpu_jit_deep_debug",
    "cpu_jit_native_diagnostics",
    "cpu_jit_arm64_disallow_fallback",
    "cpu_x64arm64_golden_debug",
    "cpu_x64arm64_boot_turbo",
)

private val HiddenSettingKeys = setOf(
    "window_width",
    "window_height",
    "internal_screen_width",
    "internal_screen_height",
    "adrenotools_bcn_patch",
    "adrenotools_gpu_mapping_import",
    "android_render_diagnostics",
    "render_wsi_debug",
    "pkg_extractor",
    "maximum_performance_mode",
    "force_max_gpu_clocks",
    "android_native_renderer_enabled",
    // Android only ever exposes a single GPU (index 0) -- there is nothing to pick between.
    "gpu_id",
    // FSR3 frame generation and the Quality/Balanced/Performance/UltraPerformance upscale presets
    // don't work reliably yet (frame gen: black frames / worse FPS than LSFG; upscale presets:
    // confusing UX on top of the same underlying issues) -- hidden until fixed. LSFG frame
    // generation is unaffected: frame_generation_backend's stored default is already "LSFG" and
    // upscale_quality_mode's default ("Custom") already makes the manual percentage below apply.
    "frame_generation_backend",
    "upscale_quality_mode",
    // Hardware (MediaCodec) video decode for AvPlayer cutscenes: off by default, hidden from the
    // UI for now (see av_player_android_video_codec_enabled's default in emulator_settings.h).
    "av_player_android_video_codec_enabled",
    // Render-diagnostics capture tool (on-screen button + this toggle): removed from the UI.
    "render_diag_capture_enabled",
    // Audio input/output device pickers: only the audio backend selector stays user-facing.
    "sdl_mic_device",
    "sdl_main_output_device",
    "sdl_padSpk_output_device",
    "openal_mic_device",
    "openal_main_output_device",
    "openal_padSpk_output_device",
    "frame_generation_hdr_mode",
    "bc_software_fallback_enabled",
    "async_shader_compilation_enabled",
)

private val GeneralSettingsGroupOrder = listOf(
    "Sistema",
    "Memoria",
    "Audio",
    "Trofeus",
    "Overlay",
    "Outros",
)

private fun generalSettingGroup(setting: SettingEntry): String = when (setting.key) {
    "show_splash",
    "console_language",
    "connected_to_network",
    "neo_mode",
    "dev_kit_mode",
    "is_ps4_pro",
    "is_devkit",
    -> "Sistema"

    "extra_dmem_in_mbytes",
    "memory_reclaim_enabled",
    "memory_exclusive_swap_enabled",
    -> "Memoria"

    "volume_slider",
    "audio_backend",
    -> "Audio"

    "trophy_key",
    "trophy_popup_disabled",
    "trophy_notification_duration",
    "trophy_notification_side",
    -> "Trofeus"

    "show_fps_counter",
    "show_ram_overlay",
    "show_gpu_overlay",
    "show_cpu_usage",
    "show_gpu_usage",
    "fps_overlay_position",
    "ram_overlay_position",
    "gpu_overlay_position",
    "cpu_usage_position",
    "gpu_usage_position",
    -> "Overlay"

    else -> "Outros"
}

private val LogSettingsGroupOrder = listOf(
    "Arquivo",
    "Filtro e nivel",
    "Reducao de ruido",
    "Outros",
)

private fun logSettingGroup(setting: SettingEntry): String = when (setting.key) {
    "enable",
    "append",
    "sync",
    "separate",
    "size_limit",
    -> "Arquivo"

    "filter",
    "log_filter",
    -> "Filtro e nivel"

    "skip_duplicate",
    "max_skip_duration",
    -> "Reducao de ruido"

    else -> "Outros"
}

private val AudioSettingsGroupOrder = listOf(
    "Backend",
    "Saida principal",
    "Microfone",
    "Alto-falante do controle",
    "Outros",
)

private fun audioSettingGroup(setting: SettingEntry): String = when (setting.key) {
    "audio_backend" -> "Backend"

    "sdl_main_output_device",
    "openal_main_output_device",
    -> "Saida principal"

    "sdl_mic_device",
    "openal_mic_device",
    -> "Microfone"

    "sdl_padSpk_output_device",
    "openal_padSpk_output_device",
    -> "Alto-falante do controle"

    else -> "Outros"
}

private val GpuSettingsGroupOrder = listOf(
    "Tela e apresentacao",
    "Escala e nitidez",
    "Frame generation",
    "Compatibilidade de GPU",
    "Diagnostico",
    "Outros",
)

private fun gpuSettingGroup(setting: SettingEntry): String = when (setting.key) {
    "full_screen",
    "full_screen_mode",
    "present_mode",
    "vblank_frequency",
    "hdr_allowed",
    -> "Tela e apresentacao"

    "fsr_enabled",
    "rcas_enabled",
    "rcas_attenuation",
    "upscale_quality_mode",
    "android_performance_resolution_scale",
    -> "Escala e nitidez"

    "frame_generation_enabled",
    "frame_generation_backend",
    "frame_generation_multiplier",
    "frame_generation_flow_scale",
    "frame_generation_performance_mode",
    "frame_generation_hdr_mode",
    -> "Frame generation"

    "null_gpu",
    "copy_gpu_buffers",
    "readbacks_mode",
    "readback_linear_images_enabled",
    "direct_memory_access_enabled",
    -> "Compatibilidade de GPU"

    "dump_shaders",
    "patch_shaders",
    "bc_software_fallback_enabled",
    -> "Diagnostico"

    else -> "Outros"
}

private val VulkanSettingsGroupOrder = listOf(
    "Dispositivo Vulkan",
    "Validacao Vulkan",
    "Cache de pipelines",
    "Marcadores de debug",
    "Diagnostico",
    "Outros",
)

private fun vulkanSettingGroup(setting: SettingEntry): String = when (setting.key) {
    "gpu_id",
    -> "Dispositivo Vulkan"

    "vkvalidation_enabled",
    "vkvalidation_core_enabled",
    "vkvalidation_sync_enabled",
    "vkvalidation_gpu_enabled",
    -> "Validacao Vulkan"

    "pipeline_cache_enabled",
    "pipeline_cache_archived",
    -> "Cache de pipelines"

    "vkhost_markers",
    "vkguest_markers",
    "pm4_dump_enabled",
    -> "Marcadores de debug"

    "renderdoc_enabled",
    "vkcrash_diagnostic_enabled",
    -> "Diagnostico"

    else -> "Outros"
}

private val AndroidSettingsGroupOrder = listOf(
    "Armazenamento",
    "Integracao Android",
    "Driver e recursos",
    "Diagnostico",
    "Outros",
)

private fun androidSettingGroup(setting: SettingEntry): String = when (setting.key) {
    "data_root",
    -> "Armazenamento"

    "picture_in_picture_enabled",
    -> "Integracao Android"

    "vulkan_driver",
    "adrenotools_turbo",
    -> "Driver e recursos"

    "android_debug_overlay",
    "android_diagnostics",
    "texture_format_diagnostics",
    "render_diag_capture_enabled",
    -> "Diagnostico"

    else -> "Outros"
}

// A meaningful, multi-aspect description of what each settings category contains.
private fun categoryDescription(section: String): String = when (section) {
    "CPU" -> "Backend, JIT ARM64 e diagnostico detalhado de execucao"
    "General" -> "Backend de CPU, idioma, volume, troféus e modo Neo/PS4 Pro"
    "GPU" -> "Resolução interna, FSR/nitidez, vblank, readbacks e driver Vulkan"
    "Vulkan" -> "GPU usada, validação, cache de pipeline e marcadores de debug"
    "Audio" -> "Backend de áudio e dispositivos de saída/microfone"
    "Input" -> "Controles na tela, mapeamento e controles físicos"
    "Log" -> "Nível, filtro, tamanho e gravação do log do emulador"
    "Debug" -> "Dump de shaders e opções de diagnóstico"
    "Android" -> "Caminho de dados, driver de GPU, desempenho e picture-in-picture"
    else -> "Opções de $section"
}

// Back button for the app bars: the arrow sits inside a circle tinted with the card color, so it
// matches the cards on the screen.
@Composable
private fun CircleBackButton(onClick: () -> Unit) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(cardBg()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = uiText.text("Voltar"),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

private fun prettyKey(key: String): String = when (key) {
    "trophy_key" -> "Chave de trofeus"
    "picture_in_picture_enabled" -> "Picture-in-picture"
    "cpu_x64arm64_golden_debug" -> "Debug golden X64-ARM64"
    "cpu_x64arm64_boot_turbo" -> "Boot turbo X64-ARM64"
    else -> key.split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
}

// Display title for a setting: prefixed with "*" when its value differs from the built-in default.
private fun settingTitle(setting: SettingEntry, uiText: AppUiText): String =
    if (setting.isModified) "* ${uiText.settingTitle(setting.key)}" else uiText.settingTitle(setting.key)

// pt-BR description of what each setting does (shown in the per-setting info dialog). Falls back to a
// generic message for keys without a dedicated explanation.
private fun settingDescription(key: String): String = when (key) {
    "cpu_backend" -> "Escolhe o motor de CPU: interpretador x64 (mais compatível) ou JIT AArch64 (mais rápido quando funciona)."
    "cpu_interpreter_max_instructions" -> "Limite de instruções do interpretador por bloco. 0 = sem limite. Útil para depurar travamentos."
    "cpu_interpreter_trace" -> "Registra cada instrução executada pelo interpretador. Deixa o jogo muito lento; só para depuração."
    "cpu_jit_self_check" -> "Diagnostico legado do JIT. No backend ARM64 normal ele fica desativado para nao trocar para o interpretador. Deixe desligado para velocidade total."
    "cpu_jit_detailed_profile" -> "Coleta cobertura, blocos quentes, fallbacks, cache e memória do JIT ARM64 sem reexecutar no interpretador. Reduz um pouco o desempenho; use durante análises."
    "cpu_jit_deep_debug" -> "Ativa diagnóstico profundo do JIT ARM64 sem validar blocos pelo interpretador. Registra estado, blocos e falhas nativas para analisar travamentos."
    "cpu_jit_native_diagnostics" -> "Forca o diagnostico a permanecer no backend JIT. Mesmo com self-check ligado, nao troca para interpretador; registra blocos, estado e falhas nativas."
    "cpu_jit_arm64_disallow_fallback" -> "Somente backend x64-aarch64-handjit: desativa o fallback de verdade. Se um bloco nao tiver NENHUMA instrucao coberta pelo JIT, o jogo para em vez de rodar no interpretador — use para forcar a superficie de bloqueios (veja os logs [ARM64-BACKEND][MISSING-OP])."
    "cpu_x64arm64_golden_debug" -> "Ativa o corpus golden do backend X64-ARM64. Use apenas para diagnostico: ele roda casos de validacao antes do jogo e pode atrasar o primeiro frame."
    "cpu_x64arm64_boot_turbo" -> "Modo experimental do backend X64-ARM64: aumenta a compilacao especulativa de blocos no boot. Desligado volta ao agendamento conservador."
    "memory_reclaim_enabled" -> "Libera para o Android páginas que o guest já desmapeou ou deixou sem acesso. Conservador: não descarta memória ainda mapeada pelo jogo."
    "memory_exclusive_swap_enabled" -> "Usa um backing privado do app para a memória direta do emulador, permitindo ao kernel descarregar páginas frias sem compartilhar esse arquivo com outros apps."
    "neo_mode" -> "Emula o modo PS4 Pro (Neo). Pode habilitar resoluções/efeitos extras em alguns jogos."
    "dev_kit_mode" -> "Emula um kit de desenvolvimento PS4. Necessário apenas para conteúdo de debug."
    "extra_dmem_in_mbytes" -> "Memória direta adicional (MB) concedida ao jogo. Aumente apenas se um jogo exigir."
    "volume_slider" -> "Volume geral do áudio emulado (0–100%)."
    "show_fps_counter" -> "Mostra o contador de FPS sobreposto durante a emulação."
    "show_ram_overlay" -> "Mostra a memória RAM usada pelo processo do emulador."
    "show_gpu_overlay" -> "Mostra a memória Vulkan usada pelo emulador."
    "show_cpu_usage" -> "Mostra o uso total da CPU do aparelho em porcentagem (média de todos os núcleos)."
    "show_gpu_usage" -> "Mostra o uso total da GPU do aparelho em porcentagem (lido do driver Adreno/kgsl)."
    "fps_overlay_position" -> "Posição do contador de FPS."
    "ram_overlay_position" -> "Posição do indicador de RAM."
    "gpu_overlay_position" -> "Posição do indicador de memória da GPU."
    "cpu_usage_position" -> "Posição do indicador de uso da CPU."
    "gpu_usage_position" -> "Posição do indicador de uso da GPU."
    "maximum_performance_mode" -> "Solicita prioridade máxima permitida pelo Android, mantém a tela e CPU ativas e habilita as APIs de desempenho disponíveis. Não altera clocks sem root."
    "adrenotools_turbo" -> "Força o modo turbo do AdrenoTools para clocks máximos da GPU enquanto o Vulkan está ativo. Pode aquecer e gastar mais bateria."
    "adrenotools_bcn_patch" -> "Aplica o patch BCn do AdrenoTools quando o driver Adreno precisar dele para texturas comprimidas BCn."
    "adrenotools_gpu_mapping_import" -> "Inicializa o hook experimental de importação de mapeamento GPU do AdrenoTools para drivers custom. Use apenas para diagnóstico."
    "texture_format_diagnostics" -> "Registra formatos de textura e image views usados pelo Vulkan no Android, incluindo BCn, swizzle, tiling, mips e fallbacks de formato. Use para investigar texturas quebradas, cores magenta ou brilho incorreto."
    "force_texture_bcn_fallback" -> "Força a conversão de texturas BC1-BC7 no Android antes do upload, útil para diagnosticar drivers que quebram BCn nativo."
    "picture_in_picture_enabled" -> "Ao sair do jogo para a launcher, mantém a atividade do jogo em picture-in-picture em vez de fechar a tela."
    "console_language" -> "Idioma do console PS4 reportado ao jogo (afeta legendas/voz quando suportado)."
    "trophy_key" -> "Chave hexadecimal de 16 bytes usada para descriptografar dados de troféus TRP."
    "trophy_popup_disabled" -> "Desativa as notificações de troféu na tela."
    "trophy_notification_duration" -> "Por quantos segundos a notificação de troféu fica visível."
    "trophy_notification_side" -> "Em qual borda da tela a notificação de troféu aparece."
    "audio_backend" -> "Biblioteca de áudio usada para reproduzir o som (SDL ou OpenAL)."
    "null_gpu" -> "Desativa a renderização gráfica (tela preta). Usado para diagnosticar problemas de CPU."
    "vblank_frequency" -> "Frequência de atualização vertical emulada (Hz). Afeta a velocidade de jogos travados a vsync."
    "full_screen" -> "Inicia a emulação em tela cheia."
    "full_screen_mode" -> "Modo de exibição: em janela, tela cheia ou tela cheia sem bordas."
    "present_mode" -> "Modo de apresentação Vulkan: Mailbox (sem tearing), Fifo (vsync) ou Immediate (sem espera)."
    "fsr_enabled" -> "Ativa o upscaling AMD FSR para melhorar a nitidez ao escalar a imagem."
    "rcas_enabled" -> "Aplica nitidez RCAS (parte do FSR) à imagem final."
    "rcas_attenuation" -> "Intensidade da nitidez RCAS. Valores menores = mais nítido."
    "frame_generation_enabled" -> "Ativa o gerador de quadros nativo leve. No Adreno 640 use primeiro 2x; pode aumentar latência e uso de GPU."
    "frame_generation_backend" -> "Backend usado para gerar quadros: LSFG (Lossless Scaling) ou FSR3 (fluxo óptico AMD, até 2x)."
    "frame_generation_multiplier" -> "Multiplicador do gerador de quadros LSFG. 1 desativa; 2x/3x/4x geram quadros intermediários reais via IA (requer um Lossless.dll importado)."
    "frame_generation_flow_scale" -> "Resolução interna usada para estimar o fluxo óptico entre os dois frames reais (LSFG). Valores menores = mais rápido, porém menos preciso nas bordas em movimento."
    "frame_generation_performance_mode" -> "Usa o modelo LSFG mais leve (Performance Mode), com menor custo de GPU e leve perda de qualidade nos quadros gerados."
    "frame_generation_hdr_mode" -> "Informa ao gerador de quadros LSFG que os frames de entrada estão em HDR, ajustando a decodificação de cor interna."
    "av_player_android_video_codec_enabled" -> "Decodifica vídeos do jogo (cutscenes/FMV) usando o decodificador de vídeo por hardware do aparelho em vez do FFmpeg por software, liberando CPU para o jogo continuar rodando sem perder FPS durante o vídeo. Se desativado, usa sempre o FFmpeg (mais compatível, porém mais pesado). Tem efeito a partir do próximo vídeo iniciado; decodificadores de hardware variam por aparelho, então há fallback automático para FFmpeg se o codec do vídeo não for suportado."
    "async_shader_compile_enabled" -> "Compila pipelines Vulkan fora da thread de renderização. Reduz travadas por shader novo; pode causar pop-in temporário até a pipeline ficar pronta."
    "force_max_gpu_clocks" -> "Forca clocks maximos da GPU Adreno via AdrenoTools/KGSL enquanto o Vulkan esta ativo. Pode aquecer e gastar mais bateria; deixe desligado se houver instabilidade."
    "upscale_quality_mode" -> "Preset de escala interna (estilo FSR2/3): Quality/Balanced/Performance/UltraPerformance fixam a escala; Custom usa o valor manual abaixo."
    "android_performance_resolution_scale" -> "Escala a resolução real de renderização do jogo (não é só um filtro visual). A imagem continua ocupando a tela inteira. Valores abaixo de 100% reduzem nitidez e custo de GPU (melhor desempenho); acima de 100% aumentam nitidez e custo de GPU."
    "android_render_diagnostics" -> "Registra tempos de acquire/present, dimensões de frame/swapchain e estado de escala para diagnosticar renderização no Android."
    "readbacks_mode" -> "Modo de leitura de memória da GPU. Necessário para alguns jogos; pode reduzir o desempenho."
    "readback_linear_images_enabled" -> "Habilita readback de imagens lineares. Corrige alguns gráficos ao custo de desempenho."
    "direct_memory_access_enabled" -> "Permite acesso direto à memória da GPU. Pode corrigir ou quebrar gráficos dependendo do jogo."
    "hdr_allowed" -> "Permite saída HDR quando a tela suportar."
    "window_width", "window_height" -> "Tamanho inicial da janela de emulação, em pixels."
    "internal_screen_width", "internal_screen_height" -> "Resolução interna de renderização. Maior = mais nítido e mais pesado."
    "vkvalidation_enabled" -> "Ativa as camadas de validação do Vulkan (diagnóstico para desenvolvedores)."
    "motion_controls_enabled" -> "Habilita os controles de movimento (giroscópio) quando o dispositivo/controle suportar."
    "is_circle_enter" -> "Usa o botão Círculo como 'confirmar' (padrão japonês) em vez de X."
    "log_filter", "filter" -> "Filtro de log por classe/nível (ex.: '*:Info'). Vazio = padrão."
    "vulkan_driver" -> "Driver Vulkan usado no Android (driver do sistema ou um Turnip importado)."
    // General
    "shad_net_enabled" -> "Ativa o suporte experimental de rede do shadPS4 (shadNet)."
    "show_splash" -> "Mostra a imagem de abertura (splash) do jogo antes de iniciar."
    "connected_to_network" -> "Reporta ao jogo que o console está conectado à internet."
    "discord_rpc_enabled" -> "Mostra o jogo em execução no seu status do Discord (Rich Presence)."
    "big_picture_scale" -> "Escala da interface no modo Big Picture (1000 = 100%)."
    "shadnet_server" -> "Endereço do servidor shadNet usado quando a rede está ativada."
    // Log
    "append" -> "Acrescenta ao log existente em vez de recriá-lo a cada execução."
    "enable" -> "Liga a gravação do log do emulador em arquivo."
    "max_skip_duration" -> "Tempo máximo (ms) que mensagens repetidas são agrupadas no log."
    "separate" -> "Grava um arquivo de log separado por subsistema."
    "size_limit" -> "Tamanho máximo do arquivo de log em bytes antes de rotacionar."
    "skip_duplicate" -> "Agrupa mensagens de log idênticas consecutivas para reduzir ruído."
    "sync" -> "Grava cada linha do log imediatamente (mais seguro, um pouco mais lento)."
    // Debug
    "debug_dump" -> "Despeja informações de depuração do jogo em disco (para desenvolvedores)."
    "shader_collect" -> "Coleta shaders do jogo para análise/depuração."
    "config_version" -> "Versão interna do arquivo de configuração. Não editável."
    // Input
    "cursor_state" -> "Quando o cursor do mouse fica oculto: Nunca, Ocioso ou Sempre."
    "cursor_hide_timeout" -> "Segundos de inatividade até o cursor sumir (modo Ocioso)."
    "usb_device_backend" -> "Backend de dispositivos USB emulados (avançado)."
    "use_special_pad" -> "Trata o controle como um dispositivo especial (ex.: guitarra/volante)."
    "special_pad_class" -> "Classe do controle especial quando 'Usar controle especial' está ligado."
    "use_unified_input_config" -> "Usa uma única configuração de input para todos os jogos."
    "default_controller_id" -> "Identificador do controle físico usado por padrão."
    "background_controller_input" -> "Continua recebendo input do controle mesmo em segundo plano."
    "ime_accessibility_enabled" -> "Ativa recursos de acessibilidade no teclado virtual (IME)."
    "ime_url_mail_short_panel" -> "Usa um painel reduzido de IME para campos de URL/e-mail."
    "camera_id" -> "Câmera usada para recursos que exigem PlayStation Camera (-1 = nenhuma)."
    // Audio devices
    "sdl_mic_device" -> "Microfone usado pelo backend SDL."
    "sdl_main_output_device" -> "Dispositivo de saída de áudio principal (SDL)."
    "sdl_padSpk_output_device" -> "Saída de áudio do alto-falante do controle (SDL)."
    "openal_mic_device" -> "Microfone usado pelo backend OpenAL."
    "openal_main_output_device" -> "Dispositivo de saída de áudio principal (OpenAL)."
    "openal_padSpk_output_device" -> "Saída de áudio do alto-falante do controle (OpenAL)."
    // GPU
    "copy_gpu_buffers" -> "Copia buffers da GPU. Corrige alguns jogos ao custo de desempenho."
    "dump_shaders" -> "Salva os shaders compilados em disco (depuração)."
    "patch_shaders" -> "Aplica correções automáticas a shaders problemáticos."
    "bc_software_fallback_enabled" -> "Força a decodificação de texturas BC1-5 pela CPU em vez do shader de GPU. Mais lento; use só se suspeitar de bug gráfico específico do driver/GPU."
    "rcas_attenuation" -> "Intensidade da nitidez RCAS. Valores menores = mais nítido."
    // Vulkan
    "gpu_id" -> "Qual GPU usar quando há mais de uma (-1 = automática)."
    "renderdoc_enabled" -> "Integra o RenderDoc para captura de frames (depuração gráfica)."
    "vkvalidation_core_enabled" -> "Camada principal de validação do Vulkan."
    "vkvalidation_sync_enabled" -> "Validação de sincronização do Vulkan (detecta hazards)."
    "vkvalidation_gpu_enabled" -> "Validação assistida pela GPU (mais lenta, mais detalhada)."
    "vkcrash_diagnostic_enabled" -> "Coleta diagnóstico extra em caso de crash do Vulkan."
    "vkhost_markers" -> "Insere marcadores de depuração do lado do host."
    "vkguest_markers" -> "Insere marcadores de depuração vindos do jogo (guest)."
    "pm4_dump_enabled" -> "Grava em disco (pm4_dump.txt) todo comando PM4 do jogo antes da tradução pra Vulkan. Ajuda a isolar bugs gráficos: comando já errado vs. tradução que corrompeu. Gera arquivo grande; desative depois de coletar."
    "pipeline_cache_enabled" -> "Guarda pipelines compilados para reduzir travadas após o primeiro uso."
    "pipeline_cache_archived" -> "Mantém um cache de pipelines arquivado entre execuções."
    "async_shader_compilation_enabled" -> "Compila shaders novos em segundo plano em vez de travar o jogo (mostra aviso na tela enquanto compila, como no rpcs3)."
    // Android
    "data_root" -> "Pasta onde o emulador guarda dados, jogos e configurações. Não editável."
    "pkg_extractor" -> "Indica se o extrator de PKG está disponível nesta build. Não editável."
    else -> "Configuração avançada do emulador (${prettyKey(key)})."
}

// Small info (ⓘ) button shown on each setting card. Opens a dialog with the setting's name and a
// pt-BR description of what it does.
@Composable
private fun SettingInfoButton(setting: SettingEntry, uiText: AppUiText) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Default.Info,
            contentDescription = uiText.information,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp),
        )
    }
    if (open) {
        org.sharpemu.android.ui.OverlayBlurEffect()
        AlertDialog(
            onDismissRequest = { open = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(uiText.settingTitle(setting.key)) },
            text = { Text(uiText.settingDescription(setting.key)) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(uiText.close) } },
        )
    }
}

@Composable
private fun AppSettingInfoButton(title: String, description: String, closeText: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Default.Info,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp),
        )
    }
    if (open) {
        org.sharpemu.android.ui.OverlayBlurEffect()
        AlertDialog(
            onDismissRequest = { open = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(title) },
            text = { Text(description) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(closeText) } },
        )
    }
}

private fun isPercentSetting(setting: SettingEntry): Boolean =
    setting.key.contains("volume") || setting.key.contains("percent") || setting.key.endsWith("slider")

private fun sliderRangeFor(setting: SettingEntry): ClosedFloatingPointRange<Float> = when {
    isPercentSetting(setting) -> 0f..100f
    setting.key == "extra_dmem_in_mbytes" -> 0f..6144f
    setting.key == "cpu_interpreter_max_instructions" -> 0f..2_000_000f
    setting.key == "frame_generation_multiplier" -> 1f..4f
    setting.key == "frame_generation_flow_scale" -> 0.25f..1.0f
    setting.type == "double" -> 0f..1f
    else -> 0f..256f
}

private fun sliderStepsFor(setting: SettingEntry): Int = when (setting.key) {
    "frame_generation_multiplier" -> 2
    else -> 0
}

private fun normalizedSliderValue(setting: SettingEntry, value: Float): Float =
    if (setting.key == "frame_generation_multiplier") value.roundToInt().toFloat() else value

// A single configuration entry, rendered as its own rounded card. The control depends on the
// value kind: switch for booleans, a bottom-sheet picker for fixed option lists, a slider for
// numeric values (Material 3), and a text field for free-form strings.
@Composable
private fun SettingCard(
    setting: SettingEntry,
    uiText: AppUiText? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    onChanged: (String) -> Unit,
) {
    val resolvedUiText = uiText ?: appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = cardBg(),
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val options = selectableSettingOptions[setting.key]
            val intOptions = intEnumSettingOptions[setting.key]
            when {
                setting.type == "bool" -> BoolSettingContent(setting, resolvedUiText, onChanged)
                setting.key in AudioInputDeviceKeys ->
                    AudioDeviceSettingContent(setting, isInput = true, resolvedUiText, onChanged)
                setting.key in AudioOutputDeviceKeys ->
                    AudioDeviceSettingContent(setting, isInput = false, resolvedUiText, onChanged)
                options != null -> SelectableSettingContent(setting, options, resolvedUiText, onChanged)
                intOptions != null -> IntEnumSettingContent(setting, intOptions, resolvedUiText, onChanged)
                setting.type == "int" || setting.type == "long" || setting.type == "double" ->
                    SliderSettingContent(setting, resolvedUiText, onChanged)
                else -> TextSettingContent(setting, resolvedUiText, onChanged)
            }
            // When the value differs from the default, a small "restore default" button sits in the
            // card's bottom-right corner; tapping it writes the built-in default back.
            if (setting.isModified && !setting.locked) {
                IconButton(
                    onClick = { onChanged(setting.default) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp),
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = resolvedUiText.restoreDefault,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoolSettingContent(setting: SettingEntry, uiText: AppUiText, onChanged: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLeadingBadge(setting)
        Spacer(Modifier.width(14.dp))
        Text(
            settingTitle(setting, uiText),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        SettingInfoButton(setting, uiText)
        AppSwitch(
            checked = setting.value == "true",
            enabled = !setting.locked,
            onCheckedChange = { onChanged(it.toString()) },
        )
    }
}

// Audio device settings used to be a free-text field, which meant the user had to already know
// (and correctly type) an exact SDL/OpenAL device name. These now show the Android devices that
// are actually connected right now (Bluetooth headset, phone speaker, wired headphones, etc.) in
// a picker, defaulting to the "Default Device" sentinel both backends already treat as "let the
// system decide" -- shown to the user as "Dispositivo Android" rather than the raw sentinel string.
private val AudioInputDeviceKeys = setOf("sdl_mic_device", "openal_mic_device")
private val AudioOutputDeviceKeys = setOf(
    "sdl_main_output_device", "openal_main_output_device",
    "sdl_padSpk_output_device", "openal_padSpk_output_device",
)
private const val DEFAULT_AUDIO_DEVICE_VALUE = "Default Device"

private data class AndroidAudioDeviceOption(val label: String, val value: String)

// Friendly label for a connected Android audio device, based on its type. Falls back to the raw
// product name (e.g. a USB device's reported name) when the type isn't one of the common cases.
private fun androidAudioDeviceLabel(
    device: android.media.AudioDeviceInfo,
    uiText: AppUiText,
): String? {
    val productName = device.productName?.toString().orEmpty()
    return when (device.type) {
        android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
        -> uiText.text("Alto-falante do aparelho")
        android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC -> uiText.text("Microfone do aparelho")
        android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        -> if (productName.isNotBlank()) "Bluetooth: $productName" else "Bluetooth"
        android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
        android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        -> uiText.text("Fone de ouvido com fio")
        android.media.AudioDeviceInfo.TYPE_USB_HEADSET,
        android.media.AudioDeviceInfo.TYPE_USB_DEVICE,
        -> if (productName.isNotBlank()) "USB: $productName" else "USB"
        else -> productName.ifBlank { null }
    }
}

// Queries the Android devices actually connected right now (via AudioManager), for the audio
// device picker. isInput selects microphone-capable devices vs. playback-capable ones.
private fun listAndroidAudioDevices(
    context: Context,
    isInput: Boolean,
    uiText: AppUiText,
): List<AndroidAudioDeviceOption> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        ?: return emptyList()
    val flag = if (isInput) {
        android.media.AudioManager.GET_DEVICES_INPUTS
    } else {
        android.media.AudioManager.GET_DEVICES_OUTPUTS
    }
    return runCatching {
        audioManager.getDevices(flag)
            .mapNotNull { device ->
                val label = androidAudioDeviceLabel(device, uiText) ?: return@mapNotNull null
                val value = device.productName?.toString()?.ifBlank { label } ?: label
                AndroidAudioDeviceOption(label, value)
            }
            .distinctBy { it.value }
    }.getOrDefault(emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioDeviceSettingContent(
    setting: SettingEntry,
    isInput: Boolean,
    uiText: AppUiText,
    onChanged: (String) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isDefault = setting.value.isBlank() || setting.value == DEFAULT_AUDIO_DEVICE_VALUE
    val displayLabel = if (isDefault) uiText.text("Dispositivo Android") else setting.value
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !setting.locked) { sheetOpen = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLeadingBadge(setting)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                settingTitle(setting, uiText),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                displayLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SettingInfoButton(setting, uiText)
    }
    if (sheetOpen) {
        val devices = remember(isInput) { listAndroidAudioDevices(context, isInput, uiText) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Text(
                uiText.settingTitle(setting.key),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text(uiText.text("Dispositivo Android")) },
                trailingContent = {
                    RadioButton(
                        selected = isDefault,
                        onClick = {
                            onChanged(DEFAULT_AUDIO_DEVICE_VALUE)
                            sheetOpen = false
                        },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    onChanged(DEFAULT_AUDIO_DEVICE_VALUE)
                    sheetOpen = false
                },
            )
            devices.forEach { device ->
                ListItem(
                    headlineContent = { Text(device.label) },
                    trailingContent = {
                        RadioButton(
                            selected = !isDefault && setting.value == device.value,
                            onClick = {
                                onChanged(device.value)
                                sheetOpen = false
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onChanged(device.value)
                        sheetOpen = false
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableSettingContent(
    setting: SettingEntry,
    options: List<String>,
    uiText: AppUiText,
    onChanged: (String) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val displayValue = if (setting.key == "cpu_backend" && setting.value !in options) {
        "x64-aarch64-asmjit"
    } else {
        setting.value
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !setting.locked) { sheetOpen = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLeadingBadge(setting)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                settingTitle(setting, uiText),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                uiText.optionLabel(displayValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingInfoButton(setting, uiText)
    }
    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Text(
                uiText.settingTitle(setting.key),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            options.forEach { option ->
                ListItem(
                    headlineContent = { Text(uiText.optionLabel(option)) },
                    trailingContent = {
                        RadioButton(
                            selected = option == displayValue,
                            onClick = {
                                onChanged(option)
                                sheetOpen = false
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onChanged(option)
                        sheetOpen = false
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// Int-backed enum setting: shows the label for the current int value and opens a bottom-sheet picker
// of labels. Selecting writes the mapped int back as a string. Falls back to showing the raw value
// when it doesn't match any known option.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntEnumSettingContent(
    setting: SettingEntry,
    options: List<Pair<String, Int>>,
    uiText: AppUiText,
    onChanged: (String) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val currentInt = setting.value.toIntOrNull()
    val currentLabel = options.firstOrNull { it.second == currentInt }?.first?.let(uiText::optionLabel)
        ?: setting.value.ifBlank { "—" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !setting.locked) { sheetOpen = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLeadingBadge(setting)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                settingTitle(setting, uiText),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                currentLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingInfoButton(setting, uiText)
    }
    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            scrimColor = Color.Transparent,
        ) {
            Text(
                uiText.settingTitle(setting.key),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                items(options, key = { it.second }) { (label, intValue) ->
                    ListItem(
                        headlineContent = { Text(uiText.optionLabel(label)) },
                        trailingContent = {
                            RadioButton(
                                selected = intValue == currentInt,
                                onClick = {
                                    onChanged(intValue.toString())
                                    sheetOpen = false
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onChanged(intValue.toString())
                            sheetOpen = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SliderSettingContent(setting: SettingEntry, uiText: AppUiText, onChanged: (String) -> Unit) {
    val range = sliderRangeFor(setting)
    val sliderSteps = sliderStepsFor(setting)
    val isDouble = setting.type == "double"
    val percent = isPercentSetting(setting)
    var sliderValue by remember(setting.section, setting.key) {
        mutableStateOf(
            (setting.value.toFloatOrNull() ?: range.start)
                .coerceIn(range.start, range.endInclusive),
        )
    }
    LaunchedEffect(setting.value) {
        val external = setting.value.toFloatOrNull()
        if (external != null && kotlin.math.abs(external - sliderValue) > 0.0001f) {
            sliderValue = external.coerceIn(range.start, range.endInclusive)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingLeadingBadge(setting)
            Spacer(Modifier.width(14.dp))
            Text(
                settingTitle(setting, uiText),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            val display = if (isDouble) {
                String.format(java.util.Locale.US, "%.2f", sliderValue)
            } else {
                sliderValue.toLong().toString()
            }
            Text(
                if (percent) "$display%" else display,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            SettingInfoButton(setting, uiText)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = normalizedSliderValue(setting, it).coerceIn(range.start, range.endInclusive) },
            onValueChangeFinished = {
                sliderValue = normalizedSliderValue(setting, sliderValue).coerceIn(range.start, range.endInclusive)
                val out = if (isDouble) {
                    String.format(java.util.Locale.US, "%.4f", sliderValue)
                } else {
                    sliderValue.toLong().toString()
                }
                onChanged(out)
            },
            valueRange = range,
            steps = sliderSteps,
            enabled = !setting.locked,
        )
    }
}

@Composable
private fun TextSettingContent(setting: SettingEntry, uiText: AppUiText, onChanged: (String) -> Unit) {
    var dialogOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !setting.locked) { dialogOpen = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLeadingBadge(setting)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                settingTitle(setting, uiText),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                setting.value.ifBlank { "—" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SettingInfoButton(setting, uiText)
    }
    if (dialogOpen) {
        var text by remember { mutableStateOf(setting.value) }
        org.sharpemu.android.ui.OverlayBlurEffect()
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(uiText.settingTitle(setting.key)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onChanged(text)
                    dialogOpen = false
                }) { Text(uiText.text("Salvar")) }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text(uiText.text("Cancelar")) }
            },
        )
    }
}
