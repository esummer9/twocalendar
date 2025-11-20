package com.ediapp.twocalendar.ui.main

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward // Use AutoMirrored version
// Removed: import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf // Import mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Schedule(val category: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit,
    initialDate: LocalDate? = null
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
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
fun DuplicateScheduleDialog(
    schedule: Pair<LocalDate, Schedule>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit
) {
    var newDate by remember { mutableStateOf(schedule.first) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
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
fun QrCodeDialog(schedule: Pair<LocalDate, Schedule>, onDismiss: () -> Unit) {
    val qrCodeBitmap = remember {
        val text = "${schedule.first}|${schedule.second.title}"
        val size = 512
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.size(300.dp)) {
            Image(
                bitmap = qrCodeBitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonalScheduleFragment(modifier: Modifier = Modifier, selectedDate: LocalDate? = null, scheduleUpdateTrigger: Int) { // Add scheduleUpdateTrigger parameter
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var baseMonth by remember { mutableStateOf(YearMonth.now()) }
    var refreshTrigger by remember { mutableIntStateOf(0) } // Use mutableIntStateOf

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            baseMonth = YearMonth.from(date)
        }
    }

    val schedules by produceState(initialValue = emptyList<Pair<LocalDate, Schedule>>(), key1 = baseMonth, key2 = refreshTrigger, key3 = scheduleUpdateTrigger) { // Add scheduleUpdateTrigger as a key
        val firstMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth, listOf("personal"))
        val secondMonthSchedulesRaw = dbHelper.getDaysForCategoryMonth(baseMonth.plusMonths(1), listOf("personal"))

        val allSchedulesRaw = (firstMonthSchedulesRaw.keys + secondMonthSchedulesRaw.keys).associateWith {
            (firstMonthSchedulesRaw[it].orEmpty() + Constants.my_sep + secondMonthSchedulesRaw[it].orEmpty()).trim()
        }

        value = allSchedulesRaw.entries
            .flatMap { (date, scheduleString) ->
                scheduleString.split(Constants.my_sep).mapNotNull { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split('|', limit = 2)
                        if (parts.size == 2) {
                            date to Schedule(category = parts[0], title = parts[1])
                        } else {
                            return@mapNotNull null
                        }
                    } else {
                        return@mapNotNull null
                    }
                }
            }
            .sortedBy { it.first }
    }

    var showDeleteDialog by remember { mutableStateOf<Pair<LocalDate, String>?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var showQrCodeDialog by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }
    var expandedItem by remember { mutableStateOf<Pair<LocalDate, Schedule>?>(null) }


    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("일정 삭제") },
            text = { Text("'${showDeleteDialog!!.second}' 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dbHelper.deletePersonalSchedule(showDeleteDialog!!.first, showDeleteDialog!!.second)
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
                                Icon(Icons.Default.Share, contentDescription = "공유")
                            }
                        }

                        DropdownMenu(
                            expanded = expandedItem == date to schedule,
                            onDismissRequest = { expandedItem = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                onClick = {
                                    showDeleteDialog = date to schedule.title
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
fun PersonalScheduleFragmentPreview() {
    TwocalendarTheme {
        // Removed: val sampleSchedules = listOf(...)
        PersonalScheduleFragment(
            modifier = Modifier.fillMaxSize(),
            scheduleUpdateTrigger = 0 // Provide a default value for preview
        )
    }
}
