package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.combat.AimAssistEngine
import com.example.combat.AimAssistMode
import com.example.combat.AimAssistRuntimeState
import com.example.combat.AimLockStage
import com.example.model.GamepadSnapshot
import com.example.model.GyroSnapshot
import com.example.model.MissionInfo
import com.example.ui.theme.IgiAmberAccent
import com.example.ui.theme.IgiBackground
import com.example.ui.theme.IgiButtonBg
import com.example.ui.theme.IgiCardSurface
import com.example.ui.theme.IgiCardSurfaceSolid
import com.example.ui.theme.IgiDangerRed
import com.example.ui.theme.IgiDisabledBorder
import com.example.ui.theme.IgiPhosphorGreen
import com.example.ui.theme.IgiSageText
import com.example.ui.theme.IgiStatusGreen
import com.example.viewmodel.IgiUiState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MissionHudScreen(
    missionNumber: Int,
    mission: MissionInfo?,
    missionBitmap: Bitmap?,
    uiState: IgiUiState,
    gyroSnapshot: GyroSnapshot,
    gamepadSnapshot: GamepadSnapshot,
    onSyncAudio: (isSceneActive: Boolean, isCutsceneOrPaused: Boolean) -> Unit,
    onSelectAimAssistMode: (AimAssistMode) -> Unit,
    onExitMission: () -> Unit
) {
    val aimEngine = remember { AimAssistEngine() }

    var isPaused by remember { mutableStateOf(false) }
    var isCutscene by remember { mutableStateOf(false) }
    var isHoldingSkip by remember { mutableStateOf(false) }
    var skipProgress by remember { mutableFloatStateOf(0f) }

    var isCrouched by remember { mutableStateOf(false) }
    var isScoped by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableIntStateOf(3) } // MP5SD3 default
    var frameCounter by remember { mutableIntStateOf(0) }

    var moveStickX by remember { mutableFloatStateOf(0f) }
    var moveStickY by remember { mutableFloatStateOf(0f) }

    // Accumulated manual look input deltas (touch look stick / screen drag / gamepad / gyro)
    var pendingLookDx by remember { mutableFloatStateOf(0f) }
    var pendingLookDy by remember { mutableFloatStateOf(0f) }
    var lookStickX by remember { mutableFloatStateOf(0f) }
    var lookStickY by remember { mutableFloatStateOf(0f) }
    var lastGyroYaw by remember { mutableFloatStateOf(gyroSnapshot.yawRadians) }
    var lastGyroPitch by remember { mutableFloatStateOf(gyroSnapshot.pitchRadians) }

    var enemies by remember { mutableStateOf(aimEngine.spawnInitialSquad()) }
    var aimState by remember { mutableStateOf(AimAssistRuntimeState()) }
    var shotFlashTimer by remember { mutableFloatStateOf(0f) }

    val availableWeapons = remember(uiState.weapons, uiState.cheats.allWeapons) {
        uiState.weapons.filter { it.defaultUnlocked || uiState.cheats.allWeapons }
    }
    val activeWeapon = availableWeapons.find { it.slot == selectedSlot }
        ?: availableWeapons.firstOrNull()

    var currentMag by remember(activeWeapon?.slot) {
        mutableIntStateOf(activeWeapon?.magSize ?: 30)
    }
    var currentReserve by remember(activeWeapon?.slot) {
        mutableIntStateOf(activeWeapon?.reserveMax ?: 150)
    }

    // Sync rain ambience (mutes during pause & cutscenes per PATCH_NOTES)
    LaunchedEffect(isPaused, isCutscene, uiState.rainAudioEnabled) {
        onSyncAudio(true, isPaused || isCutscene)
    }
    DisposableEffect(Unit) {
        onDispose { onSyncAudio(false, true) }
    }

    val currentGyro by rememberUpdatedState(gyroSnapshot)
    val currentGamepad by rememberUpdatedState(gamepadSnapshot)

    // Frame pacer loop (30/60 FPS): steps patrolling enemies & Feral-style Headshot Aim Assist
    LaunchedEffect(isPaused, isCutscene, uiState.targetFps, uiState.aimAssistConfig) {
        val frameDelayMs = if (uiState.targetFps >= 60) 16L else 33L
        val dt = frameDelayMs / 1000f
        while (!isPaused && !isCutscene) {
            frameCounter++
            if (shotFlashTimer > 0f) {
                shotFlashTimer = (shotFlashTimer - dt).coerceAtLeast(0f)
            }

            // Collect manual look deltas from touch stick, viewport drag, gamepad, and gyro
            val gyroDx = if (currentGyro.enabled) (currentGyro.yawRadians - lastGyroYaw) * 0.35f else 0f
            val gyroDy = if (currentGyro.enabled) (currentGyro.pitchRadians - lastGyroPitch) * 0.35f else 0f
            lastGyroYaw = currentGyro.yawRadians
            lastGyroPitch = currentGyro.pitchRadians

            val padDx = if (currentGamepad.connected) currentGamepad.rightX * dt * 0.55f else 0f
            val padDy = if (currentGamepad.connected) currentGamepad.rightY * dt * 0.45f else 0f
            val stickDx = lookStickX * dt * 0.50f
            val stickDy = lookStickY * dt * 0.40f

            val totalDx = pendingLookDx + gyroDx + padDx + stickDx
            val totalDy = pendingLookDy + gyroDy + padDy + stickDy
            pendingLookDx = 0f
            pendingLookDy = 0f

            val nextEnemies = aimEngine.stepEnemies(enemies, dt)
            val nextAim = aimEngine.stepAimAssist(
                state = aimState,
                enemies = nextEnemies,
                manualDeltaX = totalDx,
                manualDeltaY = totalDy,
                config = uiState.aimAssistConfig,
                dt = dt
            )
            enemies = nextEnemies
            aimState = nextAim
            delay(frameDelayMs)
        }
    }

    // Cutscene hold-to-skip progress ring
    LaunchedEffect(isCutscene, isHoldingSkip) {
        if (!isCutscene) {
            skipProgress = 0f
            return@LaunchedEffect
        }
        while (isCutscene) {
            if (isHoldingSkip) {
                skipProgress = (skipProgress + 0.04f).coerceAtMost(1f)
                if (skipProgress >= 1f) {
                    isCutscene = false
                    isHoldingSkip = false
                    skipProgress = 0f
                }
            } else if (skipProgress > 0f) {
                skipProgress = (skipProgress - 0.06f).coerceAtLeast(0f)
            }
            delay(33L)
        }
    }

    BackHandler {
        isPaused = !isPaused
    }

    val sceneBitmap = remember(missionBitmap) { missionBitmap?.asImageBitmap() }
    val bgParallaxX = (0.5f - aimState.reticleX) * 60f
    val bgParallaxY = (0.5f - aimState.reticleY) * 35f

    fun fireShot() {
        if (isPaused || isCutscene) return
        if (!uiState.cheats.infiniteAmmo && currentMag <= 0) return
        if (!uiState.cheats.infiniteAmmo) {
            currentMag--
        }
        val outcome = aimEngine.evaluateShot(aimState, enemies, uiState.aimAssistConfig)
        enemies = outcome.updatedEnemies
        aimState = outcome.updatedState
        if (outcome.wasHeadshot || outcome.wasKill) {
            shotFlashTimer = 0.45f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IgiBackground)
            // Allow dragging anywhere on the tactical viewport to aim smoothly
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    pendingLookDx += dragAmount.x / size.width.coerceAtLeast(1)
                    pendingLookDy += dragAmount.y / size.height.coerceAtLeast(1)
                }
            }
    ) {
        // Scene Backdrop with reticle-linked parallax & scope zoom
        if (sceneBitmap != null) {
            Image(
                bitmap = sceneBitmap,
                contentDescription = "Mission $missionNumber tactical view",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val zoom = if (isScoped) 1.55f else 1.12f
                        scaleX = zoom
                        scaleY = zoom
                        translationX = bgParallaxX
                        translationY = bgParallaxY
                    }
            )
        }

        // Patrolling Enemy Silhouettes (Body + Head Hitboxes) & Dynamic Auto-Headshot Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            enemies.forEach { enemy ->
                val cx = enemy.normX * w
                val cy = enemy.normY * h
                val hx = enemy.headX * w
                val hy = enemy.headY * h
                val headR = enemy.headRadius * minOf(w, h)
                val bodyHalfW = enemy.bodyHalfWidth * w
                val bodyTop = enemy.bodyTopY * h
                val bodyBottom = enemy.bodyBottomY * h
                val isLocked = enemy.id == aimState.lockedEnemyId

                if (!enemy.isAlive) {
                    // Eliminated corpse fade-out marker (lock already dropped!)
                    val fadeAlpha = (enemy.deathTimerSec / 1.6f).coerceIn(0f, 1f)
                    drawCircle(
                        color = IgiDangerRed.copy(alpha = 0.35f * fadeAlpha),
                        radius = headR * 1.1f,
                        center = Offset(hx, hy)
                    )
                    return@forEach
                }

                // Body silhouette
                val bodyColor = if (isLocked) Color(0xCC1E3A24) else Color(0xB3151E17)
                val outlineColor = if (isLocked) IgiPhosphorGreen else IgiStatusGreen.copy(alpha = 0.8f)

                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(cx - bodyHalfW, bodyTop),
                    size = Size(bodyHalfW * 2f, bodyBottom - bodyTop),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(cx - bodyHalfW, bodyTop),
                    size = Size(bodyHalfW * 2f, bodyBottom - bodyTop),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = if (isLocked) 2.dp.toPx() else 1.dp.toPx())
                )

                // Head hitbox silhouette
                val headColor = when {
                    isLocked && aimState.lockStage == AimLockStage.HEAD_LOCKED -> IgiAmberAccent.copy(alpha = 0.35f)
                    isLocked -> IgiPhosphorGreen.copy(alpha = 0.35f)
                    else -> Color(0xCC243127)
                }
                val headStroke = when {
                    isLocked && aimState.lockStage == AimLockStage.HEAD_LOCKED -> IgiAmberAccent
                    isLocked -> IgiPhosphorGreen
                    else -> IgiSageText.copy(alpha = 0.85f)
                }
                drawCircle(
                    color = headColor,
                    radius = headR,
                    center = Offset(hx, hy)
                )
                drawCircle(
                    color = headStroke,
                    radius = headR,
                    center = Offset(hx, hy),
                    style = Stroke(width = if (isLocked) 2.2.dp.toPx() else 1.2.dp.toPx())
                )

                // Subtle HP bar above head
                val hpFrac = (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
                val barW = bodyHalfW * 1.7f
                val barTop = hy - headR - 10.dp.toPx()
                drawRect(
                    color = Color(0xAA0A0C08),
                    topLeft = Offset(cx - barW / 2f, barTop),
                    size = Size(barW, 4.dp.toPx())
                )
                drawRect(
                    color = if (hpFrac > 0.5f) IgiPhosphorGreen else IgiAmberAccent,
                    topLeft = Offset(cx - barW / 2f, barTop),
                    size = Size(barW * hpFrac, 4.dp.toPx())
                )

                // Head-lock magnetic brackets when auto-headshot assist snaps onto head
                if (isLocked) {
                    val br = headR * 1.65f
                    drawCircle(
                        color = headStroke.copy(alpha = 0.65f),
                        radius = br,
                        center = Offset(hx, hy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Dynamic Aiming Reticle (moves smoothly with manual aim + body-to-head assist)
            val rx = aimState.reticleX * w
            val ry = aimState.reticleY * h
            val reticleColor = when (aimState.lockStage) {
                AimLockStage.HEAD_LOCKED -> IgiAmberAccent
                AimLockStage.SHIFTING_TO_HEAD -> IgiPhosphorGreen
                AimLockStage.RELEASED_ON_KILL -> IgiDangerRed
                AimLockStage.NONE -> IgiPhosphorGreen
            }
            val lineLen = if (isScoped) 34.dp.toPx() else 15.dp.toPx()
            val gap = if (aimState.lockStage == AimLockStage.HEAD_LOCKED) 4.dp.toPx() else 7.dp.toPx()

            if (isScoped) {
                drawCircle(
                    color = reticleColor.copy(alpha = 0.75f),
                    radius = minOf(w, h) * 0.28f,
                    center = Offset(rx, ry),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            drawCircle(
                color = reticleColor,
                radius = 2.5.dp.toPx(),
                center = Offset(rx, ry)
            )
            drawLine(reticleColor, Offset(rx - lineLen, ry), Offset(rx - gap, ry), strokeWidth = 2.dp.toPx())
            drawLine(reticleColor, Offset(rx + gap, ry), Offset(rx + lineLen, ry), strokeWidth = 2.dp.toPx())
            drawLine(reticleColor, Offset(rx, ry - lineLen), Offset(rx, ry - gap), strokeWidth = 2.dp.toPx())
            drawLine(reticleColor, Offset(rx, ry + gap), Offset(rx, ry + lineLen), strokeWidth = 2.dp.toPx())

            // Kill / Headshot hitmarker X flash
            if (shotFlashTimer > 0f) {
                val hm = 10.dp.toPx()
                val hmGap = 4.dp.toPx()
                drawLine(IgiAmberAccent, Offset(rx - hm, ry - hm), Offset(rx - hmGap, ry - hmGap), strokeWidth = 2.2.dp.toPx())
                drawLine(IgiAmberAccent, Offset(rx + hmGap, ry - hmGap), Offset(rx + hm, ry - hm), strokeWidth = 2.2.dp.toPx())
                drawLine(IgiAmberAccent, Offset(rx - hm, ry + hm), Offset(rx - hmGap, ry + hmGap), strokeWidth = 2.2.dp.toPx())
                drawLine(IgiAmberAccent, Offset(rx + hmGap, ry + hmGap), Offset(rx + hm, ry + hm), strokeWidth = 2.2.dp.toPx())
            }
        }

        // HUD Chrome
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .padding(12.dp)
        ) {
            // Top Bar: Pause (||), Mission & Assist Readout, Health & Kill Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.5.dp, IgiPhosphorGreen),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { isPaused = !isPaused }
                        .testTag("hud_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPaused) "Resume mission" else "Pause mission",
                            tint = IgiPhosphorGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, IgiPhosphorGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "M%02d // %s",
                                missionNumber,
                                (mission?.title ?: "TACTICAL OP").uppercase(Locale.US)
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(color = IgiPhosphorGreen),
                            maxLines = 1
                        )
                        val stageLabel = when (aimState.lockStage) {
                            AimLockStage.HEAD_LOCKED -> "HEAD LOCK ACTIVE"
                            AimLockStage.SHIFTING_TO_HEAD -> "BODY->HEAD SHIFT"
                            AimLockStage.RELEASED_ON_KILL -> "TARGET RELEASED"
                            AimLockStage.NONE -> "SCANNING"
                        }
                        Text(
                            text = "HS: ${aimState.headshotsCount}/${aimState.eliminationsCount} · $stageLabel · ${uiState.targetFps} FPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (aimState.lockStage == AimLockStage.HEAD_LOCKED) IgiAmberAccent else IgiStatusGreen
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Aim-Assist Mode Pill (cycles SOFT -> FOCUSED -> OFF)
                val assistMode = uiState.aimAssistConfig.mode
                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (assistMode == AimAssistMode.OFF) IgiDisabledBorder else IgiPhosphorGreen
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .clickable {
                            val next = when (assistMode) {
                                AimAssistMode.SOFT -> AimAssistMode.FOCUSED
                                AimAssistMode.FOCUSED -> AimAssistMode.OFF
                                AimAssistMode.OFF -> AimAssistMode.SOFT
                            }
                            onSelectAimAssistMode(next)
                        }
                        .testTag("hud_aim_assist_cycle_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text("HEAD ASSIST", style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText, fontSize = 9.sp))
                        Text(
                            text = if (assistMode == AimAssistMode.SOFT) "SOFT" else assistMode.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (assistMode == AimAssistMode.OFF) IgiSageText else IgiPhosphorGreen,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Health Badge
                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (uiState.cheats.infiniteHealth) IgiAmberAccent else IgiPhosphorGreen)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("HEALTH", style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText))
                        Text(
                            text = if (uiState.cheats.infiniteHealth) "∞ INVINCIBLE" else "100%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (uiState.cheats.infiniteHealth) IgiAmberAccent else IgiPhosphorGreen
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weapon Slots 1-9 + Ammo Readout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    uiState.weapons.forEach { weapon ->
                        val unlocked = weapon.defaultUnlocked || uiState.cheats.allWeapons
                        val active = weapon.slot == (activeWeapon?.slot ?: -1)
                        Surface(
                            color = if (active) IgiPhosphorGreen else IgiCardSurface,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    active -> IgiPhosphorGreen
                                    unlocked -> IgiStatusGreen
                                    else -> IgiDisabledBorder
                                }
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .clickable(enabled = unlocked) { selectedSlot = weapon.slot }
                                .testTag("weapon_slot_${weapon.slot}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = "${weapon.slot}: ${weapon.name.uppercase(Locale.US)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when {
                                            active -> Color.Black
                                            unlocked -> Color.White
                                            else -> IgiDisabledBorder
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, IgiPhosphorGreen.copy(alpha = 0.7f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeWeapon?.name?.uppercase(Locale.US) ?: "UNARMED",
                            style = MaterialTheme.typography.labelSmall.copy(color = IgiSageText)
                        )
                        Text(
                            text = if (uiState.cheats.infiniteAmmo) "∞ / ∞" else "$currentMag / $currentReserve",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (uiState.cheats.infiniteAmmo) IgiAmberAccent else IgiPhosphorGreen
                            )
                        )
                    }
                }
            }

            // Shot / Headshot feedback banner
            val shotText = aimState.lastShotLabel
            if (!shotText.isNullOrBlank() && shotFlashTimer > 0f) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, IgiAmberAccent),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = shotText,
                        style = MaterialTheme.typography.labelLarge.copy(color = IgiAmberAccent),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isCutscene) {
                // Mid-mission cutscene hold-to-skip ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Surface(
                        color = IgiCardSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, IgiAmberAccent),
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHoldingSkip = true
                                        tryAwaitRelease()
                                        isHoldingSkip = false
                                    }
                                )
                            }
                            .testTag("cutscene_skip_ring")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = IgiDisabledBorder,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = IgiPhosphorGreen,
                                        startAngle = -90f,
                                        sweepAngle = 360f * skipProgress,
                                        useCenter = false,
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CUTSCENE PLAYBACK (MUSIC MUTED)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = IgiAmberAccent)
                                )
                                Text(
                                    text = "HOLD TO SKIP CUTSCENE",
                                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White)
                                )
                            }
                        }
                    }
                }
            } else if (gamepadSnapshot.connected) {
                Surface(
                    color = IgiCardSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, IgiPhosphorGreen),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "CONTROLLER ACTIVE (${gamepadSnapshot.deviceName}) · TOUCH HUD HIDDEN",
                        style = MaterialTheme.typography.labelSmall.copy(color = IgiPhosphorGreen),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            } else {
                // Multi-touch dual stick HUD & action controls
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Move Stick
                    VirtualTouchStick(
                        label = "MOVE",
                        stickX = moveStickX,
                        stickY = moveStickY,
                        onStickChanged = { nx, ny ->
                            moveStickX = nx
                            moveStickY = ny
                        }
                    )

                    // Center stance & utility buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudActionChip(
                            label = if (isCrouched) "CROUCHED" else "CROUCH",
                            active = isCrouched,
                            onClick = { isCrouched = !isCrouched },
                            testTag = "hud_crouch_button"
                        )
                        HudActionChip(
                            label = if (isScoped) "SCOPE ON" else "SCOPE",
                            active = isScoped,
                            onClick = { isScoped = !isScoped },
                            testTag = "hud_scope_button"
                        )
                        HudActionChip(
                            label = "RELOAD",
                            active = false,
                            onClick = {
                                val magMax = activeWeapon?.magSize ?: 30
                                val needed = magMax - currentMag
                                if (uiState.cheats.infiniteAmmo) {
                                    currentMag = magMax
                                } else if (needed > 0 && currentReserve > 0) {
                                    val load = minOf(needed, currentReserve)
                                    currentMag += load
                                    currentReserve -= load
                                }
                            },
                            testTag = "hud_reload_button"
                        )
                    }

                    // Right Look Stick + Fire
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = ::fireShot,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IgiAmberAccent,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(62.dp)
                                .testTag("hud_fire_button")
                        ) {
                            Text("FIRE", style = MaterialTheme.typography.labelLarge)
                        }

                        VirtualTouchStick(
                            label = "AIM",
                            stickX = lookStickX,
                            stickY = lookStickY,
                            onStickChanged = { nx, ny ->
                                lookStickX = nx
                                lookStickY = ny
                            }
                        )
                    }
                }
            }
        }

        // Pause Menu Overlay
        if (isPaused) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB0A0C08))
                    .padding(24.dp)
            ) {
                Surface(
                    color = IgiCardSurfaceSolid,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, IgiPhosphorGreen),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "SIMULATION PAUSED",
                            style = MaterialTheme.typography.titleLarge.copy(color = IgiPhosphorGreen)
                        )
                        Text(
                            text = "Aim Assist: ${uiState.aimAssistConfig.mode.displayName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = IgiSageText)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { isPaused = false },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IgiPhosphorGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pause_resume_button")
                        ) {
                            Text("RESUME MISSION", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                isCutscene = !isCutscene
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, IgiPhosphorGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pause_toggle_cutscene_button")
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, tint = IgiPhosphorGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCutscene) "END CUTSCENE" else "TEST MID-MISSION CUTSCENE",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onExitMission,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, IgiAmberAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IgiAmberAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pause_exit_button")
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ABORT TO CAMPAIGN MENU", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualTouchStick(
    label: String,
    stickX: Float,
    stickY: Float,
    onStickChanged: (Float, Float) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(IgiCardSurface)
            .pointerInput(Unit) {
                var accX = 0f
                var accY = 0f
                detectDragGestures(
                    onDragStart = {
                        accX = 0f
                        accY = 0f
                    },
                    onDragEnd = {
                        accX = 0f
                        accY = 0f
                        onStickChanged(0f, 0f)
                    },
                    onDragCancel = {
                        accX = 0f
                        accY = 0f
                        onStickChanged(0f, 0f)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    accX = (accX + dragAmount.x / (size.width * 0.35f)).coerceIn(-1f, 1f)
                    accY = (accY + dragAmount.y / (size.height * 0.35f)).coerceIn(-1f, 1f)
                    onStickChanged(accX, accY)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = IgiPhosphorGreen.copy(alpha = 0.65f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val knobOffset = Offset(
                x = size.width / 2f + stickX * (size.width * 0.28f),
                y = size.height / 2f + stickY * (size.height * 0.28f)
            )
            drawCircle(
                color = IgiPhosphorGreen.copy(alpha = 0.85f),
                radius = 16.dp.toPx(),
                center = knobOffset
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontSize = 9.sp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun HudActionChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (active) IgiPhosphorGreen else IgiButtonBg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, IgiPhosphorGreen),
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (active) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
