package com.fedflaee.smarthealth.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import androidx.compose.ui.draw.scale

@Composable
fun LiveHeartRateScreen(heartRate: Int) {

    val progress = min(heartRate / 200f, 1f)

    val zone = when {
        heartRate == 0 -> "Waiting"
        heartRate < 80 -> "Resting"
        heartRate < 110 -> "Normal"
        else -> "High"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
            .padding(24.dp)
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Live Heart Rate",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(20.dp, CircleShape)
                        .background(
                            color = Color(0xFF1C313A),
                            shape = CircleShape
                        )
                )

                CircularProgressIndicator(
                    progress = progress,
                    strokeWidth = 14.dp,
                    color = Color(0xFFE53935),
                    trackColor = Color(0xFF37474F),
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(
                        text = if (heartRate == 0) "--" else "$heartRate",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "BPM",
                        fontSize = 16.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2A30)
                ),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Heart Rate Zone",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = zone,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (zone) {
                            "Resting" -> Color(0xFF4CAF50)
                            "Normal" -> Color(0xFF29B6F6)
                            "High" -> Color(0xFFFF5252)
                            else -> Color.Gray
                        }
                    )
                }
            }
        }
    }
}