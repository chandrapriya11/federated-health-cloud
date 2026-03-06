package com.fedflaee.smarthealth.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.fedflaee.smarthealth.R
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "SmartHealth",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            HorizontalPager(state = pagerState) { page ->

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(40.dp))

                    when (page) {
                        0 -> RealHeartSection()
                        1 -> GlassVitalsSection()
                        2 -> ProtectionSection()
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    TitleText(
                        when (page) {
                            0 -> "Monitor Your Heart"
                            1 -> "Understand Your Vitals"
                            else -> "Stay Ahead of Risks"
                        }
                    )

                    DescText(
                        when (page) {
                            0 -> "Track heart activity in real-time and stay informed about your cardiovascular health."
                            1 -> "Monitor oxygen, temperature, blood pressure and daily activity."
                            else -> "Advanced protection monitoring keeps you safe every day."
                        }
                    )
                }
            }

            Row {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .background(
                                if (pagerState.currentPage == index)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pagerState.currentPage == 2) {
                Button(
                    onClick = onGetStartedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Get Started",
                        color = Color(0xFF0D3F4A),
                        fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) {
                    Text("Next", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/* ---------------- TEXT ---------------- */

@Composable
fun TitleText(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
fun DescText(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center
    )
}

/* ---------------- PAGE 1 ---------------- */

@Composable
fun RealHeartSection() {

    val infinite = rememberInfiniteTransition()

    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing)
        )
    )

    val glow by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = glow), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val midY = size.height / 2
            val width = size.width
            val shift = progress * width
            val path = Path()

            path.moveTo(-shift, midY)

            val step = width / 6
            for (i in 0..6) {
                val x = i * step - shift
                path.lineTo(x + 20f, midY)
                path.lineTo(x + 40f, midY - 40f)
                path.lineTo(x + 60f, midY + 50f)
                path.lineTo(x + 80f, midY - 20f)
                path.lineTo(x + 100f, midY)
            }

            drawPath(path, Color.White.copy(alpha = 0.3f), style = Stroke(16f))
            drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(6f))
        }

        Image(
            painter = painterResource(id = R.drawable.heart_realistic),
            contentDescription = null,
            modifier = Modifier.size(240.dp)
        )
    }
}

/* ---------------- PAGE 2 ---------------- */

@Composable
fun GlassVitalsSection() {

    val infinite = rememberInfiniteTransition()

    val glow by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = glow), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8FD0D8),
                            Color(0xFF2C6F79)
                        )
                    ),
                    shape = CircleShape
                )
        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawLine(Color.White.copy(0.4f),
                    Offset(w / 2, 0f),
                    Offset(w / 2, h),
                    4f)

                drawLine(Color.White.copy(0.4f),
                    Offset(0f, h / 2),
                    Offset(w, h / 2),
                    4f)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(Icons.Default.WaterDrop, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Icon(Icons.Default.Thermostat, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Icon(Icons.Default.ShowChart, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Icon(Icons.Default.DirectionsWalk, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

/* ---------------- PAGE 3 ---------------- */

@Composable
fun ProtectionSection() {

    val infinite = rememberInfiniteTransition()

    val glow by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = glow), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Image(
            painter = painterResource(id = R.drawable.protection),
            contentDescription = null,
            modifier = Modifier.size(240.dp)
        )
    }
}