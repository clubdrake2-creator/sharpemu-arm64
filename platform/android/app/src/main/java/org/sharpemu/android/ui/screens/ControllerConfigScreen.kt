// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.
//
// Physical-controller configuration screen, ported from PCSX2-Android (the ControlsSection of its
// SettingsScreen.kt) and adapted for shadPS4: per-slot control type, auto-map, a full mapping guide,
// per-stick and per-button binding rows with live "press a button to capture" (onPreviewKeyEvent),
// PS4-style action badges, and the grouped Material 3 card design. Persistence goes to ControlPrefs.

package org.sharpemu.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sharpemu.android.data.ControlPrefs
import org.sharpemu.android.ui.theme.AppearancePrefs
import org.sharpemu.android.ui.theme.appUiText

// Dark-aware colors matching the rest of the settings UI: in dark mode the app background and card
// colors are swapped so cards read as elevated surfaces instead of black-on-black.
@Composable
private fun ccDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
private fun ccAppBg(): Color =
    if (ccDark()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun ccCardBg(): Color =
    if (ccDark()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

@Composable
private fun CcBackButton(onClick: () -> Unit) {
    val uiText = appUiText(AppearancePrefs.getAppLanguage(LocalContext.current))
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(ccCardBg()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = uiText.text("Voltar"),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private data class CcAction(val key: String, val label: String, val subtitle: String)

private val ccDirectional = listOf(
    CcAction("dpad_up", "Direcional ↑", "Direção digital para cima"),
    CcAction("dpad_right", "Direcional →", "Direção digital para a direita"),
    CcAction("dpad_down", "Direcional ↓", "Direção digital para baixo"),
    CcAction("dpad_left", "Direcional ←", "Direção digital para a esquerda"),
)
private val ccFace = listOf(
    CcAction("triangle", "Triângulo", "Botão de ação superior"),
    CcAction("circle", "Círculo", "Botão de ação direito"),
    CcAction("cross", "X (Cruz)", "Botão de ação inferior"),
    CcAction("square", "Quadrado", "Botão de ação esquerdo"),
)
private val ccShoulder = listOf(
    CcAction("l1", "L1", "Ombro esquerdo"),
    CcAction("l2", "L2", "Gatilho esquerdo"),
    CcAction("r1", "R1", "Ombro direito"),
    CcAction("r2", "R2", "Gatilho direito"),
)
private val ccSystem = listOf(
    CcAction("start", "Options (Start)", "Inicia/pausa"),
    CcAction("select", "Touchpad (Select)", "Ações secundárias"),
)
private val ccAnalogLeftKeys =
    listOf("left_stick_click", "left_stick_up", "left_stick_right", "left_stick_down", "left_stick_left")
private val ccAnalogRightKeys =
    listOf("right_stick_click", "right_stick_up", "right_stick_right", "right_stick_down", "right_stick_left")
private val ccAnalogPromptsLeft = listOf(
    "Pressione o clique do analógico esquerdo (L3).",
    "Mova o analógico esquerdo para cima.",
    "Mova o analógico esquerdo para a direita.",
    "Mova o analógico esquerdo para baixo.",
    "Mova o analógico esquerdo para a esquerda.",
)
private val ccAnalogPromptsRight = listOf(
    "Pressione o clique do analógico direito (R3).",
    "Mova o analógico direito para cima.",
    "Mova o analógico direito para a direita.",
    "Mova o analógico direito para baixo.",
    "Mova o analógico direito para a esquerda.",
)

private data class CcMappingState(
    val title: String,
    val actionKeys: List<String>,
    val prompts: List<String>,
    val stepIndex: Int = 0,
    val showNextButton: Boolean = false,
    val autoAdvanceOnCapture: Boolean = false,
) {
    val currentKey get() = actionKeys[stepIndex]
    val currentPrompt get() = prompts[stepIndex]
    val isLastStep get() = stepIndex >= actionKeys.lastIndex
}

private fun ccActionLabel(key: String): String = "controller_action_$key"
private fun ccActionSubtitle(key: String): String = "controller_action_${key}_subtitle"
private fun ccActionPrompt(key: String): String = "controller_prompt_$key"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerConfigScreen(slot: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val uiText = appUiText(AppearancePrefs.getAppLanguage(context))
    var controlType by remember(slot) { mutableStateOf(ControlPrefs.getControlType(context, slot)) }
    var autoMap by remember(slot) { mutableStateOf(ControlPrefs.getAutoMap(context, slot)) }
    var bindings by remember(slot) { mutableStateOf(ControlPrefs.getBindings(context, slot)) }
    var typeSheet by remember { mutableStateOf(false) }
    var mappingSheet by remember { mutableStateOf<CcMappingState?>(null) }

    val hasType = !controlType.equals("Nenhum", ignoreCase = true)
    fun value(key: String): String =
        bindings[key]?.ifBlank { uiText.text("Não definido") } ?: uiText.text("Não definido")
    fun mapped(keys: List<String>): Int = keys.count { !bindings[it].isNullOrBlank() }
    val fullKeys = ccDirectional.map { it.key } + ccFace.map { it.key } +
        ccShoulder.map { it.key } + ccSystem.map { it.key } + ccAnalogLeftKeys + ccAnalogRightKeys
    val fullPrompts = fullKeys.map { uiText.text(ccActionPrompt(it)) }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = ccAppBg(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ccAppBg(),
                ),
                title = { Text("${uiText.text("Controle")} $slot") },
                navigationIcon = { CcBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "type") {
                CcSectionHeader("${uiText.text("Controle")} #$slot")
                CcGroup {
                    CcOptionCard(
                        label = uiText.text("Tipo de controle"),
                        subtitle = uiText.text("Selecione o tipo para habilitar o mapeamento"),
                        value = uiText.optionLabel(controlType),
                        icon = Icons.Default.Tune,
                        shape = ccShape(0, 1),
                        onClick = { typeSheet = true },
                    )
                }
            }

            if (hasType) {
                item(key = "mapping") {
                    CcSectionHeader(uiText.text("Mapeamento"))
                    CcGroup {
                        CcOptionCard(
                            label = uiText.text("Guia de mapeamento completo"),
                            subtitle = uiText.text("Percorre todos os botões em sequência"),
                            value = "${mapped(fullKeys)}/${fullKeys.size} ${uiText.text("configurados")}",
                            icon = Icons.Default.Route,
                            shape = ccShape(0, 2),
                            onClick = {
                                mappingSheet = CcMappingState(
                                    title = uiText.text("Guia de mapeamento completo"),
                                    actionKeys = fullKeys,
                                    prompts = fullPrompts,
                                    autoAdvanceOnCapture = true,
                                )
                            },
                        )
                        CcSwitchCard(
                            label = uiText.text("Mapeamento automático"),
                            subtitle = if (autoMap) uiText.text("Mapa padrão de gamepad aplicado")
                            else uiText.optionLabel("Disabled"),
                            checked = autoMap,
                            icon = Icons.Default.AutoFixHigh,
                            shape = ccShape(1, 2),
                        ) { enabled ->
                            autoMap = enabled
                            ControlPrefs.setAutoMap(context, slot, enabled)
                            ControlPrefs.setBindings(
                                context, slot,
                                if (enabled) ControlPrefs.autoMapTemplate else emptyMap(),
                            )
                            bindings = ControlPrefs.getBindings(context, slot)
                        }
                    }
                }

                item(key = "sticks") {
                    CcSectionHeader(uiText.text("Analógicos"))
                    CcGroup {
                        CcBindingRow(
                            actionKey = "left_stick",
                            label = uiText.text("Analógico esquerdo"),
                            subtitle = uiText.text("Clique e direções (L3)"),
                            value = "${mapped(ccAnalogLeftKeys)}/${ccAnalogLeftKeys.size} ${uiText.text("configurados")}",
                            shape = ccShape(0, 2),
                            onClick = {
                                mappingSheet = CcMappingState(
                                    uiText.text("Mapear analógico esquerdo"), ccAnalogLeftKeys,
                                    ccAnalogLeftKeys.map { uiText.text(ccActionPrompt(it)) },
                                    showNextButton = true,
                                )
                            },
                        )
                        CcBindingRow(
                            actionKey = "right_stick",
                            label = uiText.text("Analógico direito"),
                            subtitle = uiText.text("Clique e direções (R3)"),
                            value = "${mapped(ccAnalogRightKeys)}/${ccAnalogRightKeys.size} ${uiText.text("configurados")}",
                            shape = ccShape(1, 2),
                            onClick = {
                                mappingSheet = CcMappingState(
                                    uiText.text("Mapear analógico direito"), ccAnalogRightKeys,
                                    ccAnalogRightKeys.map { uiText.text(ccActionPrompt(it)) },
                                    showNextButton = true,
                                )
                            },
                        )
                    }
                }

                val groups = listOf(
                    "Direcionais" to ccDirectional,
                    "Botões de ação" to ccFace,
                    "Ombros e gatilhos" to ccShoulder,
                    "Sistema" to ccSystem,
                )
                items(groups, key = { it.first }) { (groupName, actions) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CcSectionHeader(uiText.text(groupName))
                        CcGroup {
                            actions.forEachIndexed { index, action ->
                                CcBindingRow(
                                    actionKey = action.key,
                                    label = uiText.text(ccActionLabel(action.key)),
                                    subtitle = uiText.text(ccActionSubtitle(action.key)),
                                    value = value(action.key),
                                    shape = ccShape(index, actions.size),
                                    onClick = {
                                        mappingSheet = CcMappingState(
                                            "${uiText.text("Mapear")} ${uiText.text(ccActionLabel(action.key))}",
                                            listOf(action.key),
                                            listOf(uiText.text(ccActionPrompt(action.key))),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Type picker
    if (typeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(sheetState = sheetState, onDismissRequest = { typeSheet = false }) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    Text(
                        uiText.text("Tipo de controle"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                items(ControlPrefs.controlTypes) { type ->
                    ListItem(
                        headlineContent = { Text(uiText.optionLabel(type), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = { RadioButton(selected = type == controlType, onClick = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                controlType = type
                                ControlPrefs.setControlType(context, slot, type)
                                typeSheet = false
                            },
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    // Mapping capture sheet
    mappingSheet?.let { session ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val focusRequester = remember { FocusRequester() }
        var captured by remember(session.currentKey) { mutableStateOf(value(session.currentKey)) }
        LaunchedEffect(session.currentKey) { focusRequester.requestFocus() }

        org.sharpemu.android.ui.OverlayBlurEffect()
        ModalBottomSheet(sheetState = sheetState, onDismissRequest = { mappingSheet = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.key == Key.Back) return@onPreviewKeyEvent false
                        val keyName = event.key.toString()
                        ControlPrefs.setBinding(context, slot, session.currentKey, keyName)
                        bindings = ControlPrefs.getBindings(context, slot)
                        captured = keyName
                        when {
                            session.autoAdvanceOnCapture ->
                                mappingSheet = if (session.isLastStep) null
                                else session.copy(stepIndex = session.stepIndex + 1)
                            !session.showNextButton -> mappingSheet = null
                        }
                        true
                    },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(session.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    session.currentPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val motion = ccAnalogMotion(session.currentKey)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val pulse = rememberInfiniteTransition(label = "pulse")
                    val s by pulse.animateFloat(
                        initialValue = 0.92f, targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse,
                        ),
                        label = "s",
                    )
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .graphicsLayer { scaleX = s; scaleY = s },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Always show the real analog artwork -- it used to be replaced by a plain
                        // grey placeholder circle whenever a directional/click key was being mapped.
                        CcActionBadge(session.currentKey, Modifier.size(104.dp))
                        if ((session.showNextButton || session.autoAdvanceOnCapture) &&
                            motion != CcMotion.NONE
                        ) {
                            CcAnalogDirectionHint(motion)
                        }
                    }
                }
                Text(
                    "${uiText.text("Mapeado")}: $captured",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = {
                        if (session.showNextButton) {
                            session.actionKeys.forEach { ControlPrefs.setBinding(context, slot, it, "") }
                        } else {
                            ControlPrefs.setBinding(context, slot, session.currentKey, "")
                        }
                        bindings = ControlPrefs.getBindings(context, slot)
                        captured = uiText.text("Não definido")
                    }) { Text(uiText.restoreDefault) }
                    if (session.showNextButton) {
                        Button(onClick = {
                            mappingSheet = if (session.isLastStep) null
                            else session.copy(stepIndex = session.stepIndex + 1)
                        }) { Text(if (session.isLastStep) uiText.text("Concluir") else uiText.text("Próximo")) }
                    }
                }
            }
        }
    }
}

// --- Design helpers (ported from PCSX2 SettingsScreen) ---

// 15dp/5dp matches the grouped-card corner radius used everywhere else in the app (see
// SettingsCategoryScreen in MainActivity.kt) -- this used to be 30dp/5dp here only, which read as
// a different, inconsistent card style on this screen.
private fun ccShape(index: Int, size: Int): RoundedCornerShape {
    if (size <= 1) return RoundedCornerShape(15.dp)
    val top = if (index == 0) 15.dp else 5.dp
    val bottom = if (index == size - 1) 15.dp else 5.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun CcGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp), content = content)
}

@Composable
private fun CcSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun CcCard(shape: RoundedCornerShape, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = ccCardBg()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { Column(content = content) }
}

@Composable
private fun CcIcon(icon: ImageVector, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun CcOptionCard(
    label: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    CcCard(shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, top = 12.dp, end = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CcIcon(icon)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    value, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CcSwitchCard(
    label: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    shape: RoundedCornerShape,
    onChange: (Boolean) -> Unit,
) {
    CcCard(shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CcIcon(icon)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                thumbContent = if (checked) {
                    {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun CcBindingRow(
    actionKey: String,
    label: String,
    subtitle: String,
    value: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    CcCard(shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, top = 12.dp, end = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CcActionBadge(actionKey, Modifier.size(42.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    value, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --- Action badges (ported) ---

@Composable
private fun CcActionBadge(actionKey: String, modifier: Modifier = Modifier) {
    val res = ccButtonDrawable(actionKey)
    if (res != null) {
        // Same flat theme-tinted icon treatment as the landscape Home screen's button hints
        // (SwitchButtonHint/SwitchAddButton) -- a single-color glyph via Icon's tint, which resolves
        // to white in dark theme and dark in light theme automatically. This used to draw the raw
        // two-tone artwork (black badge + white glyph) via Image, only inverting it in light theme,
        // which left the badge black in dark theme too instead of matching the rest of the app.
        androidx.compose.material3.Icon(
            painter = androidx.compose.ui.res.painterResource(res),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
    } else {
        CcSystemBadge(actionKey.uppercase(), modifier)
    }
}

// Maps a mapping action key to its real button drawable (generated from the user's SVG buttons).
private fun ccButtonDrawable(actionKey: String): Int? = when (actionKey) {
    "triangle" -> org.sharpemu.android.R.drawable.triangle
    "circle" -> org.sharpemu.android.R.drawable.circle
    "cross" -> org.sharpemu.android.R.drawable.cross
    "square" -> org.sharpemu.android.R.drawable.square
    "dpad_up" -> org.sharpemu.android.R.drawable.dpad_top
    "dpad_right" -> org.sharpemu.android.R.drawable.dpad_right
    "dpad_down" -> org.sharpemu.android.R.drawable.dpad_bottom
    "dpad_left" -> org.sharpemu.android.R.drawable.dpad_left
    "l1" -> org.sharpemu.android.R.drawable.l1
    "l2" -> org.sharpemu.android.R.drawable.l2
    "r1" -> org.sharpemu.android.R.drawable.r1
    "r2" -> org.sharpemu.android.R.drawable.r2
    "start" -> org.sharpemu.android.R.drawable.start
    "select" -> org.sharpemu.android.R.drawable.select
    "left_stick_click" -> org.sharpemu.android.R.drawable.l3
    "right_stick_click" -> org.sharpemu.android.R.drawable.r3
    "left_stick", "left_stick_up", "left_stick_right", "left_stick_down", "left_stick_left" ->
        org.sharpemu.android.R.drawable.left_stick
    "right_stick", "right_stick_up", "right_stick_right", "right_stick_down", "right_stick_left" ->
        org.sharpemu.android.R.drawable.right_stick
    else -> null
}

@Composable
private fun CcSystemBadge(label: String, modifier: Modifier) {
    Box(
        modifier = modifier.clip(CircleShape).background(Color(0xFFBDBDBD))
            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color(0xFF505050), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

// --- Analog guide animation (ported) ---

private enum class CcMotion { CLICK, UP, RIGHT, DOWN, LEFT, NONE }

private fun ccAnalogMotion(actionKey: String): CcMotion = when {
    actionKey.endsWith("_click") -> CcMotion.CLICK
    actionKey.endsWith("_up") -> CcMotion.UP
    actionKey.endsWith("_right") -> CcMotion.RIGHT
    actionKey.endsWith("_down") -> CcMotion.DOWN
    actionKey.endsWith("_left") -> CcMotion.LEFT
    else -> CcMotion.NONE
}

// Small moving accent dot layered ON TOP of the real analog stick artwork (CcActionBadge) to hint
// which direction/click is being captured -- this used to be a full replacement for the drawable
// (a plain grey circle with a moving dot inside, no stick artwork at all); now the artwork always
// shows and this is just the direction cue drawn over it.
@Composable
private fun CcAnalogDirectionHint(motion: CcMotion) {
    val travel = 22.dp
    val transition = rememberInfiniteTransition(label = "analog")
    val clickScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes { durationMillis = 1400; 1f at 0; 0.76f at 420; 0.76f at 620; 1f at 960; 1f at 1400 },
            RepeatMode.Restart,
        ),
        label = "click",
    )
    val moveProgress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes { durationMillis = 1700; 0f at 0; 1f at 620; 0f at 1240; 0f at 1700 },
            RepeatMode.Restart,
        ),
        label = "move",
    )
    val moveX = when (motion) {
        CcMotion.RIGHT -> travel * moveProgress
        CcMotion.LEFT -> (-travel) * moveProgress
        else -> 0.dp
    }
    val moveY = when (motion) {
        CcMotion.DOWN -> travel * moveProgress
        CcMotion.UP -> (-travel) * moveProgress
        else -> 0.dp
    }
    val innerScale = if (motion == CcMotion.CLICK) clickScale else 1f
    Box(
        modifier = Modifier
            .size(26.dp).offset(x = moveX, y = moveY)
            .graphicsLayer { scaleX = innerScale; scaleY = innerScale }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
    )
}
