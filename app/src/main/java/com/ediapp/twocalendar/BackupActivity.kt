package com.ediapp.twocalendar

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ediapp.twocalendar.ui.theme.TwocalendarTheme // Adjust the import path as needed
import kotlin.random.Random

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

@OptIn(ExperimentalMaterial3Api::class)
class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPreferences = getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        var randomCode = sharedPreferences.getString("random_code", null)

        if (randomCode == null) {
            randomCode = generateRandomCode()
            sharedPreferences.edit().putString("random_code", randomCode).apply()
        }

        setContent {
            TwocalendarTheme {
                BackupScreenContent(randomCode = randomCode!!) { finish() }
            }
        }
    }

    private fun generateRandomCode(): String {
        val part1 = Random.nextInt(1000, 10000)
        val part2 = Random.nextInt(1000, 10000)
        val part3 = Random.nextInt(1000, 10000)
        return "$part1 - $part2 - $part3"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreenContent(randomCode: String, onBackPressed: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("백업정보", "복원정보")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("백업 및 복원") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
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
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedTabIndex) {
                0 -> BackupSection(randomCode = randomCode)
                1 -> RestoreSection()
            }
        }
    }
}


@Composable
fun BackupSection(modifier: Modifier = Modifier, randomCode: String = "1234 - 5678 - 9012") {
    var backupCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)) // Apply rounded corners
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)), // Apply border
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            Text("⬆️ 백업정보",
                modifier = Modifier.padding(1.dp), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("랜덤코드", modifier = Modifier.weight(1f))
                Text(randomCode, modifier = Modifier.weight(2f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("백업코드", modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = backupCodeInput,
                    onValueChange = { backupCodeInput = it },
                    label = { Text("백업코드") },
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
            Text("⬇️ 복원정보",
                modifier = Modifier.padding(1.dp), fontSize = 16.sp)
            Text("랜덤&백업코드 2개를 모두 입력하셔야 복원됩니다.",
                Modifier.padding(4.dp), color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var randomCodeInput by remember { mutableStateOf("") } // Declare randomCodeInput
                OutlinedTextField(
                    value = randomCodeInput,
                    onValueChange = { randomCodeInput = it },
                    label = { Text("랜덤코드") },
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
                    label = { Text("백업코드") },
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

@Preview(showBackground = true, widthDp = 320)
@Composable
fun PreviewBackupScreen() {
    TwocalendarTheme {
        BackupScreenContent(randomCode = "1234 - 5678 - 9012") {
            // Do nothing on back press for preview
        }
    }
}