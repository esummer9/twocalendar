package com.ediapp.twocalendar.photosaver

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoSaver {

    fun saveBackupCodeImage(
        context: Context,
        randomCode: String,
        backupCode: String,
        appName: String
    ): Boolean {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val maskedBackupCode = if (backupCode.length > 2) {
            backupCode.first() + "* ".repeat(backupCode.length - 2) + backupCode.last()
        } else {
            backupCode
        }

        val textToDraw = """
        랜덤코드: $randomCode
        백업코드: $maskedBackupCode
        저장일시: $date
        출처: $appName
        """.trimIndent()

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            textAlign = Paint.Align.LEFT
        }

        val textWidth = textToDraw.lines().maxOf { textPaint.measureText(it) }
        val textHeight = (textPaint.descent() - textPaint.ascent()) * textToDraw.lines().size
        val padding = 40f
        val margin = 5 * context.resources.displayMetrics.density

        val bitmap = Bitmap.createBitmap(
            (textWidth + padding * 2 + margin * 2).toInt(),
            (textHeight + padding * 2 + margin * 2).toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.rgb(0, 0, 139) // Dark Blue
            style = Paint.Style.STROKE
            strokeWidth = 1f // 1 pixel border
        }

        canvas.drawRect(
            margin,
            margin,
            canvas.width - margin,
            canvas.height - margin,
            borderPaint
        )

        var y = padding + margin - textPaint.ascent()
        for (line in textToDraw.lines()) {
            canvas.drawText(line, padding + margin, y, textPaint)
            y += textPaint.descent() - textPaint.ascent()
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "backup_code_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return try {
            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    if (outputStream == null) return false
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                true
            } ?: false
        } catch (e: Exception) {
            uri?.let { resolver.delete(it, null, null) }
            e.printStackTrace()
            false
        }
    }
}
