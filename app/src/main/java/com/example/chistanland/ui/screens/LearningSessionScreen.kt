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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.chistanland.ui.theme.SoftYellow
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
    var showHint by remember { mutableStateOf(false) }
    var lastInputTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSuccessFestival by remember { mutableStateOf(false) }

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
                    showSuccessFestival = true
                    delay(3000)
                    showSuccessFestival = false
                }
                is LearningViewModel.UiEvent.LevelDown -> {
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(SkyBlue.copy(alpha = 0.3f), Color.White)
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
                        modifier = Modifier.background(Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "برگشت",
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            tint = DeepOcean
                        )
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
                    onKeyClick = { viewModel.onCharTyped(it) },
                    targetChar = targetChar,
                    showHint = showHint
                )
            }

            if (showSuccessFestival) {
                SuccessFestivalOverlay()
            }
        }
    }
}

@Composable
fun SuccessFestivalOverlay() {
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
        // Assuming user will add success_fest.json to res/raw
        // Fallback to a simple colored box if not found (during build)
        context.resources.getIdentifier("success_fest", "raw", context.packageName).let { if(it==0) 1 else it } 
    ))
    
    // Using a more robust way to handle the resource for now
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(400.dp)
        )
        // Fallback text if Lottie is missing
        if (composition == null) {
            Text("✨ تبریک! ✨", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MangoOrange)
        }
    }
}

@Composable
fun WordCard(word: String, onPlaySound: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .size(220.dp)
            .graphicsLayer(translationY = floatAnim)
            .clickable { onPlaySound() }
            .shadow(16.dp, RoundedCornerShape(40.dp)),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🌟",
                    fontSize = 64.sp
                )
                Text(
                    text = word,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepOcean
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "پخش صدا",
                    tint = MangoOrange,
                    modifier = Modifier.size(32.dp)
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
        color = Color.White.copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            .border(2.dp, MangoOrange, RoundedCornerShape(20.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "$streak",
                style = MaterialTheme.typography.headlineSmall,
                color = MangoOrange,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MangoOrange,
                modifier = Modifier.size(24.dp)
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
    
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = if (streak > 0) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Yellow, Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chickEmoji, fontSize = 40.sp)
        }
        Text(
            text = if (streak > 0) "جوجه خوشحال" else "در انتظار...",
            style = MaterialTheme.typography.labelMedium,
            color = DeepOcean,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlantProgress(level: Int) {
    val progressSize by animateDpAsState(
        targetValue = (level * 24).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.height(140.dp).width(80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp, 35.dp)
                    .background(Color(0xFF8B4513), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .border(2.dp, Color(0xFF5D2E0A), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(progressSize)
                    .background(PastelGreen, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .align(Alignment.BottomCenter)
                    .offset(y = (-30).dp)
            ) {
                if (level >= 2) Leaf(Modifier.align(Alignment.TopStart).offset(x = (-8).dp))
                if (level >= 4) Leaf(Modifier.align(Alignment.CenterEnd).offset(x = 8.dp))
            }
        }
        Text("سطح دانایی: $level", style = MaterialTheme.typography.labelMedium, color = DeepOcean, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Leaf(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp, 10.dp)
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
                else -> DeepOcean.copy(alpha = 0.2f)
            }

            val displayText = if (isTyped) typedText[index].toString() else char.toString()
            val textAlpha = if (isTyped) 1f else 0.4f
            val scale by animateFloatAsState(if (isTyped) 1.2f else 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = displayText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = color.copy(alpha = textAlpha),
                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(6.dp)
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(char: String, onClick: () -> Unit, isHighlighted: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isHighlighted) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .widthIn(min = 56.dp, max = 64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) Color(0xFFFFEB3B) else MangoOrange,
        shadowElevation = if (isHighlighted) 10.dp else 4.dp
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
