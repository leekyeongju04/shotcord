package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.service.ScreenshotListenerService
import com.example.service.ScreenshotObserver
import com.example.ui.ShotCordScreen
import com.example.ui.ShotCordViewModel
import com.example.ui.theme.ShotCordTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShotCordViewModel by viewModels()
    private var screenCaptureCallback: Activity.ScreenCaptureCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenCaptureCallback = Activity.ScreenCaptureCallback {
                handleScreenCaptureDetected()
            }
        }

        setContent {
            ShotCordTheme {
                ShotCordScreen(viewModel = viewModel)
            }
        }
    }

    private fun handleScreenCaptureDetected() {
        val handler = Handler(Looper.getMainLooper())
        // Attempt queries with progressive backoff to allow OS to write MediaStore row
        val delays = listOf(200L, 700L, 1500L, 3000L)
        for (delay in delays) {
            handler.postDelayed({
                val media = ScreenshotObserver.queryLatestScreenshot(applicationContext)
                if (media != null) {
                    ScreenshotListenerService.triggerManualCapture(applicationContext)
                } else if (delay == 3000L) {
                    // Fallback to in-app captured simulated event if MediaStore row is restricted
                    viewModel.simulateCapture()
                }
            }, delay)
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenCaptureCallback?.let {
                try {
                    registerScreenCaptureCallback(mainExecutor, it)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenCaptureCallback?.let {
                try {
                    unregisterScreenCaptureCallback(it)
                } catch (_: Exception) {}
            }
        }
        super.onStop()
    }
}
