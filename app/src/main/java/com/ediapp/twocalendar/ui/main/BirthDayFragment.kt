package com.ediapp.twocalendar.ui.main

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ediapp.twocalendar.Anniversary
import com.ediapp.twocalendar.AnniversaryActivity
import com.ediapp.twocalendar.Constants
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.LunarApiService
import com.ediapp.twocalendar.R
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// data class Schedule is already defined in PersonalScheduleFragment, so it can be reused.

private enum class SortType {
    DATE, CATEGORY, NAME
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BirthDayFragment(modifier: Modifier = Modifier, selectedDate: LocalDate? = null, scheduleUpdateTrigger: Int) { // Add scheduleUpdateTrigger parameter
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var baseMonth by remember { mutableStateOf(YearMonth.now()) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // Use mutableIntStateOf
    var sortType by remember { mutableStateOf(SortType.DATE) }
    var showSortMenu by remember { mutableStateOf(false) }
    val anniversaryCount by produceState(initialValue = 0, key1 = refreshTrigger) {
        value = dbHelper.getAnniversaryCount()
    }
    val coroutineScope = rememberCoroutineScope()

    val anniversaries by produceState(initialValue = emptyList<Anniversary>(), key1 = refreshTrigger, key2 = sortType) {
        val allAnniversaries = dbHelper.getAllAnniversaries()
        value = when (sortType) {
            SortType.DATE -> allAnniversaries.sortedBy { it.date }
            SortType.CATEGORY -> allAnniversaries.sortedBy { it.schedule.category }
            SortType.NAME -> allAnniversaries.sortedBy { it.schedule.title }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            baseMonth = YearMonth.from(date)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val monthStr = "전체: ${anniversaryCount}개"
            Text(
                text = monthStr,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    context.startActivity(Intent(context, AnniversaryActivity::class.java))
                }) {
                    Icon(
                        painterResource(R.drawable.new_record),
                        modifier = Modifier.size(25.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "기념일 추가"
                    )
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            painterResource(R.drawable.sort),
                            modifier = Modifier.size(25.dp)
                            , tint = MaterialTheme.colorScheme.primary
                            , contentDescription = "정렬")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("날짜순") },
                            onClick = {
                                sortType = SortType.DATE
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("이름순") },
                            onClick = {
                                sortType = SortType.NAME
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("카테고리순") },
                            onClick = {
                                sortType = SortType.CATEGORY
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        if (anniversaries.isEmpty()) {
            Text(
                text = "기념일이 없습니다.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn (modifier = Modifier.fillMaxHeight()){
                items(anniversaries) { anniversary ->
                    val date = anniversary.date
                    val schedule = anniversary.schedule
                    var showContextMenu by remember { mutableStateOf(false) }
                    var showAddDialog by remember { mutableStateOf(false) }

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { /* No action on simple click */ },
                                    onLongClick = { showContextMenu = true }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1번째 컬럼: 카테고리
                            Text(
                                text = schedule.category,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.25f)
                            )

                            // 2번째 컬럼: 날짜/요일 및 제목
                            Column(
                                modifier = Modifier
                                    .weight(0.5f)
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
                                        append()
                                        withStyle(style = SpanStyle(color = dayOfWeekColor, fontWeight = FontWeight.Bold)) {
                                            append("($dayOfWeekText)")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = schedule.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 3번째 컬럼: 음력/양력
                            Text(
                                text = schedule.calendarType ?: "",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.25f),
                                textAlign = TextAlign.Center
                            )
                        }
                        DropdownMenu(
                            expanded = showContextMenu,
                            onDismissRequest = { showContextMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("개인일정에 추가") },
                                onClick = {
                                    showContextMenu = false
                                    showAddDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                onClick = {
                                    dbHelper.deleteAnniversary(anniversary.id)
                                    showContextMenu = false
                                    refreshTrigger++
                                }
                            )
                        }

                        if (showAddDialog) {
                            val currentYear = LocalDate.now().year
                            var selectedYear by remember { mutableIntStateOf(currentYear) }
                            val yearRange = (currentYear - 0)..(currentYear + 2)

                            AlertDialog(
                                onDismissRequest = { showAddDialog = false },
                                title = { Text("개인일정에 복사") },
                                text = {
                                    Column {
                                        Text("몇년도 값으로 복사 할까요?")
                                        yearRange.forEach { year ->
                                            Text(
                                                text = year.toString(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedYear = year }
                                                    .padding(vertical = 8.dp),
                                                color = if (selectedYear == year) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                                textAlign = TextAlign.Center,
                                                fontWeight = if (selectedYear == year) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val originalDate = anniversary.date
                                                var newDate: LocalDate? = null

                                                if (anniversary.schedule.calendarType == "음력") {

                                                    val apiConfig = Constants.API_CONFIGS["LUNAR"]
                                                    val retrofit = Retrofit.Builder()
                                                        .baseUrl(apiConfig!!.baseUrl)
                                                        .addConverterFactory(SimpleXmlConverterFactory.create())
                                                        .build()
                                                    val service = retrofit.create(LunarApiService::class.java)

                                                    try {
                                                        val response = service.getLunarDate(
                                                            serviceKey = apiConfig.serviceKey,
                                                            fromSolYear = selectedYear.toString(),
                                                            toSolYear = selectedYear.toString(),
                                                            lunMonth = String.format("%02d", originalDate.monthValue),
                                                            lunDay = String.format("%02d", originalDate.dayOfMonth)
                                                        )

                                                        if (response.isSuccessful) {
                                                            val lunarItem = response.body()?.body?.items?.itemList?.firstOrNull()
                                                            if (lunarItem != null) {
                                                                newDate = LocalDate.of(lunarItem.solYear, lunarItem.solMonth, lunarItem.solDay)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("BirthDayFragment", "API Call Failed: ${e.message}")
                                                    }
                                                } else {
                                                    newDate = LocalDate.of(selectedYear, originalDate.month, originalDate.dayOfMonth)
                                                }

                                                if (newDate != null) {
                                                    dbHelper.addBirthdayToSchedule(
                                                        category = anniversary.schedule.category,
                                                        applyDt = newDate,
                                                        title = anniversary.schedule.title
                                                    )
                                                    Toast.makeText(context, "${selectedYear}년 개인일정에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "날짜 변환에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                            showAddDialog = false
                                        }
                                    ) {
                                        Text("확인")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = { showAddDialog = false }) {
                                        Text("취소")
                                    }
                                }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirthDayFragmentPreview() {
    BirthDayFragment(scheduleUpdateTrigger = 0)
}
