package com.ediapp.twocalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Schedule(val category: String, val title: String)

class PersonalScheduleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwocalendarTheme {
                val dbHelper = DatabaseHelper(this)
                var baseMonth by remember { mutableStateOf(YearMonth.now()) }

                val schedules by produceState<Map<LocalDate, List<Schedule>>>(initialValue = emptyMap(), key1 = baseMonth) {
                    val firstMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth, listOf("personal"))
                    val secondMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth.plusMonths(1), listOf("personal"))

                    val allSchedulesRaw = firstMonthSchedulesRaw + secondMonthSchedulesRaw

                    value = allSchedulesRaw.mapValues { (_, scheduleString) ->
                        scheduleString.split('\n').mapNotNull {
                            val parts = it.split('|', limit = 2)
                            if (parts.size == 2) {
                                Schedule(category = parts[0], title = parts[1])
                            } else {
                                null
                            }
                        }
                    }
                    .filter { it.value.isNotEmpty() }
                    .toSortedMap(naturalOrder())
                }

                PersonalScheduleScreen(
                    schedules = schedules,
                    baseMonth = baseMonth,
                    onPrevMonth = { baseMonth = baseMonth.minusMonths(1) },
                    onNextMonth = { baseMonth = baseMonth.plusMonths(1) },
                    onThisMonth = { baseMonth = YearMonth.now() },
                    onBack = { finish() },
                    onDelete = { date, title ->
                        dbHelper.deletePersonalSchedule(date, title)
                        baseMonth = YearMonth.from(baseMonth) // Force recomposition
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScheduleScreen(
    schedules: Map<LocalDate, List<Schedule>>,
    baseMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onBack: () -> Unit,
    onDelete: (LocalDate, String) -> Unit
) {
    var showDialog by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }

    if (showDialog != null) {
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("일정 삭제") },
            text = { Text("'${showDialog!!.second}' 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(showDialog!!.first, showDialog!!.second)
                        showDialog = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("개인일정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val nextMonth = baseMonth.plusMonths(1)

                val monthStr = "${baseMonth.year}년 ${baseMonth.monthValue.toString().padStart(2, '0')}월 ~  ${nextMonth.monthValue.toString().padStart(2, '0')}월"
                Text(
                    text = monthStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전달")
                    }
                    IconButton(onClick = onThisMonth) {
                        Icon(
                            painter = painterResource(id = R.drawable.dot),
                            contentDescription = "이번달",
                            modifier = Modifier.size(15.dp),
                            tint = Color.Unspecified
                        )
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "다음달")
                    }
                }
            }
            if (schedules.isEmpty()) {
                Text(
                    text = "개인일정이 없습니다.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                val today = LocalDate.now()
                LazyColumn(
                ) {
                    schedules.forEach { (date, scheduleList) ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val daysDiff = ChronoUnit.DAYS.between(today, date)
                                val diffText = when {
                                    daysDiff == 0L -> "D-day"
                                    daysDiff > 0L -> "D+$daysDiff"
                                    else -> "D$daysDiff"
                                }
                                val diffColor = when {
                                    daysDiff < 0L -> Color.Gray
                                    daysDiff == 0L -> Color.Red
                                    daysDiff in 1..7 -> Color(0xFFFFA500) // Orange
                                    else -> Color.Unspecified
                                }

                                Text(
                                    text = diffText,
                                    color = diffColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.25f)
                                )

                                val dayOfWeek = date.dayOfWeek
                                val dayOfWeekText = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                                val dayOfWeekColor = when (dayOfWeek) {
                                    DayOfWeek.SATURDAY -> Color.Blue
                                    DayOfWeek.SUNDAY -> Color.Red
                                    else -> Color.Unspecified
                                }

                                Text(
                                    text = buildAnnotatedString {
                                        append(date.toString())
                                        append(" (")
                                        withStyle(style = SpanStyle(color = dayOfWeekColor, fontWeight = FontWeight.Bold)) {
                                            append(dayOfWeekText)
                                        }
                                        append(")")
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(0.75f)
                                )
                            }
                        }
                        items(scheduleList) { schedule ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = schedule.title,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showDialog = date to schedule.title }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                                }
                            }
                        }
                        // 날짜별 일정 목록 다음에 구분선을 추가합니다.
                        item {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonalScheduleScreenPreview() {
    TwocalendarTheme {
        val sampleSchedules = mapOf(
            LocalDate.now() to listOf(Schedule("personal", "개인일정 1")),
            LocalDate.now().plusDays(1) to listOf(Schedule("personal", "개인일정 2"), Schedule("personal", "미팅")),
            LocalDate.now().minusDays(3) to listOf(Schedule("personal", "지난일정"))
        )
        PersonalScheduleScreen(
            schedules = sampleSchedules,
            baseMonth = YearMonth.now(),
            onPrevMonth = {},
            onNextMonth = {},
            onThisMonth = {},
            onBack = {},
            onDelete = { _, _ -> }
        )
    }
}
