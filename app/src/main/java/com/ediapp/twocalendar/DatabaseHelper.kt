package com.ediapp.twocalendar

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import androidx.core.database.sqlite.transaction

/**
 * Database helper class for managing the application's SQLite database.
 * Handles database creation, version management, and provides helper methods for data operations.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(
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
        private const val DATABASE_VERSION = 1

        // Table name
        const val TABLE_NAME = "tb_days"

        // Column names
        const val COL_ID = "_id"
        const val COL_SOURCE = "source"
        const val COL_CATEGORY = "category"
        const val COL_TYPE = "data_type"
        const val COL_DATA_KEY = "data_key"
        const val COL_APPLY_DT = "apply_dt"
        const val COL_TITLE = "title"
        const val COL_ALIAS = "alias"
        const val COL_VALUE = "value"
        const val COL_MESSAGE = "message"
        const val COL_DESCRIPTION = "description"
        const val COL_REGISTERED_AT = "registered_at"
        const val COL_CREATED_AT = "created_at"
        const val COL_DELETED_AT = "deleted_at"
        const val COL_STATUS = "status"
        // SQL for creating the database table
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
    }

    /**
     * Called when the database is created for the first time.
     * Creates the database tables and initializes them with default data.
     */
    override fun onCreate(db: SQLiteDatabase) {
        try {
            // Begin transaction
            db.transaction {

                try {
                    // Create table
                    execSQL(SQL_CREATE_TABLE)

                    // Insert initial data
                    insertInitialData(this)

                    // Mark transaction as successful
                    Log.d(TAG, "Database created successfully")
                } finally {
                    // End transaction
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database", e)
            throw RuntimeException("Failed to create database", e)
        }
    }

    /**
     * Inserts initial data into the database.
     * @param db The database to insert data into
     */
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

        db.insertWithOnConflict(
            TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Called when the database needs to be upgraded.
     * This method will be called if the database version is increased in the application code.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Upgrading database from version $oldVersion to $newVersion")

        try {
            db.transaction {

                try {
                    // Example of database migration (add more cases as needed)
                    when (oldVersion) {
                        1 -> {
                            // Upgrade from version 1 to 2
                            // Add your migration code here
                            Log.d(TAG, "Migrating database from version 1 to 2")
                        }
                        // Add more cases for future versions
                    }

                    Log.d(TAG, "Database upgraded successfully to version $newVersion")
                } finally {
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database", e)
            // If there's an error, you might want to handle it by recreating the database
            // or notifying the user about the error
            throw RuntimeException("Failed to upgrade database", e)
        }
    }
    
    fun addSchedule(date: LocalDate, title: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_SOURCE, "manual")
            put(COL_CATEGORY, "personal")
            put(COL_TYPE, "date")
            put(COL_DATA_KEY, "personal-$date")
            put(COL_APPLY_DT, date.toString())
            put(COL_TITLE, title)
            put(COL_ALIAS, title)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deletePersonalSchedule(date: LocalDate, title: String) {
        val db = this.writableDatabase
        db.delete(
            TABLE_NAME,
            "$COL_CATEGORY = ? AND $COL_APPLY_DT = ? AND $COL_TITLE = ?",
            arrayOf("personal", date.toString(), title)
        )
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

    fun getDaysForCategoryMonth(yearMonth: java.time.YearMonth, categorys: List<String>): Map<java.time.LocalDate, String> {
        val db = this.readableDatabase
        val holidays = mutableMapOf<java.time.LocalDate, String>()
        val monthStr = String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)

        categorys.forEach { category ->

            val SQL = "SELECT $COL_APPLY_DT, $COL_TITLE FROM $TABLE_NAME WHERE $COL_CATEGORY = ? AND $COL_APPLY_DT LIKE ?"

            Log.d(TAG, "SQL: $SQL, catetory : $category, monthStr : $monthStr% ")

            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COL_APPLY_DT, COL_TITLE),
                "$COL_CATEGORY = ? AND $COL_APPLY_DT LIKE ?",
                arrayOf(category, "$monthStr%"),
                null, null, "$COL_APPLY_DT ASC"
            )

            val dateColumnIndex = cursor.getColumnIndex(COL_APPLY_DT)
            val titleColumnIndex = cursor.getColumnIndex(COL_TITLE)

            if (dateColumnIndex == -1 || titleColumnIndex == -1) {
                Log.e(TAG, "One or more columns not found in the cursor.")
                cursor.close()
                return emptyMap()
            }

            while (cursor.moveToNext()) {
                val dateStr = cursor.getString(dateColumnIndex)
                val title = cursor.getString(titleColumnIndex)
                if (dateStr != null && title != null) {
                    try {
                        val date = java.time.LocalDate.parse(dateStr)
//                        holidays[date] = title
                        if ( holidays[date] == null)
                            holidays[date] = "${category}|${title}"
                        else
                            holidays[date] += "\n${category}|${title}"
                    } catch (e: java.time.format.DateTimeParseException) {
                        Log.e(TAG, "Error parsing date: $dateStr", e)
                    }
                }
            }
            cursor.close()
        }

        Log.d(TAG, "Holidays: $holidays")
        return holidays
    }
}
