package com.ediapp.twocalendar.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.R
import java.time.LocalDate
import java.time.YearMonth


@Composable
fun TwoMonthFragment(modifier: Modifier = Modifier, fetchHolidaysForYear: (Int) -> Unit) {
    var baseMonth by remember { mutableStateOf(YearMonth.now()) }
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    val holidays = remember(baseMonth) {
        val firstMonthHolidays = dbHelper.getHolidaysForMonth(baseMonth)
        val secondMonthHolidays = dbHelper.getHolidaysForMonth(baseMonth.plusMonths(1))
        firstMonthHolidays + secondMonthHolidays
    }

    val firstMonth = baseMonth
    val secondMonth = baseMonth.plusMonths(1)

    LaunchedEffect(baseMonth) {
        fetchHolidaysForYear(baseMonth.year)
        fetchHolidaysForYear(baseMonth.plusMonths(1).year)
    }

    Scaffold {
        Column(modifier = modifier.padding(it), verticalArrangement = Arrangement.Top) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${firstMonth.year}년 ${firstMonth.monthValue}월",
                    modifier = Modifier.padding(vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { baseMonth = baseMonth.minusMonths(1) }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "이전달")
                    }
                    IconButton(onClick = { baseMonth = YearMonth.now() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.dot),
                            contentDescription = "이번달",
                            modifier = Modifier.size(15.dp),
                            tint = Color.Unspecified
                        )
                    }
                    IconButton(onClick = { baseMonth = baseMonth.plusMonths(1) }) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "다음달")
                    }
                }
            }

//        Text(text = "${firstMonth.year}년 ${firstMonth.monthValue}월", modifier = Modifier.padding(vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            MonthCalendar(yearMonth = firstMonth, holidays = holidays)

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${secondMonth.year}년 ${secondMonth.monthValue}월",
                modifier = Modifier.padding(vertical = 2.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            MonthCalendar(yearMonth = secondMonth, holidays = holidays)
        }
    }
}

@Composable
fun MonthCalendar(yearMonth: YearMonth, holidays: Map<LocalDate, String>, modifier: Modifier = Modifier) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ...
    val today = LocalDate.now()
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, verticalArrangement = Arrangement.Top) {
        // Header
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
            weekDays.forEachIndexed { index, day ->
                val color = when (index) {
                    0 -> Color.Red
                    6 -> Color.Blue
                    else -> Color.Unspecified
                }
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp))

        // Days
        val totalSlots = (daysInMonth + startDayOfWeek + 6) / 7 * 7
        val dayCells = (1..totalSlots).toList()

        dayCells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { dayIndex ->
                    val dayOfMonth = dayIndex - startDayOfWeek
                    if (dayOfMonth > 0 && dayOfMonth <= daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val isToday = date == today
                        val holiday = holidays[date]
                        val dayOfWeek = (dayIndex - 1) % 7 // 0 for Sunday, 6 for Saturday

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.45f)
                                .then(
                                    if (isToday) {
                                        Modifier.drawBehind { // This is not a composable function
                                            drawCircle(
                                                color = primaryColor, // Use the color here
                                                radius = size.minDimension / 2f,
                                                style = Stroke(width = 1.5.dp.toPx())
                                            )
                                        }
                                    } else if (holiday != null) {
                                        Modifier.drawBehind { // This is not a composable function
                                            val margin = 5.dp.toPx()
                                            drawRoundRect(
                                                color = Color.Red,
                                                topLeft = Offset(margin, margin),
                                                size = Size(size.width - (margin * 2), size.height - (margin * 2)),
                                                cornerRadius = CornerRadius(8f, 8f),
                                                style = Stroke(width = 1.5.dp.toPx())
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                val color = when {
                                    isToday -> primaryColor
                                    holiday != null -> Color.Red
                                    dayOfWeek == 0 -> Color.Red
                                    dayOfWeek == 6 -> Color.Blue
                                    else -> Color.Unspecified
                                }
                                Text(text = dayOfMonth.toString(), color = color, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.45f)
                        )
                    }
                }
            }
        }
    }
}
