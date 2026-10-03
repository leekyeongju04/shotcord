package com.example.network

import com.example.util.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class WebhookResult {
    data class Success(val code: Int, val message: String) : WebhookResult()
    data class Failure(val code: Int, val message: String) : WebhookResult()
}

object DiscordWebhookSender {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isValidWebhookUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false
        val regex = Regex("^https://(discord\\.com|discordapp\\.com)/api/webhooks/\\d+/[A-Za-z0-9_-]+$")
        return regex.matches(trimmed)
    }

    suspend fun sendScreenshotWebhook(
        webhookUrl: String,
        phFormattedTime: String,
        capturedByUser: String,
        fileName: String,
        resolution: String,
        fileSizeBytes: Long,
        thumbnailBytes: ByteArray?
    ): WebhookResult = withContext(Dispatchers.IO) {
        val trimmedUrl = webhookUrl.trim()
        if (!isValidWebhookUrl(trimmedUrl)) {
            return@withContext WebhookResult.Failure(
                code = 400,
                message = "Invalid Discord Webhook URL"
            )
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("username", "ShotCord Monitor")
                put("avatar_url", "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f4f8.png")

                val embed = JSONObject().apply {
                    put("title", "📸 Screenshot Event Captured")
                    put("color", 0x5865F2) // Discord Blurple

                    val fields = JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "🕒 Date & Time (PH Time)")
                            put("value", "`$phFormattedTime`")
                            put("inline", false)
                        })
                        put(JSONObject().apply {
                            put("name", "👤 Captured By")
                            put("value", "**$capturedByUser**")
                            put("inline", true)
                        })
                        put(JSONObject().apply {
                            put("name", "📁 File Name")
                            put("value", "`$fileName`")
                            put("inline", true)
                        })
                        if (resolution.isNotBlank() && resolution != "Unknown") {
                            put(JSONObject().apply {
                                put("name", "📐 Resolution")
                                put("value", "`$resolution`")
                                put("inline", true)
                            })
                        }
                    }
                    put("fields", fields)

                    if (thumbnailBytes != null && thumbnailBytes.isNotEmpty()) {
                        put("image", JSONObject().apply {
                            put("url", "attachment://screenshot_thumb.jpg")
                        })
                    }

                    put("footer", JSONObject().apply {
                        put("text", "ShotCord Relay • Asia/Manila (PST / UTC+8)")
                    })
                    put("timestamp", DateTimeUtil.getPhilippineTimeIso())
                }

                put("embeds", JSONArray().put(embed))
            }

            val requestBody = if (thumbnailBytes != null && thumbnailBytes.isNotEmpty()) {
                val imagePart = thumbnailBytes.toRequestBody("image/jpeg".toMediaType(), 0, thumbnailBytes.size)
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("payload_json", jsonPayload.toString())
                    .addFormDataPart("files[0]", "screenshot_thumb.jpg", imagePart)
                    .build()
            } else {
                jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            }

            val request = Request.Builder()
                .url(trimmedUrl)
                .post(requestBody)
                .addHeader("User-Agent", "ShotCord-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string().orEmpty()

            if (code in 200..299) {
                WebhookResult.Success(code, "Message dispatched ($code)")
            } else {
                WebhookResult.Failure(code, if (body.isNotBlank()) body else "HTTP $code error")
            }
        } catch (e: IOException) {
            WebhookResult.Failure(0, e.localizedMessage ?: "Network connection error")
        } catch (e: Exception) {
            WebhookResult.Failure(0, e.localizedMessage ?: "Unexpected error")
        }
    }

    suspend fun sendTestPing(webhookUrl: String, userName: String): WebhookResult = withContext(Dispatchers.IO) {
        val phTime = DateTimeUtil.getPhilippineTimeFormatted()
        val dummyBytes = com.example.util.ImageUtil.createSimulatedScreenshotBitmap(userName, phTime)
        val jpegBytes = com.example.util.ImageUtil.bitmapToJpegBytes(dummyBytes)
        dummyBytes.recycle()

        sendScreenshotWebhook(
            webhookUrl = webhookUrl,
            phFormattedTime = phTime,
            capturedByUser = userName,
            fileName = "Test_Screenshot_${System.currentTimeMillis()}.jpg",
            resolution = "720 x 1280",
            fileSizeBytes = jpegBytes.size.toLong(),
            thumbnailBytes = jpegBytes
        )
    }
}
