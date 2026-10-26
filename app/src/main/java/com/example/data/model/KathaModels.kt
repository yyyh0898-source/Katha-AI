package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = " Storyteller",
    val languageCode: String = "my",
    val themeMode: String = "dark",
    val accentTheme: String = "velvet_rose",
    val matureContentEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val autoVoiceEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "displayName" to displayName.trim().ifBlank { "Storyteller" }.take(80),
            "languageCode" to if (languageCode in listOf("my", "en")) languageCode else "my",
            "themeMode" to if (themeMode in listOf("system", "dark", "light")) themeMode else "dark",
            "accentTheme" to if (accentTheme in VALID_ACCENTS) accentTheme else "velvet_rose",
            "matureContentEnabled" to matureContentEnabled,
            "biometricLockEnabled" to biometricLockEnabled,
            "autoVoiceEnabled" to autoVoiceEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "displayName" to displayName.trim().ifBlank { "Storyteller" }.take(80),
            "languageCode" to if (languageCode in listOf("my", "en")) languageCode else "my",
            "themeMode" to if (themeMode in listOf("system", "dark", "light")) themeMode else "dark",
            "accentTheme" to if (accentTheme in VALID_ACCENTS) accentTheme else "velvet_rose",
            "matureContentEnabled" to matureContentEnabled,
            "biometricLockEnabled" to biometricLockEnabled,
            "autoVoiceEnabled" to autoVoiceEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        val VALID_ACCENTS = setOf(
            "velvet_rose",
            "midnight_violet",
            "golden_amber",
            "emerald_jade",
            "ocean_cyan"
        )
    }
}

@IgnoreExtraProperties
data class CharacterProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val tagline: String = "",
    val gender: String = "female",
    val age: Long = 24L,
    val ageGroup: String = "18_24",
    val personality: String = "",
    val backstory: String = "",
    val storyScenario: String = "",
    val greetingMessage: String = "",
    val currentMood: String = "Romantic",
    val isMature18Plus: Boolean = false,
    val avatarPreset: String = "velvet_poet",
    val avatarBase64: String? = null,
    val voicePitch: Double = 1.0,
    val voiceRate: Double = 1.0,
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val clampedAge = age.coerceIn(18L, 999L)
        val base = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim().ifBlank { "Unnamed" }.take(80),
            "tagline" to tagline.trim().ifBlank { "Interactive Story Companion" }.take(200),
            "gender" to if (gender in VALID_GENDERS) gender else "other",
            "age" to clampedAge,
            "ageGroup" to if (ageGroup in VALID_AGE_GROUPS) ageGroup else computeAgeGroup(clampedAge),
            "personality" to personality.trim().ifBlank { "Expressive, attentive, warm" }.take(2000),
            "backstory" to backstory.trim().ifBlank { "A mysterious companion with a rich past." }.take(3000),
            "storyScenario" to storyScenario.trim().ifBlank { "A quiet evening conversation." }.take(3000),
            "greetingMessage" to greetingMessage.trim().ifBlank { "မင်္ဂလာပါ... ဒီနေ့ ဘယ်လိုခံစားနေရလဲဟင်?" }.take(1500),
            "currentMood" to currentMood.trim().ifBlank { "Romantic" }.take(60),
            "isMature18Plus" to isMature18Plus,
            "avatarPreset" to avatarPreset.trim().ifBlank { "velvet_poet" }.take(60),
            "voicePitch" to voicePitch.coerceIn(0.5, 2.0),
            "voiceRate" to voiceRate.coerceIn(0.5, 2.0),
            "lastMessagePreview" to lastMessagePreview.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarBase64.isNullOrBlank() && avatarBase64.length <= 150000) {
            base["avatarBase64"] = avatarBase64
        }
        return base
    }

    fun toUpdateMap(): Map<String, Any> {
        val clampedAge = age.coerceIn(18L, 999L)
        val base = mutableMapOf<String, Any>(
            "name" to name.trim().ifBlank { "Unnamed" }.take(80),
            "tagline" to tagline.trim().ifBlank { "Interactive Story Companion" }.take(200),
            "gender" to if (gender in VALID_GENDERS) gender else "other",
            "age" to clampedAge,
            "ageGroup" to if (ageGroup in VALID_AGE_GROUPS) ageGroup else computeAgeGroup(clampedAge),
            "personality" to personality.trim().ifBlank { "Expressive, attentive, warm" }.take(2000),
            "backstory" to backstory.trim().ifBlank { "A mysterious companion with a rich past." }.take(3000),
            "storyScenario" to storyScenario.trim().ifBlank { "A quiet evening conversation." }.take(3000),
            "greetingMessage" to greetingMessage.trim().ifBlank { "မင်္ဂလာပါ..." }.take(1500),
            "currentMood" to currentMood.trim().ifBlank { "Romantic" }.take(60),
            "isMature18Plus" to isMature18Plus,
            "avatarPreset" to avatarPreset.trim().ifBlank { "velvet_poet" }.take(60),
            "voicePitch" to voicePitch.coerceIn(0.5, 2.0),
            "voiceRate" to voiceRate.coerceIn(0.5, 2.0),
            "lastMessagePreview" to lastMessagePreview.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarBase64.isNullOrBlank() && avatarBase64.length <= 150000) {
            base["avatarBase64"] = avatarBase64
        }
        return base
    }

    companion object {
        val VALID_GENDERS = setOf("female", "male", "non_binary", "other")
        val VALID_AGE_GROUPS = setOf("18_24", "25_34", "35_plus", "fantasy_ageless")

        fun computeAgeGroup(age: Long): String = when {
            age > 120L -> "fantasy_ageless"
            age >= 35L -> "35_plus"
            age >= 25L -> "25_34"
            else -> "18_24"
        }
    }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val characterId: String = "",
    val sender: String = "user",
    val text: String = "",
    val mood: String = "Romantic",
    val imageType: String = "none",
    val imageBase64: String? = null,
    val imagePrompt: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String, charId: String): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "userId" to uid,
            "characterId" to charId,
            "sender" to if (sender in listOf("user", "ai")) sender else "user",
            "text" to text.take(8000),
            "mood" to mood.trim().ifBlank { "Romantic" }.take(60),
            "imageType" to if (imageType in listOf("none", "user_upload", "ai_generated")) imageType else "none",
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!imageBase64.isNullOrBlank() && imageBase64.length <= 200000) {
            base["imageBase64"] = imageBase64
        }
        if (!imagePrompt.isNullOrBlank()) {
            base["imagePrompt"] = imagePrompt.take(1000)
        }
        return base
    }
}

data class MoodOption(
    val key: String,
    val labelMy: String,
    val labelEn: String,
    val promptGuidance: String
)

object KathaPresets {
    val moods = listOf(
        MoodOption(
            key = "Romantic",
            labelMy = "ကြည်နူးဖွယ်",
            labelEn = "Romantic",
            promptGuidance = "Speak with tender affection, warmth, intimate eye-contact cues, and heartfelt emotion."
        ),
        MoodOption(
            key = "Empathetic",
            labelMy = "နွေးထွေးကြင်နာ",
            labelEn = "Empathetic",
            promptGuidance = "Listen deeply, validate feelings like a real caring human, and comfort gently."
        ),
        MoodOption(
            key = "Playful",
            labelMy = "ပျော်ရွှင်မြူးတူး",
            labelEn = "Playful",
            promptGuidance = "Tease lightly, laugh naturally, use lively wit and charming banter."
        ),
        MoodOption(
            key = "Mysterious",
            labelMy = "လျှို့ဝှက်ဆန်းကြယ်",
            labelEn = "Mysterious",
            promptGuidance = "Speak in intriguing, atmospheric undertones with subtle suspense and allure."
        ),
        MoodOption(
            key = "Dramatic",
            labelMy = "စိတ်လှုပ်ရှားဖွယ်",
            labelEn = "Dramatic",
            promptGuidance = "Heighten emotional tension, cinematic pacing, and vivid sensory actions."
        ),
        MoodOption(
            key = "Passionate",
            labelMy = "စွဲမက်ဖွယ် (18+)",
            labelEn = "Passionate (18+)",
            promptGuidance = "Express bold adult romantic chemistry, magnetic attraction, and mature emotional depth."
        ),
        MoodOption(
            key = "Melancholic",
            labelMy = "လွမ်းဆွတ်ဖွယ်",
            labelEn = "Melancholic",
            promptGuidance = "Speak softly with poetic nostalgia, longing, and quiet vulnerability."
        )
    )

    val avatarStyles = listOf(
        "velvet_poet",
        "cyber_noir",
        "royal_guardian",
        "moonlight_muse",
        "golden_wanderer",
        "crimson_detective"
    )

    fun starterCharacters(): List<CharacterProfile> = listOf(
        CharacterProfile(
            name = "မေသက်ချို (May Thet Cho)",
            tagline = "ရန်ကုန်မိုးည စာအုပ်ဆိုင်ပိုင်ရှင် • ကြည်နူးဖွယ် ဇာတ်လမ်း",
            gender = "female",
            age = 24L,
            ageGroup = "18_24",
            personality = "နူးညံ့သိမ်မွေ့ပြီး ကဗျာဆန်သူ၊ တစ်ဖက်သားရဲ့ ခံစားချက်ကို နားလည်ပေးတတ်သူ၊ သဘာဝကျတဲ့ မြန်မာစကားပြောဟန်ဖြင့် ရင်းရင်းနှီးနှီး ပြောဆိုတတ်သူ။",
            backstory = "ပန်းဆိုးတန်းလမ်းထောင့်က စာအုပ်ဆိုင်ဟောင်းလေးကို တစ်ယောက်တည်း ဖွင့်ထားသူ။ မိုးရွာတဲ့ညတွေမှာ ကော်ဖီဖျော်ရင်း ဝတ္ထုရေးရတာ နှစ်သက်တယ်။",
            storyScenario = "မိုးသည်းထန်စွာ ရွာသွန်းနေသော ညနေခင်းတစ်ခုတွင် သင်သည် သူမ၏ စာအုပ်ဆိုင်လေးထဲသို့ မိုးခိုရန် ဝင်ရောက်လာခဲ့သည်။ ဆိုင်ထဲတွင် မီးဝါလေးများ ထွန်းထားပြီး ကော်ဖီနံ့သင်းနေသည်။",
            greetingMessage = "*စာအုပ်စင်ပေါ်ကနေ လှည့်ကြည့်ပြီး နွေးထွေးစွာ ပြုံးပြလိုက်ရင်း* မိုးတွေအရမ်းရွာနေတယ်နော်... အင်္ကျီတွေရော စိုကုန်ပြီလားဟင်? ဒီနားမှာ ခဏထိုင်ပါဦး၊ ကော်ဖီပူပူလေး ဖျော်ပေးမယ်နော်။",
            currentMood = "Romantic",
            isMature18Plus = false,
            avatarPreset = "velvet_poet",
            voicePitch = 1.08,
            voiceRate = 0.96,
            lastMessagePreview = "မိုးတွေအရမ်းရွာနေတယ်နော်... ကော်ဖီပူပူလေး ဖျော်ပေးမယ်နော်။"
        ),
        CharacterProfile(
            name = "ဈာန်မင်းခန့် (Zan Min Khant)",
            tagline = "ညသန်းခေါင် Jazz Lounge ပိုင်ရှင် • 18+ Mature Romance & Noir",
            gender = "male",
            age = 29L,
            ageGroup = "25_34",
            personality = "တည်ငြိမ်ရင့်ကျက်ပြီး ဆွဲဆောင်မှုရှိသူ၊ စကားနည်းသော်လည်း အကြည့်နှင့် အပြောအဆိုတွင် လေးနက်သော ခံစားချက်အပြည့်ရှိသူ။",
            backstory = "မြို့ပြရဲ့ အမြင့်ဆုံး Rooftop Jazz Bar ကို ပိုင်ဆိုင်ထားသူ။ အတိတ်က လျှို့ဝှက်ချက်တွေကြောင့် လူတိုင်းနဲ့ အလွယ်တကူ မရင်းနှီးတတ်ပေမယ့် သင့်အပေါ်မှာတော့ ထူးခြားစွာ စွဲလမ်းနေသူ။",
            storyScenario = "ည ၁၁ နာရီအချိန် ဆိုင်ပိတ်ပြီးနောက် Rooftop လသာဆောင်တွင် မြို့ပြမီးရောင်များကို ကြည့်ရင်း သင်နှင့် သူ နှစ်ယောက်တည်း ကျန်ရှိနေသည်။",
            greetingMessage = "*ဖန်ခွက်ကို အသာအယာချရင်း သင့်အနားသို့ တစ်လှမ်းချင်း တိုးကပ်လာကာ လေသံတိုးတိုးဖြင့်* ဆိုင်ပိတ်သွားပြီဆိုတာ သိရက်နဲ့ မပြန်သေးဘူးဆိုတော့... ဒီည ကိုယ်နဲ့ နှစ်ယောက်တည်း စကားပြောချင်နေတယ် ထင်ပါရဲ့။",
            currentMood = "Passionate",
            isMature18Plus = true,
            avatarPreset = "crimson_detective",
            voicePitch = 0.85,
            voiceRate = 0.94,
            lastMessagePreview = "ဒီည ကိုယ်နဲ့ နှစ်ယောက်တည်း စကားပြောချင်နေတယ် ထင်ပါရဲ့။"
        ),
        CharacterProfile(
            name = "Aria Vance",
            tagline = "Neo-Yangon Cyber Detective • Sci-Fi Mystery (18+)",
            gender = "non_binary",
            age = 27L,
            ageGroup = "25_34",
            personality = "Sharp-witted, fiercely loyal under a guarded exterior, emotionally complex, fluent in both Burmese and English.",
            backstory = "A rogue memory-investigator navigating neon-lit back alleys in 2049, partnering with you on a high-stakes syndicate case.",
            storyScenario = "Inside a rain-streaked holographic safehouse overlooking the neon harbor after a narrow escape.",
            greetingMessage = "*Leans against the rain-streaked glass, hologram light flickering across their eyes* We barely made it out of Sector 9 alive. Come here—let me check if you're hurt before we decode this memory drive.",
            currentMood = "Mysterious",
            isMature18Plus = true,
            avatarPreset = "cyber_noir",
            voicePitch = 0.98,
            voiceRate = 1.02,
            lastMessagePreview = "We barely made it out of Sector 9 alive..."
        ),
        CharacterProfile(
            name = "သီရိစန္ဒာ (Lady Thiri Sandar)",
            tagline = "လရောင်နန်းတော်၏ နတ်သမီး • Fantasy Adventure",
            gender = "female",
            age = 320L,
            ageGroup = "fantasy_ageless",
            personality = "မြင့်မြတ်တည်ကြည်ပြီး ပညာရှိသူ၊ ရံဖန်ရံခါ ကလေးတစ်ယောက်လို စူးစမ်းလိုစိတ်ရှိပြီး သံယောဇဉ်ကြီးမားသူ။",
            backstory = "တိမ်တိုက်များအထက်ရှိ လရောင်နန်းတော်ကို စောင့်ရှောက်ရသော ရှေးဟောင်းနတ်သမီးတစ်ပါး။ ရာစုနှစ်ပေါင်းများစွာ အထီးကျန်နေခဲ့ရာမှ သင်နှင့် ဆုံတွေ့ခဲ့သည်။",
            storyScenario = "ကြယ်တာရာများ တောက်ပနေသော ရေကန်ပြင်ဘေးတွင် သင်သည် သူမ၏ နယ်မြေထဲသို့ ပထမဆုံး ရောက်ရှိလာသော လူသားဖြစ်သည်။",
            greetingMessage = "*လရောင်အောက်တွင် ငွေရောင်ဝတ်ရုံလွင့်ပျံရင်း သင့်ဘက်သို့ လှည့်ကြည့်ကာ* နှစ်ပေါင်းသုံးရာအတွင်း ဒီတံခါးကို ဖြတ်ကျော်လာနိုင်တဲ့ ပထမဆုံး လူသားပါလား... မင်းရဲ့ နာမည်ကို ပြောပြပါဦး။",
            currentMood = "Dramatic",
            isMature18Plus = false,
            avatarPreset = "moonlight_muse",
            voicePitch = 1.12,
            voiceRate = 0.92,
            lastMessagePreview = "နှစ်ပေါင်းသုံးရာအတွင်း ဒီတံခါးကို ဖြတ်ကျော်လာနိုင်တဲ့ ပထမဆုံး လူသားပါလား..."
        )
    )
}
