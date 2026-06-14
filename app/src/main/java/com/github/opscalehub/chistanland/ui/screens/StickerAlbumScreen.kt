package com.github.opscalehub.chistanland.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.ui.LearningViewModel
import com.github.opscalehub.chistanland.ui.theme.*

/**
 * Sticker album — a collection reward. The child earns a sticker for every island they've
 * started (lastReviewTime > 0), and it gets a gold star once that letter is mastered.
 * Tapping a collected sticker plays its word. Reads existing progress, no new persistence.
 */
@Composable
fun StickerAlbumScreen(viewModel: LearningViewModel, onBack: () -> Unit) {
    val items by viewModel.filteredItems.collectAsState()

    val collected = items.count { it.lastReviewTime > 0L }
    val masteredCount = items.count { it.isMastered }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SoftYellow.copy(alpha = 0.35f), Color.White)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = DeepOcean) }
                Spacer(modifier = Modifier.width(8.dp))
                Text("📒 آلبوم استیکر", fontSize = 26.sp, fontWeight = FontWeight.Black, color = DeepOcean)
            }

            // Collection counter
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(2.dp, MangoOrange.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${toFa(collected)} از ${toFa(items.size)} استیکر جمع شد", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepOcean)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = MangoOrange, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(toFa(masteredCount), fontSize = 17.sp, fontWeight = FontWeight.Black, color = MangoOrange)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    StickerSlot(item = item, onTap = { viewModel.speakWord(item.word) })
                }
            }
        }
    }
}

@Composable
private fun StickerSlot(item: LearningItem, onTap: () -> Unit) {
    val collected = item.lastReviewTime > 0L
    val pulse by rememberInfiniteTransition(label = "sticker").animateFloat(
        1f, if (item.isMastered) 1.06f else 1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse"
    )

    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .aspectRatio(0.85f)
                .then(if (collected) Modifier.clickable { onTap() } else Modifier)
                .shadow(if (collected) 8.dp else 0.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = if (collected) Color.White else Color(0xFFEDEDED),
            border = BorderStroke(
                width = if (item.isMastered) 3.dp else 2.dp,
                color = when {
                    item.isMastered -> MangoOrange
                    collected -> PastelGreen.copy(alpha = 0.5f)
                    else -> Color.LightGray.copy(alpha = 0.5f)
                }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (collected) {
                    Text(getEmojiForWord(item.word), fontSize = (34 * pulse).sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(item.word, fontSize = 13.sp, fontWeight = FontWeight.Black, color = SkyBlue, textAlign = TextAlign.Center, maxLines = 1)
                } else {
                    Icon(Icons.Default.Lock, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("؟", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Gray.copy(alpha = 0.5f))
                }
            }
        }
        // Gold star badge for mastered letters
        if (item.isMastered) {
            Box(modifier = Modifier.matchParentSize()) {
                Icon(
                    Icons.Default.Star, null, tint = MangoOrange,
                    modifier = Modifier.size(26.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).rotate(15f)
                )
            }
        }
    }
}

/** Render an Int with Persian-Indic digits (children see familiar numerals). */
private fun toFa(n: Int): String {
    val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return n.toString().map { if (it in '0'..'9') fa[it - '0'] else it }.joinToString("")
}
