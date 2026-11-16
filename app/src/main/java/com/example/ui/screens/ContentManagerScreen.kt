package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.ContentLayer
import com.example.ui.theme.IgiAmberAccent
import com.example.ui.theme.IgiBackground
import com.example.ui.theme.IgiButtonBg
import com.example.ui.theme.IgiCardSurfaceSolid
import com.example.ui.theme.IgiDisabledBorder
import com.example.ui.theme.IgiPhosphorGreen
import com.example.ui.theme.IgiSageText
import com.example.ui.theme.IgiStatusGreen
import com.example.ui.theme.IgiSurfaceElevated

@Composable
fun ContentManagerScreen(
    layers: List<ContentLayer>,
    onBack: () -> Unit,
    onToggleLayer: (String, Boolean) -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onRescan: () -> Unit,
    onInstallPack: () -> Unit,
    onDisableAll: () -> Unit
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
                    .testTag("content_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = IgiPhosphorGreen
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONFIGURATION > CONTENT",
                    style = MaterialTheme.typography.titleMedium.copy(color = IgiPhosphorGreen)
                )
                Text(
                    text = "TOP LAYER WINS · content-profile.json",
                    style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                )
            }
            IconButton(
                onClick = onRescan,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("rescan_content_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Rescan content layers",
                    tint = IgiPhosphorGreen
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
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onInstallPack,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IgiButtonBg,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, IgiPhosphorGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("content_install_pack_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = IgiPhosphorGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INSTALL PACK", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onDisableAll,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, IgiAmberAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IgiAmberAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("content_disable_all_button")
                    ) {
                        Icon(Icons.Filled.LayersClear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISABLE ALL", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (layers.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Layers,
                                contentDescription = null,
                                tint = IgiStatusGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NO CONTENT LAYERS FOUND",
                                style = MaterialTheme.typography.titleMedium.copy(color = IgiSageText)
                            )
                            Text(
                                text = "Tap INSTALL PACK to copy a data pack folder into app-private storage.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(layers, key = { _, layer -> layer.id }) { index, layer ->
                            ContentLayerCard(
                                priority = index + 1,
                                total = layers.size,
                                layer = layer,
                                onToggle = { enabled -> onToggleLayer(layer.id, enabled) },
                                onMoveUp = { onMoveLayer(layer.id, -1) },
                                onMoveDown = { onMoveLayer(layer.id, 1) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentLayerCard(
    priority: Int,
    total: Int,
    layer: ContentLayer,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (layer.enabled) IgiSurfaceElevated else IgiCardSurfaceSolid
        ),
        border = BorderStroke(
            width = if (layer.enabled) 1.5.dp else 1.dp,
            color = if (layer.enabled) IgiPhosphorGreen else IgiDisabledBorder
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#$priority · ${layer.kind.uppercase()} LAYER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (layer.enabled) IgiPhosphorGreen else IgiSageText
                    )
                )
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                )
                Text(
                    text = "${layer.fileCount} override files · id=${layer.id}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText, fontSize = 12.sp)
                )
            }

            IconButton(
                onClick = onMoveUp,
                enabled = priority > 1,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("layer_up_${layer.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Move layer up",
                    tint = if (priority > 1) IgiPhosphorGreen else IgiDisabledBorder
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = priority < total,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("layer_down_${layer.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = "Move layer down",
                    tint = if (priority < total) IgiPhosphorGreen else IgiDisabledBorder
                )
            }

            Switch(
                checked = layer.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = IgiPhosphorGreen,
                    uncheckedThumbColor = IgiSageText,
                    uncheckedTrackColor = IgiButtonBg
                ),
                modifier = Modifier.testTag("layer_switch_${layer.id}")
            )
        }
    }
}
