package com.ediapp.twocalendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HolidayNotificationReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "holiday_notification_channel"
    private val NOTIFICATION_ID = 101

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            createNotificationChannel(it)

            val nextHoliday = findNextHoliday(it)
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

                    val mainActivityIntent = Intent(it, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(it, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

                    val builder = NotificationCompat.Builder(it, CHANNEL_ID)
                        .setSmallIcon(R.drawable.calendar_512) // Use an appropriate icon
                        .setContentTitle("다가오는 공휴일 알림")
                        .setContentText(notificationText)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
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

    private fun findNextHoliday(context: Context): Pair<LocalDate, String>? {
        val dbHelper = DatabaseHelper(context)
        val holidays = dbHelper.getUpcomingHolidaysAndAnniversaries()
        val today = LocalDate.now()
        return holidays.firstOrNull { !it.first.isBefore(today) }
    }
}
