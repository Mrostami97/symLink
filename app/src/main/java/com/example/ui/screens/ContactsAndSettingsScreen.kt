package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.SyncedContactEntity
import com.example.data.preferences.UserPreferences
import com.example.ui.components.GlassBridgeCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeDigits
import com.example.ui.theme.ContactAvatarColors
import com.example.ui.theme.ContactAvatarColorsLight
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MonoTechnicalStyle

@Composable
fun ContactsAndSettingsScreen(
    contacts: List<SyncedContactEntity>,
    preferences: UserPreferences,
    onPickContact: () -> Unit,
    onSyncAddressBook: () -> Unit,
    onPushContactsToPeer: () -> Unit,
    onDialContact: (String) -> Unit,
    onSmsContact: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetDeviceAlias: (String) -> Unit,
    onSetDefaultSim: (Int) -> Unit,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetVibrateIncoming: (Boolean) -> Unit,
    onReplayOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isDark = LocalIsDarkTheme.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val emeraldAccent = MaterialTheme.colorScheme.tertiary
    val amberAccent = MaterialTheme.colorScheme.secondary
    val avatarPalette = if (isDark) ContactAvatarColors else ContactAvatarColorsLight
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var aliasDraft by remember(preferences.deviceAlias) { mutableStateOf(preferences.deviceAlias) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_settings_tab_row")
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_contacts")
                ) {
                    Text(strings.contactsTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_settings")
                ) {
                    Text(strings.settingsTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (selectedTab == 0) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchContactsHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = primaryAccent) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contacts_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickContact,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pick_contact_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickContact, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onSyncAddressBook,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("sync_address_book_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.syncAddressBook, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (contacts.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = onPushContactsToPeer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("push_contacts_peer_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = primaryAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.pushContactsToPeer, color = primaryAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                item {
                    GlassBridgeCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = primaryAccent,
                        accentStrength = 0.25f
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(primaryAccent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Contacts,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(strings.noContactsYet, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = strings.noContactsDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredContacts, key = { it.normalizedNumber }) { contact ->
                    val avatarColor = avatarPalette[contact.avatarColorIndex % avatarPalette.size]
                    val initials = contact.displayName.trim().take(1).uppercase()
                    GlassBridgeCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = avatarColor,
                        accentStrength = 0.24f,
                        cornerRadius = 18.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor.copy(alpha = 0.16f))
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(listOf(avatarColor, primaryAccent)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = avatarColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MonoTechnicalStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onDialContact(contact.phoneNumber) }) {
                                Icon(Icons.Filled.Call, contentDescription = strings.openDialer, tint = emeraldAccent)
                            }
                            IconButton(onClick = { onSmsContact(contact.phoneNumber) }) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = strings.smsHandoffMode, tint = amberAccent)
                            }
                        }
                    }
                }
            }
        } else {
            // SETTINGS & SECURITY TAB
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryAccent,
                    accentStrength = 0.3f,
                    cornerRadius = 22.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.languageLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = preferences.languageCode == "fa",
                                onClick = { onSetLanguage("fa") },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("settings_lang_fa")
                            ) {
                                Text(strings.persianLabel)
                            }
                            SegmentedButton(
                                selected = preferences.languageCode == "en",
                                onClick = { onSetLanguage("en") },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("settings_lang_en")
                            ) {
                                Text(strings.englishLabel)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Prominent Manual Light / Dark Mode Override Switch (ignores system theme)
                        val themeAccent = if (preferences.darkTheme) primaryAccent else amberAccent
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = themeAccent.copy(alpha = if (isDark) 0.12f else 0.10f),
                            border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(themeAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (preferences.darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = strings.themeModeTitle,
                                        tint = themeAccent,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = strings.themeModeTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (preferences.darkTheme) strings.themeModeSubtitleDark else strings.themeModeSubtitleLight,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = preferences.darkTheme,
                                    onCheckedChange = onSetDarkTheme,
                                    modifier = Modifier.testTag("settings_dark_theme_switch")
                                )
                            }
                        }

                        SettingSwitchRow(
                            title = strings.autoReconnectLabel,
                            checked = preferences.autoReconnect,
                            onCheckedChange = onSetAutoReconnect,
                            testTag = "settings_auto_reconnect_switch"
                        )

                        SettingSwitchRow(
                            title = strings.vibrateIncomingLabel,
                            checked = preferences.vibrateOnIncoming,
                            onCheckedChange = onSetVibrateIncoming,
                            testTag = "settings_vibrate_switch"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = strings.defaultSimLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = preferences.preferredSimSlot == 1,
                                onClick = { onSetDefaultSim(1) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("${strings.simSlotLabel} 1".localizeDigits(strings.isPersian))
                            }
                            SegmentedButton(
                                selected = preferences.preferredSimSlot == 2,
                                onClick = { onSetDefaultSim(2) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("${strings.simSlotLabel} 2".localizeDigits(strings.isPersian))
                            }
                        }

                        OutlinedTextField(
                            value = aliasDraft,
                            onValueChange = {
                                aliasDraft = it
                                if (it.isNotBlank()) onSetDeviceAlias(it)
                            },
                            label = { Text(strings.deviceNameLabel) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_device_name_input")
                        )
                    }
                }
            }

            // Replay Animated Intro Guide Card
            item {
                GlassBridgeCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("replay_onboarding_button"),
                    accentColor = primaryAccent,
                    accentStrength = 0.42f,
                    cornerRadius = 20.dp,
                    onClick = onReplayOnboarding
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(primaryAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleOutline,
                                contentDescription = strings.replayOnboardingLabel,
                                tint = primaryAccent,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.replayOnboardingLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.replayOnboardingSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = primaryAccent
                        )
                    }
                }
            }

            // Security & Play Creator Compliance Shield Card
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = emeraldAccent,
                    accentStrength = 0.50f,
                    cornerRadius = 22.dp
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(emeraldAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = emeraldAccent, modifier = Modifier.size(24.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strings.playComplianceTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = emeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.playComplianceBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Creator Credit Badge
            item {
                GlassBridgeCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = primaryAccent,
                    accentStrength = 0.42f,
                    cornerRadius = 18.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.creatorCredit,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
