package com.example.combat

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.random.Random

enum class AimAssistMode(val id: String, val displayName: String, val description: String) {
    OFF(
        id = "off",
        displayName = "OFF",
        description = "Manual aim only — no body-to-head assist."
    ),
    SOFT(
        id = "soft",
        displayName = "SOFT (FERAL STYLE)",
        description = "Smooth body-to-head shift, subtle magnetic tracking, easy pull-away, auto-release on kill."
    ),
    FOCUSED(
        id = "focused",
        displayName = "FOCUSED",
        description = "Faster body-to-head snap and stronger head tracking while keeping kill-release."
    );

    companion object {
        fun fromId(id: String?): AimAssistMode =
            entries.find { it.id == id } ?: SOFT
    }
}

data class AimAssistConfig(
    val mode: AimAssistMode = AimAssistMode.SOFT,
    val snapSpeed: Float = 13.5f,          // Smooth exponential glide from body to head
    val trackStiffness: Float = 16.0f,     // Head tracking responsiveness
    val stickyFriction: Float = 0.45f,     // Look sensitivity dampening while locked on head
    val breakawayDistance: Float = 0.085f, // Manual pull distance that breaks lock smoothly
    val releaseCooldownSec: Float = 0.16f  // Inertial decay after kill or breakaway
) {
    fun effectiveSnapSpeed(): Float = when (mode) {
        AimAssistMode.OFF -> 0f
        AimAssistMode.SOFT -> snapSpeed
        AimAssistMode.FOCUSED -> snapSpeed * 1.65f
    }

    fun effectiveTrackStiffness(): Float = when (mode) {
        AimAssistMode.OFF -> 0f
        AimAssistMode.SOFT -> trackStiffness
        AimAssistMode.FOCUSED -> trackStiffness * 1.55f
    }

    fun effectiveFriction(): Float = when (mode) {
        AimAssistMode.OFF -> 1f
        AimAssistMode.SOFT -> stickyFriction
        AimAssistMode.FOCUSED -> 0.30f
    }

    fun effectiveBreakaway(): Float = when (mode) {
        AimAssistMode.OFF -> 0f
        AimAssistMode.SOFT -> breakawayDistance
        AimAssistMode.FOCUSED -> breakawayDistance * 1.45f
    }
}

enum class AimLockStage {
    NONE,
    SHIFTING_TO_HEAD,
    HEAD_LOCKED,
    RELEASED_ON_KILL
}

data class TacticalEnemy(
    val id: Int,
    val callsign: String,
    val normX: Float,       // Center X in [0..1]
    val normY: Float,       // Center Y (torso center) in [0..1]
    val vx: Float,          // Normalized units/sec
    val vy: Float,
    val scale: Float = 1f,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val isAlive: Boolean = true,
    val deathTimerSec: Float = 0f,
    val killedByHeadshot: Boolean = false
) {
    // Head sits above torso center; proportional to character scale
    val headX: Float get() = normX
    val headY: Float get() = normY - 0.085f * scale
    val headRadius: Float get() = 0.028f * scale

    // Body hitbox rectangle (covers chest, shoulders, upper legs)
    val bodyHalfWidth: Float get() = 0.044f * scale
    val bodyTopY: Float get() = normY - 0.060f * scale
    val bodyBottomY: Float get() = normY + 0.095f * scale

    // Outer soft-assist detection padding around body & head
    val assistPadX: Float get() = 0.024f * scale
    val assistPadY: Float get() = 0.024f * scale

    fun containsInBodyOrAssistCone(rx: Float, ry: Float): Boolean {
        if (!isAlive) return false
        val inBody = rx in (normX - bodyHalfWidth - assistPadX)..(normX + bodyHalfWidth + assistPadX) &&
            ry in (headY - headRadius - assistPadY)..(bodyBottomY + assistPadY)
        return inBody
    }

    fun isReticleOnHead(rx: Float, ry: Float, extraMagnetism: Float = 0f): Boolean {
        if (!isAlive) return false
        return hypot(rx - headX, ry - headY) <= (headRadius + extraMagnetism)
    }

    fun isReticleOnBody(rx: Float, ry: Float): Boolean {
        if (!isAlive) return false
        return rx in (normX - bodyHalfWidth)..(normX + bodyHalfWidth) &&
            ry in bodyTopY..bodyBottomY
    }
}

data class AimAssistRuntimeState(
    val reticleX: Float = 0.5f,
    val reticleY: Float = 0.5f,
    val lockedEnemyId: Int? = null,
    val lockStage: AimLockStage = AimLockStage.NONE,
    val releaseCooldownRemaining: Float = 0f,
    val breakawayAccumulated: Float = 0f,
    val frictionBlend: Float = 1f, // Smoothly ramps back to 1.0 on death to prevent camera jerk
    val lastShotLabel: String? = null,
    val headshotsCount: Int = 0,
    val eliminationsCount: Int = 0
)

class AimAssistEngine {

    fun spawnInitialSquad(): List<TacticalEnemy> = listOf(
        TacticalEnemy(id = 1, callsign = "PATROL-01", normX = 0.28f, normY = 0.52f, vx = 0.065f, vy = 0.008f, scale = 0.95f),
        TacticalEnemy(id = 2, callsign = "SENTRY-02", normX = 0.68f, normY = 0.49f, vx = -0.055f, vy = 0.006f, scale = 1.05f),
        TacticalEnemy(id = 3, callsign = "GUARD-03", normX = 0.50f, normY = 0.46f, vx = 0.045f, vy = -0.005f, scale = 0.85f)
    )

    fun stepEnemies(enemies: List<TacticalEnemy>, dt: Float): List<TacticalEnemy> {
        var nextIdSeed = (enemies.maxOfOrNull { it.id } ?: 0) + 1
        return enemies.map { enemy ->
            if (!enemy.isAlive) {
                val remaining = enemy.deathTimerSec - dt
                if (remaining <= 0f) {
                    val spawnX = if (Random.nextBoolean()) 0.22f else 0.78f
                    val speed = (0.045f + Random.nextFloat() * 0.04f) * if (spawnX < 0.5f) 1f else -1f
                    TacticalEnemy(
                        id = nextIdSeed++,
                        callsign = "HOSTILE-${nextIdSeed.toString().padStart(2, '0')}",
                        normX = spawnX,
                        normY = 0.44f + Random.nextFloat() * 0.14f,
                        vx = speed,
                        vy = (Random.nextFloat() - 0.5f) * 0.015f,
                        scale = 0.85f + Random.nextFloat() * 0.25f
                    )
                } else {
                    enemy.copy(deathTimerSec = remaining)
                }
            } else {
                var nx = enemy.normX + enemy.vx * dt
                var ny = enemy.normY + enemy.vy * dt
                var nvx = enemy.vx
                var nvy = enemy.vy
                if (nx < 0.15f) {
                    nx = 0.15f
                    nvx = -nvx
                } else if (nx > 0.85f) {
                    nx = 0.85f
                    nvx = -nvx
                }
                if (ny < 0.38f) {
                    ny = 0.38f
                    nvy = -nvy
                } else if (ny > 0.62f) {
                    ny = 0.62f
                    nvy = -nvy
                }
                enemy.copy(normX = nx, normY = ny, vx = nvx, vy = nvy)
            }
        }
    }

    /**
     * Core Feral-style Soft Auto-Aim adapted for Headshot Priority:
     * 1. When reticle touches/enters an alive enemy's body hitbox, acquires target & shifts smoothly onto head.
     * 2. Uses damped spring tracking + sticky friction while locked, rather than rigid teleporting.
     * 3. Breaks lock smoothly if user pulls strongly away (breakawayThreshold).
     * 4. Immediately drops target on death and decays friction back to 1.0 over releaseCooldownSec.
     */
    fun stepAimAssist(
        state: AimAssistRuntimeState,
        enemies: List<TacticalEnemy>,
        manualDeltaX: Float,
        manualDeltaY: Float,
        config: AimAssistConfig,
        dt: Float
    ): AimAssistRuntimeState {
        val safeDt = dt.coerceIn(0.001f, 0.05f)
        var cooldown = (state.releaseCooldownRemaining - safeDt).coerceAtLeast(0f)

        if (config.mode == AimAssistMode.OFF) {
            val rx = (state.reticleX + manualDeltaX).coerceIn(0.05f, 0.95f)
            val ry = (state.reticleY + manualDeltaY).coerceIn(0.10f, 0.85f)
            return state.copy(
                reticleX = rx,
                reticleY = ry,
                lockedEnemyId = null,
                lockStage = AimLockStage.NONE,
                releaseCooldownRemaining = 0f,
                breakawayAccumulated = 0f,
                frictionBlend = 1f
            )
        }

        // 1. Validate currently locked enemy (Release on Death rule)
        var activeTarget = state.lockedEnemyId?.let { id -> enemies.find { it.id == id } }
        if (state.lockedEnemyId != null && (activeTarget == null || !activeTarget.isAlive)) {
            // Enemy just died -> immediately drop lock and start smooth inertial decay
            val rx = (state.reticleX + manualDeltaX * state.frictionBlend).coerceIn(0.05f, 0.95f)
            val ry = (state.reticleY + manualDeltaY * state.frictionBlend).coerceIn(0.10f, 0.85f)
            return state.copy(
                reticleX = rx,
                reticleY = ry,
                lockedEnemyId = null,
                lockStage = AimLockStage.RELEASED_ON_KILL,
                releaseCooldownRemaining = config.releaseCooldownSec,
                breakawayAccumulated = 0f
            )
        }

        // 2. Smoothly ramp friction back to 1.0 when unlocked
        val targetFriction = if (activeTarget != null) config.effectiveFriction() else 1f
        val frictionAlpha = 1f - exp(-18f * safeDt)
        val newFriction = state.frictionBlend + (targetFriction - state.frictionBlend) * frictionAlpha

        var rx = (state.reticleX + manualDeltaX * newFriction).coerceIn(0.05f, 0.95f)
        var ry = (state.reticleY + manualDeltaY * newFriction).coerceIn(0.10f, 0.85f)

        // 3. Acquire target when reticle touches body hitbox (after cooldown finishes)
        if (activeTarget == null && cooldown <= 0f) {
            val candidate = enemies
                .filter { it.containsInBodyOrAssistCone(rx, ry) }
                .minByOrNull { hypot(rx - it.normX, ry - it.normY) }
            if (candidate != null) {
                activeTarget = candidate
            }
        }

        if (activeTarget == null) {
            return state.copy(
                reticleX = rx,
                reticleY = ry,
                lockedEnemyId = null,
                lockStage = if (cooldown > 0f && state.lockStage == AimLockStage.RELEASED_ON_KILL) {
                    AimLockStage.RELEASED_ON_KILL
                } else {
                    AimLockStage.NONE
                },
                releaseCooldownRemaining = cooldown,
                breakawayAccumulated = 0f,
                frictionBlend = newFriction
            )
        }

        // 4. Breakaway check: if player intentionally fights the magnet strongly, release smoothly
        val manualPullMag = hypot(manualDeltaX, manualDeltaY)
        val newBreakaway = if (manualPullMag > 0.002f) {
            state.breakawayAccumulated + manualPullMag
        } else {
            (state.breakawayAccumulated - safeDt * 0.25f).coerceAtLeast(0f)
        }

        val distFromHead = hypot(rx - activeTarget.headX, ry - activeTarget.headY)
        if (newBreakaway > config.effectiveBreakaway() && distFromHead > activeTarget.headRadius * 1.4f) {
            return state.copy(
                reticleX = rx,
                reticleY = ry,
                lockedEnemyId = null,
                lockStage = AimLockStage.NONE,
                releaseCooldownRemaining = config.releaseCooldownSec * 1.4f,
                breakawayAccumulated = 0f,
                frictionBlend = newFriction
            )
        }

        // 5. Smooth Body-to-Head shift & damped velocity tracking
        val dist = hypot(activeTarget.headX - rx, activeTarget.headY - ry)
        val isHeadLocked = dist <= activeTarget.headRadius * 0.95f
        val rate = if (isHeadLocked) config.effectiveTrackStiffness() else config.effectiveSnapSpeed()
        val alpha = 1f - exp(-rate * safeDt)

        // Blend reticle smoothly toward moving head position
        rx = (rx + (activeTarget.headX - rx) * alpha).coerceIn(0.05f, 0.95f)
        ry = (ry + (activeTarget.headY - ry) * alpha).coerceIn(0.10f, 0.85f)

        return state.copy(
            reticleX = rx,
            reticleY = ry,
            lockedEnemyId = activeTarget.id,
            lockStage = if (isHeadLocked) AimLockStage.HEAD_LOCKED else AimLockStage.SHIFTING_TO_HEAD,
            releaseCooldownRemaining = 0f,
            breakawayAccumulated = newBreakaway,
            frictionBlend = newFriction
        )
    }

    data class ShotOutcome(
        val updatedEnemies: List<TacticalEnemy>,
        val updatedState: AimAssistRuntimeState,
        val wasHeadshot: Boolean,
        val wasKill: Boolean
    )

    fun evaluateShot(
        state: AimAssistRuntimeState,
        enemies: List<TacticalEnemy>,
        config: AimAssistConfig
    ): ShotOutcome {
        val magnetismRadius = when (config.mode) {
            AimAssistMode.OFF -> 0f
            AimAssistMode.SOFT -> 0.008f
            AimAssistMode.FOCUSED -> 0.014f
        }

        // Check head hits first (with small Feral-style bullet magnetism)
        val headHit = enemies.firstOrNull {
            it.isReticleOnHead(state.reticleX, state.reticleY, magnetismRadius)
        }
        if (headHit != null) {
            val updated = enemies.map {
                if (it.id == headHit.id) {
                    it.copy(hp = 0, isAlive = false, deathTimerSec = 1.6f, killedByHeadshot = true)
                } else it
            }
            return ShotOutcome(
                updatedEnemies = updated,
                updatedState = state.copy(
                    lockedEnemyId = null, // Immediately release target on death!
                    lockStage = AimLockStage.RELEASED_ON_KILL,
                    releaseCooldownRemaining = config.releaseCooldownSec,
                    breakawayAccumulated = 0f,
                    lastShotLabel = "HEADSHOT KILL · ${headHit.callsign}",
                    headshotsCount = state.headshotsCount + 1,
                    eliminationsCount = state.eliminationsCount + 1
                ),
                wasHeadshot = true,
                wasKill = true
            )
        }

        // Otherwise check body hits
        val bodyHit = enemies.firstOrNull { it.isReticleOnBody(state.reticleX, state.reticleY) }
        if (bodyHit != null) {
            val newHp = (bodyHit.hp - 35).coerceAtLeast(0)
            val killed = newHp == 0
            val updated = enemies.map {
                if (it.id == bodyHit.id) {
                    it.copy(
                        hp = newHp,
                        isAlive = !killed,
                        deathTimerSec = if (killed) 1.6f else 0f,
                        killedByHeadshot = false
                    )
                } else it
            }
            return ShotOutcome(
                updatedEnemies = updated,
                updatedState = state.copy(
                    lockedEnemyId = if (killed) null else state.lockedEnemyId,
                    lockStage = if (killed) AimLockStage.RELEASED_ON_KILL else state.lockStage,
                    releaseCooldownRemaining = if (killed) config.releaseCooldownSec else state.releaseCooldownRemaining,
                    lastShotLabel = if (killed) "ELIMINATED · ${bodyHit.callsign}" else "BODY HIT (-35 HP)",
                    eliminationsCount = state.eliminationsCount + if (killed) 1 else 0
                ),
                wasHeadshot = false,
                wasKill = killed
            )
        }

        return ShotOutcome(
            updatedEnemies = enemies,
            updatedState = state.copy(lastShotLabel = null),
            wasHeadshot = false,
            wasKill = false
        )
    }
}
