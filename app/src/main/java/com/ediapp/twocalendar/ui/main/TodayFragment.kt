package com.ediapp.twocalendar.ui.main

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.Saying
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayFragment(modifier: Modifier = Modifier) {
    var today by remember { mutableStateOf(LocalDate.now()) }
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    val sharedPref = remember { context.getSharedPreferences("important_day_prefs", Context.MODE_PRIVATE) }
    var sayingNo by remember { mutableStateOf(sharedPref.getInt("saying_no", 1)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val showDayOfYear = sharedPref.getBoolean("show_day_of_year", true)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        val confirmEnabled by remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            today = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    },
                    enabled = confirmEnabled
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val saying by produceState<Saying?>(initialValue = null, key1 = sayingNo) {
        value = dbHelper.getSayingByNo(sayingNo)
    }

    val sayingCount by produceState(initialValue = 0) {
        value = dbHelper.getSayingCount()
    }

    val hasPersonalSchedule by produceState(initialValue = false, key1 = today) {
        val personalSchedules = dbHelper.getDaysForCategoryMonth(YearMonth.from(today), listOf("personal"))
        value = personalSchedules.containsKey(today)
    }

    val isHoliday by produceState(initialValue = false, key1 = today) {
        val holidays = dbHelper.getDaysForCategoryMonth(YearMonth.from(today), listOf("holiday"))
        value = holidays.containsKey(today)
    }

    val dayOfWeekColor = when {
        hasPersonalSchedule -> Color.Gray
        isHoliday -> Color.Red
        today.dayOfWeek == DayOfWeek.SATURDAY -> Color.Blue
        today.dayOfWeek == DayOfWeek.SUNDAY -> Color.Red
        else -> Color.Unspecified
    }

    val nextSayingNo = if (sayingNo >= sayingCount) 1 else sayingNo + 1
    sharedPref
        .edit()
        .putInt("saying_no", nextSayingNo)
        .apply()

    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 100f
                        when {
                            offsetX > threshold -> {
                                today = today.minusDays(1)
                            }
                            offsetX < -threshold -> {
                                today = today.plusDays(1)
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top part
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${today.year}년 ${today.monthValue}월",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN),
                    fontSize = 20.sp,
                    color = dayOfWeekColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${today.dayOfMonth}",
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            showDatePicker = true
                        })
                    },
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    color = dayOfWeekColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (showDayOfYear) {
                    Text(
                        text = "올해의 ${today.dayOfYear}번째 날",
                        fontSize = 20.sp
                    )
                }
                
                val nearestHolidayInfo by produceState<Pair<String, LocalDate>?>(initialValue = null, key1 = today) {
                    val holidaysMap = mutableMapOf<LocalDate, String>()

                    // Fetch holidays for the current and next year
                    // Check for next 24 months to cover a reasonable range for upcoming holidays
                    for (i in 0 until 24) { 
                        val yearMonth = YearMonth.now(ZoneId.systemDefault()).plusMonths(i.toLong())
                        holidaysMap.putAll(dbHelper.getDaysForCategoryMonth(yearMonth, listOf("holiday")))
                    }

                    var minDiff = Long.MAX_VALUE
                    var nearestHolidayDate: LocalDate? = null
                    var nearestHolidayTitle: String? = null

                    for ((date, categoryDayString) in holidaysMap) {
                        if (date.isAfter(today)) { // Only consider future holidays
                            val diff = ChronoUnit.DAYS.between(today, date)
                            if (diff < minDiff) {
                                minDiff = diff
                                nearestHolidayDate = date
                                nearestHolidayTitle = categoryDayString.split("|").lastOrNull() 
                            }
                        }
                    }

                    if (nearestHolidayDate != null && nearestHolidayTitle != null) {
                        value = Pair(nearestHolidayTitle, nearestHolidayDate)
                    } else {
                        value = null
                    }
                }

                nearestHolidayInfo?.let { (title, date) ->
                    val diff = ChronoUnit.DAYS.between(today, date)
                    val formattedDate = date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
                    Text(
                        text = "D-${diff} ${title} ${formattedDate}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val importantDayCalculation = sharedPref.getString("important_day_calculation", "지난일수")
                if(importantDayCalculation != "표시안함" ) {
                    val importantDayDateString = sharedPref.getString("important_day_date", null)
                    val title = sharedPref.getString("important_day_title", "")

                    importantDayDateString?.let {
                        val importantDate = LocalDate.parse(it)
                        var diff: Long = 0
                        var dDayText = ""

//                        Log.d("ImportantDay", "importantDayCalculation: $importantDayCalculation $importantDate $today ")

                        if(importantDate > today) {
                            diff = ChronoUnit.DAYS.between(today, importantDate)
                            dDayText = "$title ${-diff}"
                        } else {
                            dDayText = if (importantDayCalculation == "남은일수") {

                                val targetMonthDay = MonthDay.of(importantDate.monthValue, importantDate.dayOfMonth)
                                var nextOccurrence = targetMonthDay.atYear(today.year)
                                while (nextOccurrence.isBefore(today)) {
                                    nextOccurrence = nextOccurrence.plusYears(1)
                                }

                                val diff = ChronoUnit.DAYS.between(today, nextOccurrence)
//                                Log.d("ImportantDay", "nextOccurrence: $nextOccurrence $diff")
                                when {
                                    diff > 0 -> "$title -$diff"
                                    diff < 0 -> "$title +${-diff}"
                                    else -> "$title D-Day"
                                }
                            } else { // "지난일수"
                                val diff = ChronoUnit.DAYS.between(importantDate, today)
                                when {
                                    diff > 0 -> "$title +$diff"
                                    diff < 0 -> "$title -$diff"
                                    else -> "$title D-Day"
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dDayText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
            }
            // Bottom part
            Box(
                modifier = Modifier
                    .border(1.dp, Color.Gray)
                    .padding(16.dp)
                    .clickable {
                        val nextSayingNo = if (sayingNo >= sayingCount) 1 else sayingNo + 1
                        sharedPref
                            .edit()
                            .putInt("saying_no", nextSayingNo)
                            .apply()
                        sayingNo = nextSayingNo
                    }
            ) {
                Column {
                    saying?.let {
                        Text(
                            text = it.saying,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "- ${it.author}",
                            fontSize = 20.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}
