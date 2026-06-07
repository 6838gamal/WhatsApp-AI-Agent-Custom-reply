package gamalsolutions.whatscustomreply.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

            // 2. Horizontal Contact filter pills Row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (settings.appLanguage == "en") "Filter by WhatsApp Contact:" else "تصفية حسب جهة الاتصال:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
                
                androidx.compose.foundation.lazy.LazyRow(
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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { editingReply = reply },
                                    onLongClick = { editingReply = reply }
                                )
                                .testTag("reply_card_${reply.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Keyword Tag Display
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (reply.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${labels.containsTag}: ${reply.keyword}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (reply.isEnabled) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        // Contact Tag Display
                                        if (!reply.contactName.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (reply.isEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${labels.contactSpecificOnly}${reply.contactName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (reply.isEnabled) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = labels.appliesToAll,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = reply.replyText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (reply.isEnabled) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = reply.isEnabled,
                                    onCheckedChange = { viewModel.toggleReplyCode(reply, it) },
                                    modifier = Modifier.testTag("reply_toggle_${reply.id}")
                                )
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
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                viewModel.clearPrefilledContact()
            },
            title = { Text(labels.addRuleTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = labels.addRuleSubtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text(labels.matchKeywordLabel) },
                        placeholder = { Text(labels.matchKeywordPlaceholder) },
                        modifier = Modifier.fillMaxWidth().testTag("add_keyword_input"),
                        singleLine = true
                    )
                    TextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text(labels.contactNameLabel) },
                        placeholder = { Text(labels.contactNamePlaceholder) },
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_input"),
                        singleLine = true
                    )
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text(labels.replyTextLabel) },
                        placeholder = { Text(labels.replyTextPlaceholder) },
                        modifier = Modifier.fillMaxWidth().testTag("add_reply_input"),
                        minLines = 3
                    )
                    if (isError) {
                        Text(
                            text = labels.fieldsRequiredError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (keyword.isBlank() || replyText.isBlank()) {
                            isError = true
                        } else {
                            viewModel.addReply(
                                keyword = keyword.trim(),
                                replyText = replyText.trim(),
                                contactName = contactName.trim().ifEmpty { null }
                            )
                            showAddDialog = false
                            viewModel.clearPrefilledContact()
                        }
                    },
                    modifier = Modifier.testTag("save_new_reply_button")
                ) {
                    Text(labels.saveRuleBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    viewModel.clearPrefilledContact()
                }) {
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
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingReply = null },
            title = { Text(labels.editRuleTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text(labels.matchKeywordLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_keyword_input"),
                        singleLine = true
                    )
                    TextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text(labels.contactNameLabel) },
                        placeholder = { Text(labels.contactNamePlaceholder) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_contact_input"),
                        singleLine = true
                    )
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text(labels.replyTextLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_reply_input"),
                        minLines = 3
                    )
                    if (isError) {
                        Text(
                            text = labels.fieldsRequiredError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Delete button styled in error color red
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

                    Row {
                        TextButton(onClick = { editingReply = null }) {
                            Text(labels.cancelBtn)
                        }
                        TextButton(
                            onClick = {
                                if (keyword.isBlank() || replyText.isBlank()) {
                                    isError = true
                                } else {
                                    viewModel.updateReply(
                                        reply.copy(
                                            keyword = keyword.trim(),
                                            replyText = replyText.trim(),
                                            contactName = contactName.trim().ifEmpty { null }
                                        )
                                    )
                                    editingReply = null
                                }
                            },
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
