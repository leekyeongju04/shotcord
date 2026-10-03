package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.DiscordWebhookSender
import com.example.util.DateTimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ShotCord", appName)
    }

    @Test
    fun testPhilippineTimeFormatting() {
        // Fix to 1700000000000L = 2023-11-14 22:13:20 UTC
        // Manila is UTC+8 => 2023-11-15 06:13:20 AM
        val epochMillis = 1700000000000L
        val formatted = DateTimeUtil.getPhilippineTimeFormatted(epochMillis)
        assertTrue(formatted.contains("PST (UTC+8)"))
        assertTrue(formatted.contains("2023"))
        assertTrue(formatted.contains("November 15"))

        val shortTime = DateTimeUtil.getPhilippineTimeShort(epochMillis)
        assertTrue(shortTime.contains("Nov 15"))
    }

    @Test
    fun testWebhookValidation() {
        assertTrue(
            DiscordWebhookSender.isValidWebhookUrl(
                "https://discord.com/api/webhooks/1234567890/AbCdEfGhIjKlMnOpQrStUvWxYz-123_456"
            )
        )
        assertTrue(
            DiscordWebhookSender.isValidWebhookUrl(
                "https://discordapp.com/api/webhooks/987654321/token_test-123"
            )
        )
        assertFalse(DiscordWebhookSender.isValidWebhookUrl("https://example.com/webhook"))
        assertFalse(DiscordWebhookSender.isValidWebhookUrl("invalid-url"))
        assertFalse(DiscordWebhookSender.isValidWebhookUrl(""))
    }
}
