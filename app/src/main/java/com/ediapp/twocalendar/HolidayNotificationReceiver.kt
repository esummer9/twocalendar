package com.ediapp.twocalendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

class HolidayNotificationReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "holiday_notification_channel"
    private val NOTIFICATION_ID = 101

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            createNotificationChannel(it)

            val nextHoliday = findNextHoliday()
            nextHoliday?.let {(date, name) ->
                val today = LocalDate.now()
                val daysUntil = ChronoUnit.DAYS.between(today, date)

                val notificationText = when {
                    daysUntil == 0L -> "오늘은 공휴일 ($name) 입니다!"
                    daysUntil > 0 -> "$name 까지 ${daysUntil}일 남았습니다."
                    else -> ""
                }

                if (notificationText.isNotEmpty()) {
                    val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val builder = NotificationCompat.Builder(it, CHANNEL_ID)
                        .setSmallIcon(R.drawable.calendar_512) // Use an appropriate icon
                        .setContentTitle("다가오는 공휴일 알림")
                        .setContentText(notificationText)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)

                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "공휴일 알림"
            val descriptionText = "다가오는 공휴일에 대한 알림"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun findNextHoliday(): Pair<LocalDate, String>? {
        val holidays = listOf(
            Pair(LocalDate.of(2025, 1, 1), "신정"),
            Pair(LocalDate.of(2025, 1, 28), "설날"),
            Pair(LocalDate.of(2025, 1, 29), "설날"),
            Pair(LocalDate.of(2025, 1, 30), "설날"),
            Pair(LocalDate.of(2025, 3, 1), "삼일절"),
            Pair(LocalDate.of(2025, 5, 5), "어린이날"),
            Pair(LocalDate.of(2025, 5, 6), "대체 공휴일"), // 어린이날 대체공휴일
            Pair(LocalDate.of(2025, 5, 26), "부처님 오신 날"),
            Pair(LocalDate.of(2025, 6, 6), "현충일"),
            Pair(LocalDate.of(2025, 8, 15), "광복절"),
            Pair(LocalDate.of(2025, 10, 3), "개천절"),
            Pair(LocalDate.of(2025, 10, 6), "추석"),
            Pair(LocalDate.of(2025, 10, 7), "추석"),
            Pair(LocalDate.of(2025, 10, 8), "추석"),
            Pair(LocalDate.of(2025, 10, 9), "한글날"),
            Pair(LocalDate.of(2025, 12, 25), "성탄절")
        )

        val today = LocalDate.now()
        return holidays
            .filter { it.first >= today || it.first.year == today.year + 1 } // Include holidays from next year for proper wrap-around
            .minByOrNull {
                // If the holiday is in the past for the current year, consider it for the next year
                val holidayDate = if (it.first.isBefore(today)) it.first.plusYears(1) else it.first
                ChronoUnit.DAYS.between(today, holidayDate)
            }
    }
}
