package com.example.glitchface

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.FaceRetouchingNatural
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { GlitchTheme { GlitchApp() } }
    }
}

private val Pink = Color(0xFFFF5FD2)
private val Violet = Color(0xFF8E7BFF)
private val Bg = Color(0xFF110F1A)
private val CardBg = Color(0xFF1E1A2D)

@Composable
private fun GlitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pink,
            onPrimary = Color(0xFF1B0716),
            secondary = Violet,
            background = Bg,
            surface = CardBg
        ),
        content = content
    )
}

@Composable
private fun GlitchApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val say: (String) -> Unit = { m ->
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            snack.showSnackbar(m)
        }
    }

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var processed by remember { mutableStateOf<Bitmap?>(null) }
    var faces by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var enabled by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var detecting by remember { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf(1) }
    var intensity by remember { mutableFloatStateOf(0.6f) }
    var vhs by remember { mutableStateOf(true) }
    var headerShown by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }

    LaunchedEffect(Unit) { headerShown = true }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            detecting = true
            original = null
            processed = null
            faces = emptyList()
            enabled = emptySet()
            try {
                val bmp = withContext(Dispatchers.Default) { decode(context, uri) }
                original = bmp
                val found = detectFaces(bmp)
                faces = found
                enabled = found.indices.toSet()
                if (found.isEmpty()) say("Лица не найдены")
            } catch (e: Exception) {
                say("Ошибка: ${e.message}")
            } finally {
                detecting = false
            }
        }
    }

    LaunchedEffect(original, faces, enabled, seed, intensity, vhs) {
        val o = original
        if (o == null || faces.isEmpty()) {
            processed = null
            return@LaunchedEffect
        }
        delay(30)
        val sel = faces.withIndex().filter { it.index in enabled }
        processed = withContext(Dispatchers.Default) { glitch(o, sel, seed, intensity, vhs) }
    }

    val pick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val hasImage = original != null
    val ready = hasImage && !detecting
    val current = processed ?: original

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF241A3F), Bg)))
                .padding(pad)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    headerShown,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { -it }
                ) { Header() }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val shown = current
                    if (shown == null) {
                        EmptyState(detecting, pick)
                    } else {
                        PhotoStage(
                            shown = shown,
                            faces = faces,
                            enabled = enabled,
                            detecting = detecting,
                            shakeX = { shake.value }
                        ) { i -> enabled = if (i in enabled) enabled - i else enabled + i }
                    }
                }

                AnimatedVisibility(
                    ready && faces.isNotEmpty(),
                    enter = fadeIn() + expandVertically()
                ) {
                    Controls(
                        total = faces.size,
                        active = enabled.size,
                        intensity = intensity,
                        onIntensity = { intensity = it },
                        vhs = vhs,
                        onVhs = { vhs = it }
                    )
                }

                AnimatedVisibility(
                    hasImage,
                    enter = fadeIn() + slideInVertically { it }
                ) {
                    val btn = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = CardBg,
                        contentColor = Color.White,
                        disabledContainerColor = CardBg.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = pick,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Другое фото", fontWeight = FontWeight.SemiBold)
                        }
                        FilledTonalIconButton(
                            onClick = {
                                seed++
                                scope.launch {
                                    for (v in listOf(16f, -16f, 9f, -5f, 0f)) {
                                        shake.animateTo(v, tween(45))
                                    }
                                }
                                scope.launch { spin.animateTo(spin.value + 360f, tween(450)) }
                            },
                            enabled = ready && faces.isNotEmpty(),
                            modifier = Modifier.size(52.dp),
                            colors = btn
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle, null,
                                Modifier.graphicsLayer { rotationZ = spin.value }
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                current?.let { b ->
                                    scope.launch {
                                        val u = saveBitmap(context, b)
                                        say(if (u != null) "Сохранено в Pictures/GlitchFace" else "Не удалось сохранить")
                                    }
                                }
                            },
                            enabled = ready,
                            modifier = Modifier.size(52.dp),
                            colors = btn
                        ) { Icon(Icons.Rounded.Save, null) }
                        FilledTonalIconButton(
                            onClick = {
                                current?.let { b ->
                                    scope.launch {
                                        val u = saveBitmap(context, b)
                                        if (u != null) {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/jpeg"
                                                putExtra(Intent.EXTRA_STREAM, u)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(send, "Поделиться"))
                                        }
                                    }
                                }
                            },
                            enabled = ready,
                            modifier = Modifier.size(52.dp),
                            colors = btn
                        ) { Icon(Icons.Rounded.Share, null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Pink, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.FaceRetouchingNatural, null,
                tint = Color.White, modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "GlitchFace",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(Pink, Violet)),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            )
            Text(
                "Глитч-эффект для лиц",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyState(loading: Boolean, onPick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "pulse")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "ring"
    )
    val beat by t.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "beat"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        scaleX = 1f + phase * 0.6f
                        scaleY = 1f + phase * 0.6f
                        alpha = 0.5f * (1f - phase)
                    }
                    .border(2.dp, Pink, CircleShape)
            )
            Box(
                Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        scaleX = beat
                        scaleY = beat
                    }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Pink, Violet)))
                    .clickable(enabled = !loading, onClick = onPick),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White, strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Icon(
                        Icons.Rounded.AddPhotoAlternate, null,
                        tint = Color.White, modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (loading) "Загружаю и ищу лица…" else "Выбери фото",
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Все найденные лица закроются глитчем",
            color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhotoStage(
    shown: Bitmap,
    faces: List<Rect>,
    enabled: Set<Int>,
    detecting: Boolean,
    shakeX: () -> Float,
    onToggle: (Int) -> Unit
) {
    val toggle by rememberUpdatedState(onToggle)
    val appear by animateFloatAsState(if (detecting) 0f else 1f, tween(600), label = "appear")
    val inf = rememberInfiniteTransition(label = "scan")
    val scan by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanY"
    )
    val image = remember(shown) { shown.asImageBitmap() }
    val shape = RoundedCornerShape(24.dp)

    Box(
        Modifier
            .aspectRatio(shown.width.toFloat() / shown.height)
            .graphicsLayer { translationX = shakeX() }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .pointerInput(faces, shown) {
                detectTapGestures { off ->
                    val s = shown.width / size.width.toFloat()
                    val x = (off.x * s).toInt()
                    val y = (off.y * s).toInt()
                    val hit = faces.indices.lastOrNull { faces[it].contains(x, y) }
                    if (hit != null) toggle(hit)
                }
            }
    ) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / shown.width
            if (detecting) {
                val y = size.height * scan
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Pink.copy(alpha = 0.45f), Color.Transparent),
                        startY = y - 70f, endY = y + 70f
                    ),
                    topLeft = Offset(0f, y - 70f),
                    size = Size(size.width, 140f)
                )
                drawLine(Pink, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
            }
            faces.forEachIndexed { i, r ->
                val on = i in enabled
                drawRoundRect(
                    color = (if (on) Pink else Color.White)
                        .copy(alpha = (if (on) 0.95f else 0.6f) * appear),
                    topLeft = Offset(r.left * k, r.top * k),
                    size = Size(r.width() * k, r.height() * k),
                    cornerRadius = CornerRadius(18f),
                    style = Stroke(
                        width = 3.5f,
                        pathEffect = if (on) null else PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                    )
                )
            }
        }
    }
}

@Composable
private fun Controls(
    total: Int,
    active: Int,
    intensity: Float,
    onIntensity: (Float) -> Unit,
    vhs: Boolean,
    onVhs: (Boolean) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TouchApp, null, tint = Pink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = active,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn()) togetherWith
                        (slideOutVertically { it } + fadeOut())
                },
                label = "count"
            ) { n ->
                Text(
                    "Закрыто лиц: $n из $total",
                    color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            "Нажми на рамку на фото, чтобы включить или выключить лицо",
            color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Tune, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Slider(
                value = intensity,
                onValueChange = onIntensity,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Pink,
                    activeTrackColor = Pink,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
        FilterChip(
            selected = vhs,
            onClick = { onVhs(!vhs) },
            label = { Text("VHS-фильтр") },
            leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(18.dp)) }
        )
    }
}

// ---------------- логика ----------------

private fun decode(context: Context, uri: Uri): Bitmap {
    val bmp = ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(context.contentResolver, uri)
    ) { d, info, _ ->
        d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val m = max(info.size.width, info.size.height)
        if (m > 2048) {
            val k = 2048f / m
            d.setTargetSize((info.size.width * k).toInt(), (info.size.height * k).toInt())
        }
    }
    return if (bmp.config != Bitmap.Config.ARGB_8888) bmp.copy(Bitmap.Config.ARGB_8888, false) else bmp
}

private suspend fun detectFaces(bmp: Bitmap): List<Rect> {
    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.04f)
            .build()
    )
    try {
        val found = detector.process(InputImage.fromBitmap(bmp, 0)).await()
        val bounds = Rect(0, 0, bmp.width, bmp.height)
        return found.mapNotNull { f ->
            val b = f.boundingBox
            val px = (b.width() * 0.2f).toInt()
            val py = (b.height() * 0.25f).toInt()
            val r = Rect(b.left - px, b.top - py, b.right + px, b.bottom + py)
            if (r.intersect(bounds) && r.width() >= 8 && r.height() >= 8) r else null
        }.sortedBy { it.left }
    } finally {
        detector.close()
    }
}

private fun glitch(
    src: Bitmap,
    faces: List<IndexedValue<Rect>>,
    seed: Int,
    intensity: Float,
    vhs: Boolean
): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(out)
    val gap = AndroidPaint().apply { color = android.graphics.Color.argb(210, 255, 255, 255) }

    for ((i, r) in faces) {
        val rnd = Random(seed * 1000 + i)
        val piece = Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height())
        val w = piece.width
        val hgt = piece.height

        val minH = max(2, hgt / 90)
        val maxH = max(minH + 1, (hgt / (8f + 28f * intensity)).toInt())
        val winMin = max(2, w / 12)
        val winMax = max(winMin + 1, (w * (0.75f - 0.55f * intensity)).toInt())
        val gapChance = 0.04f + 0.10f * intensity

        var y = 0
        while (y < hgt) {
            val h = minOf(rnd.nextInt(minH, maxH), hgt - y)
            val winW = minOf(rnd.nextInt(winMin, winMax), w)
            val winX = rnd.nextInt(0, max(1, w - winW + 1))
            val srcRect = Rect(winX, y, winX + winW, y + h)
            val dst = Rect(r.left, r.top + y, r.right, r.top + y + h)
            canvas.drawBitmap(piece, srcRect, dst, null)

            if (rnd.nextFloat() < gapChance) {
                val x0 = r.left + rnd.nextInt(0, max(1, w / 2))
                val gw = rnd.nextInt(max(1, w / 8), max(2, w / 2))
                canvas.drawRect(
                    x0.toFloat(), dst.top.toFloat(),
                    (x0 + gw).toFloat(), dst.bottom.toFloat(), gap
                )
            }
            y += h
        }
        piece.recycle()
    }

    if (!vhs) return out

    val tinted = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
    val c2 = AndroidCanvas(tinted)
    val tint = AndroidPaint().apply {
        colorFilter = PorterDuffColorFilter(
            android.graphics.Color.argb(45, 200, 140, 220), PorterDuff.Mode.SRC_ATOP
        )
    }
    c2.drawBitmap(out, 0f, 0f, tint)
    val line = AndroidPaint().apply {
        color = android.graphics.Color.argb(32, 0, 0, 0)
        strokeWidth = 1f
    }
    var yy = 0
    while (yy < tinted.height) {
        c2.drawLine(0f, yy.toFloat(), tinted.width.toFloat(), yy.toFloat(), line)
        yy += 3
    }
    out.recycle()
    return tinted
}

private suspend fun saveBitmap(context: Context, bmp: Bitmap): Uri? = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "glitch_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlitchFace")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext null
    context.contentResolver.openOutputStream(uri)?.use {
        bmp.compress(Bitmap.CompressFormat.JPEG, 95, it)
    }
    uri
}
