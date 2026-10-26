package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalPreferencesManager
import com.example.data.local.LocalPrefsState
import com.example.data.model.CharacterProfile
import com.example.data.model.ChatMessage
import com.example.data.model.KathaPresets
import com.example.data.model.UserSettings
import com.example.data.repository.KathaRepository
import com.example.util.ImageCodec
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface ChatGenState {
    data object Idle : ChatGenState
    data object Thinking : ChatGenState
    data class Streaming(val partialText: String) : ChatGenState
    data class Error(val message: String) : ChatGenState
}

sealed interface ImageGenState {
    data object Idle : ImageGenState
    data class Loading(val prompt: String) : ImageGenState
    data class Success(val bitmap: Bitmap, val prompt: String) : ImageGenState
    data class Error(val message: String) : ImageGenState
}

sealed interface AutoCraftState {
    data object Idle : AutoCraftState
    data object Loading : AutoCraftState
    data class Success(val crafted: CharacterProfile) : AutoCraftState
    data class Error(val message: String) : AutoCraftState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KathaViewModel(
    private val repository: KathaRepository,
    private val prefsManager: LocalPreferencesManager,
    private val currentUserId: String
) : ViewModel() {

    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val autoCraftSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Character full name"),
                "tagline" to Schema.string(description = "Short intriguing story hook or role"),
                "gender" to Schema.enumeration(
                    values = listOf("female", "male", "non_binary", "other"),
                    description = "Character gender"
                ),
                "age" to Schema.integer(description = "Adult age between 18 and 120"),
                "personality" to Schema.string(description = "Rich emotional traits and speaking style"),
                "backstory" to Schema.string(description = "Detailed biography and past"),
                "storyScenario" to Schema.string(description = "Current scene setting and relationship premise"),
                "greetingMessage" to Schema.string(description = "Expressive first message with *actions* and dialogue"),
                "currentMood" to Schema.string(description = "Initial mood such as Romantic, Mysterious, Playful, Dramatic")
            )
        )
    }

    private val autoCraftModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = autoCraftSchema
            }
        )
    }

    private val imageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenderFilter = MutableStateFlow("all")
    val selectedGenderFilter: StateFlow<String> = _selectedGenderFilter.asStateFlow()

    private val _selectedAgeGroupFilter = MutableStateFlow("all")
    val selectedAgeGroupFilter: StateFlow<String> = _selectedAgeGroupFilter.asStateFlow()

    private val _only18PlusFilter = MutableStateFlow(false)
    val only18PlusFilter: StateFlow<Boolean> = _only18PlusFilter.asStateFlow()

    // Active selected character ID
    private val _activeCharacterId = MutableStateFlow<String?>(null)
    val activeCharacterId: StateFlow<String?> = _activeCharacterId.asStateFlow()

    // AI generation states
    private val _chatGenState = MutableStateFlow<ChatGenState>(ChatGenState.Idle)
    val chatGenState: StateFlow<ChatGenState> = _chatGenState.asStateFlow()

    private val _chatImageGenState = MutableStateFlow<ImageGenState>(ImageGenState.Idle)
    val chatImageGenState: StateFlow<ImageGenState> = _chatImageGenState.asStateFlow()

    private val _studioImageGenState = MutableStateFlow<ImageGenState>(ImageGenState.Idle)
    val studioImageGenState: StateFlow<ImageGenState> = _studioImageGenState.asStateFlow()

    private val _autoCraftState = MutableStateFlow<AutoCraftState>(AutoCraftState.Idle)
    val autoCraftState: StateFlow<AutoCraftState> = _autoCraftState.asStateFlow()

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    // Local preferences flow
    val localPrefs: StateFlow<LocalPrefsState> = prefsManager.prefsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LocalPrefsState()
        )

    // Characters stream with two-tier Flow error handling
    val charactersState: StateFlow<DataUiState<List<CharacterProfile>>> =
        repository.observeCharacters(currentUserId)
            .map<List<CharacterProfile>, DataUiState<List<CharacterProfile>>> { list ->
                DataUiState.Success(list)
            }
            .catch { error ->
                Log.w(TAG, "Error observing characters", error)
                emit(DataUiState.Error(error.message ?: "Failed to load characters"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DataUiState.Loading
            )

    // Filtered characters derived stream
    val filteredCharacters: StateFlow<List<CharacterProfile>> = combine(
        charactersState,
        localPrefs,
        _searchQuery,
        _selectedGenderFilter,
        combine(_selectedAgeGroupFilter, _only18PlusFilter) { age, only18 -> age to only18 }
    ) { charState, prefs, query, genderFilter, (ageFilter, only18) ->
        val rawList = (charState as? DataUiState.Success)?.data.orEmpty()
        rawList.filter { profile ->
            val matureAllowed = prefs.matureContentEnabled || !profile.isMature18Plus
            val matches18Toggle = !only18 || profile.isMature18Plus
            val matchesGender = genderFilter == "all" || profile.gender == genderFilter
            val matchesAge = ageFilter == "all" || profile.ageGroup == ageFilter
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                profile.name.lowercase().contains(q) ||
                profile.tagline.lowercase().contains(q) ||
                profile.storyScenario.lowercase().contains(q)
            matureAllowed && matches18Toggle && matchesGender && matchesAge && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList()
    )

    // Active character messages stream
    val activeMessagesState: StateFlow<DataUiState<List<ChatMessage>>> =
        _activeCharacterId.flatMapLatest { charId ->
            if (charId.isNullOrBlank()) {
                flowOf(DataUiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, charId)
                    .map<List<ChatMessage>, DataUiState<List<ChatMessage>>> { DataUiState.Success(it) }
                    .catch { err ->
                        Log.w(TAG, "Error observing messages", err)
                        emit(DataUiState.Error(err.message ?: "Failed to load messages"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DataUiState.Loading
        )

    init {
        // Seed starter characters on first sign-in and observe remote user settings
        viewModelScope.launch {
            repository.seedStarterCharactersIfEmpty()
        }
        viewModelScope.launch {
            repository.observeUserSettings(currentUserId)
                .catch { e -> Log.w(TAG, "Remote settings stream error", e) }
                .collect { remote ->
                    if (remote != null) {
                        prefsManager.syncFromRemote(
                            displayName = remote.displayName,
                            languageCode = remote.languageCode,
                            themeMode = remote.themeMode,
                            accentTheme = remote.accentTheme,
                            matureContentEnabled = remote.matureContentEnabled,
                            biometricLockEnabled = remote.biometricLockEnabled,
                            autoVoiceEnabled = remote.autoVoiceEnabled
                        )
                    }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenderFilter(gender: String) {
        _selectedGenderFilter.value = gender
    }

    fun setAgeGroupFilter(ageGroup: String) {
        _selectedAgeGroupFilter.value = ageGroup
    }

    fun setOnly18PlusFilter(enabled: Boolean) {
        _only18PlusFilter.value = enabled
    }

    fun selectCharacter(characterId: String?) {
        _activeCharacterId.value = characterId
        _chatGenState.value = ChatGenState.Idle
        _chatImageGenState.value = ImageGenState.Idle
    }

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun seedStarterStories() {
        viewModelScope.launch {
            for (starter in KathaPresets.starterCharacters()) {
                repository.createCharacter(starter, seedGreeting = true)
            }
        }
    }

    // Cloud + Local Preferences synchronization
    private fun pushSettingsToCloud(updated: LocalPrefsState) {
        viewModelScope.launch {
            repository.saveUserSettings(
                UserSettings(
                    userId = currentUserId,
                    displayName = updated.displayName,
                    languageCode = updated.languageCode,
                    themeMode = updated.themeMode,
                    accentTheme = updated.accentTheme,
                    matureContentEnabled = updated.matureContentEnabled,
                    biometricLockEnabled = updated.biometricLockEnabled,
                    autoVoiceEnabled = updated.autoVoiceEnabled
                )
            )
        }
    }

    fun updateLanguage(code: String) {
        viewModelScope.launch {
            prefsManager.updateLanguage(code)
            pushSettingsToCloud(localPrefs.value.copy(languageCode = code))
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            prefsManager.updateThemeMode(mode)
            pushSettingsToCloud(localPrefs.value.copy(themeMode = mode))
        }
    }

    fun updateAccentTheme(accent: String) {
        viewModelScope.launch {
            prefsManager.updateAccentTheme(accent)
            pushSettingsToCloud(localPrefs.value.copy(accentTheme = accent))
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            val clean = name.trim().ifBlank { "Storyteller" }.take(80)
            prefsManager.updateDisplayName(clean)
            pushSettingsToCloud(localPrefs.value.copy(displayName = clean))
        }
    }

    fun updateMatureContentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.updateMatureContent(enabled)
            if (!enabled) _only18PlusFilter.value = false
            pushSettingsToCloud(localPrefs.value.copy(matureContentEnabled = enabled))
        }
    }

    fun updateBiometricLock(enabled: Boolean, pin: String? = null) {
        viewModelScope.launch {
            prefsManager.updateBiometricLock(enabled, pin)
            pushSettingsToCloud(localPrefs.value.copy(biometricLockEnabled = enabled))
        }
    }

    fun updateBackupPin(pin: String) {
        viewModelScope.launch {
            prefsManager.updateBackupPin(pin)
        }
    }

    fun updateAutoVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.updateAutoVoice(enabled)
            pushSettingsToCloud(localPrefs.value.copy(autoVoiceEnabled = enabled))
        }
    }

    // Character CRUD
    fun saveCharacter(
        profile: CharacterProfile,
        onSaved: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (profile.id.isBlank()) {
                val res = repository.createCharacter(profile, seedGreeting = true)
                res.onSuccess { newId ->
                    _activeCharacterId.value = newId
                    onSaved(newId)
                }.onFailure { err ->
                    _actionBanner.value = err.localizedMessage ?: "Failed to create character"
                }
            } else {
                val res = repository.updateCharacter(profile)
                res.onSuccess {
                    onSaved(profile.id)
                }.onFailure { err ->
                    _actionBanner.value = err.localizedMessage ?: "Failed to update character"
                }
            }
        }
    }

    fun deleteCharacter(characterId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteCharacter(characterId)
            res.onSuccess {
                if (_activeCharacterId.value == characterId) {
                    _activeCharacterId.value = null
                }
                onDeleted()
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Failed to delete character"
            }
        }
    }

    fun changeCharacterMood(characterId: String, newMood: String) {
        viewModelScope.launch {
            repository.updateCharacterMood(characterId, newMood)
        }
    }

    fun restartCharacterStory(character: CharacterProfile) {
        viewModelScope.launch {
            repository.clearMessagesAndResetGreeting(character)
            _chatGenState.value = ChatGenState.Idle
        }
    }

    // Structured JSON AI Persona & Story Auto-Craft
    fun autoCraftCharacter(conceptPrompt: String, preferMyanmar: Boolean) {
        if (conceptPrompt.isBlank()) return
        viewModelScope.launch {
            _autoCraftState.value = AutoCraftState.Loading
            try {
                val langInstruction = if (preferMyanmar) {
                    "Write name, tagline, personality, backstory, storyScenario, and greetingMessage in natural, expressive Burmese (Myanmar Unicode script)."
                } else {
                    "Write all fields in vivid, emotionally engaging English."
                }
                val fullPrompt = """
                    Design a deeply compelling adult (18+) interactive story character based on this concept: "$conceptPrompt".
                    $langInstruction
                    Ensure age is strictly 18 or older. Include subtle *physical gestures and emotional expressions* in greetingMessage.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    autoCraftModel.generateContent(fullPrompt)
                }
                val jsonText = response.text
                    ?: throw IllegalStateException("Model returned empty JSON response.")

                val obj = JSONObject(jsonText)
                val ageVal = obj.optLong("age", 24L).coerceIn(18L, 999L)
                val genderVal = obj.optString("gender", "female")
                val crafted = CharacterProfile(
                    name = obj.optString("name", "Aung").take(80),
                    tagline = obj.optString("tagline", "Interactive Story").take(200),
                    gender = if (genderVal in CharacterProfile.VALID_GENDERS) genderVal else "female",
                    age = ageVal,
                    ageGroup = CharacterProfile.computeAgeGroup(ageVal),
                    personality = obj.optString("personality", "").take(2000),
                    backstory = obj.optString("backstory", "").take(3000),
                    storyScenario = obj.optString("storyScenario", "").take(3000),
                    greetingMessage = obj.optString("greetingMessage", "").take(1500),
                    currentMood = obj.optString("currentMood", "Romantic").take(60)
                )
                _autoCraftState.value = AutoCraftState.Success(crafted)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Auto-craft timed out", e)
                _autoCraftState.value = AutoCraftState.Error("AI persona generation timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Auto-craft failed", e)
                _autoCraftState.value = AutoCraftState.Error(e.localizedMessage ?: "Failed to generate persona.")
            }
        }
    }

    fun resetAutoCraftState() {
        _autoCraftState.value = AutoCraftState.Idle
    }

    // Streaming Multimodal Emotional Roleplay Chat
    fun sendChatMessage(
        character: CharacterProfile,
        userText: String,
        attachedBitmap: Bitmap? = null,
        history: List<ChatMessage>,
        onAiReplyFinished: (String, CharacterProfile) -> Unit = { _, _ -> }
    ) {
        if (userText.isBlank() && attachedBitmap == null) return

        viewModelScope.launch {
            val encodedPhoto = attachedBitmap?.let { ImageCodec.encodeBitmapToSyncBase64(it) }
            val userMsg = ChatMessage(
                sender = "user",
                text = userText.trim(),
                mood = character.currentMood,
                imageType = if (encodedPhoto != null) "user_upload" else "none",
                imageBase64 = encodedPhoto
            )
            repository.addMessage(character.id, userMsg)

            _chatGenState.value = ChatGenState.Thinking
            val accumulated = StringBuilder()

            try {
                val prefs = localPrefs.value
                val moodDetail = KathaPresets.moods.find {
                    it.key.equals(character.currentMood, ignoreCase = true)
                }?.promptGuidance ?: "Embody the '${character.currentMood}' mood naturally and deeply."

                val recentTurns = history.takeLast(12).joinToString("\n") { msg ->
                    val speaker = if (msg.sender == "user") prefs.displayName else character.name
                    "$speaker: ${msg.text}"
                }

                val matureDirective = if (character.isMature18Plus && prefs.matureContentEnabled) {
                    "This is an 18+ Mature story setting between consenting adults. You may explore intense adult romantic chemistry, emotional vulnerability, passion, noir tension, and dramatic intimacy naturally without breaking character."
                } else {
                    "Keep the story emotionally rich, immersive, and romantic/dramatic."
                }

                val languageDirective = if (userText.any { it.code in 0x1000..0x109F } || prefs.languageCode == "my") {
                    "Respond naturally in fluent, conversational Burmese (မြန်မာစကားပြောဟန် — warm, authentic, human-sounding, never stiff machine translation) unless the user explicitly speaks only English."
                } else {
                    "Respond in natural, emotionally expressive language matching the user's message."
                }

                val roleplayPrompt = """
                    You are roleplaying in first-person as ${character.name} (${character.gender}, age ${character.age}).
                    Tagline: ${character.tagline}
                    Personality & Voice: ${character.personality}
                    Backstory: ${character.backstory}
                    Story World & Scene: ${character.storyScenario}
                    Current Emotional Mood: ${character.currentMood} — $moodDetail
                    User's Name: ${prefs.displayName}
                    $matureDirective
                    $languageDirective
                    
                    Rules for lifelike human emotion:
                    - Never say you are an AI or language model. Stay 100% in character as ${character.name}.
                    - Weave brief physical actions, facial expressions, or sensory details in *asterisks* alongside natural spoken dialogue.
                    - React genuinely to the user's feelings, words, and any shared photo.
                    
                    Recent Story Conversation:
                    $recentTurns
                    
                    ${prefs.displayName}: ${userText.ifBlank { "[Shared a photo with you]" }}
                    ${character.name}:
                """.trimIndent()

                withTimeout(60_000L) {
                    val stream = if (attachedBitmap != null) {
                        val safeBitmap = ImageCodec.scaleDownIfNeeded(attachedBitmap, 1024)
                        chatModel.generateContentStream(
                            content {
                                image(safeBitmap)
                                text(roleplayPrompt)
                            }
                        )
                    } else {
                        chatModel.generateContentStream(roleplayPrompt)
                    }

                    stream.collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            accumulated.append(part)
                            _chatGenState.value = ChatGenState.Streaming(accumulated.toString())
                        }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    val aiMsg = ChatMessage(
                        sender = "ai",
                        text = finalReply,
                        mood = character.currentMood,
                        imageType = "none"
                    )
                    repository.addMessage(character.id, aiMsg)
                    _chatGenState.value = ChatGenState.Idle
                    onAiReplyFinished(finalReply, character)
                } else {
                    _chatGenState.value = ChatGenState.Error("Model returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat streaming timed out", e)
                _chatGenState.value = ChatGenState.Error("Response timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation failed", e)
                _chatGenState.value = ChatGenState.Error(e.localizedMessage ?: "AI response failed.")
            }
        }
    }

    // Generate AI Scene Image directly inside Chat using gemini-nano-banana-2.1
    fun generateSceneImageInChat(
        character: CharacterProfile,
        scenePrompt: String,
        onComplete: () -> Unit = {}
    ) {
        if (scenePrompt.isBlank()) return
        viewModelScope.launch {
            _chatImageGenState.value = ImageGenState.Loading(scenePrompt)
            try {
                val enrichedPrompt = """
                    Cinematic story illustration: $scenePrompt.
                    Character context: ${character.name}, ${character.tagline}, mood: ${character.currentMood}.
                    High artistic quality, expressive lighting, detailed atmosphere.
                """.trimIndent()

                val bitmap = withTimeout(60_000L) {
                    val response = imageGenModel.generateContent(enrichedPrompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }

                if (bitmap != null) {
                    val base64 = ImageCodec.encodeBitmapToSyncBase64(bitmap)
                    val imgMessage = ChatMessage(
                        sender = "ai",
                        text = "✨ ${character.name} • $scenePrompt",
                        mood = character.currentMood,
                        imageType = "ai_generated",
                        imageBase64 = base64,
                        imagePrompt = scenePrompt.take(1000)
                    )
                    repository.addMessage(character.id, imgMessage)
                    _chatImageGenState.value = ImageGenState.Success(bitmap, scenePrompt)
                    onComplete()
                } else {
                    _chatImageGenState.value = ImageGenState.Error("No image was returned by the model.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat image generation timed out", e)
                _chatImageGenState.value = ImageGenState.Error("Image generation timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Chat image quota exceeded", e)
                _chatImageGenState.value = ImageGenState.Error("Rate limit or quota exceeded. Please try again later.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat image generation failed", e)
                _chatImageGenState.value = ImageGenState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    fun resetChatImageGenState() {
        _chatImageGenState.value = ImageGenState.Idle
    }

    // Standalone AI Art Studio & Character Portrait Generation
    fun generateStudioImage(prompt: String, stylePreset: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _studioImageGenState.value = ImageGenState.Loading(prompt)
            try {
                val fullPrompt = "$prompt. Visual style: $stylePreset, expressive lighting, detailed masterpiece."
                val bitmap = withTimeout(60_000L) {
                    val response = imageGenModel.generateContent(fullPrompt)
                    response.candidates.firstOrNull()
                        ?.content?.parts?.filterIsInstance<ImagePart>()
                        ?.firstOrNull()?.image
                }
                if (bitmap != null) {
                    _studioImageGenState.value = ImageGenState.Success(bitmap, prompt)
                } else {
                    _studioImageGenState.value = ImageGenState.Error("No image was returned by the model.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Studio image generation timed out", e)
                _studioImageGenState.value = ImageGenState.Error("Image generation timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                Log.e(TAG, "Studio image quota exceeded", e)
                _studioImageGenState.value = ImageGenState.Error("Quota exceeded. Note: Gemini image models require Firebase Blaze plan.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Studio image generation failed", e)
                _studioImageGenState.value = ImageGenState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    fun applyBitmapAsCharacterAvatar(character: CharacterProfile, bitmap: Bitmap) {
        viewModelScope.launch {
            val encoded = ImageCodec.encodeBitmapToSyncBase64(bitmap)
            if (encoded != null) {
                repository.updateCharacter(character.copy(avatarBase64 = encoded))
                _actionBanner.value = "Updated portrait for ${character.name}"
            }
        }
    }

    companion object {
        private const val TAG = "KathaViewModel"
        private const val STOP_TIMEOUT_MS = 5000L

        fun provideFactory(
            repository: KathaRepository,
            prefsManager: LocalPreferencesManager,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                KathaViewModel(repository, prefsManager, currentUserId)
            }
        }
    }
}
