package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.local.LocalPreferencesManager
import com.example.data.local.LocalPrefsState
import com.example.data.model.CharacterProfile
import com.example.data.repository.KathaRepository
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharacterEditorScreen
import com.example.ui.screens.ChatStoryScreen
import com.example.ui.screens.HomeCharactersScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KathaTheme
import com.example.util.BiometricHelper
import com.example.util.EnglishStrings
import com.example.util.LocalAppStrings
import com.example.util.MyanmarStrings
import com.example.util.NetworkMonitor
import com.example.util.VoiceController
import com.example.util.configureAppCheck
import com.example.viewmodel.DataUiState
import com.example.viewmodel.KathaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            KathaRootApp(activity = this)
        }
    }
}

private enum class PrimaryTab {
    STORIES,
    STUDIO,
    SETTINGS
}

private sealed interface SubRoute {
    data object None : SubRoute
    data object CreateCharacter : SubRoute
    data class EditCharacter(val characterId: String) : SubRoute
    data class Chat(val characterId: String) : SubRoute
}

@Composable
fun KathaRootApp(activity: FragmentActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember(context) { LocalPreferencesManager(context.applicationContext) }
    val localPrefs by prefsManager.prefsFlow.collectAsStateWithLifecycle(initialValue = LocalPrefsState())
    val isOnline by NetworkMonitor.observeOnlineStatus(context).collectAsStateWithLifecycle(initialValue = true)

    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val activeStrings = if (localPrefs.languageCode == "en") EnglishStrings else MyanmarStrings

    // Biometric lock session state
    var hasUnlockedThisSession by rememberSaveable { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    val triggerSystemBiometric: () -> Unit = {
        biometricError = null
        BiometricHelper.promptBiometricUnlock(
            activity = activity,
            title = activeStrings.lockScreenTitle,
            subtitle = activeStrings.lockScreenSubtitle,
            onSuccess = { hasUnlockedThisSession = true },
            onError = { msg -> biometricError = msg }
        )
    }

    LaunchedEffect(localPrefs.biometricLockEnabled, currentUser?.uid) {
        if (currentUser != null && localPrefs.biometricLockEnabled && !hasUnlockedThisSession) {
            if (BiometricHelper.canUseSystemBiometric(context)) {
                triggerSystemBiometric()
            }
        }
    }

    CompositionLocalProvider(LocalAppStrings provides activeStrings) {
        KathaTheme(
            themeMode = localPrefs.themeMode,
            accentTheme = localPrefs.accentTheme
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                val user = currentUser
                if (user == null) {
                    // Unauthenticated session: compose AuthScreen only (never compose feature ViewModels)
                    AuthScreen(
                        currentLanguageCode = localPrefs.languageCode,
                        onLanguageChange = { lang ->
                            scope.launch { prefsManager.updateLanguage(lang) }
                        },
                        onAuthSuccess = {}
                    )
                } else if (localPrefs.biometricLockEnabled && !hasUnlockedThisSession) {
                    BiometricLockOverlay(
                        backupPin = localPrefs.backupPin.ifBlank { "1234" },
                        errorMessage = biometricError,
                        onTriggerBiometric = triggerSystemBiometric,
                        onUnlockSuccess = { hasUnlockedThisSession = true },
                        onPinError = { msg -> biometricError = msg }
                    )
                } else {
                    AuthenticatedMainShell(
                        currentUserId = user.uid,
                        userEmail = user.email,
                        prefsManager = prefsManager,
                        isOnline = isOnline
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedMainShell(
    currentUserId: String,
    userEmail: String?,
    prefsManager: LocalPreferencesManager,
    isOnline: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAppStrings.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val voiceController = remember(context) { VoiceController(context) }
    DisposableEffect(voiceController) {
        onDispose { voiceController.shutdown() }
    }

    // ViewModel keyed by currentUserId and configured with custom Firestore database ID
    val viewModel: KathaViewModel = viewModel(
        key = currentUserId,
        factory = KathaViewModel.provideFactory(
            repository = KathaRepository(context.applicationContext),
            prefsManager = prefsManager,
            currentUserId = currentUserId
        )
    )

    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val filteredCharacters by viewModel.filteredCharacters.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGender by viewModel.selectedGenderFilter.collectAsStateWithLifecycle()
    val selectedAgeGroup by viewModel.selectedAgeGroupFilter.collectAsStateWithLifecycle()
    val only18Plus by viewModel.only18PlusFilter.collectAsStateWithLifecycle()
    val activeCharacterId by viewModel.activeCharacterId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val chatGenState by viewModel.chatGenState.collectAsStateWithLifecycle()
    val chatImageGenState by viewModel.chatImageGenState.collectAsStateWithLifecycle()
    val studioImageGenState by viewModel.studioImageGenState.collectAsStateWithLifecycle()
    val autoCraftState by viewModel.autoCraftState.collectAsStateWithLifecycle()
    val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionBanner) {
        if (!actionBanner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(actionBanner!!)
            viewModel.clearBanner()
        }
    }

    var selectedTab by remember { mutableStateOf(PrimaryTab.STORIES) }
    var subRoute by remember { mutableStateOf<SubRoute>(SubRoute.None) }

    val allCharacters = (charactersState as? DataUiState.Success)?.data.orEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 840.dp

        // Full-screen Character Editor route
        val currentSub = subRoute
        if (currentSub is SubRoute.CreateCharacter || currentSub is SubRoute.EditCharacter) {
            val editingProfile: CharacterProfile? = when (currentSub) {
                is SubRoute.EditCharacter -> allCharacters.find { it.id == currentSub.characterId }
                else -> null
            }
            CharacterEditorScreen(
                initialProfile = editingProfile,
                matureModeUnlocked = localPrefs.matureContentEnabled,
                autoCraftState = autoCraftState,
                portraitGenState = studioImageGenState,
                voiceController = voiceController,
                onUnlockMatureMode = { viewModel.updateMatureContentEnabled(true) },
                onAutoCraft = { idea ->
                    viewModel.autoCraftCharacter(idea, preferMyanmar = localPrefs.languageCode == "my")
                },
                onResetAutoCraft = { viewModel.resetAutoCraftState() },
                onGenerateAiPortrait = { prompt ->
                    viewModel.generateStudioImage(prompt, "Cinematic Portrait")
                },
                onSave = { updated ->
                    viewModel.saveCharacter(updated) { savedId ->
                        subRoute = SubRoute.Chat(savedId)
                    }
                },
                onDelete = if (editingProfile != null) {
                    { id ->
                        viewModel.deleteCharacter(id) {
                            subRoute = SubRoute.None
                        }
                    }
                } else null,
                onBack = { subRoute = SubRoute.None }
            )
            return@BoxWithConstraints
        }

        // Handheld full-screen Chat route
        if (!isExpandedLayout && currentSub is SubRoute.Chat) {
            val chatCharacter = allCharacters.find { it.id == currentSub.characterId }
            if (chatCharacter != null) {
                val msgList = (activeMessagesState as? DataUiState.Success)?.data.orEmpty()
                ChatStoryScreen(
                    character = chatCharacter,
                    messagesState = activeMessagesState,
                    chatGenState = chatGenState,
                    chatImageGenState = chatImageGenState,
                    autoVoiceEnabled = localPrefs.autoVoiceEnabled,
                    voiceController = voiceController,
                    showBackButton = true,
                    onBack = { subRoute = SubRoute.None },
                    onEditCharacter = { subRoute = SubRoute.EditCharacter(chatCharacter.id) },
                    onRestartStory = { viewModel.restartCharacterStory(chatCharacter) },
                    onChangeMood = { newMood -> viewModel.changeCharacterMood(chatCharacter.id, newMood) },
                    onSendMessage = { text, photo ->
                        viewModel.sendChatMessage(
                            character = chatCharacter,
                            userText = text,
                            attachedBitmap = photo,
                            history = msgList,
                            onAiReplyFinished = { replyText, charProfile ->
                                if (localPrefs.autoVoiceEnabled) {
                                    voiceController.speak(
                                        utteranceId = "auto_reply",
                                        rawText = replyText,
                                        pitch = charProfile.voicePitch.toFloat(),
                                        rate = charProfile.voiceRate.toFloat()
                                    )
                                }
                            }
                        )
                    },
                    onGenerateSceneImage = { scenePrompt ->
                        viewModel.generateSceneImageInChat(chatCharacter, scenePrompt)
                    },
                    onResetImageGenState = { viewModel.resetChatImageGenState() }
                )
                return@BoxWithConstraints
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isExpandedLayout) {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                        NavigationBarItem(
                            selected = selectedTab == PrimaryTab.STORIES,
                            onClick = { selectedTab = PrimaryTab.STORIES },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == PrimaryTab.STORIES) Icons.Filled.AutoStories else Icons.Outlined.AutoStories,
                                    contentDescription = strings.tabStories
                                )
                            },
                            label = { Text(strings.tabStories) },
                            modifier = Modifier.testTag("nav_tab_stories")
                        )
                        NavigationBarItem(
                            selected = selectedTab == PrimaryTab.STUDIO,
                            onClick = { selectedTab = PrimaryTab.STUDIO },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == PrimaryTab.STUDIO) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                    contentDescription = strings.tabImageStudio
                                )
                            },
                            label = { Text(strings.tabImageStudio) },
                            modifier = Modifier.testTag("nav_tab_studio")
                        )
                        NavigationBarItem(
                            selected = selectedTab == PrimaryTab.SETTINGS,
                            onClick = { selectedTab = PrimaryTab.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == PrimaryTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = strings.tabSettings
                                )
                            },
                            label = { Text(strings.tabSettings) },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedLayout) {
                    NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
                        NavigationRailItem(
                            selected = selectedTab == PrimaryTab.STORIES,
                            onClick = { selectedTab = PrimaryTab.STORIES },
                            icon = { Icon(Icons.Filled.AutoStories, contentDescription = strings.tabStories) },
                            label = { Text(strings.tabStories) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == PrimaryTab.STUDIO,
                            onClick = { selectedTab = PrimaryTab.STUDIO },
                            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = strings.tabImageStudio) },
                            label = { Text(strings.tabImageStudio) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == PrimaryTab.SETTINGS,
                            onClick = { selectedTab = PrimaryTab.SETTINGS },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = strings.tabSettings) },
                            label = { Text(strings.tabSettings) }
                        )
                    }
                }

                when (selectedTab) {
                    PrimaryTab.STORIES -> {
                        if (isExpandedLayout) {
                            // Canonical List-Detail Side-by-Side Layout for Tablets / Foldables
                            HomeCharactersScreen(
                                charactersUiState = charactersState,
                                filteredCharacters = filteredCharacters,
                                localPrefs = localPrefs,
                                isOnline = isOnline,
                                searchQuery = searchQuery,
                                selectedGender = selectedGender,
                                selectedAgeGroup = selectedAgeGroup,
                                only18Plus = only18Plus,
                                activeCharacterId = activeCharacterId,
                                onSearchChange = viewModel::setSearchQuery,
                                onGenderChange = viewModel::setGenderFilter,
                                onAgeGroupChange = viewModel::setAgeGroupFilter,
                                onToggle18PlusFilter = viewModel::setOnly18PlusFilter,
                                onEnableMatureMode = { viewModel.updateMatureContentEnabled(true) },
                                onQuickToggleTheme = {
                                    viewModel.updateThemeMode(if (localPrefs.themeMode == "light") "dark" else "light")
                                },
                                onQuickToggleLanguage = {
                                    viewModel.updateLanguage(if (localPrefs.languageCode == "my") "en" else "my")
                                },
                                onSelectCharacter = { ch ->
                                    viewModel.selectCharacter(ch.id)
                                },
                                onEditCharacter = { ch ->
                                    subRoute = SubRoute.EditCharacter(ch.id)
                                },
                                onCreateCharacter = {
                                    subRoute = SubRoute.CreateCharacter
                                },
                                onLoadStarterStories = viewModel::seedStarterStories,
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight()
                            )

                            VerticalDivider()

                            val activeChar = allCharacters.find { it.id == activeCharacterId }
                                ?: filteredCharacters.firstOrNull()

                            if (activeChar != null) {
                                LaunchedEffect(activeChar.id) {
                                    if (activeCharacterId != activeChar.id) {
                                        viewModel.selectCharacter(activeChar.id)
                                    }
                                }
                                val msgList = (activeMessagesState as? DataUiState.Success)?.data.orEmpty()
                                ChatStoryScreen(
                                    character = activeChar,
                                    messagesState = activeMessagesState,
                                    chatGenState = chatGenState,
                                    chatImageGenState = chatImageGenState,
                                    autoVoiceEnabled = localPrefs.autoVoiceEnabled,
                                    voiceController = voiceController,
                                    showBackButton = false,
                                    onBack = {},
                                    onEditCharacter = { subRoute = SubRoute.EditCharacter(activeChar.id) },
                                    onRestartStory = { viewModel.restartCharacterStory(activeChar) },
                                    onChangeMood = { mood -> viewModel.changeCharacterMood(activeChar.id, mood) },
                                    onSendMessage = { text, photo ->
                                        viewModel.sendChatMessage(
                                            character = activeChar,
                                            userText = text,
                                            attachedBitmap = photo,
                                            history = msgList,
                                            onAiReplyFinished = { replyText, charProfile ->
                                                if (localPrefs.autoVoiceEnabled) {
                                                    voiceController.speak(
                                                        utteranceId = "auto_reply",
                                                        rawText = replyText,
                                                        pitch = charProfile.voicePitch.toFloat(),
                                                        rate = charProfile.voiceRate.toFloat()
                                                    )
                                                }
                                            }
                                        )
                                    },
                                    onGenerateSceneImage = { prompt ->
                                        viewModel.generateSceneImageInChat(activeChar, prompt)
                                    },
                                    onResetImageGenState = viewModel::resetChatImageGenState,
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .fillMaxHeight()
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .fillMaxHeight()
                                ) {
                                    Text(
                                        text = strings.emptyCharactersHint,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            HomeCharactersScreen(
                                charactersUiState = charactersState,
                                filteredCharacters = filteredCharacters,
                                localPrefs = localPrefs,
                                isOnline = isOnline,
                                searchQuery = searchQuery,
                                selectedGender = selectedGender,
                                selectedAgeGroup = selectedAgeGroup,
                                only18Plus = only18Plus,
                                activeCharacterId = activeCharacterId,
                                onSearchChange = viewModel::setSearchQuery,
                                onGenderChange = viewModel::setGenderFilter,
                                onAgeGroupChange = viewModel::setAgeGroupFilter,
                                onToggle18PlusFilter = viewModel::setOnly18PlusFilter,
                                onEnableMatureMode = { viewModel.updateMatureContentEnabled(true) },
                                onQuickToggleTheme = {
                                    viewModel.updateThemeMode(if (localPrefs.themeMode == "light") "dark" else "light")
                                },
                                onQuickToggleLanguage = {
                                    viewModel.updateLanguage(if (localPrefs.languageCode == "my") "en" else "my")
                                },
                                onSelectCharacter = { ch ->
                                    viewModel.selectCharacter(ch.id)
                                    subRoute = SubRoute.Chat(ch.id)
                                },
                                onEditCharacter = { ch ->
                                    subRoute = SubRoute.EditCharacter(ch.id)
                                },
                                onCreateCharacter = {
                                    subRoute = SubRoute.CreateCharacter
                                },
                                onLoadStarterStories = viewModel::seedStarterStories
                            )
                        }
                    }

                    PrimaryTab.STUDIO -> {
                        ImageStudioScreen(
                            studioImageGenState = studioImageGenState,
                            characters = allCharacters,
                            onGenerateImage = viewModel::generateStudioImage,
                            onApplyAvatarToCharacter = viewModel::applyBitmapAsCharacterAvatar
                        )
                    }

                    PrimaryTab.SETTINGS -> {
                        SettingsScreen(
                            localPrefs = localPrefs,
                            userEmail = userEmail,
                            isOnline = isOnline,
                            onLanguageChange = viewModel::updateLanguage,
                            onThemeModeChange = viewModel::updateThemeMode,
                            onAccentThemeChange = viewModel::updateAccentTheme,
                            onDisplayNameChange = viewModel::updateDisplayName,
                            onMatureContentChange = viewModel::updateMatureContentEnabled,
                            onBiometricLockChange = viewModel::updateBiometricLock,
                            onBackupPinChange = viewModel::updateBackupPin,
                            onAutoVoiceChange = viewModel::updateAutoVoiceEnabled,
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {},
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
