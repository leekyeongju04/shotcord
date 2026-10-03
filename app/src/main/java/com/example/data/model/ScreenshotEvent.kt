package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshot_events")
data class ScreenshotEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val phFormattedTime: String = "",
    val capturedByUser: String = "",
    val imageUri: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val resolution: String = "",
    val status: String = STATUS_PENDING,
    val httpCode: Int = 0,
    val errorMessage: String? = null,
    val thumbnailBytes: ByteArray? = null
) {
    companion object {
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_PENDING = "PENDING"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScreenshotEvent
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
