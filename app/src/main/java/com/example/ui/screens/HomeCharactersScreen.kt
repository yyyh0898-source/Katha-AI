package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LocalPrefsState
import com.example.data.model.CharacterProfile
import com.example.data.model.KathaPresets
import com.example.ui.components.CharacterAvatarView
import com.example.ui.components.CloudSyncStatusPill
import com.example.ui.components.MatureAgeGateDialog
import com.example.util.LocalAppStrings
import com.example.viewmodel.DataUiState

@Composable
fun HomeCharactersScreen(
    charactersUiState: DataUiState<List<CharacterProfile>>,
    filteredCharacters: List<CharacterProfile>,
    localPrefs: LocalPrefsState,
    isOnline: Boolean,
    searchQuery: String,
    selectedGender: String,
    selectedAgeGroup: String,
    only18Plus: Boolean,
    activeCharacterId: String?,
    onSearchChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAgeGroupChange: (String) -> Unit,
    onToggle18PlusFilter: (Boolean) -> Unit,
    onEnableMatureMode: () -> Unit,
    onQuickToggleTheme: () -> Unit,
    onQuickToggleLanguage: () -> Unit,
    onSelectCharacter: (CharacterProfile) -> Unit,
    onEditCharacter: (CharacterProfile) -> Unit,
    onCreateCharacter: () -> Unit,
    onLoadStarterStories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showAgeGateDialog by remember { mutableStateOf(false) }

    if (showAgeGateDialog) {
        MatureAgeGateDialog(
            onConfirm18Plus = {
                showAgeGateDialog = false
                onEnableMatureMode()
                onToggle18PlusFilter(true)
            },
            onDismiss = { showAgeGateDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Hero Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.appSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onQuickToggleLanguage() }
                                    .testTag("header_language_toggle")
                            ) {
                                Text(
                                    text = if (localPrefs.languageCode == "my") "MY" else "EN",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onQuickToggleTheme,
                                modifier = Modifier.testTag("header_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (localPrefs.themeMode == "light") Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = strings.settingsAppearance,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    CloudSyncStatusPill(isOnline = isOnline)
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(strings.searchPlaceholder, maxLines = 1) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("character_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gender + 18+ Filter Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                FilterChip(
                    selected = selectedGender == "all",
                    onClick = { onGenderChange("all") },
                    label = { Text(strings.filterAll) },
                    modifier = Modifier.testTag("filter_gender_all")
                )
                FilterChip(
                    selected = selectedGender == "female",
                    onClick = { onGenderChange("female") },
                    label = { Text(strings.filterFemale) },
                    modifier = Modifier.testTag("filter_gender_female")
                )
                FilterChip(
                    selected = selectedGender == "male",
                    onClick = { onGenderChange("male") },
                    label = { Text(strings.filterMale) },
                    modifier = Modifier.testTag("filter_gender_male")
                )
                FilterChip(
                    selected = selectedGender == "non_binary",
                    onClick = { onGenderChange("non_binary") },
                    label = { Text(strings.filterNonBinary) },
                    modifier = Modifier.testTag("filter_gender_nb")
                )
                FilterChip(
                    selected = only18Plus,
                    onClick = {
                        if (!localPrefs.matureContentEnabled) {
                            showAgeGateDialog = true
                        } else {
                            onToggle18PlusFilter(!only18Plus)
                        }
                    },
                    label = { Text(strings.filter18PlusBadge) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFBE123C),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_18_plus_chip")
                )
            }

            // Age Bracket Filter Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                FilterChip(
                    selected = selectedAgeGroup == "all",
                    onClick = { onAgeGroupChange("all") },
                    label = { Text(if (strings.isMyanmar) "အသက်အားလုံး" else "All Ages") },
                    modifier = Modifier.testTag("filter_age_all")
                )
                FilterChip(
                    selected = selectedAgeGroup == "18_24",
                    onClick = { onAgeGroupChange("18_24") },
                    label = { Text(strings.filterAge1824) },
                    modifier = Modifier.testTag("filter_age_18_24")
                )
                FilterChip(
                    selected = selectedAgeGroup == "25_34",
                    onClick = { onAgeGroupChange("25_34") },
                    label = { Text(strings.filterAge2534) },
                    modifier = Modifier.testTag("filter_age_25_34")
                )
                FilterChip(
                    selected = selectedAgeGroup == "35_plus",
                    onClick = { onAgeGroupChange("35_plus") },
                    label = { Text(strings.filterAge35Plus) },
                    modifier = Modifier.testTag("filter_age_35_plus")
                )
                FilterChip(
                    selected = selectedAgeGroup == "fantasy_ageless",
                    onClick = { onAgeGroupChange("fantasy_ageless") },
                    label = { Text(strings.filterAgeFantasy) },
                    modifier = Modifier.testTag("filter_age_fantasy")
                )
            }

            // Character List Content
            when (charactersUiState) {
                is DataUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DataUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = charactersUiState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is DataUiState.Success -> {
                    if (filteredCharacters.isEmpty()) {
                        EmptyCharactersPlaceholder(
                            onLoadStarter = onLoadStarterStories,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("characters_lazy_column")
                        ) {
                            items(
                                items = filteredCharacters,
                                key = { it.id }
                            ) { character ->
                                CharacterStoryCard(
                                    character = character,
                                    isSelected = character.id == activeCharacterId,
                                    onClick = { onSelectCharacter(character) },
                                    onEdit = { onEditCharacter(character) }
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCreateCharacter,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = strings.createCharacterFab
                )
            },
            text = { Text(strings.createCharacterFab) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_character_fab")
        )
    }
}

@Composable
private fun CharacterStoryCard(
    character: CharacterProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val strings = LocalAppStrings.current
    val moodDisplay = remember(character.currentMood, strings.isMyanmar) {
        val preset = KathaPresets.moods.find { it.key.equals(character.currentMood, ignoreCase = true) }
        if (preset != null) {
            if (strings.isMyanmar) preset.labelMy else preset.labelEn
        } else {
            character.currentMood
        }
    }

    val genderLabel = when (character.gender) {
        "female" -> strings.filterFemale
        "male" -> strings.filterMale
        "non_binary" -> strings.filterNonBinary
        else -> strings.filterAll
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_card_${character.id}")
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            CharacterAvatarView(
                name = character.name,
                avatarPreset = character.avatarPreset,
                avatarBase64 = character.avatarBase64,
                size = 64.dp,
                isMature18Plus = character.isMature18Plus
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_character_${character.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = strings.editCharacterProfile,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = character.tagline,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata badges (Mood + Gender/Age)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✦ $moodDisplay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$genderLabel • ${character.age}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = character.lastMessagePreview.isNotBlank()) {
                    Text(
                        text = character.lastMessagePreview.replace("*", ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCharactersPlaceholder(
    onLoadStarter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = strings.emptyCharactersTitle,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.emptyCharactersHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLoadStarter,
            modifier = Modifier.testTag("load_starter_stories_button")
        ) {
            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.loadStarterStories)
        }
    }
}
