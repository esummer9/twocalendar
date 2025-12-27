package com.ediapp.twocalendar.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ediapp.twocalendar.Anniversary
import com.ediapp.twocalendar.R
import com.ediapp.twocalendar.ui.common.QrCodeImage
import java.time.LocalDate


@Composable
fun CalendarShareDialog(
    anniversaries: List<Anniversary>,
    onDismiss: () -> Unit,
    onConfirm: (List<Anniversary>, Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val yearRange = (currentYear - 3)..(currentYear + 3)
    var selectedAnniversaries by remember { mutableStateOf(emptyList<Anniversary>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("캘린더등록") },
        text = {
            Column {
                Text("등록할 년도를 선택하세요:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (selectedYear > yearRange.first) selectedYear-- }) {
                        Icon(painterResource(id = R.drawable.left_arrow), modifier = Modifier.size(22.dp), contentDescription = "이전 년도")
                    }
                    Text(text = selectedYear.toString(), style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { if (selectedYear < yearRange.last) selectedYear++ }) {
                        Icon(painterResource(id = R.drawable.right_arrow), modifier = Modifier.size(22.dp), contentDescription = "다음 년도")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("공유할 항목을 선택하세요:")
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(anniversaries) { anniversary ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAnniversaries = if (selectedAnniversaries.contains(anniversary)) {
                                        selectedAnniversaries - anniversary
                                    } else {
                                        selectedAnniversaries + anniversary
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedAnniversaries.contains(anniversary),
                                onCheckedChange = {
                                    selectedAnniversaries = if (it) {
                                        selectedAnniversaries + anniversary
                                    } else {
                                        selectedAnniversaries - anniversary
                                    }
                                }
                            )
                            Text(text = anniversary.schedule.title)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAnniversaries, selectedYear) },
                enabled = selectedAnniversaries.isNotEmpty()
            ) {
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


@Composable
fun AnniversarySelectionDialog(
    anniversaries: List<Anniversary>,
    onDismiss: () -> Unit,
    onConfirm: (List<Anniversary>) -> Unit
) {
    var selectedAnniversaries by remember { mutableStateOf(emptyList<Anniversary>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("공유대상 선택") },
        text = {
            LazyColumn {
                items(anniversaries) { anniversary ->
                    val isSelected = selectedAnniversaries.contains(anniversary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAnniversaries = if (isSelected) {
                                    selectedAnniversaries - anniversary
                                } else {
                                    selectedAnniversaries + anniversary
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { isChecked ->
                                selectedAnniversaries = if (isChecked) {
                                    selectedAnniversaries + anniversary
                                } else {
                                    selectedAnniversaries - anniversary
                                }
                            }
                        )
                        Text(text = anniversary.schedule.title, modifier = Modifier.weight(1f))
                        Text(text = anniversary.originDt.toString())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedAnniversaries)
                    onDismiss()
                }
            ) {
                Text("QR로 공유")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
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
                if (!json.isNullOrEmpty()) {
                    QrCodeImage(data = json, size = 800) // Use a smaller size for the dialog
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("다른 핸드폰에서 QR 코드를 스캔하여 기념일 정보를 가져올 수 있습니다.")
                } else {
                    Text("기념일 정보가 없습니다.")
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

