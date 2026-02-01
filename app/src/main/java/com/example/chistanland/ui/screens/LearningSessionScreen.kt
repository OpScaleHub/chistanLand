package com.example.chistanland.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.*
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
    val avatarState by viewModel.avatarState.collectAsState()
    val view = LocalView.current
    val scrollState = rememberScrollState()
    
    val shakeOffset = remember { Animatable(0f) }
    val levelDownY = remember { Animatable(0f) }
    
    var showHint by remember { mutableStateOf(false) }
    var hintBlocked by remember { mutableStateOf(false) }
    var lastInputTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSuccessFestival by remember { mutableStateOf(false) }

    LaunchedEffect(typedText) {
        lastInputTime = System.currentTimeMillis()
        showHint = false
        hintBlocked = true
        delay(800) 
        hintBlocked = false
    }

    LaunchedEffect(lastInputTime) {
        delay(5000) 
        if (!hintBlocked) {
            showHint = true
            viewModel.playHintInstruction() 
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
                is LearningViewModel.UiEvent.StartReviewSession -> {}
            }
        }
    }

    if (currentItem == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val item = currentItem!!
    
    // شناسایی دقیق هدف تایپ (کلمه برای حروف، خود نشانه برای اعداد)
    val targetFullString = remember(item) {
        if (item.category == "NUMBER") item.character else item.word
    }

    val targetChar by remember(typedText, targetFullString) {
        derivedStateOf { targetFullString.getOrNull(typedText.length)?.toString() ?: "" }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (item.category == "NUMBER") MangoOrange.copy(alpha = 0.2f) else SkyBlue.copy(alpha = 0.4f),
                                Color.White
                            )
                        )
                    )
                    .verticalScroll(scrollState)
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت", tint = SkyBlue)
                    }
                    
                    LearningAvatar(state = avatarState, modifier = Modifier.size(64.dp))
                    
                    StreakIndicator(streak = streak)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = levelDownY.value },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    ChickStatus(streak = streak)
                    PlantProgress(level = item.level)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // نمایش کارت تصویر/نشانه
                WordCard(
                    item = item,
                    onPlaySound = { viewModel.startLearning(item) },
                    modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // نمایش حروف یا رقم هدف
                WordDisplay(
                    targetWord = targetFullString,
                    typedText = typedText,
                    charStatus = charStatus,
                    modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                )

                Spacer(modifier = Modifier.heightIn(min = 32.dp).weight(1f))

                KidKeyboard(
                    keys = keyboardKeys,
                    onKeyClick = viewModel::onCharTyped, 
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
fun WordCard(item: com.example.chistanland.data.LearningItem, onPlaySound: () -> Unit, modifier: Modifier = Modifier) {
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
            .sizeIn(minWidth = 200.dp, minHeight = 200.dp, maxWidth = 260.dp, maxHeight = 260.dp)
            .graphicsLayer { translationY = floatAnim }
            .clickable { onPlaySound() }
            .shadow(20.dp, RoundedCornerShape(48.dp)),
        shape = RoundedCornerShape(48.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = if (item.category == "NUMBER") "🔢" else "🌟", fontSize = 64.sp)
                Text(
                    text = if (item.category == "NUMBER") item.word else item.word, 
                    fontSize = 42.sp, 
                    fontWeight = FontWeight.Black, 
                    color = if (item.category == "NUMBER") MangoOrange else SkyBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "پخش صدا", tint = MangoOrange, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun StreakIndicator(streak: Int) {
    val scale by animateFloatAsState(targetValue = if (streak > 0) 1.2f else 1f, label = "streakScale")
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(2.5.dp, MangoOrange.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .shadow(4.dp, RoundedCornerShape(24.dp))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MangoOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "$streak", style = MaterialTheme.typography.titleMedium, color = MangoOrange, fontWeight = FontWeight.Black)
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
                .size(64.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Yellow.copy(alpha = 0.4f), Color.Transparent)
                    ), 
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chickEmoji, fontSize = 40.sp)
        }
        Text(text = if (streak > 0) "جوجه خوشحال" else "در انتظار...", fontSize = 12.sp, color = DeepOcean, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlantProgress(level: Int) {
    val progressSize by animateDpAsState(targetValue = (level * 15).dp.coerceAtMost(80.dp), label = "plantGrowth")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(100.dp).width(60.dp), contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.size(44.dp, 30.dp).background(Color(0xFF8B4513), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).border(1.5.dp, Color(0xFF5D2E0A), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)))
            Box(modifier = Modifier.width(8.dp).height(progressSize).background(PastelGreen, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).align(Alignment.BottomCenter).offset(y = (-26).dp)) {
                if (level >= 2) Leaf(Modifier.align(Alignment.TopStart).offset(x = (-8).dp))
                if (level >= 4) Leaf(Modifier.align(Alignment.CenterEnd).offset(x = 8.dp))
            }
        }
        Text("سطح: $level", fontSize = 12.sp, color = DeepOcean, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Leaf(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(14.dp, 10.dp).background(PastelGreen, RoundedCornerShape(50.dp)))
}

@Composable
fun WordDisplay(targetWord: String, typedText: String, charStatus: List<Boolean>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
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
            val scale by animateFloatAsState(if (isTyped) 1.25f else 1f, label = "charScale")

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = displayText, 
                    fontSize = 42.sp, 
                    fontWeight = FontWeight.Black, 
                    color = color.copy(alpha = textAlpha), 
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
                Box(modifier = Modifier.width(32.dp).height(6.dp).background(color, RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
fun KidKeyboard(keys: List<String>, onKeyClick: (String) -> Unit, targetChar: String, showHint: Boolean) {
    val rows = keys.chunked((keys.size + 1) / 2)
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                row.forEach { char ->
                    val isHighlighted = showHint && char == targetChar
                    KeyButton(char = char, onClick = { onKeyClick(char) }, isHighlighted = isHighlighted, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun KeyButton(char: String, onClick: () -> Unit, isHighlighted: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "key")
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = if (isHighlighted) 1.1f else 1f, animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "scale")
    val animatedBgColor by animateColorAsState(targetValue = if (isHighlighted) Color(0xFFFFD600) else MangoOrange, label = "keyColor")

    Surface(
        modifier = modifier
            .aspectRatio(1.1f)
            .widthIn(max = 64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onClick() }
            .shadow(if (isHighlighted) 10.dp else 2.dp, RoundedCornerShape(16.dp)), 
        shape = RoundedCornerShape(16.dp), 
        color = animatedBgColor
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = char, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (isHighlighted) DeepOcean else Color.White)
        }
    }
}
