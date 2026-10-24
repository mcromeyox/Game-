package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.VehicleModel
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onTestDrive: (VehicleModel) -> Unit
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val isArabic = uiState.isArabic
    var selectedVehicle by remember { mutableStateOf(uiState.selectedVehicleModel) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val vehicleColors = listOf(
        Color(0xFF2E4057) to "Military Blue",
        Color(0xFFD97706) to "Desert Camo",
        Color(0xFF0284C7) to "Glacier Cyan",
        Color(0xFFE11D48) to "Inferno Red",
        Color(0xFF1E293B) to "Midnight Carbon"
    )

    Scaffold(
        containerColor = PubgDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isArabic) "مرآب المركبات وفيزياء القيادة" else "GARAGE & VEHICLE TUNING",
                        color = PubgYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("garage_back_button")) {
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
            // Vehicle Horizontal Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VehicleModel.entries.forEach { model ->
                    val isSelected = selectedVehicle == model
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedVehicle = model }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PubgYellow else PubgCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("garage_tab_${model.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E293B) else PubgCardBg
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(model.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.id.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PubgYellow else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Visual Showcase Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.5.dp, PubgYellow.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Showroom Stage Floor & Light
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f + 15.dp.toPx()

                        // Stage light spotlight
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(PubgCyan.copy(alpha = 0.25f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = size.width * 0.45f
                            ),
                            center = Offset(cx, cy),
                            radius = size.width * 0.45f
                        )

                        // Showroom pedestal ring
                        drawOval(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(cx - 130.dp.toPx(), cy + 20.dp.toPx()),
                            size = Size(260.dp.toPx(), 40.dp.toPx())
                        )
                        drawOval(
                            color = PubgYellow.copy(alpha = 0.6f),
                            topLeft = Offset(cx - 130.dp.toPx(), cy + 20.dp.toPx()),
                            size = Size(260.dp.toPx(), 40.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )

                        // Render vehicle chassis on pedestal
                        val tintColor = vehicleColors[selectedColorIndex].first
                        val vehW = 140.dp.toPx()
                        val vehH = 65.dp.toPx()

                        // Shadow
                        drawOval(
                            color = Color(0x66000000),
                            topLeft = Offset(cx - vehW / 2f + 10f, cy + 15.dp.toPx()),
                            size = Size(vehW, 25.dp.toPx())
                        )

                        // Wheels
                        val wheelW = 32.dp.toPx()
                        val wheelH = 14.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(cx - vehW * 0.40f, cy - vehH * 0.55f),
                            size = Size(wheelW, wheelH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(cx + vehW * 0.18f, cy - vehH * 0.55f),
                            size = Size(wheelW, wheelH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(cx - vehW * 0.40f, cy + vehH * 0.35f),
                            size = Size(wheelW, wheelH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(cx + vehW * 0.18f, cy + vehH * 0.35f),
                            size = Size(wheelW, wheelH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        // Body
                        drawRoundRect(
                            color = tintColor,
                            topLeft = Offset(cx - vehW / 2f, cy - vehH / 2f),
                            size = Size(vehW, vehH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                        )

                        // Windshield / Cabin
                        drawRoundRect(
                            color = Color(0xFF0284C7),
                            topLeft = Offset(cx - vehW * 0.15f, cy - vehH * 0.35f),
                            size = Size(vehW * 0.45f, vehH * 0.7f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }

                    // Vehicle Name Tag
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isArabic) selectedVehicle.nameAr else selectedVehicle.nameEn,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${selectedVehicle.maxSpeedKmh.toInt()} KM/H • ${selectedVehicle.mass.toInt()} KG",
                            color = PubgYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color / Paint Finishes
            Text(
                text = if (isArabic) "ألوان وطلاء الهيكل:" else "PAINT & FINISHES:",
                color = PubgYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                vehicleColors.forEachIndexed { index, (color, _) ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColorIndex == index) 3.dp else 1.dp,
                                color = if (selectedColorIndex == index) PubgYellow else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { selectedColorIndex = index }
                            .testTag("paint_color_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Vehicle Physics & Specifications Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PubgCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "مواصفات الأداء والفيزياء" else "PHYSICS & PERFORMANCE SPECS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VehicleStatBar(
                        label = if (isArabic) "السرعة القصوى" else "Top Speed",
                        value = "${selectedVehicle.maxSpeedKmh.toInt()} km/h",
                        progress = selectedVehicle.maxSpeedKmh / 180f,
                        color = PubgCyan
                    )

                    VehicleStatBar(
                        label = if (isArabic) "التسارع والنيترو" else "Acceleration",
                        value = "${selectedVehicle.acceleration} G",
                        progress = selectedVehicle.acceleration / 10f,
                        color = PubgBoostOrange
                    )

                    VehicleStatBar(
                        label = if (isArabic) "مرونة الدريفت والانزلاق" else "Drift & Slip Rating",
                        value = "${(selectedVehicle.driftRating * 100).toInt()}%",
                        progress = selectedVehicle.driftRating,
                        color = PubgYellow
                    )

                    VehicleStatBar(
                        label = if (isArabic) "قوة الدرع والمتانة" else "Armor & Durability",
                        value = "${selectedVehicle.armor.toInt()} HP",
                        progress = selectedVehicle.armor / 1500f,
                        color = PubgGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Equip & Test Drive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.selectVehicleModel(selectedVehicle) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.selectedVehicleModel == selectedVehicle) Color(0xFF166534) else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("equip_vehicle_button")
                ) {
                    Text(
                        text = if (uiState.selectedVehicleModel == selectedVehicle) {
                            if (isArabic) "مُجهز حالياً ✔" else "EQUIPPED ✔"
                        } else {
                            if (isArabic) "تجهيز للمباراة" else "EQUIP"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        viewModel.selectVehicleModel(selectedVehicle)
                        onTestDrive(selectedVehicle)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PubgYellow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f).height(50.dp).testTag("test_drive_button")
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تجربة القيادة 🏎️" else "TEST DRIVE 🏎️",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VehicleStatBar(label: String, value: String, progress: Float, color: Color) {
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
