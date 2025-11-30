package com.ediapp.twocalendar
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ediapp.twocalendar.ui.main.BirthDayFragment
import com.ediapp.twocalendar.ui.main.PersonalScheduleFragment
import com.ediapp.twocalendar.ui.main.TodayFragment
import com.ediapp.twocalendar.ui.main.TwoMonthFragment
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp // Added import for FirebaseApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
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

fun isEmulator(): Boolean {
    Log.d("isEmulator", "Build.MODEL: ${Build.MODEL}")
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("sdk_gphone64")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.BRAND.startsWith("generic")
            && Build.DEVICE.startsWith("generic")
            || "google_sdk" == Build.PRODUCT)
}
class MainActivity : ComponentActivity() {
    private val dbHelper by lazy { DatabaseHelper(this) }
    private lateinit var holidayNotificationScheduler: HolidayNotificationScheduler
    companion object {
        private const val TAG = "MainActivity"
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "POST_NOTIFICATIONS permission granted")
            holidayNotificationScheduler.scheduleDailyNotification()
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS permission denied")
            Toast.makeText(this, "알림 권한이 거부되어 공휴일 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val viewGuideCount = sharedPreferences.getInt("view_guide_count", 0)
        if (viewGuideCount < 5 || viewGuideCount % 20 == 1) {
            startActivity(Intent(this, GuideActivity::class.java))
        }

        FirebaseApp.initializeApp(this) // Initialize FirebaseApp here
        MobileAds.initialize(this)
        val androidId = getAndroidId(this) // Renamed variable
        Log.d(TAG, "Android ID: $androidId")
        holidayNotificationScheduler = HolidayNotificationScheduler(this)
        // Request POST_NOTIFICATIONS permission if on Android 13 (API 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted, schedule notification
                holidayNotificationScheduler.scheduleDailyNotification()
            } else {
                // Request permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // For devices below Android 13, permission is not required at runtime
            holidayNotificationScheduler.scheduleDailyNotification()
        }
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
        val apiKeys = listOf("HOLIDAY")
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
                                ?: throw IllegalArgumentException("API config for ${'$'}apiKey not found")
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
                                    Log.e("MainActivity", "Error fetching holidays for ${'$'}apiKey: ${'$'}{response.errorBody()?.string()}")
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Exception fetching holidays for ${'$'}apiKey", e)
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
fun QrCodeImage(data: String, size: Int = 512) {
    val writer = QRCodeWriter()
    val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
    val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code"
    )
}

@Composable
fun BirthdayQrCodeDialog(
    json: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("생일정보 공유") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (json != null) {
                    QrCodeImage(data = json, size = 800) // Use a smaller size for the dialog
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("다른 핸드폰에서 QR 코드를 스캔하여 생일정보를 가져올 수 있습니다.")
                } else {
                    Text("생일 정보가 없습니다.")
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
                adUnitId = Constants.AD_UNIT_ID_BANNER // Test ad unit ID
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
    val tabTitles = listOf("오늘", "1+1 달", "개인일정", stringResource(id = R.string.birthday))
    val pagerState = rememberPagerState(initialPage = 0) { tabTitles.size }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedSchedules by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHolidays by remember { mutableStateOf(true) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var scheduleUpdateTrigger by remember { mutableIntStateOf(0) }
    var selectedDateForPersonalSchedule by remember { mutableStateOf<LocalDate?>(null) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var qrCodeScheduleTitle by remember { mutableStateOf<String?>(null) }
    var qrCodeScheduleDate by remember { mutableStateOf<LocalDate?>(null) }
    var backupCalendarEnabledByRemoteConfig by remember { mutableStateOf(false) } // Placeholder for Remote Config value
    var showBirthdayQrCodeDialog by remember { mutableStateOf(false) }
    var birthdayQrJson by remember { mutableStateOf<String?>(null) }

    // TODO: Implement Firebase Remote Config to update backupCalendarEnabledByRemoteConfig.
    // Example:
    /*
    LaunchedEffect(Unit) {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("RemoteConfig", "Config params updated: $updated")
                    backupCalendarEnabledByRemoteConfig = remoteConfig.getBoolean("backup_calendar_enabled")
                } else {
                    Log.e("RemoteConfig", "Config fetch failed")
                }
            }
    }
    */
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

    val birthdayQrCodeScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { contents ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val records = contents.split(Constants.my_sep)
                    var importedCount = 0
                    for (recordString in records) {
                        if (recordString.isBlank()) continue
                        val fields = recordString.split('|')
                        if (fields.size >= 6) { // name|shortName|anniversaryType|calendarType|isYearAccurate|apply_dt
                            val name = fields[2]
                            val shortName = fields[3]
                            val anniversaryType = fields[0]
                            val calendarType = fields[4]
                            val isYearAccurate = fields[5].toBoolean()
                            val applyDt = fields[1]

                            dbHelper.addAnniversary(
                                source = "qr-code",
                                name = name,
                                shortName = shortName,
                                category = anniversaryType,
                                calendarType = calendarType,
                                isYearAccurate = isYearAccurate,
                                applyDt = LocalDate.parse(applyDt)
                            )
                            importedCount++
                        } else {
                            Log.e("MainActivity", "Invalid birthday record format: ${'$'}recordString")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${importedCount}개의 생일 정보를 가져왔습니다.", Toast.LENGTH_LONG).show()
                        scheduleUpdateTrigger++
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing birthday QR code: ${'$'}{e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "생일 정보 QR코드를 처리하는 중 오류가 발생했습니다: ${'$'}{e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
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

    if (showBirthdayQrCodeDialog) {
        BirthdayQrCodeDialog(json = birthdayQrJson, onDismiss = {
            showBirthdayQrCodeDialog = false
            birthdayQrJson = null
        })
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
                                enabled = isEmulator() || backupCalendarEnabledByRemoteConfig,
                                onClick = {
                                    context.startActivity(Intent(context, BackupActivity::class.java))
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("가이드 보기") },
                                onClick = {
                                    context.startActivity(Intent(context, GuideActivity::class.java))
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                },
                actions = {
                    when (pagerState.currentPage) {
                        1 -> { // "1+1 달"
                            IconButton(onClick = {
                                showScheduleDialog = true
                            }) {
                                Icon(painter = painterResource(id = R.drawable.double_check), contentDescription = "Double Check")
                            }
                        }
                        2 -> { // "개인일정"
                            IconButton(onClick = {
                                showAddScheduleDialog = true
                            }) {
                                Icon(painter = painterResource(id = R.drawable.add),
                                    contentDescription = "개인일정 추가")
                            }
                            IconButton(onClick = {
                                val options = ScanOptions()
                                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                options.setPrompt("다른 핸드폰의 일정 QR Code를 스캔하세요.")
                                options.setCameraId(0) // Use a specific camera of the device
                                options.setBeepEnabled(false)
                                options.setBarcodeImageEnabled(false)
                                qrCodeScannerLauncher.launch(options)
                            }) {
                                Icon(painter = painterResource(id = R.drawable.import_qrcode),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = "QR Code Read")
                            }
                        }
                        3 -> { // "기념일" (BirthDayFragment)
                            IconButton(onClick = {
                                context.startActivity(Intent(context, AnniversaryActivity::class.java))
                            }) {
                                Icon(painter = painterResource(id = R.drawable.data_add),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(R.string.anniversary))
                            }

                            IconButton(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val result = dbHelper.getAllBirthdaysForQrCode()
//                                    Log.d("JSON", result.joinToString(Constants.my_sep))
//                                    val json = Gson().toJson(result)
                                    withContext(Dispatchers.Main) {
                                        birthdayQrJson = result.joinToString(Constants.my_sep)
                                        showBirthdayQrCodeDialog = true
                                    }
                                }
                            }) {
                                Icon(painter = painterResource(id = R.drawable.qr_code),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = "생일 QR Code")
                            }

                            IconButton(onClick = {
                                val options = ScanOptions()
                                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                options.setPrompt("다른폰의 생일 QR Code를 스캔하세요.")
                                options.setCameraId(0) // Use a specific camera of the device
                                options.setBeepEnabled(false)
                                options.setBarcodeImageEnabled(false)
                                birthdayQrCodeScannerLauncher.launch(options)
                            }) {
                                Icon(painter = painterResource(id = R.drawable.import_qrcode),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = "생일 QR Code 읽어오기")
                            }

                        }
                        else -> { // "오늘", "개인일정"

                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (pagerState.currentPage != 1) { // TwoMonthFragment (now at index 1) is where the banner should be hidden
                    AdmobBanner(modifier = Modifier.padding(20.dp))
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
                0 -> TodayFragment()
                1 -> TwoMonthFragment(
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
                2 -> PersonalScheduleFragment(
                    modifier = Modifier.fillMaxHeight(),
                    selectedDate = selectedDateForPersonalSchedule,
                    scheduleUpdateTrigger = scheduleUpdateTrigger // Pass the trigger
                )
                3 -> BirthDayFragment(
                    modifier = Modifier.fillMaxHeight(),
                    selectedDate = selectedDateForPersonalSchedule,
                    scheduleUpdateTrigger = scheduleUpdateTrigger
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