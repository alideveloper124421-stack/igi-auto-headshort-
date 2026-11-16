package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.combat.AimAssistConfig
import com.example.combat.AimAssistMode
import com.example.model.GamepadSnapshot
import com.example.model.GyroSnapshot
import com.example.model.RendererBackend
import com.example.ui.theme.IgiAmberAccent
import com.example.ui.theme.IgiBackground
import com.example.ui.theme.IgiButtonBg
import com.example.ui.theme.IgiCardSurfaceSolid
import com.example.ui.theme.IgiDisabledBorder
import com.example.ui.theme.IgiPhosphorGreen
import com.example.ui.theme.IgiSageText
import com.example.ui.theme.IgiStatusGreen
import com.example.viewmodel.IgiUiState
import java.util.Locale

@Composable
fun EngineSettingsScreen(
    uiState: IgiUiState,
    gyroSnapshot: GyroSnapshot,
    gamepadSnapshot: GamepadSnapshot,
    onBack: () -> Unit,
    onSelectRenderer: (RendererBackend) -> Unit,
    onSetTargetFps: (Int) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onResetGyro: () -> Unit,
    onToggleRainAudio: (Boolean) -> Unit,
    onRequestMicPermission: () -> Unit,
    onExportLogcat: () -> Unit,
    onSelectAimAssistMode: (AimAssistMode) -> Unit,
    onUpdateAimAssistConfig: ((AimAssistConfig) -> AimAssistConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IgiBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(IgiCardSurfaceSolid)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = IgiPhosphorGreen
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ENGINE, INPUT & AUDIO DIAGNOSTICS",
                    style = MaterialTheme.typography.titleMedium.copy(color = IgiPhosphorGreen)
                )
                Text(
                    text = "ASSETS: ${uiState.catalog.lampCount} LAMPS · ${uiState.catalog.materialOverrideCount} PBR MATERIALS (${uiState.catalog.glassMaterialCount} GLASS)",
                    style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                )
            }
        }

        HorizontalDivider(color = IgiPhosphorGreen.copy(alpha = 0.35f))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 14.dp)
            ) {
                // Graphics & Frame Pacer
                SettingsSectionCard(title = "GRAPHICS & FRAME PACER") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RendererBackend.entries.forEach { backend ->
                            val selected = uiState.rendererBackend == backend
                            Button(
                                onClick = { onSelectRenderer(backend) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) IgiPhosphorGreen else IgiButtonBg,
                                    contentColor = if (selected) Color.Black else IgiSageText
                                ),
                                border = BorderStroke(1.dp, if (selected) IgiPhosphorGreen else IgiDisabledBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(backend.displayName, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "FRAME PACER TARGET: ${uiState.targetFps} FPS (30 FPS thermal-friendly default)",
                        style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(30, 60).forEach { fps ->
                            val selected = uiState.targetFps == fps
                            Button(
                                onClick = { onSetTargetFps(fps) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) IgiPhosphorGreen else IgiButtonBg,
                                    contentColor = if (selected) Color.Black else IgiSageText
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("fps_${fps}_button")
                            ) {
                                Text("$fps FPS", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Auto Headshot Aim Assist (Feral-style soft lock for headshots)
                SettingsSectionCard(title = "AUTO HEADSHOT AIM ASSIST") {
                    val assist = uiState.aimAssistConfig
                    Text(
                        text = assist.mode.description,
                        style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AimAssistMode.entries.forEach { mode ->
                            val selected = assist.mode == mode
                            Button(
                                onClick = { onSelectAimAssistMode(mode) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) IgiPhosphorGreen else IgiButtonBg,
                                    contentColor = if (selected) Color.Black else IgiSageText
                                ),
                                border = BorderStroke(1.dp, if (selected) IgiPhosphorGreen else IgiDisabledBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("aim_assist_${mode.id}_button")
                            ) {
                                Text(
                                    text = if (mode == AimAssistMode.SOFT) "SOFT" else mode.displayName,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    if (assist.mode != AimAssistMode.OFF) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = String.format(Locale.US, "BODY-TO-HEAD SNAP SPEED: %.1f", assist.snapSpeed),
                            style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                        )
                        Slider(
                            value = assist.snapSpeed,
                            onValueChange = { value ->
                                onUpdateAimAssistConfig { it.copy(snapSpeed = value) }
                            },
                            valueRange = 7f..24f,
                            colors = SliderDefaults.colors(
                                thumbColor = IgiPhosphorGreen,
                                activeTrackColor = IgiPhosphorGreen,
                                inactiveTrackColor = IgiButtonBg
                            )
                        )
                        Text(
                            text = String.format(Locale.US, "MANUAL BREAKAWAY PULL: %.3f", assist.breakawayDistance),
                            style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                        )
                        Slider(
                            value = assist.breakawayDistance,
                            onValueChange = { value ->
                                onUpdateAimAssistConfig { it.copy(breakawayDistance = value) }
                            },
                            valueRange = 0.04f..0.16f,
                            colors = SliderDefaults.colors(
                                thumbColor = IgiPhosphorGreen,
                                activeTrackColor = IgiPhosphorGreen,
                                inactiveTrackColor = IgiButtonBg
                            )
                        )
                    }
                }

                // Gyroscope Sensor Aim
                SettingsSectionCard(title = "GYROSCOPE AIM SENSOR") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null,
                            tint = IgiPhosphorGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (gyroSnapshot.available) "Hardware Gyroscope Ready" else "Gyroscope Sensor Unavailable",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Yaw: %.3f rad · Pitch: %.3f rad",
                                    gyroSnapshot.yawRadians,
                                    gyroSnapshot.pitchRadians
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp)
                            )
                        }
                        Switch(
                            checked = gyroSnapshot.enabled,
                            onCheckedChange = onToggleGyro,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = IgiPhosphorGreen
                            ),
                            modifier = Modifier.testTag("gyro_enable_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onResetGyro,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IgiButtonBg,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, IgiPhosphorGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gyro_recenter_button")
                    ) {
                        Text("RECENTER GYROSCOPE", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Hardware Controller Support
                SettingsSectionCard(title = "GAMEPAD / CONTROLLER SUPPORT") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint = if (gamepadSnapshot.connected) IgiPhosphorGreen else IgiSageText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (gamepadSnapshot.connected) {
                                    "CONNECTED: ${gamepadSnapshot.deviceName}"
                                } else {
                                    "NO CONTROLLER DETECTED (Touch HUD Active)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (gamepadSnapshot.connected) IgiPhosphorGreen else Color.White
                                )
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "L(%.2f, %.2f) R(%.2f, %.2f) LT=%.2f RT=%.2f · %s",
                                    gamepadSnapshot.leftX,
                                    gamepadSnapshot.leftY,
                                    gamepadSnapshot.rightX,
                                    gamepadSnapshot.rightY,
                                    gamepadSnapshot.leftTrigger,
                                    gamepadSnapshot.rightTrigger,
                                    gamepadSnapshot.lastInputLabel
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val mappings = listOf(
                        "Left / Right Stick" to "Move / Look (dead zone 0.18)",
                        "RT / LT" to "Fire / Aim",
                        "RB / LB" to "Next / Previous Weapon",
                        "A / B / X / Y" to "Jump (hold skip) / Crouch / Reload / Use",
                        "Start / Select" to "Pause / Map Computer",
                        "D-Pad Up / Down" to "Binoculars / Flashlight",
                        "L3 / R3 (hold)" to "Walk-Run Toggle / Weapon Wheel"
                    )
                    mappings.forEach { (input, action) ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(input, style = MaterialTheme.typography.labelSmall.copy(color = IgiStatusGreen))
                            Text(action, style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp))
                        }
                    }
                }

                // Audio & Co-op Voice Mic Permission (asked only on demand)
                SettingsSectionCard(title = "AUDIO & CO-OP VOICE") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ambient Rain Bed (rain-loop.wav)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Auto-mutes during cutscenes & pause",
                                style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp)
                            )
                        }
                        Switch(
                            checked = uiState.rainAudioEnabled,
                            onCheckedChange = onToggleRainAudio,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = IgiPhosphorGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onRequestMicPermission,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IgiButtonBg,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.voiceMicPermissionGranted) IgiPhosphorGreen else IgiAmberAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mic_permission_button")
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, tint = IgiPhosphorGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.voiceMicPermissionGranted) {
                                "CO-OP VOICE MIC GRANTED"
                            } else {
                                "ENABLE CO-OP VOICE MICROPHONE"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Diagnostics Log Export
                Button(
                    onClick = onExportLogcat,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IgiButtonBg,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, IgiPhosphorGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_logcat_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = IgiPhosphorGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT LOGCAT TO DOWNLOADS", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = IgiCardSurfaceSolid,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, IgiPhosphorGreen.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(color = IgiPhosphorGreen)
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
