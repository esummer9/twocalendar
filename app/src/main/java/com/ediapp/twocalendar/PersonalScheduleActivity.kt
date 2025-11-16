package com.ediapp.twocalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
                val year = intent.getIntExtra("year", YearMonth.now().year)
                val month = intent.getIntExtra("month", YearMonth.now().monthValue)
                var baseMonth by remember { mutableStateOf(YearMonth.of(year, month)) }
                var refreshTrigger by remember { mutableStateOf(0) }

                val schedules by produceState<List<Pair<LocalDate, Schedule>>>(initialValue = emptyList(), key1 = baseMonth, key2 = refreshTrigger) {
                    val firstMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth, listOf("personal"))
                    val secondMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth.plusMonths(1), listOf("personal"))

                    val allSchedulesRaw = (firstMonthSchedulesRaw.keys + secondMonthSchedulesRaw.keys).associateWith {
                        (firstMonthSchedulesRaw[it].orEmpty() + "\n" + secondMonthSchedulesRaw[it].orEmpty()).trim()
                    }

                    value = allSchedulesRaw.entries
                        .flatMap { (date, scheduleString) ->
                            scheduleString.split('\n').mapNotNull { line ->
                                if (line.isNotBlank()) {
                                    val parts = line.split('|', limit = 2)
                                    if (parts.size == 2) {
                                        date to Schedule(category = parts[0], title = parts[1])
                                    } else {
                                        null
                                    }
                                } else {
                                    null
                                }
                            }
                        }
                        .sortedBy { it.first }
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
                        refreshTrigger++
                    },
                    onAdd = { date, title ->
                        dbHelper.addSchedule(date, title) // TODO: Implement addPersonalSchedule in DatabaseHelper
                        refreshTrigger++
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("개인일정") },
        text = {
            Column {
                Box {
                    TextField(
                        value = selectedDate.toString(),
                        onValueChange = {},
                        label = { Text("날짜") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClick = { showDatePicker = true })
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(selectedDate, title)
                    }
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalScheduleScreen(
    schedules: List<Pair<LocalDate, Schedule>>,
    baseMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onBack: () -> Unit,
    onDelete: (LocalDate, String) -> Unit,
    onAdd: (LocalDate, String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("일정 삭제") },
            text = { Text("'${showDeleteDialog!!.second}' 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(showDeleteDialog!!.first, showDeleteDialog!!.second)
                        showDeleteDialog = null
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("취소")
                }
            }
        )
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, title ->
                onAdd(date, title)
                showAddDialog = false
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
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "추가")
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

                val monthStr = "${baseMonth.year}년 ${baseMonth.monthValue.toString().padStart(2, '0')}월 ~ ${nextMonth.monthValue.toString().padStart(2, '0')}월"
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
                    items(schedules) { (date, schedule) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1번째 컬럼: D-day 값
                            val daysDiff = ChronoUnit.DAYS.between(date, today)
                            val diffText = when {
                                daysDiff == 0L -> "오늘"
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

                            // 2번째 컬럼: 날짜/요일 및 제목
                            Column(
                                modifier = Modifier
                                    .weight(0.75f)
                                    .padding(horizontal = 8.dp)
                            ) {
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
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = schedule.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 3번째 컬럼: 삭제 버튼
                            IconButton(onClick = { showDeleteDialog = date to schedule.title }) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
        val sampleSchedules = listOf(
            LocalDate.now() to Schedule("personal", "개인일정 1"),
            LocalDate.now().plusDays(1) to Schedule("personal", "개인일정 2"),
            LocalDate.now().plusDays(1) to Schedule("personal", "미팅"),
            LocalDate.now().minusDays(3) to Schedule("personal", "지난일정")
        )
        PersonalScheduleScreen(
            schedules = sampleSchedules,
            baseMonth = YearMonth.now(),
            onPrevMonth = {},
            onNextMonth = {},
            onThisMonth = {},
            onBack = {},
            onDelete = { _, _ -> },
            onAdd = { _, _ -> }
        )
    }
}
