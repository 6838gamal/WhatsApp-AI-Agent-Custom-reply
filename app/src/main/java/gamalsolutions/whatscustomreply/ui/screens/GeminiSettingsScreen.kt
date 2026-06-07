package gamalsolutions.whatscustomreply.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamalsolutions.whatscustomreply.ui.ArStrings
import gamalsolutions.whatscustomreply.ui.EnStrings
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val testResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings

    var showKey by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = labels.geminiEngineHeader,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = labels.geminiEngineDesc,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Api Key Entry
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = "API key icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(labels.apiKeyLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "If left empty, the system will fallback to our default trial project key, which might have rate limit boundaries. Save your own key below for infinite requests."
                    } else {
                        "إذا تم ترك هذا الحقل فارغاً، فسيستخدم الاسترداد التلقائي مفتاحنا الافتراضي المشترك، والذي قد تطبق عليه شروط الاستهلاك ومعدل تكرار الطلبات. احفظ مفتاح الخاص بك لتجربة غير محدودة."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = apiKey,
                    onValueChange = { viewModel.updateGeminiApiKey(it) },
                    placeholder = { Text("AIzaSy...") },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle API Key Visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 2. Model Selection
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Tune Model",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(labels.selectModel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Pick the generation engine path. Flash models are highly responsive for real-time messaging, with minimal latency."
                    } else {
                        "اختر إصدار نموذج التوليد التلقائي للرد الحواري الذكي. نماذج Flash خفيفة للغاية وسريعة الاستجابة بأقل مهلة زمنية ممكّنة."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Render customized Radio list for clean Material M3 selection
                val modelsList = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.1-pro-preview")
                modelsList.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (settings.geminiModel == m) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .padding(8.dp)
                            .testTag("model_option_$m")
                            .clickable { viewModel.updateGeminiModel(m) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.geminiModel == m,
                            onClick = { viewModel.updateGeminiModel(m) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (m) {
                                    "gemini-2.5-flash" -> "Gemini 2.5 Flash (Default / الافتراضي)"
                                    "gemini-3.5-flash" -> "Gemini 3.5 Flash (Recommended / موصى به)"
                                    else -> "Gemini 3.1 Pro (Heavy Reasoning / تفكير عميق)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = m,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. System Prompt Config
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = "System Instructions prompt icon",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(labels.customSystemPrompt, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = labels.customSystemPromptDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = settings.systemPrompt,
                    onValueChange = { viewModel.updateSystemPrompt(it) },
                    modifier = Modifier.fillMaxWidth().testTag("system_prompt_field"),
                    minLines = 4,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 4. Test API Connection Widget
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
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
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Diagnostics icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (settings.appLanguage == "en") "Diagnostic API Connection" else "فحص وتشخيص الاتصال",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Run a live ping test against the Google AI servers using the stored API key to guarantee everything is registered and ready."
                    } else {
                        "أجرِ فحصًا حيًّا بالاتصال بخوادم الذكاء الاصطناعي من جوجل للتحقق من فاعلية وصحة مفتاح API الخاص بك وصلاحيته للعمل مباشرة."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.testGeminiConnection() },
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_api_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (settings.appLanguage == "en") "Pinging..." else "جاري الربط والفحص...")
                        } else {
                            Text(labels.testConnectionBtn)
                        }
                    }

                    if (testResult != null) {
                        TextButton(
                            onClick = { viewModel.resetConnectionTestResult() },
                            modifier = Modifier.testTag("clear_test_result_button")
                        ) {
                            Text(if (settings.appLanguage == "en") "Clear" else "مسح")
                        }
                    }
                }

                testResult?.let { res ->
                    val isSuccess = res.startsWith("Success") || res.startsWith("success") || res.contains("نجاح") || res.contains("متصل")
                    val labelText = if (isSuccess) {
                        if (settings.appLanguage == "en") "Success: Connected with Google Servers!" else "تم بنجاح! الاتصال بخوادم جوجل يعمل بكفاءة."
                    } else {
                        res
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSuccess) Color(0xFF1B5E20).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("test_api_result_label")
                        )
                    }
                }
            }
        }
    }
}
