package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CharacterProfile
import com.example.data.model.ChatMessage
import com.example.data.model.KathaPresets
import com.example.ui.components.CharacterAvatarView
import com.example.ui.components.ImageGeneratingShimmerBox
import com.example.ui.components.RoleplayFormattedText
import com.example.util.ImageCodec
import com.example.util.LocalAppStrings
import com.example.util.VoiceController
import com.example.viewmodel.ChatGenState
import com.example.viewmodel.DataUiState
import com.example.viewmodel.ImageGenState
import kotlinx.coroutines.launch

@Composable
fun ChatStoryScreen(
    character: CharacterProfile,
    messagesState: DataUiState<List<ChatMessage>>,
    chatGenState: ChatGenState,
    chatImageGenState: ImageGenState,
    autoVoiceEnabled: Boolean,
    voiceController: VoiceController,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onEditCharacter: () -> Unit,
    onRestartStory: () -> Unit,
    onChangeMood: (String) -> Unit,
    onSendMessage: (String, Bitmap?) -> Unit,
    onGenerateSceneImage: (String) -> Unit,
    onResetImageGenState: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showBackButton) {
        BackHandler {
            voiceController.stop()
            onBack()
        }
    }

    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speakingId by voiceController.speakingId.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var attachedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    var showCustomMoodDialog by remember { mutableStateOf(false) }
    var customMoodInput by remember { mutableStateOf("") }
    var showImagePromptDialog by remember { mutableStateOf(false) }
    var sceneImagePrompt by remember { mutableStateOf("") }

    val messages = (messagesState as? DataUiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()

    // Auto-scroll on new messages or streaming text
    LaunchedEffect(messages.size, chatGenState, chatImageGenState) {
        val totalItems = messages.size + (if (chatGenState !is ChatGenState.Idle) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Android Photo Picker for sending images in chat
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                attachedPhoto = ImageCodec.decodeUriToBitmap(context, uri, maxDimension = 1024)
            }
        }
    }

    // Speech-to-Text RecognizerIntent launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    if (showCustomMoodDialog) {
        AlertDialog(
            onDismissRequest = { showCustomMoodDialog = false },
            title = { Text(strings.moodLabel) },
            text = {
                OutlinedTextField(
                    value = customMoodInput,
                    onValueChange = { customMoodInput = it.take(50) },
                    placeholder = { Text(strings.customMoodPlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_mood_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customMoodInput.isNotBlank()) {
                            onChangeMood(customMoodInput.trim())
                        }
                        showCustomMoodDialog = false
                    },
                    modifier = Modifier.testTag("confirm_custom_mood_button")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMoodDialog = false }) {
                    Text(strings.ageGateCancel)
                }
            }
        )
    }

    if (showImagePromptDialog) {
        AlertDialog(
            onDismissRequest = { showImagePromptDialog = false },
            title = { Text(strings.generateImageDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sceneImagePrompt,
                        onValueChange = { sceneImagePrompt = it },
                        placeholder = { Text(strings.generateImagePromptHint) },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_image_prompt_input")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        AssistChip(
                            onClick = {
                                sceneImagePrompt = "${character.name} in ${character.storyScenario}, expressive portrait, warm cinematic lighting"
                            },
                            label = { Text(if (strings.isMyanmar) "ဇာတ်ဝင်ခန်းပုံ" else "Current Scene") }
                        )
                        AssistChip(
                            onClick = {
                                sceneImagePrompt = "Close-up emotional portrait of ${character.name} looking at viewer with ${character.currentMood} expression"
                            },
                            label = { Text(if (strings.isMyanmar) "အနီးကပ်ပုံ" else "Close-up") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sceneImagePrompt.isNotBlank()) {
                            showImagePromptDialog = false
                            onGenerateSceneImage(sceneImagePrompt.trim())
                            sceneImagePrompt = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_generate_chat_image_button")
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.studioGenerateBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagePromptDialog = false }) {
                    Text(strings.ageGateCancel)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Chat Top Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = {
                                voiceController.stop()
                                onBack()
                            },
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    CharacterAvatarView(
                        name = character.name,
                        avatarPreset = character.avatarPreset,
                        avatarBase64 = character.avatarBase64,
                        size = 44.dp,
                        isMature18Plus = character.isMature18Plus
                    )
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = character.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onEditCharacter,
                        modifier = Modifier.testTag("chat_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = strings.editCharacterProfile
                        )
                    }
                    IconButton(
                        onClick = onRestartStory,
                        modifier = Modifier.testTag("chat_restart_story_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = strings.resetStoryChat
                        )
                    }
                }

                // Dynamic Mood Selector Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${strings.moodLabel}:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    KathaPresets.moods.forEach { preset ->
                        FilterChip(
                            selected = character.currentMood.equals(preset.key, ignoreCase = true),
                            onClick = { onChangeMood(preset.key) },
                            label = {
                                Text(
                                    text = if (strings.isMyanmar) preset.labelMy else preset.labelEn,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("chat_mood_${preset.key}")
                        )
                    }
                    AssistChip(
                        onClick = {
                            customMoodInput = character.currentMood
                            showCustomMoodDialog = true
                        },
                        label = {
                            Text(
                                text = if (strings.isMyanmar) "+ စိတ်ကြိုက်" else "+ Custom",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("chat_custom_mood_chip")
                    )
                }
            }
        }

        // Messages Thread
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat_messages_list")
        ) {
            // Story Scenario Premise Banner
            item(key = "story_premise_header") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = character.storyScenario,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            items(
                items = messages,
                key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}" } }
            ) { msg ->
                ChatMessageBubble(
                    message = msg,
                    character = character,
                    isSpeaking = speakingId == msg.id && msg.id.isNotBlank(),
                    onToggleSpeak = {
                        val utterId = msg.id.ifBlank { "msg_temp" }
                        if (speakingId == utterId) {
                            voiceController.stop()
                        } else {
                            voiceController.speak(
                                utteranceId = utterId,
                                rawText = msg.text,
                                pitch = character.voicePitch.toFloat(),
                                rate = character.voiceRate.toFloat()
                            )
                        }
                    }
                )
            }

            // Streaming / Thinking Bubble
            when (chatGenState) {
                is ChatGenState.Thinking -> {
                    item(key = "ai_thinking") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${character.name}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                is ChatGenState.Streaming -> {
                    item(key = "ai_streaming") {
                        ChatMessageBubble(
                            message = ChatMessage(
                                id = "streaming_live",
                                sender = "ai",
                                text = chatGenState.partialText,
                                mood = character.currentMood
                            ),
                            character = character,
                            isSpeaking = false,
                            onToggleSpeak = {}
                        )
                    }
                }

                is ChatGenState.Error -> {
                    item(key = "ai_error") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = chatGenState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                ChatGenState.Idle -> Unit
            }

            // Active AI Scene Image Generation Feedback (30-60s Shimmer or Error)
            if (chatImageGenState is ImageGenState.Loading) {
                item(key = "chat_image_loading") {
                    ImageGeneratingShimmerBox(promptText = chatImageGenState.prompt)
                }
            } else if (chatImageGenState is ImageGenState.Error) {
                item(key = "chat_image_error") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = chatImageGenState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onResetImageGenState) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }
        }

        // Composer Bar + Attached Photo Preview
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (attachedPhoto != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = attachedPhoto!!.asImageBitmap(),
                            contentDescription = strings.sendPhotoTooltip,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.sendPhotoTooltip,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { attachedPhoto = null }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove photo")
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Send Photo Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("chat_attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.cd_attach_photo),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Generate AI Image Button
                    IconButton(
                        onClick = { showImagePromptDialog = true },
                        enabled = chatImageGenState !is ImageGenState.Loading,
                        modifier = Modifier.testTag("chat_generate_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.cd_generate_image),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Voice Input (Speech-to-Text) Button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE,
                                        if (strings.isMyanmar) "my-MM" else "en-US"
                                    )
                                }
                                speechRecognizerLauncher.launch(intent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.testTag("chat_voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.cd_voice_input),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(strings.chatInputPlaceholder, maxLines = 1) },
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val canSend = (inputText.isNotBlank() || attachedPhoto != null) &&
                        chatGenState !is ChatGenState.Thinking &&
                        chatGenState !is ChatGenState.Streaming

                    IconButton(
                        onClick = {
                            val textToSend = inputText
                            val photoToSend = attachedPhoto
                            inputText = ""
                            attachedPhoto = null
                            onSendMessage(textToSend, photoToSend)
                        },
                        enabled = canSend,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send_message)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    character: CharacterProfile,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit
) {
    val isUser = message.sender == "user"
    val decodedImage = remember(message.imageBase64) {
        ImageCodec.decodeBase64ToBitmap(message.imageBase64)
    }

    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isUser) {
            CharacterAvatarView(
                name = character.name,
                avatarPreset = character.avatarPreset,
                avatarBase64 = character.avatarBase64,
                size = 34.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (decodedImage != null) {
                    Image(
                        bitmap = decodedImage.asImageBitmap(),
                        contentDescription = message.imagePrompt ?: "Chat image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 260.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    RoleplayFormattedText(
                        rawText = message.text,
                        textColor = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        actionColor = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                if (!isUser && message.text.isNotBlank() && message.id != "streaming_live") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = "✦ ${message.mood}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onToggleSpeak,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("speak_msg_${message.id}")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.cd_speak_message),
                                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
