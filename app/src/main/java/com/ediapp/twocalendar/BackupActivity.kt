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
import androidx.compose.runtime.rememberCoroutineScope
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
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Placeholder DatabaseHelper for demonstration. 
// Replace with your actual DatabaseHelper if it exists and has similar functionality.

@OptIn(ExperimentalMaterial3Api::class)
class BackupActivity : ComponentActivity() {
    private val dbHelper by lazy { DatabaseHelper(this) }
//    private val storage = Firebase.storage/**/

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
                BackupScreenContent(randomCode = randomCode!!, dbHelper = dbHelper) { finish() }
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
fun BackupScreenContent(randomCode: String, dbHelper: DatabaseHelper, onBackPressed: () -> Unit) {
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
                0 -> BackupSection(randomCode = randomCode, dbHelper = dbHelper)
                1 -> RestoreSection(dbHelper = dbHelper)
            }
        }
    }
}


@Composable
fun BackupSection(modifier: Modifier = Modifier, randomCode: String, dbHelper: DatabaseHelper) {
    var backupCodeInput by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                Button(
                    onClick = {
                        if (backupCodeInput.isBlank()) {
                            Toast.makeText(context, "백업코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val sharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val lastBackupDate = sharedPreferences.getString("last_backup_date", null)
                        var backupCount = sharedPreferences.getInt("backup_count", 0)

                        if (today != lastBackupDate) {
                            backupCount = 0
                        }

                        if (backupCount >= 3) {
                            Toast.makeText(context, "하루 백업 횟수(3회)를 초과했습니다.", Toast.LENGTH_SHORT).show()
//                            return@Button
                        }

                        backupCount++
                        editor.putString("last_backup_date", today)
                        editor.putInt("backup_count", backupCount)
                        editor.apply()

                        sharedPreferences.edit().putString("backup_code", backupCodeInput).apply()

                        isBackingUp = true
                        Toast.makeText(context, "백업을 시작합니다...", Toast.LENGTH_LONG).show()

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val days = dbHelper.getAllPersonalSchedules()
                                val gson = Gson()
                                val jsonString = gson.toJson(days)

                                val tempFile = File(context.cacheDir, "tb_days.json")
                                FileWriter(tempFile).use { it.write(jsonString) }

                                val storagePath = "twocalendar/${randomCode.replace(" ", "")}/${backupCodeInput}/tb_days.json"
                                val fileUri = Uri.fromFile(tempFile)

                                val uploadTask = Firebase.storage.reference.child(storagePath).putFile(fileUri)

                                uploadTask.addOnSuccessListener { taskSnapshot ->
                                    coroutineScope.launch {
                                        Toast.makeText(context, "백업 성공!", Toast.LENGTH_SHORT).show()
                                        isBackingUp = false
                                    }
                                    tempFile.delete()
                                    val db = Firebase.firestore
                                    val calendar = Calendar.getInstance()
                                    calendar.add(Calendar.DAY_OF_YEAR, 5)
                                    val expiryDate = calendar.time

                                    val backupLog = hashMapOf(
                                        "expiry_dt" to expiryDate,
                                        "random_code" to backupCodeInput.trim()
                                    )

                                    db.collection("twocalendar").document(randomCode.replace(" ", ""))
                                        .set(backupLog)
                                        .addOnSuccessListener {
                                            Log.d("BackupActivity", "Firestore log successfully written!")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("BackupActivity", "Error writing Firestore log", e)
                                        }

                                    Log.d("BackupActivity", "Backup successful: ${taskSnapshot.metadata?.path}")
                                }.addOnFailureListener { exception ->
                                    coroutineScope.launch {
                                        Toast.makeText(context, "백업 실패: ${exception.message}", Toast.LENGTH_LONG).show()
                                        isBackingUp = false
                                    }
                                    tempFile.delete()
                                    Log.e("BackupActivity", "Backup failed", exception)
                                }
                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    Toast.makeText(context, "백업 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
                                    isBackingUp = false
                                }
                                Log.e("BackupActivity", "Error during backup process", e)
                            }
                        }
                    },
                    enabled = !isBackingUp
                ) {
                    Text(if (isBackingUp) "백업 중..." else "백업하기")
                }
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreSection(modifier: Modifier = Modifier, dbHelper: DatabaseHelper) {
    var backupCodeInput by remember { mutableStateOf("") }
    var randomCodeInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                Button(onClick = {
                    if (randomCodeInput.isBlank() || backupCodeInput.isBlank()) {
                        Toast.makeText(context, "랜덤코드와 백업코드를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    Toast.makeText(context, "복원을 시작합니다...", Toast.LENGTH_LONG).show()

                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val storagePath = "twocalendar/${randomCodeInput.replace(" ", "")}/${backupCodeInput}/tb_days.json"
                            val oneMegabyte: Long = 1024 * 1024
                            val fileRef = Firebase.storage.reference.child(storagePath)

                            fileRef.getBytes(oneMegabyte).addOnSuccessListener { bytes ->
                                coroutineScope.launch(Dispatchers.Main) { // Use CoroutineScope for main thread Toast
                                    try {
                                        val jsonString = String(bytes)
                                        val gson = Gson()
                                        val typeToken = object : TypeToken<List<DayRecord>>() {}.type
                                        val daysToRestore: List<DayRecord> = gson.fromJson(jsonString, typeToken)

//                                        Log.d("RestoreSection", "daysToRestore: $daysToRestore")

                                        val successCount = dbHelper.restoreDays(daysToRestore)

                                        Toast.makeText(context, "총 ${daysToRestore.size}건 중 ${successCount}건 복원 성공!", Toast.LENGTH_SHORT).show()
//                                        Log.d("RestoreSection", "Restore successful")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "복원 데이터 처리 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
                                        Log.e("RestoreSection", "Error processing restored data", e)
                                    }
                                }
                            }.addOnFailureListener { exception ->
                                coroutineScope.launch(Dispatchers.Main) { // Use CoroutineScope for main thread Toast
                                    Toast.makeText(context, "복원 실패: ${exception.message}", Toast.LENGTH_LONG).show()
                                    Log.e("RestoreSection", "Restore failed", exception)
                                }
                            }

                        } catch (e: Exception) {
                            coroutineScope.launch(Dispatchers.Main) { // Use CoroutineScope for main thread Toast
                                Toast.makeText(context, "복원 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                            Log.e("RestoreSection", "Error during restore process", e)
                        }
                    }
                }) {
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
        // Provide a mock DatabaseHelper for the preview
        val mockDbHelper = DatabaseHelper(LocalContext.current) // This might require a mock context if not in an actual app context
        BackupScreenContent(randomCode = "1234 - 5678 - 9012", dbHelper = mockDbHelper) {
            // Do nothing on back press for preview
        }
    }
}
