package com.example.glitchface

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.FaceRetouchingNatural
import androidx.compose.material.icons.rounded.Gif
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.PhotoLibrary
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

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

// ---------------- состояние редактора ----------------

private data class Snap(val enabled: Set<Int>, val manual: List<Rect>)

@Stable
private class Editor {
    var original by mutableStateOf<Bitmap?>(null)
    var processed by mutableStateOf<Bitmap?>(null)
    var faces by mutableStateOf<List<Rect>>(emptyList())
    var manual by mutableStateOf<List<Rect>>(emptyList())
    var enabled by mutableStateOf<Set<Int>>(emptySet())
    var detecting by mutableStateOf(false)
    var busy by mutableStateOf<String?>(null)
    var seed by mutableIntStateOf(1)
    var mode by mutableStateOf(Mode.GLITCH)
    var palette by mutableStateOf(Palette.PINK)
    var emoji by mutableStateOf("😎")
    var intensity by mutableFloatStateOf(0.6f)
    var watermark by mutableStateOf("")
    var drawMode by mutableStateOf(false)
    var showOriginal by mutableStateOf(false)
    var undo by mutableStateOf<List<Snap>>(emptyList())
    var redo by mutableStateOf<List<Snap>>(emptyList())

    val all: List<Rect> get() = faces + manual

    fun params() = Params(mode, intensity, palette, emoji, watermark, seed)

    fun selected(): List<IndexedValue<Rect>> = all.withIndex().filter { it.index in enabled }

    fun clear() {
        original = null
        processed = null
        faces = emptyList()
        manual = emptyList()
        enabled = emptySet()
        undo = emptyList()
        redo = emptyList()
        drawMode = false
        showOriginal = false
    }

    private fun commit(e: Set<Int>, m: List<Rect>) {
        undo = undo + Snap(enabled, manual)
        redo = emptyList()
        enabled = e
        manual = m
    }

    fun toggle(i: Int) = commit(if (i in enabled) enabled - i else enabled + i, manual)

    fun addManual(r: Rect) {
        val idx = faces.size + manual.size
        commit(enabled + idx, manual + r)
    }

    fun doUndo() {
        val s = undo.lastOrNull() ?: return
        redo = redo + Snap(enabled, manual)
        undo = undo.dropLast(1)
        enabled = s.enabled
        manual = s.manual
    }

    fun doRedo() {
        val s = redo.lastOrNull() ?: return
        undo = undo + Snap(enabled, manual)
        redo = redo.dropLast(1)
        enabled = s.enabled
        manual = s.manual
    }
}

// ---------------- экран ----------------

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
    val ed = remember { Editor() }
    val shake = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    var headerShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { headerShown = true }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            ed.detecting = true
            ed.clear()
            try {
                val bmp = withContext(Dispatchers.Default) { decode(context, uri) }
                ed.original = bmp
                val found = detectFaces(bmp)
                ed.faces = found
                ed.enabled = found.indices.toSet()
                if (found.isEmpty()) say("Лица не найдены. Выдели область вручную")
            } catch (ex: Exception) {
                say("Ошибка: ${ex.message}")
            } finally {
                ed.detecting = false
            }
        }
    }

    val batchPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            ed.busy = "Обработка 0/${uris.size}"
            val ok = processBatch(context, uris, ed.params()) { n, total ->
                ed.busy = "Обработка $n/$total"
            }
            ed.busy = null
            say("Готово: сохранено $ok из ${uris.size} (Pictures/GlitchFace)")
        }
    }

    LaunchedEffect(
        ed.original, ed.faces, ed.manual, ed.enabled,
        ed.mode, ed.palette, ed.emoji, ed.intensity, ed.watermark, ed.seed
    ) {
        val o = ed.original
        val sel = ed.selected()
        if (o == null || (sel.isEmpty() && ed.watermark.isBlank())) {
            ed.processed = null
            return@LaunchedEffect
        }
        delay(60)
        val p = ed.params()
        ed.processed = withContext(Dispatchers.Default) { render(o, sel, p) }
    }

    val pick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val batch = {
        batchPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val hasImage = ed.original != null
    val ready = hasImage && !ed.detecting
    val current = ed.processed ?: ed.original
    val shown = if (ed.showOriginal) ed.original else current

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
                    .padding(horizontal = 20.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    headerShown,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { -it }
                ) {
                    Header(
                        canUndo = ed.undo.isNotEmpty(),
                        canRedo = ed.redo.isNotEmpty(),
                        onUndo = { ed.doUndo() },
                        onRedo = { ed.doRedo() }
                    )
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (shown == null) {
                        EmptyState(ed.detecting, pick, batch)
                    } else {
                        PhotoStage(
                            shown = shown,
                            faces = ed.all,
                            enabled = ed.enabled,
                            detecting = ed.detecting,
                            drawMode = ed.drawMode,
                            shakeX = { shake.value },
                            onToggle = { ed.toggle(it) },
                            onDraw = { ed.addManual(it) },
                            onHold = { ed.showOriginal = it }
                        )
                    }
                }

                AnimatedVisibility(ready, enter = fadeIn() + expandVertically()) {
                    Controls(ed)
                }

                AnimatedVisibility(hasImage, enter = fadeIn() + slideInVertically { it }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = pick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Другое фото", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = batch,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Пакет", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ActionButton(
                                Icons.Rounded.Shuffle, "Другой", ready && ed.selected().isNotEmpty(),
                                rotate = { spin.value }
                            ) {
                                ed.seed++
                                scope.launch {
                                    for (v in listOf(16f, -16f, 9f, -5f, 0f)) {
                                        shake.animateTo(v, tween(45))
                                    }
                                }
                                scope.launch { spin.animateTo(spin.value + 360f, tween(450)) }
                            }
                            ActionButton(Icons.Rounded.Save, "Сохранить", ready) {
                                current?.let { b ->
                                    scope.launch {
                                        val u = saveJpeg(context, b)
                                        say(if (u != null) "Сохранено в Pictures/GlitchFace" else "Не удалось сохранить")
                                    }
                                }
                            }
                            ActionButton(Icons.Rounded.Gif, "GIF", ready) {
                                val o = ed.original
                                val sel = ed.selected()
                                if (o == null) return@ActionButton
                                if (ed.mode != Mode.GLITCH) {
                                    say("GIF-анимация работает в режиме «Глитч»")
                                } else if (sel.isEmpty()) {
                                    say("Выбери хотя бы одно лицо")
                                } else {
                                    scope.launch {
                                        ed.busy = "Собираю GIF…"
                                        val u = try {
                                            exportGif(context, o, sel, ed.params())
                                        } catch (ex: Exception) {
                                            null
                                        }
                                        ed.busy = null
                                        say(if (u != null) "GIF сохранён в Pictures/GlitchFace" else "Не удалось собрать GIF")
                                    }
                                }
                            }
                            ActionButton(Icons.Rounded.Share, "Отправить", ready) {
                                current?.let { b ->
                                    scope.launch {
                                        val u = saveJpeg(context, b)
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
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(ed.busy != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .pointerInput(Unit) { detectTapGestures { } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Pink)
                        Spacer(Modifier.height(14.dp))
                        Text(ed.busy ?: "", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    rotate: () -> Float = { 0f },
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = CardBg,
                contentColor = Color.White,
                disabledContainerColor = CardBg.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.3f)
            )
        ) {
            Icon(icon, null, Modifier.graphicsLayer { rotationZ = rotate() })
        }
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun Header(canUndo: Boolean, canRedo: Boolean, onUndo: () -> Unit, onRedo: () -> Unit) {
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
        Column(Modifier.weight(1f)) {
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
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(
                Icons.AutoMirrored.Rounded.Undo, null,
                tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.25f)
            )
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(
                Icons.AutoMirrored.Rounded.Redo, null,
                tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun EmptyState(loading: Boolean, onPick: () -> Unit, onBatch: () -> Unit) {
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
            "Все найденные лица закроются эффектом",
            color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        OutlinedButton(
            onClick = onBatch,
            enabled = !loading,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Пакетная обработка")
        }
    }
}

@Composable
private fun PhotoStage(
    shown: Bitmap,
    faces: List<Rect>,
    enabled: Set<Int>,
    detecting: Boolean,
    drawMode: Boolean,
    shakeX: () -> Float,
    onToggle: (Int) -> Unit,
    onDraw: (Rect) -> Unit,
    onHold: (Boolean) -> Unit
) {
    val toggle by rememberUpdatedState(onToggle)
    val draw by rememberUpdatedState(onDraw)
    val hold by rememberUpdatedState(onHold)
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }

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
            .pointerInput(faces, shown, drawMode) {
                if (drawMode) {
                    detectDragGestures(
                        onDragStart = {
                            dragStart = it
                            dragEnd = it
                        },
                        onDragEnd = {
                            val a = dragStart
                            val b = dragEnd
                            if (a != null && b != null) {
                                val s = shown.width / size.width.toFloat()
                                val r = Rect(
                                    (minOf(a.x, b.x) * s).toInt().coerceIn(0, shown.width),
                                    (minOf(a.y, b.y) * s).toInt().coerceIn(0, shown.height),
                                    (maxOf(a.x, b.x) * s).toInt().coerceIn(0, shown.width),
                                    (maxOf(a.y, b.y) * s).toInt().coerceIn(0, shown.height)
                                )
                                if (r.width() >= 24 && r.height() >= 24) draw(r)
                            }
                            dragStart = null
                            dragEnd = null
                        },
                        onDragCancel = {
                            dragStart = null
                            dragEnd = null
                        },
                        onDrag = { change, _ -> dragEnd = change.position }
                    )
                } else {
                    detectTapGestures(
                        onPress = {
                            hold(true)
                            tryAwaitRelease()
                            hold(false)
                        },
                        onTap = { off ->
                            val s = shown.width / size.width.toFloat()
                            val x = (off.x * s).toInt()
                            val y = (off.y * s).toInt()
                            val hit = faces.indices.lastOrNull { faces[it].contains(x, y) }
                            if (hit != null) toggle(hit)
                        }
                    )
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
            val a = dragStart
            val b = dragEnd
            if (a != null && b != null) {
                val tl = Offset(minOf(a.x, b.x), minOf(a.y, b.y))
                val sz = Size(abs(b.x - a.x), abs(b.y - a.y))
                drawRect(Pink.copy(alpha = 0.2f), topLeft = tl, size = sz)
                drawRect(Pink, topLeft = tl, size = sz, style = Stroke(width = 3f))
            }
        }
    }
}

@Composable
private fun Controls(ed: Editor) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .heightIn(max = 230.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TouchApp, null, tint = Pink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = ed.enabled.size,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn()) togetherWith
                        (slideOutVertically { it } + fadeOut())
                },
                label = "count"
            ) { n ->
                Text(
                    "Закрыто лиц: $n из ${ed.all.size}",
                    color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            "Нажми на рамку: вкл/выкл. Удерживай фото: оригинал.",
            color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Mode.values().forEach { m ->
                FilterChip(
                    selected = ed.mode == m,
                    onClick = { ed.mode = m },
                    label = { Text(m.label) },
                    leadingIcon = {
                        Icon(
                            when (m) {
                                Mode.GLITCH -> Icons.Rounded.AutoFixHigh
                                Mode.PIXEL -> Icons.Rounded.GridOn
                                Mode.BLUR -> Icons.Rounded.BlurOn
                                Mode.EMOJI -> Icons.Rounded.EmojiEmotions
                            },
                            null, Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        AnimatedVisibility(ed.mode == Mode.EMOJI) {
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMOJIS.forEach { em ->
                    FilterChip(
                        selected = ed.emoji == em,
                        onClick = { ed.emoji = em },
                        label = { Text(em, fontSize = 20.sp) }
                    )
                }
            }
        }

        Row(
            Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Palette.values().forEach { pal ->
                FilterChip(
                    selected = ed.palette == pal,
                    onClick = { ed.palette = pal },
                    label = { Text(pal.label) }
                )
            }
        }

        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Tune, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Slider(
                value = ed.intensity,
                onValueChange = { ed.intensity = it },
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
            selected = ed.drawMode,
            onClick = { ed.drawMode = !ed.drawMode },
            label = { Text(if (ed.drawMode) "Рисую область: проведи по фото" else "Своя область") },
            leadingIcon = { Icon(Icons.Rounded.Crop, null, Modifier.size(18.dp)) }
        )

        OutlinedTextField(
            value = ed.watermark,
            onValueChange = { ed.watermark = it.take(32) },
            singleLine = true,
            label = { Text("Водяной знак, например @ник") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}
