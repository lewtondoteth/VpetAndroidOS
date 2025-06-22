package com.example.digipet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.digipet.ui.theme.DigipetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://web-production-998e.up.railway.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val petApi = retrofit.create(PetApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigipetTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Black
                ) { padding ->
                    var pet by remember { mutableStateOf<PetResponse?>(null) }
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            petApi.getPet().execute().let { resp ->
                                if (resp.isSuccessful) pet = resp.body()
                            }
                        }
                    }

                    PetScreen(
                        pet = pet,
                        onFeed = { /* TODO */ },
                        onPlay = { /* TODO */ },
                        onClean = { /* TODO */ },
                        onRest = { /* TODO */ },
                        onQuit = { finish() },
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun PetScreen(
    pet: PetResponse?,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onClean: () -> Unit,
    onRest: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(16.dp)
    ) {
        AnimatedSpriteInContainer(
            spriteResId    = R.drawable.pet_sprite,
            frameCount     = 2,
            cols           = 8,
            rows           = 1,
            frameDuration  = 1000,
            spriteSize     = 96.dp,
            containerWidth = 300.dp,
            containerHeight= 120.dp,
            step           = 16.dp
        )

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🍖 ${"%.1f".format(pet?.stats?.hunger ?: 0f)}", color = Color.White)
            Text("⚡ ${"%.1f".format(pet?.stats?.energy ?: 0f)}", color = Color.White)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) { ActionButton("🍖 Feed", onFeed) }
            Box(modifier = Modifier.weight(1f)) { ActionButton("🔍 Play", onPlay) }
            Box(modifier = Modifier.weight(1f)) { ActionButton("🧽 Clean", onClean) }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) { ActionButton("💤 Rest", onRest) }
            Box(modifier = Modifier.weight(1f)) { ActionButton("× Quit", onQuit) }
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
fun AnimatedSpriteInContainer(
    spriteResId: Int,
    frameCount: Int,
    cols: Int,
    rows: Int,
    frameDuration: Int,
    spriteSize: Dp,
    containerWidth: Dp,
    containerHeight: Dp,
    step: Dp
) {
    val ctx = LocalContext.current
    val sheetBmp = remember { BitmapFactory.decodeResource(ctx.resources, spriteResId) }
    val tileW = sheetBmp.width / cols
    val tileH = sheetBmp.height / rows

    // Pre-slice frames
    val rawFrames: List<Bitmap> = remember {
        List(frameCount) { i ->
            val col = i % cols
            val row = i / cols
            Bitmap.createBitmap(sheetBmp, col * tileW, row * tileH, tileW, tileH)
        }
    }

    // Create directional frames
    val rightFrames = remember(rawFrames) { rawFrames.map { it.asImageBitmap() } }
    val leftFrames  = remember(rawFrames) {
        rawFrames.map {
            val matrix = Matrix().apply { preScale(-1f, 1f) }
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, false).asImageBitmap()
        }
    }

    var idx by remember { mutableStateOf(0) }
    var offsetX by remember { mutableStateOf(containerWidth - spriteSize) }
    // Start direction as positive so sprite walks “forward” to the right initially
    var direction by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(frameDuration.toLong())
            idx = (idx + 1) % frameCount
            if (idx == 0) {
                offsetX += step * direction
                if (offsetX <= 0.dp || offsetX >= containerWidth - spriteSize) {
                    direction *= -1f
                    offsetX = offsetX.coerceIn(0.dp, containerWidth - spriteSize)
                }
            }
        }
    }

    // Swap facing: moving right uses leftFrames, moving left uses rightFrames
    val frames = if (direction > 0f) leftFrames else rightFrames
    val frameBmp = frames[idx]
    val painter = remember(frameBmp) { BitmapPainter(frameBmp, filterQuality = FilterQuality.None) }

    Box(
        modifier = Modifier
            .width(containerWidth)
            .height(containerHeight)
            .clipToBounds()
            .background(Color.DarkGray)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(spriteSize)
                .offset(x = offsetX, y = (containerHeight - spriteSize) / 2)
        )
    }
}
