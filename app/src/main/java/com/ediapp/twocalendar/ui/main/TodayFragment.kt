package com.ediapp.twocalendar.ui.main

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.Saying
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun TodayFragment(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    val sharedPref = remember { context.getSharedPreferences("important_day_prefs", Context.MODE_PRIVATE) }
    var sayingNo by remember { mutableStateOf(sharedPref.getInt("saying_no", 1)) }

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

    val dayOfWeekColor = when {
        hasPersonalSchedule -> Color.Gray
        today.dayOfWeek == DayOfWeek.SATURDAY -> Color.Blue
        today.dayOfWeek == DayOfWeek.SUNDAY -> Color.Red
        else -> Color.Unspecified
    }

    val nextSayingNo = if (sayingNo >= sayingCount) 1 else sayingNo + 1
    sharedPref
        .edit()
        .putInt("saying_no", nextSayingNo)
        .apply()


    Column(
        modifier = modifier.fillMaxSize(),
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
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = dayOfWeekColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "올해의 ${today.dayOfYear}번째 날",
                fontSize = 20.sp
            )

            val importantDayDateString = sharedPref.getString("important_day_date", null)
            importantDayDateString?.let {
                val importantDate = LocalDate.parse(it)
                val title = sharedPref.getString("important_day_title", "")

                val diff = ChronoUnit.DAYS.between(importantDate, today)
                val dDayText = when {
                    diff > 0 -> "$title +$diff"
                    diff < 0 -> "$title -$diff"
                    else -> "D-Day"
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dDayText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
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
