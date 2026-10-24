package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.audio.GameSoundEngine
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

/**
 * High-performance battle royale physics & combat engine.
 * Handles vehicle dynamics (drifting, nitro, jumps, roadkill),
 * ballistics, bot AI, zone shrinking, and particle effects.
 */
class GamePhysicsEngine(
    private val soundEngine: GameSoundEngine,
    val onKillFeed: (KillFeedEntry) -> Unit
) {
    // Active simulation collections
    val vehicles = mutableListOf<VehicleInstance>()
    val bots = mutableListOf<BotEntity>()
    val bullets = mutableListOf<Bullet>()
    val particles = mutableListOf<Particle>()
    val lootDrops = mutableListOf<LootDrop>()
    var airdrop: Airdrop? = null
    val zone = CircleZone()

    var airdropPlaneX = -200f
    var airdropPlaneY = 2000f
    var airdropPlaneActive = false
    val grenades = mutableListOf<GrenadeEntity>()
    val footstepPings = mutableListOf<FootstepPing>()
    val redZone = RedZone()

    private var botSpawnTimer = 0f
    private var airdropTimer = 25f // Spawn airdrop after 25s

    fun initializeMatch(mode: GameMode) {
        vehicles.clear()
        vehicles.addAll(GameMap.spawnInitialVehicles())
        lootDrops.clear()
        lootDrops.addAll(GameMap.spawnLoot())
        bots.clear()
        bullets.clear()
        particles.clear()
        grenades.clear()
        footstepPings.clear()
        redZone.active = false
        redZone.timer = 40f
        airdrop = null
        airdropPlaneActive = false

        // Configure bots based on mode
        val botCount = when (mode) {
            GameMode.BATTLE_ROYALE -> 25
            GameMode.VEHICLE_WARFARE -> 12
            GameMode.FIRING_RANGE -> 4
        }

        val botNames = listOf(
            "Falcon_Sniper", "Desert_Fox", "Ghost_Rider", "Shadow_Op", "Alpha_Wolf",
            "Nitro_King", "Road_Warrior", "Viper_77", "Apex_Predator", "Storm_Breaker",
            "Cobra_Strike", "Blitz_Driver", "Maverick_99", "Steel_Titan", "Hawk_Eye",
            "Reaper_Squad", "Delta_Commando", "Zero_Cool", "Turbo_Drifter", "Crimson_Ghost",
            "Omega_Sniper", "Night_Hunter", "Iron_Claw", "Savage_Beast", "Phoenix_Ace"
        )

        for (i in 0 until botCount) {
            val name = botNames.getOrElse(i) { "Bot_$i" }
            val angle = Random.nextFloat() * 360f
            val dist = Random.nextFloat() * 1200f + 300f
            val bx = (2000f + cos(Math.toRadians(angle.toDouble())) * dist).toFloat().coerceIn(400f, 3600f)
            val by = (2000f + sin(Math.toRadians(angle.toDouble())) * dist).toFloat().coerceIn(400f, 3600f)
            val weapon = if (i % 3 == 0) WeaponModel.AKM else WeaponModel.M416
            bots.add(
                BotEntity(
                    id = "bot_$i",
                    name = name,
                    x = bx,
                    y = by,
                    angleDeg = Random.nextFloat() * 360f,
                    health = 100f,
                    weapon = weapon
                )
            )
        }

        // Initialize Circle Zone
        zone.whiteCenterX = 2000f
        zone.whiteCenterY = 2000f
        zone.whiteRadius = if (mode == GameMode.FIRING_RANGE) 3000f else 1500f
        zone.blueCenterX = 2000f
        zone.blueCenterY = 2000f
        zone.blueRadius = if (mode == GameMode.FIRING_RANGE) 3500f else 2200f
        zone.targetRadius = zone.whiteRadius
        zone.phase = 1
        zone.timeRemainingSec = 75
        zone.isShrinking = false
        zone.damagePerSec = 1.8f
    }

    /**
     * Main update tick at dt seconds (typically ~0.016s for 60fps).
     */
    fun update(
        dt: Float,
        player: PlayerState,
        steerInput: Float,
        accelInput: Float,
        brakeInput: Boolean,
        handbrakeInput: Boolean,
        nitroInput: Boolean,
        shootInput: Boolean,
        mode: GameMode
    ) {
        updateZone(dt, player, mode)
        updateRedZone(dt, player, mode)
        updatePlayerVehicle(dt, player, steerInput, accelInput, brakeInput, handbrakeInput, nitroInput)
        updatePlayerOnFoot(dt, player, steerInput, accelInput, shootInput)
        updateBots(dt, player)
        updateBullets(dt, player)
        updateGrenades(dt, player)
        updateFootstepPings(dt)
        updateParticles(dt)
        updateAirdrop(dt, player)
        checkLootPickups(player)
        player.screenShake = (player.screenShake * 0.88f).coerceAtLeast(0f)
    }

    private fun updateZone(dt: Float, player: PlayerState, mode: GameMode) {
        if (mode == GameMode.FIRING_RANGE) return

        zone.timeRemainingSec -= (dt).toInt()
        if (zone.timeRemainingSec <= 0) {
            if (!zone.isShrinking) {
                zone.isShrinking = true
                zone.timeRemainingSec = 40
            } else {
                // Advance phase
                zone.phase++
                zone.isShrinking = false
                zone.timeRemainingSec = 60
                zone.blueRadius = zone.whiteRadius
                zone.whiteRadius = (zone.whiteRadius * 0.6f).coerceAtLeast(180f)
                zone.targetRadius = zone.whiteRadius
                zone.damagePerSec += 1.2f

                // Random shift inside current white zone
                val shiftAngle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val shiftDist = Random.nextFloat() * (zone.blueRadius - zone.whiteRadius) * 0.5f
                zone.whiteCenterX = (zone.whiteCenterX + cos(shiftAngle) * shiftDist).coerceIn(800f, 3200f)
                zone.whiteCenterY = (zone.whiteCenterY + sin(shiftAngle) * shiftDist).coerceIn(800f, 3200f)
            }
        }

        if (zone.isShrinking) {
            val shrinkSpeed = (zone.blueRadius - zone.targetRadius) / 40f
            zone.blueRadius = (zone.blueRadius - shrinkSpeed * dt).coerceAtLeast(zone.targetRadius)
            // Interpolate blue center towards white center
            zone.blueCenterX += (zone.whiteCenterX - zone.blueCenterX) * 0.02f
            zone.blueCenterY += (zone.whiteCenterY - zone.blueCenterY) * 0.02f
        }

        // Zone damage to player if outside
        val distToCenter = hypot(player.x - zone.blueCenterX, player.y - zone.blueCenterY)
        if (distToCenter > zone.blueRadius) {
            player.health = (player.health - zone.damagePerSec * dt).coerceAtLeast(0f)
            // Screen blue electrical sparks
            if (Random.nextFloat() < 0.3f) {
                particles.add(
                    Particle(
                        x = player.x + (Random.nextFloat() - 0.5f) * 60f,
                        y = player.y + (Random.nextFloat() - 0.5f) * 60f,
                        vx = (Random.nextFloat() - 0.5f) * 30f,
                        vy = (Random.nextFloat() - 0.5f) * 30f,
                        life = 0.4f,
                        maxLife = 0.4f,
                        color = Color(0xFF00E5FF),
                        size = 5f
                    )
                )
            }
        }
    }

    private fun updatePlayerVehicle(
        dt: Float,
        player: PlayerState,
        steerInput: Float,
        accelInput: Float,
        brakeInput: Boolean,
        handbrakeInput: Boolean,
        nitroInput: Boolean
    ) {
        val vehId = player.activeVehicleId ?: return
        val vehicle = vehicles.find { it.id == vehId } ?: run {
            player.activeVehicleId = null
            return
        }

        if (vehicle.isDestroyed) {
            player.activeVehicleId = null
            return
        }

        val onRoad = GameMap.isOnRoad(vehicle.x, vehicle.y)
        val friction = if (onRoad) 0.985f else 0.960f
        val maxSpeed = (if (onRoad) vehicle.model.maxSpeedKmh else vehicle.model.maxSpeedKmh * 0.75f) * 0.12f

        // Nitro handling
        val isBoosting = nitroInput && vehicle.nitro > 0f && accelInput > 0f
        vehicle.isBoosting = isBoosting
        if (isBoosting) {
            vehicle.nitro = (vehicle.nitro - 18f * dt).coerceAtLeast(0f)
            soundEngine.playNitro()
            // Nitro exhaust flame particles
            val rad = Math.toRadians((vehicle.angleDeg + 180f).toDouble())
            particles.add(
                Particle(
                    x = vehicle.x + cos(rad).toFloat() * 30f,
                    y = vehicle.y + sin(rad).toFloat() * 30f,
                    vx = cos(rad).toFloat() * 60f + (Random.nextFloat() - 0.5f) * 20f,
                    vy = sin(rad).toFloat() * 60f + (Random.nextFloat() - 0.5f) * 20f,
                    life = 0.25f,
                    maxLife = 0.25f,
                    color = if (Random.nextBoolean()) Color(0xFFFF6B00) else Color(0xFF00E5FF),
                    size = 12f
                )
            )
        } else {
            // Passive slow nitro recovery
            vehicle.nitro = (vehicle.nitro + 4f * dt).coerceAtMost(100f)
        }

        val nitroMultiplier = if (isBoosting) 1.55f else 1.0f

        // Fuel consumption
        if (accelInput > 0f) {
            vehicle.fuel = (vehicle.fuel - 0.8f * dt).coerceAtLeast(0f)
        }
        val hasFuel = vehicle.fuel > 0f

        // Acceleration & Braking
        val accelForce = if (hasFuel) vehicle.model.acceleration * 0.08f * nitroMultiplier else 0f
        if (accelInput > 0f) {
            vehicle.speed += accelInput * accelForce
        } else if (brakeInput) {
            if (vehicle.speed > 0.5f) {
                vehicle.speed -= accelForce * 1.8f // Hard brake
            } else {
                vehicle.speed -= accelForce * 0.6f // Reverse
            }
        } else {
            vehicle.speed *= friction
        }

        val currentMaxSpeed = maxSpeed * nitroMultiplier
        vehicle.speed = vehicle.speed.coerceIn(-currentMaxSpeed * 0.4f, currentMaxSpeed)

        // Steering dynamics with speed scaling
        val speedFactor = (abs(vehicle.speed) / currentMaxSpeed).coerceIn(0.1f, 1f)
        val turnRate = 95f * steerInput * speedFactor * (if (vehicle.speed < 0) -1f else 1f)
        vehicle.angleDeg += turnRate * dt

        // Drifting physics
        val isHandbraking = handbrakeInput && abs(vehicle.speed) > 1.5f
        vehicle.isHandbraking = isHandbraking

        if (isHandbraking || (abs(steerInput) > 0.7f && abs(vehicle.speed) > maxSpeed * 0.55f)) {
            val driftIntensity = vehicle.model.driftRating * (if (isHandbraking) 1.6f else 1.1f)
            vehicle.driftAngle = (vehicle.driftAngle + steerInput * 45f * driftIntensity * dt).coerceIn(-50f, 50f)
            val driftGained = abs(vehicle.speed) * 45f * dt * (if (isBoosting) 2.5f else 1.0f)
            vehicle.driftScore += driftGained
            player.totalDriftScore += driftGained

            soundEngine.playDrift()

            // Tire skid tracks and smoke behind rear wheels
            val backRad = Math.toRadians((vehicle.angleDeg + 180f).toDouble())
            val leftRad = Math.toRadians((vehicle.angleDeg - 90f).toDouble())
            for (side in listOf(-1f, 1f)) {
                val wheelX = vehicle.x + cos(backRad).toFloat() * 22f + cos(leftRad).toFloat() * (side * 14f)
                val wheelY = vehicle.y + sin(backRad).toFloat() * 22f + sin(leftRad).toFloat() * (side * 14f)
                particles.add(
                    Particle(
                        x = wheelX,
                        y = wheelY,
                        vx = (Random.nextFloat() - 0.5f) * 10f,
                        vy = (Random.nextFloat() - 0.5f) * 10f,
                        life = 0.5f,
                        maxLife = 0.5f,
                        color = Color(0x77FFFFFF),
                        size = 14f
                    )
                )
                // Black tire skid mark
                particles.add(
                    Particle(
                        x = wheelX,
                        y = wheelY,
                        vx = 0f,
                        vy = 0f,
                        life = 4.0f,
                        maxLife = 4.0f,
                        color = Color(0x661E293B),
                        size = 8f,
                        isTireSkid = true
                    )
                )
            }
        } else {
            vehicle.driftAngle *= 0.88f // Snap back
        }

        // Stunt jump airtime
        if (vehicle.airTime > 0f) {
            vehicle.airTime = (vehicle.airTime - dt).coerceAtLeast(0f)
        }

        // Apply movement velocity
        val moveAngleRad = Math.toRadians((vehicle.angleDeg + vehicle.driftAngle).toDouble())
        val moveDist = vehicle.speed * 60f * dt
        val nextX = (vehicle.x + cos(moveAngleRad).toFloat() * moveDist).coerceIn(100f, 3900f)
        val nextY = (vehicle.y + sin(moveAngleRad).toFloat() * moveDist).coerceIn(100f, 3900f)

        // Check obstacles collision
        var collided = false
        for (obs in GameMap.obstacles) {
            val dist = hypot(nextX - obs.x, nextY - obs.y)
            if (obs.type == ObstacleType.STUNT_RAMP && dist < 55f && vehicle.speed > 3f) {
                // Hit ramp! Airborne jump!
                vehicle.airTime = 1.2f
                player.totalDriftScore += 250f
                soundEngine.playNitro()
            } else if (obs.type == ObstacleType.GAS_STATION_PAD && dist < 70f) {
                // Refuel
                vehicle.fuel = (vehicle.fuel + 40f * dt).coerceAtMost(100f)
            } else if (obs.type != ObstacleType.STUNT_RAMP && obs.type != ObstacleType.GAS_STATION_PAD) {
                val halfW = obs.width / 2f + 25f
                val halfH = obs.height / 2f + 25f
                if (abs(nextX - obs.x) < halfW && abs(nextY - obs.y) < halfH) {
                    collided = true
                    if (abs(vehicle.speed) > 4f) {
                        val crashDamage = abs(vehicle.speed) * 40f
                        vehicle.health = (vehicle.health - crashDamage).coerceAtLeast(0f)
                        soundEngine.playCrash()
                        // Sparks
                        for (i in 0 until 12) {
                            particles.add(
                                Particle(
                                    x = vehicle.x,
                                    y = vehicle.y,
                                    vx = (Random.nextFloat() - 0.5f) * 150f,
                                    vy = (Random.nextFloat() - 0.5f) * 150f,
                                    life = 0.4f,
                                    maxLife = 0.4f,
                                    color = Color(0xFFFFCC00),
                                    size = 5f
                                )
                            )
                        }
                    }
                    vehicle.speed = -vehicle.speed * 0.35f
                    break
                }
            }
        }

        if (!collided) {
            vehicle.x = nextX
            vehicle.y = nextY
        }

        // Roadkill check against bots
        if (abs(vehicle.speed) > 2.2f) {
            for (bot in bots) {
                if (bot.isAlive && hypot(vehicle.x - bot.x, vehicle.y - bot.y) < 45f) {
                    val roadkillDamage = abs(vehicle.speed) * 60f
                    bot.health -= roadkillDamage
                    // Knockback bot
                    bot.x += cos(moveAngleRad).toFloat() * 50f
                    bot.y += sin(moveAngleRad).toFloat() * 50f
                    soundEngine.playCrash()

                    if (bot.health <= 0f) {
                        bot.isAlive = false
                        player.kills++
                        player.roadkills++
                        onKillFeed(
                            KillFeedEntry(
                                killer = "You",
                                victim = bot.name,
                                weapon = vehicle.model.nameEn,
                                isPlayerInvolved = true,
                                isRoadkill = true
                            )
                        )
                    }
                }
            }
        }

        // Check if vehicle explodes
        if (vehicle.health <= 0f && !vehicle.isDestroyed) {
            vehicle.isDestroyed = true
            player.activeVehicleId = null
            player.health = (player.health - 65f).coerceAtLeast(0f)
            soundEngine.playExplosion()
            spawnExplosionParticles(vehicle.x, vehicle.y)
        }

        // Sync player position to vehicle
        player.x = vehicle.x
        player.y = vehicle.y
        player.angleDeg = vehicle.angleDeg
        player.distanceDrivenMeters += abs(moveDist) * 0.1f
    }

    private fun updatePlayerOnFoot(
        dt: Float,
        player: PlayerState,
        moveX: Float,
        moveY: Float,
        isShooting: Boolean
    ) {
        if (player.activeVehicleId != null) {
            // Player in vehicle drive-by shooting
            if (isShooting) {
                firePlayerWeapon(player)
            }
            return
        }

        if (player.isVaulting) {
            player.vaultProgress += dt / 0.45f
            val t = player.vaultProgress.coerceIn(0f, 1f)
            player.x = player.vaultStartX + (player.vaultTargetX - player.vaultStartX) * t
            player.y = player.vaultStartY + (player.vaultTargetY - player.vaultStartY) * t
            if (player.vaultProgress >= 1f) {
                player.isVaulting = false
                player.vaultProgress = 0f
            }
            return
        }

        // Energy drink boost effect: health regen and +20% move speed
        if (player.boostPercent > 0f) {
            player.boostPercent = (player.boostPercent - 1.2f * dt).coerceAtLeast(0f)
            if (player.health < player.maxHealth) {
                player.health = (player.health + 2.5f * dt).coerceAtMost(player.maxHealth)
            }
        }

        // Player reload progress
        if (player.isReloading) {
            player.reloadProgress += dt / 1.8f
            if (player.reloadProgress >= 1f) {
                finishPlayerReload(player)
            }
        }

        // Tactical Stance Speeds
        val baseSpeed = when (player.stance) {
            TacticalStance.STANDING -> 190f * (if (player.boostPercent > 20f) 1.25f else 1.0f)
            TacticalStance.CROUCHING -> 120f
            TacticalStance.PRONE -> 60f
        }

        val targetVx = moveX * baseSpeed
        val targetVy = moveY * baseSpeed

        // Weighted inertia & momentum (smooth acceleration & braking)
        val accelRate = 0.18f
        player.velocityX += (targetVx - player.velocityX) * accelRate
        player.velocityY += (targetVy - player.velocityY) * accelRate

        val currentSpeed = hypot(player.velocityX, player.velocityY)
        if (currentSpeed > 5f) {
            val inputAngle = atan2(player.velocityY.toDouble(), player.velocityX.toDouble()).toFloat()
            player.angleDeg = Math.toDegrees(inputAngle.toDouble()).toFloat()
        }

        val nextX = (player.x + player.velocityX * dt).coerceIn(100f, 3900f)
        val nextY = (player.y + player.velocityY * dt).coerceIn(100f, 3900f)

        // Obstacle collision
        var canMove = true
        for (obs in GameMap.obstacles) {
            if (obs.type != ObstacleType.GAS_STATION_PAD && obs.type != ObstacleType.STUNT_RAMP) {
                val halfW = obs.width / 2f + 18f
                val halfH = obs.height / 2f + 18f
                if (abs(nextX - obs.x) < halfW && abs(nextY - obs.y) < halfH) {
                    canMove = false
                    player.velocityX = 0f
                    player.velocityY = 0f
                    break
                }
            }
        }
        if (canMove) {
            player.x = nextX
            player.y = nextY
        }

        if (isShooting && !player.isReloading) {
            firePlayerWeapon(player)
        }
    }

    private var lastPlayerShotTime = 0L

    private fun firePlayerWeapon(player: PlayerState) {
        val weapon = when (player.activeWeaponSlot) {
            0 -> player.primaryWeapon
            1 -> player.secondaryWeapon
            else -> WeaponModel.PAN
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPlayerShotTime < weapon.fireRateMs) return

        // Ammo check
        val currentMag = if (player.activeWeaponSlot == 0) player.ammoInMagPrimary else player.ammoInMagSecondary
        if (!weapon.isMelee && currentMag <= 0) {
            startPlayerReload(player)
            return
        }

        lastPlayerShotTime = currentTime

        if (!weapon.isMelee) {
            if (player.activeWeaponSlot == 0) player.ammoInMagPrimary-- else player.ammoInMagSecondary--
        }

        soundEngine.playGunshot(weapon.id)

        // Bullet spawn
        val spread = (Random.nextFloat() - 0.5f) * weapon.recoil * 12f
        val shootAngleRad = Math.toRadians((player.angleDeg + spread).toDouble())
        val vx = cos(shootAngleRad).toFloat() * weapon.bulletSpeed
        val vy = sin(shootAngleRad).toFloat() * weapon.bulletSpeed

        if (!weapon.isMelee) {
            player.shotsFired++
            bullets.add(
                Bullet(
                    x = player.x + cos(shootAngleRad).toFloat() * 25f,
                    y = player.y + sin(shootAngleRad).toFloat() * 25f,
                    vx = vx,
                    vy = vy,
                    damage = weapon.damage,
                    shooterId = "player",
                    isFromPlayer = true,
                    maxDistance = if (weapon.isSniper) 1400f else 850f
                )
            )
            // Muzzle flash particle
            particles.add(
                Particle(
                    x = player.x + cos(shootAngleRad).toFloat() * 32f,
                    y = player.y + sin(shootAngleRad).toFloat() * 32f,
                    vx = vx * 0.1f,
                    vy = vy * 0.1f,
                    life = 0.08f,
                    maxLife = 0.08f,
                    color = Color(0xFFFFF07A),
                    size = 15f
                )
            )
        } else {
            // Pan melee swing
            soundEngine.playGunshot("PAN")
            for (bot in bots) {
                if (bot.isAlive && hypot(player.x - bot.x, player.y - bot.y) < 65f) {
                    bot.health -= weapon.damage
                    if (bot.health <= 0f) {
                        bot.isAlive = false
                        player.kills++
                        onKillFeed(
                            KillFeedEntry(
                                killer = "You",
                                victim = bot.name,
                                weapon = "Cast Iron Pan",
                                isPlayerInvolved = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun startPlayerReload(player: PlayerState) {
        if (player.isReloading) return
        val weapon = if (player.activeWeaponSlot == 0) player.primaryWeapon else player.secondaryWeapon
        if (weapon.isMelee) return

        val reserve = when (weapon.ammoType) {
            "5.56mm" -> player.reserve556
            "7.62mm" -> player.reserve762
            ".300 Mag" -> player.reserve300Mag
            else -> 0
        }
        if (reserve <= 0) return

        player.isReloading = true
        player.reloadProgress = 0f
        soundEngine.playReload()
    }

    private fun finishPlayerReload(player: PlayerState) {
        player.isReloading = false
        player.reloadProgress = 0f
        val weapon = if (player.activeWeaponSlot == 0) player.primaryWeapon else player.secondaryWeapon
        val currentMag = if (player.activeWeaponSlot == 0) player.ammoInMagPrimary else player.ammoInMagSecondary
        val needed = weapon.magCapacity - currentMag

        when (weapon.ammoType) {
            "5.56mm" -> {
                val toLoad = min(needed, player.reserve556)
                if (player.activeWeaponSlot == 0) player.ammoInMagPrimary += toLoad else player.ammoInMagSecondary += toLoad
                player.reserve556 -= toLoad
            }
            "7.62mm" -> {
                val toLoad = min(needed, player.reserve762)
                if (player.activeWeaponSlot == 0) player.ammoInMagPrimary += toLoad else player.ammoInMagSecondary += toLoad
                player.reserve762 -= toLoad
            }
            ".300 Mag" -> {
                val toLoad = min(needed, player.reserve300Mag)
                if (player.activeWeaponSlot == 0) player.ammoInMagPrimary += toLoad else player.ammoInMagSecondary += toLoad
                player.reserve300Mag -= toLoad
            }
        }
    }

    private fun updateBots(dt: Float, player: PlayerState) {
        for (bot in bots) {
            if (!bot.isAlive) continue

            bot.aiStateTimer += dt
            if (bot.aiStateTimer > 3f) {
                bot.aiStateTimer = 0f
                // Pick target: if outside safe zone, move towards zone center; else hunt player if within range
                val distToZoneCenter = hypot(bot.x - zone.whiteCenterX, bot.y - zone.whiteCenterY)
                val distToPlayer = hypot(bot.x - player.x, bot.y - player.y)

                if (distToZoneCenter > zone.whiteRadius * 0.8f) {
                    bot.aiAction = "FLEE_ZONE"
                    bot.targetX = zone.whiteCenterX + (Random.nextFloat() - 0.5f) * zone.whiteRadius * 0.4f
                    bot.targetY = zone.whiteCenterY + (Random.nextFloat() - 0.5f) * zone.whiteRadius * 0.4f
                } else if (distToPlayer < 450f) {
                    bot.aiAction = "HUNT"
                    bot.targetX = player.x
                    bot.targetY = player.y
                } else {
                    bot.aiAction = "PATROL"
                    bot.targetX = bot.x + (Random.nextFloat() - 0.5f) * 400f
                    bot.targetY = bot.y + (Random.nextFloat() - 0.5f) * 400f
                }
            }

            // Move towards target
            val dx = bot.targetX - bot.x
            val dy = bot.targetY - bot.y
            val dist = hypot(dx, dy)
            if (dist > 15f) {
                val angleRad = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                bot.angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                val moveSpeed = 110f * dt
                bot.x += cos(angleRad) * moveSpeed
                bot.y += sin(angleRad) * moveSpeed
            }

            // Fire at player if within line of sight
            val distToPlayer = hypot(bot.x - player.x, bot.y - player.y)
            if (distToPlayer < 350f && Random.nextFloat() < 0.035f) {
                val aimAngleRad = atan2((player.y - bot.y).toDouble(), (player.x - bot.x).toDouble()).toFloat()
                bot.angleDeg = Math.toDegrees(aimAngleRad.toDouble()).toFloat()
                val spread = (Random.nextFloat() - 0.5f) * 0.25f
                val finalAngle = aimAngleRad + spread
                bullets.add(
                    Bullet(
                        x = bot.x + cos(finalAngle) * 20f,
                        y = bot.y + sin(finalAngle) * 20f,
                        vx = cos(finalAngle) * bot.weapon.bulletSpeed * 0.7f,
                        vy = sin(finalAngle) * bot.weapon.bulletSpeed * 0.7f,
                        damage = bot.weapon.damage * 0.45f, // Scaled for fun engagement
                        shooterId = bot.id,
                        isFromPlayer = false,
                        maxDistance = 550f
                    )
                )
                soundEngine.playGunshot(bot.weapon.id)
            }

            // Bot blue zone damage
            val distToBlue = hypot(bot.x - zone.blueCenterX, bot.y - zone.blueCenterY)
            if (distToBlue > zone.blueRadius) {
                bot.health -= zone.damagePerSec * dt * 1.5f
                if (bot.health <= 0f) {
                    bot.isAlive = false
                    onKillFeed(
                        KillFeedEntry(
                            killer = "Blue Zone",
                            victim = bot.name,
                            weapon = "Playzone",
                            isPlayerInvolved = false
                        )
                    )
                }
            }
        }
    }

    private fun updateBullets(dt: Float, player: PlayerState) {
        val iter = bullets.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.x += b.vx * 60f * dt
            b.y += b.vy * 60f * dt
            b.distanceTraveled += hypot(b.vx, b.vy) * 60f * dt

            var hit = false

            // Hit Player?
            if (!b.isFromPlayer) {
                if (hypot(b.x - player.x, b.y - player.y) < 24f) {
                    hit = true
                    // Armor mitigation
                    val vestReduc = when (player.vestLevel) {
                        3 -> 0.55f
                        2 -> 0.40f
                        1 -> 0.25f
                        else -> 0.0f
                    }
                    val actualDmg = b.damage * (1f - vestReduc)
                    player.health = (player.health - actualDmg).coerceAtLeast(0f)
                    player.vestDurability = (player.vestDurability - actualDmg * 0.5f).coerceAtLeast(0f)
                    // Blood puff
                    spawnBloodParticles(b.x, b.y)
                }
            } else {
                // Hit Bots?
                for (bot in bots) {
                    val hitDist = hypot(b.x - bot.x, b.y - bot.y)
                    if (bot.isAlive && hitDist < 26f) {
                        hit = true
                        val isHeadshot = hitDist < 12f || (Random.nextFloat() < 0.25f)
                        val finalDamage = if (isHeadshot) b.damage * 2.0f else b.damage
                        bot.health -= finalDamage
                        player.damageDealt += finalDamage
                        player.shotsHit++
                        if (isHeadshot) {
                            player.headshotsCount++
                        }

                        // Server Lag Compensation verification event
                        val simulatedRewind = (24..38).random()
                        player.lastHitEvent = HitMarkerEvent(
                            timestamp = System.currentTimeMillis(),
                            isHeadshot = isHeadshot,
                            damage = finalDamage,
                            rewindMs = simulatedRewind,
                            serverTick = 60,
                            victimName = bot.name
                        )

                        spawnBloodParticles(b.x, b.y)

                        if (bot.health <= 0f) {
                            bot.isAlive = false
                            player.kills++
                            val currentWeapon = if (player.activeWeaponSlot == 0) player.primaryWeapon.nameEn else player.secondaryWeapon.nameEn
                            onKillFeed(
                                KillFeedEntry(
                                    killer = "You",
                                    victim = bot.name,
                                    weapon = currentWeapon,
                                    isPlayerInvolved = true
                                )
                            )
                        }
                        break
                    }
                }
            }

            // Hit Vehicles?
            if (!hit) {
                for (veh in vehicles) {
                    if (!veh.isDestroyed && hypot(b.x - veh.x, b.y - veh.y) < 40f) {
                        hit = true
                        veh.health -= b.damage
                        // Metal sparks
                        for (i in 0 until 5) {
                            particles.add(
                                Particle(
                                    x = b.x,
                                    y = b.y,
                                    vx = (Random.nextFloat() - 0.5f) * 80f,
                                    vy = (Random.nextFloat() - 0.5f) * 80f,
                                    life = 0.2f,
                                    maxLife = 0.2f,
                                    color = Color(0xFFFFEB3B),
                                    size = 4f
                                )
                            )
                        }
                        break
                    }
                }
            }

            // Hit obstacle?
            if (!hit) {
                for (obs in GameMap.obstacles) {
                    if (obs.type != ObstacleType.GAS_STATION_PAD && obs.type != ObstacleType.STUNT_RAMP) {
                        if (abs(b.x - obs.x) < obs.width / 2f && abs(b.y - obs.y) < obs.height / 2f) {
                            hit = true
                            break
                        }
                    }
                }
            }

            if (hit || b.distanceTraveled >= b.maxDistance) {
                iter.remove()
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
            }
        }
    }

    private fun updateAirdrop(dt: Float, player: PlayerState) {
        airdropTimer -= dt
        if (airdropTimer <= 0f && airdrop == null) {
            airdropTimer = 90f // Next airdrop in 90s
            // Drop near zone center
            val dropX = zone.whiteCenterX + (Random.nextFloat() - 0.5f) * zone.whiteRadius * 0.7f
            val dropY = zone.whiteCenterY + (Random.nextFloat() - 0.5f) * zone.whiteRadius * 0.7f
            airdrop = Airdrop(x = dropX, y = dropY)
            onKillFeed(
                KillFeedEntry(
                    killer = "AIRDROP",
                    victim = "Cargo Plane Inbound",
                    weapon = "Supply Flare",
                    isPlayerInvolved = false
                )
            )
        }

        airdrop?.let { drop ->
            if (!drop.isLanded) {
                drop.altitude -= 45f * dt
                if (drop.altitude <= 0f) {
                    drop.altitude = 0f
                    drop.isLanded = true
                    soundEngine.playCrash()
                }
            } else {
                // Billowing red flare smoke
                drop.flareTimer -= dt
                if (drop.flareTimer > 0f && Random.nextFloat() < 0.4f) {
                    particles.add(
                        Particle(
                            x = drop.x + (Random.nextFloat() - 0.5f) * 15f,
                            y = drop.y + (Random.nextFloat() - 0.5f) * 15f,
                            vx = (Random.nextFloat() - 0.5f) * 20f,
                            vy = -45f - Random.nextFloat() * 30f,
                            life = 1.4f,
                            maxLife = 1.4f,
                            color = Color(0xDDFF1744),
                            size = 18f
                        )
                    )
                }
            }

            // Check if player loots airdrop crate
            if (drop.isLanded && hypot(player.x - drop.x, player.y - drop.y) < 45f) {
                player.primaryWeapon = WeaponModel.AWM
                player.ammoInMagPrimary = 5
                player.reserve300Mag += 20
                player.helmetLevel = 3
                player.helmetDurability = 250f
                player.vestLevel = 3
                player.vestDurability = 300f
                player.firstAidKits += 2
                player.energyDrinks += 3
                soundEngine.playHeal()
                onKillFeed(
                    KillFeedEntry(
                        killer = "You",
                        victim = "Airdrop Looted!",
                        weapon = "AWM + Level 3 Gear",
                        isPlayerInvolved = true
                    )
                )
                airdrop = null
            }
        }
    }

    private fun checkLootPickups(player: PlayerState) {
        val iter = lootDrops.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (hypot(player.x - item.x, player.y - item.y) < 38f) {
                when (item.type) {
                    "WEAPON" -> {
                        if (item.title.contains("M416")) {
                            player.primaryWeapon = WeaponModel.M416
                            player.reserve556 += 60
                        } else if (item.title.contains("AKM")) {
                            player.secondaryWeapon = WeaponModel.AKM
                            player.reserve762 += 60
                        } else if (item.title.contains("Groza")) {
                            player.primaryWeapon = WeaponModel.GROZA
                            player.reserve762 += 60
                        }
                    }
                    "AMMO" -> {
                        player.reserve556 += 90
                        player.reserve762 += 60
                    }
                    "ARMOR" -> {
                        player.helmetLevel = 3
                        player.vestLevel = 3
                        player.helmetDurability = 250f
                        player.vestDurability = 300f
                    }
                    "MED" -> {
                        player.firstAidKits += 2
                        player.energyDrinks += 2
                    }
                    "FUEL" -> {
                        player.jerryCans += 1
                        // Also top up current vehicle nitro if driving
                        player.activeVehicleId?.let { vehId ->
                            vehicles.find { it.id == vehId }?.let { it.nitro = 100f }
                        }
                    }
                }
                soundEngine.playReload()
                iter.remove()
            }
        }
    }

    private fun spawnBloodParticles(x: Float, y: Float) {
        for (i in 0 until 6) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Random.nextFloat() - 0.5f) * 60f,
                    vy = (Random.nextFloat() - 0.5f) * 60f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    color = Color(0xFFB71C1C),
                    size = 6f
                )
            )
        }
    }

    private fun spawnExplosionParticles(x: Float, y: Float) {
        for (i in 0 until 35) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 180f + 40f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 0.7f,
                    maxLife = 0.7f,
                    color = if (Random.nextBoolean()) Color(0xFFFF5722) else Color(0xFFFFC107),
                    size = 20f
                )
            )
        }
    }

    fun toggleVehicleEntry(player: PlayerState): Boolean {
        if (player.activeVehicleId != null) {
            // Exit vehicle
            val veh = vehicles.find { it.id == player.activeVehicleId }
            if (veh != null) {
                veh.driverId = null
                veh.speed *= 0.5f
                player.x = veh.x + 35f
                player.y = veh.y
            }
            player.activeVehicleId = null
            return true
        } else {
            // Find closest vehicle within 60f
            val closest = vehicles.filter { !it.isDestroyed }.minByOrNull { hypot(player.x - it.x, player.y - it.y) }
            if (closest != null && hypot(player.x - closest.x, player.y - closest.y) < 65f) {
                player.activeVehicleId = closest.id
                closest.driverId = "player"
                return true
            }
        }
        return false
    }

    fun honkVehicleHorn(player: PlayerState) {
        val veh = player.activeVehicleId?.let { id -> vehicles.find { it.id == id } } ?: return
        soundEngine.playHorn()
        // Alert nearby bots
        for (bot in bots) {
            if (bot.isAlive && hypot(veh.x - bot.x, veh.y - bot.y) < 600f) {
                bot.targetX = veh.x
                bot.targetY = veh.y
            }
        }
    }

    private fun updateGrenades(dt: Float, player: PlayerState) {
        val iter = grenades.iterator()
        while (iter.hasNext()) {
            val g = iter.next()
            g.x += g.vx * dt
            g.y += g.vy * dt
            g.vx *= 0.94f // Air & ground drag
            g.vy *= 0.94f
            g.timer -= dt

            // Trail smoke
            if (Random.nextFloat() < 0.35f) {
                particles.add(
                    Particle(
                        x = g.x,
                        y = g.y,
                        vx = 0f,
                        vy = 0f,
                        life = 0.4f,
                        maxLife = 0.4f,
                        color = Color(0x6694A3B8),
                        size = 6f
                    )
                )
            }

            if (g.timer <= 0f && !g.isExploded) {
                g.isExploded = true
                soundEngine.playExplosion()
                player.screenShake = 22f
                spawnExplosionParticles(g.x, g.y)

                // Damage radius 140f
                // Damage Player?
                val distToPlayer = hypot(g.x - player.x, g.y - player.y)
                if (distToPlayer < 140f) {
                    val dmg = ((140f - distToPlayer) / 140f) * 110f
                    player.health = (player.health - dmg).coerceAtLeast(0f)
                }

                // Damage Bots
                for (bot in bots) {
                    if (bot.isAlive) {
                        val d = hypot(g.x - bot.x, g.y - bot.y)
                        if (d < 140f) {
                            val dmg = ((140f - d) / 140f) * 125f
                            bot.health -= dmg
                            if (bot.health <= 0f) {
                                bot.isAlive = false
                                player.kills++
                                onKillFeed(
                                    KillFeedEntry(
                                        killer = "You",
                                        victim = bot.name,
                                        weapon = "Frag Grenade",
                                        isPlayerInvolved = true
                                    )
                                )
                            }
                        }
                    }
                }

                // Damage Vehicles
                for (veh in vehicles) {
                    if (!veh.isDestroyed) {
                        val d = hypot(g.x - veh.x, g.y - veh.y)
                        if (d < 150f) {
                            veh.health -= ((150f - d) / 150f) * 350f
                        }
                    }
                }

                iter.remove()
            }
        }
    }

    private fun updateRedZone(dt: Float, player: PlayerState, mode: GameMode) {
        if (mode == GameMode.FIRING_RANGE) return

        redZone.timer -= dt
        if (redZone.timer <= 0f) {
            if (!redZone.active) {
                // Activate new Red Zone
                redZone.active = true
                redZone.timer = 30f // Active for 30s
                // Spawn within current zone
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val dist = Random.nextFloat() * (zone.whiteRadius * 0.7f)
                redZone.x = zone.whiteCenterX + cos(angle) * dist
                redZone.y = zone.whiteCenterY + sin(angle) * dist
                soundEngine.playHorn()
                onKillFeed(
                    KillFeedEntry(
                        killer = "RED ZONE",
                        victim = "Artillery Barrage Active",
                        weapon = "Air Strike",
                        isPlayerInvolved = false
                    )
                )
            } else {
                redZone.active = false
                redZone.timer = 50f // Next redzone in 50s
            }
        }

        if (redZone.active) {
            redZone.bombTimer -= dt
            if (redZone.bombTimer <= 0f) {
                redZone.bombTimer = 0.8f + Random.nextFloat() * 0.8f
                // Artillery bomb drops inside red zone
                val bAngle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val bDist = Random.nextFloat() * redZone.radius
                val bombX = redZone.x + cos(bAngle) * bDist
                val bombY = redZone.y + sin(bAngle) * bDist
                spawnExplosionParticles(bombX, bombY)
                soundEngine.playExplosion()

                // Check player hit
                if (hypot(bombX - player.x, bombY - player.y) < 70f) {
                    player.health = (player.health - 60f).coerceAtLeast(0f)
                    player.screenShake = 25f
                }
            }
        }
    }

    private fun updateFootstepPings(dt: Float) {
        // Collect noise events from moving bots
        if (Random.nextFloat() < 0.08f) {
            for (bot in bots) {
                if (bot.isAlive) {
                    footstepPings.add(
                        FootstepPing(
                            x = bot.x,
                            y = bot.y,
                            isVehicle = bot.inVehicleId != null
                        )
                    )
                    break
                }
            }
        }

        val iter = footstepPings.iterator()
        while (iter.hasNext()) {
            val ping = iter.next()
            ping.alpha -= 0.6f * dt
            if (ping.alpha <= 0f) {
                iter.remove()
            }
        }
    }

    fun throwGrenade(player: PlayerState): Boolean {
        if (player.grenadesCount <= 0) return false
        player.grenadesCount--
        val rad = Math.toRadians(player.angleDeg.toDouble())
        val throwSpeed = 440f
        grenades.add(
            GrenadeEntity(
                x = player.x + cos(rad).toFloat() * 25f,
                y = player.y + sin(rad).toFloat() * 25f,
                vx = cos(rad).toFloat() * throwSpeed,
                vy = sin(rad).toFloat() * throwSpeed
            )
        )
        soundEngine.playReload()
        return true
    }

    fun fireFlareGun(player: PlayerState): Boolean {
        if (player.flareGunsCount <= 0) return false
        player.flareGunsCount--
        soundEngine.playGunshot("AWM")
        player.screenShake = 16f
        airdrop = Airdrop(x = player.x, y = player.y, altitude = 450f)
        onKillFeed(
            KillFeedEntry(
                killer = "YOU",
                victim = "Flare Summoned!",
                weapon = "Supply Plane",
                isPlayerInvolved = true
            )
        )
        return true
    }

    fun tryVaultOrMantle(player: PlayerState): Boolean {
        if (player.isVaulting || player.activeVehicleId != null) return false
        val rad = Math.toRadians(player.angleDeg.toDouble())
        val forwardX = cos(rad).toFloat()
        val forwardY = sin(rad).toFloat()
        val checkDist = 50f

        for (obs in GameMap.obstacles) {
            if (obs.type == ObstacleType.WALL || obs.type == ObstacleType.BUILDING || obs.type == ObstacleType.HANGAR) {
                val testX = player.x + forwardX * checkDist
                val testY = player.y + forwardY * checkDist
                if (abs(testX - obs.x) < obs.width / 2f + 20f && abs(testY - obs.y) < obs.height / 2f + 20f) {
                    player.isVaulting = true
                    player.vaultProgress = 0f
                    player.vaultStartX = player.x
                    player.vaultStartY = player.y
                    val leapDist = 65f
                    player.vaultTargetX = (player.x + forwardX * leapDist).coerceIn(100f, 3900f)
                    player.vaultTargetY = (player.y + forwardY * leapDist).coerceIn(100f, 3900f)
                    player.stance = TacticalStance.STANDING
                    soundEngine.playDrift()
                    return true
                }
            }
        }
        return false
    }

    fun toggleCrouch(player: PlayerState) {
        player.stance = if (player.stance == TacticalStance.CROUCHING) TacticalStance.STANDING else TacticalStance.CROUCHING
        soundEngine.playReload()
    }

    fun toggleProne(player: PlayerState) {
        player.stance = if (player.stance == TacticalStance.PRONE) TacticalStance.STANDING else TacticalStance.PRONE
        soundEngine.playReload()
    }

    fun togglePerspective(player: PlayerState) {
        player.perspective = if (player.perspective == CameraPerspective.TPP) CameraPerspective.FPP else CameraPerspective.TPP
        soundEngine.playReload()
    }
}
