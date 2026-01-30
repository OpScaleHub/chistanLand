package com.example.chistanland.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.MangoOrange
import com.example.chistanland.ui.theme.PastelGreen
import com.example.chistanland.ui.theme.SoftYellow

@Composable
fun LearningSessionScreen(
    viewModel: LearningViewModel,
    onBack: () -> Unit
) {
    val currentItem by viewModel.currentItem.collectAsState()
    val typedText by viewModel.typedText.collectAsState()

    if (currentItem == null) {
        // Success state or transition back
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val item = currentItem!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Progress (Plant Metaphor)
        PlantProgress(level = item.level)

        Spacer(modifier = Modifier.height(40.dp))

        // Image Placeholder (In real app, use Painter or Coil)
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SoftYellow),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "تصویر ${item.word}", fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Word Display (MonkeyType style)
        WordDisplay(targetWord = item.word, typedText = typedText)

        Spacer(modifier = Modifier.weight(1f))

        // Custom Keyboard
        KidKeyboard(
            onKeyClick = { viewModel.onCharTyped(it) },
            allowedChars = listOf("آ", "ب", "پ", "ت", "ث", "ا") // Dynamically filter in real app
        )
    }
}

@Composable
fun PlantProgress(level: Int) {
    val progressSize by animateDpAsState(
        targetValue = (level * 30).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(150.dp)
                .width(100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Pot
            Box(
                modifier = Modifier
                    .size(60.dp, 40.dp)
                    .background(Color(0xFF8B4513), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
            )
            // Stem
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(progressSize)
                    .background(PastelGreen)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-35).dp)
            )
        }
        Text("سطح یادگیری: $level", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WordDisplay(targetWord: String, typedText: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        targetWord.forEachIndexed { index, char ->
            val isTyped = index < typedText.length
            val color = if (isTyped) PastelGreen else Color.LightGray
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = char.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun KidKeyboard(
    onKeyClick: (String) -> Unit,
    allowedChars: List<String>
) {
    val alphabet = listOf(
        listOf("آ", "ب", "پ", "ت", "ث"),
        listOf("ج", "چ", "ح", "خ", "د"),
        listOf("ذ", "ر", "ز", "ژ", "س")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        alphabet.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { char ->
                    KeyButton(char = char, onClick = { onKeyClick(char) })
                }
            }
        }
    }
}

@Composable
fun KeyButton(char: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(60.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MangoOrange,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = char,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
