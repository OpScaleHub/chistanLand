package com.github.opscalehub.chistanland.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.ui.LearningViewModel
import com.github.opscalehub.chistanland.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun IslandMapScreen(
    viewModel: LearningViewModel,
    onStartSession: (LearningItem) -> Unit,
    onStartReview: (List<LearningItem>) -> Unit,
    onOpenParentPanel: () -> Unit,
    onOpenStickers: () -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.filteredItems.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    val dailyStreak by viewModel.dailyStreak.collectAsState()

    val allMastered = remember(items) { items.isNotEmpty() && items.all { it.isMastered } }
    var showFinalCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(allMastered) {
        if (allMastered && items.isNotEmpty()) {
            delay(800)
            showFinalCelebration = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SkyBlue.copy(alpha = 0.5f), Color.White)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MapHeader(
                dailyStreak = dailyStreak,
                onOpenParentPanel = onOpenParentPanel,
                onOpenStickers = onOpenStickers,
                onBack = onBack
            )

            if (items.isEmpty()) {
                EmptyMapState { viewModel.seedData() }
            } else {
                SagaMap(items, onStartSession, onStartReview)
            }
        }

        if (showFinalCelebration) {
            FinalVictoryOverlay(onClose = { showFinalCelebration = false })
        }
    }
}

@Composable
fun EmptyMapState(onSeed: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("هنوز چیزی اینجا نیست!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = DeepOcean, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text("برای شروع یادگیری، دکمه زیر رو بزن تا جزیره‌ها ظاهر بشن.", fontSize = 16.sp, color = DeepOcean.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onSeed, colors = ButtonDefaults.buttonColors(containerColor = PastelGreen), shape = RoundedCornerShape(16.dp), modifier = Modifier.height(56.dp)) {
                Text("شروع ماجراجویی", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
fun FinalVictoryOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    val resId = remember { context.resources.getIdentifier("success_fest_anim", "raw", context.packageName) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(if (resId != 0) resId else 1))

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            if (composition != null) {
                LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(400.dp))
            }
            Text("🎉 قهرمان الفبا! 🎉", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
            Text("تو تمام نشانه‌ها رو با موفقیت یاد گرفتی!", fontSize = 20.sp, color = MangoOrange, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = PastelGreen), shape = RoundedCornerShape(24.dp), modifier = Modifier.height(64.dp).fillMaxWidth(0.7f)) {
                Text("بسیار عالی!", fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun MapHeader(dailyStreak: Int, onOpenParentPanel: () -> Unit, onOpenStickers: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeepOcean) }
            Text(text = "جزیره‌های دانایی", fontSize = 24.sp, fontWeight = FontWeight.Black, color = DeepOcean)
            if (dailyStreak > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                DailyStreakChip(dailyStreak)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sticker album — the collection reward
            Surface(
                modifier = Modifier.size(48.dp).clickable { onOpenStickers() },
                shape = CircleShape, color = Color.White.copy(alpha = 0.7f), border = BorderStroke(2.dp, MangoOrange.copy(alpha = 0.4f))
            ) { Box(contentAlignment = Alignment.Center) { Text("📒", fontSize = 22.sp) } }
            Spacer(modifier = Modifier.width(10.dp))
            // Parent panel (long-press, hidden from kids)
            Surface(
                modifier = Modifier.size(48.dp).pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenParentPanel() }) },
                shape = CircleShape, color = Color.White.copy(alpha = 0.5f), border = BorderStroke(2.dp, SkyBlue.copy(alpha = 0.3f))
            ) { Icon(Icons.Default.Settings, null, modifier = Modifier.padding(12.dp), tint = DeepOcean.copy(alpha = 0.4f)) }
        }
    }
}

@Composable
fun DailyStreakChip(days: Int) {
    val faDays = days.toString().map { c -> if (c in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[c - '0'] else c }.joinToString("")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MangoOrange.copy(alpha = 0.15f),
        border = BorderStroke(2.dp, MangoOrange.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("🔥", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(3.dp))
            Text("$faDays روز", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MangoOrange)
        }
    }
}

@Composable
fun SagaMap(items: List<LearningItem>, onStartSession: (LearningItem) -> Unit, onStartReview: (List<LearningItem>) -> Unit) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp, top = 20.dp)) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            // منطق مترقی: جزیره زمانی قفل است که آیتم قبلی هنوز حتی یک بار هم دیده نشده باشد
            val isLocked = index > 0 && items[index - 1].lastReviewTime == 0L
            val isEven = index % 2 == 0

            Column(horizontalAlignment = if (isEven) Alignment.Start else Alignment.End) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = if (isEven) Alignment.CenterStart else Alignment.CenterEnd) {
                    if (index < items.size - 1) PathConnection(isEven)
                    IslandNode(item = item, isLocked = isLocked, modifier = Modifier.padding(horizontal = 40.dp), onClick = { onStartSession(item) })
                }
                
                // شهربازی مرور بعد از هر ۳ جزیره
                if ((index + 1) % 3 == 0 && index < items.size - 1) {
                    // منطق اصلاح شده: شهربازی زمانی باز می‌شود که تمام حروفِ قبل از آن حداقل یک بار دیده شده باشند
                    val reviewItems = items.take(index + 1)
                    val isReviewLocked = reviewItems.any { it.lastReviewTime == 0L }
                    
                    AmusementParkNode(
                        isLocked = isReviewLocked, 
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), 
                        onClick = { onStartReview(reviewItems) }
                    )
                }
            }
        }
    }
}

@Composable
fun AmusementParkNode(isLocked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(100.dp).clickable(enabled = !isLocked) { onClick() }.shadow(if (isLocked) 0.dp else 15.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp), color = if (isLocked) Color.LightGray else Color(0xFFFF4081), border = BorderStroke(4.dp, Color.White)
            ) { Box(contentAlignment = Alignment.Center) { if (isLocked) Icon(Icons.Default.Lock, null, tint = Color.Gray) else Text("🎡", fontSize = 50.sp) } }
            Text("شهربازی مرور", color = if (isLocked) Color.Gray else Color(0xFFFF4081), fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun PathConnection(isEven: Boolean) {
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp).offset(y = 90.dp)) {
        val path = Path().apply {
            if (isEven) { moveTo(size.width * 0.25f, 0f); cubicTo(size.width * 0.25f, size.height * 0.5f, size.width * 0.75f, size.height * 0.5f, size.width * 0.75f, size.height) }
            else { moveTo(size.width * 0.75f, 0f); cubicTo(size.width * 0.75f, size.height * 0.5f, size.width * 0.25f, size.height * 0.5f, size.width * 0.25f, size.height) }
        }

        drawPath(path = path, color = DeepOcean.copy(alpha = 0.1f), style = Stroke(width = 8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)))
    }
}

@SuppressLint("DiscouragedApi")
@Composable
fun IslandNode(item: LearningItem, isLocked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "island")
    val floatAnim by infiniteTransition.animateFloat(0f, 10f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "float")
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.graphicsLayer(translationY = if (!isLocked) floatAnim else 0f).clickable(enabled = !isLocked) { onClick() }) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(115.dp).shadow(if (isLocked) 0.dp else 12.dp, CircleShape),
                shape = CircleShape,
                color = when {
                    isLocked -> Color(0xFFE0E0E0)
                    item.isMastered -> Color(0xFFFFD600)
                    else -> PastelGreen
                },
                border = BorderStroke(4.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLocked) Icon(Icons.Default.Lock, null, tint = Color.Gray)
                    else {
                        val emoji = getEmojiForItem(item)
                        if (emoji != "🌟") {
                            Text(text = emoji, fontSize = 54.sp)
                        } else {
                            val imageResId = remember(item.imageUrl) { context.resources.getIdentifier(item.imageUrl, "drawable", context.packageName) }
                            if (imageResId != 0) {
                                Image(painter = painterResource(id = imageResId), null, modifier = Modifier.size(70.dp).clip(CircleShape))
                            } else {
                                Text(text = emoji, fontSize = 54.sp)
                            }
                        }
                    }
                }
            }
            if (!isLocked) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(36.dp),
                    shape = CircleShape, color = DeepOcean, border = BorderStroke(2.dp, Color.White)
                ) { Box(contentAlignment = Alignment.Center) { Text(item.character, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) } }
            }
            if (item.isMastered) Icon(Icons.Default.Star, null, tint = MangoOrange, modifier = Modifier.size(32.dp).align(Alignment.TopStart).offset(x = (-6).dp, y = (-6).dp).rotate(-15f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = if (isLocked) "؟؟؟" else item.word, fontWeight = FontWeight.Bold, color = if (isLocked) Color.Gray else DeepOcean, fontSize = 16.sp)
    }
}

// Reuse the single source of truth for word→picture (kept in LearningSessionScreen),
// so the map, word cards and sticker album never drift out of sync again.
fun getEmojiForItem(item: LearningItem): String = getEmojiForWord(item.word)
