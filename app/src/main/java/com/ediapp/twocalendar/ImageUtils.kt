package com.ediapp.twocalendar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.core.widget.NestedScrollView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun captureAndShare(view: View, context: Context) {
    val bitmap = withContext(Dispatchers.Main) {
        getBitmapFromView(view)
    }

    withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "screenshot.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "이미지 공유"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun getBitmapFromView(view: View): Bitmap {
    val nestedScrollView = findNestedScrollViewRecursive(view)
    if (nestedScrollView != null && nestedScrollView.childCount > 0) {
        // 1. Store original state for perfect restoration.
        val originalWidth = nestedScrollView.width
        val originalHeight = nestedScrollView.height
        val originalScrollX = nestedScrollView.scrollX
        val originalScrollY = nestedScrollView.scrollY
        val originalLayerType = nestedScrollView.layerType

        // Store original clipChildren for all parents.
        val parentViews = mutableListOf<ViewGroup>()
        val originalClipChildren = mutableListOf<Boolean>()
        var parentView = nestedScrollView.parent
        while (parentView is ViewGroup) {
            parentViews.add(parentView)
            originalClipChildren.add(parentView.clipChildren)
            parentView = parentView.parent
        }

        try {
            // 2. Accurately measure the child to get its full, unrestricted content height.
            val child = nestedScrollView.getChildAt(0)
            val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY)
            val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            child.measure(widthMeasureSpec, heightMeasureSpec)
            val totalHeight = child.measuredHeight

            if (totalHeight <= originalHeight) {
                return view.drawToBitmap()
            }

            // 3. Create a bitmap with the full content size.
            val bitmap = Bitmap.createBitmap(originalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // Ensure a non-transparent background.

            // 4. PREPARE FOR DRAWING:
            // a) Disable clipping on all parents to allow drawing outside original bounds.
            parentViews.forEach { it.clipChildren = false }
            // b) Force software rendering to bypass GPU optimizations.
            nestedScrollView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            // c) Scroll to the top to ensure we draw from the beginning.
            nestedScrollView.scrollTo(0, 0)

            // 5. Temporarily resize the scroll view to its full content size.
            nestedScrollView.measure(
                View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(totalHeight, View.MeasureSpec.EXACTLY)
            )
            nestedScrollView.layout(0, 0, originalWidth, totalHeight)

            // 6. Draw the fully-expanded view.
            nestedScrollView.draw(canvas)

            return bitmap

        } finally {
            // 7. RESTORE: Put everything back exactly as it was.
            nestedScrollView.setLayerType(originalLayerType, null)
            // Restore original clipping for all parents.
            parentViews.forEachIndexed { index, p -> p.clipChildren = originalClipChildren[index] }

            nestedScrollView.measure(
                View.MeasureSpec.makeMeasureSpec(originalWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(originalHeight, View.MeasureSpec.EXACTLY)
            )
            nestedScrollView.layout(0, 0, originalWidth, originalHeight)
            nestedScrollView.scrollTo(originalScrollX, originalScrollY)
        }
    }

    return view.drawToBitmap()
}


private fun findNestedScrollViewRecursive(view: View): NestedScrollView? {
    if (view is NestedScrollView) {
        return view
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val childView = view.getChildAt(i)
            val result = findNestedScrollViewRecursive(childView)
            if (result != null) {
                return result
            }
        }
    }
    return null
}
