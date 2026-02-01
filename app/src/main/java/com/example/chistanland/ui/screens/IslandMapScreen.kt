package com.example.chistanland.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.chistanland.data.LearningItem
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslandMapScreen(
    viewModel: LearningViewModel,
    onStartItem: (LearningItem) -> Unit,
    onStartReview: (List<LearningItem>) -> Unit,
    onOpenParentPanel: () -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.filteredItems.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    
    val allMastered = remember(items) { items.isNotEmpty() && items.all { it.isMastered } }
    var showFinalCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(allMastered) {
        if (allMastered && items.isNotEmpty() && category == "ALPHABET") {
            delay(800)
            showFinalCelebration = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (category == "NUMBER") 
                        listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color.White)
                    else 
                        listOf(SkyBlue.copy(alpha = 0.5f), Color.White)
                )
            )
    ) {
        if (category == "NUMBER") {
            NumberPeakDecorations()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            MapHeader(
                category = category ?: "",
                onOpenParentPanel = onOpenParentPanel,
                onBack = onBack
            )
            
            if (items.isEmpty()) {
                EmptyMapState { viewModel.seedData() }
            } else {
                SagaMap(items, category ?: "", onStartItem, onStartReview)
            }
        }

        if (showFinalCelebration) {
            FinalVictoryOverlay(onClose = { showFinalCelebration = false })
        }
    }
}

@Composable
fun NumberPeakDecorations() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("☁️", Modifier.offset(x = 20.dp, y = 100.dp).alpha(0.6f), fontSize = 40.sp)
        Text("☁️", Modifier.align(Alignment.TopEnd).offset(x = (-30).dp, y = 180.dp).alpha(0.4f), fontSize = 50.sp)
        Text("❄️", Modifier.offset(x = 100.dp, y = 300.dp).alpha(0.3f), fontSize = 20.sp)
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

@Composable
fun FinalVictoryOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier("success_fest_anim", "raw", context.packageName)
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
fun MapHeader(category: String, onOpenParentPanel: () -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DeepOcean) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (category == "NUMBER") "قله اعداد" else "جزیره‌های دانایی", fontSize = 28.sp, fontWeight = FontWeight.Black, color = DeepOcean)
        }
        Surface(
            modifier = Modifier.size(48.dp).pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenParentPanel() }) },
            shape = CircleShape, color = Color.White.copy(alpha = 0.5f), border = BorderStroke(2.dp, SkyBlue.copy(alpha = 0.3f))
        ) { Icon(Icons.Default.Settings, null, modifier = Modifier.padding(12.dp), tint = DeepOcean.copy(alpha = 0.4f)) }
    }
}

@Composable
fun SagaMap(items: List<LearningItem>, category: String, onStartItem: (LearningItem) -> Unit, onStartReview: (List<LearningItem>) -> Unit) {
    val firstLockedIndex = items.indexOfFirst { !it.isMastered }.let { if (it == -1) items.size - 1 else it }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (firstLockedIndex - 1).coerceAtLeast(0))

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp, top = 20.dp)) {
        itemsIndexed(items) { index, item ->
            val isLocked = index > 0 && !items[index - 1].isMastered
            val isEven = index % 2 == 0
            
            Column(horizontalAlignment = if (isEven) Alignment.Start else Alignment.End) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = if (isEven) Alignment.CenterStart else Alignment.CenterEnd) {
                    if (index < items.size - 1) PathConnection(isEven, category)
                    IslandNode(item = item, isLocked = isLocked, modifier = Modifier.padding(horizontal = 40.dp), onClick = { onStartItem(item) })
                }
                if (category == "ALPHABET" && (index + 1) % 3 == 0 && index < items.size - 1) {
                    AmusementParkNode(isLocked = !item.isMastered, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), onClick = { onStartReview(items.take(index + 1)) })
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
fun PathConnection(isEven: Boolean, category: String) {
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp).offset(y = 90.dp)) {
        val path = Path().apply {
            if (isEven) { moveTo(size.width * 0.25f, 0f); cubicTo(size.width * 0.25f, size.height * 0.5f, size.width * 0.75f, size.height * 0.5f, size.width * 0.75f, size.height) }
            else { moveTo(size.width * 0.75f, 0f); cubicTo(size.width * 0.75f, size.height * 0.5f, size.width * 0.25f, size.height * 0.5f, size.width * 0.25f, size.height) }
        }
        
        if (category == "NUMBER") {
            drawPath(path = path, color = Color.Gray.copy(alpha = 0.2f), style = Stroke(width = 12f))
            drawPath(path = path, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), 0f)))
        } else {
            drawPath(path = path, color = DeepOcean.copy(alpha = 0.1f), style = Stroke(width = 8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)))
        }
    }
}

@Composable
fun IslandNode(item: LearningItem, isLocked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "island")
    val floatAnim by infiniteTransition.animateFloat(0f, 10f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "float")
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.graphicsLayer(translationY = if (!isLocked) floatAnim else 0f).clickable(enabled = !isLocked) { onClick() }) {
        Box(contentAlignment = Alignment.Center) {
            if (!isLocked && item.category == "NUMBER") {
                QuantityIndicator(item.character)
            }

            Surface(
                modifier = Modifier.size(115.dp).shadow(if (isLocked) 0.dp else 12.dp, CircleShape),
                shape = CircleShape,
                color = when { 
                    isLocked -> Color(0xFFE0E0E0)
                    item.isMastered -> Color(0xFFFFD600)
                    else -> if (item.category == "NUMBER") MangoOrange else PastelGreen 
                },
                border = BorderStroke(4.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLocked) Icon(Icons.Default.Lock, null, tint = Color.Gray)
                    else {
                        val emoji = getEmojiForItem(item)
                        if (emoji != "🌟" && emoji != "🔢") {
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
                    shape = CircleShape, color = if (item.category == "NUMBER") DeepOcean else DeepOcean, border = BorderStroke(2.dp, Color.White)
                ) { Box(contentAlignment = Alignment.Center) { Text(item.character, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) } }
            }
            if (item.isMastered) Icon(Icons.Default.Star, null, tint = MangoOrange, modifier = Modifier.size(32.dp).align(Alignment.TopStart).offset(x = (-6).dp, y = (-6).dp).rotate(-15f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = if (isLocked) "؟؟؟" else item.word, fontWeight = FontWeight.Bold, color = if (isLocked) Color.Gray else DeepOcean, fontSize = 16.sp)
    }
}

@Composable
fun QuantityIndicator(numberChar: String, dotSize: Dp = 12.dp, radiusSize: Dp = 72.dp) {
    val count = remember(numberChar) {
        when(numberChar) {
            "۱" -> 1; "۲" -> 2; "۳" -> 3; "۴" -> 4; "۵" -> 5
            "۶" -> 6; "۷" -> 7; "۸" -> 8; "۹" -> 9; else -> 0
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val angle by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "rotate")
    val pulse by infiniteTransition.animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")

    if (count > 0) {
        Box(modifier = Modifier.size(radiusSize * 2 + dotSize), contentAlignment = Alignment.Center) {
            repeat(count) { i ->
                val dotAngle = (angle + (i * (360f / count))) * (Math.PI / 180f).toFloat()
                Box(
                    modifier = Modifier
                        .offset(
                            x = (radiusSize.value * cos(dotAngle)).dp,
                            y = (radiusSize.value * sin(dotAngle)).dp
                        )
                        .size(dotSize * pulse)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White, MangoOrange, Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .shadow(6.dp, CircleShape)
                )
            }
        }
    }
}

fun getEmojiForItem(item: LearningItem): String {
    if (item.category == "NUMBER") {
        return when(item.character) {
            "۰" -> "🥚"; "۱" -> "🦒"; "۲" -> "🦢"; "۳" -> "🦋"; "۴" -> "🍀"
            "۵" -> "🖐️"; "۶" -> "🐌"; "۷" -> "🌈"; "۸" -> "🐙"; "۹" -> "🎈"
            else -> "🔢"
        }
    }
    return when(item.word) {
        "آب" -> "💧"; "بابا" -> "🧔"; "باد" -> "🌬️"; "بام" -> "🏠"; "سبد" -> "🧺"
        "نان" -> "🍞"; "ابر" -> "☁️"; "دست" -> "🖐️"; "بوم" -> "🖼️"; "سیب" -> "🍎"
        "باز" -> "🦅"; "آش" -> "🥣"; "کتاب" -> "📚"; "سگ" -> "🐕"; "برف" -> "❄️"
        "شاخ" -> "🦌"; "قایق" -> "⛵"; "لباس" -> "👕"; "تاج" -> "👑"; "چای" -> "🍵"
        "کوه" -> "⛰️"; "ژله" -> "🍮"; "صورت" -> "👤"; "ذرت" -> "🌽"; "عینک" -> "👓"
        "ثروت" -> "💰"; "حلزون" -> "🐌"; "ضامن" -> "🛡️"; "طوطی" -> "🦜"; "غذا" -> "🍲"; "ظرف" -> "🍽️"
        else -> "🌟"
    }
}
