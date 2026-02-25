package com.github.opscalehub.chistanland.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.opscalehub.chistanland.ui.theme.*

@Composable
fun HomeScreen(
    onSelectCategory: (String) -> Unit,
    onOpenParentPanel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SkyBlue.copy(alpha = 0.3f), Color.White)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "چیستان‌آباد",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = DeepOcean,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            CategoryCard(
                title = "جزیره الفبا",
                subtitle = "سفر به دنیای نشانه‌ها",
                icon = "🏝️",
                color = PastelGreen,
                onClick = { onSelectCategory("ALPHABET") }
            )

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "تنظیمات والدین (لمس طولانی)",
                color = DeepOcean.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onOpenParentPanel() }
                        )
                    }
                    .padding(8.dp)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onOpenParentPanel() }
                    )
                },
            color = Color.Transparent
        ) {}
    }
}

@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(40.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = RoundedCornerShape(24.dp),
                color = color.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 50.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DeepOcean
            )
            Text(
                text = subtitle,
                fontSize = 18.sp,
                color = DeepOcean.copy(alpha = 0.6f)
            )
        }
    }
}
