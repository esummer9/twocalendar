package com.ediapp.twocalendar

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.ediapp.twocalendar.network.LunarApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

object UpdateBirthdayUtil {

    suspend fun updateBirthdayRecords(context: Context) {
        withContext(Dispatchers.IO) {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.writableDatabase
            val currentYear = LocalDate.now().year

            val cursor = db.query(
                DatabaseHelper.TABLE_NAME_ANNIVERSARY,
                arrayOf(
                    DatabaseHelper.COL_ID,
                    DatabaseHelper.COL_APPLY_DT,
                    DatabaseHelper.COL_ORIGIN_DT,
                    DatabaseHelper.COL_SOL_LUN
                ),
                "strftime('%Y', ${DatabaseHelper.COL_APPLY_DT}) != ? AND ${DatabaseHelper.COL_DELETED_AT} IS NULL",
                arrayOf(currentYear.toString()),
                null, null, null
            )

            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID))
                val originDtStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ORIGIN_DT))
                val solLun = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SOL_LUN))
                val originDt = LocalDate.parse(originDtStr)

                var newApplyDt: LocalDate? = null
                if (solLun == "양력") {
                    newApplyDt = originDt.withYear(currentYear)
                } else if (solLun == "음력") {
                    val solarDate = LunarApi.convertToSolar(currentYear, originDt.monthValue, originDt.dayOfMonth)
                    if (solarDate != null) {
                        newApplyDt = solarDate
                    }
                }

                if (newApplyDt != null) {
                    val values = ContentValues().apply {
                        put(DatabaseHelper.COL_APPLY_DT, newApplyDt.toString())
                    }
                    db.update(
                        DatabaseHelper.TABLE_NAME_ANNIVERSARY,
                        values,
                        "${DatabaseHelper.COL_ID} = ?",
                        arrayOf(id.toString())
                    )
                }
            }
            cursor.close()
        }
    }
}
