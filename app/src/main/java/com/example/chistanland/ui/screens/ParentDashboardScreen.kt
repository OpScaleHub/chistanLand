package com.example.chistanland.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chistanland.ui.LearningViewModel
import com.example.chistanland.ui.theme.DeepOcean
import com.example.chistanland.ui.theme.PastelGreen
import com.example.chistanland.ui.theme.SoftYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: LearningViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()
    val narrative = viewModel.getParentNarrative()
    var showRawData by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("گزارش پیشرفت", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                    }
                }
            )
        },
        containerColor = SoftYellow.copy(alpha = 0.1f)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    "داستان سفر فرزند شما",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepOcean,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Generative Narrative Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DeepOcean)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تحلیل هوشمند",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepOcean.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = narrative,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = DeepOcean
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(onClick = { showRawData = !showRawData }) {
                            Text(if (showRawData) "پنهان‌سازی جزییات فنی" else "مشاهده داده‌های خام")
                        }
                    }
                }
            }

            if (showRawData) {
                items(items) { item ->
                    RawDataItem(item)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun RawDataItem(item: com.example.chistanland.data.LearningItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "حرف: ${item.character}", fontWeight = FontWeight.Bold, color = DeepOcean)
                Text(text = "کلمه: ${item.word}", style = MaterialTheme.typography.bodySmall, color = DeepOcean.copy(alpha = 0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "سطح: ${item.level}/5", color = DeepOcean, fontSize = 12.sp)
                LinearProgressIndicator(
                    progress = { item.level / 5f },
                    color = PastelGreen,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    modifier = Modifier.width(80.dp).padding(top = 4.dp)
                )
            }
        }
    }
}
