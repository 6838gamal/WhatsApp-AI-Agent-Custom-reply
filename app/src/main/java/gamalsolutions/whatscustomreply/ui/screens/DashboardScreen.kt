package gamalsolutions.whatscustomreply.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel

// Helper to check Notification Listener service status
fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val flatName = ComponentName(context, gamalsolutions.whatscustomreply.service.WhatsAppNotificationListenerService::class.java).flattenToString()
    return packageNames != null && packageNames.contains(flatName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToReplies: () -> Unit,
    onNavigateToGemini: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val totalLogs by viewModel.totalLogCount.collectAsStateWithLifecycle()
    val successLogs by viewModel.successLogCount.collectAsStateWithLifecycle()

    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

    // Periodically re-check permission if the user goes away and comes back
    LaunchedEffect(Unit) {
        while (true) {
            isPermissionGranted = isNotificationServiceEnabled(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Futuristic Indigo/Violet Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "WhatsApp AI Auto-Responder",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automated chat assistant running locally inside your device.",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp
                )
            }
        }

        // 1. Notification Access Warning Card
        if (!isPermissionGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().testTag("permission_warning_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning Notification Access",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Notification Access Required",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Android requires Notification Access permission for this app to detect incoming WhatsApp notifications and trigger replies.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("grant_permission_button")
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. Global Status Toggle Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (settings.isServiceEnabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (settings.isServiceEnabled) Icons.Filled.CheckCircle else Icons.Filled.Pause,
                            contentDescription = "Service Status Icon",
                            tint = if (settings.isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Column {
                        Text(
                            text = "Auto Reply Service",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (settings.isServiceEnabled) "Active and listening..." else "Service is paused.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = settings.isServiceEnabled,
                    onCheckedChange = { viewModel.updateServiceEnabled(it) },
                    modifier = Modifier.testTag("service_toggle_switch")
                )
            }
        }

        // 3. Operational Mode Settings Displays
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToReplies() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = "Custom Keywords",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Custom Replies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Trigger replies based on keyword sets.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToGemini() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Gemini Settings",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Gemini AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Configure prompt rules and Gemini models.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Mode Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current Active Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (settings.replyMode) {
                            "CUSTOM" -> Icons.Filled.Settings
                            "GEMINI" -> Icons.Filled.AutoAwesome
                            else -> Icons.Filled.SettingsInputComposite
                        },
                        contentDescription = "Active Mode Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = when (settings.replyMode) {
                            "CUSTOM" -> "Custom Replies Only"
                            "GEMINI" -> "Gemini AI Engine"
                            else -> "Hybrid Responder (Custom -> Gemini)"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = when (settings.replyMode) {
                        "CUSTOM" -> "Matches strict phrases or keywords to reply manually."
                        "GEMINI" -> "Uses system-prompts and incoming text to draft AI answers."
                        else -> "Prioritizes keywords first, falling back to Gemini AI when no match is detected."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Statistcs Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = "Incoming messages icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Processed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalLogs Messages", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Reply,
                        contentDescription = "Success replies icon",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Column {
                        Text("Sent Success", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$successLogs Replies", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // 4. Developer Playground / Simulation Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeveloperMode,
                        contentDescription = "Developer Playground",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Local Integration Testing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "Simulate an incoming message to instantly view auto-reply logs in this application without leaving your screen.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var testSender by remember { mutableStateOf("John Doe") }
                var testMsg by remember { mutableStateOf("Hello! Are you available to chat?") }
                var simulationResponse by remember { mutableStateOf<String?>(null) }
                var isSimulating by remember { mutableStateOf(false) }

                TextField(
                    value = testSender,
                    onValueChange = { testSender = it },
                    label = { Text("Sender Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                TextField(
                    value = testMsg,
                    onValueChange = { testMsg = it },
                    label = { Text("Message Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Button(
                    onClick = {
                        isSimulating = true
                        simulationResponse = "Processing rules..."
                        scope.launch {
                            // Run the exact processing pipeline inside the service manually in a safe simulated run
                            val repliesList = viewModel.replies.value
                            val enabledReplies = repliesList.filter { it.isEnabled }
                            var matched: String? = null
                            for (r in enabledReplies) {
                                if (testMsg.contains(r.keyword, ignoreCase = true)) {
                                    matched = r.replyText
                                    break
                                }
                            }

                            var outputText = "No reply found matching current Rules/Modes."
                            var logMode = "CUSTOM (SIMULATED)"
                            var transactionSuccess = false

                            if (settings.replyMode == "CUSTOM") {
                                if (matched != null) {
                                    outputText = matched
                                    transactionSuccess = true
                                }
                            } else if (settings.replyMode == "GEMINI") {
                                logMode = "GEMINI (SIMULATED)"
                                simulationResponse = "Invoking Gemini API..."
                                val apiResult = viewModel.geminiRepository.generateReply(
                                    prompt = "Sender: $testSender\nMessage: $testMsg",
                                    systemPrompt = settings.systemPrompt,
                                    model = settings.geminiModel
                                )
                                apiResult.onSuccess { text ->
                                    outputText = text
                                    transactionSuccess = true
                                }.onFailure { e ->
                                    outputText = "Gemini Error: ${e.message}"
                                }
                            } else if (settings.replyMode == "HYBRID") {
                                if (matched != null) {
                                    outputText = matched
                                    transactionSuccess = true
                                    logMode = "CUSTOM (SIMULATED)"
                                } else {
                                    logMode = "GEMINI (SIMULATED)"
                                    simulationResponse = "Keyword failed. Invoking Gemini AI fallback..."
                                    val apiResult = viewModel.geminiRepository.generateReply(
                                        prompt = "Sender: $testSender\nMessage: $testMsg",
                                        systemPrompt = settings.systemPrompt,
                                        model = settings.geminiModel
                                    )
                                    apiResult.onSuccess { text ->
                                        outputText = text
                                        transactionSuccess = true
                                    }.onFailure { e ->
                                        outputText = "Gemini Fallback Error: ${e.message}"
                                    }
                                }
                            }

                            // Simulation delay
                            delay(500)
                            isSimulating = false
                            simulationResponse = "Simulated Reply: \"$outputText\""

                            // Register inside DB log for visual logs/statistics immediately!
                            viewModel.insertLog(
                                AutoReplyLogEntity(
                                    senderName = testSender,
                                    messageText = testMsg,
                                    replyText = outputText,
                                    mode = logMode,
                                    isSuccess = transactionSuccess
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("simulate_button"),
                    enabled = !isSimulating
                ) {
                    if (isSimulating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulating...")
                    } else {
                        Text("Inject Simulated Message")
                    }
                }

                simulationResponse?.let { res ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = res,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("simulation_result_text")
                        )
                    }
                }
            }
        }
    }
}
