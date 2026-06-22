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
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val labels = if (settings.appLanguage == "en") EnStrings else ArStrings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = labels.generalPreferences,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = labels.generalPreferencesDesc,
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
                    Text(labels.repliesModeTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = labels.repliesModeDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                val modes = listOf(
                    Triple("CUSTOM", labels.modeCustom, labels.modeCustomDesc),
                    Triple("API", labels.modeGemini, labels.modeGeminiDesc),
                    Triple("HYBRID", labels.modeHybrid, labels.modeHybridDesc)
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

        // 2. App Display Language Preference
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = "Language Preference Icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(labels.appLanguageLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = labels.changeLanguageSettingDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Arabic Button
                    Button(
                        onClick = { viewModel.updateAppLanguage("ar") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.appLanguage == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (settings.appLanguage == "ar") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("lang_arabic_button")
                    ) {
                        if (settings.appLanguage == "ar") {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("العربية", fontWeight = FontWeight.Bold)
                    }

                    // English Button
                    Button(
                        onClick = { viewModel.updateAppLanguage("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.appLanguage == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (settings.appLanguage == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("lang_english_button")
                    ) {
                        if (settings.appLanguage == "en") {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("English", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Call Responder & Voice Config Cards (REQUESTED BY USER)
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
                            imageVector = Icons.Filled.PhoneCallback,
                            contentDescription = "Calls Auto responder icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(labels.callReplyHeader, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Switch(
                        checked = settings.callReplyEnabled,
                        onCheckedChange = { viewModel.updateCallReplyEnabled(it) },
                        modifier = Modifier.testTag("call_reply_enabled_switch")
                    )
                }

                Text(
                    text = labels.callReplyDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = settings.callReplyEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = labels.callReplyTextLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        TextField(
                            value = settings.callReplyText,
                            onValueChange = { viewModel.updateCallReplyText(it) },
                            modifier = Modifier.fillMaxWidth().testTag("call_reply_text_field"),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        // 4. System sound levels and profiles config
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
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Sound config profile icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(labels.audioSettingsHeader, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = labels.audioSettingsDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Ringer profile mode selector (Normal, Vibrate, Silent)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = labels.ringerModeLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val profiles = listOf(
                            Triple(0, Icons.Filled.VolumeOff, labels.ringerModeSilent),
                            Triple(1, Icons.Filled.Vibration, labels.ringerModeVibrate),
                            Triple(2, Icons.Filled.VolumeUp, labels.ringerModeNormal)
                        )
                        profiles.forEach { (mode, icon, text) ->
                            val isSelected = settings.ringerMode == mode
                            IconButton(
                                onClick = { viewModel.updateRingerMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .padding(8.dp)
                                    .testTag("ringer_mode_option_$mode")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = text,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Volume Controls (Ringer & Media) sliders
                AnimatedVisibility(
                    visible = settings.ringerMode == 2,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(labels.ringerVolumeLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${settings.ringerVolume}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = settings.ringerVolume.toFloat(),
                            onValueChange = { viewModel.updateRingerVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.testTag("ringer_volume_slider")
                        )
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(labels.mediaVolumeLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${settings.mediaVolume}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Slider(
                        value = settings.mediaVolume.toFloat(),
                        onValueChange = { viewModel.updateMediaVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag("media_volume_slider")
                    )
                }
            }
        }

        // Quiet Focus Mode - Uninterrupted Screentime (Requested by User)
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
                        imageVector = Icons.Filled.DoNotDisturbOn,
                        contentDescription = "Quiet Focus icon",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(labels.quietModeHeader, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = labels.quietModeDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(labels.dismissNotificationsSetting, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(labels.dismissNotificationsSettingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.dismissNotificationsEnabled,
                        onCheckedChange = { viewModel.updateDismissNotificationsEnabled(it) },
                        modifier = Modifier.testTag("dismiss_notifications_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(labels.voiceReplyAnnounceSetting, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(labels.voiceReplyAnnounceSettingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.voiceReplyEnabled,
                        onCheckedChange = { viewModel.updateVoiceReplyEnabled(it) },
                        modifier = Modifier.testTag("voice_reply_switch")
                    )
                }
            }
        }

        // 5. Chat Filtering Constraints
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
                    Text(labels.filterPreferences, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(labels.ignoreGroupsSetting, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(labels.ignoreGroupsSettingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(labels.ignoreDuplicatesSetting, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(labels.ignoreDuplicatesSettingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(labels.replyOnceSetting, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(labels.replyOnceSettingDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.replyOncePerUser,
                        onCheckedChange = { viewModel.updateReplyOncePerUser(it) },
                        modifier = Modifier.testTag("reply_once_switch")
                    )
                }
            }
        }

        // 6. Simulated Response Delay Config
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
                        Text(labels.randomDelaySetting, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Switch(
                        checked = settings.randomDelayEnabled,
                        onCheckedChange = { viewModel.updateRandomDelayEnabled(it) },
                        modifier = Modifier.testTag("delay_enabled_switch")
                    )
                }

                Text(
                    text = labels.randomDelaySettingDesc,
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

        // 7. Working Hours Schedule
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
                        Text(labels.workingHoursSetting, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Switch(
                        checked = settings.workingHoursEnabled,
                        onCheckedChange = { viewModel.updateWorkingHoursEnabled(it) },
                        modifier = Modifier.testTag("working_hours_enabled_switch")
                    )
                }

                Text(
                    text = labels.workingHoursSettingDesc,
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
                            label = { Text(labels.workingHoursStartLabel) },
                            placeholder = { Text("09:00") },
                            modifier = Modifier.weight(1f).testTag("working_hours_start_field"),
                            singleLine = true
                        )

                        TextField(
                            value = settings.workingHoursEnd,
                            onValueChange = { viewModel.updateWorkingHoursEnd(it) },
                            label = { Text(labels.workingHoursEndLabel) },
                            placeholder = { Text("18:00") },
                            modifier = Modifier.weight(1f).testTag("working_hours_end_field"),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 8. Multi-Account Management Section
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PeopleOutline,
                        contentDescription = "Multi Account filters",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(labels.accountsSectionTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Text(
                    text = labels.accountsSectionDesc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input field for Primary Phone
                TextField(
                    value = settings.primaryAccountPhone,
                    onValueChange = { viewModel.updatePrimaryAccountPhone(it) },
                    label = { Text(labels.primaryPhoneLabel) },
                    placeholder = { Text("+9665xxxxxxxx") },
                    modifier = Modifier.fillMaxWidth().testTag("primary_phone_input_field"),
                    singleLine = true
                )

                // Input field for Additional Phones
                TextField(
                    value = settings.additionalAccountPhones,
                    onValueChange = { viewModel.updateAdditionalAccountPhones(it) },
                    label = { Text(labels.additionalPhonesLabel) },
                    placeholder = { Text("+9665xxxxxxxx, +9665yyyyyyyy") },
                    modifier = Modifier.fillMaxWidth().testTag("additional_phones_input_field"),
                    singleLine = true
                )
            }
        }
    }
}
