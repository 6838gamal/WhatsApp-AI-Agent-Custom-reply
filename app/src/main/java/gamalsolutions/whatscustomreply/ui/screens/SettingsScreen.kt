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
import gamalsolutions.whatscustomreply.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
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
                text = "Preferences & Constraints",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Configure conditions and boundaries for automatic response replies.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Interactive Response Engine Mode Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
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
                        imageVector = Icons.Filled.Rule,
                        contentDescription = "Rule Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Auto Reply Mode Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = "Specify how incoming messages should be classified and answered.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                val modes = listOf(
                    Triple("CUSTOM", "Custom Replies Only", "Relies entirely on matching keywords."),
                    Triple("GEMINI", "Gemini AI Engine", "Generate smart, custom replies using Gemini Flash."),
                    Triple("HYBRID", "Hybrid Responder Mode", "Checks keywords first. Invokes Gemini as fallback.")
                )

                modes.forEach { (modeKey, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (settings.replyMode == modeKey) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.updateReplyMode(modeKey) }
                            .padding(10.dp)
                            .testTag("reply_mode_option_$modeKey"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.replyMode == modeKey,
                            onClick = { viewModel.updateReplyMode(modeKey) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 2. Chat Filtering Constraints
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterAlt,
                        contentDescription = "Filters icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text("Target Filtering Rules", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ignore Group Chats", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Skip automated replies for WhatsApp group notifications.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.ignoreGroups,
                        onCheckedChange = { viewModel.updateIgnoreGroups(it) },
                        modifier = Modifier.testTag("ignore_groups_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Block Duplicates", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Avoid answering identical notifications repeatedly.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.ignoreDuplicates,
                        onCheckedChange = { viewModel.updateIgnoreDuplicates(it) },
                        modifier = Modifier.testTag("ignore_duplicates_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reply Once Per User", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Only trigger the automated loop once for each contact name.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.replyOncePerUser,
                        onCheckedChange = { viewModel.updateReplyOncePerUser(it) },
                        modifier = Modifier.testTag("reply_once_switch")
                    )
                }
            }
        }

        // 3. Simulated Response Delay Config
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HourglassEmpty,
                            contentDescription = "Delay icon",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text("Reply Delay Simulation", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Switch(
                        checked = settings.randomDelayEnabled,
                        onCheckedChange = { viewModel.updateRandomDelayEnabled(it) },
                        modifier = Modifier.testTag("delay_enabled_switch")
                    )
                }

                Text(
                    text = "Simulates natural human behavior by waiting a randomized delay interval before submitting responses.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (settings.randomDelayEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextField(
                            value = settings.randomDelayMin.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { min -> viewModel.updateRandomDelayMin(min) }
                            },
                            label = { Text("Min Seconds") },
                            modifier = Modifier.weight(1f).testTag("delay_min_field"),
                            singleLine = true
                        )

                        TextField(
                            value = settings.randomDelayMax.toString(),
                            onValueChange = {
                                it.toIntOrNull()?.let { max -> viewModel.updateRandomDelayMax(max) }
                            },
                            label = { Text("Max Seconds") },
                            modifier = Modifier.weight(1f).testTag("delay_max_field"),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 4. Working Hours Schedule
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Working Hours schedules Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Working Hours Limits", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Switch(
                        checked = settings.workingHoursEnabled,
                        onCheckedChange = { viewModel.updateWorkingHoursEnabled(it) },
                        modifier = Modifier.testTag("working_hours_enabled_switch")
                    )
                }

                Text(
                    text = "When active, auto-replies will only trigger if the message falls within your custom business hours.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (settings.workingHoursEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextField(
                            value = settings.workingHoursStart,
                            onValueChange = { viewModel.updateWorkingHoursStart(it) },
                            label = { Text("Start Time (HH:mm)") },
                            placeholder = { Text("09:00") },
                            modifier = Modifier.weight(1f).testTag("working_hours_start_field"),
                            singleLine = true
                        )

                        TextField(
                            value = settings.workingHoursEnd,
                            onValueChange = { viewModel.updateWorkingHoursEnd(it) },
                            label = { Text("End Time (HH:mm)") },
                            placeholder = { Text("18:00") },
                            modifier = Modifier.weight(1f).testTag("working_hours_end_field"),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
