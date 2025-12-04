package com.ediapp.twocalendar

import android.content.ContentValues
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ExampleUnitTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setup() {
        dbHelper = DatabaseHelper(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun getUpcomingHolidaysAndAnniversaries_returnsCorrectData() {
        // 1. Insert mock data into the database
        val db = dbHelper.writableDatabase
        val today = LocalDate.now()

        val holiday1 = ContentValues().apply {
            put(DatabaseHelper.COL_SOURCE, "app")
            put(DatabaseHelper.COL_CATEGORY, "holiday")
            put(DatabaseHelper.COL_APPLY_DT, today.plusDays(1).toString())
            put(DatabaseHelper.COL_TITLE, "Upcoming Holiday")
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, holiday1)

        val birthday1 = ContentValues().apply {
            put(DatabaseHelper.COL_SOURCE, "app")
            put(DatabaseHelper.COL_CATEGORY, "생일")
            put(DatabaseHelper.COL_APPLY_DT, today.plusDays(5).toString())
            put(DatabaseHelper.COL_TITLE, "John's Birthday")
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, birthday1)
        
        val anniversary1 = ContentValues().apply {
            put(DatabaseHelper.COL_SOURCE, "app")
            put(DatabaseHelper.COL_CATEGORY, "기념일")
            put(DatabaseHelper.COL_APPLY_DT, today.plusDays(10).toString())
            put(DatabaseHelper.COL_TITLE, "Wedding Anniversary")
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, anniversary1)

        val anniversary11 = ContentValues().apply {
            put(DatabaseHelper.COL_SOURCE, "app")
            put(DatabaseHelper.COL_CATEGORY, "기념일")
            put(DatabaseHelper.COL_APPLY_DT, today.plusDays(9).toString())
            put(DatabaseHelper.COL_TITLE, "Wedding Anniversary")
        }
        db.insert(DatabaseHelper.TABLE_NAME, null, anniversary11)

        // 2. Call the function to be tested
        val upcomingEvents = dbHelper.getUpcomingHolidaysAndAnniversaries()

        Log.d("ExampleUnitTest", "getUpcomingHolidaysAndAnniversaries: $upcomingEvents")

        // 3. Assert the results
        assertEquals(4, upcomingEvents.size)

        assertEquals(today.plusDays(1), upcomingEvents[0].first)
        assertEquals("Upcoming Holiday", upcomingEvents[0].second)

        assertEquals(today.plusDays(5), upcomingEvents[1].first)
        assertEquals("John's Birthday", upcomingEvents[1].second)

        assertEquals(today.plusDays(10), upcomingEvents[2].first)
        assertEquals("Wedding Anniversary", upcomingEvents[2].second)
    }
}
