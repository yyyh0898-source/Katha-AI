package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CharacterProfile
import com.example.data.model.ChatMessage
import com.example.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KathaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createCharacter_and_observeCharacters_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = KathaRepository(firestore)

        val profile = CharacterProfile(
            name = "May Thu",
            tagline = "Yangon Bookstore Companion",
            gender = "female",
            age = 24L,
            ageGroup = "18_24",
            personality = "Warm and poetic",
            backstory = "Loves rainy nights and coffee.",
            storyScenario = "Rainy evening in downtown Yangon.",
            greetingMessage = "မင်္ဂလာပါ... ကော်ဖီသောက်မလားဟင်?",
            currentMood = "Romantic",
            isMature18Plus = false,
            avatarPreset = "velvet_poet"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCharacter(profile, seedGreeting = true)
        }
        assertTrue("Expected character creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val characterId = createResult.getOrThrow()
        assertTrue(characterId.isNotEmpty())

        val characters = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCharacters(aliceUid).first { list -> list.any { it.id == characterId } }
        }
        assertTrue(characters.any { it.id == characterId && it.name == "May Thu" })
    }

    @Test
    fun addMessage_and_observeMessages_authenticatedOwner_emitsMessages() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = KathaRepository(firestore)

        val charId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCharacter(
                CharacterProfile(
                    name = "Zan Min",
                    tagline = "Noir Lounge Host",
                    gender = "male",
                    age = 29L,
                    ageGroup = "25_34",
                    personality = "Calm and charismatic",
                    backstory = "Owns a rooftop lounge.",
                    storyScenario = "Midnight conversation.",
                    greetingMessage = "Welcome to the rooftop.",
                    currentMood = "Passionate",
                    isMature18Plus = true,
                    avatarPreset = "crimson_detective"
                ),
                seedGreeting = true
            ).getOrThrow()
        }

        val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addMessage(
                characterId = charId,
                message = ChatMessage(
                    sender = "user",
                    text = "ဒီည ရာသီဥတုလေး အေးချမ်းတယ်နော်",
                    mood = "Passionate",
                    imageType = "none"
                )
            )
        }
        assertTrue("Expected message creation to succeed: ${msgResult.exceptionOrNull()}", msgResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(aliceUid, charId).first { list -> list.size >= 2 }
        }
        assertTrue(messages.any { it.text.contains("အေးချမ်းတယ်နော်") })
    }

    @Test
    fun saveUserSettings_and_observeUserSettings_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = KathaRepository(firestore)

        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettings(
                    displayName = "Ko Aung",
                    languageCode = "my",
                    themeMode = "dark",
                    accentTheme = "midnight_violet",
                    matureContentEnabled = true,
                    biometricLockEnabled = true,
                    autoVoiceEnabled = false
                )
            )
        }
        assertTrue("Expected saveUserSettings to succeed: ${saveRes.exceptionOrNull()}", saveRes.isSuccess)

        val loaded = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserSettings(aliceUid).first { it != null }
        }
        assertNotNull(loaded)
        assertEquals("Ko Aung", loaded?.displayName)
        assertEquals("midnight_violet", loaded?.accentTheme)
    }

    @Test
    fun getCharacterById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = KathaRepository(firestore)
        val charId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCharacter(
                CharacterProfile(
                    name = "Private Story",
                    tagline = "Secret",
                    gender = "female",
                    age = 25L,
                    ageGroup = "25_34",
                    personality = "Private",
                    backstory = "Private",
                    storyScenario = "Private",
                    greetingMessage = "Hello",
                    currentMood = "Mysterious"
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = KathaRepository(firestore)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCharacterById(aliceUid, charId)
        }
        assertTrue("Expected cross-user access to fail", bobResult.isFailure)
        val fsEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeCharacters_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = KathaRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCharacters(aliceUid).first()
            }
            fail("Expected unauthenticated observeCharacters to throw")
        } catch (e: Exception) {
            caught = e
        }

        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
