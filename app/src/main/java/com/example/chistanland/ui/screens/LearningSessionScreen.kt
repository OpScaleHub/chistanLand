package com.example.chistanland.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.MangoOrange
import com.example.chistanland.ui.theme.PastelGreen
import com.example.chistanland.ui.theme.SoftYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LearningSessionScreen(
    viewModel: LearningViewModel,
    onBack: () -> Unit
) {
    val currentItem by viewModel.currentItem.collectAsState()
    val typedText by viewModel.typedText.collectAsState()
    val charStatus by viewModel.charStatus.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val keyboardKeys by viewModel.keyboardKeys.collectAsState()
    val view = LocalView.current

    val shakeOffset = remember { Animatable(0f) }
    val levelDownY = remember { Animatable(0f) }
    var showHint by remember { mutableStateOf(false) }
    var lastInputTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(typedText) {
        lastInputTime = System.currentTimeMillis()
        showHint = false
    }

    LaunchedEffect(lastInputTime) {
        delay(4000)
        showHint = true
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is LearningViewModel.UiEvent.Error -> {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    repeat(4) {
                        shakeOffset.animateTo(15f, animationSpec = tween(40))
                        shakeOffset.animateTo(-15f, animationSpec = tween(40))
                    }
                    shakeOffset.animateTo(0f, animationSpec = tween(40))
                }
                is LearningViewModel.UiEvent.Success -> {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                is LearningViewModel.UiEvent.LevelDown -> {
                    // "Falling Balloon" animation
                    levelDownY.animateTo(300f, animationSpec = tween(1000, easing = LinearOutSlowInEasing))
                    levelDownY.snapTo(0f)
                }
            }
        }
    }

    if (currentItem == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val item = currentItem!!
    val targetChar = item.word.getOrNull(typedText.length)?.toString() ?: ""

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                }
                StreakIndicator(streak = streak)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Indicators
            Row(
                modifier = Modifier.fillMaxWidth().graphicsLayer(translationY = levelDownY.value),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                PlantProgress(level = item.level)
                ChickStatus(streak = streak)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Word Image Card
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(translationX = shakeOffset.value)
                    .clip(RoundedCornerShape(32.dp))
                    .background(SoftYellow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.word,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MangoOrange
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Dynamic Word Display
            WordDisplay(
                targetWord = item.word,
                typedText = typedText,
                charStatus = charStatus,
                modifier = Modifier.graphicsLayer(translationX = shakeOffset.value)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Adaptive Keyboard
            KidKeyboard(
                keys = keyboardKeys,
                onKeyClick = { viewModel.onCharTyped(it) },
                targetChar = targetChar,
                showHint = showHint
            )
        }
    }
}

@Composable
fun StreakIndicator(streak: Int) {
    val scale by animateFloatAsState(
        targetValue = if (streak > 0) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Text(
            text = "$streak",
            style = MaterialTheme.typography.headlineMedium,
            color = MangoOrange,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = MangoOrange,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun ChickStatus(streak: Int) {
    val chickEmoji = when {
        streak >= 5 -> "🐣"
        streak > 0 -> "🐥"
        else -> "🥚"
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.Yellow.copy(alpha = if (streak > 0) 1f else 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chickEmoji, fontSize = 32.sp)
        }
        Text(
            text = if (streak > 0) "خوشحال" else "در انتظار",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PlantProgress(level: Int) {
    val progressSize by animateDpAsState(
        targetValue = (level * 20).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.height(120.dp).width(80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp, 30.dp)
                    .background(Color(0xFF8B4513), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(progressSize)
                    .background(PastelGreen)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-25).dp)
            )
        }
        Text("سطح رشد: $level", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WordDisplay(
    targetWord: String,
    typedText: String,
    charStatus: List<Boolean>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        targetWord.forEachIndexed { index, char ->
            val status = charStatus.getOrNull(index)
            val isTyped = index < typedText.length
            
            val color = when {
                status == true -> PastelGreen
                status == false -> Color.Red
                else -> Color.Gray.copy(alpha = 0.2f)
            }

            val displayText = if (isTyped) typedText[index].toString() else char.toString()
            val textAlpha = if (isTyped) 1f else 0.3f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = displayText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color.copy(alpha = textAlpha)
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(6.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun KidKeyboard(
    keys: List<String>,
    onKeyClick: (String) -> Unit,
    targetChar: String,
    showHint: Boolean
) {
    // Layout keys in 2 rows
    val rows = keys.chunked((keys.size + 1) / 2)

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { char ->
                    val isHighlighted = showHint && char == targetChar
                    KeyButton(
                        char = char,
                        onClick = { onKeyClick(char) },
                        isHighlighted = isHighlighted
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(char: String, onClick: () -> Unit, isHighlighted: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isHighlighted) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) Color.Yellow else MangoOrange,
        shadowElevation = if (isHighlighted) 8.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = char,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MangoOrange else Color.White
            )
        }
    }
}
