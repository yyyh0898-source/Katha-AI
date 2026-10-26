package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalPrefsState
import com.example.ui.components.CloudSyncStatusPill
import com.example.ui.components.MatureAgeGateDialog
import com.example.util.LocalAppStrings

@Composable
fun SettingsScreen(
    localPrefs: LocalPrefsState,
    userEmail: String?,
    isOnline: Boolean,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onAccentThemeChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onMatureContentChange: (Boolean) -> Unit,
    onBiometricLockChange: (Boolean, String?) -> Unit,
    onBackupPinChange: (String) -> Unit,
    onAutoVoiceChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var nameDraft by remember(localPrefs.displayName) { mutableStateOf(localPrefs.displayName) }
    var pinDraft by remember(localPrefs.backupPin) { mutableStateOf(localPrefs.backupPin) }
    var showAgeGate by remember { mutableStateOf(false) }

    if (showAgeGate) {
        MatureAgeGateDialog(
            onConfirm18Plus = {
                showAgeGate = false
                onMatureContentChange(true)
            },
            onDismiss = { showAgeGate = false }
        )
    }

    val accentSwatches = listOf(
        Triple("velvet_rose", "Velvet Rose", Color(0xFFE11D48)),
        Triple("midnight_violet", "Midnight Violet", Color(0xFF8B5CF6)),
        Triple("golden_amber", "Golden Amber", Color(0xFFD97706)),
        Triple("emerald_jade", "Emerald Jade", Color(0xFF059669)),
        Triple("ocean_cyan", "Ocean Cyan", Color(0xFF0284C7))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.tabSettings,
                    style = MaterialTheme.typography.headlineMedium
                )
                CloudSyncStatusPill(isOnline = isOnline)
            }

            if (!userEmail.isNullOrBlank()) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Story Roleplay Name & Language Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsLanguage,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = localPrefs.languageCode == "my",
                            onClick = { onLanguageChange("my") },
                            label = { Text("မြန်မာဘာသာ (Myanmar)") },
                            modifier = Modifier.testTag("settings_lang_my")
                        )
                        FilterChip(
                            selected = localPrefs.languageCode == "en",
                            onClick = { onLanguageChange("en") },
                            label = { Text("English") },
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = {
                            nameDraft = it.take(80)
                            onDisplayNameChange(nameDraft)
                        },
                        label = { Text(strings.settingsDisplayName) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_display_name_input")
                    )
                }
            }

            // Theme Mode & Custom Accent Palette Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsAppearance,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = localPrefs.themeMode == "dark",
                            onClick = { onThemeModeChange("dark") },
                            label = { Text(strings.settingsThemeDark) },
                            modifier = Modifier.testTag("settings_theme_dark")
                        )
                        FilterChip(
                            selected = localPrefs.themeMode == "light",
                            onClick = { onThemeModeChange("light") },
                            label = { Text(strings.settingsThemeLight) },
                            modifier = Modifier.testTag("settings_theme_light")
                        )
                        FilterChip(
                            selected = localPrefs.themeMode == "system",
                            onClick = { onThemeModeChange("system") },
                            label = { Text(strings.settingsThemeSystem) },
                            modifier = Modifier.testTag("settings_theme_system")
                        )
                    }

                    Text(
                        text = strings.settingsAccentPalette,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        accentSwatches.forEach { (key, label, swatchColor) ->
                            val isSelected = localPrefs.accentTheme == key
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onAccentThemeChange(key) }
                                    .testTag("settings_accent_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = label,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Security (Face ID / Fingerprint + Backup PIN), 18+ Mature & Auto-Voice Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.settingsPrivacySecurity,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Biometric Lock Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsBiometricTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = strings.settingsBiometricDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = localPrefs.biometricLockEnabled,
                            onCheckedChange = { enabled ->
                                val defaultPinIfEmpty = if (enabled && pinDraft.isBlank()) "1234" else pinDraft
                                if (enabled && pinDraft.isBlank()) {
                                    pinDraft = "1234"
                                }
                                onBiometricLockChange(enabled, defaultPinIfEmpty)
                            },
                            modifier = Modifier.testTag("settings_biometric_switch")
                        )
                    }

                    if (localPrefs.biometricLockEnabled) {
                        OutlinedTextField(
                            value = pinDraft,
                            onValueChange = { raw ->
                                pinDraft = raw.filter { it.isDigit() }.take(6)
                                onBackupPinChange(pinDraft)
                            },
                            label = { Text(strings.settingsPinFallbackLabel) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_backup_pin_input")
                        )
                    }

                    HorizontalDivider()

                    // 18+ Mature Content Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsMatureTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = strings.settingsMatureDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = localPrefs.matureContentEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showAgeGate = true
                                } else {
                                    onMatureContentChange(false)
                                }
                            },
                            modifier = Modifier.testTag("settings_mature_switch")
                        )
                    }

                    HorizontalDivider()

                    // Auto-Voice Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsAutoVoiceTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = strings.settingsAutoVoiceDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = localPrefs.autoVoiceEnabled,
                            onCheckedChange = onAutoVoiceChange,
                            modifier = Modifier.testTag("settings_auto_voice_switch")
                        )
                    }
                }
            }

            // Sign Out Button
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_sign_out_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.settingsSignOut)
            }

            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}
