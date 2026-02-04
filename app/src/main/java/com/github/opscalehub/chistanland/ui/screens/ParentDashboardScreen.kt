package com.github.opscalehub.chistanland.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.opscalehub.chistanland.data.LearningItem
import com.github.opscalehub.chistanland.ui.LearningViewModel
import com.github.opscalehub.chistanland.ui.theme.*
import com.github.opscalehub.chistanland.util.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: LearningViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.allItems.collectAsState()
    val narrative = viewModel.getParentNarrative()
    var showRawData by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "پنل نظارتی والدین",
                        fontWeight = FontWeight.Black,
                        color = DeepOcean
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "برگشت",
                            tint = DeepOcean
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, SkyBlue.copy(alpha = 0.1f))
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                // TTS Settings Card
                item {
                    TtsSettingsCard(onClick = { ttsManager.openTtsSettings() })
                }

                // Summary Stats
                item {
                    ProgressSummaryRow(items)
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "داستان سفر دانایی",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepOcean,
                            modifier = Modifier.weight(1f)
                        )
                        // اضافه کردن آواتار کوچک به پنل والدین
                        LearningAvatar(state = "HAPPY", modifier = Modifier.size(48.dp))
                    }
                }

                // Generative Narrative Card
                item {
                    NarrativeCard(narrative, showRawData, onToggleData = { showRawData = !showRawData })
                }

                // Audit Details (Show me why)
                item {
                    AnimatedVisibility(
                        visible = showRawData,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "جزییات فنی یادگیری (Audit Log)",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepOcean.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            items.forEach { item ->
                                RawDataItem(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TtsSettingsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepOcean),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "فعال‌سازی صدای آفلاین",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "برای بهترین تجربه، صدای فارسی گوگل را نصب کنید.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MangoOrange)
            ) {
                Text("تنظیمات", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProgressSummaryRow(items: List<LearningItem>) {
    val mastered = items.count { it.isMastered }
    val total = items.size
    val progress = if (total > 0) mastered.toFloat() / total else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SkyBlue.copy(alpha = 0.1f))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(70.dp),
                color = PastelGreen,
                strokeWidth = 8.dp,
                trackColor = Color.White
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                fontWeight = FontWeight.Black,
                color = DeepOcean
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = "پیشرفت کل",
                fontWeight = FontWeight.Bold,
                color = DeepOcean
            )
            Text(
                text = "$mastered از $total حرف در حافظه بلندمدت",
                fontSize = 14.sp,
                color = DeepOcean.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun NarrativeCard(
    narrative: String,
    isDataVisible: Boolean,
    onToggleData: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MangoOrange.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MangoOrange,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تحلیل هوشمند پیشرفت",
                    style = MaterialTheme.typography.labelLarge,
                    color = MangoOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = narrative,
                fontSize = 18.sp,
                lineHeight = 32.sp,
                color = DeepOcean,
                textAlign = TextAlign.Justify
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onToggleData,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepOcean.copy(alpha = 0.05f),
                    contentColor = DeepOcean
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDataVisible) Icons.Default.Star else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isDataVisible) "بستن جزییات فنی" else "علت این تحلیل چیست؟ (Show me why)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RawDataItem(item: LearningItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SkyBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.character, fontWeight = FontWeight.Black, fontSize = 20.sp, color = DeepOcean)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.word, fontWeight = FontWeight.Bold, color = DeepOcean)
                Text(
                    text = "آخرین تمرین: ${if(item.lastReviewTime == 0L) "هرگز" else "اخیراً"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "سطح ${item.level}",
                    color = if(item.isMastered) MangoOrange else DeepOcean,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    progress = { item.level / 5f },
                    color = if(item.isMastered) MangoOrange else PastelGreen,
                    trackColor = Color.LightGray.copy(alpha = 0.2f),
                    modifier = Modifier.width(60.dp).clip(CircleShape).height(6.dp)
                )
            }
        }
    }
}
