package com.ediapp.twocalendar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class QRCodeActivity : ComponentActivity() {
    private val dbHelper by lazy { DatabaseHelper(this) }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val tabTitles = listOf("개인일정", "기념일")
            val pagerState = rememberPagerState { tabTitles.size }
            val coroutineScope = rememberCoroutineScope()

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
                        } catch (_: Exception) {
                            Toast.makeText(this, "날짜 형식 오류. YYYY-MM-DD 형식을 사용하세요.", Toast.LENGTH_LONG).show()
                            qrCodeScheduleTitle = contents
                            qrCodeScheduleDate = null
                        }
                    } else {
                        qrCodeScheduleTitle = contents
                        qrCodeScheduleDate = null
                    }
                } ?: run {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                }
            }

            val anniversaryQrCodeScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
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
                                        applyDt = LocalDate.parse(applyDt),
                                        originDt = LocalDate.parse(applyDt)
                                    )
                                    importedCount++
                                } else {
                                    Log.e("QRCodeActivity", "Invalid anniversary record format: $recordString")
                                }
                            }

                            runOnUiThread {
                                Toast.makeText(this@QRCodeActivity, "${importedCount}개의 기념일 정보를 가져왔습니다.", Toast.LENGTH_LONG).show()
                                finish() // Close activity after importing
                            }
                        } catch (e: Exception) {
                            Log.e("QRCodeActivity", "Error parsing anniversary QR code: ${e.message}", e)
                            runOnUiThread {
                                Toast.makeText(this@QRCodeActivity, "기념일 정보 QR코드를 처리하는 중 오류가 발생했습니다: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } ?: run {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
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
                            runOnUiThread {
                                Toast.makeText(this@QRCodeActivity, "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        qrCodeScheduleTitle = null
                        qrCodeScheduleDate = null
                        finish() // Close activity after adding schedule
                    },
                    initialTitle = qrCodeScheduleTitle ?: "",
                    initialDate = qrCodeScheduleDate
                )
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("QR Code 읽기") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    TabRow(selectedTabIndex = pagerState.currentPage) {
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
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.calendar_512),
                                        contentDescription = "Personal QR Code",
                                        modifier = Modifier.size(150.dp),
                                    )
                                    Button(onClick = {
                                        val options = ScanOptions()
                                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        options.setPrompt("다른 핸드폰의 일정 QR Code를 스캔하세요.")
                                        options.setCameraId(0) // Use a specific camera of the device
                                        options.setBeepEnabled(false)
                                        options.setBarcodeImageEnabled(false)
                                        qrCodeScannerLauncher.launch(options)
                                    }) {
                                        Text("개인일정 QR 읽기")
                                    }
                                }
                            }
                            1 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.confetti),
                                        contentDescription = "Anniversary QR Code",
                                        modifier = Modifier.size(150.dp)
                                    )
                                    Button(onClick = {
                                        val options = ScanOptions()
                                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        options.setPrompt("다른폰의 기념일 QR Code를 스캔하세요.")
                                        options.setCameraId(0) // Use a specific camera of the device
                                        options.setBeepEnabled(false)
                                        options.setBarcodeImageEnabled(false)
                                        anniversaryQrCodeScannerLauncher.launch(options)
                                    }) {
                                        Text("기념일 QR 읽기")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPersonalScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit,
    initialTitle: String = "",
    initialDate: LocalDate? = null
) {
    var selectedDate by remember { mutableStateOf(initialDate ?: LocalDate.now()) }
    var title by remember { mutableStateOf(initialTitle) }
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
                        value = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        onValueChange = { },
                        label = { Text("날짜") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
