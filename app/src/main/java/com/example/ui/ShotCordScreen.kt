package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.ScreenshotEvent
import com.example.data.preferences.AppPreferences
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordRed
import com.example.ui.theme.DiscordYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotCordScreen(viewModel: ShotCordViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var webhookInput by remember(uiState.webhookUrl) { mutableStateOf(uiState.webhookUrl) }
    var userNameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    var showConfigSection by remember { mutableStateOf(true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.processSelectedImage(context, uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.toggleService(context)
    }

    fun requestPermissionsAndToggle() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            viewModel.toggleService(context)
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionsAndToggle()
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiscordBlurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "ShotCord",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ShotCord",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Config Toggle Icon
                    IconButton(
                        onClick = { showConfigSection = !showConfigSection },
                        modifier = Modifier.testTag("toggle_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Config",
                            tint = if (showConfigSection) DiscordBlurple else TextSecondary
                        )
                    }

                    // Clear All Icon
                    if (uiState.events.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllEvents() },
                            modifier = Modifier.testTag("clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Status & Actions Control Card
                item {
                    StatusControlCard(
                        isActive = uiState.isServiceActive,
                        eventsCount = uiState.events.size,
                        isTesting = uiState.isTesting,
                        isSimulating = uiState.isSimulating,
                        onToggleService = { requestPermissionsAndToggle() },
                        onTestWebhook = { viewModel.sendTestPing() },
                        onSimulateScreenshot = { viewModel.simulateCapture() },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                // Collapsible Config Section
                item {
                    AnimatedVisibility(visible = showConfigSection) {
                        ConfigurationCard(
                            webhookUrl = webhookInput,
                            onWebhookChange = { webhookInput = it },
                            onSaveWebhook = { viewModel.updateWebhookUrl(webhookInput) },
                            onPasteWebhook = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                val text = if (clip != null && clip.itemCount > 0) {
                                    clip.getItemAt(0).text?.toString()?.trim().orEmpty()
                                } else ""
                                val targetUrl = text.ifBlank { AppPreferences.DEFAULT_WEBHOOK_URL }
                                webhookInput = targetUrl
                                viewModel.updateWebhookUrl(targetUrl)
                            },
                            userName = userNameInput,
                            onUserNameChange = { userNameInput = it },
                            onSaveUserName = { viewModel.updateUserName(userNameInput) },
                            onResetDeviceName = {
                                val defaultName = AppPreferences.DEFAULT_USER_NAME
                                userNameInput = defaultName
                                viewModel.updateUserName(defaultName)
                            },
                            autoSend = uiState.autoSend,
                            onToggleAutoSend = { viewModel.toggleAutoSend() }
                        )
                    }
                }

                // Captures Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Feed",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Captures (${uiState.events.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        if (uiState.events.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Timezone",
                                    tint = DiscordBlurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Asia/Manila (PST)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DiscordBlurple
                                )
                            }
                        }
                    }
                }

                // Captures List
                if (uiState.events.isEmpty()) {
                    item {
                        EmptyStateCard(
                            onSimulate = { viewModel.simulateCapture() },
                            onPick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                } else {
                    items(uiState.events, key = { it.id }) { event ->
                        ScreenshotEventCard(
                            event = event,
                            onClick = { viewModel.selectEvent(event) },
                            onRetry = { viewModel.retryEvent(event) },
                            onCopyDetails = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Screenshot Event", "${event.phFormattedTime}\n${event.capturedByUser}\n${event.fileName}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { viewModel.deleteEvent(event) }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    uiState.selectedEvent?.let { event ->
        EventDetailDialog(
            event = event,
            onDismiss = { viewModel.selectEvent(null) },
            onRetry = {
                viewModel.retryEvent(event)
                viewModel.selectEvent(null)
            },
            onDelete = {
                viewModel.deleteEvent(event)
                viewModel.selectEvent(null)
            }
        )
    }
}

@Composable
fun StatusControlCard(
    isActive: Boolean,
    eventsCount: Int,
    isTesting: Boolean,
    isSimulating: Boolean,
    onToggleService: () -> Unit,
    onTestWebhook: () -> Unit,
    onSimulateScreenshot: () -> Unit,
    onPickPhoto: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardBg = DarkSurface
    val statusColor by animateColorAsState(
        targetValue = if (isActive) DiscordGreen else TextMuted,
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Indicator & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .then(if (isActive) Modifier.scale(pulseScale) else Modifier)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isActive) "LISTENING" else "STANDBY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = statusColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$eventsCount LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Action Icons Row (Strictly using icons for actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Play / Stop Listener
                FilledIconButton(
                    onClick = onToggleService,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("toggle_service_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isActive) DiscordRed else DiscordGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Listener",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Send Test Webhook
                FilledTonalIconButton(
                    onClick = onTestWebhook,
                    enabled = !isTesting,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("test_webhook_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = DiscordBlurple
                    )
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DiscordBlurple,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Test Webhook",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Simulate Screenshot Capture
                FilledTonalIconButton(
                    onClick = onSimulateScreenshot,
                    enabled = !isSimulating,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("simulate_screenshot_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = DiscordYellow
                    )
                ) {
                    if (isSimulating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DiscordYellow,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Simulate Screenshot",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Pick Photo from Storage
                FilledTonalIconButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("pick_photo_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Select Screenshot",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigurationCard(
    webhookUrl: String,
    onWebhookChange: (String) -> Unit,
    onSaveWebhook: () -> Unit,
    onPasteWebhook: () -> Unit,
    userName: String,
    onUserNameChange: (String) -> Unit,
    onSaveUserName: () -> Unit,
    onResetDeviceName: () -> Unit,
    autoSend: Boolean,
    onToggleAutoSend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Webhook URL field
            OutlinedTextField(
                value = webhookUrl,
                onValueChange = onWebhookChange,
                label = { Text("Discord Webhook") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("webhook_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DiscordBlurple,
                    unfocusedBorderColor = DarkBorder,
                    focusedLabelColor = DiscordBlurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (webhookUrl.isNotEmpty()) {
                            IconButton(onClick = { onWebhookChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(onClick = onPasteWebhook) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = DiscordBlurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onSaveWebhook) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = DiscordGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            // User Identity field
            OutlinedTextField(
                value = userName,
                onValueChange = onUserNameChange,
                label = { Text("Device User") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DiscordBlurple,
                    unfocusedBorderColor = DarkBorder,
                    focusedLabelColor = DiscordBlurple,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onResetDeviceName) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = "Device Default",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onSaveUserName) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = DiscordGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            // Auto-relay Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Auto",
                        tint = if (autoSend) DiscordGreen else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-Relay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Switch(
                    checked = autoSend,
                    onCheckedChange = { onToggleAutoSend() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DiscordGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }
    }
}

@Composable
fun ScreenshotEventCard(
    event: ScreenshotEvent,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onCopyDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (event.status) {
        ScreenshotEvent.STATUS_SUCCESS -> DiscordGreen
        ScreenshotEvent.STATUS_FAILED -> DiscordRed
        else -> DiscordYellow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("event_card_${event.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (event.thumbnailBytes != null && event.thumbnailBytes.isNotEmpty()) {
                    val bitmap = remember(event.thumbnailBytes) {
                        try {
                            BitmapFactory.decodeByteArray(event.thumbnailBytes, 0, event.thumbnailBytes.size)?.asImageBitmap()
                        } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextMuted)
                    }
                } else if (event.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = event.imageUri,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (event.status == ScreenshotEvent.STATUS_SUCCESS) "204 SENT" else event.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PH Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.phFormattedTime,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // User
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = DiscordBlurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.capturedByUser,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DiscordBlurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Icons on card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (event.status != ScreenshotEvent.STATUS_SUCCESS) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Retry",
                            tint = DiscordBlurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onCopyDetails,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    onSimulate: () -> Unit,
    onPick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(
                    onClick = onSimulate,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = DiscordYellow
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Simulate")
                }

                FilledTonalIconButton(
                    onClick = onPick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = DiscordBlurple
                    )
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick")
                }
            }
        }
    }
}

@Composable
fun EventDetailDialog(
    event: ScreenshotEvent,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.fileName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Thumbnail Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (event.thumbnailBytes != null && event.thumbnailBytes.isNotEmpty()) {
                        val bitmap = remember(event.thumbnailBytes) {
                            try {
                                BitmapFactory.decodeByteArray(event.thumbnailBytes, 0, event.thumbnailBytes.size)?.asImageBitmap()
                            } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Full Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (event.imageUri.isNotBlank()) {
                        AsyncImage(
                            model = event.imageUri,
                            contentDescription = "Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Info Rows
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = DiscordBlurple, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(event.phFormattedTime, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = DiscordGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(event.capturedByUser, fontSize = 12.sp, color = TextPrimary)
                        }
                        if (event.resolution.isNotBlank()) {
                            Text(event.resolution, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        }
                        if (event.errorMessage != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DiscordRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(event.errorMessage, fontSize = 11.sp, color = DiscordRed)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiscordRed)
                }
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = DiscordBlurple)
                }
            }
        }
    )
}
