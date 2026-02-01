package com.example.chistanland.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chistanland.data.LearningItem
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.*

@Composable
fun IslandMapScreen(
    viewModel: LearningViewModel,
    onStartItem: (LearningItem) -> Unit,
    onStartReview: (List<LearningItem>) -> Unit, // تغییر در اینجا
    onOpenParentPanel: () -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.filteredItems.collectAsState()
    val category by viewModel.selectedCategory.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (category == "NUMBER") 
                        listOf(MangoOrange.copy(alpha = 0.2f), Color.White)
                    else 
                        listOf(SkyBlue.copy(alpha = 0.5f), Color.White)
                )
            )
    ) {
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
    }
}

@Composable
fun MapHeader(
    category: String,
    onOpenParentPanel: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepOcean)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (category == "NUMBER") "قله اعداد" else "جزیره‌های دانایی",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepOcean
                )
            }
        }
        
        Surface(
            modifier = Modifier.size(48.dp).pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenParentPanel() })
            },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.5f),
            border = BorderStroke(2.dp, SkyBlue.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Settings, null, modifier = Modifier.padding(12.dp), tint = DeepOcean.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun SagaMap(
    items: List<LearningItem>,
    category: String,
    onStartItem: (LearningItem) -> Unit,
    onStartReview: (List<LearningItem>) -> Unit // تغییر در اینجا
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 20.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val isLocked = index > 0 && !items[index - 1].isMastered
            val isEven = index % 2 == 0
            
            Column(horizontalAlignment = if (isEven) Alignment.Start else Alignment.End) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = if (isEven) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (index < items.size - 1) PathConnection(isEven)
                    
                    IslandNode(
                        item = item,
                        isLocked = isLocked,
                        modifier = Modifier.padding(horizontal = 40.dp),
                        onClick = { onStartItem(item) }
                    )
                }

                if (category == "ALPHABET" && (index + 1) % 3 == 0 && index < items.size - 1) {
                    val reviewLocked = !item.isMastered
                    AmusementParkNode(
                        isLocked = reviewLocked,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        onClick = { 
                            // فقط آیتم‌هایی که تا قبل از این شهربازی هستند برای مرور ارسال می‌شوند
                            val allowedItems = items.take(index + 1)
                            onStartReview(allowedItems) 
                        }
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
                modifier = Modifier
                    .size(100.dp)
                    .clickable(enabled = !isLocked) { onClick() }
                    .shadow(if (isLocked) 0.dp else 15.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = if (isLocked) Color.LightGray else Color(0xFFFF4081),
                border = BorderStroke(4.dp, Color.White)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLocked) {
                        Icon(Icons.Default.Lock, null, tint = Color.Gray)
                    } else {
                        Text("🎡", fontSize = 50.sp)
                    }
                }
            }
            Text(
                "شهربازی مرور",
                color = if (isLocked) Color.Gray else Color(0xFFFF4081),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PathConnection(isEven: Boolean) {
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp).offset(y = 90.dp)) {
        val path = Path().apply {
            if (isEven) {
                moveTo(size.width * 0.25f, 0f)
                cubicTo(size.width * 0.25f, size.height * 0.5f, size.width * 0.75f, size.height * 0.5f, size.width * 0.75f, size.height)
            } else {
                moveTo(size.width * 0.75f, 0f)
                cubicTo(size.width * 0.75f, size.height * 0.5f, size.width * 0.25f, size.height * 0.5f, size.width * 0.25f, size.height)
            }
        }
        drawPath(path = path, color = DeepOcean.copy(alpha = 0.1f), style = Stroke(width = 8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)))
    }
}

@Composable
fun IslandNode(item: LearningItem, isLocked: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "island")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "float"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.graphicsLayer(translationY = if (!isLocked) floatAnim else 0f).clickable(enabled = !isLocked) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(110.dp).shadow(if (isLocked) 0.dp else 10.dp, CircleShape),
                shape = CircleShape,
                color = when {
                    isLocked -> Color(0xFFE0E0E0)
                    item.isMastered -> Color(0xFFFFD600)
                    else -> if (item.category == "NUMBER") MangoOrange else PastelGreen
                },
                border = BorderStroke(4.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLocked) Icon(Icons.Default.Lock, null, tint = Color.Gray)
                    else Text(item.character, fontSize = 44.sp, fontWeight = FontWeight.Black, color = if (item.isMastered || item.category == "NUMBER") DeepOcean else Color.White)
                }
            }
            if (item.isMastered) Icon(Icons.Default.Star, null, tint = MangoOrange, modifier = Modifier.size(30.dp).align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp).rotate(-15f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = if (isLocked) "؟؟؟" else item.word, fontWeight = FontWeight.Bold, color = if (isLocked) Color.Gray else DeepOcean)
    }
}

@Composable
fun EmptyMapState(onSeed: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onSeed, colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)) {
            Text("شروع ماجراجویی الفبا", fontWeight = FontWeight.Bold)
        }
    }
}
