package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterProfile
import com.example.ui.components.ImageGeneratingShimmerBox
import com.example.util.LocalAppStrings
import com.example.viewmodel.ImageGenState

@Composable
fun ImageStudioScreen(
    studioImageGenState: ImageGenState,
    characters: List<CharacterProfile>,
    onGenerateImage: (prompt: String, style: String) -> Unit,
    onApplyAvatarToCharacter: (CharacterProfile, android.graphics.Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Cinematic Anime Romance") }
    var selectedCharForAvatar by remember(characters) { mutableStateOf(characters.firstOrNull()) }

    val artStyles = listOf(
        "Cinematic Anime Romance",
        "Photorealistic Dramatic Portrait",
        "Dark Velvet Noir",
        "Dreamy Watercolor",
        "Cyberpunk Neon",
        "Ethereal Fantasy Oil Painting"
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        ) {
            Text(
                text = strings.studioTitle,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = strings.studioSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Style Presets
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                artStyles.forEach { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style },
                        label = { Text(style) },
                        modifier = Modifier.testTag("studio_style_${style.take(10)}")
                    )
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(strings.studioPromptLabel) },
                minLines = 3,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_prompt_input")
            )

            // Quick Inspiration Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                AssistChip(
                    onClick = {
                        prompt = "A cozy rainy night bookshop in Yangon with warm golden lanterns and a gentle young woman holding coffee"
                    },
                    label = { Text(if (strings.isMyanmar) "ရန်ကုန်မိုးည စာအုပ်ဆိုင်" else "Rainy Bookshop") }
                )
                AssistChip(
                    onClick = {
                        prompt = "Charismatic noir lounge host on a midnight city rooftop under neon reflections"
                    },
                    label = { Text(if (strings.isMyanmar) "ညသန်းခေါင် Rooftop" else "Midnight Rooftop") }
                )
                AssistChip(
                    onClick = {
                        prompt = "Celestial guardian princess standing by a starlit lotus lake under a silver full moon"
                    },
                    label = { Text(if (strings.isMyanmar) "လရောင်နန်းတော်" else "Moonlight Palace") }
                )
            }

            Button(
                onClick = { onGenerateImage(prompt.trim(), selectedStyle) },
                enabled = prompt.isNotBlank() && studioImageGenState !is ImageGenState.Loading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("studio_generate_button")
            ) {
                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.studioGenerateBtn, style = MaterialTheme.typography.titleMedium)
            }

            // Output Area
            when (studioImageGenState) {
                is ImageGenState.Idle -> Unit

                is ImageGenState.Loading -> {
                    ImageGeneratingShimmerBox(promptText = studioImageGenState.prompt)
                }

                is ImageGenState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = studioImageGenState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is ImageGenState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Image(
                                bitmap = studioImageGenState.bitmap.asImageBitmap(),
                                contentDescription = studioImageGenState.prompt,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .testTag("studio_generated_image")
                            )

                            if (characters.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    characters.forEach { ch ->
                                        FilterChip(
                                            selected = selectedCharForAvatar?.id == ch.id,
                                            onClick = { selectedCharForAvatar = ch },
                                            label = { Text(ch.name) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        selectedCharForAvatar?.let { target ->
                                            onApplyAvatarToCharacter(target, studioImageGenState.bitmap)
                                        }
                                    },
                                    enabled = selectedCharForAvatar != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("studio_apply_avatar_button")
                                ) {
                                    Icon(imageVector = Icons.Filled.Face, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.studioApplyAvatarBtn)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
