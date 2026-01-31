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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chistanland.data.LearningItem
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.DeepOcean
import com.example.chistanland.ui.theme.MangoOrange
import com.example.chistanland.ui.theme.PastelGreen
import com.example.chistanland.ui.theme.SkyBlue

@Composable
fun IslandMapScreen(
    viewModel: LearningViewModel,
    onStartItem: (LearningItem) -> Unit,
    onOpenParentPanel: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SkyBlue.copy(alpha = 0.5f), Color.White)
                )
            )
    ) {
        // Clouds/Background elements could be added here
        
        Column(modifier = Modifier.fillMaxSize()) {
            MapHeader(onOpenParentPanel)
            
            if (items.isEmpty()) {
                EmptyMapState { viewModel.seedData() }
            } else {
                SagaMap(items, onStartItem)
            }
        }
    }
}

@Composable
fun MapHeader(onOpenParentPanel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "جزیره‌های دانایی",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = DeepOcean
            )
            Text(
                text = "ماجراجویی الفبا",
                fontSize = 16.sp,
                color = DeepOcean.copy(alpha = 0.6f)
            )
        }
        
        // Parent Gate: Long press required
        Surface(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onOpenParentPanel() })
                },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.5f),
            border = BorderStroke(2.dp, SkyBlue.copy(alpha = 0.3f))
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "تنظیمات والدین",
                modifier = Modifier.padding(12.dp),
                tint = DeepOcean.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun SagaMap(items: List<LearningItem>, onStartItem: (LearningItem) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 20.dp)
    ) {
        itemsIndexed(items) { index, item ->
            val isLocked = index > 0 && !items[index - 1].isMastered
            val isEven = index % 2 == 0
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = if (isEven) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                // Connection Path (Conceptual)
                if (index < items.size - 1) {
                    PathConnection(isEven)
                }
                
                IslandNode(
                    item = item,
                    isLocked = isLocked,
                    modifier = Modifier.padding(horizontal = 40.dp),
                    onClick = { onStartItem(item) }
                )
            }
        }
    }
}

@Composable
fun PathConnection(isEven: Boolean) {
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).offset(y = 80.dp)) {
        val path = Path().apply {
            if (isEven) {
                moveTo(size.width * 0.25f, 0f)
                cubicTo(
                    size.width * 0.25f, size.height * 0.5f,
                    size.width * 0.75f, size.height * 0.5f,
                    size.width * 0.75f, size.height
                )
            } else {
                moveTo(size.width * 0.75f, 0f)
                cubicTo(
                    size.width * 0.75f, size.height * 0.5f,
                    size.width * 0.25f, size.height * 0.5f,
                    size.width * 0.25f, size.height
                )
            }
        }
        drawPath(
            path = path,
            color = DeepOcean.copy(alpha = 0.1f),
            style = Stroke(
                width = 8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
            )
        )
    }
}

@Composable
fun IslandNode(
    item: LearningItem,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "island")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer(translationY = if (!isLocked) floatAnim else 0f)
            .clickable(enabled = !isLocked) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Island Base
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(if (isLocked) 0.dp else 12.dp, CircleShape),
                shape = CircleShape,
                color = when {
                    isLocked -> Color(0xFFE0E0E0)
                    item.isMastered -> Color(0xFFFFD600)
                    else -> PastelGreen
                },
                border = BorderStroke(
                    4.dp, 
                    if (isLocked) Color.LightGray else Color.White.copy(alpha = 0.5f)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLocked) {
                        Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    } else {
                        Text(
                            text = item.character,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = if (item.isMastered) DeepOcean else Color.White
                        )
                    }
                }
            }

            // Level Badges/Indicators
            if (!isLocked && item.level > 1 && !item.isMastered) {
                SmallPlantBadge(item.level, Modifier.align(Alignment.TopEnd))
            }
            
            if (item.isMastered) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MangoOrange,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .rotate(-15f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = if (isLocked) Color.Transparent else Color.White.copy(alpha = 0.8f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isLocked) "؟؟؟" else item.word,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) Color.Gray else DeepOcean
            )
        }
    }
}

@Composable
fun SmallPlantBadge(level: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(2.dp, PastelGreen),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = when(level) {
                    2 -> "🌱"
                    3 -> "🌿"
                    4 -> "🌸"
                    else -> "🥚"
                },
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun EmptyMapState(onSeed: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("نقشه هنوز خالی است!", color = DeepOcean)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSeed,
                colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)
            ) {
                Text("شروع ماجراجویی", fontWeight = FontWeight.Bold)
            }
        }
    }
}
