package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val isArabic = uiState.isArabic

    var selectedGraphics by remember { mutableStateOf(uiState.graphicsQuality) }
    var selectedControls by remember { mutableStateOf(uiState.vehicleControlScheme) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var antiAliasing by remember { mutableStateOf(true) }
    var shadowsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = PubgDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isArabic) "إعدادات اللعبة والجرافيك والتحكم" else "GRAPHICS & CONTROL SETTINGS",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "لغة الواجهة (Language)" else "INTERFACE LANGUAGE",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { if (!isArabic) viewModel.toggleLanguage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isArabic) PubgYellow else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("lang_arabic_button")
                        ) {
                            Text("العربية 🇸🇦", color = if (isArabic) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { if (isArabic) viewModel.toggleLanguage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isArabic) PubgYellow else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("lang_english_button")
                        ) {
                            Text("English 🇬🇧", color = if (!isArabic) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Graphics Quality Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "إعدادات الرسومات ومعدل الإطارات (Graphics & FPS)" else "GRAPHICS QUALITY & FPS",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val graphicsOptions = listOf(
                        "Smooth 90FPS" to if (isArabic) "سلس 90 إطار/ث" else "Smooth 90 FPS",
                        "Ultra 60FPS" to if (isArabic) "فائق 60 إطار/ث (موصى به)" else "Ultra HD 60 FPS",
                        "HDR Extreme" to if (isArabic) "HDR أقصى جودة" else "HDR Extreme Visuals"
                    )

                    graphicsOptions.forEach { (key, label) ->
                        val isSelected = selectedGraphics == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGraphics = key }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedGraphics = key },
                                colors = RadioButtonDefaults.colors(selectedColor = PubgYellow)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = if (isSelected) PubgYellow else Color.White, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PubgCardBorder)

                    // Anti-aliasing toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArabic) "تنعيم الحواف (Anti-Aliasing)" else "Anti-Aliasing (MSAA)", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = antiAliasing,
                            onCheckedChange = { antiAliasing = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PubgYellow, checkedTrackColor = PubgYellowDark)
                        )
                    }

                    // Dynamic Shadows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArabic) "ظلال المركبات الديناميكية" else "Dynamic Vehicle Shadows", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = shadowsEnabled,
                            onCheckedChange = { shadowsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PubgYellow, checkedTrackColor = PubgYellowDark)
                        )
                    }
                }
            }

            // Vehicle Controls Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "أسلوب قيادة المركبات (Vehicle Controls)" else "VEHICLE DRIVING CONTROLS",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedControls = "PEDALS"
                                    viewModel.setVehicleControlScheme("PEDALS")
                                }
                                .border(
                                    width = if (selectedControls == "PEDALS") 2.dp else 1.dp,
                                    color = if (selectedControls == "PEDALS") PubgYellow else PubgCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedControls == "PEDALS") Color(0xFF1E293B) else PubgDarkBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🏎️ دواسـات + دريفت", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("دواسة سرعة، فرامل، وزر نيترو ودريفت يدوي", fontSize = 10.sp, color = PubgTextSecondary)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedControls = "JOYSTICK"
                                    viewModel.setVehicleControlScheme("JOYSTICK")
                                }
                                .border(
                                    width = if (selectedControls == "JOYSTICK") 2.dp else 1.dp,
                                    color = if (selectedControls == "JOYSTICK") PubgYellow else PubgCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedControls == "JOYSTICK") Color(0xFF1E293B) else PubgDarkBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🕹️ عصا التحكم الافتراضية", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("توجيه وتسارع عبر المقبض الديناميكي", fontSize = 10.sp, color = PubgTextSecondary)
                            }
                        }
                    }
                }
            }

            // Audio & Haptics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "الصوت والاهتزاز التكتيكي (Haptics)" else "AUDIO & TACTICAL HAPTICS",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArabic) "المؤثرات الصوتية والمحرك" else "Master Game Audio & Engine FX", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = uiState.soundEnabled,
                            onCheckedChange = { viewModel.toggleSound() },
                            colors = SwitchDefaults.colors(checkedThumbColor = PubgYellow, checkedTrackColor = PubgYellowDark)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArabic) "اهتزاز الارتداد والاصطدام (Vibration)" else "Gun Recoil & Vehicle Crash Haptics", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = { hapticsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PubgYellow, checkedTrackColor = PubgYellowDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
