package gamalsolutions.whatscustomreply.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamalsolutions.whatscustomreply.data.database.CustomReplyEntity
import gamalsolutions.whatscustomreply.ui.ArStrings
import gamalsolutions.whatscustomreply.ui.EnStrings
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RepliesScreen(
    viewModel: MainViewModel
) {
    val replies by viewModel.replies.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prefilledContact by viewModel.prefilledContact.collectAsStateWithLifecycle()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings

    var showAddDialog by remember { mutableStateOf(false) }
    var editingReply by remember { mutableStateOf<CustomReplyEntity?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedContactFilter by remember { mutableStateOf<String?>("ALL") } // "ALL", "GLOBAL", or specific contact name

    // Trigger add dialog automatically if a contact has been pre-selected from direct shortcut
    LaunchedEffect(prefilledContact) {
        if (prefilledContact != null) {
            showAddDialog = true
        }
    }

    // Identify unique contacts that currently have active custom rules
    val uniqueContacts = remember(replies) {
        replies.map { r -> r.contactName?.trim() ?: "" }
            .filter { it.isNotBlank() }
            .distinct()
    }

    // Reactive filtered replies list
    val filteredReplies = remember(replies, selectedContactFilter, searchQuery) {
        replies.filter { reply ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                reply.keyword.contains(searchQuery, ignoreCase = true) ||
                (reply.contactName?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (reply.targetAccount?.contains(searchQuery, ignoreCase = true) ?: false) ||
                reply.replyText.contains(searchQuery, ignoreCase = true)
            }
            
            val matchesContact = when (selectedContactFilter) {
                "ALL" -> true
                "GLOBAL" -> reply.contactName.isNullOrBlank()
                else -> reply.contactName?.trim()?.equals(selectedContactFilter?.trim(), ignoreCase = true) == true
            }
            
            matchesSearch && matchesContact
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_reply_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Keyword Reply")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = labels.customRepliesHeader,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = labels.customRepliesDesc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(labels.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_custom_replies"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // Direct Interactive Voice Assistant Quick Settings Card (Avoids fragmentation / tish-shatut)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isExpanded = !isExpanded }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Voice Assistant Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (settings.appLanguage == "en") "Interactive Voice Call Assistant" else "المجيب الصوتي التفاعلي للمكالمات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (settings.interactiveVoiceCallEnabled) {
                                        if (settings.appLanguage == "en") "Status: Active (Answering Calls)" else "الحالة: نشط (المجيب الصوتي يعمل عند ردك على المكالمة)"
                                    } else {
                                        if (settings.appLanguage == "en") "Status: Off" else "الحالة: معطّل (انقر للتنشيط)"
                                    },
                                    fontSize = 11.sp,
                                    color = if (settings.interactiveVoiceCallEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.interactiveVoiceCallEnabled,
                                onCheckedChange = { viewModel.updateInteractiveVoiceCallEnabled(it) },
                                modifier = Modifier.scale(0.85f).testTag("quick_voice_switch")
                            )
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand Config"
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                            
                            Text(
                                text = if (settings.appLanguage == "en") "Starting Welcome Greeting prompt:" else "النص الترحيبي التلقائي للبدء (عند الرد):",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            OutlinedTextField(
                                value = settings.interactiveVoiceCallPrompt,
                                onValueChange = { viewModel.updateInteractiveVoiceCallPrompt(it) },
                                modifier = Modifier.fillMaxWidth().testTag("quick_voice_prompt_field"),
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "info",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                    )
                                    Text(
                                        text = if (settings.appLanguage == "en") {
                                            "How it works: When a call is answered, the assistant speaks this greeting, then listens to the caller and matches their speech against your active rules listed below!"
                                        } else {
                                            "آلية العمل: عند فتح الخط والرد، ينطق المجيب بهذا النص الترحيبي، ثم يستمع للمتصل ويطابق كلامه مع كلماتك المفتاحية والقواعد المحددة في الأسفل للرد عليه بنص أو رسالة صوتية تلقائياً!"
                                        },
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Horizontal Contact filter pills Row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (settings.appLanguage == "en") "Filter by WhatsApp Contact:" else "تصفية حسب جهة الاتصال:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    // ALL Filter
                    item {
                        FilterChip(
                            selected = selectedContactFilter == "ALL",
                            onClick = { selectedContactFilter = "ALL" },
                            label = { Text(labels.filterAll) },
                            leadingIcon = {
                                if (selectedContactFilter == "ALL") {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }

                    // GLOBAL (Generic Rules) Filter
                    item {
                        FilterChip(
                            selected = selectedContactFilter == "GLOBAL",
                            onClick = { selectedContactFilter = "GLOBAL" },
                            label = { Text(labels.filterGlobal) },
                            leadingIcon = {
                                if (selectedContactFilter == "GLOBAL") {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }

                    // Dynamic Contacts Filter Chips
                    items(uniqueContacts) { contact ->
                        FilterChip(
                            selected = selectedContactFilter == contact,
                            onClick = { selectedContactFilter = contact },
                            label = { Text(contact) },
                            leadingIcon = {
                                if (selectedContactFilter == contact) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Results Listing or Empty State
            if (replies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("empty_replies_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = "Empty Custom Replies",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = labels.noRepliesConfigured,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = labels.noRepliesRecommendation,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("empty_state_add_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Icon")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(labels.createFirstRule)
                        }
                    }
                }
            } else if (filteredReplies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("empty_filtered_replies_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "No Results Matches",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = if (settings.appLanguage == "en") "No matches found" else "لا توجد نتائج مطابقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (selectedContactFilter != "ALL" && selectedContactFilter != "GLOBAL") {
                                labels.noRepliesForSelectedContact
                            } else {
                                if (settings.appLanguage == "en") {
                                    "No custom rules match your filter or keyword query above."
                                } else {
                                    "لا توجد قواعد رد مخصصة تطابق البحث وتصفية جهات الاتصال المختارة."
                                }
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredReplies, key = { it.id }) { reply ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (reply.isEnabled) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingReply = reply },
                                    onLongClick = { editingReply = reply }
                                )
                                .testTag("reply_card_${reply.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. TOP ROW: Trigger Icon, Keyword Display, and Enable Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Specific trigger icon
                                        val triggerIcon = when (reply.triggerType) {
                                            "CALL_ACTIVE" -> Icons.Default.PhoneInTalk
                                            "CALL_MISSED" -> Icons.Default.PhoneCallback
                                            else -> Icons.Default.ChatBubble
                                        }
                                        val triggerColor = when (reply.triggerType) {
                                            "CALL_ACTIVE" -> MaterialTheme.colorScheme.tertiary
                                            "CALL_MISSED" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(triggerColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = triggerIcon,
                                                contentDescription = null,
                                                tint = triggerColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = if (settings.appLanguage == "en") "If message/call matches:" else "إذا احتوى كلام المرسل/الرنين على:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                            )
                                            Text(
                                                text = "\"${reply.keyword}\"",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (reply.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = reply.isEnabled,
                                        onCheckedChange = { viewModel.toggleReplyCode(reply, it) },
                                        modifier = Modifier.testTag("reply_toggle_${reply.id}").scale(0.85f)
                                    )
                                }

                                // 2. MIDDLE BUBBLE: Clear shaded container for the Reply Text content
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (reply.replyType == "VOICE") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        val replyIcon = if (reply.replyType == "VOICE") Icons.Default.RecordVoiceOver else Icons.Default.ChatBubbleOutline
                                        val replyColor = if (reply.replyType == "VOICE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        
                                        Icon(
                                            imageVector = replyIcon,
                                            contentDescription = null,
                                            tint = replyColor,
                                            modifier = Modifier.size(18.dp).padding(top = 1.dp)
                                        )
                                        
                                        Column {
                                            Text(
                                                text = if (reply.replyType == "VOICE") {
                                                    if (settings.appLanguage == "en") "🎙️ Spoken Voice Reply" else "🎙️ رد مسموع (بصوت المساعد)"
                                                } else {
                                                    if (settings.appLanguage == "en") "📝 Written Text Reply" else "📝 رد تلقائي نصي"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = replyColor
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = reply.replyText,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (reply.isEnabled) MaterialTheme.colorScheme.onSurface 
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                // 3. BOTTOM ROW: Filtering Metadata Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Trigger Context badge
                                    val triggerLabel = when (reply.triggerType) {
                                        "CALL_ACTIVE" -> if (settings.appLanguage == "en") "Call Active" else "رنين المكالمة"
                                        "CALL_MISSED" -> if (settings.appLanguage == "en") "Missed Call" else "مكالمة فائتة"
                                        else -> if (settings.appLanguage == "en") "Chat Message" else "رسالة دردشة"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = triggerLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Contacts filter badge
                                    if (!reply.contactName.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = reply.contactName,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (settings.appLanguage == "en") "All Contacts" else "جميع السندرالمرسلين",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    // Target Account phone number match context
                                    if (!reply.targetAccount.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = reply.targetAccount,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Reply Dialog ---
    if (showAddDialog) {
        var keyword by remember { mutableStateOf("") }
        var replyText by remember { mutableStateOf("") }
        var contactName by remember { mutableStateOf(prefilledContact ?: "") }
        var triggerType by remember { mutableStateOf("CHAT") } // "CHAT", "CALL_ACTIVE", "CALL_MISSED"
        var replyType by remember { mutableStateOf("TEXT") } // "TEXT", "VOICE"
        var targetAccount by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                viewModel.clearPrefilledContact()
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = labels.addRuleTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = labels.addRuleSubtitle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // SECTION 1: TRIGGER CONDITION
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (settings.appLanguage == "en") "1. Trigger Condition & Keyword" else "1. شرط التشغيل والكلمة المفتاحية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text(labels.matchKeywordLabel) },
                            placeholder = { Text(labels.matchKeywordPlaceholder) },
                            modifier = Modifier.fillMaxWidth().testTag("add_keyword_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = labels.ruleTriggerTypeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = triggerType == "CHAT",
                                onClick = { triggerType = "CHAT" },
                                label = { Text(labels.triggerChat) }
                            )
                            FilterChip(
                                selected = triggerType == "CALL_ACTIVE",
                                onClick = { triggerType = "CALL_ACTIVE" },
                                label = { Text(labels.triggerCallActive) }
                            )
                            FilterChip(
                                selected = triggerType == "CALL_MISSED",
                                onClick = { triggerType = "CALL_MISSED" },
                                label = { Text(labels.triggerCallMissed) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // SECTION 2: RESPONSE CONTENT
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (settings.appLanguage == "en") "2. Automated Response" else "2. صيغة وقناة الرد التلقائي",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Text(
                            text = labels.ruleReplyTypeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = replyType == "TEXT",
                                onClick = { replyType = "TEXT" },
                                label = { Text(labels.replyTypeText) }
                            )
                            FilterChip(
                                selected = replyType == "VOICE",
                                onClick = { replyType = "VOICE" },
                                label = { Text(labels.replyTypeVoice) }
                            )
                        }

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text(labels.replyTextLabel) },
                            placeholder = { Text(labels.replyTextPlaceholder) },
                            modifier = Modifier.fillMaxWidth().testTag("add_reply_input"),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // SECTION 3: ADVANCED OPTIONAL FILTERS
                    var showOptionalFilters by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOptionalFilters = !showOptionalFilters }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (settings.appLanguage == "en") "3. Advanced Filters (Optional)" else "3. فلاتر التصفية المتقدمة (اختياري)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Icon(
                                imageVector = if (showOptionalFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showOptionalFilters,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = contactName,
                                    onValueChange = { contactName = it },
                                    label = { Text(labels.contactNameLabel) },
                                    placeholder = { Text(labels.contactNamePlaceholder) },
                                    modifier = Modifier.fillMaxWidth().testTag("add_contact_input"),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = targetAccount,
                                    onValueChange = { targetAccount = it },
                                    label = { Text(labels.ruleTargetAccountLabel) },
                                    placeholder = { Text(labels.ruleTargetAccountDesc) },
                                    modifier = Modifier.fillMaxWidth().testTag("add_target_account_input"),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    if (isError) {
                        Text(
                            text = labels.fieldsRequiredError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyword.isBlank() || replyText.isBlank()) {
                            isError = true
                        } else {
                            viewModel.addReply(
                                keyword = keyword.trim(),
                                replyText = replyText.trim(),
                                contactName = contactName.trim().ifEmpty { null },
                                triggerType = triggerType,
                                replyType = replyType,
                                targetAccount = targetAccount.trim().ifEmpty { null }
                            )
                            showAddDialog = false
                            viewModel.clearPrefilledContact()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_new_reply_button")
                ) {
                    Text(labels.saveRuleBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddDialog = false
                        viewModel.clearPrefilledContact()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(labels.cancelBtn)
                }
            }
        )
    }

    // --- Edit / Delete Dialog ---
    editingReply?.let { reply ->
        var keyword by remember { mutableStateOf(reply.keyword) }
        var replyText by remember { mutableStateOf(reply.replyText) }
        var contactName by remember { mutableStateOf(reply.contactName ?: "") }
        var triggerType by remember { mutableStateOf(reply.triggerType) }
        var replyType by remember { mutableStateOf(reply.replyType) }
        var targetAccount by remember { mutableStateOf(reply.targetAccount ?: "") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingReply = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = labels.editRuleTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp)
                ) {
                    // SECTION 1: TRIGGER CONDITION
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (settings.appLanguage == "en") "1. Trigger Condition & Keyword" else "1. شرط التشغيل والكلمة المفتاحية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text(labels.matchKeywordLabel) },
                            modifier = Modifier.fillMaxWidth().testTag("edit_keyword_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = labels.ruleTriggerTypeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = triggerType == "CHAT",
                                onClick = { triggerType = "CHAT" },
                                label = { Text(labels.triggerChat) }
                            )
                            FilterChip(
                                selected = triggerType == "CALL_ACTIVE",
                                onClick = { triggerType = "CALL_ACTIVE" },
                                label = { Text(labels.triggerCallActive) }
                            )
                            FilterChip(
                                selected = triggerType == "CALL_MISSED",
                                onClick = { triggerType = "CALL_MISSED" },
                                label = { Text(labels.triggerCallMissed) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // SECTION 2: RESPONSE CONTENT
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (settings.appLanguage == "en") "2. Automated Response" else "2. صيغة وقناة الرد التلقائي",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Text(
                            text = labels.ruleReplyTypeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = replyType == "TEXT",
                                onClick = { replyType = "TEXT" },
                                label = { Text(labels.replyTypeText) }
                            )
                            FilterChip(
                                selected = replyType == "VOICE",
                                onClick = { replyType = "VOICE" },
                                label = { Text(labels.replyTypeVoice) }
                            )
                        }

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text(labels.replyTextLabel) },
                            modifier = Modifier.fillMaxWidth().testTag("edit_reply_input"),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // SECTION 3: ADVANCED OPTIONAL FILTERS
                    var showOptionalFilters by remember { mutableStateOf(!contactName.isEmpty() || !targetAccount.isEmpty()) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOptionalFilters = !showOptionalFilters }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (settings.appLanguage == "en") "3. Advanced Filters (Optional)" else "3. فلاتر التصفية المتقدمة (اختياري)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Icon(
                                imageVector = if (showOptionalFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showOptionalFilters,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = contactName,
                                    onValueChange = { contactName = it },
                                    label = { Text(labels.contactNameLabel) },
                                    placeholder = { Text(labels.contactNamePlaceholder) },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_contact_input"),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = targetAccount,
                                    onValueChange = { targetAccount = it },
                                    label = { Text(labels.ruleTargetAccountLabel) },
                                    placeholder = { Text(labels.ruleTargetAccountDesc) },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_target_account_input"),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    if (isError) {
                        Text(
                            text = labels.fieldsRequiredError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Button styled in error color red
                    TextButton(
                        onClick = {
                            viewModel.deleteReply(reply)
                            editingReply = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_reply_button")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Icon")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(labels.deleteRuleBtn)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { editingReply = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(labels.cancelBtn)
                        }
                        Button(
                            onClick = {
                                if (keyword.isBlank() || replyText.isBlank()) {
                                    isError = true
                                } else {
                                    viewModel.updateReply(
                                        reply.copy(
                                            keyword = keyword.trim(),
                                            replyText = replyText.trim(),
                                            contactName = contactName.trim().ifEmpty { null },
                                            triggerType = triggerType,
                                            replyType = replyType,
                                            targetAccount = targetAccount.trim().ifEmpty { null }
                                        )
                                    )
                                    editingReply = null
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_edit_reply_button")
                        ) {
                            Text(labels.saveBtn)
                        }
                    }
                }
            }
        )
    }
}
