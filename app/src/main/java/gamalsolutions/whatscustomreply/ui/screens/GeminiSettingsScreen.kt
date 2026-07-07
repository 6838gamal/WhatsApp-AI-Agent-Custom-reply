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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val testResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings

    val scrollState = rememberScrollState()
    var showApiKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        Column {
            Text(
                text = if (settings.appLanguage == "en") "Gemini AI Config" else "إعدادات الذكاء الاصطناعي جيمناي",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (settings.appLanguage == "en") "Configure the Gemini parameters for automatic responses" else "قم بضبط محددات وإعدادات جيمناي للردود التلقائية الذكية",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. API Key Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                    Text(
                        text = if (settings.appLanguage == "en") "Gemini API Key" else "مفتاح Gemini API",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Paste your Gemini API Key. Your key is kept secure inside client-side DataStore."
                    } else {
                        "أدخل مفتاح Gemini API الخاص بك لتشغيل الذكاء الاصطناعي والاستجابة الفورية للرسائل."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = settings.geminiApiKey,
                    onValueChange = { viewModel.updateGeminiApiKey(it) },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle API Key Visibility"
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 2. System Instructions Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                        contentDescription = "System Instructions Icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (settings.appLanguage == "en") "AI System Instructions" else "توجيهات وموجه النظام لجيمناي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Direct how the AI should behave (e.g. tone, response length, language restrictions)."
                    } else {
                        "وجه الذكاء الاصطناعي حول كيفية الرد وصياغة الكلام (مثال: الرد بشكل ودّي، بلهجة معينة، بالاختصار)."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = settings.geminiSystemInstruction,
                    onValueChange = { viewModel.updateGeminiSystemInstruction(it) },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_instructions_field"),
                    minLines = 4,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 3. Target Reply Scope Card (خيارات الرد على الدردشة الفردية أو المجموعات أو كليهما)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                        imageVector = Icons.Filled.Group,
                        contentDescription = "Target Scope Icon",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = if (settings.appLanguage == "en") "Reply Target Scope" else "نطاق تفعيل الرد والدردشة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Determine who Gemini will reply to: only individual private chats, only group chats, or both."
                    } else {
                        "حدد المستهدفين بالرد الآلي الذكي: الدردشات الفردية الخاصة فقط، المجموعات والمجالس فقط، أو كلاهما."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val scopes = listOf(
                    Triple("INDIVIDUAL", if (settings.appLanguage == "en") "Individual Chats" else "الدردشات الفردية فقط", Icons.Default.Person),
                    Triple("GROUPS", if (settings.appLanguage == "en") "Group Chats" else "المجموعات فقط", Icons.Default.Groups),
                    Triple("BOTH", if (settings.appLanguage == "en") "Both Chats & Groups" else "كلاهما (الفردية والمجموعات)", Icons.Default.AllInclusive)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scopes.forEach { (id, label, icon) ->
                        val isSelected = settings.replyTargetScope == id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateReplyTargetScope(id) }
                                .testTag("scope_option_$id"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateReplyTargetScope(id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Test Gemini connection Widget
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Diagnostics icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (settings.appLanguage == "en") "Test Gemini Intelligence" else "فحص استجابة جيمناي للدردشة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Test your API Key connection. The system will send a secure ping to Gemini with your custom system instructions."
                    } else {
                        "قم بإجراء محاكاة لربط مفتاح API. سيرسل النظام إشارة تحقق سريعة ومقيدة بالتعليمات للتأكد من دقة الردود وصحة المفتاح."
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
                            .testTag("test_gemini_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (settings.appLanguage == "en") "Querying..." else "جاري التحقق واستخراج الرد...")
                        } else {
                            Text(if (settings.appLanguage == "en") "Test Connectivity" else "بدء فحص الربط")
                        }
                    }

                    if (testResult != null) {
                        TextButton(
                            onClick = { viewModel.resetConnectionTestResult() },
                            modifier = Modifier.testTag("clear_test_gemini_result")
                        ) {
                            Text(if (settings.appLanguage == "en") "Clear" else "مسح")
                        }
                    }
                }

                testResult?.let { res ->
                    val isSuccess = res.startsWith("Success") || res.startsWith("success") || res.contains("نجاح") || res.contains("متصل")
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
                            text = res,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("test_gemini_result_label")
                        )
                    }
                }
            }
        }

        // 5. Chat Sandbox Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            val coroutineScope = rememberCoroutineScope()
            val sandboxMessages = remember { mutableStateListOf<SandboxMessage>() }
            val chatListScrollState = rememberScrollState()
            var inputText by remember { mutableStateOf("") }
            var isSendingMessage by remember { mutableStateOf(false) }

            LaunchedEffect(settings.appLanguage) {
                if (sandboxMessages.isEmpty()) {
                    sandboxMessages.add(
                        SandboxMessage(
                            sender = "AI",
                            text = if (settings.appLanguage == "en") {
                                "Hello! I am Gemini. Send me any message here to test my responses and system instructions configuration in real-time."
                            } else {
                                "مرحباً! أنا المساعد الذكي جيمناي. يمكنك كتابة أي رسالة وتجربتي هنا مباشرة لفحص دقة صياغة الردود ومناسبتها للتوجيهات."
                            }
                        )
                    )
                }
            }

            LaunchedEffect(sandboxMessages.size) {
                if (sandboxMessages.isNotEmpty()) {
                    chatListScrollState.animateScrollTo(chatListScrollState.maxValue)
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forum,
                            contentDescription = "Chat Sandbox Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (settings.appLanguage == "en") "Gemini Chat Sandbox" else "محاكي الدردشة التجريبي لجيمناي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    if (sandboxMessages.size > 1) {
                        IconButton(
                            onClick = {
                                sandboxMessages.clear()
                                sandboxMessages.add(
                                    SandboxMessage(
                                        sender = "AI",
                                        text = if (settings.appLanguage == "en") {
                                            "Hello! I am Gemini. Send me any message here to test my responses and system instructions configuration in real-time."
                                        } else {
                                            "مرحباً! أنا المساعد الذكي جيمناي. يمكنك كتابة أي رسالة وتجربتي هنا مباشرة لفحص دقة صياغة الردود ومناسبتها للتوجيهات."
                                        }
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Clear Chat History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Simulate a private chat conversation with Gemini to see exactly how it processes your input and rules."
                    } else {
                        "قم بإجراء محادثة تجريبية مباشرة مع جيمناي لمشاهدة واختبار التجاوب الفعلي ومطابقة القواعد."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Chat Messages Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(chatListScrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sandboxMessages.forEach { msg ->
                            val isUser = msg.sender == "USER"
                            val isError = msg.isError

                            val alignment = if (isUser) Alignment.End else Alignment.Start
                            val bubbleColor = when {
                                isUser -> MaterialTheme.colorScheme.primary
                                isError -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                            val textColor = when {
                                isUser -> MaterialTheme.colorScheme.onPrimary
                                isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                    shape = if (isUser) {
                                        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
                                    } else {
                                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
                                    },
                                    modifier = Modifier.widthIn(max = 240.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (isError) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Error,
                                                    contentDescription = "Error Icon",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (settings.appLanguage == "en") "Configuration Error" else "خطأ في الاتصال والتهيئة",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = textColor
                                        )
                                    }
                                }
                                Text(
                                    text = if (isUser) (if (settings.appLanguage == "en") "You" else "أنت")
                                           else if (isError) (if (settings.appLanguage == "en") "System" else "النظام")
                                           else (if (settings.appLanguage == "en") "Gemini" else "جيمناي"),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isSendingMessage) {
                            // Typing indicator
                            Row(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (settings.appLanguage == "en") "Gemini is replying..." else "جيمناي يكتب الرد الآن...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Chat Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (settings.appLanguage == "en") {
                                    if (settings.geminiApiKey.isBlank()) "Please configure API Key first" else "Type message to test..."
                                } else {
                                    if (settings.geminiApiKey.isBlank()) "يرجى تهيئة مفتاح API أولاً" else "اكتب رسالة تجريبية هنا..."
                                },
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sandbox_chat_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        enabled = !isSendingMessage && settings.geminiApiKey.isNotBlank()
                    )

                    IconButton(
                        onClick = {
                            val msg = inputText.trim()
                            if (msg.isNotEmpty()) {
                                inputText = ""
                                sandboxMessages.add(SandboxMessage(sender = "USER", text = msg))
                                isSendingMessage = true
                                coroutineScope.launch {
                                    val result = viewModel.geminiRepository.generateReply(
                                        apiKey = settings.geminiApiKey,
                                        systemInstruction = settings.geminiSystemInstruction,
                                        message = msg
                                    )
                                    isSendingMessage = false
                                    result.onSuccess { reply ->
                                        sandboxMessages.add(SandboxMessage(sender = "AI", text = reply))
                                    }.onFailure { error ->
                                        sandboxMessages.add(
                                            SandboxMessage(
                                                sender = "ERROR",
                                                text = error.message ?: "Unknown error occurred.",
                                                isError = true
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !isSendingMessage && inputText.isNotBlank() && settings.geminiApiKey.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send Test Message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

data class SandboxMessage(
    val sender: String,
    val text: String,
    val isError: Boolean = false
)
