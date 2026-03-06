package com.fedflaee.smarthealth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedflaee.smarthealth.viewmodel.HealthViewModel

@Composable
fun HealthTabs(
    liveHr: Int,
    latitude: Double,
    longitude: Double,
    healthViewModel: HealthViewModel
) {

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {

            Row(
                modifier = Modifier.padding(6.dp)
            ) {

                TabButton(
                    text = "LIVE",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = "SIMULATION",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )

                // ✅ NEW TAB (Only addition)
                TabButton(
                    text = "ANALYSIS",
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedTab) {
            0 -> LiveHeartRateScreen(liveHr)
            1 -> SimulatedVitalsScreen()
            2 -> HeartAnalysisScreen(healthViewModel) // ✅ NEW SCREEN
        }

        Text(
            text = "Location: %.4f , %.4f".format(latitude, longitude),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface

    val textColor =
        if (selected) Color.White
        else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        modifier = modifier.padding(4.dp),
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}