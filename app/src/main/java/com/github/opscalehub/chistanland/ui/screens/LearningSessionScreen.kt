package com.github.opscalehub.chistanland.ui.screens

import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.text.style.TextAlign
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
    val isGenerating by viewModel.isGenerating.collectAsState()
    val recognitionOptions by viewModel.recognitionOptions.collectAsState()
    
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current

    // Single, idempotent exit path used by the on-screen back button, the system/gesture
    // back, and a natural SessionComplete — so leaving can never double-pop or fire after
    // we've already left, and it always cancels in-flight session coroutines safely.
    var exited by remember { mutableStateOf(false) }
    val leave: () -> Unit = {
        if (!exited) {
            exited = true
            viewModel.cancelSession()
            onBack()
        }
    }
    BackHandler { leave() }

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
                    leave()
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
    val targetFullString = item.word
    
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
                colors = listOf(SkyBlue.copy(alpha = 0.2f), Color.White)
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
                        onClick = { leave() },
                        modifier = Modifier.size(48.dp).background(SkyBlue.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "برگشت", tint = SkyBlue)
                    }
                    LearningAvatar(state = avatarState, modifier = Modifier.size(64.dp))
                    StreakIndicator(streak = streak)
                }

                // Content Area with Animation Transition
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = activityType,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.92f, animationSpec = tween(600)))
                                .togetherWith(fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.08f, animationSpec = tween(400)))
                        },
                        label = "ActivityTransition"
                    ) { targetType ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                                ChickStatus(streak = streak)
                                PlantProgress(level = item.level)
                            }

                            when (targetType) {
                                LearningViewModel.ActivityType.STORY_TELLING -> {
                                    StoryModeUI(item = item, isGenerating = isGenerating)
                                }
                                LearningViewModel.ActivityType.TRACE_LETTER -> {
                                    TracingModeUI(item = item, onComplete = { viewModel.onCharTyped(item.character) })
                                }
                                LearningViewModel.ActivityType.WORD_RECOGNITION -> {
                                    WordRecognitionUI(
                                        item = item,
                                        options = recognitionOptions,
                                        enabled = !isTransitioning,
                                        onPick = { viewModel.onImagePicked(it) },
                                        modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                                    )
                                }
                                else -> {
                                    WordCard(
                                        item = item,
                                        // "بشنو" replays the audio for the CURRENT exercise — it must not
                                        // restart/re-randomize the activity (which startLearning would do).
                                        onPlaySound = { if (!isTransitioning) viewModel.playHintInstruction() },
                                        modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
                                    )

                                    // On first encounter, show the letter's 4 connected forms (the hard part
                                    // of Persian script): same letter, different shape when joined.
                                    if (targetType == LearningViewModel.ActivityType.PHONICS_INTRO) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        LetterFormsStrip(letter = item.character)
                                    }

                                    Spacer(modifier = Modifier.height(40.dp))

                                    WordDisplay(
                                        targetWord = targetFullString,
                                        typedText = typedText,
                                        charStatus = charStatus,
                                        activityType = targetType,
                                        missingCharIndex = missingCharIndex,
                                        onPositioned = { pos, size -> dropTargetPosition = pos; dropTargetSize = size },
                                        modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
                                        newLetter = item.character
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                // Keyboard Section
                AnimatedVisibility(
                    visible = activityType != LearningViewModel.ActivityType.STORY_TELLING &&
                        activityType != LearningViewModel.ActivityType.TRACE_LETTER &&
                        activityType != LearningViewModel.ActivityType.WORD_RECOGNITION,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
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
                
                if (activityType == LearningViewModel.ActivityType.STORY_TELLING) {
                    Button(
                        onClick = { viewModel.onCharTyped("") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .height(72.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PastelGreen)
                    ) {
                        Text("✨ متوجه شدم! ✨", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            AnimatedVisibility(visible = showSuccessFestival, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 1.2f)) {
                SuccessFestivalOverlay()
            }
        }
    }
}

@Composable
fun StoryModeUI(item: com.github.opscalehub.chistanland.data.LearningItem, isGenerating: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "StoryFloat")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -10f, 
        targetValue = 10f, 
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), 
        label = "float"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .graphicsLayer { translationY = floatAnim }
            .shadow(24.dp, RoundedCornerShape(40.dp)),
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(4.dp, SkyBlue.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📖 دنیایِ قصه‌ها", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepOcean)
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                if (isGenerating) {
                    CircularProgressIndicator(color = MangoOrange, strokeWidth = 6.dp, modifier = Modifier.size(80.dp))
                } else {
                    Text(getEmojiForWord(item.word), fontSize = 100.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isGenerating) "درحالِ ساختن یک داستان جادویی..." else "به داستان گوش کن عزیزم...",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepOcean.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Samples the actual outline of a glyph (from the font) into a list of target points,
 * centered inside a box of the given pixel size. This lets tracing validate that the
 * child followed the *letter's real shape* — not just that they covered screen area.
 * Works for all 32 letters with no per-letter art assets.
 */
private fun glyphContours(character: String, widthPx: Int, heightPx: Int): List<List<Offset>> {
    if (widthPx <= 0 || heightPx <= 0 || character.isBlank()) return emptyList()
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = heightPx * 0.62f
        textAlign = AndroidPaint.Align.LEFT
    }
    val bounds = AndroidRect()
    paint.getTextBounds(character, 0, character.length, bounds)
    // Center the glyph in the box (bounds.top is negative, above the baseline).
    val baseX = (widthPx - bounds.width()) / 2f - bounds.left
    val baseY = (heightPx - bounds.height()) / 2f - bounds.top

    val path = AndroidPath()
    paint.getTextPath(character, 0, character.length, baseX, baseY, path)

    // Each contour is a separate piece of the letter (e.g. the body of «ب» and its dot below).
    // We must trace every piece, so we keep them grouped rather than flattened.
    val pm = PathMeasure(path, false)
    val step = (heightPx * 0.035f).coerceAtLeast(8f) // ~28 samples down the glyph height
    val contours = mutableListOf<List<Offset>>()
    val pos = FloatArray(2)
    do {
        val len = pm.length
        val pts = mutableListOf<Offset>()
        var d = 0f
        while (d <= len) {
            if (pm.getPosTan(d, pos, null)) pts.add(Offset(pos[0], pos[1]))
            d += step
        }
        if (pts.isNotEmpty()) contours.add(pts)
    } while (pm.nextContour())
    return contours
}

/**
 * Hand-authored CENTERLINE stroke paths, in WRITING ORDER and direction. Each entry is an
 * ordered list of strokes; each stroke is a polyline of NORMALIZED (0..1) points over the
 * tracing box, in the order/direction a child should draw it (start → end). A child must
 * complete stroke 0 before stroke 1, following each from its start dot — so this teaches real
 * penmanship (order + direction), not just shape coverage.
 *
 * Letters NOT in this map fall back to outline-coverage tracing (see [glyphContours] /
 * [CoverageTracingSurface]) — so adding a letter is purely additive and never regresses others.
 *
 * AUTHORING NOTE (important): coordinates must sit on top of the faint guide glyph (a centered
 * Compose Text). Add a letter ONLY after verifying on-device that its polyline overlays the
 * rendered glyph — a mismatched centerline would teach the WRONG shape. Persian is written
 * right-to-left; most isolated letters are one body stroke, with dots/marks as later strokes.
 */
private val letterStrokeData: Map<String, List<List<Offset>>> = mapOf(
    // آ (alef-madda): 1) the tall body, drawn top → bottom; 2) the madda "hat" on top.
    // Calibrated on-device against the rendered glyph (OnePlus Nord, default font).
    "آ" to listOf(
        listOf(Offset(0.51f, 0.30f), Offset(0.51f, 0.58f)),
        listOf(Offset(0.43f, 0.27f), Offset(0.57f, 0.27f))
    ),
    // ر (re): one stroke — down the right side, then curve left around the bowl into the tail.
    "ر" to listOf(
        listOf(Offset(0.56f, 0.43f), Offset(0.57f, 0.53f), Offset(0.55f, 0.62f), Offset(0.50f, 0.69f), Offset(0.45f, 0.72f), Offset(0.40f, 0.66f))
    ),
    // د (dal): one stroke — top tip, down the right side, then the foot sweeping left along the baseline.
    "د" to listOf(
        listOf(Offset(0.54f, 0.46f), Offset(0.57f, 0.52f), Offset(0.56f, 0.59f), Offset(0.49f, 0.61f), Offset(0.43f, 0.60f))
    ),
    // و (vav): one stroke — round head loop (clockwise from the top), then a short tail down-left.
    "و" to listOf(
        listOf(Offset(0.50f, 0.49f), Offset(0.58f, 0.56f), Offset(0.56f, 0.64f), Offset(0.49f, 0.67f), Offset(0.43f, 0.63f), Offset(0.43f, 0.55f), Offset(0.49f, 0.62f), Offset(0.45f, 0.71f))
    ),
    // ب (be): shallow bowl drawn right→left, then one dot below the centre.
    "ب" to listOf(
        listOf(Offset(0.62f, 0.56f), Offset(0.55f, 0.62f), Offset(0.47f, 0.645f), Offset(0.40f, 0.645f), Offset(0.33f, 0.595f)),
        listOf(Offset(0.49f, 0.73f), Offset(0.51f, 0.73f))
    ),
    // ت (te): same bowl, then two dots above.
    "ت" to listOf(
        listOf(Offset(0.62f, 0.56f), Offset(0.55f, 0.62f), Offset(0.47f, 0.645f), Offset(0.40f, 0.645f), Offset(0.33f, 0.595f)),
        listOf(Offset(0.43f, 0.45f), Offset(0.45f, 0.45f)),
        listOf(Offset(0.55f, 0.45f), Offset(0.57f, 0.45f))
    ),
    // ن (nun): deep bowl (right→left), then one dot above the centre.
    "ن" to listOf(
        listOf(Offset(0.58f, 0.50f), Offset(0.57f, 0.62f), Offset(0.50f, 0.70f), Offset(0.43f, 0.62f), Offset(0.40f, 0.50f)),
        listOf(Offset(0.49f, 0.47f), Offset(0.51f, 0.47f))
    ),
    // ی (ye): wide bowl with a return tail (right→left), then two dots below.
    "ی" to listOf(
        listOf(Offset(0.60f, 0.53f), Offset(0.59f, 0.63f), Offset(0.50f, 0.68f), Offset(0.43f, 0.64f), Offset(0.41f, 0.55f)),
        listOf(Offset(0.45f, 0.72f), Offset(0.47f, 0.72f)),
        listOf(Offset(0.53f, 0.72f), Offset(0.55f, 0.72f))
    ),
    // ز (ze): same body as ر, plus one dot above.
    "ز" to listOf(
        listOf(Offset(0.56f, 0.43f), Offset(0.57f, 0.53f), Offset(0.55f, 0.62f), Offset(0.50f, 0.69f), Offset(0.45f, 0.72f), Offset(0.40f, 0.66f)),
        listOf(Offset(0.51f, 0.40f), Offset(0.53f, 0.40f))
    ),
    // س (sin): three teeth then a shallow bowl tail, all right→left, no dots.
    "س" to listOf(
        listOf(Offset(0.64f, 0.58f), Offset(0.61f, 0.52f), Offset(0.58f, 0.58f), Offset(0.54f, 0.52f), Offset(0.51f, 0.58f), Offset(0.47f, 0.52f), Offset(0.44f, 0.58f), Offset(0.40f, 0.63f), Offset(0.45f, 0.66f))
    ),
    // م (mim): round head loop then a tail descending below the baseline, one stroke.
    "م" to listOf(
        listOf(Offset(0.54f, 0.52f), Offset(0.58f, 0.56f), Offset(0.54f, 0.60f), Offset(0.49f, 0.57f), Offset(0.52f, 0.52f), Offset(0.51f, 0.62f), Offset(0.48f, 0.72f))
    ),
    // ── steps 12–32 ── (bodies follow standard order: body first, then dots; jim-family = top stroke→hook)
    // ش (shin): sin body (three teeth into a bowl), then three dots above.
    "ش" to listOf(
        listOf(Offset(0.64f, 0.58f), Offset(0.61f, 0.52f), Offset(0.58f, 0.58f), Offset(0.54f, 0.52f), Offset(0.51f, 0.58f), Offset(0.47f, 0.52f), Offset(0.44f, 0.58f), Offset(0.40f, 0.63f), Offset(0.45f, 0.66f)),
        listOf(Offset(0.45f, 0.44f), Offset(0.47f, 0.44f)),
        listOf(Offset(0.51f, 0.44f), Offset(0.53f, 0.44f)),
        listOf(Offset(0.57f, 0.44f), Offset(0.59f, 0.44f))
    ),
    // ه (he): isolated loop/knot, one stroke (clockwise around the egg-shaped glyph).
    "ه" to listOf(
        listOf(Offset(0.50f, 0.48f), Offset(0.57f, 0.54f), Offset(0.55f, 0.62f), Offset(0.49f, 0.64f), Offset(0.43f, 0.60f), Offset(0.44f, 0.52f), Offset(0.50f, 0.48f))
    ),
    // پ (pe): be bowl, then three dots below.
    "پ" to listOf(
        listOf(Offset(0.62f, 0.56f), Offset(0.55f, 0.62f), Offset(0.47f, 0.645f), Offset(0.40f, 0.645f), Offset(0.33f, 0.595f)),
        listOf(Offset(0.41f, 0.72f), Offset(0.43f, 0.72f)),
        listOf(Offset(0.47f, 0.72f), Offset(0.49f, 0.72f)),
        listOf(Offset(0.53f, 0.72f), Offset(0.55f, 0.72f))
    ),
    // خ (khe): jim body (top stroke → hook below baseline), then one dot above.
    "خ" to listOf(
        listOf(Offset(0.38f, 0.46f), Offset(0.50f, 0.43f), Offset(0.60f, 0.45f), Offset(0.58f, 0.54f), Offset(0.50f, 0.63f), Offset(0.41f, 0.64f), Offset(0.36f, 0.56f)),
        listOf(Offset(0.49f, 0.39f), Offset(0.51f, 0.39f))
    ),
    // ف (fe): small head loop then a bowl to the left, one dot above the head.
    "ف" to listOf(
        listOf(Offset(0.52f, 0.45f), Offset(0.57f, 0.50f), Offset(0.52f, 0.55f), Offset(0.48f, 0.52f), Offset(0.51f, 0.46f), Offset(0.48f, 0.57f), Offset(0.41f, 0.62f), Offset(0.36f, 0.59f)),
        listOf(Offset(0.50f, 0.40f), Offset(0.52f, 0.40f))
    ),
    // ق (qaf): head then a deep round bowl below the baseline, two dots above.
    "ق" to listOf(
        listOf(Offset(0.52f, 0.47f), Offset(0.57f, 0.52f), Offset(0.52f, 0.57f), Offset(0.47f, 0.53f), Offset(0.52f, 0.48f), Offset(0.52f, 0.60f), Offset(0.48f, 0.72f), Offset(0.41f, 0.72f), Offset(0.39f, 0.61f)),
        listOf(Offset(0.45f, 0.41f), Offset(0.47f, 0.41f)),
        listOf(Offset(0.53f, 0.41f), Offset(0.55f, 0.41f))
    ),
    // ل (lam): tall stroke from the top, curving into a bowl at the bottom, one stroke.
    "ل" to listOf(
        listOf(Offset(0.56f, 0.30f), Offset(0.54f, 0.45f), Offset(0.53f, 0.58f), Offset(0.48f, 0.64f), Offset(0.40f, 0.62f))
    ),
    // ک (kaf): main stroke (down then a base to the right), plus the small inner mark.
    "ک" to listOf(
        listOf(Offset(0.46f, 0.32f), Offset(0.43f, 0.48f), Offset(0.42f, 0.60f), Offset(0.52f, 0.61f), Offset(0.62f, 0.59f)),
        listOf(Offset(0.47f, 0.46f), Offset(0.56f, 0.40f))
    ),
    // گ (gaf): like kaf, with a second slash on top — three strokes.
    "گ" to listOf(
        listOf(Offset(0.46f, 0.32f), Offset(0.43f, 0.48f), Offset(0.42f, 0.60f), Offset(0.52f, 0.61f), Offset(0.62f, 0.59f)),
        listOf(Offset(0.47f, 0.44f), Offset(0.58f, 0.38f)),
        listOf(Offset(0.47f, 0.37f), Offset(0.58f, 0.31f))
    ),
    // چ (che): jim body, then three dots below.
    "چ" to listOf(
        listOf(Offset(0.38f, 0.46f), Offset(0.50f, 0.43f), Offset(0.60f, 0.45f), Offset(0.58f, 0.54f), Offset(0.50f, 0.63f), Offset(0.41f, 0.64f), Offset(0.36f, 0.56f)),
        listOf(Offset(0.42f, 0.60f), Offset(0.44f, 0.60f)),
        listOf(Offset(0.48f, 0.60f), Offset(0.50f, 0.60f)),
        listOf(Offset(0.54f, 0.60f), Offset(0.56f, 0.60f))
    ),
    // ج (jim): jim body, then one dot below (inside the belly).
    "ج" to listOf(
        listOf(Offset(0.38f, 0.46f), Offset(0.50f, 0.43f), Offset(0.60f, 0.45f), Offset(0.58f, 0.54f), Offset(0.50f, 0.63f), Offset(0.41f, 0.64f), Offset(0.36f, 0.56f)),
        listOf(Offset(0.47f, 0.58f), Offset(0.49f, 0.58f))
    ),
    // ح (he-jimi): jim body, no dots.
    "ح" to listOf(
        listOf(Offset(0.38f, 0.46f), Offset(0.50f, 0.43f), Offset(0.60f, 0.45f), Offset(0.58f, 0.54f), Offset(0.50f, 0.63f), Offset(0.41f, 0.64f), Offset(0.36f, 0.56f))
    ),
    // ع (ein): open head curl at top, then a bowl below the baseline, one stroke.
    "ع" to listOf(
        listOf(Offset(0.58f, 0.46f), Offset(0.50f, 0.42f), Offset(0.47f, 0.50f), Offset(0.55f, 0.53f), Offset(0.52f, 0.62f), Offset(0.43f, 0.70f), Offset(0.50f, 0.75f), Offset(0.58f, 0.69f))
    ),
    // غ (ghein): ein body, then one dot above.
    "غ" to listOf(
        listOf(Offset(0.58f, 0.46f), Offset(0.50f, 0.42f), Offset(0.47f, 0.50f), Offset(0.55f, 0.53f), Offset(0.52f, 0.62f), Offset(0.43f, 0.70f), Offset(0.50f, 0.75f), Offset(0.58f, 0.69f)),
        listOf(Offset(0.54f, 0.39f), Offset(0.56f, 0.39f))
    ),
    // ط (ta): oval body, then a vertical ascender stroke upward — two strokes.
    "ط" to listOf(
        listOf(Offset(0.58f, 0.60f), Offset(0.50f, 0.64f), Offset(0.42f, 0.60f), Offset(0.50f, 0.56f), Offset(0.58f, 0.60f)),
        listOf(Offset(0.57f, 0.58f), Offset(0.57f, 0.32f))
    ),
    // ص (sad): closed eye/loop on the right, then a bowl to the left, one stroke.
    "ص" to listOf(
        listOf(Offset(0.58f, 0.55f), Offset(0.62f, 0.60f), Offset(0.56f, 0.63f), Offset(0.52f, 0.59f), Offset(0.57f, 0.55f), Offset(0.50f, 0.63f), Offset(0.42f, 0.64f), Offset(0.36f, 0.60f))
    ),
    // ض (zad): sad body, then one dot above.
    "ض" to listOf(
        listOf(Offset(0.58f, 0.55f), Offset(0.62f, 0.60f), Offset(0.56f, 0.63f), Offset(0.52f, 0.59f), Offset(0.57f, 0.55f), Offset(0.50f, 0.63f), Offset(0.42f, 0.64f), Offset(0.36f, 0.60f)),
        listOf(Offset(0.49f, 0.46f), Offset(0.51f, 0.46f))
    ),
    // ظ (za): ta body (oval + ascender), then one dot above.
    "ظ" to listOf(
        listOf(Offset(0.58f, 0.60f), Offset(0.50f, 0.64f), Offset(0.42f, 0.60f), Offset(0.50f, 0.56f), Offset(0.58f, 0.60f)),
        listOf(Offset(0.57f, 0.58f), Offset(0.57f, 0.32f)),
        listOf(Offset(0.45f, 0.46f), Offset(0.47f, 0.46f))
    ),
    // ذ (zal): dal body, then one dot above.
    "ذ" to listOf(
        listOf(Offset(0.54f, 0.46f), Offset(0.57f, 0.52f), Offset(0.56f, 0.59f), Offset(0.49f, 0.61f), Offset(0.43f, 0.60f)),
        listOf(Offset(0.51f, 0.38f), Offset(0.53f, 0.38f))
    ),
    // ث (se): be bowl, then three dots above.
    "ث" to listOf(
        listOf(Offset(0.62f, 0.56f), Offset(0.55f, 0.62f), Offset(0.47f, 0.645f), Offset(0.40f, 0.645f), Offset(0.33f, 0.595f)),
        listOf(Offset(0.41f, 0.46f), Offset(0.43f, 0.46f)),
        listOf(Offset(0.47f, 0.46f), Offset(0.49f, 0.46f)),
        listOf(Offset(0.53f, 0.46f), Offset(0.55f, 0.46f))
    ),
    // ژ (zhe): re body, then three dots above.
    "ژ" to listOf(
        listOf(Offset(0.56f, 0.43f), Offset(0.57f, 0.53f), Offset(0.55f, 0.62f), Offset(0.50f, 0.69f), Offset(0.45f, 0.72f), Offset(0.40f, 0.66f)),
        listOf(Offset(0.46f, 0.34f), Offset(0.48f, 0.34f)),
        listOf(Offset(0.52f, 0.34f), Offset(0.54f, 0.34f)),
        listOf(Offset(0.58f, 0.34f), Offset(0.60f, 0.34f))
    )
)

/** Euclidean distance between two points. */
private fun dist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x; val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/** Resample a polyline into points spaced ~`spacing` px apart (keeps first & last). */
private fun resamplePolyline(poly: List<Offset>, spacing: Float): List<Offset> {
    if (poly.size < 2) return poly
    val out = mutableListOf(poly.first())
    var prev = poly.first()
    var carry = 0f
    for (i in 1 until poly.size) {
        var segStart = prev
        val segEnd = poly[i]
        var segLen = dist(segStart, segEnd)
        while (segLen > 0f && carry + segLen >= spacing) {
            val t = (spacing - carry) / segLen
            val np = Offset(segStart.x + (segEnd.x - segStart.x) * t, segStart.y + (segEnd.y - segStart.y) * t)
            out.add(np)
            segStart = np
            segLen = dist(segStart, segEnd)
            carry = 0f
        }
        carry += segLen
        prev = segEnd
    }
    if (out.last() != poly.last()) out.add(poly.last())
    return out
}

/** Scale normalized (0..1) authored strokes to box pixels and resample each evenly. */
private fun scaledStrokes(strokes: List<List<Offset>>, w: Int, h: Int): List<List<Offset>> {
    if (w == 0 || h == 0) return emptyList()
    val spacing = (h * 0.035f).coerceAtLeast(8f)
    return strokes.map { poly -> resamplePolyline(poly.map { Offset(it.x * w, it.y * h) }, spacing) }
}

/**
 * Tracing entry point. Letters with hand-authored stroke data get the ordered, directional
 * stroke-following experience; every other letter falls back to the forgiving outline-coverage
 * tracer. This split keeps authoring incremental and risk-free.
 */
@Composable
fun TracingModeUI(item: com.github.opscalehub.chistanland.data.LearningItem, onComplete: () -> Unit) {
    if (letterStrokeData.containsKey(item.character)) {
        OrderedTracingSurface(item, letterStrokeData.getValue(item.character), onComplete)
    } else {
        CoverageTracingSurface(item, onComplete)
    }
}

/**
 * Real finger-tracing with SHAPE accuracy. We extract the letter's outline into target
 * points and light each one up only when the finger passes near it (forgiving tolerance
 * for small hands). Progress = fraction of the letter's shape actually traced — so a
 * random scribble in empty space won't fill it; the child must follow the glyph.
 */
@Composable
fun CoverageTracingSurface(item: com.github.opscalehub.chistanland.data.LearningItem, onComplete: () -> Unit) {
    val view = LocalView.current
    // remember(item.id): every state resets cleanly when the lesson advances to a new letter.
    val stroke = remember(item.id) { mutableStateListOf<Offset>() }
    var completed by remember(item.id) { mutableStateOf(false) }
    var boxSize by remember(item.id) { mutableStateOf(IntSize.Zero) }
    var progress by remember(item.id) { mutableFloatStateOf(0f) }

    // Target points grouped by contour (each separate piece of the letter, e.g. body + dot).
    val contours = remember(item.character, boxSize) { glyphContours(item.character, boxSize.width, boxSize.height) }
    val targets = remember(contours) { contours.flatten() } // flat list for drawing
    val hit = remember(item.id, contours) { contours.map { BooleanArray(it.size) } }
    var hitVersion by remember(item.id, contours) { mutableIntStateOf(0) } // bumps to trigger redraw

    val tolerance = remember(boxSize) { (boxSize.width * 0.07f).coerceAtLeast(38f) } // tighter: must hug the shape
    val perContourFraction = 0.7f // EVERY piece (incl. the dot) must be ~70% traced — defeats stray scribbles

    val scale by animateFloatAsState(if (completed) 1.15f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "TracingScale")

    fun isHit(globalIndex: Int): Boolean {
        var idx = globalIndex
        for (c in contours.indices) {
            if (idx < contours[c].size) return hit[c][idx]
            idx -= contours[c].size
        }
        return false
    }

    fun registerTouch(o: Offset) {
        if (contours.isEmpty()) return
        var changed = false
        val tol2 = tolerance * tolerance
        for (c in contours.indices) {
            val pts = contours[c]
            for (i in pts.indices) {
                if (!hit[c][i]) {
                    val dx = pts[i].x - o.x
                    val dy = pts[i].y - o.y
                    if (dx * dx + dy * dy <= tol2) { hit[c][i] = true; changed = true }
                }
            }
        }
        if (changed) {
            hitVersion++
            var totalHit = 0; var total = 0
            var allContoursCovered = true
            for (c in contours.indices) {
                val cHits = hit[c].count { it }
                totalHit += cHits; total += contours[c].size
                if (cHits.toFloat() / contours[c].size < perContourFraction) allContoursCovered = false
            }
            progress = if (total == 0) 0f else totalHit.toFloat() / total
            if (allContoursCovered) completed = true
        }
    }

    // Fire completion (with a celebratory beat) once enough of the shape is traced.
    LaunchedEffect(completed) {
        if (completed) {
            progress = 1f
            try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (e: Exception) {}
            delay(500)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Text("🎨 نِشانه جادویی", fontSize = 30.sp, fontWeight = FontWeight.Black, color = DeepOcean)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(listOf(Color.White, PastelGreen.copy(alpha = 0.1f))),
                    shape = CircleShape
                )
                .border(6.dp, if (completed) MangoOrange else PastelGreen, CircleShape)
                .shadow(if (completed) 30.dp else 10.dp, CircleShape)
                .clip(CircleShape)
                .onGloballyPositioned { boxSize = it.size }
                .pointerInput(item.id) {
                    // Handle the initial touch-down AND every move in one loop, so a plain TAP
                    // (no movement) counts too — needed to complete single-point dot strokes
                    // (e.g. the dots of ب ت ن), which detectDragGestures would ignore.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (!completed) { stroke.add(down.position); registerTouch(down.position) }
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { c ->
                                if (c.pressed && !completed) {
                                    stroke.add(c.position); registerTouch(c.position); c.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Faint full glyph so the child always sees the whole letter to aim for.
            Text(
                item.character,
                fontSize = 175.sp,
                fontWeight = FontWeight.Black,
                color = (if (completed) MangoOrange else PastelGreen).copy(alpha = if (completed) 0.9f else 0.22f)
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                hitVersion // read so the canvas redraws as dots get covered
                // Guide dots along the letter's outline: faint until traced, then orange.
                if (!completed) {
                    targets.forEachIndexed { i, p ->
                        val h = isHit(i)
                        drawCircle(
                            color = if (h) MangoOrange else DeepOcean.copy(alpha = 0.25f),
                            radius = if (h) 9f else 6f,
                            center = p
                        )
                    }
                }
                // The child's crayon stroke.
                for (i in 1 until stroke.size) {
                    drawLine(
                        color = MangoOrange,
                        start = stroke[i - 1],
                        end = stroke[i],
                        strokeWidth = 30f,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (completed) {
                Text("✨", modifier = Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = 40.dp), fontSize = 44.sp)
                Text("⭐", modifier = Modifier.align(Alignment.BottomStart).offset(x = 40.dp, y = (-40).dp), fontSize = 34.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Slim progress bar so the child sees the magic "filling up" as they trace.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PastelGreen.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(MangoOrange, RoundedCornerShape(6.dp))
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (completed) "آفرین! نِشانه رو کامل کشیدی! ✨" else "نقطه‌ها رو با انگشتت دنبال کن!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DeepOcean.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Ordered, directional stroke-following for letters with authored centerline strokes.
 * The child must trace stroke 0 from its start dot to its end, THEN stroke 1, and so on —
 * advancing only in order (small look-ahead keeps fast dragging forgiving). A pulsing green
 * dot marks where to start each stroke; finished strokes stay orange. This teaches writing
 * order and direction, not just shape.
 */
@Composable
fun OrderedTracingSurface(
    item: com.github.opscalehub.chistanland.data.LearningItem,
    authored: List<List<Offset>>,
    onComplete: () -> Unit
) {
    val view = LocalView.current
    val stroke = remember(item.id) { mutableStateListOf<Offset>() }
    var completed by remember(item.id) { mutableStateOf(false) }
    var boxSize by remember(item.id) { mutableStateOf(IntSize.Zero) }
    var progress by remember(item.id) { mutableFloatStateOf(0f) }

    val strokes = remember(item.character, boxSize) { scaledStrokes(authored, boxSize.width, boxSize.height) }
    val totalPts = remember(strokes) { strokes.sumOf { it.size } }
    var strokeIdx by remember(item.id, strokes) { mutableIntStateOf(0) }
    var cursor by remember(item.id, strokes) { mutableIntStateOf(-1) } // furthest in-order index reached; -1 = not started
    var version by remember(item.id, strokes) { mutableIntStateOf(0) }  // bumps to redraw the canvas

    val tolerance = remember(boxSize) { (boxSize.width * 0.11f).coerceAtLeast(48f) } // forgiving for small hands
    val lookahead = 4 // how far ahead along the stroke a single touch may advance the cursor

    val scale by animateFloatAsState(if (completed) 1.15f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "OrderedTracingScale")
    val startPulse by rememberInfiniteTransition(label = "startPulse").animateFloat(1f, 1.6f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "startPulseV")

    fun registerTouch(o: Offset) {
        if (completed || strokes.isEmpty() || strokeIdx >= strokes.size) return
        val cur = strokes[strokeIdx]
        val last = cur.size - 1
        val tol2 = tolerance * tolerance
        var best = cursor
        var j = (cursor + 1).coerceAtLeast(0)
        while (j <= minOf(cursor + lookahead, last)) {
            val dx = cur[j].x - o.x; val dy = cur[j].y - o.y
            if (dx * dx + dy * dy <= tol2) best = j
            j++
        }
        if (best > cursor) {
            cursor = best
            version++
            val donePts = (0 until strokeIdx).sumOf { strokes[it].size }
            progress = if (totalPts == 0) 0f else (donePts + cursor + 1).toFloat() / totalPts
            if (cursor >= last) {
                if (strokeIdx >= strokes.size - 1) completed = true
                else { strokeIdx++; cursor = -1 }
            }
        }
    }

    LaunchedEffect(completed) {
        if (completed) {
            progress = 1f
            try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (e: Exception) {}
            delay(500)
            onComplete()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
        Text("🎨 نِشانه جادویی", fontSize = 30.sp, fontWeight = FontWeight.Black, color = DeepOcean)
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(scale)
                .background(brush = Brush.radialGradient(listOf(Color.White, PastelGreen.copy(alpha = 0.1f))), shape = CircleShape)
                .border(6.dp, if (completed) MangoOrange else PastelGreen, CircleShape)
                .shadow(if (completed) 30.dp else 10.dp, CircleShape)
                .clip(CircleShape)
                .onGloballyPositioned { boxSize = it.size }
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragStart = { offset -> if (!completed) { stroke.add(offset); registerTouch(offset) } },
                        onDrag = { change, _ -> if (!completed) { change.consume(); stroke.add(change.position); registerTouch(change.position) } }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Faint whole glyph so the child always sees the letter to aim for.
            Text(item.character, fontSize = 175.sp, fontWeight = FontWeight.Black,
                color = (if (completed) MangoOrange else PastelGreen).copy(alpha = if (completed) 0.9f else 0.22f))

            Canvas(modifier = Modifier.matchParentSize()) {
                version // read so the canvas redraws as the cursor advances
                if (!completed) {
                    strokes.forEachIndexed { s, pts ->
                        when {
                            s < strokeIdx -> pts.forEach { drawCircle(MangoOrange, 8f, it) } // finished stroke
                            s == strokeIdx -> {
                                pts.forEachIndexed { i, p ->
                                    val done = i <= cursor
                                    drawCircle(if (done) MangoOrange else DeepOcean.copy(alpha = 0.3f), if (done) 9f else 6f, p)
                                }
                                // Pulsing green "start here" marker on the next expected point.
                                val nextIdx = (cursor + 1).coerceIn(0, pts.size - 1)
                                drawCircle(PastelGreen, 7f * startPulse, pts[nextIdx])
                                drawCircle(Color(0xFF2E7D32), 7f, pts[nextIdx])
                            }
                            else -> pts.forEach { drawCircle(DeepOcean.copy(alpha = 0.12f), 5f, it) } // upcoming stroke (faint)
                        }
                    }
                }
                for (i in 1 until stroke.size) {
                    drawLine(MangoOrange, stroke[i - 1], stroke[i], strokeWidth = 30f, cap = StrokeCap.Round)
                }
            }

            if (completed) {
                Text("✨", modifier = Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = 40.dp), fontSize = 44.sp)
                Text("⭐", modifier = Modifier.align(Alignment.BottomStart).offset(x = 40.dp, y = (-40).dp), fontSize = 34.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(PastelGreen.copy(alpha = 0.2f))) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(MangoOrange, RoundedCornerShape(6.dp)))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            when {
                completed -> "آفرین! نِشانه رو کامل کشیدی! ✨"
                strokes.size > 1 -> "از نقطه‌ی سبز شروع کن — خط ${strokeIdx + 1} از ${strokes.size}"
                else -> "از نقطه‌ی سبز شروع کن و دنبالش کن!"
            },
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepOcean.copy(alpha = 0.7f), textAlign = TextAlign.Center
        )
    }
}

/**
 * Image-pick recognition: the child hears/sees the word and taps the matching picture among
 * choices. Reinforces word→meaning. Pictures come from the shared emoji map, so no art assets.
 */
@Composable
fun WordRecognitionUI(
    item: com.github.opscalehub.chistanland.data.LearningItem,
    options: List<com.github.opscalehub.chistanland.data.LearningItem>,
    enabled: Boolean,
    onPick: (com.github.opscalehub.chistanland.data.LearningItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📷 کدوم تصویره؟", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepOcean)
        Spacer(modifier = Modifier.height(20.dp))

        // The target word (written), so the child links the heard word to its letters.
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(3.dp, SkyBlue.copy(alpha = 0.3f)),
            modifier = Modifier.shadow(10.dp, RoundedCornerShape(28.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(item.word, fontSize = 40.sp, fontWeight = FontWeight.Black, color = SkyBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Default.PlayArrow, null, tint = MangoOrange, modifier = Modifier.size(30.dp))
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Picture choices — tap the one that matches the word.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            options.forEach { option ->
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(3.dp, PastelGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp))
                        .clickable(enabled = enabled) { onPick(option) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(getEmojiForWord(option.word), fontSize = 56.sp)
                    }
                }
            }
        }
    }
}

/**
 * Shows a letter's four positional forms (isolated / initial / medial / final) so the child
 * learns that the *same* letter changes shape when it joins its neighbours — the single
 * hardest part of reading Persian. Forms are produced with zero-width joiners (U+200D), so
 * the font renders the correct shape; non-connecting letters (ا د ر ز و …) naturally show
 * little change, which is itself correct.
 */
@Composable
fun LetterFormsStrip(letter: String) {
    val zwj = "\u200D" // zero-width joiner: makes the font render initial/medial/final shapes
    val forms = listOf(
        "تنها" to letter,
        "آغاز" to "$letter$zwj",
        "میانه" to "$zwj$letter$zwj",
        "پایان" to "$zwj$letter"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("این نِشانه چه شکل‌هایی داره؟", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepOcean.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            forms.forEach { (label, glyph) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SkyBlue.copy(alpha = 0.12f),
                        border = BorderStroke(2.dp, SkyBlue.copy(alpha = 0.35f))
                    ) {
                        Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                            Text(glyph, fontSize = 34.sp, fontWeight = FontWeight.Black, color = DeepOcean)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(label, fontSize = 12.sp, color = DeepOcean.copy(alpha = 0.55f), fontWeight = FontWeight.Bold)
                }
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
    modifier: Modifier = Modifier,
    newLetter: String = ""
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
            // The newly-introduced letter is highlighted in red inside the word (pedagogical cue).
            val isNewLetter = newLetter.isNotEmpty() && char.toString() == newLetter && status == null && !isMissing

            val color = when {
                status == true -> PastelGreen
                status == false -> Color.Red
                isMissing -> MangoOrange
                isNewLetter -> Color(0xFFE53935)
                else -> SkyBlue.copy(alpha = 0.3f)
            }
            
            val displayText = if (isMissing && !isTyped) "?" else char.toString()
            val textAlpha = if (isTyped || isNewLetter) 1f else 0.5f
            val scale by animateFloatAsState(if (isTyped) 1.25f else 1f, label = "charScale")
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                    text = displayText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = color.copy(alpha = textAlpha),
                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                )
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(if (isMissing) 10.dp else 6.dp)
                        .background(color, RoundedCornerShape(3.dp))
                        .shadow(if (isMissing) 4.dp else 0.dp, RoundedCornerShape(3.dp))
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
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val emoji = getEmojiForWord(item.word)
                val context = LocalContext.current

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    if (emoji != "🌟") {
                        Text(text = emoji, fontSize = 120.sp)
                    } else {
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
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(item.word, fontSize = 42.sp, fontWeight = FontWeight.Black, color = SkyBlue)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = MangoOrange, modifier = Modifier.size(32.dp))
                    Text("بشنو", color = MangoOrange, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun SuccessFestivalOverlay() {
    val context = LocalContext.current
    val resId = remember { try { context.resources.getIdentifier("success_fest_anim", "raw", context.packageName) } catch(e: Exception) { 0 } }
    
    if (resId != 0) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            LottieAnimation(
                composition = composition, 
                iterations = LottieConstants.IterateForever, 
                modifier = Modifier.size(450.dp)
            )
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(4.dp, MangoOrange),
                modifier = Modifier.shadow(16.dp, RoundedCornerShape(32.dp))
            ) {
                Text(
                    "✨ صد آفرین! ✨", 
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    fontSize = 44.sp, 
                    fontWeight = FontWeight.Black, 
                    color = MangoOrange
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
            Text("✨ تبریک! ✨", fontSize = 64.sp, fontWeight = FontWeight.Black, color = MangoOrange)
        }
    }
}

fun getEmojiForWord(word: String): String {
    return when(word) {
        // زنجیره یادگیری ۳۲ حرفی (Concrete V2.1) — هر کلمه یک تصویر ذهنی روشن
        // «آ» شیء ملموسی ندارد؛ خودِ نشانه نمایش داده می‌شود (نه حرف انگلیسی A).
        "آ" -> "آ"
        // واژه‌های نمونه (تمرین عمیق‌تر با حروفِ یادگرفته‌شده)
        "آب" -> "💧"
        "بادام" -> "🌰"
        "آدم" -> "🧍"
        "مار" -> "🐍"
        "مادر" -> "👩"
        "بز" -> "🐐"
        "رود" -> "🏞️"
        "بازو" -> "💪"
        "اسب" -> "🐴"
        "سوسمار" -> "🦎"
        "آسمان" -> "☁️"
        "نردبان" -> "🪜"
        "توت" -> "🫐"
        "دست" -> "✋"
        "دریا" -> "🌊"
        "میز" -> "🪑"
        "موش" -> "🐭"
        "ماه" -> "🌙"
        "پروانه" -> "🦋"
        "خرس" -> "🐻"
        "آفتاب" -> "☀️"
        "قاشق" -> "🥄"
        "فیل" -> "🐘"
        "کفش" -> "👟"
        "گاو" -> "🐄"
        "چتر" -> "☂️"
        "جنگل" -> "🌲"
        "حلزون" -> "🐌"
        "عینک" -> "👓"
        "مرغ" -> "🐔"
        "طوطی" -> "🦜"
        "صدف" -> "🐚"
        "حوض" -> "⛲"
        "کاغذ" -> "📄"
        "ژاکت" -> "🧥"
        "بابا" -> "🧔"
        "باد" -> "🌬️"
        "بام" -> "🏠"
        "آرد" -> "🌾"
        "بازار" -> "🏪"
        "دود" -> "💨"
        "سبد" -> "🧺"
        "نان" -> "🍞"
        "تاب" -> "🛝"
        "سینی" -> "🍽️"
        "آش" -> "🍲"
        "کوه" -> "⛰️"
        "توپ" -> "⚽"
        "شاخ" -> "🦌"
        "برف" -> "❄️"
        "قند" -> "🍬"
        "گل" -> "🌷"
        "کارد" -> "🔪"
        "سگ" -> "🐕"
        "چادر" -> "⛺"
        "جوجه" -> "🐤"
        "حوله" -> "🧺"
        "عروسک" -> "🧸"
        "غار" -> "🕳️"
        "طناب" -> "🪢"
        "صابون" -> "🧼"
        "وضو" -> "🚰"
        "ظرف" -> "🍽️"
        "ذره‌بین" -> "🔍"
        "مثلث" -> "🔺"
        "ژله" -> "🍮"
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
    
    Column(modifier = Modifier.wrapContentWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(modifier = Modifier.wrapContentWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
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
                scaleX = if (isDragging) 1.25f else scalePulse
                scaleY = if (isDragging) 1.25f else scalePulse
                shadowElevation = if (isDragging) 20f else 6f
            }
            .pointerInput(isDragEnabled) {
                if (!isDragEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isDragging = true; view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
                    onDragEnd = {
                        isDragging = false
                        onDroppedOnTarget(char)
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
            .shadow(if (isHighlighted || isDragging) 12.dp else 4.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp), color = animatedBgColor, border = BorderStroke(3.dp, Color.White.copy(alpha = 0.7f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(char, fontSize = (size.value * 0.45).sp, fontWeight = FontWeight.ExtraBold, color = if (isHighlighted) DeepOcean else Color.White)
        }
    }
}
