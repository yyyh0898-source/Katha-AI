package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.ImageCodec
import com.example.util.LocalAppStrings

@Composable
fun CharacterAvatarView(
    name: String,
    avatarPreset: String,
    avatarBase64: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isMature18Plus: Boolean = false
) {
    val decodedBitmap = remember(avatarBase64) {
        ImageCodec.decodeBase64ToBitmap(avatarBase64)
    }

    val gradientColors = remember(avatarPreset) {
        when (avatarPreset) {
            "cyber_noir" -> listOf(Color(0xFF0F172A), Color(0xFF7C3AED), Color(0xFF06B6D4))
            "royal_guardian" -> listOf(Color(0xFF1E1B4B), Color(0xFFB45309), Color(0xFFFDE68A))
            "moonlight_muse" -> listOf(Color(0xFF172554), Color(0xFF6366F1), Color(0xFFE0E7FF))
            "golden_wanderer" -> listOf(Color(0xFF451A03), Color(0xFFD97706), Color(0xFFFCD34D))
            "crimson_detective" -> listOf(Color(0xFF2A0815), Color(0xFFBE123C), Color(0xFFFB7185))
            else -> listOf(Color(0xFF2E1065), Color(0xFFDB2777), Color(0xFFFDA4AF))
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_character_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        shape = CircleShape
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors))
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.28f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = this.size.minDimension * 0.34f,
                        center = Offset(this.size.width * 0.72f, this.size.height * 0.28f)
                    )
                }
                val initial = name.trim().firstOrNull()?.uppercase() ?: "K"
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (isMature18Plus) {
            Surface(
                color = Color(0xFFBE123C),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            ) {
                Text(
                    text = "18+",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun CloudSyncStatusPill(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val bgColor = if (isOnline) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isOnline) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = modifier.testTag("cloud_sync_status_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = if (isOnline) strings.cloudSyncedOnline else strings.offlineCachedMode,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isOnline) strings.cloudSyncedOnline else strings.offlineCachedMode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RoleplayFormattedText(
    rawText: String,
    textColor: Color,
    actionColor: Color,
    modifier: Modifier = Modifier
) {
    val annotated = remember(rawText, textColor, actionColor) {
        buildAnnotatedString {
            val regex = Regex("\\*([^*]+)\\*")
            var lastIndex = 0
            for (match in regex.findAll(rawText)) {
                if (match.range.first > lastIndex) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(rawText.substring(lastIndex, match.range.first))
                    }
                }
                withStyle(
                    SpanStyle(
                        color = actionColor,
                        fontStyle = FontStyle.Italic
                    )
                ) {
                    append(match.groupValues[1])
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < rawText.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(rawText.substring(lastIndex))
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}

@Composable
fun ImageGeneratingShimmerBox(
    promptText: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        start = Offset(progress - 300f, progress - 300f),
        end = Offset(progress, progress)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(24.dp)
            .testTag("image_gen_loading_box")
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = strings.generateImageLoading,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (promptText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$promptText\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MatureAgeGateDialog(
    onConfirm18Plus: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.ageGateTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = strings.ageGateMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm18Plus,
                modifier = Modifier.testTag("confirm_18_plus_button")
            ) {
                Text(strings.ageGateConfirm)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_18_plus_button")
            ) {
                Text(strings.ageGateCancel)
            }
        }
    )
}

@Composable
fun BiometricLockOverlay(
    backupPin: String,
    errorMessage: String?,
    onTriggerBiometric: () -> Unit,
    onUnlockSuccess: () -> Unit,
    onPinError: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var enteredPin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = strings.lockScreenTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.lockScreenTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.lockScreenSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onTriggerBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("unlock_biometric_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.unlockBiometricButton)
                }

                if (backupPin.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text(strings.unlockWithPinPlaceholder) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_pin_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            if (enteredPin == backupPin) {
                                onUnlockSuccess()
                            } else {
                                onPinError(if (strings.isMyanmar) "PIN နံပါတ် မှားယွင်းနေပါသည်" else "Incorrect PIN")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_pin_submit_button")
                    ) {
                        Text(strings.unlockPinButton)
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
