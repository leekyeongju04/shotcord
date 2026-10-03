package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScreenshotDatabase
import com.example.data.model.ScreenshotEvent
import com.example.data.preferences.AppPreferences
import com.example.network.DiscordWebhookSender
import com.example.network.WebhookResult
import com.example.service.ScreenshotListenerService
import com.example.util.DateTimeUtil
import com.example.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShotCordUiState(
    val webhookUrl: String = "",
    val userName: String = "",
    val isServiceActive: Boolean = false,
    val autoSend: Boolean = true,
    val compressQuality: Int = 85,
    val events: List<ScreenshotEvent> = emptyList(),
    val selectedEvent: ScreenshotEvent? = null,
    val isTesting: Boolean = false,
    val isSimulating: Boolean = false,
    val infoMessage: String? = null
)

class ShotCordViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val database = ScreenshotDatabase.getInstance(application)
    private val dao = database.screenshotDao()

    private val _uiState = MutableStateFlow(
        ShotCordUiState(
            webhookUrl = AppPreferences.DEFAULT_WEBHOOK_URL,
            userName = AppPreferences.DEFAULT_USER_NAME
        )
    )
    val uiState: StateFlow<ShotCordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.webhookUrlFlow.collect { url ->
                _uiState.update { it.copy(webhookUrl = url) }
            }
        }
        viewModelScope.launch {
            prefs.userNameFlow.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            prefs.isServiceActiveFlow.collect { active ->
                _uiState.update { it.copy(isServiceActive = active) }
            }
        }
        viewModelScope.launch {
            prefs.autoSendFlow.collect { auto ->
                _uiState.update { it.copy(autoSend = auto) }
            }
        }
        viewModelScope.launch {
            prefs.compressQualityFlow.collect { quality ->
                _uiState.update { it.copy(compressQuality = quality) }
            }
        }
        viewModelScope.launch {
            dao.getAllEvents().collect { events ->
                _uiState.update { it.copy(events = events) }
            }
        }
    }

    fun updateWebhookUrl(url: String) {
        viewModelScope.launch {
            prefs.setWebhookUrl(url)
            _uiState.update { it.copy(infoMessage = "Saved") }
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            prefs.setUserName(name)
            _uiState.update { it.copy(infoMessage = "Updated") }
        }
    }

    fun toggleAutoSend() {
        viewModelScope.launch {
            val current = _uiState.value.autoSend
            prefs.setAutoSend(!current)
        }
    }

    fun toggleService(context: Context) {
        val nextState = !_uiState.value.isServiceActive
        if (nextState) {
            ScreenshotListenerService.start(context)
        } else {
            ScreenshotListenerService.stop(context)
        }
    }

    fun sendTestPing() {
        val url = _uiState.value.webhookUrl.trim()
        val user = _uiState.value.userName.trim()

        if (url.isEmpty()) {
            _uiState.update { it.copy(infoMessage = "Missing URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true) }
            val phTime = DateTimeUtil.getPhilippineTimeFormatted()
            val bitmap = ImageUtil.createSimulatedScreenshotBitmap(user, phTime)
            val jpegBytes = ImageUtil.bitmapToJpegBytes(bitmap)
            bitmap.recycle()

            val event = ScreenshotEvent(
                timestampEpoch = System.currentTimeMillis(),
                phFormattedTime = phTime,
                capturedByUser = user,
                imageUri = "test://simulated",
                fileName = "Test_Screenshot_${System.currentTimeMillis()}.jpg",
                fileSizeBytes = jpegBytes.size.toLong(),
                resolution = "720 x 1280",
                status = ScreenshotEvent.STATUS_PENDING,
                thumbnailBytes = jpegBytes
            )

            val insertedId = dao.insert(event)

            val result = DiscordWebhookSender.sendScreenshotWebhook(
                webhookUrl = url,
                phFormattedTime = phTime,
                capturedByUser = user,
                fileName = event.fileName,
                resolution = event.resolution,
                fileSizeBytes = event.fileSizeBytes,
                thumbnailBytes = jpegBytes
            )

            when (result) {
                is WebhookResult.Success -> {
                    dao.update(
                        event.copy(
                            id = insertedId,
                            status = ScreenshotEvent.STATUS_SUCCESS,
                            httpCode = result.code
                        )
                    )
                    _uiState.update { it.copy(infoMessage = "Sent (${result.code})") }
                }
                is WebhookResult.Failure -> {
                    dao.update(
                        event.copy(
                            id = insertedId,
                            status = ScreenshotEvent.STATUS_FAILED,
                            httpCode = result.code,
                            errorMessage = result.message
                        )
                    )
                    _uiState.update { it.copy(infoMessage = "Failed: ${result.message}") }
                }
            }
            _uiState.update { it.copy(isTesting = false) }
        }
    }

    fun simulateCapture() {
        val user = _uiState.value.userName.trim()
        val url = _uiState.value.webhookUrl.trim()
        val autoSend = _uiState.value.autoSend

        viewModelScope.launch {
            _uiState.update { it.copy(isSimulating = true) }
            val epoch = System.currentTimeMillis()
            val phTime = DateTimeUtil.getPhilippineTimeFormatted(epoch)
            val bitmap = ImageUtil.createSimulatedScreenshotBitmap(user, phTime)
            val jpegBytes = ImageUtil.bitmapToJpegBytes(bitmap)
            bitmap.recycle()

            val fileName = "Screenshot_${epoch}.jpg"
            val event = ScreenshotEvent(
                timestampEpoch = epoch,
                phFormattedTime = phTime,
                capturedByUser = user,
                imageUri = "simulated://$epoch",
                fileName = fileName,
                fileSizeBytes = jpegBytes.size.toLong(),
                resolution = "720 x 1280",
                status = ScreenshotEvent.STATUS_PENDING,
                thumbnailBytes = jpegBytes
            )

            val insertedId = dao.insert(event)

            if (autoSend && url.isNotBlank()) {
                val result = DiscordWebhookSender.sendScreenshotWebhook(
                    webhookUrl = url,
                    phFormattedTime = phTime,
                    capturedByUser = user,
                    fileName = fileName,
                    resolution = event.resolution,
                    fileSizeBytes = event.fileSizeBytes,
                    thumbnailBytes = jpegBytes
                )

                when (result) {
                    is WebhookResult.Success -> {
                        dao.update(
                            event.copy(
                                id = insertedId,
                                status = ScreenshotEvent.STATUS_SUCCESS,
                                httpCode = result.code
                            )
                        )
                        _uiState.update { it.copy(infoMessage = "Dispatched") }
                    }
                    is WebhookResult.Failure -> {
                        dao.update(
                            event.copy(
                                id = insertedId,
                                status = ScreenshotEvent.STATUS_FAILED,
                                httpCode = result.code,
                                errorMessage = result.message
                            )
                        )
                        _uiState.update { it.copy(infoMessage = "Failed") }
                    }
                }
            } else {
                _uiState.update { it.copy(infoMessage = "Captured") }
            }
            _uiState.update { it.copy(isSimulating = false) }
        }
    }

    fun processSelectedImage(context: Context, uri: Uri) {
        val user = _uiState.value.userName.trim()
        val url = _uiState.value.webhookUrl.trim()
        val autoSend = _uiState.value.autoSend
        val quality = _uiState.value.compressQuality

        viewModelScope.launch {
            val meta = withContext(Dispatchers.IO) {
                ImageUtil.getImageMetadata(context, uri)
            }
            val thumbnailBytes = withContext(Dispatchers.IO) {
                ImageUtil.loadScaledThumbnail(context, uri, 1080, quality)
            }
            val epoch = System.currentTimeMillis()
            val phTime = DateTimeUtil.getPhilippineTimeFormatted(epoch)

            val event = ScreenshotEvent(
                timestampEpoch = epoch,
                phFormattedTime = phTime,
                capturedByUser = user,
                imageUri = uri.toString(),
                fileName = meta.fileName,
                fileSizeBytes = meta.sizeBytes,
                resolution = meta.resolution,
                status = ScreenshotEvent.STATUS_PENDING,
                thumbnailBytes = thumbnailBytes
            )

            val insertedId = dao.insert(event)

            if (autoSend && url.isNotBlank()) {
                val result = DiscordWebhookSender.sendScreenshotWebhook(
                    webhookUrl = url,
                    phFormattedTime = phTime,
                    capturedByUser = user,
                    fileName = meta.fileName,
                    resolution = meta.resolution,
                    fileSizeBytes = meta.sizeBytes,
                    thumbnailBytes = thumbnailBytes
                )

                when (result) {
                    is WebhookResult.Success -> {
                        dao.update(
                            event.copy(
                                id = insertedId,
                                status = ScreenshotEvent.STATUS_SUCCESS,
                                httpCode = result.code
                            )
                        )
                        _uiState.update { it.copy(infoMessage = "Dispatched") }
                    }
                    is WebhookResult.Failure -> {
                        dao.update(
                            event.copy(
                                id = insertedId,
                                status = ScreenshotEvent.STATUS_FAILED,
                                httpCode = result.code,
                                errorMessage = result.message
                            )
                        )
                        _uiState.update { it.copy(infoMessage = "Failed") }
                    }
                }
            } else {
                _uiState.update { it.copy(infoMessage = "Loaded") }
            }
        }
    }

    fun retryEvent(event: ScreenshotEvent) {
        val url = _uiState.value.webhookUrl.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(infoMessage = "Missing URL") }
            return
        }

        viewModelScope.launch {
            dao.update(event.copy(status = ScreenshotEvent.STATUS_PENDING, errorMessage = null))

            val result = DiscordWebhookSender.sendScreenshotWebhook(
                webhookUrl = url,
                phFormattedTime = event.phFormattedTime,
                capturedByUser = event.capturedByUser,
                fileName = event.fileName,
                resolution = event.resolution,
                fileSizeBytes = event.fileSizeBytes,
                thumbnailBytes = event.thumbnailBytes
            )

            when (result) {
                is WebhookResult.Success -> {
                    dao.update(
                        event.copy(
                            status = ScreenshotEvent.STATUS_SUCCESS,
                            httpCode = result.code,
                            errorMessage = null
                        )
                    )
                    _uiState.update { it.copy(infoMessage = "Dispatched") }
                }
                is WebhookResult.Failure -> {
                    dao.update(
                        event.copy(
                            status = ScreenshotEvent.STATUS_FAILED,
                            httpCode = result.code,
                            errorMessage = result.message
                        )
                    )
                    _uiState.update { it.copy(infoMessage = "Failed: ${result.message}") }
                }
            }
        }
    }

    fun deleteEvent(event: ScreenshotEvent) {
        viewModelScope.launch {
            dao.delete(event)
            if (_uiState.value.selectedEvent?.id == event.id) {
                _uiState.update { it.copy(selectedEvent = null) }
            }
        }
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            dao.clearAll()
            _uiState.update { it.copy(selectedEvent = null, infoMessage = "Cleared") }
        }
    }

    fun selectEvent(event: ScreenshotEvent?) {
        _uiState.update { it.copy(selectedEvent = event) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
