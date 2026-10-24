package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttachmentGrip
import com.example.model.AttachmentMag
import com.example.model.AttachmentMuzzle
import com.example.model.ScopeType
import com.example.model.WeaponModel
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmoryScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val isArabic = uiState.isArabic
    var selectedWeapon by remember { mutableStateOf(uiState.selectedWeaponModel) }
    var selectedScope by remember { mutableStateOf(uiState.player.currentScope) }
    var selectedMuzzle by remember { mutableStateOf(uiState.player.muzzleAttachment) }
    var selectedGrip by remember { mutableStateOf(uiState.player.gripAttachment) }
    var selectedMag by remember { mutableStateOf(uiState.player.magAttachment) }

    val totalRecoilReduction = selectedMuzzle.recoilReduction + selectedGrip.recoilReduction
    val effectiveRecoil = (selectedWeapon.recoil * (1f - totalRecoilReduction)).coerceAtLeast(0.05f)
    val effectiveMagCapacity = selectedWeapon.magCapacity + selectedMag.extraCapacity

    Scaffold(
        containerColor = PubgDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isArabic) "مستودع الأسلحة والمعدات" else "ARMORY & WEAPON WORKBENCH",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("armory_back_button")) {
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
            // Weapon Selection Horizontal Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WeaponModel.entries.forEach { weapon ->
                    val isSelected = selectedWeapon == weapon
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedWeapon = weapon },
                        label = { Text(weapon.id.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PubgYellow,
                            selectedLabelColor = Color.Black,
                            containerColor = PubgCardBg,
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("armory_chip_${weapon.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weapon Showcase Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PubgYellow.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) selectedWeapon.nameAr else selectedWeapon.nameEn,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Caliber: ${selectedWeapon.ammoType} • Mag: $effectiveMagCapacity Rounds",
                        color = PubgCyan,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stylized Gun Icon / Graphic Silhouette
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedWeapon.isMelee) "🍳" else if (selectedWeapon.isSniper) "🎯" else "🔫",
                            fontSize = 44.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats
                    WeaponStatBar(
                        label = if (isArabic) "قوة الضرر" else "Hit Damage",
                        value = "${selectedWeapon.damage.toInt()}",
                        progress = selectedWeapon.damage / 110f,
                        color = PubgRedZone
                    )

                    WeaponStatBar(
                        label = if (isArabic) "معدل الإطلاق" else "Fire Rate",
                        value = "${selectedWeapon.fireRateMs} ms",
                        progress = (1000f - selectedWeapon.fireRateMs) / 1000f,
                        color = PubgYellow
                    )

                    WeaponStatBar(
                        label = if (isArabic) "سرعة الرصاصة" else "Bullet Velocity",
                        value = "${selectedWeapon.bulletSpeed.toInt()} m/s",
                        progress = selectedWeapon.bulletSpeed / 60f,
                        color = PubgCyan
                    )

                    WeaponStatBar(
                        label = if (isArabic) "ثبات الارتداد (مع الملحقات)" else "Recoil Control (w/ Attachments)",
                        value = "${((1f - effectiveRecoil) * 100).toInt()}% (+${(totalRecoilReduction * 100).toInt()}%)",
                        progress = 1f - effectiveRecoil,
                        color = PubgGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attachment Configuration (Muzzle)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "ملحق فوهة السلاح (Muzzle Attachment)" else "MUZZLE ATTACHMENT",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttachmentMuzzle.entries.forEach { muzzle ->
                            val isChosen = selectedMuzzle == muzzle
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedMuzzle = muzzle },
                                label = { Text(muzzle.label.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PubgYellow,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attachment Configuration (Foregrip)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "المقبض الأمامي (Foregrip)" else "FOREGRIP ATTACHMENT",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttachmentGrip.entries.forEach { grip ->
                            val isChosen = selectedGrip == grip
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedGrip = grip },
                                label = { Text(grip.label.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PubgYellow,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attachment Configuration (Magazine)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "خزنة الذخيرة (Magazine)" else "MAGAZINE TYPE",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttachmentMag.entries.forEach { mag ->
                            val isChosen = selectedMag == mag
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedMag = mag },
                                label = { Text(mag.label.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PubgYellow,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attachment Configuration (Scopes)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "منظار التصويب الملحق (Optics / Scope)" else "ATTACHED OPTICS & SCOPE",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScopeType.entries.forEach { scope ->
                            val isChosen = selectedScope == scope
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedScope = scope }
                                    .border(
                                        width = if (isChosen) 2.dp else 1.dp,
                                        color = if (isChosen) PubgYellow else PubgCardBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .testTag("scope_${scope.name.lowercase()}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) Color(0xFF1E293B) else PubgDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🔭", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = scope.label.take(7),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChosen) PubgYellow else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Equip Button
            Button(
                onClick = {
                    viewModel.equipLoadout(
                        weapon = selectedWeapon,
                        scope = selectedScope,
                        muzzle = selectedMuzzle,
                        grip = selectedGrip,
                        mag = selectedMag
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.selectedWeaponModel == selectedWeapon &&
                        uiState.player.muzzleAttachment == selectedMuzzle &&
                        uiState.player.gripAttachment == selectedGrip &&
                        uiState.player.magAttachment == selectedMag &&
                        uiState.player.currentScope == selectedScope) Color(0xFF166534) else PubgYellow
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("equip_weapon_button")
            ) {
                Text(
                    text = if (isArabic) "تجهيز التشكيلة الكاملة مع الملحقات ✔" else "EQUIP FULL CUSTOM LOADOUT ✔",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun WeaponStatBar(label: String, value: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = PubgTextSecondary, fontSize = 11.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF1E293B)
        )
    }
}
