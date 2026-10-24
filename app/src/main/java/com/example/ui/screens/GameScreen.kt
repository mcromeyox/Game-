package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.GameMap
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import kotlin.math.*

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackToLobby: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val physics = viewModel.physicsEngine
    val player = uiState.player
    val isArabic = uiState.isArabic

    val inVeh = player.activeVehicleId?.let { id -> physics.vehicles.find { it.id == id } }

    // Camera offset
    var cameraScale by remember { mutableFloatStateOf(1.0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Game Rendering Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Touch aiming
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val angleRad = atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                            player.angleDeg = Math.toDegrees(angleRad).toFloat()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val angleRad = atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                            player.angleDeg = Math.toDegrees(angleRad).toFloat()
                        }
                    )
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Calculate camera zoom based on vehicle speed
            val inVeh = player.activeVehicleId?.let { id -> physics.vehicles.find { it.id == id } }
            val currentSpeedKmh = inVeh?.let { abs(it.speed) * 10f } ?: 0f
            val targetScale = if (player.isScoping) {
                player.currentScope.magnification
            } else if (inVeh != null) {
                (0.9f - (currentSpeedKmh / 200f) * 0.25f).coerceIn(0.65f, 0.95f)
            } else if (player.perspective == CameraPerspective.FPP) {
                1.50f
            } else {
                1.0f
            }
            cameraScale += (targetScale - cameraScale) * 0.1f

            val shakeOffset = if (player.screenShake > 0.5f) {
                Offset(
                    (kotlin.random.Random.nextFloat() - 0.5f) * player.screenShake,
                    (kotlin.random.Random.nextFloat() - 0.5f) * player.screenShake
                )
            } else Offset.Zero

            val camX = canvasW / 2f - player.x * cameraScale + shakeOffset.x
            val camY = canvasH / 2f - player.y * cameraScale + shakeOffset.y

            // Draw World Terrain
            drawRect(
                color = PubgGrass,
                topLeft = Offset(camX, camY),
                size = Size(GameMap.WORLD_WIDTH * cameraScale, GameMap.WORLD_HEIGHT * cameraScale)
            )

            // Draw Desert Sand Zone (North-East quadrant)
            drawRect(
                color = PubgSand,
                topLeft = Offset(camX + 2200f * cameraScale, camY),
                size = Size(1800f * cameraScale, 1800f * cameraScale)
            )

            // Draw Roads (Highways)
            for (road in GameMap.roads) {
                val startScreen = Offset(camX + road.start.x * cameraScale, camY + road.start.y * cameraScale)
                val endScreen = Offset(camX + road.end.x * cameraScale, camY + road.end.y * cameraScale)
                // Asphalt surface
                drawLine(
                    color = PubgAsphalt,
                    start = startScreen,
                    end = endScreen,
                    strokeWidth = road.width * cameraScale,
                    cap = StrokeCap.Round
                )
                // Road shoulders (dirt borders)
                drawLine(
                    color = Color(0xFF1E293B),
                    start = startScreen,
                    end = endScreen,
                    strokeWidth = (road.width - 8f) * cameraScale,
                    cap = StrokeCap.Round
                )
                // Yellow center dashed line
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = startScreen,
                    end = endScreen,
                    strokeWidth = 3f * cameraScale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f * cameraScale, 20f * cameraScale), 0f)
                )
            }

            // Draw Obstacles (Buildings, Hangars, Ramps, Gas Stations)
            for (obs in GameMap.obstacles) {
                val obsScreenX = camX + (obs.x - obs.width / 2f) * cameraScale
                val obsScreenY = camY + (obs.y - obs.height / 2f) * cameraScale
                val obsW = obs.width * cameraScale
                val obsH = obs.height * cameraScale

                // Drop shadow
                drawRect(
                    color = Color(0x55000000),
                    topLeft = Offset(obsScreenX + 8f * cameraScale, obsScreenY + 8f * cameraScale),
                    size = Size(obsW, obsH)
                )

                when (obs.type) {
                    ObstacleType.BUILDING, ObstacleType.HANGAR -> {
                        drawRect(
                            color = Color(0xFF475569),
                            topLeft = Offset(obsScreenX, obsScreenY),
                            size = Size(obsW, obsH)
                        )
                        // Roof detail
                        drawRect(
                            color = Color(0xFF334155),
                            topLeft = Offset(obsScreenX + 6f * cameraScale, obsScreenY + 6f * cameraScale),
                            size = Size((obs.width - 12f) * cameraScale, (obs.height - 12f) * cameraScale)
                        )
                    }
                    ObstacleType.WATCHTOWER -> {
                        drawCircle(
                            color = Color(0xFF1E293B),
                            center = Offset(camX + obs.x * cameraScale, camY + obs.y * cameraScale),
                            radius = (obs.width / 2f) * cameraScale
                        )
                    }
                    ObstacleType.WALL -> {
                        drawRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(obsScreenX, obsScreenY),
                            size = Size(obsW, obsH)
                        )
                    }
                    ObstacleType.TREE -> {
                        drawCircle(
                            color = Color(0xFF1B4332),
                            center = Offset(camX + obs.x * cameraScale, camY + obs.y * cameraScale),
                            radius = (obs.width / 2f) * cameraScale
                        )
                    }
                    ObstacleType.ROCK -> {
                        drawCircle(
                            color = Color(0xFF78716C),
                            center = Offset(camX + obs.x * cameraScale, camY + obs.y * cameraScale),
                            radius = (obs.width / 2f) * cameraScale
                        )
                    }
                    ObstacleType.STUNT_RAMP -> {
                        // Neon yellow-striped stunt ramp
                        drawRect(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(obsScreenX, obsScreenY),
                            size = Size(obsW, obsH)
                        )
                        drawLine(
                            color = Color.Black,
                            start = Offset(obsScreenX, obsScreenY + obsH / 2f),
                            end = Offset(obsScreenX + obsW, obsScreenY + obsH / 2f),
                            strokeWidth = 4f * cameraScale,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    }
                    ObstacleType.GAS_STATION_PAD -> {
                        // Gas pad with green cross
                        drawRect(
                            color = Color(0xFF0F766E),
                            topLeft = Offset(obsScreenX, obsScreenY),
                            size = Size(obsW, obsH)
                        )
                        drawCircle(
                            color = Color(0xFF34D399),
                            center = Offset(camX + obs.x * cameraScale, camY + obs.y * cameraScale),
                            radius = (obs.width * 0.35f) * cameraScale
                        )
                    }
                }
            }

            // Draw Tire Skid marks & Particles
            for (p in physics.particles) {
                val px = camX + p.x * cameraScale
                val py = camY + p.y * cameraScale
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = p.color.alpha * alpha),
                    radius = p.size * cameraScale * (if (p.isTireSkid) 1f else (1.5f - alpha * 0.5f)),
                    center = Offset(px, py)
                )
            }

            // Draw Loot Items
            for (item in physics.lootDrops) {
                val lx = camX + item.x * cameraScale
                val ly = camY + item.y * cameraScale
                // Glowing beacon circle
                drawCircle(
                    color = when (item.type) {
                        "WEAPON" -> Color(0x66F59E0B)
                        "ARMOR" -> Color(0x6606B6D4)
                        "MED" -> Color(0x6610B981)
                        else -> Color(0x66A855F7)
                    },
                    radius = 16f * cameraScale,
                    center = Offset(lx, ly)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * cameraScale,
                    center = Offset(lx, ly)
                )
            }

            // Draw Airdrop Crate
            physics.airdrop?.let { drop ->
                val ax = camX + drop.x * cameraScale
                val ay = camY + drop.y * cameraScale
                if (!drop.isLanded) {
                    // Parachute
                    val chuteY = ay - (drop.altitude + 35f) * cameraScale
                    drawArc(
                        color = Color(0xFFEF4444),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(ax - 28f * cameraScale, chuteY - 20f * cameraScale),
                        size = Size(56f * cameraScale, 40f * cameraScale)
                    )
                    // Crate box
                    drawRect(
                        color = Color(0xFF991B1B),
                        topLeft = Offset(ax - 14f * cameraScale, ay - drop.altitude * cameraScale - 14f * cameraScale),
                        size = Size(28f * cameraScale, 28f * cameraScale)
                    )
                } else {
                    // Landed crate with blue tarp and red flare
                    drawRect(
                        color = Color(0xFFB91C1C),
                        topLeft = Offset(ax - 16f * cameraScale, ay - 16f * cameraScale),
                        size = Size(32f * cameraScale, 32f * cameraScale)
                    )
                    drawRect(
                        color = Color(0xFF1E40AF),
                        topLeft = Offset(ax - 16f * cameraScale, ay - 16f * cameraScale),
                        size = Size(32f * cameraScale, 8f * cameraScale)
                    )
                }
            }

            // Draw Vehicles
            for (veh in physics.vehicles) {
                val vx = camX + veh.x * cameraScale
                val vy = camY + veh.y * cameraScale
                drawVehicleOnCanvas(veh, vx, vy, cameraScale)
            }

            // Draw Bots
            for (bot in physics.bots) {
                if (bot.isAlive) {
                    val bx = camX + bot.x * cameraScale
                    val by = camY + bot.y * cameraScale
                    drawBotOnCanvas(bot, bx, by, cameraScale)
                }
            }

            // Draw Player (if not in vehicle or draw on top of vehicle)
            if (player.activeVehicleId == null) {
                val px = camX + player.x * cameraScale
                val py = camY + player.y * cameraScale
                drawPlayerOnCanvas(player, px, py, cameraScale)
            }

            // Draw Bullets
            for (b in physics.bullets) {
                val bx = camX + b.x * cameraScale
                val by = camY + b.y * cameraScale
                val bulletColor = if (b.isFromPlayer) Color(0xFFFDE047) else Color(0xFFF87171)
                drawLine(
                    color = bulletColor,
                    start = Offset(bx, by),
                    end = Offset(bx - b.vx * 0.4f * cameraScale, by - b.vy * 0.4f * cameraScale),
                    strokeWidth = 3f * cameraScale,
                    cap = StrokeCap.Round
                )
            }

            // Draw Grenades
            for (g in physics.grenades) {
                val gx = camX + g.x * cameraScale
                val gy = camY + g.y * cameraScale
                drawCircle(color = Color(0xFF15803D), radius = 6f * cameraScale, center = Offset(gx, gy))
                drawCircle(color = Color(0xFFEF4444), radius = 2.5f * cameraScale, center = Offset(gx, gy))
            }

            // Draw Footstep & Vehicle Audio Radar Pings on Terrain
            for (ping in physics.footstepPings) {
                val fx = camX + ping.x * cameraScale
                val fy = camY + ping.y * cameraScale
                drawCircle(
                    color = (if (ping.isVehicle) PubgYellow else Color(0xFFFF9800)).copy(alpha = ping.alpha * 0.75f),
                    radius = (if (ping.isVehicle) 18f else 10f) * cameraScale,
                    center = Offset(fx, fy),
                    style = Stroke(width = 2f * cameraScale)
                )
            }

            // Draw Red Zone (Artillery Bombardment Hazard)
            if (physics.redZone.active) {
                val redCenterScreen = Offset(camX + physics.redZone.x * cameraScale, camY + physics.redZone.y * cameraScale)
                val redRadiusScreen = physics.redZone.radius * cameraScale
                drawCircle(
                    color = Color(0x33EF4444),
                    center = redCenterScreen,
                    radius = redRadiusScreen
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    center = redCenterScreen,
                    radius = redRadiusScreen,
                    style = Stroke(width = 3f * cameraScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 15f), 0f))
                )
            }

            // Draw Playzone Circles (Safe Zone White, Blue Zone Electric Wall)
            val whiteCenterScreen = Offset(camX + physics.zone.whiteCenterX * cameraScale, camY + physics.zone.whiteCenterY * cameraScale)
            val whiteRadiusScreen = physics.zone.whiteRadius * cameraScale
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                center = whiteCenterScreen,
                radius = whiteRadiusScreen,
                style = Stroke(width = 3f * cameraScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f))
            )

            val blueCenterScreen = Offset(camX + physics.zone.blueCenterX * cameraScale, camY + physics.zone.blueCenterY * cameraScale)
            val blueRadiusScreen = physics.zone.blueRadius * cameraScale
            drawCircle(
                color = PubgCyan.copy(alpha = 0.7f),
                center = blueCenterScreen,
                radius = blueRadiusScreen,
                style = Stroke(width = 5f * cameraScale)
            )
            // Outer blue electric haze
            drawCircle(
                color = PubgBlueZone.copy(alpha = 0.18f),
                center = blueCenterScreen,
                radius = blueRadiusScreen + 20f * cameraScale,
                style = Stroke(width = 40f * cameraScale)
            )

            // Environmental Weather & Atmosphere
            when (uiState.weather) {
                TimeOfDay.SUNSET -> {
                    drawRect(
                        color = Color(0x35E65100),
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
                TimeOfDay.NIGHT -> {
                    // Nighttime darkness
                    drawRect(
                        color = Color(0xAA030712),
                        topLeft = Offset.Zero,
                        size = size
                    )
                    // Headlight or Tactical Flashlight cone ahead of player/vehicle
                    val lightOriginX = canvasW / 2f
                    val lightOriginY = canvasH / 2f
                    val facingRad = Math.toRadians(player.angleDeg.toDouble())
                    val coneLength = 320f * cameraScale
                    val tipX = lightOriginX + cos(facingRad).toFloat() * coneLength
                    val tipY = lightOriginY + sin(facingRad).toFloat() * coneLength
                    drawCircle(
                        color = Color(0x35FEF08A),
                        center = Offset(tipX * 0.6f + lightOriginX * 0.4f, tipY * 0.6f + lightOriginY * 0.4f),
                        radius = 120f * cameraScale
                    )
                }
                TimeOfDay.DAY -> { /* Clear bright daylight */ }
            }
        }

        // Scope ADS Overlay (Red Dot, 4x, 8x reticle)
        if (player.isScoping) {
            ScopeOverlay(scopeType = player.currentScope)
        }

        // Top PUBG Compass & Header HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compass Tape Banner
            CompassTape(headingDeg = player.angleDeg)

            Spacer(modifier = Modifier.height(4.dp))

            // Match Info Bar: Alive, Kills, Zone Timer & Server Netcode Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC0F172A), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBackToLobby,
                    modifier = Modifier.size(32.dp).testTag("leave_match_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit Match",
                        tint = PubgYellow
                    )
                }

                // Perspective Toggle (TPP / FPP)
                Button(
                    onClick = { viewModel.togglePerspective() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp).testTag("toggle_perspective_button")
                ) {
                    Text(
                        text = if (player.perspective == CameraPerspective.TPP) "TPP" else "FPP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PubgYellow
                    )
                }

                // Dynamic Weather Switcher
                Button(
                    onClick = { viewModel.toggleWeather() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp).testTag("toggle_weather_button")
                ) {
                    Text(
                        text = when (uiState.weather) {
                            TimeOfDay.DAY -> "☀️"
                            TimeOfDay.SUNSET -> "🌅"
                            TimeOfDay.NIGHT -> "🌙"
                        },
                        fontSize = 12.sp
                    )
                }

                // Dedicated Server Netcode Telemetry Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF022C22), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.serverTickRate}Hz | ${uiState.pingMs}ms",
                        color = Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Players Alive
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = PubgYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "الأحياء: ${uiState.aliveCount}" else "ALIVE: ${uiState.aliveCount}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Kills Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "قتلات: ${player.kills}" else "KILLS: ${player.kills}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Zone Timer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = PubgCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${physics.zone.timeRemainingSec}s",
                        color = PubgCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Top Right: Mini-Map & Kill Feed
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Mini-Map Widget
                MiniMapWidget(
                    playerX = player.x,
                    playerY = player.y,
                    playerAngle = player.angleDeg,
                    zone = physics.zone,
                    redZone = physics.redZone,
                    vehicles = physics.vehicles,
                    airdrop = physics.airdrop
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Kill Feed Messages
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    uiState.killFeed.take(3).forEach { entry ->
                        KillFeedItem(entry = entry)
                    }
                }
            }
        }

        // Center Screen Hit Marker & Crosshair
        val lastHit = player.lastHitEvent
        val isRecentHit = lastHit != null && (System.currentTimeMillis() - lastHit.timestamp < 1000)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val crossColor = if (isRecentHit) {
                    if (lastHit?.isHeadshot == true) Color(0xFFEF4444) else Color(0xFFFBBF24)
                } else Color.White.copy(alpha = 0.55f)

                // Crosshair tick marks
                val gap = if (player.isScoping) 3f else 6f
                val len = if (player.isScoping) 10f else 7f
                drawLine(crossColor, Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = 1.8f)
                drawLine(crossColor, Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = 1.8f)
                drawLine(crossColor, Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = 1.8f)
                drawLine(crossColor, Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = 1.8f)

                // Hit Marker diagonal X when recently hit
                if (isRecentHit) {
                    val hitLen = if (lastHit?.isHeadshot == true) 14f else 10f
                    val hitColor = if (lastHit?.isHeadshot == true) Color(0xFFEF4444) else Color(0xFFFBBF24)
                    val hitStroke = if (lastHit?.isHeadshot == true) 3f else 2f
                    drawLine(hitColor, Offset(cx - hitLen, cy - hitLen), Offset(cx - 3f, cy - 3f), strokeWidth = hitStroke)
                    drawLine(hitColor, Offset(cx + 3f, cy + 3f), Offset(cx + hitLen, cy + hitLen), strokeWidth = hitStroke)
                    drawLine(hitColor, Offset(cx + hitLen, cy - hitLen), Offset(cx + 3f, cy - 3f), strokeWidth = hitStroke)
                    drawLine(hitColor, Offset(cx - hitLen, cy + hitLen), Offset(cx - 3f, cy + 3f), strokeWidth = hitStroke)
                }
            }
        }

        // Dedicated Server Netcode Validation Toast (Floating below Compass)
        if (isRecentHit && lastHit != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xEE0F172A),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (lastHit.isHeadshot) Color(0xFFEF4444) else Color(0xFFF59E0B)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lastHit.isHeadshot) "⚡ HEADSHOT CONFIRMED" else "⚡ HIT CONFIRMED",
                            color = if (lastHit.isHeadshot) Color(0xFFEF4444) else PubgYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "-${lastHit.damage.toInt()} HP (${lastHit.victimName})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF064E3B)
                        ) {
                            Text(
                                text = "Rewind ${lastHit.rewindMs}ms | 60Hz",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Main Bottom Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Controls: Joystick for movement / steering
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    VirtualJoystick(
                        onMove = { dx, dy ->
                            viewModel.inputSteer = dx
                            viewModel.inputAccel = -dy // push up is forward
                        }
                    )
                }

                // Center Column: Health Bar, Boost Bar, Helmet/Vest Durability, Weapon Selector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    // Vehicle Drift Score Combo Banner
                    inVeh?.let { veh ->
                        if (veh.driftScore > 10f) {
                            Text(
                                text = "🔥 DRIFT +${veh.driftScore.toInt()} PTS",
                                color = PubgYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Helmet & Vest Durability Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xAA1E293B),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "🪖 Lvl ${player.helmetLevel} (${player.helmetDurability.toInt()})",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xAA1E293B),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "🛡️ Lvl ${player.vestLevel} (${player.vestDurability.toInt()})",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // PUBG Health Bar (White/Green)
                    Column(modifier = Modifier.fillMaxWidth(0.92f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("HP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${player.health.toInt()}/100", color = Color.White, fontSize = 11.sp)
                        }
                        LinearProgressIndicator(
                            progress = { player.health / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (player.health > 35f) PubgGreen else PubgRedZone,
                            trackColor = Color(0x88334155)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Boost Bar (Orange Energy Drink)
                        LinearProgressIndicator(
                            progress = { player.boostPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PubgBoostOrange,
                            trackColor = Color(0x44334155)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Weapon Slots and Quick Meds
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary Weapon Button
                        WeaponCardSlot(
                            name = player.primaryWeapon.nameEn,
                            ammoMag = player.ammoInMagPrimary,
                            ammoReserve = when (player.primaryWeapon.ammoType) {
                                "5.56mm" -> player.reserve556
                                "7.62mm" -> player.reserve762
                                ".300 Mag" -> player.reserve300Mag
                                else -> 0
                            },
                            isSelected = player.activeWeaponSlot == 0,
                            onClick = { viewModel.switchWeapon(0) },
                            testTag = "slot_primary_weapon"
                        )

                        // Secondary Weapon Button
                        WeaponCardSlot(
                            name = player.secondaryWeapon.nameEn,
                            ammoMag = player.ammoInMagSecondary,
                            ammoReserve = player.reserve762,
                            isSelected = player.activeWeaponSlot == 1,
                            onClick = { viewModel.switchWeapon(1) },
                            testTag = "slot_secondary_weapon"
                        )

                        // Pan Melee
                        WeaponCardSlot(
                            name = "Pan",
                            ammoMag = 1,
                            ammoReserve = 0,
                            isSelected = player.activeWeaponSlot == 2,
                            onClick = { viewModel.switchWeapon(2) },
                            testTag = "slot_pan"
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick Meds Bar
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickMedButton("🩹 First Aid (${player.firstAidKits})") { viewModel.useFirstAid() }
                        QuickMedButton("🥤 Boost (${player.energyDrinks})") { viewModel.useEnergyDrink() }
                        QuickMedButton("💉 MedKit (${player.medKits})") { viewModel.useMedKit() }
                    }
                }

                // Right Controls: Fire / Vehicle Pedals
                if (inVeh == null) {
                    // On-Foot Combat Action Controls
                    OnFootActionControls(
                        viewModel = viewModel,
                        player = player,
                        canEnterVehicle = physics.vehicles.any { !it.isDestroyed && hypot(player.x - it.x, player.y - it.y) < 65f }
                    )
                } else {
                    // Vehicle Cockpit Controls (Nitro, Drift, Horn, Gas, Brake)
                    VehicleCockpitControls(
                        vehicle = inVeh,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Game Over / Chicken Dinner Modal Overlay
        if (uiState.isGameOver) {
            GameOverOverlay(
                isVictory = uiState.isVictory,
                kills = player.kills,
                damageDealt = player.damageDealt,
                shotsFired = player.shotsFired,
                shotsHit = player.shotsHit,
                headshots = player.headshotsCount,
                roadkills = player.roadkills,
                driftScore = player.totalDriftScore.toInt(),
                distanceKm = player.distanceDrivenMeters / 1000f,
                serverRegion = uiState.serverRegion,
                isArabic = isArabic,
                onPlayAgain = { viewModel.startMatch(uiState.currentMode) },
                onLobby = onBackToLobby
            )
        }
    }
}

@Composable
fun CompassTape(headingDeg: Float) {
    val normalizedHeading = ((headingDeg % 360f) + 360f) % 360f
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(28.dp)
            .background(Color(0x990F172A), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x55F59E0B), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        val cardinal = when (normalizedHeading.toInt()) {
            in 338..360, in 0..22 -> "N 0°"
            in 23..67 -> "NE 45°"
            in 68..112 -> "E 90°"
            in 113..157 -> "SE 135°"
            in 158..202 -> "S 180°"
            in 203..247 -> "SW 225°"
            in 248..292 -> "W 270°"
            else -> "NW 315°"
        }
        Text(
            text = "▲ $cardinal (${normalizedHeading.toInt()}°)",
            color = PubgYellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MiniMapWidget(
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    zone: CircleZone,
    redZone: RedZone,
    vehicles: List<VehicleInstance>,
    airdrop: Airdrop?
) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(Color(0xDD0F172A))
            .border(2.dp, PubgYellow, CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val mapScale = size.width / GameMap.WORLD_WIDTH

            // White Safe Zone
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = zone.whiteRadius * mapScale,
                center = Offset(zone.whiteCenterX * mapScale, zone.whiteCenterY * mapScale),
                style = Stroke(width = 1.5f)
            )

            // Blue Zone
            drawCircle(
                color = PubgCyan.copy(alpha = 0.7f),
                radius = zone.blueRadius * mapScale,
                center = Offset(zone.blueCenterX * mapScale, zone.blueCenterY * mapScale),
                style = Stroke(width = 2f)
            )

            // Red Zone
            if (redZone.active) {
                drawCircle(
                    color = Color(0x66EF4444),
                    radius = redZone.radius * mapScale,
                    center = Offset(redZone.x * mapScale, redZone.y * mapScale)
                )
            }

            // Vehicles on map
            for (veh in vehicles) {
                if (!veh.isDestroyed) {
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = 2.5f,
                        center = Offset(veh.x * mapScale, veh.y * mapScale)
                    )
                }
            }

            // Airdrop
            airdrop?.let { drop ->
                drawCircle(
                    color = Color.Red,
                    radius = 4f,
                    center = Offset(drop.x * mapScale, drop.y * mapScale)
                )
            }

            // Player Icon & Arrow
            val px = playerX * mapScale
            val py = playerY * mapScale
            drawCircle(color = PubgGreen, radius = 4f, center = Offset(px, py))
            rotate(playerAngle, pivot = Offset(px, py)) {
                drawLine(
                    color = PubgYellow,
                    start = Offset(px, py),
                    end = Offset(px + 10f, py),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}

@Composable
fun KillFeedItem(entry: KillFeedEntry) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (entry.isPlayerInvolved) Color(0xCCB45309) else Color(0xAA1E293B),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.killer,
                color = if (entry.killer == "You") PubgYellow else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = if (entry.isRoadkill) " 💥🚗 " else " 🔫 [${entry.weapon}] ",
                color = Color.LightGray,
                fontSize = 9.sp
            )
            Text(
                text = entry.victim,
                color = Color(0xFFF87171),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun VirtualJoystick(
    onMove: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 55f

    Box(
        modifier = Modifier
            .size(130.dp)
            .background(Color(0x550F172A), CircleShape)
            .border(2.dp, Color(0x6694A3B8), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = knobOffset + dragAmount
                        val dist = newOffset.getDistance()
                        knobOffset = if (dist > maxRadius) {
                            newOffset * (maxRadius / dist)
                        } else {
                            newOffset
                        }
                        onMove(knobOffset.x / maxRadius, knobOffset.y / maxRadius)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Center Knob
        Box(
            modifier = Modifier
                .offset(knobOffset.x.dp, knobOffset.y.dp)
                .size(54.dp)
                .background(PubgYellow.copy(alpha = 0.85f), CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun WeaponCardSlot(
    name: String,
    ammoMag: Int,
    ammoReserve: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PubgYellowDark else Color(0xBB1E293B)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier
            .height(48.dp)
            .border(
                1.dp,
                if (isSelected) PubgYellow else Color(0x3394A3B8),
                RoundedCornerShape(8.dp)
            )
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name.take(6),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.LightGray
            )
            Text(
                text = "$ammoMag/$ammoReserve",
                fontSize = 10.sp,
                color = if (isSelected) PubgYellow else Color.Gray
            )
        }
    }
}

@Composable
fun QuickMedButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB0F172A)),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color.White)
    }
}

@Composable
fun OnFootActionControls(
    viewModel: GameViewModel,
    player: PlayerState,
    canEnterVehicle: Boolean
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // Enter Vehicle Button (visible when near vehicle)
        AnimatedVisibility(
            visible = canEnterVehicle,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            Button(
                onClick = { viewModel.toggleVehicleEntry() },
                colors = ButtonDefaults.buttonColors(containerColor = PubgYellow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).testTag("enter_vehicle_button")
            ) {
                Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        // Tactical Stance & Vaulting Controls Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Jump / Vault Button
            IconButton(
                onClick = { viewModel.triggerJumpOrVault() },
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xAA1E293B), CircleShape)
                    .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
                    .testTag("jump_vault_button")
            ) {
                Text("🧗", fontSize = 18.sp)
            }

            // Crouch Button
            IconButton(
                onClick = { viewModel.toggleCrouch() },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (player.stance == TacticalStance.CROUCHING) PubgYellow else Color(0xAA1E293B),
                        CircleShape
                    )
                    .border(1.5.dp, PubgYellow, CircleShape)
                    .testTag("crouch_button")
            ) {
                Text("🧎", fontSize = 18.sp)
            }

            // Prone Button
            IconButton(
                onClick = { viewModel.toggleProne() },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (player.stance == TacticalStance.PRONE) PubgYellow else Color(0xAA1E293B),
                        CircleShape
                    )
                    .border(1.5.dp, PubgYellow, CircleShape)
                    .testTag("prone_button")
            ) {
                Text("🛌", fontSize = 18.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Throw Grenade Button
            IconButton(
                onClick = { viewModel.throwGrenade() },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xAA1E293B), CircleShape)
                    .border(1.5.dp, if (player.grenadesCount > 0) Color(0xFF10B981) else Color.Gray, CircleShape)
                    .testTag("throw_grenade_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💣", fontSize = 15.sp)
                    Text("${player.grenadesCount}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Flare Gun Button
            if (player.flareGunsCount > 0) {
                IconButton(
                    onClick = { viewModel.fireFlareGun() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xCCB91C1C), CircleShape)
                        .border(1.5.dp, PubgYellow, CircleShape)
                        .testTag("fire_flare_button")
                ) {
                    Text("🚀", fontSize = 18.sp)
                }
            }

            // Scope ADS Toggle
            IconButton(
                onClick = { viewModel.toggleScope() },
                modifier = Modifier
                    .size(52.dp)
                    .background(if (player.isScoping) PubgYellow else Color(0xAA1E293B), CircleShape)
                    .border(1.5.dp, PubgYellow, CircleShape)
                    .testTag("ads_scope_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = "Scope",
                    tint = if (player.isScoping) Color.Black else Color.White
                )
            }

            // Reload Button
            IconButton(
                onClick = { viewModel.triggerReload() },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xAA1E293B), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .testTag("reload_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
            }
        }

        // Main Fire Button
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(PubgRedZone, CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { viewModel.inputShooting = true },
                        onDragEnd = { viewModel.inputShooting = false },
                        onDragCancel = { viewModel.inputShooting = false },
                        onDrag = { change, _ -> change.consume() }
                    )
                }
                .testTag("fire_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = "Fire",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun VehicleCockpitControls(
    vehicle: VehicleInstance,
    viewModel: GameViewModel
) {
    val speedKmh = abs(vehicle.speed) * 10f

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // Digital Speedometer & Cockpit Gauge
        Row(
            modifier = Modifier
                .background(Color(0xCC0F172A), RoundedCornerShape(10.dp))
                .border(1.5.dp, PubgCyan, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${speedKmh.toInt()}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = PubgYellow
                )
                Text(
                    text = "KM/H",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = PubgTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "⛽ Fuel: ${vehicle.fuel.toInt()}%",
                    fontSize = 10.sp,
                    color = if (vehicle.fuel > 20f) PubgGreen else PubgRedZone
                )
                Text(
                    text = "🛠️ Armor: ${vehicle.health.toInt()}",
                    fontSize = 10.sp,
                    color = Color.White
                )
                Text(
                    text = "🚀 Nitro: ${vehicle.nitro.toInt()}%",
                    fontSize = 10.sp,
                    color = PubgBoostOrange
                )
            }
        }

        // Upper Vehicle Actions: Horn, Exit Vehicle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Horn
            IconButton(
                onClick = { viewModel.honkHorn() },
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xAA1E293B), CircleShape)
                    .border(1.dp, PubgYellow, CircleShape)
                    .testTag("vehicle_horn_button")
            ) {
                Icon(imageVector = Icons.Default.Campaign, contentDescription = "Horn", tint = PubgYellow)
            }

            // Exit Vehicle
            Button(
                onClick = { viewModel.toggleVehicleEntry() },
                colors = ButtonDefaults.buttonColors(containerColor = PubgRedZone),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(46.dp).testTag("exit_vehicle_button")
            ) {
                Text("خروج 🚪", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Nitro Boost & Handbrake (Drift) Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Nitro Boost Button
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.inputNitro) PubgBoostOrange else Color(0xCCEA580C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(width = 68.dp, height = 50.dp)
                    .border(2.dp, Color(0xFFFDBA74), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.inputNitro = true },
                            onDragEnd = { viewModel.inputNitro = false },
                            onDragCancel = { viewModel.inputNitro = false },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }
                    .testTag("nitro_boost_button")
            ) {
                Text("🚀 نيترو", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            // Handbrake (Drift) Button
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.inputHandbrake) PubgYellow else Color(0xAA334155)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(width = 68.dp, height = 50.dp)
                    .border(2.dp, PubgYellow, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.inputHandbrake = true },
                            onDragEnd = { viewModel.inputHandbrake = false },
                            onDragCancel = { viewModel.inputHandbrake = false },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }
                    .testTag("handbrake_drift_button")
            ) {
                Text("دريفت 🏎️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Gas & Brake Pedals
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Brake / Reverse Pedal
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.inputBrake) PubgRedZone else Color(0xAA475569)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(width = 68.dp, height = 54.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.inputBrake = true },
                            onDragEnd = { viewModel.inputBrake = false },
                            onDragCancel = { viewModel.inputBrake = false },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }
                    .testTag("brake_pedal_button")
            ) {
                Text("فرامل 🛑", fontSize = 11.sp, color = Color.White)
            }

            // Gas Pedal (Accelerate)
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.inputAccel > 0f) PubgGreen else Color(0xFF059669)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(width = 76.dp, height = 62.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { viewModel.inputAccel = 1.0f },
                            onDragEnd = { viewModel.inputAccel = 0.0f },
                            onDragCancel = { viewModel.inputAccel = 0.0f },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }
                    .testTag("gas_pedal_button")
            ) {
                Text("دواسة ⚡", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
fun ScopeOverlay(scopeType: ScopeType) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.75f

        // Dark vignette border outside lens
        drawRect(color = Color(0xCC000000))
        drawCircle(
            color = Color.Transparent,
            radius = radius,
            center = Offset(cx, cy),
            blendMode = BlendMode.Clear
        )

        // Outer scope ring
        drawCircle(
            color = Color(0xFF0F172A),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 24.dp.toPx())
        )

        if (scopeType == ScopeType.RED_DOT) {
            // Red dot optic
            drawCircle(color = Color.Red, radius = 4.dp.toPx(), center = Offset(cx, cy))
            drawCircle(
                color = Color.Red.copy(alpha = 0.5f),
                radius = 18.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        } else {
            // Sniper 4x/8x reticle with mil-dots
            drawLine(
                color = Color.Black,
                start = Offset(cx - radius * 0.85f, cy),
                end = Offset(cx + radius * 0.85f, cy),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.Black,
                start = Offset(cx, cy - radius * 0.85f),
                end = Offset(cx, cy + radius * 0.85f),
                strokeWidth = 2.dp.toPx()
            )
            // Rangefinder hashes
            for (step in 1..4) {
                val dist = step * 35.dp.toPx()
                drawLine(
                    color = Color.Black,
                    start = Offset(cx - 12.dp.toPx(), cy + dist),
                    end = Offset(cx + 12.dp.toPx(), cy + dist),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            // Center illuminated amber dot
            drawCircle(color = PubgYellow, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
        }
    }
}

@Composable
fun GameOverOverlay(
    isVictory: Boolean,
    kills: Int,
    damageDealt: Float,
    shotsFired: Int,
    shotsHit: Int,
    headshots: Int,
    roadkills: Int,
    driftScore: Int,
    distanceKm: Float,
    serverRegion: ServerRegion,
    isArabic: Boolean,
    onPlayAgain: () -> Unit,
    onLobby: () -> Unit
) {
    val accuracyPercent = if (shotsFired > 0) ((shotsHit.toFloat() / shotsFired.toFloat()) * 100).toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE000000))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .border(2.dp, if (isVictory) PubgYellow else PubgRedZone, RoundedCornerShape(16.dp))
                .testTag("game_over_card"),
            colors = CardDefaults.cardColors(containerColor = PubgCardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isVictory) {
                    Image(
                        painter = painterResource(id = R.drawable.chicken_dinner_victory_1792865047354),
                        contentDescription = "Chicken Dinner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) "عشاء دجاج الليلة! فوز ساحق 🏆" else "WINNER WINNER CHICKEN DINNER! 🏆",
                        color = PubgYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = if (isArabic) "حظ أوفر في المعركة القادمة! 💀" else "BETTER LUCK NEXT TIME! 💀",
                        color = PubgRedZone,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Match Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(title = if (isArabic) "القتلات" else "KILLS", value = "$kills")
                    StatPill(title = if (isArabic) "الضرر" else "DAMAGE", value = "${damageDealt.toInt()}")
                    StatPill(title = if (isArabic) "دهس مركبة" else "ROADKILLS", value = "$roadkills")
                    StatPill(title = if (isArabic) "دريفت" else "DRIFT", value = "$driftScore")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Combat Precision & Rank Points Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(title = if (isArabic) "دقة الرمي" else "ACCURACY", value = "$accuracyPercent%")
                    StatPill(title = if (isArabic) "رأسية" else "HEADSHOTS", value = "$headshots")
                    StatPill(title = if (isArabic) "الترتيب" else "RANK", value = if (isVictory) "#1 / 100" else "#${(4..18).random()} / 100")
                    StatPill(title = if (isArabic) "نقاط التقييم" else "RATING", value = if (isVictory) "+38 RP" else "+12 RP")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dedicated Server Netcode Telemetry Card
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33F59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UE5 DEDICATED SERVER TELEMETRY",
                                color = PubgYellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${serverRegion.flagEmoji} ${serverRegion.dataCenter}",
                                color = PubgCyan,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tick: ${serverRegion.tickRate}.0 Hz • Ping: ${serverRegion.basePingMs}ms • Loss: 0.0%",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (isArabic) "تزامن موثق 100% ⚡" else "100% Rewind Validated",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLobby,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_lobby_button")
                    ) {
                        Text(if (isArabic) "الصالة" else "Lobby")
                    }

                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = PubgYellow),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_play_again_button")
                    ) {
                        Text(
                            text = if (isArabic) "معركة جديدة 🔄" else "Play Again 🔄",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(title, color = PubgTextSecondary, fontSize = 9.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// Canvas Helpers for Vehicles, Bots, and Player
private fun DrawScope.drawVehicleOnCanvas(veh: VehicleInstance, vx: Float, vy: Float, scale: Float) {
    rotate(veh.angleDeg, pivot = Offset(vx, vy)) {
        val w = 55f * scale
        val h = 28f * scale

        // Drop shadow
        drawRect(
            color = Color(0x66000000),
            topLeft = Offset(vx - w / 2f + 5f * scale, vy - h / 2f + 5f * scale),
            size = Size(w, h)
        )

        // Headlights beam when driving
        if (veh.headlightsOn && !veh.isDestroyed) {
            val beamPath = Path().apply {
                moveTo(vx + w / 2f, vy - h * 0.3f)
                lineTo(vx + w / 2f + 160f * scale, vy - h * 1.5f)
                lineTo(vx + w / 2f + 160f * scale, vy + h * 1.5f)
                lineTo(vx + w / 2f, vy + h * 0.3f)
                close()
            }
            drawPath(
                path = beamPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x44FEF08A), Color.Transparent),
                    startX = vx + w / 2f,
                    endX = vx + w / 2f + 160f * scale
                )
            )
        }

        // Tires (4 wheels)
        val wheelW = 14f * scale
        val wheelH = 6f * scale
        val wheelColor = Color(0xFF0F172A)
        drawRect(wheelColor, Offset(vx - w * 0.35f, vy - h * 0.65f), Size(wheelW, wheelH))
        drawRect(wheelColor, Offset(vx + w * 0.20f, vy - h * 0.65f), Size(wheelW, wheelH))
        drawRect(wheelColor, Offset(vx - w * 0.35f, vy + h * 0.45f), Size(wheelW, wheelH))
        drawRect(wheelColor, Offset(vx + w * 0.20f, vy + h * 0.45f), Size(wheelW, wheelH))

        // Vehicle Chassis
        val bodyColor = if (veh.isDestroyed) Color(0xFF334155) else veh.model.defaultColor
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(vx - w / 2f, vy - h / 2f),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
        )

        // Windshield and roof
        drawRoundRect(
            color = if (veh.isDestroyed) Color(0xFF1E293B) else Color(0xFF0284C7),
            topLeft = Offset(vx - w * 0.15f, vy - h * 0.35f),
            size = Size(w * 0.45f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale, 3f * scale)
        )

        // Health bar above vehicle
        if (veh.health < veh.maxHealth && !veh.isDestroyed) {
            val barW = 40f * scale
            val barH = 4f * scale
            drawRect(Color(0x88000000), Offset(vx - barW / 2f, vy - h * 0.9f), Size(barW, barH))
            drawRect(
                Color(0xFF10B981),
                Offset(vx - barW / 2f, vy - h * 0.9f),
                Size(barW * (veh.health / veh.maxHealth), barH)
            )
        }
    }
}

private fun DrawScope.drawBotOnCanvas(bot: BotEntity, bx: Float, by: Float, scale: Float) {
    rotate(bot.angleDeg, pivot = Offset(bx, by)) {
        val r = 14f * scale
        // Body shadow
        drawCircle(Color(0x44000000), radius = r, center = Offset(bx + 3f, by + 3f))
        // Bot Torso (tactical desert camo)
        drawCircle(Color(0xFF78350F), radius = r, center = Offset(bx, by))
        // Weapon in hands
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(bx, by),
            end = Offset(bx + r * 1.8f, by),
            strokeWidth = 3f * scale,
            cap = StrokeCap.Round
        )
        // Red helmet for enemy bot
        drawCircle(Color(0xFFB91C1C), radius = r * 0.65f, center = Offset(bx, by))

        // Health bar
        val barW = 26f * scale
        drawRect(Color(0x88000000), Offset(bx - barW / 2f, by - r * 1.5f), Size(barW, 3f * scale))
        drawRect(
            Color(0xFFEF4444),
            Offset(bx - barW / 2f, by - r * 1.5f),
            Size(barW * (bot.health / 100f), 3f * scale)
        )
    }
}

private fun DrawScope.drawPlayerOnCanvas(player: PlayerState, px: Float, py: Float, scale: Float) {
    rotate(player.angleDeg, pivot = Offset(px, py)) {
        val baseR = when (player.stance) {
            TacticalStance.STANDING -> 15f * scale
            TacticalStance.CROUCHING -> 12f * scale
            TacticalStance.PRONE -> 9f * scale
        }

        // Vaulting leap parkour trail
        if (player.isVaulting) {
            drawCircle(Color(0x88F59E0B), radius = baseR * 1.7f, center = Offset(px, py), style = Stroke(width = 3f * scale))
        }

        // Prone Crawling Soldier
        if (player.stance == TacticalStance.PRONE) {
            // Shadow
            drawOval(Color(0x44000000), Offset(px - baseR * 2.2f + 3f, py - baseR + 3f), Size(baseR * 4f, baseR * 2f))
            // Crawling legs & body
            drawRoundRect(
                Color(0xFF1E3A8A),
                Offset(px - baseR * 2f, py - baseR * 0.8f),
                Size(baseR * 3.5f, baseR * 1.6f),
                androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale)
            )
            // Sniper rifle barrel extended in front
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(px, py),
                end = Offset(px + baseR * 3.2f, py),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )
            // Helmet
            drawCircle(Color(0xFF1E293B), radius = baseR * 0.75f, center = Offset(px + baseR * 0.5f, py))
            return@rotate
        }

        // Standing / Crouching rendering
        drawCircle(Color(0x55000000), radius = baseR, center = Offset(px + 4f, py + 4f))
        drawCircle(Color(0xFF1E3A8A), radius = baseR, center = Offset(px, py))
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(px, py),
            end = Offset(px + baseR * 2.2f, py),
            strokeWidth = 4f * scale,
            cap = StrokeCap.Round
        )
        val helmetColor = when (player.helmetLevel) {
            3 -> Color(0xFF1E293B)
            2 -> Color(0xFF334155)
            else -> Color(0xFF475569)
        }
        drawCircle(helmetColor, radius = baseR * 0.7f, center = Offset(px, py))
        drawLine(
            color = PubgCyan,
            start = Offset(px + baseR * 0.2f, py - baseR * 0.35f),
            end = Offset(px + baseR * 0.2f, py + baseR * 0.35f),
            strokeWidth = 2f * scale
        )
    }
}
