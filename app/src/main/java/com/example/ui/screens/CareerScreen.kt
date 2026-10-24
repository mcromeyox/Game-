package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val isArabic = uiState.isArabic

    val winRate = if (uiState.totalMatches > 0) {
        ((uiState.totalWins.toFloat() / uiState.totalMatches) * 100).toInt()
    } else 0

    val kdRatio = if (uiState.totalMatches > 0) {
        String.format("%.2f", uiState.totalKills.toFloat() / maxOf(1, uiState.totalMatches - uiState.totalWins))
    } else "0.0"

    Scaffold(
        containerColor = PubgDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isArabic) "السجل القتالي وإحصائيات المسيرة" else "CAREER STATS & COMBAT RECORD",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("career_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PubgCardBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Rank Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PubgYellow, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👑", fontSize = 42.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isArabic) "تصنيف الغازي الأسطوري" else "CONQUEROR TIER",
                            color = PubgYellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isArabic) "الترتيب الأعلى في خادم الشرق الأوسط" else "Rank #42 • Top Tier Leaderboard",
                            color = PubgTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Stat Metrics Grid
            Text(
                text = if (isArabic) "إحصائيات المعارك الإجمالية:" else "SEASON COMBAT METRICS:",
                color = PubgYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "عشاء دجاج (فوز)" else "CHICKEN DINNERS",
                    value = "${uiState.totalWins}",
                    subtitle = if (isArabic) "معدل الفوز $winRate%" else "Win Rate $winRate%",
                    icon = "🏆",
                    color = PubgYellow
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "إجمالي القتلات" else "TOTAL KILLS",
                    value = "${uiState.totalKills}",
                    subtitle = "K/D: $kdRatio",
                    icon = "🎯",
                    color = PubgRedZone
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "قتلات الدهس بالمركبة" else "ROADKILLS",
                    value = "${uiState.totalRoadkills}",
                    subtitle = if (isArabic) "اصطدامات مميتة" else "Vehicle Impacts",
                    icon = "💥",
                    color = PubgBoostOrange
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = if (isArabic) "أعلى نقاط دريفت" else "TOP DRIFT SCORE",
                    value = "${uiState.topDriftScore}",
                    subtitle = if (isArabic) "انزلاق احترافي" else "Max Drift Combo",
                    icon = "🏎️",
                    color = PubgCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            MetricCard(
                modifier = Modifier.fillMaxWidth(),
                title = if (isArabic) "إجمالي مسافة قيادة المركبات" else "TOTAL DISTANCE DRIVEN IN VEHICLES",
                value = String.format("%.1f KM", uiState.totalDistanceKm),
                subtitle = if (isArabic) "قيادة على الطرق والوعرة والقفزات" else "Highway Drifting & Off-Road Stunts",
                icon = "🛣️",
                color = PubgGreen
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Badges & Achievements
            Text(
                text = if (isArabic) "الأوسمة والجوائز المكتسبة:" else "TACTICAL ACHIEVEMENTS & MEDALS:",
                color = PubgYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AchievementBadgeItem(
                    emoji = "🍗",
                    title = if (isArabic) "سيد عشاء الدجاج" else "Chicken Dinner Master",
                    desc = if (isArabic) "حقق الفوز في معارك الباتل رويال بصفتك الناجي الأخير" else "Won survivor matches as the last warrior standing",
                    unlocked = true
                )
                AchievementBadgeItem(
                    emoji = "🏎️",
                    title = if (isArabic) "ملك الدريفت والسرعة" else "Apex Drift King",
                    desc = if (isArabic) "سجل أكثر من 3000 نقطة دريفت في جولة واحدة" else "Accumulated over 3,000 drift points in a single match",
                    unlocked = true
                )
                AchievementBadgeItem(
                    emoji = "🚗",
                    title = if (isArabic) "مصفح الطرقات" else "Road Dominator",
                    desc = if (isArabic) "دهس أكثر من 15 عدواً باستخدام جيب UAZ أو الباجي" else "Eliminated 15+ enemies using vehicle ramming",
                    unlocked = true
                )
                AchievementBadgeItem(
                    emoji = "🎯",
                    title = if (isArabic) "قناص الدروب" else "Airdrop Marksman",
                    desc = if (isArabic) "احصل على قناصة AWM من الصندوق الجوي" else "Looted AWM crate sniper with 8x optical scope",
                    unlocked = true
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, PubgCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = PubgCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = PubgTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun AchievementBadgeItem(
    emoji: String,
    title: String,
    desc: String,
    unlocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (unlocked) PubgYellow.copy(alpha = 0.5f) else PubgCardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = PubgCardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = if (unlocked) PubgYellow else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(desc, color = PubgTextSecondary, fontSize = 11.sp)
            }
        }
    }
}
