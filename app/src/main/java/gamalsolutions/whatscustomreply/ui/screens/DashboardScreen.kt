package gamalsolutions.whatscustomreply.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import gamalsolutions.whatscustomreply.ui.ArStrings
import gamalsolutions.whatscustomreply.ui.EnStrings
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel

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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val totalLogs by viewModel.totalLogCount.collectAsStateWithLifecycle()
    val successLogs by viewModel.successLogCount.collectAsStateWithLifecycle()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings

    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }

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
        // App Hero Banner
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
                    text = if (settings.appLanguage == "en") "Gemini Auto-Responder" else "مجيب الجيمناي الآلي للواتساب",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (settings.appLanguage == "en") "Automated smart replies powered by Gemini 2.5 Flash" else "ردود ذكية تلقائية مدعومة بالكامل بنموذج Gemini 2.5 Flash",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }

        // Notification Permission Warning
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
                            text = labels.notificationAccessHeader,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = labels.notificationAccessBody,
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
                        Text(labels.grantPermission, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Master Switch Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            text = if (settings.appLanguage == "en") "Auto-Reply Status" else "حالة الرد التلقائي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (settings.isServiceEnabled) (if (settings.appLanguage == "en") "Service is active and responding" else "الخدمة نشطة وتستقبل الرسائل")
                                   else (if (settings.appLanguage == "en") "Service is paused" else "الخدمة متوقفة مؤقتاً"),
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

        // Statistics Section
        Text(
            text = if (settings.appLanguage == "en") "Response Statistics" else "إحصائيات الاستجابة للذكاء الاصطناعي",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Replies Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = "$totalLogs", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = if (settings.appLanguage == "en") "Total Chats" else "إجمالي المحادثات",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Success Rate Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                val rate = if (totalLogs > 0) (successLogs * 100) / totalLogs else 0
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(text = "$rate%", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = if (settings.appLanguage == "en") "Success Rate" else "نسبة النجاح",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Shortcuts Section
        Text(
            text = if (settings.appLanguage == "en") "Quick Access" else "الوصول السريع",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gemini Settings shortcut
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToGemini() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (settings.appLanguage == "en") "Gemini Config" else "ضبط الجيمناي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (settings.appLanguage == "en") "Set API key, instructions, & reply scope" else "تحديد المفتاح، تعليمات الرد، ونطاق المجموعات والأفراد",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // System Logs shortcut
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToSettings() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = if (settings.appLanguage == "en") "Response Logs" else "سجلات الردود",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (settings.appLanguage == "en") "View message exchange history" else "استعراض سجل استجابة الذكاء الاصطناعي للرسائل الواردة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
