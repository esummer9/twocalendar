package com.ediapp.twocalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme // Adjust the import path as needed

@OptIn(ExperimentalMaterial3Api::class)
class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwocalendarTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("백업 및 복원") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp) // Add padding around the column
                    ) {
                        BackupSection()
                        Spacer(modifier = Modifier.height(16.dp))
                        RestoreSection()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BackupSection(modifier: Modifier = Modifier) {
    var backupCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)), // Apply border
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            Text("⬆\uFE0F 백업정보",
                modifier = Modifier.padding(1.dp), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("랜덤코드", modifier = Modifier.weight(1f))
                Text("1223 - 2244 - 3355", modifier = Modifier.weight(2f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("랜덤코드", modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = backupCodeInput,
                    onValueChange = { backupCodeInput = it },
                    label = { Text("랜덤코드") },
                    modifier = Modifier.weight(2f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("랜덤코드,백업코드 2개를 모두 적어 두셔야 합니다.",
                Modifier.padding(4.dp), color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { /* TODO: Implement backup logic */ }) {
                    Text("백업하기")
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun RestoreSection(modifier: Modifier = Modifier) {
    var backupCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)), // Apply border
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("⬇\uFE0F 복원정보",
                modifier = Modifier.padding(1.dp), fontSize = 16.sp)
            Text("랜덤&백업코드 2개를 모두 입력하셔야 복원됩니다.",
                Modifier.padding(4.dp), color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = backupCodeInput,
                    onValueChange = { backupCodeInput = it },
                    label = { Text("백업코드") },
                    modifier = Modifier.weight(2f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = backupCodeInput,
                    onValueChange = { backupCodeInput = it },
                    label = { Text("랜덤코드") },
                    modifier = Modifier.weight(2f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End

                ) {
                Button(onClick = { /* TODO: Implement backup logic */ }) {
                    Text("복원하기")
                }
            }
        }
    }
}