package com.ediapp.twocalendar.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ediapp.twocalendar.Anniversary
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.R
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
            val monthStr = "전체 기념일: ${anniversaryCount}개"
            Text(
                text = monthStr,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        painterResource(R.drawable.sort), modifier = Modifier.size(30.dp)
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
                                text = { Text("삭제") },
                                onClick = {
                                    dbHelper.deleteAnniversary(anniversary.id)
                                    showContextMenu = false
                                    refreshTrigger++
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
