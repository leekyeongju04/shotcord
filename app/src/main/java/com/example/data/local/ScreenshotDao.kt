package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScreenshotEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    @Query("SELECT * FROM screenshot_events ORDER BY timestampEpoch DESC")
    fun getAllEvents(): Flow<List<ScreenshotEvent>>

    @Query("SELECT * FROM screenshot_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): ScreenshotEvent?

    @Query("SELECT * FROM screenshot_events WHERE imageUri = :uri LIMIT 1")
    suspend fun getEventByUri(uri: String): ScreenshotEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScreenshotEvent): Long

    @Update
    suspend fun update(event: ScreenshotEvent)

    @Delete
    suspend fun delete(event: ScreenshotEvent)

    @Query("DELETE FROM screenshot_events")
    suspend fun clearAll()
}
