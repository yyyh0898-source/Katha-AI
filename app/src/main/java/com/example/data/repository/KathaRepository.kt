package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CharacterProfile
import com.example.data.model.ChatMessage
import com.example.data.model.KathaPresets
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class KathaRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserSettings(userId: String): Flow<UserSettings?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(settings.toUpdateMap()).await()
            } else {
                docRef.set(settings.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCharacters(userId: String): Flow<List<CharacterProfile>> {
        val path = "users/$userId/characters"
        return db.collection("users")
            .document(userId)
            .collection("characters")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CharacterProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending {
                    it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getCharacterById(ownerUserId: String, characterId: String): Result<CharacterProfile?> {
        val docRef = db.collection("users")
            .document(ownerUserId)
            .collection("characters")
            .document(characterId)
        return try {
            val snap = docRef.get().await()
            val character = if (snap.exists()) {
                snap.toObject(
                    CharacterProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(character)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createCharacter(
        profile: CharacterProfile,
        seedGreeting: Boolean = true
    ): Result<String> {
        val uid = requireUserId()
        val charId = UUID.randomUUID().toString()
        val charRef = db.collection("users")
            .document(uid)
            .collection("characters")
            .document(charId)
        return try {
            val initialPreview = profile.lastMessagePreview.ifBlank {
                profile.greetingMessage.take(120)
            }
            val payload = profile.copy(lastMessagePreview = initialPreview).toCreateMap(uid)
            charRef.set(payload).await()

            if (seedGreeting && profile.greetingMessage.isNotBlank()) {
                val msgId = UUID.randomUUID().toString()
                val msgRef = charRef.collection("messages").document(msgId)
                val greetingMsg = ChatMessage(
                    sender = "ai",
                    text = profile.greetingMessage,
                    mood = profile.currentMood,
                    imageType = "none"
                )
                msgRef.set(greetingMsg.toCreateMap(uid, charId)).await()
            }
            Result.success(charId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, charRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCharacter(profile: CharacterProfile): Result<Unit> {
        val uid = requireUserId()
        require(profile.id.isNotBlank()) { "Character ID must not be blank for update" }
        val charRef = db.collection("users")
            .document(uid)
            .collection("characters")
            .document(profile.id)
        return try {
            charRef.update(profile.toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, charRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCharacterMood(characterId: String, mood: String): Result<Unit> {
        val uid = requireUserId()
        val charRef = db.collection("users")
            .document(uid)
            .collection("characters")
            .document(characterId)
        return try {
            charRef.update(
                mapOf(
                    "currentMood" to mood.trim().ifBlank { "Romantic" }.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, charRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCharacter(characterId: String): Result<Unit> {
        val uid = requireUserId()
        val charRef = db.collection("users")
            .document(uid)
            .collection("characters")
            .document(characterId)
        return try {
            val msgsSnap = charRef.collection("messages")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in msgsSnap.documents) {
                doc.reference.delete().await()
            }
            charRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, charRef.path)
            Result.failure(e)
        }
    }

    fun observeMessages(userId: String, characterId: String): Flow<List<ChatMessage>> {
        val path = "users/$userId/characters/$characterId/messages"
        return db.collection("users")
            .document(userId)
            .collection("characters")
            .document(characterId)
            .collection("messages")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy {
                    it.createdAt ?: Timestamp.now()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addMessage(
        characterId: String,
        message: ChatMessage
    ): Result<String> {
        val uid = requireUserId()
        val msgId = UUID.randomUUID().toString()
        val charRef = db.collection("users")
            .document(uid)
            .collection("characters")
            .document(characterId)
        val msgRef = charRef.collection("messages").document(msgId)
        return try {
            msgRef.set(message.toCreateMap(uid, characterId)).await()
            val preview = when {
                message.text.isNotBlank() -> message.text.take(140)
                message.imageType == "ai_generated" -> "[AI Generated Scene]"
                message.imageType == "user_upload" -> "[Sent a Photo]"
                else -> ""
            }
            charRef.update(
                mapOf(
                    "lastMessagePreview" to preview.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(msgId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearMessagesAndResetGreeting(character: CharacterProfile): Result<Unit> {
        val uid = requireUserId()
        val charRef = db.collection("users")
            .document(uid)
            .collection("characters")
            .document(character.id)
        return try {
            val msgsSnap = charRef.collection("messages")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in msgsSnap.documents) {
                doc.reference.delete().await()
            }
            if (character.greetingMessage.isNotBlank()) {
                val msgId = UUID.randomUUID().toString()
                charRef.collection("messages").document(msgId).set(
                    ChatMessage(
                        sender = "ai",
                        text = character.greetingMessage,
                        mood = character.currentMood,
                        imageType = "none"
                    ).toCreateMap(uid, character.id)
                ).await()
            }
            charRef.update(
                mapOf(
                    "lastMessagePreview" to character.greetingMessage.take(140),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "${charRef.path}/messages")
            Result.failure(e)
        }
    }

    suspend fun seedStarterCharactersIfEmpty(): Result<Unit> {
        val uid = requireUserId()
        val charsRef = db.collection("users").document(uid).collection("characters")
        return try {
            val existing = charsRef.whereEqualTo("userId", uid).limit(1).get().await()
            if (existing.isEmpty) {
                for (starter in KathaPresets.starterCharacters()) {
                    createCharacter(starter, seedGreeting = true)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, charsRef.path)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "KathaRepository"

        fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(databaseId)
            try {
                val settings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                firestore.firestoreSettings = settings
            } catch (e: IllegalStateException) {
                // Settings already locked by an earlier getInstance() call in this process; safe to ignore
                Log.d(TAG, "Firestore settings already initialized: ${e.message}")
            }
            return firestore
        }
    }
}
