package com.fedflaee.smarthealth.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedflaee.smarthealth.viewmodel.HealthViewModel
import com.fedflaee.smarthealth.viewmodel.TimeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartAnalysisScreen(viewModel: HealthViewModel) {

    var selectedMode by remember { mutableStateOf(TimeMode.WEEK) }
    var showDatePicker by remember { mutableStateOf(false) }

    val rawData = when (selectedMode) {
        TimeMode.WEEK -> viewModel.getWeeklyAverageHeartRates()
        TimeMode.YEAR -> viewModel.getMonthlyAveragesForSelectedYear()
        else -> emptyList()
    }

    val labels = when (selectedMode) {
        TimeMode.WEEK ->
            listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

        TimeMode.YEAR ->
            listOf(
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
            )

        else -> emptyList()
    }

    val validValues = rawData.filter { it > 0 }
    val avg = if (validValues.isNotEmpty()) validValues.average() else 0.0
    val max = validValues.maxOrNull() ?: 0f
    val min = validValues.minOrNull() ?: 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F8))
            .padding(20.dp)
    ) {

        Text(
            text = "Heart Analysis",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // MODE SWITCH
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            ModeButton("Week", TimeMode.WEEK, selectedMode) {
                selectedMode = TimeMode.WEEK
            }

            ModeButton("Year", TimeMode.YEAR, selectedMode) {
                selectedMode = TimeMode.YEAR
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HEADER (Centered Year / Week)
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            // CENTERED ARROWS + YEAR / WEEK
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(onClick = {
                    if (selectedMode == TimeMode.WEEK)
                        viewModel.previousWeek()
                    else
                        viewModel.previousYear()
                }) {
                    Text("<", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = if (selectedMode == TimeMode.WEEK)
                        viewModel.getCurrentWeekRangeLabel()
                    else
                        viewModel.getCurrentYearLabel(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    if (selectedMode == TimeMode.WEEK)
                        viewModel.nextWeek()
                    else
                        viewModel.nextYear()
                }) {
                    Text(">", fontWeight = FontWeight.Bold)
                }
            }

            // CALENDAR ICON STAYS RIGHT
            IconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date"
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // GRAPH CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .height(250.dp)
            ) {

                val minScale = 40f
                val maxScale = 160f

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(45.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf(160, 140, 120, 100, 80, 60, 40).forEach {
                        Text(it.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.Bottom
                ) {

                    labels.forEachIndexed { index, label ->

                        val value = rawData.getOrElse(index) { 0f }
                        val clamped = value.coerceIn(minScale, maxScale)
                        val ratio = (clamped - minScale) / (maxScale - minScale)

                        val animatedRatio by animateFloatAsState(
                            targetValue = ratio,
                            animationSpec = tween(600),
                            label = ""
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {

                            if (value > 0) {
                                Text(
                                    text = value.toInt().toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B7F79)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height((200 * animatedRatio).dp)
                                    .background(
                                        color = Color(0xFF1B7F79),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SUMMARY CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                StatItem("Avg", avg.toInt())
                VerticalDivider()
                StatItem("Max", max.toInt())
                VerticalDivider()
                StatItem("Min", min.toInt())
            }
        }
    }

    // DATE PICKER
    if (showDatePicker) {

        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.selectDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun VerticalDivider() {
    Divider(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp),
        color = Color(0xFFE0E0E0)
    )
}

@Composable
fun ModeButton(
    text: String,
    mode: TimeMode,
    selectedMode: TimeMode,
    onClick: () -> Unit
) {
    val selected = mode == selectedMode

    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected)
                Color(0xFF1B7F79)
            else
                Color(0xFFE0E0E0),
            contentColor = if (selected)
                Color.White
            else
                Color.Black
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B7F79)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}