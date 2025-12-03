package com.ediapp.twocalendar.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ediapp.twocalendar.Anniversary

@Composable
fun BirthdaySelectionDialog(
    anniversaries: List<Anniversary>,
    onDismiss: () -> Unit,
    onConfirm: (List<Anniversary>) -> Unit
) {
    var selectedAnniversaries by remember { mutableStateOf(emptyList<Anniversary>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR 생성대상 선택") },
        text = {
            LazyColumn {
                items(anniversaries) { anniversary ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedAnniversaries.contains(anniversary),
                            onCheckedChange = { isChecked ->
                                selectedAnniversaries = if (isChecked) {
                                    selectedAnniversaries + anniversary
                                } else {
                                    selectedAnniversaries - anniversary
                                }
                            }
                        )
                        Text(text = anniversary.schedule.title, modifier = Modifier.weight(1f))
                        Text(text = anniversary.date.toString())
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
                Text("QR생성")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
