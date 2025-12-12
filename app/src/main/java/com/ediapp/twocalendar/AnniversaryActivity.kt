package com.ediapp.twocalendar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ediapp.twocalendar.network.LunarApi
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import com.ediapp.twocalendar.ui.main.EXTRA_ANNIVERSARY_ID // Import the constant

data class AnniversaryData(
    var id: Long = 0L,
    var name: String = "",
    var shortName: String = "",
    var anniversaryType: String = "",
    var calendarType: String = "",
    var isYearAccurate: Boolean = false,
    var selectedDate: LocalDate = LocalDate.now()
)

suspend fun LunToSolar ( year: Int, month: Int, day: Int): LocalDate? {
    return LunarApi.convertToSolar(year, month, day)
}

class AnniversaryActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwocalendarTheme {
                val context = LocalContext.current
                val dbHelper = remember { DatabaseHelper(context) }
                var anniversaryData by remember { mutableStateOf(AnniversaryData()) }
                val coroutineScope = rememberCoroutineScope()

                val anniversaryId = intent.getLongExtra(EXTRA_ANNIVERSARY_ID, 0L)
                Log.d("AnniversaryActivity", "Received anniversaryId: $anniversaryId")

                LaunchedEffect(anniversaryId) {
                    if (anniversaryId != 0L) {
                        val anniversary = dbHelper.getAnniversaryById(anniversaryId)
                        anniversary?.let { ann ->
                            Log.d("AnniversaryActivity", "Loaded anniversary: $ann")
                            anniversaryData = AnniversaryData(
                                id = ann.id.toLong(),
                                name = ann.schedule.title,
                                shortName = ann.shortName,
                                anniversaryType = ann.schedule.category,
                                calendarType = ann.schedule.calendarType?: "양력",
                                isYearAccurate = ann.isYearAccurate,
                                selectedDate = ann.originDt
                            )
                            Log.d("AnniversaryActivity", "AnniversaryData after update: $anniversaryData")
                        } ?: run {
                            Log.e("AnniversaryActivity", "Anniversary with ID $anniversaryId not found.")
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(id = R.string.anniversary)) },
                            navigationIcon = {
                                IconButton(onClick = { (context as? Activity)?.finish() }) {
                                    Icon(Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.back_button_description))
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    if (anniversaryData.name.isBlank() ||
                                        anniversaryData.anniversaryType.isBlank() ||
                                        anniversaryData.calendarType.isBlank()) {
                                        Toast.makeText(context, "이름, 기념일 종류, 달력 종류는 필수 항목입니다.", Toast.LENGTH_SHORT).show()
                                    } else {

                                        coroutineScope.launch {
                                            val year = LocalDate.now().year

                                            val applyDt: LocalDate? = if (anniversaryData.calendarType == "음력") {
                                                LunToSolar(year,
                                                    anniversaryData.selectedDate.monthValue,
                                                    anniversaryData.selectedDate.dayOfMonth
                                                )
                                            } else {
                                                LocalDate.of(
                                                    year,
                                                    anniversaryData.selectedDate.monthValue,
                                                    anniversaryData.selectedDate.dayOfMonth
                                                )
                                            }

                                            Log.d("AnniversaryActivity", "applyDt: $applyDt")

                                            if (anniversaryData.id == 0L) {
                                                dbHelper.addAnniversary(
                                                    name = anniversaryData.name,
                                                    shortName = anniversaryData.shortName,
                                                    category = anniversaryData.anniversaryType,
                                                    calendarType = anniversaryData.calendarType,
                                                    isYearAccurate = anniversaryData.isYearAccurate,
                                                    originDt = anniversaryData.selectedDate,
                                                    applyDt = applyDt ?: anniversaryData.selectedDate
                                                )
                                            } else {
                                                dbHelper.updateAnniversary(
                                                    id = anniversaryData.id,
                                                    name = anniversaryData.name,
                                                    shortName = anniversaryData.shortName,
                                                    category = anniversaryData.anniversaryType,
                                                    calendarType = anniversaryData.calendarType,
                                                    isYearAccurate = anniversaryData.isYearAccurate,
                                                    originDt = anniversaryData.selectedDate,
                                                    applyDt = applyDt ?: anniversaryData.selectedDate
                                                )
                                            }

                                            withContext(Dispatchers.Main) {
                                                (context as? Activity)?.finish()
                                            }
                                        }
                                    }
                                }) {
                                    Icon(painter = painterResource(id = R.drawable.save),
                                        contentDescription = stringResource(id = R.string.save_button_description),
                                        Modifier.size(30.dp))
                                }
                            }
                        )
                    },
                    content = {paddingValues ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AnniversaryInputCard(anniversaryData) { anniversaryData = it }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryInputCard(
    anniversaryData: AnniversaryData,
    onAnniversaryDataChange: (AnniversaryData) -> Unit
) {
    val anniversaryTypes = listOf(
        stringResource(id = R.string.anniversary_type_birthday),
        stringResource(id = R.string.anniversary_type_anniversary),
    )
    var isAnniversaryTypeExpanded by remember { mutableStateOf(false) }

    val calendarTypes = listOf(
        stringResource(id = R.string.calendar_type_solar),
        stringResource(id = R.string.calendar_type_lunar),
    )

    var isCalendarTypeExpanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = anniversaryData.selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onAnniversaryDataChange(anniversaryData.copy(selectedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(id = R.string.anniversary_info_title))
            Spacer(modifier = Modifier.height(16.dp)) // Add spacer after title

            // 이름, 약칭
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = anniversaryData.name,
                    onValueChange = { onAnniversaryDataChange(anniversaryData.copy(name = it)) },
                    label = { Text(stringResource(id = R.string.name_label)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) 

            // 기념일 종류, 양력/음력
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = isAnniversaryTypeExpanded,
                    onExpandedChange = { isAnniversaryTypeExpanded = !isAnniversaryTypeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = anniversaryData.anniversaryType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAnniversaryTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = isAnniversaryTypeExpanded,
                        onDismissRequest = { isAnniversaryTypeExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        anniversaryTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onAnniversaryDataChange(anniversaryData.copy(anniversaryType = type))
                                    isAnniversaryTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                ExposedDropdownMenuBox(
                    expanded = isCalendarTypeExpanded,
                    onExpandedChange = { isCalendarTypeExpanded = !isCalendarTypeExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = anniversaryData.calendarType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.calendar_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCalendarTypeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = isCalendarTypeExpanded,
                        onDismissRequest = { isCalendarTypeExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        calendarTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onAnniversaryDataChange(anniversaryData.copy(calendarType = type))
                                    isCalendarTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 날짜 선택 TextField
            Box {
                OutlinedTextField(
                    value = anniversaryData.selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    onValueChange = {},
                    label = { Text(stringResource(id = R.string.birthday)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = { showDatePicker = true })
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.anniversary_not_accurate_label))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnniversaryActivityPreview() {
    TwocalendarTheme {
        AnniversaryInputCard(AnniversaryData()) {}
    }
}