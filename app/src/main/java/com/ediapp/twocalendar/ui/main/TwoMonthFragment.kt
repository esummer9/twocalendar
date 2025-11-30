package com.ediapp.twocalendar.ui.main

import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.Constants.my_sep
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId


@Composable
fun TwoMonthFragment(
    modifier: Modifier = Modifier,
    fetchHolidaysForYear: (Int) -> Unit,
    visibleCalList: Boolean,
    selectedPersonalSchedules: List<String>,
    showHolidays: Boolean,
    onMonthChanged: (YearMonth) -> Unit,
    onNavigateToPersonalSchedule: (LocalDate) -> Unit,
    scheduleUpdateTrigger: Int
) {
    var baseMonth by remember { mutableStateOf(YearMonth.now()) }
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var newlyAddedSchedules by remember { mutableStateOf(listOf<String>()) }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedDateForDialog by remember { mutableStateOf<LocalDate?>(null) }

    // HolidayList의 가시성을 제어하는 상태 변수
    var showHolidayListInFirstMonth by remember { mutableStateOf(true) }
    var showHolidayListInSecondMonth by remember { mutableStateOf(true) }

    val holidays = remember(baseMonth, selectedPersonalSchedules, newlyAddedSchedules, showHolidays, scheduleUpdateTrigger) {
        val allSelectedSchedules = selectedPersonalSchedules + newlyAddedSchedules

        val categories = mutableListOf<String>()
        if (showHolidays) {
            categories.add("holiday")
        }
        if (allSelectedSchedules.isNotEmpty()) {
            categories.add("personal")
            categories.add("생일")
            categories.add("기념일")
        }

        if (categories.isEmpty()) {
            emptyMap()
        } else {
            val allSchedules = dbHelper.getDaysForCategoryMonth(baseMonth, categories) +
                    dbHelper.getDaysForCategoryMonth(baseMonth.plusMonths(1), categories)

            if (allSelectedSchedules.isNotEmpty()) {
                allSchedules.mapValues { (_, descriptions) ->
                    descriptions.split(my_sep).filter { desc ->
                        val parts = desc.split('|')
                        if (parts.size < 3) return@filter true

                        val category = parts[1]
                        val title = parts[2]
                        if (category == "personal" || category == "생일" || category == "기념일") {
                            title in allSelectedSchedules
                        } else {
                            true
                        }
                    }.joinToString(my_sep)
                }.filterValues { it.isNotBlank() }
            } else {
                allSchedules
            }
        }
    }

    val onDateLongClick = { date: LocalDate ->
        selectedDateForDialog = date
        showScheduleDialog = true
    }

    val onDateClick = { date: LocalDate ->
        val holiday = holidays[date]
        if (holiday?.contains("personal") == true) {
            onNavigateToPersonalSchedule(date) // Call the new callback
        }
    }


    Log.d("holidays", "visible:$visibleCalList | $holidays ")

    val firstMonth = baseMonth
    val secondMonth = baseMonth.plusMonths(1)

    LaunchedEffect(baseMonth) {
        onMonthChanged(baseMonth)
        fetchHolidaysForYear(baseMonth.year)
        fetchHolidaysForYear(baseMonth.plusMonths(1).year)
    }

    Scaffold {
        val scrollState = rememberScrollState()
        var offsetX by remember { mutableFloatStateOf(0f) }
        Column(
            modifier = modifier
                .padding(it)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        },
                        onDragEnd = {
                            when {
                                offsetX > 100 -> { // Swipe Right
                                    baseMonth = baseMonth.minusMonths(1)
                                }
                                offsetX < -100 -> { // Swipe Left
                                    baseMonth = baseMonth.plusMonths(1)
                                }
                            }
                            offsetX = 0f
                        }
                    )
                }
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전달")
                    }
                    Button(
                        onClick = { baseMonth = YearMonth.now() },
                        modifier = Modifier.size(50.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.rectangle),
                                contentDescription = "이번달 배경",
                                modifier = Modifier.size(44.dp),
                                tint = Color.Unspecified
                            )
                            Text(
                                text = LocalDate.now().monthValue.toString().padStart(2, '0'),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = { baseMonth = baseMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "다음달")
                    }
                }
            }

            MonthCalendar(yearMonth = firstMonth, holidays = holidays, onDateLongClick = onDateLongClick, onDateClick = onDateClick, visible = visibleCalList)
//            Log.d("holidays2", "firstMonth : $holidays")
            if(visibleCalList) {
                HolidayList(holidays = holidays, yearMonth = firstMonth, visible = showHolidayListInFirstMonth)
                if (holidays.any { (date, description) -> YearMonth.from(date) == firstMonth && description.contains("personal") }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showHolidayListInFirstMonth = !showHolidayListInFirstMonth }) {
                            Icon(
                                imageVector = if (!showHolidayListInFirstMonth) Icons.Filled.Info else Icons.Filled.Close,
                                contentDescription = "개인일정 목록 보기 토글"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${secondMonth.year}년 ${secondMonth.monthValue}월",
                modifier = Modifier.padding(vertical = 2.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            MonthCalendar(yearMonth = secondMonth, holidays = holidays, onDateLongClick = onDateLongClick, onDateClick = onDateClick, visible = visibleCalList)
//            Log.d("holidays3", "secondMonth : $holidays")
            if(visibleCalList) {
                HolidayList(holidays = holidays, yearMonth = secondMonth, visible = showHolidayListInSecondMonth)
                if (holidays.any { (date, description) -> YearMonth.from(date) == secondMonth && description.contains("personal") }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showHolidayListInSecondMonth = !showHolidayListInSecondMonth }) {
                            Icon(
                                imageVector = if (!showHolidayListInSecondMonth) Icons.Filled.Info else Icons.Filled.Close,
                                contentDescription = "개인일정 목록 보기 토글"
                            )
                        }
                    }
                }
            }

        }
    }

    if (showScheduleDialog && selectedDateForDialog != null) {
        AddScheduleDialog(
            date = selectedDateForDialog!!,
            onDismiss = { showScheduleDialog = false },
            onConfirm = { title, time ->
                dbHelper.addPersonalSchedule(selectedDateForDialog!!, title)
                showScheduleDialog = false
                newlyAddedSchedules = newlyAddedSchedules + title
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, time: LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(is24Hour = false)
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                onConfirm(title, time)
            }) {
                Text("확인")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}


@Composable
fun MonthCalendar(yearMonth: YearMonth, holidays: Map<LocalDate, String>, modifier: Modifier = Modifier, onDateLongClick: (LocalDate) -> Unit, onDateClick: (LocalDate) -> Unit, visible: Boolean) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1, ...
    val today = LocalDate.now()

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

                        /**
                         * 공휴일, 국경일 : 빨강색
                         * 내일정 : 오렌지
                         * 공휴일 + 내일정 : 보라색
                         */

                        var dayColor = Color.Black
                        if (holiday != null) {
                            val isHoliday = holiday.contains("holiday")
                            val isPersonal = holiday.contains("personal")

                            dayColor = when {
                                isHoliday && isPersonal -> Color(0xFF800080)
                                isHoliday -> Color.Red
                                isPersonal -> Color(0xFFFFA500)
                                else -> Color.Black
                            }
                        }

//                        dayColor = if ( holiday?.split("|")?.first()?.startsWith("holiday") == true) Color.Red else Color.Gray
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.45f)
                                .pointerInput(date) {
                                    detectTapGestures(
                                        onLongPress = { onDateLongClick(date) },
                                        onTap = { onDateClick(date) }
                                    )
                                }
                                .drawBehind {
                                    if (isToday) {
                                        val barHeight = 5.dp.toPx()
                                        val barWidth = size.width * 0.8f
                                        drawRect(
                                            color = Color.LightGray,
                                            topLeft = Offset(x = (size.width - barWidth) / 2, y = size.height - barHeight),
                                            size = Size(width = barWidth, height = barHeight)
                                        )
                                    }
                                    if (holiday != null) {
                                        val margin = 5.dp.toPx()
                                        drawRoundRect(
                                            color = dayColor,
                                            topLeft = Offset(margin, margin),
                                            size = Size(
                                                size.width - (margin * 2),
                                                size.height - (margin * 2)
                                            ),
                                            cornerRadius = CornerRadius(8f, 8f),
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                val color = when {
                                    isToday -> Color.Black
                                    holiday != null -> dayColor
                                    dayOfWeek == 0 -> Color.Red
                                    dayOfWeek == 6 -> Color.Blue
                                    else -> Color.Unspecified
                                }
                                Text(text = dayOfMonth.toString(), color = color, fontSize = 16.sp)
                                if (visible && holiday != null) {
//                                    Text(text = holiday, fontSize = 10.sp)
                                }
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

@Composable
fun HolidayList(holidays: Map<LocalDate, String>, yearMonth: YearMonth, visible: Boolean) {
    val context = LocalContext.current
    if (visible) {
        val monthSchedulesByDate = holidays.filter { (date, _) ->
            date.year == yearMonth.year && date.month == yearMonth.month
        }.mapValues { (_, description) ->
            description.split(my_sep).filter { it.isNotBlank() }
        }.toList().sortedBy { it.first.dayOfMonth }

        if (monthSchedulesByDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                monthSchedulesByDate.forEach { (date, schedules) ->
                    schedules.forEach { scheduleDescription ->


                        val parts = scheduleDescription.split('|')
                        val type = parts.getOrNull(1)
                        val scheduleTitle = parts.getOrNull(2)

                        Log.d("TwoMonthFragment", "Check List 2: $scheduleDescription | $type | $scheduleTitle")
                        if (scheduleTitle != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${date.dayOfMonth}일: $scheduleTitle", fontSize = 14.sp)

                                if (type == "personal") {
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, scheduleTitle)
                                            putExtra(CalendarContract.Events.ALL_DAY, true)
                                            val beginTime = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                                            val endTime = date.plusDays(0).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                                        }
                                        context.startActivity(intent)
                                    }) {
                                        Icon(Icons.Filled.Share, contentDescription = "Share to Calendar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
