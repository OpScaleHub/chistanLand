package com.github.opscalehub.chistanland.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.github.opscalehub.chistanland.ui.LearningViewModel
import com.github.opscalehub.chistanland.ui.theme.*
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
    val activityType by viewModel.activityType.collectAsState()
    val missingCharIndex by viewModel.missingCharIndex.collectAsState()
    
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current

    val shakeOffset = remember { Animatable(0f) }
    var showHint by remember { mutableStateOf(false) }
    var hintBlocked by remember { mutableStateOf(false) }
    var lastInputTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSuccessFestival by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(false) }

    // Drop Target State
    var dropTargetPosition by remember { mutableStateOf(Offset.Zero) }
    var dropTargetSize by remember { mutableStateOf(IntSize.Zero) }

    val safeKeySize = remember(configuration.screenWidthDp, keyboardKeys.size) {
        val screenWidth = configuration.screenWidthDp.dp
        val horizontalPadding = 48.dp
        val totalSpacing = 40.dp
        val keysPerRow = if (keyboardKeys.size <= 9) 3 else 4
        ((screenWidth - horizontalPadding - totalSpacing) / keysPerRow).coerceIn(56.dp, 72.dp)
    }

    LaunchedEffect(typedText) {
        lastInputTime = System.currentTimeMillis()
        showHint = false
        hintBlocked = true
        delay(800)
        hintBlocked = false
    }

    LaunchedEffect(lastInputTime, isTransitioning, currentItem) {
        if (isTransitioning || currentItem == null) return@LaunchedEffect
        delay(7000)
        if (!hintBlocked && currentItem != null) {
            showHint = true
            viewModel.playHintInstruction()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is LearningViewModel.UiEvent.Error -> {
                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    } catch (e: Exception) {}
                    repeat(4) {
                        shakeOffset.animateTo(15f, animationSpec = tween(40))
                        shakeOffset.animateTo(-15f, animationSpec = tween(40))
                    }
                    shakeOffset.animateTo(0f, animationSpec = tween(40))
                }
                is LearningViewModel.UiEvent.Success -> {
                    isTransitioning = true
                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    } catch (e: Exception) {}
                    showSuccessFestival = true
                    delay(2500)
                    showSuccessFestival = false
                }
                is LearningViewModel.UiEvent.SessionComplete -> {
                    onBack()
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(currentItem?.id) {
        if (currentItem != null) {
            isTransitioning = false
            showHint = false
        }
    }

    if (currentItem == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MangoOrange)
        }
        return
    }

    val item = currentItem!!
    
    // هماهنگی UI با منطق آموزشی
    val targetFullString = remember(item, activityType) { 
        if (item.category == "NUMBER") {
            item.character 
        } else {
            item.word
        }
    }
    
    val targetChar by remember(typedText, targetFullString, activityType, missingCharIndex) {
        derivedStateOf { 
            if (activityType == LearningViewModel.ActivityType.MISSING_LETTER && missingCharIndex != -1) {
                targetFullString.getOrNull(missingCharIndex)?.toString() ?: ""
            } else {
                targetFullString.getOrNull(typedText.length)?.toString() ?: "" 
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (item.category == "NUMBER") MangoOrange.copy(alpha = 0.1f) else SkyBlue.copy(alpha = 0.2f),
                    Color.White
                )
            )
        )) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (!isTransitioning) onBack() },
                        modifier = Modifier.size(48.dp).background(SkyBlue.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "برگشت", tint = SkyBlue)
                    }
                    LearningAvatar(state = avatarState, modifier = Modifier.size(64.dp))
                    StreakIndicator(streak = streak)
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        ChickStatus(streak = streak)
                        PlantProgress(level = item.level)
                    }

                    if (item.category == "NUMBER") {
                        SessionQuantityIndicator(item.character)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    WordCard(
                        item = item,
                        onPlaySound = { if (!isTransitioning) viewModel.startLearning(item) },
                        modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    WordDisplay(
                        targetWord = targetFullString,
                        typedText = typedText,
                        charStatus = charStatus,
                        activityType = activityType,
                        missingCharIndex = missingCharIndex,
                        onPositioned = { pos, size -> dropTargetPosition = pos; dropTargetSize = size },
                        modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Keyboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f))))
                        .padding(bottom = 32.dp, top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KidKeyboard(
                        keys = keyboardKeys,
                        onKeyClick = { if (!isTransitioning) viewModel.onCharTyped(it) },
                        targetChar = targetChar,
                        showHint = showHint && !hintBlocked && !isTransitioning,
                        keySize = safeKeySize,
                        isDragEnabled = activityType == LearningViewModel.ActivityType.SPELLING, 
                        onDroppedOnTarget = { char ->
                            if (!isTransitioning) viewModel.onCharTyped(char)
                        },
                        dropTargetPosition = dropTargetPosition,
                        dropTargetSize = dropTargetSize
                    )
                }
            }

            AnimatedVisibility(visible = showSuccessFestival, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 1.2f)) {
                SuccessFestivalOverlay()
            }
        }
    }
}

@Composable
fun WordDisplay(
    targetWord: String,
    typedText: String,
    charStatus: List<Boolean>,
    activityType: LearningViewModel.ActivityType,
    missingCharIndex: Int,
    onPositioned: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.onGloballyPositioned { onPositioned(it.positionInRoot(), it.size) },
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        targetWord.forEachIndexed { index, char ->
            val isMissing = activityType == LearningViewModel.ActivityType.MISSING_LETTER && index == missingCharIndex
            val status = charStatus.getOrNull(index)
            val isTyped = index < typedText.length || (activityType == LearningViewModel.ActivityType.MISSING_LETTER && !isMissing)
            
            val color = when {
                status == true -> PastelGreen
                status == false -> Color.Red
                isMissing -> MangoOrange
                else -> SkyBlue.copy(alpha = 0.3f)
            }
            
            val displayText = if (isMissing && !isTyped) "?" else char.toString()
            val textAlpha = if (isTyped) 1f else 0.5f
            val scale by animateFloatAsState(if (isTyped) 1.25f else 1f, label = "charScale")
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = displayText,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = color.copy(alpha = textAlpha),
                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                )
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(if (isMissing) 8.dp else 6.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun WordCard(item: com.github.opscalehub.chistanland.data.LearningItem, onPlaySound: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wordCard")
    val floatAnim by infiniteTransition.animateFloat(initialValue = -8f, targetValue = 8f, animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse), label = "float")

    Card(
        modifier = modifier
            .sizeIn(minWidth = 260.dp, minHeight = 260.dp, maxWidth = 300.dp, maxHeight = 300.dp)
            .graphicsLayer { translationY = floatAnim }
            .clickable { onPlaySound() }
            .shadow(20.dp, RoundedCornerShape(48.dp)),
        shape = RoundedCornerShape(48.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val emoji = getEmojiForWord(item.word, item.category)
                val context = LocalContext.current

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    val imageResId = remember(item.imageUrl) { try { context.resources.getIdentifier(item.imageUrl, "drawable", context.packageName) } catch(e: Exception) { 0 } }
                    if (imageResId != 0) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = item.word,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                        )
                    } else {
                        Text(text = emoji, fontSize = 120.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(item.word, fontSize = 38.sp, fontWeight = FontWeight.Black, color = if (item.category == "NUMBER") MangoOrange else SkyBlue)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = MangoOrange, modifier = Modifier.size(28.dp))
                    Text("بشنو", color = MangoOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SessionQuantityIndicator(numberChar: String) {
    val count = remember(numberChar) {
        when(numberChar) {
            "۱" -> 1; "۲" -> 2; "۳" -> 3; "۴" -> 4; "۵" -> 5
            "۶" -> 6; "۷" -> 7; "۸" -> 8; "۹" -> 9; "۰" -> 0; else -> 0
        }
    }
    val rows = if (count <= 5) 1 else 2
    val itemsPerRow = if (count <= 5) count else (count + 1) / 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .border(1.dp, MangoOrange.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) { rowIndex ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val start = rowIndex * itemsPerRow
                val end = minOf(start + itemsPerRow, count)
                for (i in start until end) {
                    MagicOrb(index = i)
                }
            }
        }
        if (count == 0) {
            Text("خالی (صفر)", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MagicOrb(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val floatAnim by infiniteTransition.animateFloat(0f, 8f, infiniteRepeatable(tween(1000 + (index * 100), easing = EaseInOutSine), RepeatMode.Reverse), label = "float")
    Box(modifier = Modifier.size(32.dp).graphicsLayer { translationY = floatAnim }.background(brush = Brush.radialGradient(colors = listOf(Color.White, MangoOrange)), shape = CircleShape).border(2.dp, Color.White, CircleShape).shadow(4.dp, CircleShape))
}

@Composable
fun SuccessFestivalOverlay() {
    val context = LocalContext.current
    val resId = remember { try { context.resources.getIdentifier("success_fest_anim", "raw", context.packageName) } catch(e: Exception) { 0 } }
    
    if (resId != 0) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            LottieAnimation(
                composition = composition, 
                iterations = LottieConstants.IterateForever, 
                modifier = Modifier.size(400.dp)
            )
            Text("✨ تبریک! ✨", fontSize = 54.sp, fontWeight = FontWeight.Black, color = MangoOrange, modifier = Modifier.shadow(8.dp, CircleShape))
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Text("✨ تبریک! ✨", fontSize = 64.sp, fontWeight = FontWeight.Black, color = MangoOrange)
        }
    }
}

fun getEmojiForWord(word: String, category: String): String {
    return when(word) {
        "آ" -> "🌟"; "آب" -> "💧"; "باد" -> "🌬️"; "بام" -> "🏠"; "بار" -> "⚖️" // Changed to a scale for 'load'
        "سبد" -> "🧺"; "بابا" -> "🧔"; "نان" -> "🍞"; "باز" -> "🦅"; "دست" -> "🖐️"
        else -> "🌟"
    }
}

@Composable
fun StreakIndicator(streak: Int) {
    val scale by animateFloatAsState(if (streak > 0) 1.2f else 1f, label = "streakScale")
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.border(2.5.dp, MangoOrange.copy(alpha = 0.7f), RoundedCornerShape(24.dp)).shadow(4.dp, RoundedCornerShape(24.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Icon(Icons.Default.Star, null, tint = MangoOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("$streak", style = MaterialTheme.typography.titleMedium, color = MangoOrange, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ChickStatus(streak: Int) {
    val chickEmoji = when { streak >= 10 -> "👑"; streak >= 5 -> "🐣"; streak > 0 -> "🐥"; else -> "🥚" }
    val pulse by rememberInfiniteTransition(label = "chick").animateFloat(1f, if (streak > 0) 1.15f else 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(64.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.background(Brush.radialGradient(listOf(Color.Yellow.copy(alpha = 0.4f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
            Text(chickEmoji, fontSize = 40.sp)
        }
        Text(if (streak > 0) "جوجه خوشحال" else "در انتظار...", fontSize = 12.sp, color = DeepOcean, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlantProgress(level: Int) {
    val progressSize by animateDpAsState((level * 15).dp.coerceAtMost(80.dp), label = "plantGrowth")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(100.dp).width(60.dp), contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.size(44.dp, 30.dp).background(Color(0xFF8B4513), RoundedCornerShape(12.dp)).border(1.5.dp, Color(0xFF5D2E0A), RoundedCornerShape(12.dp)))
            Box(modifier = Modifier.width(8.dp).height(progressSize).background(PastelGreen, RoundedCornerShape(4.dp)).align(Alignment.BottomCenter).offset(y = (-26).dp)) {
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
fun KidKeyboard(
    keys: List<String>, 
    onKeyClick: (String) -> Unit, 
    targetChar: String, 
    showHint: Boolean, 
    keySize: Dp,
    isDragEnabled: Boolean = false,
    onDroppedOnTarget: (String) -> Unit = {},
    dropTargetPosition: Offset = Offset.Zero,
    dropTargetSize: IntSize = IntSize.Zero
) {
    val maxKeysPerRow = when {
        keys.size <= 4 -> keys.size
        keys.size <= 9 -> 3
        else -> 4
    }
    val reversedKeys = keys.reversed()
    val rows = reversedKeys.chunked(maxKeysPerRow)
    
    Column(modifier = Modifier.wrapContentWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(modifier = Modifier.wrapContentWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                row.forEach { char ->
                    val isHighlighted = showHint && char == targetChar
                    KeyButton(
                        char = char, 
                        onClick = { onKeyClick(char) }, 
                        isHighlighted = isHighlighted, 
                        size = keySize,
                        isDragEnabled = isDragEnabled,
                        onDroppedOnTarget = onDroppedOnTarget,
                        dropTargetPosition = dropTargetPosition,
                        dropTargetSize = dropTargetSize
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    char: String, 
    onClick: () -> Unit, 
    isHighlighted: Boolean, 
    size: Dp,
    isDragEnabled: Boolean = false,
    onDroppedOnTarget: (String) -> Unit = {},
    dropTargetPosition: Offset = Offset.Zero,
    dropTargetSize: IntSize = IntSize.Zero
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val view = LocalView.current

    val infiniteTransition = rememberInfiniteTransition(label = "key")
    val scalePulse by infiniteTransition.animateFloat(1f, if (isHighlighted) 1.12f else 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "scale")
    val animatedBgColor by animateColorAsState(if (isHighlighted) Color(0xFFFFD600) else MangoOrange, label = "keyColor")

    Surface(
        modifier = Modifier
            .size(size)
            .offset(x = offsetX.dp, y = offsetY.dp)
            .graphicsLayer { 
                scaleX = if (isDragging) 1.2f else scalePulse
                scaleY = if (isDragging) 1.2f else scalePulse
                shadowElevation = if (isDragging) 16f else 4f
            }
            .pointerInput(isDragEnabled) {
                if (!isDragEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isDragging = true; view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
                    onDragEnd = {
                        isDragging = false
                        val isDroppedInTarget = true 
                        if (isDroppedInTarget) onDroppedOnTarget(char)
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x / 2.5f
                        offsetY += dragAmount.y / 2.5f
                    }
                )
            }
            .clickable(enabled = !isDragging) { onClick() }
            .shadow(if (isHighlighted || isDragging) 12.dp else 4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp), color = animatedBgColor, border = BorderStroke(3.dp, Color.White.copy(alpha = 0.6f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(char, fontSize = (size.value * 0.42).sp, fontWeight = FontWeight.ExtraBold, color = if (isHighlighted) DeepOcean else Color.White)
        }
    }
}
