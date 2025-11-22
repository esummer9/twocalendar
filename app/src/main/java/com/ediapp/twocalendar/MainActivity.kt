package com.ediapp.twocalendar

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.ediapp.twocalendar.ui.main.PersonalScheduleFragment
import com.ediapp.twocalendar.ui.main.TodayFragment
import com.ediapp.twocalendar.ui.main.TwoMonthFragment
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val dbHelper by lazy { DatabaseHelper(this) }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this)

        val androidId = getAndroidId(this) // Renamed variable
        Log.d(TAG, "Android ID: $androidId")

        enableEdgeToEdge()
        setContent {
            var isFetching by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    fetchAndSaveHolidays()
                }
                isFetching = false
            }

            TwocalendarTheme {
                if (isFetching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("공휴일 정보를 가져오고 있습니다")
                        }
                    }
                } else {
                    MainScreenWithBottomBar(dbHelper = dbHelper, fetchHolidaysForYear = { year ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            fetchAndSaveHolidays(year)
                        }
                    })
                }
            }
        }
    }

    suspend fun fetchAndSaveHolidays(yearToFetch: Int? = null) {
        val apiKeys = listOf("NATIONAL_HOLIDAY", "HOLIDAY")

        val yearsToFetch = if (yearToFetch != null) {
            listOf(yearToFetch)
        } else {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            listOf(currentYear, currentYear - 1, currentYear + 1)
        }

        coroutineScope {
            yearsToFetch.forEach { year ->
                launch {
                    apiKeys.forEach { apiKey ->

                        val category = apiKey.lowercase()

                        if (dbHelper.countDaysByCategoryAndYear(category, year) == 0) {
                            val holidayApiConfig = Constants.API_CONFIGS[apiKey]
                                ?: throw IllegalArgumentException("API config for $apiKey not found")

                            val retrofit = Retrofit.Builder()
                                .baseUrl(holidayApiConfig.baseUrl)
                                .addConverterFactory(SimpleXmlConverterFactory.create())
                                .build()

                            val service = retrofit.create(HolidayApiService::class.java)

                            try {
                                val response = service.getHolidays(
                                    serviceKey = holidayApiConfig.serviceKey,
                                    year = year,
                                    month = "" // Fetch for the whole year
                                )

                                if (response.isSuccessful) {
                                    val holidays = response.body()?.body?.items?.itemList
                                    holidays?.forEach { holiday ->
                                        holiday.locdate?.let {
                                            dbHelper.addDay(
                                                source = "data.go.kr",
                                                category = category,
                                                type = "date",
                                                dataKey = holiday.locdate!!,
                                                title = holiday.dateName ?: ""
                                            )
                                        }
                                    }
                                } else {
                                    Log.e("MainActivity", "Error fetching holidays for $apiKey: ${response.errorBody()?.string()}")
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Exception fetching holidays for $apiKey", e)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalScheduleSelectionDialog(
    allSchedules: List<String>,
    selectedSchedules: List<String>,
    showHolidays: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, Boolean) -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedSchedules.toSet()) }
    var holidaysChecked by remember { mutableStateOf(showHolidays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("개인일정 표시") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { holidaysChecked = !holidaysChecked }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = holidaysChecked,
                        onCheckedChange = { holidaysChecked = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("공휴일")
                }

                LazyColumn {
                    items(allSchedules) { schedule ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelection = currentSelection.toMutableSet()
                                    if (schedule in newSelection) {
                                        newSelection.remove(schedule)
                                    } else {
                                        newSelection.add(schedule)
                                    }
                                    currentSelection = newSelection
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = schedule in currentSelection,
                                onCheckedChange = { isChecked ->
                                    val newSelection = currentSelection.toMutableSet()
                                    if (isChecked) {
                                        newSelection.add(schedule)
                                    } else {
                                        newSelection.remove(schedule)
                                    }
                                    currentSelection = newSelection
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(schedule)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentSelection.toList(), holidaysChecked) }) {
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

// New Composable for adding personal schedules
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit,
    initialTitle: String = "",
    initialDate: LocalDate? = null
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var title by remember { mutableStateOf(initialTitle) }

    val year = selectedDate.year
    val month = selectedDate.monthValue - 1 // DatePickerDialog month is 0-indexed
    val day = selectedDate.dayOfMonth

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
            selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth)
        }, year, month, day
    )

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
        title = { Text("개인일정 추가") },
        text = {
            Column {
                Box {
                    TextField(
                        value = selectedDate.toString(),
                        onValueChange = { },
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
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(selectedDate, title)
                    }
                }
            ) {
                Text("저장")
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
fun AdmobBanner(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                val adWidth = configuration.screenWidthDp

                // 2. 현재 방향에 맞는 적응형 배너 크기를 가져옵니다.
                // 이 함수는 주어진 너비에 대해 최적의 높이를 계산합니다.
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)

                setAdSize(adSize) // 변경된 부분
//                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ad unit ID
                adUnitId = "ca-app-pub-9901915016619662/9566315087" // Test ad unit ID
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // AdView's properties should only be set once, typically in the factory.
            // If you need to update adUnitId or adSize based on state, you would do it here.
            // However, for a banner ad, it's usually static.
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreenWithBottomBar(dbHelper: DatabaseHelper, fetchHolidaysForYear: (Int) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("1+1 달", "오늘", "개인일정")
    val pagerState = rememberPagerState(initialPage = 1) { tabTitles.size }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedSchedules by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHolidays by remember { mutableStateOf(true) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var scheduleUpdateTrigger by remember { mutableIntStateOf(0) }
    var selectedDateForPersonalSchedule by remember { mutableStateOf<LocalDate?>(null) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var qrCodeScheduleTitle by remember { mutableStateOf<String?>(null) }
    var qrCodeScheduleDate by remember { mutableStateOf<LocalDate?>(null) }

    val qrCodeScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { contents ->
            val parts = contents.split('|', limit = 2)
            if (parts.size == 2) {
                try {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    qrCodeScheduleDate = LocalDate.parse(parts[0], formatter)
                    qrCodeScheduleTitle = parts[1]
                } catch (e: Exception) {
                    Toast.makeText(context, "날짜 형식 오류. YYYY-MM-DD 형식을 사용하세요.", Toast.LENGTH_LONG).show()
                    qrCodeScheduleTitle = contents
                    qrCodeScheduleDate = null
                }
            } else {
                qrCodeScheduleTitle = contents
                qrCodeScheduleDate = null
            }
        } ?: run {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
        }
    }

    if (qrCodeScheduleTitle != null) {
        AddPersonalScheduleDialog(
            onDismiss = {
                qrCodeScheduleTitle = null
                qrCodeScheduleDate = null
            },
            onConfirm = { date, title ->
                coroutineScope.launch(Dispatchers.IO) {
                    dbHelper.addDay(
                        source = "qr_code",
                        category = "personal",
                        type = "date",
                        dataKey = date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), // YYYYMMDD format
                        title = title
                    )
                    scheduleUpdateTrigger++
                }
                qrCodeScheduleTitle = null
                qrCodeScheduleDate = null
            },
            initialTitle = qrCodeScheduleTitle ?: "",
            initialDate = qrCodeScheduleDate
        )
    }


    if (showScheduleDialog) {
        val allSchedules by produceState(initialValue = emptyList(), dbHelper, currentYearMonth, scheduleUpdateTrigger) {
            value = withContext(Dispatchers.IO) {
                dbHelper.getDistinctScheduleTitlesForMonth("personal", currentYearMonth) + dbHelper.getDistinctScheduleTitlesForMonth("personal", currentYearMonth.plusMonths(1))
            }
        }

        PersonalScheduleSelectionDialog(
            allSchedules = allSchedules,
            selectedSchedules = selectedSchedules,
            showHolidays = showHolidays,
            onDismiss = {
                showScheduleDialog = false
            },
            onConfirm = { newSelection, newShowHolidays ->
                selectedSchedules = newSelection
                showHolidays = newShowHolidays
                showScheduleDialog = false
            }
        )
    }

    if (showAddScheduleDialog) {
        AddPersonalScheduleDialog(
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { date, title ->
                coroutineScope.launch(Dispatchers.IO) {
                    dbHelper.addDay(
                        source = "user_input",
                        category = "personal",
                        type = "date",
                        dataKey = date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), // YYYYMMDD format
                        title = title
                    )
                    scheduleUpdateTrigger++
                }
                showAddScheduleDialog = false
            }
        )
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("설정") },
                                onClick = {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("백업하기") },
                                onClick = {
                                    context.startActivity(Intent(context, BackupActivity::class.java))
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                },
                actions = {
                    if (pagerState.currentPage == 0) {
                        IconButton(onClick = {
                            showScheduleDialog = true
                        }) {
                            Icon(painter = painterResource(id = R.drawable.double_check), contentDescription = "Double Check")
                        }
                    } else {
                        IconButton(onClick = {
                            val options = ScanOptions()
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setPrompt("다른 장치의 일정 QR Code를 스캔하세요.")
                            options.setCameraId(0) // Use a specific camera of the device
                            options.setBeepEnabled(false)
                            options.setBarcodeImageEnabled(false)
//                            options.setOrientationLocked(true)
                            qrCodeScannerLauncher.launch(options)
                        }) {
                            Icon(painter = painterResource(id = R.drawable.qr_code_read), contentDescription = "QR Code Read")
                        }
                    }
                    IconButton(onClick = {
                        showAddScheduleDialog = true
                    }) {
                        Icon(painter = painterResource(id = R.drawable.add), contentDescription = "개인일정 추가")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (pagerState.currentPage != 0) { // TwoMonthFragment가 아닐 때만 AdmobBanner 표시
                    AdmobBanner()
                }

                TabRow(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(color = MaterialTheme.colorScheme.primary)
                                    .align(Alignment.TopCenter)
                            )
                        }
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(text = title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top
        ) {
            when (it) {
                0 -> TwoMonthFragment(
                    modifier = Modifier.fillMaxHeight(),
                    fetchHolidaysForYear = fetchHolidaysForYear,
                    visibleCalList = true,
                    selectedPersonalSchedules = selectedSchedules,
                    showHolidays = showHolidays,
                    onMonthChanged = { currentYearMonth = it },
                    onNavigateToPersonalSchedule = { date ->
                        selectedDateForPersonalSchedule = date
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    scheduleUpdateTrigger = scheduleUpdateTrigger // Pass the trigger
                )
                1 -> TodayFragment()
                2 -> PersonalScheduleFragment(
                    modifier = Modifier.fillMaxHeight(),
                    selectedDate = selectedDateForPersonalSchedule,
                    scheduleUpdateTrigger = scheduleUpdateTrigger // Pass the trigger
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TwocalendarTheme {
        MainScreenWithBottomBar(dbHelper = DatabaseHelper(LocalContext.current), fetchHolidaysForYear = {})
    }
}
