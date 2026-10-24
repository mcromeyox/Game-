package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onStartGame: (GameMode) -> Unit,
    onNavigateToGarage: () -> Unit,
    onNavigateToArmory: () -> Unit,
    onNavigateToCareer: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isArabic = uiState.isArabic
    var selectedMode by remember { mutableStateOf(uiState.currentMode) }
    var showRegionPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PubgDarkBg,
        bottomBar = {
            LobbyBottomBar(
                isArabic = isArabic,
                currentTab = "LOBBY",
                onTabSelect = { tab ->
                    when (tab) {
                        "GARAGE" -> onNavigateToGarage()
                        "ARMORY" -> onNavigateToArmory()
                        "CAREER" -> onNavigateToCareer()
                        "SETTINGS" -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar / User Profile Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar and Rank
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(2.dp, PubgYellow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎖️", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Apex_Warrior",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PubgYellowDark
                            ) {
                                Text(
                                    text = if (isArabic) "غازي ⭐" else "CONQUEROR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isArabic) "المستوى 48 • موسم 2026" else "Level 48 • Season 2026",
                            color = PubgTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Language & Sound Quick Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("toggle_language_button")
                    ) {
                        Text(
                            text = if (isArabic) "EN" else "عربي",
                            color = PubgYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("toggle_sound_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound",
                            tint = if (uiState.soundEnabled) PubgCyan else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Hero Banner with Generated PUBG Vehicle Key Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, PubgYellow.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.battle_lobby_hero_1792865034504),
                    contentDescription = "PUBG Battle Apex Hero",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Gradient Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xDD0A0E17))
                            )
                        )
                )

                // Banner text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isArabic) "الموسم الجديد: سرعة المعركة القصوى" else "VELOCITY WARFARE: SEASON APEX",
                        color = PubgYellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isArabic) "فيزياء قيادة متطورة • جرافيك واقعي • دريفت وقفزات نارية" else "Enhanced Vehicle Physics • Ultra Visuals • Stunt & Nitro Action",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dedicated Server Datacenter Selector Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showRegionPicker = true }
                    .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp))
                    .testTag("server_region_selector"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.serverRegion.flagEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) uiState.serverRegion.nameAr else uiState.serverRegion.nameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF0F766E),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "60Hz UE5",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.serverRegion.dataCenter} • ${uiState.serverRegion.basePingMs}ms Latency",
                                color = PubgCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "تغيير ▾" else "REGION ▾",
                            color = PubgYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Game Mode Selection Cards
            Text(
                text = if (isArabic) "اختر نمط اللعب:" else "SELECT GAME MODE:",
                color = PubgYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PubgYellow else PubgCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("mode_${mode.name.lowercase()}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E293B) else PubgCardBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (mode) {
                                    GameMode.BATTLE_ROYALE -> "🪂"
                                    GameMode.VEHICLE_WARFARE -> "🏎️"
                                    GameMode.FIRING_RANGE -> "🎯"
                                },
                                fontSize = 28.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) mode.titleAr else mode.titleEn,
                                    color = if (isSelected) PubgYellow else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isArabic) mode.descriptionAr else mode.descriptionEn,
                                    color = PubgTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMode = mode },
                                colors = RadioButtonDefaults.colors(selectedColor = PubgYellow)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equipped Vehicle & Weapon Quick Preview Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vehicle Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToGarage() }
                        .border(1.dp, PubgCardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isArabic) "مركبتك الحالية" else "VEHICLE", color = PubgTextSecondary, fontSize = 10.sp)
                            Text(uiState.selectedVehicleModel.iconEmoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) uiState.selectedVehicleModel.nameAr else uiState.selectedVehicleModel.nameEn,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${uiState.selectedVehicleModel.maxSpeedKmh.toInt()} km/h • Nitro Ready",
                            color = PubgCyan,
                            fontSize = 10.sp
                        )
                    }
                }

                // Weapon Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToArmory() }
                        .border(1.dp, PubgCardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isArabic) "السلاح المفضل" else "WEAPON", color = PubgTextSecondary, fontSize = 10.sp)
                            Text("🔫", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) uiState.selectedWeaponModel.nameAr else uiState.selectedWeaponModel.nameEn,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${uiState.selectedWeaponModel.ammoType} • Scope 4x",
                            color = PubgYellow,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Big Action Button: START MATCH
            Button(
                onClick = {
                    viewModel.startMatchmaking(selectedMode) {
                        onStartGame(selectedMode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PubgYellow),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .testTag("start_match_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "بحث خادم مخصص ودخول المعركة ⚡" else "MATCHMAKE & DEPLOY ⚡",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Region Selector Dialog
        if (showRegionPicker) {
            RegionSelectorDialog(
                currentRegion = uiState.serverRegion,
                isArabic = isArabic,
                onSelectRegion = {
                    viewModel.setServerRegion(it)
                    showRegionPicker = false
                },
                onDismiss = { showRegionPicker = false }
            )
        }

        // Dedicated Server Matchmaking Modal
        if (uiState.isMatchmakingDialogVisible) {
            MatchmakingDialog(
                status = uiState.matchmakingStatus,
                isArabic = isArabic,
                onCancel = { viewModel.cancelMatchmaking() }
            )
        }
    }
}

@Composable
fun LobbyBottomBar(
    isArabic: Boolean,
    currentTab: String,
    onTabSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = PubgCardBg,
        contentColor = PubgTextPrimary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == "LOBBY",
            onClick = { onTabSelect("LOBBY") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Lobby") },
            label = { Text(if (isArabic) "الصالة" else "Lobby", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PubgYellow,
                selectedTextColor = PubgYellow,
                indicatorColor = Color(0x33F59E0B)
            )
        )

        NavigationBarItem(
            selected = currentTab == "GARAGE",
            onClick = { onTabSelect("GARAGE") },
            icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Garage") },
            label = { Text(if (isArabic) "المركبات" else "Garage", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PubgYellow,
                selectedTextColor = PubgYellow,
                indicatorColor = Color(0x33F59E0B)
            )
        )

        NavigationBarItem(
            selected = currentTab == "ARMORY",
            onClick = { onTabSelect("ARMORY") },
            icon = { Icon(Icons.Default.Shield, contentDescription = "Armory") },
            label = { Text(if (isArabic) "الأسلحة" else "Armory", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PubgYellow,
                selectedTextColor = PubgYellow,
                indicatorColor = Color(0x33F59E0B)
            )
        )

        NavigationBarItem(
            selected = currentTab == "CAREER",
            onClick = { onTabSelect("CAREER") },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Career") },
            label = { Text(if (isArabic) "الإحصائيات" else "Career", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PubgYellow,
                selectedTextColor = PubgYellow,
                indicatorColor = Color(0x33F59E0B)
            )
        )

        NavigationBarItem(
            selected = currentTab == "SETTINGS",
            onClick = { onTabSelect("SETTINGS") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text(if (isArabic) "الإعدادات" else "Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PubgYellow,
                selectedTextColor = PubgYellow,
                indicatorColor = Color(0x33F59E0B)
            )
        )
    }
}

@Composable
fun RegionSelectorDialog(
    currentRegion: ServerRegion,
    isArabic: Boolean,
    onSelectRegion: (ServerRegion) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, PubgYellow, RoundedCornerShape(16.dp))
                .testTag("region_selector_modal"),
            colors = CardDefaults.cardColors(containerColor = PubgCardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "اختر منطقة الخادم المخصص" else "SELECT DEDICATED SERVER REGION",
                            color = PubgYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isArabic) "خوادم UE5 بسرعة 60Hz للتزامن الفائق" else "60Hz Dedicated Servers • Zero-Lag Netcode",
                            color = PubgTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_regions")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServerRegion.entries.forEach { region ->
                        val isSelected = currentRegion == region
                        val pingColor = when {
                            region.basePingMs < 40 -> Color(0xFF10B981)
                            region.basePingMs < 80 -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRegion(region) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PubgYellow else PubgCardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("region_item_${region.id}"),
                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(region.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isArabic) region.nameAr else region.nameEn,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${region.dataCenter} • ${region.tickRate}Hz Tick",
                                            color = PubgTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = pingColor.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, pingColor)
                                    ) {
                                        Text(
                                            text = "${region.basePingMs}ms",
                                            color = pingColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectRegion(region) },
                                        colors = RadioButtonDefaults.colors(selectedColor = PubgYellow)
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

@Composable
fun MatchmakingDialog(
    status: MatchmakingStatus,
    isArabic: Boolean,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(2.dp, PubgYellow, RoundedCornerShape(18.dp))
                .testTag("matchmaking_modal"),
            colors = CardDefaults.cardColors(containerColor = PubgCardBg),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F766E)
                ) {
                    Text(
                        text = "UE5 DEDICATED SERVER • 60Hz NETCODE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Radar / Server icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(2.dp, PubgYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (status.stage) {
                            MatchmakingStage.FINDING_PLAYERS -> "🌐"
                            MatchmakingStage.ALLOCATING_SERVER -> "⚡"
                            MatchmakingStage.SYNCING_UE5_LUMEN -> "🗺️"
                            MatchmakingStage.READY -> "🎯"
                            else -> "🔍"
                        },
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) "جاري البحث عن لاعبين ومطابقة الخادم" else "MATCHMAKING IN PROGRESS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )

                Text(
                    text = "${status.serverRegion.flagEmoji} ${if (isArabic) status.serverRegion.nameAr else status.serverRegion.nameEn} (${status.serverRegion.basePingMs}ms)",
                    color = PubgYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                val progress = (status.playersFound.toFloat() / status.maxPlayers.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PubgYellow,
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "اللاعبين: ${status.playersFound} / ${status.maxPlayers}" else "Players: ${status.playersFound} / ${status.maxPlayers}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = PubgCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Stage Technical Details
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val stageTitle = when (status.stage) {
                            MatchmakingStage.FINDING_PLAYERS -> if (isArabic) "البحث عن 100 لاعب بنظام MMR المتكافئ..." else "Matching 100 players via MMR rating..."
                            MatchmakingStage.ALLOCATING_SERVER -> if (isArabic) "تخصيص خادم UE5 مخصص (Port 7777 - 60Hz)..." else "Allocating UE5 Server instance (Port 7777 - 60Hz)..."
                            MatchmakingStage.SYNCING_UE5_LUMEN -> if (isArabic) "مزامنة خريطة Erangel بمساحة 8x8km وإضاءة Lumen..." else "Streaming Erangel 8x8km World Partition & Lumen..."
                            MatchmakingStage.READY -> if (isArabic) "اكتمل التزامن! الدخول للمباراة..." else "Match Synchronized! Deploying..."
                            else -> "Initializing..."
                        }
                        Text(
                            text = stageTitle,
                            color = PubgTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_cancel_matchmaking")
                ) {
                    Text(
                        text = if (isArabic) "إلغاء البحث" else "CANCEL SEARCH",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

