package com.ediapp.twocalendar.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.Constants
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.R
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// data class Schedule is already defined in PersonalScheduleFragment, so it can be reused.

private enum class SortType {
    DATE, D_DAY, NAME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdayDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit,
    initialDate: LocalDate? = null
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now(ZoneOffset.UTC)) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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
        title = { Text("생일") },
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
                    label = { Text("이름") },
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
fun EditBirthdayDialog(
    schedule: Pair<LocalDate, Schedule>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String, LocalDate, String) -> Unit
) {
    var newTitle by remember { mutableStateOf(schedule.second.title) }
    var newDate by remember { mutableStateOf(schedule.first) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = newDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            newDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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
        title = { Text("생일 수정") },
        text = {
            Column {
                Box {
                    TextField(
                        value = newDate.toString(),
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
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(schedule.first, schedule.second.title, newDate, newTitle)
                    }
                }
            ) {
                Text("수정")
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
fun DuplicateBirthdayDialog(
    schedule: Pair<LocalDate, Schedule>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit
) {
    var newDate by remember { mutableStateOf(schedule.first) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = schedule.first.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            newDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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
        title = { Text("생일 복제") },
        text = {
            Column {
                Text("'${schedule.second.title}' 생일을 복제할 날짜를 선택하세요.")
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    TextField(
                        value = newDate.toString(),
                        onValueChange = {},
                        label = { Text("새 날짜") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClick = { showDatePicker = true })
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(newDate, schedule.second.title)
                    onDismiss()
                }
            ) {
                Text("복제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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
    val birthdayCount by produceState(initialValue = 0, key1 = refreshTrigger) {
        value = dbHelper.getBirthdayCount()
    }

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            baseMonth = YearMonth.from(date)
        }
    }

    val schedules by produceState<List<Pair<LocalDate, Schedule>>>(initialValue = emptyList(), baseMonth, refreshTrigger, scheduleUpdateTrigger, sortType) {
        val firstMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth, listOf("birthday"))
        val secondMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth.plusMonths(1), listOf("birthday"))

        val allSchedulesRaw = (firstMonthSchedulesRaw.keys + secondMonthSchedulesRaw.keys).associateWith {
            (firstMonthSchedulesRaw[it].orEmpty() + Constants.my_sep + secondMonthSchedulesRaw[it].orEmpty()).trim()
        }

        val parsedSchedules = allSchedulesRaw.entries
            .flatMap { (date, scheduleString) ->
                scheduleString.split(Constants.my_sep).mapNotNull { line ->
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

        value = when (sortType) {
            SortType.DATE -> parsedSchedules.sortedBy { it.first }
            SortType.NAME -> parsedSchedules.sortedBy { it.second.title }
            SortType.D_DAY -> {
                val today = LocalDate.now()
                parsedSchedules.sortedBy { ChronoUnit.DAYS.between(it.first, today) }
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var showQrCodeDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var expandedItem by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }


    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("생일 삭제") },
            text = { Text("'${showDeleteDialog!!.second}' 생일을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dbHelper.deleteBirthday(showDeleteDialog!!.first, showDeleteDialog!!.second)
                        refreshTrigger++
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
        AddBirthdayDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, title ->
                dbHelper.addBirthday(date, title)
                refreshTrigger++
                showAddDialog = false
            },
            initialDate = selectedDate
        )
    }

    if (showEditDialog != null) {
        EditBirthdayDialog(
            schedule = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = { oldDate, oldTitle, newDate, newTitle ->
                dbHelper.updateBirthday(oldDate, oldTitle, newDate, newTitle)
                refreshTrigger++
                showEditDialog = null
            }
        )
    }

    if (showDuplicateDialog != null) {
        DuplicateBirthdayDialog(
            schedule = showDuplicateDialog!!,
            onDismiss = { showDuplicateDialog = null },
            onConfirm = { date, title ->
                dbHelper.addBirthday(date, title)
                refreshTrigger++
                showDuplicateDialog = null
            }
        )
    }

    if (showQrCodeDialog != null) {
        QrCodeDialog(schedule = showQrCodeDialog!!, onDismiss = { showQrCodeDialog = null })
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val monthStr = "전체 생일: ${birthdayCount}개"
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
                        text = { Text("디데이순") },
                        onClick = {
                            sortType = SortType.D_DAY
                            showSortMenu = false
                        }
                    )
                }
            }
        }
        if (schedules.isEmpty()) {
            Text(
                text = "생일 정보가 없습니다.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            val today = LocalDate.now()
            LazyColumn (modifier = Modifier.fillMaxHeight()){
                items(schedules) { (date, schedule) ->
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { /* No action on simple click */ },
                                    onLongClick = { expandedItem = date to schedule }
                                )
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

                            // 3번째 컬럼: 공유 버튼
                            IconButton(onClick = { showQrCodeDialog = date to schedule }) {
                                Icon(painter = painterResource(id = R.drawable.qr_code), contentDescription = "QR Code")
                            }
                        }

                        DropdownMenu(
                            expanded = expandedItem == date to schedule,
                            onDismissRequest = { expandedItem = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("수정") },
                                onClick = {
                                    showEditDialog = date to schedule
                                    expandedItem = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("복제") },
                                onClick = {
                                    showDuplicateDialog = date to schedule
                                    expandedItem = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                onClick = {
                                    showDeleteDialog = date to schedule.title
                                    expandedItem = null
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
    TwocalendarTheme {
        // Removed: val sampleSchedules = listOf(...)
        BirthDayFragment(
            modifier = Modifier.fillMaxSize(),
            scheduleUpdateTrigger = 0 // Provide a default value for preview
        )
    }
}
