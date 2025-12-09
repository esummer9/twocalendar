package com.ediapp.twocalendar

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.ediapp.twocalendar.ui.main.Schedule
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class Saying(val saying: String, val author: String)
data class Anniversary(val id: Int, val originDt: LocalDate, val applyDt: LocalDate, val schedule: Schedule)

data class DayRecord(
    val id: Int,
    val source: String,
    val category: String,
    val type: String?,
    val dataKey: String?,
    val applyDt: String?,
    val title: String?,
    val alias: String?,
    val value: Int?,
    val message: String?,
    val description: String?,
    val registeredAt: String?,
    val createdAt: String?,
    val deletedAt: String?,
    val status: String?
)

data class BirthdayRecord(
    val category: String,
    val apply_dt: String?,
    val origin_dt: String?,
    val title: String?,
    val alias: String?,
    val sol_lun: String?,
    val verify: Boolean,
    val sttus: String?
){
    // toString() 메서드를 오버라이드하여 필드를 "|"로 구분하는 문자열을 반환
    override fun toString(): String {
        return listOf(
            category,
            apply_dt ?: "", // null일 경우 빈 문자열로 대체
            title ?: "",
            alias ?: "",
            sol_lun ?: "",
            verify.toString(),
            sttus ?: ""
        ).joinToString("|") // 컬럼 구분자로 "|" 사용
    }
}


class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
    DatabaseErrorHandler { dbObj ->
        Log.e(TAG, "Database error occurred: ${dbObj.path}")
    }
) {
    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "ediapp.db"
        private const val DATABASE_VERSION = 3

        // Table name
        const val TABLE_NAME = "tb_days"
        const val TABLE_NAME_SAYING = "tb_saying"
        const val TABLE_NAME_ANNIVERSARY = "tb_birthday"

        // Column names
        const val COL_ID = "_id"
        const val COL_SOURCE = "source"
        const val COL_CATEGORY = "category"
        const val COL_TYPE = "data_type"
        const val COL_DATA_KEY = "data_key"
        const val COL_APPLY_DT = "apply_dt"
        const val COL_ORIGIN_DT = "origin_dt"
        const val COL_TITLE = "title"
        const val COL_ALIAS = "alias"
        const val COL_VALUE = "value"
        const val COL_MESSAGE = "message"
        const val COL_DESCRIPTION = "description"
        const val COL_REGISTERED_AT = "registered_at"
        const val COL_CREATED_AT = "created_at"
        const val COL_DELETED_AT = "deleted_at"
        const val COL_STATUS = "status"

        // tb_saying columns
        const val COL_SAYING_SAYING = "saying"
        const val COL_SAYING_AUTHOR = "author"
        const val COL_SOL_LUN = "sol_lun"
        const val COL_VERIFY = "is_verify"

        /**
         * 기념일, 생일, 기타
         */
        private val SQL_CREATE_TABLE_ANNIVERSARY = """
            CREATE TABLE $TABLE_NAME_ANNIVERSARY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SOURCE VARCHAR(50) NOT NULL,
                $COL_CATEGORY VARCHAR(50) NOT NULL,
                $COL_TYPE VARCHAR(50) DEFAULT NULL,
                $COL_DATA_KEY VARCHAR(50) DEFAULT NULL,
                $COL_APPLY_DT VARCHAR(50) DEFAULT NULL,
                $COL_ORIGIN_DT VARCHAR(50) DEFAULT NULL,
                $COL_TITLE VARCHAR(100),
                $COL_ALIAS VARCHAR(100),
                $COL_VALUE INTEGER DEFAULT 0,
                $COL_MESSAGE VARCHAR(250),
                $COL_DESCRIPTION TEXT,
                $COL_REGISTERED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                
                $COL_SOL_LUN VARCHAR(10),
                $COL_VERIFY boolean DEFAULT false,
                
                $COL_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                $COL_DELETED_AT DATETIME DEFAULT NULL,
                $COL_STATUS VARCHAR(50) DEFAULT 'active',
                UNIQUE($COL_SOURCE, $COL_CATEGORY, $COL_DATA_KEY) ON CONFLICT REPLACE
            )
        """.trimIndent()

        // SQL for creating the database table
        /**
         * 공휴일 : Holiday
         * 개인일정 : personal
         */
        private val SQL_CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SOURCE VARCHAR(50) NOT NULL,
                $COL_CATEGORY VARCHAR(50) NOT NULL,
                $COL_TYPE VARCHAR(50) DEFAULT NULL,
                $COL_DATA_KEY VARCHAR(50) DEFAULT NULL,
                $COL_APPLY_DT VARCHAR(50) DEFAULT NULL,
                $COL_TITLE VARCHAR(100),
                $COL_ALIAS VARCHAR(100),
                $COL_VALUE INTEGER DEFAULT 0,
                $COL_MESSAGE VARCHAR(250),
                $COL_DESCRIPTION TEXT,
                $COL_REGISTERED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                $COL_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
                $COL_DELETED_AT DATETIME DEFAULT NULL,
                $COL_STATUS VARCHAR(50) DEFAULT 'active',
                UNIQUE($COL_SOURCE, $COL_CATEGORY, $COL_DATA_KEY) ON CONFLICT REPLACE
            )
        """.trimIndent()

        /* 명언목록 */
        private val SQL_CREATE_TABLE_SAYING = """
            CREATE TABLE $TABLE_NAME_SAYING (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SAYING_SAYING TEXT NOT NULL,
                $COL_SAYING_AUTHOR VARCHAR(100) NOT NULL
            )
        """.trimIndent()
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.transaction {
                execSQL(SQL_CREATE_TABLE)
                execSQL(SQL_CREATE_TABLE_SAYING)
                execSQL(SQL_CREATE_TABLE_ANNIVERSARY)

                insertInitialData(this)
                insertSayingData(this)
            }
            Log.d(TAG, "Database created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database", e)
            throw RuntimeException("Failed to create database", e)
        }
    }

    private fun insertInitialData(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COL_SOURCE, "app")
            put(COL_CATEGORY, "system")
            put(COL_TYPE, "install")
            put(COL_DATA_KEY, "app_install")
            put(COL_TITLE, "Application Installed")
            put(COL_ALIAS, "install")
            put(COL_VALUE, 1)
            put(COL_STATUS, "active")
            put(COL_MESSAGE, "Application has been installed")
            put(COL_DESCRIPTION, "Initial installation record")
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    public fun insertSayingData(db: SQLiteDatabase) {
        try {
            context.assets.open("tb_saying.txt").bufferedReader().useLines { lines ->
                db.transaction {
                    lines.forEach { line ->
                        val parts = line.split('\t')
                        if (parts.size == 2) {
                            val values = ContentValues().apply {
                                put(COL_SAYING_SAYING, parts[0])
                                put(COL_SAYING_AUTHOR, parts[1])
                            }
                            db.insert(TABLE_NAME_SAYING, null, values)
                        }
                    }
                }
            }
            Log.d(TAG, "Successfully inserted data into tb_saying")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting data into tb_saying", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Upgrading database from version $oldVersion to $newVersion")

        try {
            if (oldVersion < 2) {
                db.transaction {
                    execSQL(SQL_CREATE_TABLE_SAYING)
                    insertSayingData(this)
                }
            }
            if (oldVersion < 3) {
                db.transaction {
                    execSQL(SQL_CREATE_TABLE_ANNIVERSARY)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database", e)
            throw RuntimeException("Failed to upgrade database", e)
        }
    }

    fun getSayingByNo(no: Int): Saying? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME_SAYING,
            arrayOf(COL_SAYING_SAYING, COL_SAYING_AUTHOR),
            "$COL_ID = ?",
            arrayOf(no.toString()),
            null, null, null
        )
        var saying: Saying? = null
        if (cursor.moveToFirst()) {
            val sayingText = cursor.getString(cursor.getColumnIndexOrThrow(COL_SAYING_SAYING))
            val authorText = cursor.getString(cursor.getColumnIndexOrThrow(COL_SAYING_AUTHOR))
            saying = Saying(sayingText, authorText)
        }
        cursor.close()
        return saying
    }

    fun getSayingCount(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME_SAYING", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getAnniversaryCount(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME_ANNIVERSARY WHERE $COL_DELETED_AT IS NULL", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getUpcomingHolidaysAndAnniversaries(): List<Pair<LocalDate, String>> {
        val db = this.readableDatabase
        val results = mutableListOf<Pair<LocalDate, String>>()
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        var dates = mutableListOf<String>()

        arrayOf("holiday","생일","기념일").forEach { cat ->
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COL_APPLY_DT, COL_CATEGORY),
                "$COL_CATEGORY = ? AND $COL_APPLY_DT > ?",
                arrayOf(cat, todayStr),
                null, null, "$COL_APPLY_DT ASC", "1")
            cursor.use {
                while (it.moveToNext()) {
                    val dateStr = it.getString(it.getColumnIndexOrThrow(COL_APPLY_DT))
                    val catStr = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))

                    dates.add("$catStr|$dateStr")
                }
            }
        }

        dates.forEach { date ->
            val (catVal, dateVal) = date.split("|")
            val holidayCursor = db.query(
                TABLE_NAME,
                arrayOf(COL_APPLY_DT, COL_TITLE),
                "$COL_CATEGORY = ? AND $COL_APPLY_DT = ?",
                arrayOf(catVal, dateVal),
                null, null, "$COL_APPLY_DT ASC",
            )
            holidayCursor.use {
                while (it.moveToNext()) {
                    val dateStr = it.getString(it.getColumnIndexOrThrow(COL_APPLY_DT))
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                    results.add(Pair(LocalDate.parse(dateStr), title))
                }
            }
        }
        Log.d(TAG, "getUpcomingHolidaysAndAnniversaries: $results")
        return results.sortedBy { it.first }
    }

    fun getDistinctScheduleTitlesForMonth(categories: List<String>, yearMonth: YearMonth): List<String> {
        val db = this.readableDatabase
        val titles = mutableListOf<String>()
        val monthStr = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)

        if (categories.isEmpty()) {
            return emptyList()
        }

        val categoryPlaceholders = categories.joinToString { "?" }
        val selection = "$COL_CATEGORY IN ($categoryPlaceholders) AND $COL_APPLY_DT LIKE ?"
        val selectionArgs = categories.toTypedArray() + "$monthStr%"

        val cursor = db.query(
            TABLE_NAME,
            arrayOf("DISTINCT $COL_TITLE"),
            selection,
            selectionArgs,
            null, null, "$COL_TITLE ASC"
        )

        val titleColumnIndex = cursor.getColumnIndex(COL_TITLE)
        if (titleColumnIndex == -1) {
            Log.e(TAG, "Column not found in the cursor.")
            cursor.close()
            return emptyList()
        }

        while (cursor.moveToNext()) {
            titles.add(cursor.getString(titleColumnIndex))
        }
        cursor.close()
        return titles
    }

    fun addPersonalSchedule(date: LocalDate, title: String) {
        val randVal = Random.nextInt()
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_SOURCE, "manual")
            put(COL_CATEGORY, "personal")
            put(COL_TYPE, "date")
            put(COL_DATA_KEY, "personal-$date-$randVal")
            put(COL_APPLY_DT, date.toString())
            put(COL_TITLE, title)
            put(COL_ALIAS, title)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun addBirthdayToSchedule(category: String, applyDt: LocalDate, title: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_SOURCE, "birthday")
            put(COL_CATEGORY, category)
            put(COL_TYPE, "date")
            put(COL_APPLY_DT, applyDt.toString())
            put(COL_TITLE, title)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun updatePersonalSchedule(id: Int, newDate: LocalDate, newTitle: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_APPLY_DT, newDate.toString())
            put(COL_TITLE, newTitle)
        }
        db.update(
            TABLE_NAME,
            values,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deletePersonalSchedule(id: Int) {
        val db = this.writableDatabase
        db.delete(
            TABLE_NAME,
            "$COL_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun addAnniversary(source: String = "manual", name: String, shortName: String, category: String, calendarType: String, isYearAccurate: Boolean, originDt: LocalDate, applyDt: LocalDate) {
        val db = this.writableDatabase

        val selection = "$COL_TITLE = ? AND $COL_CATEGORY = ? AND $COL_SOL_LUN = ? AND $COL_APPLY_DT = ? and deleted_at is null "
        val selectionArgs = arrayOf(name, category, calendarType, originDt.toString())

        Log.d(TAG, "addAnniversary: ${selectionArgs.joinToString ("")}")

        val cursor = db.query(
            TABLE_NAME_ANNIVERSARY,
            arrayOf(COL_ID),
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            // Record exists, update it.
            val values = ContentValues().apply {
                put(COL_SOURCE, source)
                put(COL_ALIAS, shortName)
                put(COL_VERIFY, isYearAccurate)
            }
            db.update(
                TABLE_NAME_ANNIVERSARY,
                values,
                selection,
                selectionArgs
            )
            Log.d(TAG, "Updated anniversary for: $name")
        } else {
            // Record does not exist, insert a new one.
            val randVal = Random.nextInt()
            val values = ContentValues().apply {
                put(COL_SOURCE, source)
                put(COL_TYPE, "date")
                put(COL_DATA_KEY, "${category}-${originDt}-${randVal}")
                put(COL_TITLE, name)
                put(COL_ALIAS, shortName)
                put(COL_CATEGORY, category)
                put(COL_SOL_LUN, calendarType)
                put(COL_VERIFY, isYearAccurate)
//                var applyDt = LocalDate.of(LocalDate.now().year, originDt.monthValue, originDt.dayOfMonth)
                put(COL_APPLY_DT, applyDt.toString())
                put(COL_ORIGIN_DT, originDt.toString())
            }
            db.insert(TABLE_NAME_ANNIVERSARY, null, values)
            Log.d(TAG, "Inserted new anniversary for: $name")
        }
        cursor.close()
    }
    
    fun deleteAnniversary(id: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_DELETED_AT, LocalDate.now().toString())
            put(COL_STATUS, "deleted")
        }
        val whereClause = "$COL_ID = ?"
        val whereArgs = arrayOf(id.toString())
        db.update(TABLE_NAME_ANNIVERSARY, values, whereClause, whereArgs)
    }

    fun getAllAnniversaries(): List<Anniversary> {
        val db = this.readableDatabase
        val anniversaries = mutableListOf<Anniversary>()
        val cursor = db.query(
            TABLE_NAME_ANNIVERSARY,
            arrayOf(COL_ID, COL_APPLY_DT, COL_ORIGIN_DT, COL_TITLE, COL_ALIAS, COL_CATEGORY, COL_SOL_LUN),
            "$COL_DELETED_AT IS NULL", null, null, null, "$COL_APPLY_DT ASC"
        )

        val idColumnIndex = cursor.getColumnIndexOrThrow(COL_ID)
        val originDtColumnIndex = cursor.getColumnIndexOrThrow(COL_ORIGIN_DT)
        val applyDtColumnIndex = cursor.getColumnIndexOrThrow(COL_APPLY_DT)
        val titleColumnIndex = cursor.getColumnIndexOrThrow(COL_TITLE)
        val aliasColumnIndex = cursor.getColumnIndexOrThrow(COL_ALIAS)
        val categoryColumnIndex = cursor.getColumnIndexOrThrow(COL_CATEGORY)
        val solLunColumnIndex = cursor.getColumnIndexOrThrow(COL_SOL_LUN)


        while (cursor.moveToNext()) {
            val id = cursor.getInt(idColumnIndex)
            val originDt = cursor.getString(originDtColumnIndex)
            val applyDt = cursor.getString(applyDtColumnIndex)
            val title = cursor.getString(titleColumnIndex)
            val alias = cursor.getString(aliasColumnIndex)
            val category = cursor.getString(categoryColumnIndex)
            val solLun = cursor.getString(solLunColumnIndex)
            if (originDt != null && title != null) {
                try {
                    val date = LocalDate.parse(originDt)
                    val applyDate = LocalDate.parse(applyDt)
                    anniversaries.add(Anniversary(id, date, applyDate, Schedule(id, category, title, solLun)))
                } catch (e: java.time.format.DateTimeParseException) {
                    Log.e(TAG, "Error parsing anniversary date: $originDt", e)
                }
            }
        }
        cursor.close()
        return anniversaries
    }

    fun addDay(source: String, category: String, type: String, dataKey: String, title: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_SOURCE, source)
            put(COL_CATEGORY, category)
            put(COL_TYPE, type)
            put(COL_DATA_KEY, "category-${dataKey}")
            val dateParts = "${dataKey.take(4)}-${dataKey.take(6).drop(4)}-${dataKey.drop(6)}"
            put(COL_APPLY_DT, dateParts)
            put(COL_TITLE, title)
        }

        Log.d(TAG, "Adding day: $values")

        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun countDaysByCategoryAndYear(category: String, year: Int): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_NAME WHERE $COL_CATEGORY = ? AND $COL_DATA_KEY LIKE ?"
        val cursor = db.rawQuery(query, arrayOf(category, "category-$year%"))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getDaysForCategoryMonth(yearMonth: YearMonth, categorys: List<String>): Map<LocalDate, String> {
        val db = this.readableDatabase
        val holidays = mutableMapOf<LocalDate, String>()
        val monthStr = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)

        categorys.forEach { category ->
            val tableName = if (category == "anniversary") TABLE_NAME_ANNIVERSARY else TABLE_NAME
            val SQL = "SELECT $COL_ID, $COL_APPLY_DT, $COL_TITLE FROM $tableName WHERE $COL_CATEGORY = ? AND $COL_APPLY_DT LIKE ?"

            Log.d(TAG, "SQL: $SQL, catetory : $category, monthStr : $monthStr% ")

            val cursor = db.query(
                tableName,
                arrayOf(COL_ID, COL_APPLY_DT, COL_TITLE),
                "$COL_CATEGORY = ? AND $COL_APPLY_DT LIKE ?",
                arrayOf(category, "$monthStr%"),
                null, null, "$COL_APPLY_DT ASC"
            )

            val idColumnIndex = cursor.getColumnIndex(COL_ID)
            val dateColumnIndex = cursor.getColumnIndex(COL_APPLY_DT)
            val titleColumnIndex = cursor.getColumnIndex(COL_TITLE)

            if (idColumnIndex == -1 || dateColumnIndex == -1 || titleColumnIndex == -1) {
                Log.e(TAG, "One or more columns not found in the cursor.")
                cursor.close()
                return emptyMap()
            }

            while (cursor.moveToNext()) {
                val id = cursor.getInt(idColumnIndex)
                val dateStr = cursor.getString(dateColumnIndex)
                val title = cursor.getString(titleColumnIndex)
                if (dateStr != null && title != null) {
                    try {
                        val date = LocalDate.parse(dateStr)
                        if (holidays[date] == null)
                            holidays[date] = "${id}|${category}|${title}"
                        else
                            holidays[date] += "${Constants.my_sep}${id}|${category}|${title}"
                    } catch (e: java.time.format.DateTimeParseException) {
                        Log.e(TAG, "Error parsing date: $dateStr", e)
                    }
                }
            }
            cursor.close()
        }

//        Log.d(TAG, "Holidays: $holidays")
        return holidays
    }

    fun getAllPersonalSchedules(): List<DayRecord> {
        val personalSchedules = mutableListOf<DayRecord>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null, // All columns
            "($COL_CATEGORY = ? and $COL_CATEGORY = ? and $COL_CATEGORY = ? ) AND $COL_DELETED_AT IS NULL",
            arrayOf("personal", "기념일", "생일"),
            null, null, "$COL_APPLY_DT ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COL_ID))
                val source = it.getString(it.getColumnIndexOrThrow(COL_SOURCE))
                val category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))
                val type = it.getString(it.getColumnIndexOrThrow(COL_TYPE))
                val dataKey = it.getString(it.getColumnIndexOrThrow(COL_DATA_KEY))
                val applyDt = it.getString(it.getColumnIndexOrThrow(COL_APPLY_DT))
                val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                val alias = it.getString(it.getColumnIndexOrThrow(COL_ALIAS))
                val value = it.getInt(it.getColumnIndexOrThrow(COL_VALUE))
                val message = it.getString(it.getColumnIndexOrThrow(COL_MESSAGE))
                val description = it.getString(it.getColumnIndexOrThrow(COL_DESCRIPTION))
                val registeredAt = it.getString(it.getColumnIndexOrThrow(COL_REGISTERED_AT))
                val createdAt = it.getString(it.getColumnIndexOrThrow(COL_CREATED_AT))
                val deletedAt = it.getString(it.getColumnIndexOrThrow(COL_DELETED_AT))
                val status = it.getString(it.getColumnIndexOrThrow(COL_STATUS))

                personalSchedules.add(
                    DayRecord(
                        id = id,
                        source = source,
                        category = category,
                        type = type,
                        dataKey = dataKey,
                        applyDt = applyDt,
                        title = title,
                        alias = alias,
                        value = value,
                        message = message,
                        description = description,
                        registeredAt = registeredAt,
                        createdAt = createdAt,
                        deletedAt = deletedAt,
                        status = status
                    )
                )
            }
        }
        return personalSchedules
    }

    fun restoreDays(days: List<DayRecord>): Int {
        val db = this.writableDatabase
        var successCount = 0
        db.transaction {
            for (day in days) {
                val values = ContentValues().apply {
                    put(COL_SOURCE, day.source)
                    put(COL_CATEGORY, day.category)
                    put(COL_TYPE, day.type)
                    put(COL_DATA_KEY, day.dataKey)
                    put(COL_APPLY_DT, day.applyDt)
                    put(COL_TITLE, day.title)
                    put(COL_ALIAS, day.alias)
                    put(COL_VALUE, day.value)
                    put(COL_MESSAGE, day.message)
                    put(COL_DESCRIPTION, day.description)
                    put(COL_REGISTERED_AT, day.registeredAt)
                    put(COL_CREATED_AT, LocalDateTime.now().toString())
                    put(COL_DELETED_AT, day.deletedAt)
                    put(COL_STATUS, day.status)
                }
                val result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                if (result != -1L) {
                    successCount++
                }
            }
        }
        return successCount
    }
}