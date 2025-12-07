package com.ediapp.twocalendar.ui.main

import android.content.Intent
import android.graphics.Bitmap
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward // Use AutoMirrored version
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf // Import mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ediapp.twocalendar.Constants
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.R
import com.ediapp.twocalendar.ui.common.QrCodeImage
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Schedule(val id: Int, val category: String, val title: String, val calendarType: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
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
fun EditScheduleDialog(
    schedule: Pair<LocalDate, Schedule>,
    onDismiss: () -> Unit,
    onConfirm: (Int, LocalDate, String) -> Unit
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
        title = { Text("일정 수정") },
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
                    label = { Text("제목") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        onConfirm(schedule.second.id, newDate, newTitle)
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
fun DuplicateScheduleDialog(
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
        title = { Text("일정 복제") },
        text = {
            Column {
                Text("'${schedule.second.title}' 일정을 복제할 날짜를 선택하세요.")
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

@Composable
fun PersonalScheduleQrCodeDialog(
    json: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("개인일정 공유") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!json.isNullOrEmpty()) {
                    QrCodeImage(data = json, size = 800) // Use a smaller size for the dialog
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("다른 핸드폰에서 QR 코드를 스캔하여 일정 정보를 가져올 수 있습니다.")
                } else {
                    Text("일정 정보가 없습니다.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

@Composable
fun PersonalScheduleSelectionDialog(
    schedules: List<Pair<LocalDate, Schedule>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<LocalDate, Schedule>>) -> Unit
) {
    val selectedSchedules = remember { mutableStateOf<List<Pair<LocalDate, Schedule>>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("공유할 일정 선택") },
        text = {
            LazyColumn {
                items(schedules) { schedulePair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val currentSelection = selectedSchedules.value.toMutableList()
                                if (currentSelection.contains(schedulePair)) {
                                    currentSelection.remove(schedulePair)
                                } else {
                                    currentSelection.add(schedulePair)
                                }
                                selectedSchedules.value = currentSelection
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedSchedules.value.contains(schedulePair),
                            onCheckedChange = { isChecked ->
                                val currentSelection = selectedSchedules.value.toMutableList()
                                if (isChecked) {
                                    currentSelection.add(schedulePair)
                                } else {
                                    currentSelection.remove(schedulePair)
                                }
                                selectedSchedules.value = currentSelection
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${schedulePair.first}: ${schedulePair.second.title}")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedSchedules.value) }) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonalScheduleFragment(modifier: Modifier = Modifier, selectedDate: LocalDate? = null, scheduleUpdateTrigger: Int) { // Add scheduleUpdateTrigger parameter
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var baseMonth by remember { mutableStateOf(YearMonth.now()) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // Use mutableIntStateOf
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var showScheduleSelectionDialog by remember { mutableStateOf(false) }
    var showScheduleQrCodeDialog by remember { mutableStateOf(false) }
    var scheduleQrJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            baseMonth = YearMonth.from(date)
        }
    }

    val schedules by produceState(initialValue = emptyList<Pair<LocalDate, Schedule>>(), key1 = baseMonth, key2 = refreshTrigger, key3 = scheduleUpdateTrigger) { // Add scheduleUpdateTrigger as a key
        val firstMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth, listOf("personal", "생일", "기념일"))
        val secondMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth.plusMonths(1), listOf("personal", "생일", "기념일"))

        val allSchedulesRaw = (firstMonthSchedulesRaw.keys + secondMonthSchedulesRaw.keys).associateWith {
            (firstMonthSchedulesRaw[it].orEmpty() + Constants.my_sep + secondMonthSchedulesRaw[it].orEmpty()).trim()
        }

        value = allSchedulesRaw.entries
            .flatMap { (date, scheduleString) ->
                scheduleString.split(Constants.my_sep).mapNotNull { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split('|', limit = 3) // limit 3 to get id, category, title
                        if (parts.size == 3) {
                            val id = parts[0].toInt()
                            val category = parts[1]
                            val title = parts[2]
                            date to Schedule(id = id, category = category, title = title)
                        } else {
                            return@mapNotNull null
                        }
                    }
                    else {
                        return@mapNotNull null
                    }
                }
            }
            .sortedBy { it.first }
    }

    var showDeleteDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var expandedItemIndex by remember { mutableStateOf<Int?>(null) }


    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("일정 삭제") },
            text = { Text("'${showDeleteDialog!!.second.title}' 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dbHelper.deletePersonalSchedule(showDeleteDialog!!.second.id)
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
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, title ->
                dbHelper.addPersonalSchedule(date, title)
                refreshTrigger++
                showAddDialog = false
            },
            initialDate = selectedDate
        )
    }

    if (showEditDialog != null) {
        EditScheduleDialog(
            schedule = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = { id, newDate, newTitle ->
                dbHelper.updatePersonalSchedule(id, newDate, newTitle)
                refreshTrigger++
                showEditDialog = null
            }
        )
    }

    if (showDuplicateDialog != null) {
        DuplicateScheduleDialog(
            schedule = showDuplicateDialog!!,
            onDismiss = { showDuplicateDialog = null },
            onConfirm = { date, title ->
                dbHelper.addPersonalSchedule(date, title)
                refreshTrigger++
                showDuplicateDialog = null
            }
        )
    }

    if (showScheduleSelectionDialog) {
        PersonalScheduleSelectionDialog(
            schedules = schedules,
            onDismiss = { showScheduleSelectionDialog = false },
            onConfirm = { selectedSchedules ->
                coroutineScope.launch {
                    val result = selectedSchedules.map { schedulePair ->
                        "${schedulePair.first}|${schedulePair.second.title}"
                    }
                    scheduleQrJson = result.joinToString(Constants.my_sep)
                    showScheduleQrCodeDialog = true
                    showScheduleSelectionDialog = false // dismiss selection dialog
                }
            }
        )
    }

    if (showScheduleQrCodeDialog) {
        PersonalScheduleQrCodeDialog(json = scheduleQrJson, onDismiss = {
            showScheduleQrCodeDialog = false
            scheduleQrJson = null
        })
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(visible = isFabMenuExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            text = { Text("추가") },
                            icon = { Icon(Icons.Filled.Add, "추가") },
                            onClick = {
                                showAddDialog = true
                                isFabMenuExpanded = false
                            }
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("QR공유") },
                            icon = { Icon(painter = painterResource(id = R.drawable.qr_share)

                                , "QR공유") }
                            ,
                            onClick = {
                                showScheduleSelectionDialog = true
                                isFabMenuExpanded = false
                            }
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = !isFabMenuExpanded }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "메뉴 열기")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "다음달") // Use AutoMirrored version
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
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    itemsIndexed(schedules) { index, (date, schedule) ->
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { /* No action on simple click */ },
                                        onLongClick = { expandedItemIndex = index }
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

                                // 3번째 컬럼: 공유 버튼
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, schedule.title)
                                        val startTime = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                                        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "일정 공유"))
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.share),
                                        modifier = Modifier.size(25.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = "구글캘린더"
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedItemIndex == index,
                                onDismissRequest = { expandedItemIndex = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("수정") },
                                    onClick = {
                                        showEditDialog = date to schedule
                                        expandedItemIndex = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("복제") },
                                    onClick = {
                                        showDuplicateDialog = date to schedule
                                        expandedItemIndex = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("삭제") },
                                    onClick = {
                                        showDeleteDialog = date to schedule
                                        expandedItemIndex = null
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
}

@Preview(showBackground = true)
@Composable
fun PersonalScheduleFragmentPreview() {
    TwocalendarTheme {
        // Removed: val sampleSchedules = listOf(...)
        PersonalScheduleFragment(
            modifier = Modifier.fillMaxSize(),
            scheduleUpdateTrigger = 0 // Provide a default value for preview
        )
    }
}
