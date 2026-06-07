package com.iebayirli.scdetect

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iebayirli.scdetect.ml.ClassificationResult
import com.iebayirli.scdetect.ml.ScoliosisClassifier
import com.iebayirli.scdetect.ui.theme.SCDetectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SCDetectTheme {
                SCDetectApp()
            }
        }
    }
}

@Composable
fun SCDetectApp() {
    val context = LocalContext.current
    val classifier = remember { ScoliosisClassifier(context) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var result by remember { mutableStateOf<ClassificationResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            result = null
            isAnalyzing = true
            val source = ImageDecoder.createSource(context.contentResolver, it)
            val bitmap = ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, false)
            result = classifier.classify(bitmap)
            isAnalyzing = false
        }
    }

    Scaffold(
        topBar = { TopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            InfoCard()
            ImagePickerCard(
                uri = selectedUri,
                onPickImage = { launcher.launch("image/*") }
            )
            if (isAnalyzing) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(
                visible = result != null,
                enter = fadeIn() + slideInVertically()
            ) {
                result?.let { ResultCard(it) }
            }
            DisclaimerText()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    TopAppBar(
        title = {
            Text(
                text = "SC Detect",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White
        )
    )
}

@Composable
fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Omurga X-Ray Analizi",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "X-ray görüntüsünü yükleyin. Model 3 sınıf tahmin eder: Normal, Skolyoz veya Spondilolistez.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ImagePickerCard(uri: Uri?, onPickImage: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable { onPickImage() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Seçilen X-ray",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                // Değiştir butonu
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Değiştir", color = Color.White, fontSize = 12.sp)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "X-Ray Görüntüsü Seç",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Galeriden seçmek için dokun",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ResultCard(result: ClassificationResult) {
    val isNormal = result.label == "normal"
    val isScoliosis = result.label == "scoliosis"

    val mainColor = when (result.label) {
        "normal" -> Color(0xFF2E7D32)
        "scoliosis" -> Color(0xFFB71C1C)
        else -> Color(0xFFE65100)
    }

    val labelText = when (result.label) {
        "normal" -> "Normal"
        "scoliosis" -> "Skolyoz"
        "spondylolisthesis" -> "Spondilolistez"
        else -> result.label
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isNormal) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = mainColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Analiz Sonucu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ana sonuç
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(mainColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = labelText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = mainColor
                    )
                    Text(
                        text = "%.1f%% güven".format(result.confidence * 100),
                        fontSize = 14.sp,
                        color = mainColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tüm sınıf skorları
            Text(
                text = "Tüm Skorlar",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val sortedScores = result.allScores.entries.sortedByDescending { it.value }
            sortedScores.forEach { (label, score) ->
                val displayLabel = when (label) {
                    "normal" -> "Normal"
                    "scoliosis" -> "Skolyoz"
                    "spondylolisthesis" -> "Spondilolistez"
                    else -> label
                }
                val barColor = when (label) {
                    "normal" -> Color(0xFF2E7D32)
                    "scoliosis" -> Color(0xFFB71C1C)
                    else -> Color(0xFFE65100)
                }
                ScoreBar(label = displayLabel, score = score, color = barColor)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ScoreBar(label: String, score: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = "%.1f%%".format(score * 100),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
fun DisclaimerText() {
    Text(
        text = "Bu uygulama yalnızca araştırma amaçlıdır. Klinik tanı için kullanılamaz. Lütfen bir sağlık uzmanına başvurun.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}
