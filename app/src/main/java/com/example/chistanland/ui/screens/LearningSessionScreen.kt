package com.example.chistanland.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.MangoOrange
import com.example.chistanland.ui.theme.PastelGreen
import com.example.chistanland.ui.theme.SkyBlue
import com.example.chistanland.ui.theme.DeepOcean
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
    
    // Hint logic with protection against flickering
    var showHint by remember { mutableStateOf(false) }
    var hintBlocked by remember { mutableStateOf(false) }
    var lastInputTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSuccessFestival by remember { mutableStateOf(false) }

    LaunchedEffect(typedText) {
        lastInputTime = System.currentTimeMillis()
        showHint = false
        // Block hint for a short duration after any input to prevent "ghost" highlighting
        hintBlocked = true
        delay(800) 
        hintBlocked = false
    }

    LaunchedEffect(lastInputTime) {
        delay(5000) // 5 seconds of idle time before showing hint
        if (!hintBlocked) {
            showHint = true
        }
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
                    showSuccessFestival = true
                    delay(2500)
                    showSuccessFestival = false
                }
                is LearningViewModel.UiEvent.LevelDown -> {
                    levelDownY.animateTo(300f, animationSpec = tween(1000, easing = LinearOutSlowInEasing))
                    levelDownY.snapTo(0f)
                }
                is LearningViewModel.UiEvent.StartReviewSession -> {
                    // StartReviewSession event received - can be used for navigation or special effects
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(SkyBlue.copy(alpha = 0.4f), Color.White)
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(SkyBlue.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "برگشت",
                            tint = SkyBlue
                        )
                    }
                    StreakIndicator(streak = streak)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Indicators
                Row(
                    modifier = Modifier.fillMaxWidth().graphicsLayer(translationY = levelDownY.value),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    ChickStatus(streak = streak)
                    PlantProgress(level = item.level)
                }

                Spacer(modifier = Modifier.height(32.dp))

                WordCard(
                    word = item.word,
                    onPlaySound = { viewModel.startLearning(item) },
                    modifier = Modifier.graphicsLayer(translationX = shakeOffset.value)
                )

                Spacer(modifier = Modifier.height(40.dp))

                WordDisplay(
                    targetWord = item.word,
                    typedText = typedText,
                    charStatus = charStatus,
                    modifier = Modifier.graphicsLayer(translationX = shakeOffset.value)
                )

                Spacer(modifier = Modifier.weight(1f))

                KidKeyboard(
                    keys = keyboardKeys,
                    onKeyClick = { char ->
                        showHint = false
                        viewModel.onCharTyped(char)
                    },
                    targetChar = targetChar,
                    showHint = showHint && !hintBlocked
                )
            }

            AnimatedVisibility(
                visible = showSuccessFestival,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 1.2f)
            ) {
                SuccessFestivalOverlay()
            }
        }
    }
}

@Composable
fun SuccessFestivalOverlay() {
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
        context.resources.getIdentifier("success_fest_anim", "raw", context.packageName).let { if(it==0) 1 else it } 
    ))
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(400.dp)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✨ تبریک! ✨",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = MangoOrange,
                modifier = Modifier.shadow(8.dp, CircleShape)
            )
        }
    }
}

@Composable
fun WordCard(word: String, onPlaySound: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wordCard")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    )

    Card(
        modifier = modifier
            .size(240.dp)
            .graphicsLayer(translationY = floatAnim)
            .clickable { onPlaySound() }
            .shadow(20.dp, RoundedCornerShape(48.dp)),
        shape = RoundedCornerShape(48.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🌟",
                    fontSize = 72.sp
                )
                Text(
                    text = word,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = SkyBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "پخش صدا",
                    tint = MangoOrange,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun StreakIndicator(streak: Int) {
    val scale by animateFloatAsState(
        targetValue = if (streak > 0) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .border(2.5.dp, MangoOrange.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .shadow(4.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MangoOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$streak",
                style = MaterialTheme.typography.headlineSmall,
                color = MangoOrange,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun ChickStatus(streak: Int) {
    val chickEmoji = when {
        streak >= 10 -> "👑"
        streak >= 5 -> "🐣"
        streak > 0 -> "🐥"
        else -> "🥚"
    }
    
    val pulse by rememberInfiniteTransition(label = "chick").animateFloat(
        initialValue = 1f,
        targetValue = if (streak > 0) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Yellow.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chickEmoji, fontSize = 48.sp)
        }
        Text(
            text = if (streak > 0) "جوجه خوشحال" else "در انتظار...",
            fontSize = 14.sp,
            color = DeepOcean,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlantProgress(level: Int) {
    val progressSize by animateDpAsState(
        targetValue = (level * 20).dp.coerceAtMost(100.dp),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.height(140.dp).width(80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp, 38.dp)
                    .background(Color(0xFF8B4513), RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .border(2.dp, Color(0xFF5D2E0A), RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            )
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(progressSize)
                    .background(PastelGreen, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .align(Alignment.BottomCenter)
                    .offset(y = (-32).dp)
            ) {
                if (level >= 2) Leaf(Modifier.align(Alignment.TopStart).offset(x = (-10).dp))
                if (level >= 4) Leaf(Modifier.align(Alignment.CenterEnd).offset(x = 10.dp))
            }
        }
        Text("سطح دانایی: $level", fontSize = 14.sp, color = DeepOcean, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Leaf(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp, 12.dp)
            .background(PastelGreen, RoundedCornerShape(50.dp))
    )
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
                else -> SkyBlue.copy(alpha = 0.3f)
            }

            val displayText = if (isTyped) typedText[index].toString() else char.toString()
            val textAlpha = if (isTyped) 1f else 0.5f
            val scale by animateFloatAsState(if (isTyped) 1.25f else 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = displayText,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = color.copy(alpha = textAlpha),
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(8.dp)
                        .background(color, RoundedCornerShape(4.dp))
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
    val rows = keys.chunked((keys.size + 1) / 2)

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { char ->
                    val isHighlighted = showHint && char == targetChar
                    KeyButton(
                        char = char,
                        onClick = { onKeyClick(char) },
                        isHighlighted = isHighlighted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(char: String, onClick: () -> Unit, isHighlighted: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "key")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isHighlighted) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFFFFD600) else MangoOrange,
        animationSpec = tween(400)
    )

    Surface(
        modifier = modifier
            .aspectRatio(1.1f)
            .widthIn(max = 72.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() }
            .shadow(if (isHighlighted) 12.dp else 4.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = animatedBgColor,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = char,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlighted) DeepOcean else Color.White
            )
        }
    }
}
