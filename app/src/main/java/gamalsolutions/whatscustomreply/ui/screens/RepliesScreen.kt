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
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RepliesScreen(
    viewModel: MainViewModel
) {
    val replies by viewModel.replies.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingReply by remember { mutableStateOf<CustomReplyEntity?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Custom Replies",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Configure matching text terms to auto-respond with custom templates.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                            text = "No custom replies configured",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Add phrases like 'price', 'address', or 'help' so the auto-responder can handle conversations without your intervention.",
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
                            Text("Create First Rule")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(replies, key = { it.id }) { reply ->
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
                                            text = "Contains: ${reply.keyword}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (reply.isEnabled) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                        )
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
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Reply") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Match any incoming WhatsApp text containing this phrase to auto-respond instantly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Keyword Phrase") },
                        placeholder = { Text("e.g. price") },
                        modifier = Modifier.fillMaxWidth().testTag("add_keyword_input"),
                        singleLine = true
                    )
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Auto-Reply Text") },
                        placeholder = { Text("e.g. Our basic plan tier is $20/month.") },
                        modifier = Modifier.fillMaxWidth().testTag("add_reply_input"),
                        minLines = 3
                    )
                    if (isError) {
                        Text(
                            text = "Please enter both fields to save.",
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
                            viewModel.addReply(keyword.trim(), replyText.trim())
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_new_reply_button")
                ) {
                    Text("Save Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Edit / Delete Dialog ---
    editingReply?.let { reply ->
        var keyword by remember { mutableStateOf(reply.keyword) }
        var replyText by remember { mutableStateOf(reply.replyText) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingReply = null },
            title = { Text("Edit Custom Reply") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Keyword Phrase") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_keyword_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Auto-Reply Text") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_reply_input"),
                        minLines = 3
                    )
                    if (isError) {
                        Text(
                            text = "Please fill in both fields.",
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
                        Text("Delete")
                    }

                    Row {
                        TextButton(onClick = { editingReply = null }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                if (keyword.isBlank() || replyText.isBlank()) {
                                    isError = true
                                } else {
                                    viewModel.updateReply(
                                        reply.copy(keyword = keyword.trim(), replyText = replyText.trim())
                                    )
                                    editingReply = null
                                }
                            },
                            modifier = Modifier.testTag("save_edit_reply_button")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        )
    }
}
