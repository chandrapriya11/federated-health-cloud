package com.fedflaee.smarthealth.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedflaee.smarthealth.R

@Composable
fun IntroScreen(
    onStartClick: () -> Unit
) {

    val infinite = rememberInfiniteTransition()

    // Body breathing animation
    val scale by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Glow pulse animation
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Floating animation
    val floatAnim by infinite.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        )
    )

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

        // Subtle floating particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(35) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = (2..5).random().toFloat(),
                    center = Offset(
                        x = (0..size.width.toInt()).random().toFloat(),
                        y = (0..size.height.toInt()).random().toFloat()
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(50.dp))

            Text(
                text = "SmartHealth",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentAlignment = Alignment.Center
            ) {

                // Glow circle behind body
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Human body image
                Image(
                    painter = painterResource(id = R.drawable.intro),
                    contentDescription = null,
                    modifier = Modifier
                        .size(350.dp)
                        .scale(scale)
                )

                // Floating icons around body

                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 40.dp, y = 120.dp + floatAnim.dp)
                        .size(36.dp)
                )

                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-40).dp, y = 130.dp - floatAnim.dp)
                        .size(36.dp)
                )

                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 50.dp, y = (-100).dp + floatAnim.dp)
                        .size(36.dp)
                )

                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-50).dp, y = (-110).dp - floatAnim.dp)
                        .size(36.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Your Body. Your Signals.",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        "Let's Start →",
                        color = Color(0xFF0D3F4A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))
        }
    }
}