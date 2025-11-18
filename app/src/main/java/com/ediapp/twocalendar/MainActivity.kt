package com.ediapp.twocalendar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ediapp.twocalendar.ui.main.TodayFragment
import com.ediapp.twocalendar.ui.main.TwoMonthFragment
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.time.YearMonth
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val dbHelper by lazy { DatabaseHelper(this) }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val andorid_id = getAndroidId(this)
        Log.d(TAG, "Android ID: $andorid_id")

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
                    MainScreenWithTopBar(dbHelper = dbHelper, fetchHolidaysForYear = { year ->
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreenWithTopBar(dbHelper: DatabaseHelper, fetchHolidaysForYear: (Int) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("1+1 달", "오늘")
    val pagerState = rememberPagerState { tabTitles.size }
    val view = LocalView.current

    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedSchedules by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHolidays by remember { mutableStateOf(true) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var scheduleUpdateTrigger by remember { mutableStateOf(0) }

    if (showScheduleDialog) {
        val allSchedules by produceState<List<String>>(initialValue = emptyList(), dbHelper, currentYearMonth, scheduleUpdateTrigger) {
            value = withContext(Dispatchers.IO) {
                dbHelper.getDistinctScheduleTitlesForMonth("personal", currentYearMonth)
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
                                text = { Text("개인일정") },
                                onClick = {
                                    context.startActivity(Intent(context, PersonalScheduleActivity::class.java))
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("설정") },
                                onClick = {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
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
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            captureAndShare(view, context)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "공유")
                    }
                }
            )
        },
        bottomBar = {
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
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> TwoMonthFragment(
                    modifier = Modifier.fillMaxHeight(),
                    fetchHolidaysForYear = fetchHolidaysForYear,
                    visibleCalList = true,
                    selectedPersonalSchedules = selectedSchedules,
                    showHolidays = showHolidays,
                    onMonthChanged = { currentYearMonth = it }
                )
                1 -> TodayFragment()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TwocalendarTheme {
        MainScreenWithTopBar(dbHelper = DatabaseHelper(LocalContext.current), fetchHolidaysForYear = {})
    }
}
