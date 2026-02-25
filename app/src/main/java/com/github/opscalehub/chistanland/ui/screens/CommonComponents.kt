package com.github.opscalehub.chistanland.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import com.github.opscalehub.chistanland.ui.theme.*

@Composable
fun LearningAvatar(state: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resId = remember { context.resources.getIdentifier("avatar", "raw", context.packageName) }

    if (resId != 0) {
        // High-end Rive Animation
        AndroidView(
            modifier = modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .border(2.dp, Color.White, CircleShape),
            factory = { ctx ->
                RiveAnimationView(ctx).apply {
                    setRiveResource(resId, stateMachineName = "State Machine 1", autoplay = true)
                }
            },
            update = { view ->
                // Dynamically update the state machine inputs based on ViewModel state
                view.setBooleanState("State Machine 1", "isHappy", state == "HAPPY")
                view.setBooleanState("State Machine 1", "isTalking", state == "SPEAKING")
                view.setBooleanState("State Machine 1", "isThinking", state == "THINKING")
            }
        )
    } else {
        // Fallback to the robust legacy emoji avatar if .riv file is missing
        LegacyEmojiAvatar(state, modifier)
    }
}

@Composable
fun LegacyEmojiAvatar(state: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == "SPEAKING") -10f else -4f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "bounce"
    )

    val avatarEmoji = when (state) {
        "SPEAKING" -> "👄"
        "HAPPY" -> "🤩"
        "THINKING" -> "🤔"
        else -> "😊"
    }

    val avatarBg = when (state) {
        "SPEAKING" -> SkyBlue.copy(alpha = 0.3f)
        "HAPPY" -> Color.Yellow.copy(alpha = 0.3f)
        "THINKING" -> Color.Gray.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .graphicsLayer(translationY = bounce)
            .background(avatarBg, CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .shadow(4.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🧚", fontSize = 32.sp)
        Text(
            text = avatarEmoji,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp)
        )
    }
}
