package com.example.service

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log

data class ScreenshotMediaInfo(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val resolution: String,
    val timestampEpoch: Long
)

class ScreenshotObserver(
    private val context: Context,
    private val onScreenshotDetected: (Uri, String, Long, String, Long) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val processedIds = HashSet<Long>()
    private var lastProcessedTime: Long = 0L

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        // Check after brief delay to allow OS to finish writing disk blocks
        Handler(Looper.getMainLooper()).postDelayed({
            checkLatestMedia()
        }, 400)
    }

    fun checkLatestMedia() {
        try {
            val media = queryLatestScreenshot(context) ?: return

            val now = System.currentTimeMillis()
            val id = ContentUris.parseId(media.uri)

            if (!processedIds.contains(id) && (now - lastProcessedTime > 1200)) {
                processedIds.add(id)
                lastProcessedTime = now
                if (processedIds.size > 200) {
                    processedIds.clear()
                    processedIds.add(id)
                }

                onScreenshotDetected(
                    media.uri,
                    media.displayName,
                    media.sizeBytes,
                    media.resolution,
                    media.timestampEpoch
                )
            }
        } catch (e: Exception) {
            Log.e("ScreenshotObserver", "Error checking media", e)
        }
    }

    companion object {
        fun queryLatestScreenshot(context: Context): ScreenshotMediaInfo? {
            return try {
                val projection = mutableListOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.Images.Media.RELATIVE_PATH)
                        add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    }
                    @Suppress("DEPRECATION")
                    add(MediaStore.Images.Media.DATA)
                }.toTypedArray()

                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                        val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                        val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

                        val id = cursor.getLong(idCol)
                        val displayName = cursor.getString(nameCol) ?: "Screenshot.png"
                        var dateAdded = cursor.getLong(dateAddedCol)
                        // Normalize seconds to milliseconds if necessary
                        if (dateAdded < 10_000_000_000L) {
                            dateAdded *= 1000L
                        }
                        val size = cursor.getLong(sizeCol)

                        val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                        val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                        val resolution = if (width > 0 && height > 0) "$width x $height" else "1080 x 2400"

                        val path = if (cursor.getColumnIndex(MediaStore.Images.Media.DATA) != -1) {
                            @Suppress("DEPRECATION")
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)) ?: ""
                        } else ""

                        val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH) != -1
                        ) {
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)) ?: ""
                        } else ""

                        val bucketName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME) != -1
                        ) {
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)) ?: ""
                        } else ""

                        val currentTimeMs = System.currentTimeMillis()
                        val diffMs = kotlin.math.abs(currentTimeMs - dateAdded)
                        val isRecent = diffMs <= 180_000L // 3 minutes window

                        val isScreenshot = isScreenshotMatch(displayName, path, relativePath, bucketName)

                        if (isRecent && isScreenshot) {
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )
                            return ScreenshotMediaInfo(
                                uri = contentUri,
                                displayName = displayName,
                                sizeBytes = size,
                                resolution = resolution,
                                timestampEpoch = dateAdded
                            )
                        }
                    }
                    null
                }
            } catch (e: Exception) {
                Log.e("ScreenshotObserver", "Error querying latest screenshot", e)
                null
            }
        }

        fun isScreenshotMatch(name: String, data: String, relativePath: String, bucket: String): Boolean {
            val lowerName = name.lowercase()
            val lowerData = data.lowercase()
            val lowerRel = relativePath.lowercase()
            val lowerBucket = bucket.lowercase()

            return lowerBucket.contains("screenshot") ||
                    lowerRel.contains("screenshot") ||
                    lowerData.contains("screenshot") ||
                    lowerName.contains("screenshot") ||
                    lowerName.contains("screencapture") ||
                    lowerName.contains("screen_") ||
                    lowerName.contains("screen-") ||
                    lowerName.startsWith("ss_") ||
                    lowerName.startsWith("ss-") ||
                    lowerBucket.contains("capture")
        }
    }
}
