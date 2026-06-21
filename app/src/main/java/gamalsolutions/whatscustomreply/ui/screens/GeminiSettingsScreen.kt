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
    val testResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings

    val scrollState = rememberScrollState()

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

        // 1. API endpoint URL Config Card
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
                        imageVector = Icons.Filled.Language,
                        contentDescription = "API url icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(labels.apiUrlLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Enter the full routing path of your API (Webhook / REST Endpoint) which will accept incoming messages and calculate a response."
                    } else {
                        "أدخل الرابط الكامل لقناة الاتصال (واجهة API أو Webhook) التي ستستقبل الرسائل الواردة لتوليد الرد الآلي."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = settings.apiUrl,
                    onValueChange = { viewModel.updateApiUrl(it) },
                    placeholder = { Text("https://api.example.com/reply") },
                    modifier = Modifier.fillMaxWidth().testTag("api_url_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 2. Request Method Config Card
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
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "HTTP method icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(labels.apiMethodLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val methods = listOf("POST", "GET")
                    methods.forEach { method ->
                        val isSelected = settings.apiMethod == method
                        Button(
                            onClick = { viewModel.updateApiMethod(method) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("method_${method.lowercase()}")
                        ) {
                            Text(method, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Custom Headers Card
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
                        imageVector = Icons.Filled.ListAlt,
                        contentDescription = "Headers Icon",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(labels.apiHeadersLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Paste HTTP custom query headers below, with exactly one 'HeaderName: Value' pair on each individual line."
                    } else {
                        "أضف ترويسات الطلب المخصصة بالأسفل، مع تدوين زوج 'اسم الترويسة: القيمة' في كل سطر بشكل مستقل."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = settings.apiHeaders,
                    onValueChange = { viewModel.updateApiHeaders(it) },
                    placeholder = { Text("Content-Type: application/json\nAuthorization: Bearer my-api-token") },
                    modifier = Modifier.fillMaxWidth().testTag("api_headers_field"),
                    minLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 4. Request Body JSON Template (Supported for POST method only)
        AnimatedVisibility(
            visible = settings.apiMethod == "POST",
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
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
                            imageVector = Icons.Filled.Code,
                            contentDescription = "Body Template Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(labels.apiBodyTemplateLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Text(
                        text = if (settings.appLanguage == "en") {
                            "Customize the payload JSON body template. Use placeholders {sender} and {message} to inject values automatically."
                        } else {
                            "خصّص بنية جسم طلب الإرسال JSON المتناقل. استخدم الرموز البديلة {sender} لجهة الاتصال و {message} لنص الرسالة المستلمة ليتم تعويضهما آلياً."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextField(
                        value = settings.apiBodyTemplate,
                        onValueChange = { viewModel.updateApiBodyTemplate(it) },
                        modifier = Modifier.fillMaxWidth().testTag("api_body_template_field"),
                        minLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // 5. JSON response path parameter field
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
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Response path icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(labels.apiResponsePathLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Specify the object key or dot-notation nested path mapping of response (e.g., 'reply' or 'data.text'). Leave empty to use the raw reply content directly."
                    } else {
                        "عيّن اسم الحقل البرمجي المستهدف في الاستجابة (مثل 'reply' أو 'text.reply'). اتركه فارغًا لاستخدام نص الاستجابة بالكامل كرسالة رد."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextField(
                    value = settings.apiResponsePath,
                    onValueChange = { viewModel.updateApiResponsePath(it) },
                    placeholder = { Text("reply") },
                    modifier = Modifier.fillMaxWidth().testTag("api_response_path_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // 6. Test custom API connection Widget
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
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Diagnostics icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = labels.testConnectionBtn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = if (settings.appLanguage == "en") {
                        "Execute a live connection test to verify that your custom API endpoint properly authenticates and resolves the message response."
                    } else {
                        "أجرِ فحص فوري ومحبّك للتأكد من ربط واجهة البيانات واستجابتها لتوليد نصوص الردود تلقائياً بكفاءة."
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
                        onClick = { viewModel.testApiConnection() },
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_custom_api_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (settings.appLanguage == "en") "Querying..." else "جاري الاستدعاء والفحص...")
                        } else {
                            Text(labels.testConnectionBtn)
                        }
                    }

                    if (testResult != null) {
                        TextButton(
                            onClick = { viewModel.resetConnectionTestResult() },
                            modifier = Modifier.testTag("clear_test_api_result")
                        ) {
                            Text(if (settings.appLanguage == "en") "Clear" else "مسح")
                        }
                    }
                }

                testResult?.let { res ->
                    val isSuccess = res.startsWith("Success") || res.startsWith("success") || res.contains("نجاح") || res.contains("متصل")
                    val labelText = if (isSuccess) {
                        if (settings.appLanguage == "en") "Success: Connected with API!" else "تم بنجاح! الاتصال واستخراج الردود البرمجية يعمل بكفاءة."
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
                            modifier = Modifier.testTag("test_custom_api_result_label")
                        )
                    }
                }
            }
        }
    }
}
