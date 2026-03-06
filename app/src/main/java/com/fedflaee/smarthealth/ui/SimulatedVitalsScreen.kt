package com.fedflaee.smarthealth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fedflaee.smarthealth.viewmodel.HealthViewModel
import com.fedflaee.smarthealth.utils.SimulatedVitals
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.center

@Composable
fun SimulatedVitalsScreen(
    healthViewModel: HealthViewModel = viewModel()
) {

    val vitals by healthViewModel.simulatedVitals.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D3F4A),
                        Color(0xFF155E69),
                        Color(0xFF1C6F7B)
                    )
                )
            )
    ) {

        // Soft center glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = center
            )
        }

        // Your existing Column content goes here {

        Column {

            Text(
                text = "Simulation",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Vitals") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> OverviewSection(vitals)
                1 -> VitalsSection(vitals)
            }
        }
    }
}

/* ================= OVERVIEW ================= */

@Composable
fun OverviewSection(vitals: SimulatedVitals) {

    HeartRateCard(
        heartRate = vitals.heartRate,
        severity = vitals.severity,
        isAbnormal = vitals.isAbnormal
    )

    Spacer(modifier = Modifier.height(20.dp))

    ContextCard(vitals)
}

/* ================= VITALS ================= */

@Composable
fun VitalsSection(vitals: SimulatedVitals) {

    Column {

        Row {
            VitalCard("SpO₂", "${vitals.spo2} %", Icons.Default.Air, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            VitalCard("Blood Pressure", vitals.bp, Icons.Default.Favorite, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            VitalCard(
                "Temperature",
                "${"%.1f".format(vitals.temperature)} °C",
                Icons.Default.Thermostat,
                Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            VitalCard(
                "Respiratory Rate",
                "${vitals.respiratoryRate}/min",
                Icons.Default.Waves,
                Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        VitalCard(
            "Steps",
            vitals.steps.toString(),
            Icons.Default.DirectionsWalk,
            Modifier.fillMaxWidth()
        )
    }
}

/* ================= HEART CARD ================= */

@Composable
fun HeartRateCard(
    heartRate: Int,
    severity: String,
    isAbnormal: Boolean
) {

    val color = when (severity) {
        "NORMAL" -> Color(0xFF4CAF50)
        "ABNORMAL" -> Color(0xFFFFB300)
        "SEVERE" -> Color(0xFFFF7043)
        "CRITICAL" -> Color(0xFFD32F2F)
        else -> Color(0xFF145D63)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = color)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Heart Rate", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "$heartRate",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text("BPM", color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                severity,
                color = color,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                if (isAbnormal)
                    "Physiological stress detected"
                else
                    "Normal physiological state",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

/* ================= CONTEXT CARD ================= */

@Composable
fun ContextCard(vitals: SimulatedVitals) {

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            ContextItem(Icons.Default.DirectionsRun, "Activity", vitals.activity)
            ContextItem(Icons.Default.Nightlight, "Sleep", vitals.sleepState)
            ContextItem(Icons.Default.Person, "Age", vitals.ageGroup)
            ContextItem(Icons.Default.Schedule, "Time", vitals.timeOfDay)
        }
    }
}

@Composable
fun ContextItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Icon(icon, contentDescription = null, tint = Color.Gray)

        Spacer(modifier = Modifier.height(4.dp))

        Text(label, fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(4.dp))

        Text(value, fontWeight = FontWeight.Bold)
    }
}

/* ================= VITAL CARD ================= */

@Composable
fun VitalCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}