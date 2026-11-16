package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.RendererBackend
import com.example.ui.theme.IgiBackground
import com.example.ui.theme.IgiBrightMintBorder
import com.example.ui.theme.IgiButtonBg
import com.example.ui.theme.IgiCardSurface
import com.example.ui.theme.IgiDisabledBg
import com.example.ui.theme.IgiDisabledBorder
import com.example.ui.theme.IgiDisabledText
import com.example.ui.theme.IgiPhosphorGreen
import com.example.ui.theme.IgiSageText
import com.example.ui.theme.IgiStatusGreen
import com.example.viewmodel.IgiUiState

@Composable
fun LauncherScreen(
    uiState: IgiUiState,
    backgroundBitmap: Bitmap?,
    onSelectRenderer: (RendererBackend) -> Unit,
    onSelectGameFolder: () -> Unit,
    onPlayClicked: () -> Unit,
    onInstallContentPack: () -> Unit,
    onDisableAllContent: () -> Unit,
    onOpenCampaignMenu: () -> Unit
) {
    val bgImageBitmap = remember(backgroundBitmap) { backgroundBitmap?.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IgiBackground)
    ) {
        if (bgImageBitmap != null) {
            Image(
                bitmap = bgImageBitmap,
                contentDescription = "Project IGI tactical splash background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x880A0C08),
                                Color(0xAA0A0C08),
                                Color(0xCC0A0C08)
                            )
                        )
                    )
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 560.dp
            Surface(
                color = IgiCardSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, IgiPhosphorGreen),
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = if (isWide) 32.dp else 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.launcher_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = IgiPhosphorGreen,
                            fontSize = if (isWide) 20.sp else 17.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.launcher_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IgiSageText,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.installProgress.isRunning && !uiState.installProgress.isIndeterminate) {
                        LinearProgressIndicator(
                            progress = { uiState.installProgress.percent / 100f },
                            color = IgiPhosphorGreen,
                            trackColor = IgiDisabledBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    } else if (!uiState.isInitialized || uiState.installProgress.isRunning) {
                        LinearProgressIndicator(
                            color = IgiPhosphorGreen,
                            trackColor = IgiDisabledBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { if (uiState.isGameFolderVerified) 1f else 0.15f },
                            color = if (uiState.isGameFolderVerified) IgiPhosphorGreen else IgiStatusGreen,
                            trackColor = IgiDisabledBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.statusText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (uiState.isGameFolderVerified) IgiPhosphorGreen else IgiStatusGreen,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("launcher_status_text")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.label_renderer),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = IgiSageText,
                            letterSpacing = 1.6.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RendererOptionButton(
                            label = stringResource(R.string.btn_vulkan),
                            selected = uiState.rendererBackend == RendererBackend.VULKAN,
                            onClick = { onSelectRenderer(RendererBackend.VULKAN) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("renderer_vulkan_button")
                        )
                        RendererOptionButton(
                            label = stringResource(R.string.btn_webgpu),
                            selected = uiState.rendererBackend == RendererBackend.WEBGPU,
                            onClick = { onSelectRenderer(RendererBackend.WEBGPU) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("renderer_webgpu_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isWide) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TacticalActionButton(
                                text = stringResource(R.string.btn_select_folder),
                                icon = Icons.Filled.FolderOpen,
                                onClick = onSelectGameFolder,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("select_game_folder_button")
                            )
                            PlayPrimaryButton(
                                enabled = uiState.isGameFolderVerified,
                                onClick = onPlayClicked,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("play_button")
                            )
                        }
                    } else {
                        TacticalActionButton(
                            text = stringResource(R.string.btn_select_folder),
                            icon = Icons.Filled.FolderOpen,
                            onClick = onSelectGameFolder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_game_folder_button")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PlayPrimaryButton(
                            enabled = uiState.isGameFolderVerified,
                            onClick = onPlayClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("play_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isWide) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TacticalActionButton(
                                text = stringResource(R.string.btn_install_pack),
                                icon = Icons.Filled.Layers,
                                onClick = onInstallContentPack,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("install_pack_button")
                            )
                            TacticalActionButton(
                                text = stringResource(R.string.btn_disable_content),
                                icon = Icons.Filled.LayersClear,
                                onClick = onDisableAllContent,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("disable_content_button")
                            )
                        }
                    } else {
                        TacticalActionButton(
                            text = stringResource(R.string.btn_install_pack),
                            icon = Icons.Filled.Layers,
                            onClick = onInstallContentPack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("install_pack_button")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TacticalActionButton(
                            text = stringResource(R.string.btn_disable_content),
                            icon = Icons.Filled.LayersClear,
                            onClick = onDisableAllContent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("disable_content_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onOpenCampaignMenu,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, IgiStatusGreen.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = IgiSageText,
                            containerColor = Color(0x4414341C)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_campaign_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Open Campaign and Engine Menu",
                            tint = IgiPhosphorGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (uiState.isGameFolderVerified) {
                                "CAMPAIGN, CONTENT & ENGINE"
                            } else {
                                "OPEN CAMPAIGN & ENGINE (PREVIEW)"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    val enabledLayerCount = uiState.contentLayers.count { it.enabled }
                    if (uiState.contentLayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsEsports,
                                contentDescription = null,
                                tint = IgiStatusGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "CONTENT LAYERS: $enabledLayerCount/${uiState.contentLayers.size} ACTIVE · 30 FPS PACER",
                                style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RendererOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) IgiBrightMintBorder else IgiDisabledBorder
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) IgiPhosphorGreen else IgiButtonBg,
            contentColor = if (selected) Color.Black else IgiDisabledText
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun TacticalActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.5.dp, IgiPhosphorGreen),
        colors = ButtonDefaults.buttonColors(
            containerColor = IgiButtonBg,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IgiPhosphorGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun PlayPrimaryButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) IgiBrightMintBorder else IgiDisabledBorder
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = IgiPhosphorGreen,
            contentColor = Color.Black,
            disabledContainerColor = IgiDisabledBg,
            disabledContentColor = IgiDisabledText
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = stringResource(R.string.btn_play),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
