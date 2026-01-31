package com.example.chistanland.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.chistanland.data.LearningItem
import com.example.chistanland.ui.LearningViewModel

@Composable
fun IslandMapScreen(
    viewModel: LearningViewModel,
    onStartItem: (LearningItem) -> Unit,
    onOpenParentPanel: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "جزیره‌های دانایی",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewModel.seedData() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("برای شروع ماجراجویی ضربه بزنید!", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(items) { index, item ->
                        IslandNode(
                            item = item,
                            isLocked = index > 0 && !items[index - 1].isMastered,
                            onClick = { onStartItem(item) }
                        )
                    }
                }
            }
        }

        // Parent Gate: Small gear icon that requires long press to open
        IconButton(
            onClick = { /* Do nothing on simple click to avoid accidental child access */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onOpenParentPanel() }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "تنظیمات والدین",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun IslandNode(
    item: LearningItem,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val color = when {
        item.isMastered -> Color(0xFFFFD700) // Golden Palace
        isLocked -> Color.Gray.copy(alpha = 0.5f) // Misty Island
        else -> MaterialTheme.colorScheme.tertiary // Active Island
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = !isLocked) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.char,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
        }
        Text(
            text = if (isLocked) "قفل" else item.word,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    items: List<T>,
    itemContent: @Composable (index: Int, item: T) -> Unit
) {
    items(items.size) { index ->
        itemContent(index, items[index])
    }
}
