package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

data class ImageMetadata(
    val fileName: String,
    val sizeBytes: Long,
    val resolution: String
)

object ImageUtil {

    fun getImageMetadata(context: Context, uri: Uri): ImageMetadata {
        var name = "Screenshot_${System.currentTimeMillis()}.jpg"
        var size = 0L
        var resolution = "Unknown"

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            var input: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            if (options.outWidth > 0 && options.outHeight > 0) {
                resolution = "${options.outWidth} x ${options.outHeight}"
            }
        } catch (_: Exception) {}

        return ImageMetadata(fileName = name, sizeBytes = size, resolution = resolution)
    }

    fun loadScaledThumbnail(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1200,
        quality: Int = 85
    ): ByteArray? {
        return try {
            var input = context.contentResolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, boundsOptions)
            input.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            val maxSide = max(origWidth, origHeight)
            while ((maxSide / sampleSize) > maxDimension * 1.5) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            input = context.contentResolver.openInputStream(uri) ?: return null
            val decodedBitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input.close()

            if (decodedBitmap == null) return null

            // Scale down if still larger than maxDimension
            val scale = if (max(decodedBitmap.width, decodedBitmap.height) > maxDimension) {
                maxDimension.toFloat() / max(decodedBitmap.width, decodedBitmap.height)
            } else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                val newW = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)
                val resized = Bitmap.createScaledBitmap(decodedBitmap, newW, newH, true)
                if (resized != decodedBitmap) decodedBitmap.recycle()
                resized
            } else {
                decodedBitmap
            }

            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), stream)
            scaledBitmap.recycle()
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun createSimulatedScreenshotBitmap(userName: String, phTimeText: String): Bitmap {
        val width = 720
        val height = 1280
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#12141C"))

        // Status bar area
        val barPaint = Paint().apply { color = Color.parseColor("#1E202B") }
        canvas.drawRect(0f, 0f, width.toFloat(), 64f, barPaint)

        // Card area
        val cardPaint = Paint().apply { color = Color.parseColor("#252836") }
        canvas.drawRoundRect(40f, 100f, (width - 40).toFloat(), 500f, 24f, 24f, cardPaint)

        // Accent top bar
        val accentPaint = Paint().apply { color = Color.parseColor("#5865F2") }
        canvas.drawRoundRect(40f, 100f, (width - 40).toFloat(), 120f, 12f, 12f, accentPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("ShotCord Screen Event", 80f, 180f, textPaint)

        val subPaint = Paint().apply {
            color = Color.parseColor("#9BA3B8")
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("Captured: $phTimeText", 80f, 240f, subPaint)
        canvas.drawText("User: $userName", 80f, 290f, subPaint)
        canvas.drawText("Zone: Asia/Manila (UTC+8)", 80f, 340f, subPaint)

        // Watermark badge
        val badgePaint = Paint().apply { color = Color.parseColor("#57F287") }
        canvas.drawCircle(80f, 410f, 12f, badgePaint)
        canvas.drawText("Live Monitoring Active", 110f, 418f, subPaint)

        return bitmap
    }

    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
