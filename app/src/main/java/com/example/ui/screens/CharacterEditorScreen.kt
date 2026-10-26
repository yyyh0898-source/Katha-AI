package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CharacterProfile
import com.example.data.model.KathaPresets
import com.example.ui.components.CharacterAvatarView
import com.example.ui.components.ImageGeneratingShimmerBox
import com.example.ui.components.MatureAgeGateDialog
import com.example.util.ImageCodec
import com.example.util.LocalAppStrings
import com.example.util.VoiceController
import com.example.viewmodel.AutoCraftState
import com.example.viewmodel.ImageGenState
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CharacterEditorScreen(
    initialProfile: CharacterProfile?,
    matureModeUnlocked: Boolean,
    autoCraftState: AutoCraftState,
    portraitGenState: ImageGenState,
    voiceController: VoiceController,
    onUnlockMatureMode: () -> Unit,
    onAutoCraft: (String) -> Unit,
    onResetAutoCraft: () -> Unit,
    onGenerateAiPortrait: (String) -> Unit,
    onSave: (CharacterProfile) -> Unit,
    onDelete: ((String) -> Unit)?,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(initialProfile) { mutableStateOf(initialProfile?.name ?: "") }
    var tagline by remember(initialProfile) { mutableStateOf(initialProfile?.tagline ?: "") }
    var gender by remember(initialProfile) { mutableStateOf(initialProfile?.gender ?: "female") }
    var ageText by remember(initialProfile) { mutableStateOf((initialProfile?.age ?: 24L).toString()) }
    var personality by remember(initialProfile) { mutableStateOf(initialProfile?.personality ?: "") }
    var backstory by remember(initialProfile) { mutableStateOf(initialProfile?.backstory ?: "") }
    var storyScenario by remember(initialProfile) { mutableStateOf(initialProfile?.storyScenario ?: "") }
    var greetingMessage by remember(initialProfile) { mutableStateOf(initialProfile?.greetingMessage ?: "") }
    var currentMood by remember(initialProfile) { mutableStateOf(initialProfile?.currentMood ?: "Romantic") }
    var isMature18Plus by remember(initialProfile) { mutableStateOf(initialProfile?.isMature18Plus ?: false) }
    var avatarPreset by remember(initialProfile) { mutableStateOf(initialProfile?.avatarPreset ?: "velvet_poet") }
    var avatarBase64 by remember(initialProfile) { mutableStateOf(initialProfile?.avatarBase64) }
    var voicePitch by remember(initialProfile) { mutableFloatStateOf((initialProfile?.voicePitch ?: 1.0).toFloat()) }
    var voiceRate by remember(initialProfile) { mutableFloatStateOf((initialProfile?.voiceRate ?: 1.0).toFloat()) }

    var autoCraftIdea by remember { mutableStateOf("") }
    var showAgeGate by remember { mutableStateOf(false) }

    // Apply AI Auto-Crafted fields when ready
    LaunchedEffect(autoCraftState) {
        if (autoCraftState is AutoCraftState.Success) {
            val c = autoCraftState.crafted
            name = c.name
            tagline = c.tagline
            gender = c.gender
            ageText = c.age.toString()
            personality = c.personality
            backstory = c.backstory
            storyScenario = c.storyScenario
            greetingMessage = c.greetingMessage
            currentMood = c.currentMood
            onResetAutoCraft()
        }
    }

    // Apply AI-generated portrait when ready
    LaunchedEffect(portraitGenState) {
        if (portraitGenState is ImageGenState.Success) {
            val encoded = ImageCodec.encodeBitmapToSyncBase64(portraitGenState.bitmap)
            if (encoded != null) {
                avatarBase64 = encoded
            }
        }
    }

    // Zero-permission Android Photo Picker for custom character portrait
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageCodec.decodeUriToBitmap(context, uri, maxDimension = 640)
                if (bmp != null) {
                    avatarBase64 = ImageCodec.encodeBitmapToSyncBase64(bmp)
                }
            }
        }
    }

    if (showAgeGate) {
        MatureAgeGateDialog(
            onConfirm18Plus = {
                showAgeGate = false
                onUnlockMatureMode()
                isMature18Plus = true
            },
            onDismiss = { showAgeGate = false }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Text(
                text = if (initialProfile == null) strings.editorCreateTitle else strings.editorEditTitle,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (initialProfile != null && onDelete != null) {
                IconButton(
                    onClick = { onDelete(initialProfile.id) },
                    modifier = Modifier.testTag("editor_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = strings.deleteCharacterButton,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            // AI Auto-Craft Persona Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.aiAutoCraftButton,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = autoCraftIdea,
                        onValueChange = { autoCraftIdea = it },
                        placeholder = { Text(strings.aiAutoCraftPromptHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_craft_prompt_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onAutoCraft(autoCraftIdea) },
                        enabled = autoCraftIdea.isNotBlank() && autoCraftState !is AutoCraftState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_craft_submit_button")
                    ) {
                        if (autoCraftState is AutoCraftState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(strings.aiAutoCraftButton)
                        }
                    }
                    if (autoCraftState is AutoCraftState.Error) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = autoCraftState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Avatar & Portrait Studio Section
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CharacterAvatarView(
                        name = name.ifBlank { "K" },
                        avatarPreset = avatarPreset,
                        avatarBase64 = avatarBase64,
                        size = 88.dp,
                        isMature18Plus = isMature18Plus
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset style selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        KathaPresets.avatarStyles.forEach { styleKey ->
                            val isChosen = avatarPreset == styleKey && avatarBase64 == null
                            CharacterAvatarView(
                                name = name.ifBlank { "K" },
                                avatarPreset = styleKey,
                                avatarBase64 = null,
                                size = 48.dp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .then(
                                        if (isChosen) {
                                            Modifier.border(
                                                2.5.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        avatarPreset = styleKey
                                        avatarBase64 = null
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_pick_photo_button")
                        ) {
                            Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.uploadPortraitButton, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                val prompt = buildString {
                                    append("Expressive character portrait of ")
                                    append(name.ifBlank { "an intriguing protagonist" })
                                    append(", ")
                                    append(gender)
                                    append(", ")
                                    append(tagline.ifBlank { personality.ifBlank { "warm cinematic lighting" } })
                                }
                                onGenerateAiPortrait(prompt)
                            },
                            enabled = portraitGenState !is ImageGenState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_generate_portrait_button")
                        ) {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.generatePortraitButton, maxLines = 1)
                        }
                    }

                    if (portraitGenState is ImageGenState.Loading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ImageGeneratingShimmerBox(promptText = portraitGenState.prompt)
                    } else if (portraitGenState is ImageGenState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = portraitGenState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Core Identity Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.fieldName) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_name_input")
            )

            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text(strings.fieldTagline) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_tagline_input")
            )

            // Gender Identity
            Text(text = strings.fieldGender, style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    "female" to strings.filterFemale,
                    "male" to strings.filterMale,
                    "non_binary" to strings.filterNonBinary,
                    "other" to (if (strings.isMyanmar) "အခြား" else "Other")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = gender == key,
                        onClick = { gender = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("editor_gender_$key")
                    )
                }
            }

            // Adult Age (18+)
            OutlinedTextField(
                value = ageText,
                onValueChange = { input -> ageText = input.filter { it.isDigit() }.take(3) },
                label = { Text(strings.fieldAge) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_age_input")
            )

            // Initial Mood Selection
            Text(text = strings.moodLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                KathaPresets.moods.forEach { moodOption ->
                    FilterChip(
                        selected = currentMood.equals(moodOption.key, ignoreCase = true),
                        onClick = { currentMood = moodOption.key },
                        label = { Text(if (strings.isMyanmar) moodOption.labelMy else moodOption.labelEn) },
                        modifier = Modifier.testTag("editor_mood_${moodOption.key}")
                    )
                }
            }

            // 18+ Mature Audience Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.fieldMature18Plus,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = strings.fieldMatureDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMature18Plus,
                        onCheckedChange = { checked ->
                            if (checked && !matureModeUnlocked) {
                                showAgeGate = true
                            } else {
                                isMature18Plus = checked
                            }
                        },
                        modifier = Modifier.testTag("editor_mature_switch")
                    )
                }
            }

            // Story & Persona Textareas
            OutlinedTextField(
                value = personality,
                onValueChange = { personality = it },
                label = { Text(strings.fieldPersonality) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_personality_input")
            )

            OutlinedTextField(
                value = backstory,
                onValueChange = { backstory = it },
                label = { Text(strings.fieldBackstory) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_backstory_input")
            )

            OutlinedTextField(
                value = storyScenario,
                onValueChange = { storyScenario = it },
                label = { Text(strings.fieldScenario) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_scenario_input")
            )

            OutlinedTextField(
                value = greetingMessage,
                onValueChange = { greetingMessage = it },
                label = { Text(strings.fieldGreeting) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_greeting_input")
            )

            HorizontalDivider()

            // Voice Pitch & Rate Tuning
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = strings.fieldVoiceSettings, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = {
                        val sample = greetingMessage.ifBlank {
                            if (strings.isMyanmar) "မင်္ဂလာပါ ကျွန်ုပ်နဲ့ စကားပြောကြရအောင်" else "Hello, let's begin our story together."
                        }
                        voiceController.speak(
                            utteranceId = "preview_voice",
                            rawText = sample,
                            pitch = voicePitch,
                            rate = voiceRate
                        )
                    },
                    modifier = Modifier.testTag("editor_test_voice_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.speakAloud)
                }
            }

            Text(
                text = "${strings.fieldVoicePitch}: ${String.format(Locale.US, "%.2f", voicePitch)}x",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = voicePitch,
                onValueChange = { voicePitch = it },
                valueRange = 0.6f..1.6f
            )

            Text(
                text = "${strings.fieldVoiceSpeed}: ${String.format(Locale.US, "%.2f", voiceRate)}x",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = voiceRate,
                onValueChange = { voiceRate = it },
                valueRange = 0.65f..1.45f
            )

            // Save Action Button
            Button(
                onClick = {
                    val parsedAge = (ageText.toLongOrNull() ?: 24L).coerceIn(18L, 999L)
                    val updated = (initialProfile ?: CharacterProfile()).copy(
                        name = name.trim().ifBlank { "Katha Companion" },
                        tagline = tagline.trim().ifBlank { "Interactive Story" },
                        gender = gender,
                        age = parsedAge,
                        ageGroup = CharacterProfile.computeAgeGroup(parsedAge),
                        personality = personality.trim().ifBlank { "Warm, expressive, human-like" },
                        backstory = backstory.trim().ifBlank { "An intriguing story companion." },
                        storyScenario = storyScenario.trim().ifBlank { "A heartfelt private conversation." },
                        greetingMessage = greetingMessage.trim().ifBlank { "မင်္ဂလာပါ... စကားပြောကြရအောင်နော်။" },
                        currentMood = currentMood,
                        isMature18Plus = isMature18Plus,
                        avatarPreset = avatarPreset,
                        avatarBase64 = avatarBase64,
                        voicePitch = voicePitch.toDouble(),
                        voiceRate = voiceRate.toDouble()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("editor_save_character_button")
            ) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.saveCharacterButton, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
