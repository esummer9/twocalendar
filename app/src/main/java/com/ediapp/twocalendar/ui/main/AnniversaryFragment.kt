package com.ediapp.twocalendar.ui.main

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ediapp.twocalendar.Anniversary
import com.ediapp.twocalendar.AnniversaryActivity
import com.ediapp.twocalendar.Constants
import com.ediapp.twocalendar.DatabaseHelper
import com.ediapp.twocalendar.R
import com.ediapp.twocalendar.network.LunarApiService
import com.ediapp.twocalendar.ui.common.QrCodeImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// data class Schedule is already defined in PersonalScheduleFragment, so it can be reused.

private enum class SortType {
    DATE, CATEGORY, NAME
}

const val EXTRA_ANNIVERSARY_ID = "extra_anniversary_id"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnniversaryFragment(modifier: Modifier = Modifier, selectedDate: LocalDate? = null, scheduleUpdateTrigger: Int) { // Add scheduleUpdateTrigger parameter
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

    var showBirthdaySelectionDialog by remember { mutableStateOf(false) }
    var showBirthdayQrCodeDialog by remember { mutableStateOf(false) }
    var birthdayQrJson by remember { mutableStateOf<String?>(null) }
    var showCalendarShareDialog by remember { mutableStateOf(false) }

    var anniversariesToBulkAdd by remember { mutableStateOf<List<Anniversary>?>(null) }
    var yearToBulkAdd by remember { mutableIntStateOf(0) }

    var showCalendarAddSuccessDialog by remember { mutableStateOf(false) }
    var calendarAddSuccessCount by remember { mutableIntStateOf(0) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.WRITE_CALENDAR] == true && permissions[Manifest.permission.READ_CALENDAR] == true) {
                anniversariesToBulkAdd?.let { anniversaries ->
                    if (yearToBulkAdd != 0) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val successCount = addEventsToCalendar(context, anniversaries, yearToBulkAdd)
                            withContext(Dispatchers.Main) {
                                if (successCount > 0) {
                                    calendarAddSuccessCount = successCount
                                    showCalendarAddSuccessDialog = true
                                }
                                anniversariesToBulkAdd = null
                                yearToBulkAdd = 0
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "캘린더 읽기/쓰기 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    )


    val anniversaries by produceState(initialValue = emptyList<Anniversary>(), key1 = refreshTrigger, key2 = sortType) {
        val allAnniversaries = dbHelper.getAllAnniversaries()
        value = when (sortType) {
            SortType.DATE -> allAnniversaries.sortedBy { it.originDt }
            SortType.CATEGORY -> allAnniversaries.sortedBy { it.schedule.category }
            SortType.NAME -> allAnniversaries.sortedBy { it.schedule.title }
        }
    }

    if (showCalendarAddSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarAddSuccessDialog = false },
            title = { Text("알림") },
            text = { Text(stringResource(id = R.string.calendar_add_success_message, calendarAddSuccessCount)) },
            confirmButton = {
                Button(onClick = { showCalendarAddSuccessDialog = false }) {
                    Text("확인")
                }
            }
        )
    }

    if (showBirthdaySelectionDialog) {
        AnniversarySelectionDialog(
            anniversaries = anniversaries,
            onDismiss = { showBirthdaySelectionDialog = false },
            onConfirm = { selectedAnniversaries ->
                coroutineScope.launch(Dispatchers.IO) {
                    val result = selectedAnniversaries.map { ann ->
                        "${ann.schedule.category}|${ann.originDt}|${ann.schedule.title}|${ann.shortName}|${ann.schedule.calendarType}|${ann.isYearAccurate}"
                    }
                    withContext(Dispatchers.Main) {
                        birthdayQrJson = result.joinToString(Constants.my_sep)
                        showBirthdayQrCodeDialog = true
                    }
                }
            }
        )
    }

    if (showBirthdayQrCodeDialog) {
        BirthdayQrCodeDialog(json = birthdayQrJson, onDismiss = {
            showBirthdayQrCodeDialog = false
            birthdayQrJson = null
        })
    }

    if (showCalendarShareDialog) {
        CalendarShareDialog(
            anniversaries = anniversaries,
            onDismiss = { showCalendarShareDialog = false },
            onConfirm = { selectedAnniversaries, year ->
                showCalendarShareDialog = false
                if (selectedAnniversaries.size > 1) {
                    val hasWritePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    val hasReadPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

                    if (hasWritePermission && hasReadPermission) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val successCount = addEventsToCalendar(context, selectedAnniversaries, year)
                            withContext(Dispatchers.Main) {
                                if (successCount > 0) {
                                    calendarAddSuccessCount = successCount
                                    showCalendarAddSuccessDialog = true
                                }
                            }
                        }
                    } else {
                        anniversariesToBulkAdd = selectedAnniversaries
                        yearToBulkAdd = year
                        calendarPermissionLauncher.launch(
                            arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
                        )
                    }
                } else if (selectedAnniversaries.isNotEmpty()) {
                    // Single add
                    coroutineScope.launch {
                        val ann = selectedAnniversaries.first()
                        val originalDate = ann.originDt
                        var newDate: LocalDate? = null

                        if (ann.schedule.calendarType == "음력") {
                            val apiConfig = Constants.API_CONFIGS["LUNAR"]
                            val retrofit = Retrofit.Builder()
                                .baseUrl(apiConfig!!.baseUrl)
                                .addConverterFactory(SimpleXmlConverterFactory.create())
                                .build()
                            val service = retrofit.create(LunarApiService::class.java)
                            try {
                                val response = service.getLunarDate(
                                    serviceKey = apiConfig.serviceKey,
                                    fromSolYear = year.toString(),
                                    toSolYear = year.toString(),
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
                                Log.e("AnniversaryFragment", "API Call Failed: ${e.message}")
                            }
                        } else {
                            newDate = LocalDate.of(year, originalDate.month, originalDate.dayOfMonth)
                        }

                        if (newDate != null) {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                data = CalendarContract.Events.CONTENT_URI
                                putExtra(CalendarContract.Events.TITLE, ann.schedule.title)
                                val startTime = newDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                            }
                            context.startActivity(Intent.createChooser(intent, "기념일 공유"))
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "${ann.schedule.title} 날짜 변환에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )
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

    var isFabMenuExpanded by remember { mutableStateOf(false) }

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
                            icon = { Icon(Icons.Filled.Add, contentDescription = "추가") },
                            onClick = {
                                context.startActivity(Intent(context, AnniversaryActivity::class.java))
                                isFabMenuExpanded = false
                            }
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("QR공유") },
                            icon = { Icon(painter = painterResource(id = R.drawable.qr_share), "QR공유", modifier = Modifier.size(25.dp)) },
                            onClick = {
                                showBirthdaySelectionDialog = true
                                isFabMenuExpanded = false
                            }
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("달력공유") },
                            icon = { Icon(painter = painterResource(id = R.drawable.calendar_share), "캘린더로 공유", modifier = Modifier.size(25.dp)) },
                            onClick = {
                                showCalendarShareDialog = true
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
                val monthStr = "전체: ${anniversaryCount}개"
                Text(
                    text = monthStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                painterResource(R.drawable.sort),
                                modifier = Modifier.size(25.dp),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "정렬"
                            )
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
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(anniversaries) { anniversary ->
                        val originDt = anniversary.originDt
                        val applyDt = anniversary.applyDt
                        val schedule = anniversary.schedule
                        var showContextMenu by remember { mutableStateOf(false) }
                        var showAddDialog by remember { mutableStateOf(false) }
                        var showDeleteConfirmationDialog by remember { mutableStateOf(false) } // Add state for delete confirmation

                        val daysDiff = ChronoUnit.DAYS.between(applyDt, LocalDate.now())
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

                                Column(
                                    modifier = Modifier
                                        .weight(0.2f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = diffText,
                                        color = diffColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(style = SpanStyle(color = Color.DarkGray)) {
                                                append(schedule.category)
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                // 2번째 컬럼: 날짜/요일 및 제목
                                Column(
                                    modifier = Modifier
                                        .weight(0.65f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    val dayOfWeek = applyDt.dayOfWeek
                                    val dayOfWeekText = applyDt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

                                    val dayOfWeekColor = when (dayOfWeek) {
                                        DayOfWeek.SATURDAY -> Color.Blue
                                        DayOfWeek.SUNDAY -> Color.Red
                                        else -> Color.Unspecified
                                    }

                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(style = SpanStyle(color = Color.Unspecified, fontWeight = FontWeight.Bold)) {
                                                append(schedule.title)
                                                append(' ')
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text = buildAnnotatedString {
                                            append("(올해) ")

                                            withStyle(style = SpanStyle(color = Color.Unspecified, fontWeight = FontWeight.Bold)) {
                                                append("$applyDt ")
                                            }

                                            withStyle(style = SpanStyle(color = dayOfWeekColor, fontWeight = FontWeight.Bold)) {
                                                append("($dayOfWeekText)")
                                            }

                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )


                                    Text(
                                        text = buildAnnotatedString {
                                            val lunSolColor = when(schedule.calendarType) {
                                                "음력" -> Color.Blue
                                                else -> Color.Unspecified
                                            }
                                            withStyle(style = SpanStyle(color = lunSolColor, fontWeight = FontWeight.Normal)) {
                                                append("(${schedule.calendarType}) ")
                                            }
                                            append(originDt.toString())
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                }
                                Column(
                                    modifier = Modifier
                                        .weight(0.1f)
                                        .padding(horizontal = 1.dp)
                                ) {
                                    // Google Calendar 공유하기 자리
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.Events.TITLE, schedule.title)
                                            val startTime = anniversary.applyDt.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                                            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "기념일 공유"))
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.share),
                                            modifier = Modifier.size(25.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                            contentDescription = "구글캘린더"
                                        )
                                    }
                                }
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
                                    text = { Text("수정") },
                                    onClick = {
                                        showContextMenu = false
                                        val intent = Intent(context, AnniversaryActivity::class.java).apply {
                                            putExtra(EXTRA_ANNIVERSARY_ID, anniversary.id.toLong())
                                        }
                                        context.startActivity(intent)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("삭제") },
                                    onClick = {
                                        showContextMenu = false
                                        showDeleteConfirmationDialog = true // Show confirmation dialog
                                    }
                                )

                            }

                            // Delete Confirmation Dialog
                            if (showDeleteConfirmationDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirmationDialog = false },
                                    title = { Text("삭제 확인") },
                                    text = {
                                        Text(
                                            buildAnnotatedString {
                                                append("정말로 ")
                                                withStyle(style = SpanStyle(color = Color.DarkGray, fontWeight = FontWeight.Bold)) {
                                                    append(schedule.title)
                                                }
                                                append(" 기념일을 삭제하시겠습니까?")
                                            }
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                dbHelper.deleteAnniversary(anniversary.id)
                                                showDeleteConfirmationDialog = false
                                                refreshTrigger++
                                            }
                                        ) {
                                            Text("확인")
                                        }
                                    },
                                    dismissButton = {
                                        Button(onClick = { showDeleteConfirmationDialog = false }) {
                                            Text("취소")
                                        }
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
                                                    val originalDate = anniversary.originDt
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

                                                        val category = anniversary.schedule.category

                                                        var title = anniversary.schedule.title
                                                        if (category == "생일" || category == "기념일" )
                                                            title = if (title.contains(category)) title else "$title - $category"

                                                        dbHelper.addBirthdayToSchedule(
                                                            category = category,
                                                            applyDt = newDate,
                                                            title = title
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
}

private suspend fun addEventsToCalendar(context: Context, anniversaries: List<Anniversary>, year: Int): Int {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return 0
    }

    val calendarId = getPrimaryCalendarId(context)
    if (calendarId == null) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "캘린더를 찾을 수 없습니다. 캘린더 앱에서 기본 캘린더를 설정해주세요.", Toast.LENGTH_LONG).show()
        }
        return 0
    }

    var successCount = 0
    for (ann in anniversaries) {
        val originalDate = ann.originDt
        var newDate: LocalDate? = null

        if (ann.schedule.calendarType == "음력") {
            val apiConfig = Constants.API_CONFIGS["LUNAR"]
            val retrofit = Retrofit.Builder()
                .baseUrl(apiConfig!!.baseUrl)
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
            val service = retrofit.create(LunarApiService::class.java)
            try {
                val response = service.getLunarDate(
                    serviceKey = apiConfig.serviceKey,
                    fromSolYear = year.toString(),
                    toSolYear = year.toString(),
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
                Log.e("AnniversaryFragment", "API Call Failed: ${e.message}")
            }
        } else {
            newDate = LocalDate.of(year, originalDate.month, originalDate.dayOfMonth)
        }

        if (newDate != null) {
            val startTime = newDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, startTime)
                put(CalendarContract.Events.TITLE, ann.schedule.title)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                put(CalendarContract.Events.ALL_DAY, 1)
            }
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            successCount++
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "${ann.schedule.title} 날짜 변환에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    return successCount
}

private fun getPrimaryCalendarId(context: Context): Long? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return null
    }

    val projection = arrayOf(CalendarContract.Calendars._ID)
    val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.IS_PRIMARY} = 1"

    context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(0)
        }
    }

    // If no primary, get the first visible calendar
    val selection2 = "${CalendarContract.Calendars.VISIBLE} = 1"
    context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection2, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(0)
        }
    }
    return null
}

@Preview(showBackground = true)
@Composable
fun AnniversaryFragmentPreview() {
    AnniversaryFragment(scheduleUpdateTrigger = 0)
}
