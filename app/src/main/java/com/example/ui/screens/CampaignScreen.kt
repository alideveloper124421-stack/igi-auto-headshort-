package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CheatsState
import com.example.model.MissionInfo
import com.example.ui.theme.IgiAmberAccent
import com.example.ui.theme.IgiBackground
import com.example.ui.theme.IgiButtonBg
import com.example.ui.theme.IgiCardSurfaceSolid
import com.example.ui.theme.IgiDisabledBorder
import com.example.ui.theme.IgiPhosphorGreen
import com.example.ui.theme.IgiSageText
import com.example.ui.theme.IgiStatusGreen
import com.example.ui.theme.IgiSurfaceElevated
import com.example.viewmodel.IgiUiState
import java.util.Locale

@Composable
fun CampaignScreen(
    uiState: IgiUiState,
    missionBitmapProvider: (Int) -> Bitmap?,
    onBackToLauncher: () -> Unit,
    onSelectMission: (Int) -> Unit,
    onLaunchMission: (Int) -> Unit,
    onOpenContentManager: () -> Unit,
    onOpenEngineSettings: () -> Unit,
    onToggleRainAudio: (Boolean) -> Unit,
    onUpdateCheats: ((CheatsState) -> CheatsState) -> Unit
) {
    var showCheatsModal by remember { mutableStateOf(false) }

    val selectedMission = uiState.catalog.missions.find { it.number == uiState.selectedMissionNumber }
        ?: uiState.catalog.missions.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IgiBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top tactical header with subtle top-right Cheats Gear Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IgiCardSurfaceSolid)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = onBackToLauncher,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("campaign_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Launcher",
                        tint = IgiPhosphorGreen
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CAMPAIGN MISSIONS (1–14)",
                        style = MaterialTheme.typography.titleMedium.copy(color = IgiPhosphorGreen),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${uiState.rendererBackend.displayName} · ${uiState.targetFps} FPS · " +
                            if (uiState.isGameFolderVerified) "RETAIL DATA VERIFIED" else "PREVIEW MODE (NO RETAIL FOLDER)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (uiState.isGameFolderVerified) IgiStatusGreen else IgiAmberAccent
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleRainAudio(!uiState.rainAudioEnabled) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("rain_audio_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.rainAudioEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = if (uiState.rainAudioEnabled) "Mute ambient rain" else "Unmute ambient rain",
                        tint = if (uiState.rainAudioEnabled) IgiPhosphorGreen else IgiSageText
                    )
                }

                // Subtle Cheats Gear Icon (from README / PATCH_NOTES)
                IconButton(
                    onClick = { showCheatsModal = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("cheats_gear_button")
                ) {
                    val anyCheat = uiState.cheats.infiniteHealth ||
                        uiState.cheats.infiniteAmmo ||
                        uiState.cheats.allWeapons
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Open Cheats Menu",
                        tint = if (anyCheat) IgiAmberAccent else IgiPhosphorGreen
                    )
                }
            }

            HorizontalDivider(color = IgiPhosphorGreen.copy(alpha = 0.35f))

            // Quick bar: Configuration > Content & Input/Engine
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenContentManager,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, IgiPhosphorGreen.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = IgiButtonBg,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_content_manager_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = IgiPhosphorGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CONTENT (${uiState.contentLayers.count { it.enabled }})", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onOpenEngineSettings,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, IgiPhosphorGreen.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = IgiButtonBg,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_engine_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        tint = IgiPhosphorGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ENGINE & INPUT", style = MaterialTheme.typography.labelLarge)
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val isSplitLayout = maxWidth >= 680.dp
                if (isSplitLayout) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.catalog.missions, key = { it.number }) { mission ->
                                MissionListRow(
                                    mission = mission,
                                    selected = mission.number == uiState.selectedMissionNumber,
                                    bitmap = missionBitmapProvider(mission.number),
                                    onClick = { onSelectMission(mission.number) }
                                )
                            }
                        }
                        if (selectedMission != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .padding(bottom = 16.dp)
                            ) {
                                MissionBriefingPanel(
                                    mission = selectedMission,
                                    bitmap = missionBitmapProvider(selectedMission.number),
                                    cheats = uiState.cheats,
                                    isRetailVerified = uiState.isGameFolderVerified,
                                    onLaunch = { onLaunchMission(selectedMission.number) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .align(Alignment.TopCenter)
                            .fillMaxSize()
                    ) {
                        if (selectedMission != null) {
                            item(key = "briefing_card") {
                                MissionBriefingPanel(
                                    mission = selectedMission,
                                    bitmap = missionBitmapProvider(selectedMission.number),
                                    cheats = uiState.cheats,
                                    isRetailVerified = uiState.isGameFolderVerified,
                                    onLaunch = { onLaunchMission(selectedMission.number) }
                                )
                            }
                        }
                        items(uiState.catalog.missions, key = { it.number }) { mission ->
                            MissionListRow(
                                mission = mission,
                                selected = mission.number == uiState.selectedMissionNumber,
                                bitmap = missionBitmapProvider(mission.number),
                                onClick = { onSelectMission(mission.number) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCheatsModal) {
        CheatsModalDialog(
            cheats = uiState.cheats,
            onDismiss = { showCheatsModal = false },
            onUpdateCheats = onUpdateCheats
        )
    }
}

@Composable
private fun MissionListRow(
    mission: MissionInfo,
    selected: Boolean,
    bitmap: Bitmap?,
    onClick: () -> Unit
) {
    val thumb = remember(bitmap) { bitmap?.asImageBitmap() }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) IgiSurfaceElevated else IgiCardSurfaceSolid
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) IgiPhosphorGreen else IgiDisabledBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mission_row_${mission.number}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 62.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(IgiBackground)
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = "Mission ${mission.number}: ${mission.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.US, "MISSION %02d", mission.number),
                    style = MaterialTheme.typography.labelSmall.copy(color = IgiStatusGreen)
                )
                Text(
                    text = mission.title.uppercase(Locale.US),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (selected) IgiPhosphorGreen else Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Fog: ${mission.fogPreset.density} · ${mission.cameraPoints.size} cam pts" +
                        if (mission.hasCustomLut) " · LUT" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected Mission",
                    tint = IgiPhosphorGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MissionBriefingPanel(
    mission: MissionInfo,
    bitmap: Bitmap?,
    cheats: CheatsState,
    isRetailVerified: Boolean,
    onLaunch: () -> Unit
) {
    val hero = remember(bitmap) { bitmap?.asImageBitmap() }
    val firstPoint = mission.cameraPoints.firstOrNull()

    Surface(
        color = IgiCardSurfaceSolid,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, IgiPhosphorGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (hero != null) {
                Image(
                    bitmap = hero,
                    contentDescription = "Mission ${mission.number} reconnaissance preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(168f / 100f)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = String.format(Locale.US, "OPERATION %02d // %s", mission.number, mission.title.uppercase(Locale.US)),
                style = MaterialTheme.typography.titleLarge.copy(color = IgiPhosphorGreen)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val coordsText = if (firstPoint != null) {
                String.format(Locale.US, "WORLD COORDS: (%.1f, %.1f, %.1f)", firstPoint.x, firstPoint.y, firstPoint.z)
            } else {
                "WORLD COORDS: N/A"
            }
            Text(
                text = "$coordsText · FOG FALLOFF ${mission.fogPreset.heightFalloff}m",
                style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
            )

            val activeCheats = buildList {
                if (cheats.infiniteHealth) add("INF HEALTH")
                if (cheats.infiniteAmmo) add("INF AMMO")
                if (cheats.allWeapons) add("ALL WEAPONS")
            }
            if (activeCheats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = IgiAmberAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CHEATS ACTIVE: ${activeCheats.joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall.copy(color = IgiAmberAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLaunch,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgiPhosphorGreen,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("launch_mission_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRetailVerified) "DEPLOY MISSION" else "LAUNCH TACTICAL HUD PREVIEW",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun CheatsModalDialog(
    cheats: CheatsState,
    onDismiss: () -> Unit,
    onUpdateCheats: ((CheatsState) -> CheatsState) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IgiCardSurfaceSolid,
        titleContentColor = IgiPhosphorGreen,
        textContentColor = Color.White,
        title = {
            Text(
                text = "TACTICAL CHEATS",
                style = MaterialTheme.typography.titleLarge.copy(color = IgiPhosphorGreen)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CheatSwitchRow(
                    title = "Infinite Health",
                    subtitle = "Zero damage from bullets, falls, explosions, electric fences",
                    checked = cheats.infiniteHealth,
                    testTag = "cheat_infinite_health_switch",
                    onCheckedChange = { checked ->
                        onUpdateCheats { it.copy(infiniteHealth = checked) }
                    }
                )
                CheatSwitchRow(
                    title = "Infinite Ammunition",
                    subtitle = "Weapon magazines and reserves never deplete",
                    checked = cheats.infiniteAmmo,
                    testTag = "cheat_infinite_ammo_switch",
                    onCheckedChange = { checked ->
                        onUpdateCheats { it.copy(infiniteAmmo = checked) }
                    }
                )
                CheatSwitchRow(
                    title = "All Weapons",
                    subtitle = "Unlocks full 1–9 arsenal for the mission",
                    checked = cheats.allWeapons,
                    testTag = "cheat_all_weapons_switch",
                    onCheckedChange = { checked ->
                        onUpdateCheats { it.copy(allWeapons = checked) }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cheats_close_button")
            ) {
                Text("DONE", color = IgiPhosphorGreen, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun CheatSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(color = Color.White))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = IgiPhosphorGreen,
                uncheckedThumbColor = IgiSageText,
                uncheckedTrackColor = IgiButtonBg
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
