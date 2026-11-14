package com.example.glitchface

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

internal fun decode(context: Context, uri: Uri): Bitmap {
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

internal suspend fun detectFaces(bmp: Bitmap): List<Rect> {
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

/** Сохранение в Pictures/GlitchFace. JPEG/GIF собираются заново, поэтому EXIF и геотеги не попадают в файл. */
internal suspend fun saveMedia(
    context: Context,
    mime: String,
    ext: String,
    write: (OutputStream) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "glitch_${System.currentTimeMillis()}.$ext")
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlitchFace")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext null
    context.contentResolver.openOutputStream(uri)?.use(write)
    uri
}

internal suspend fun saveJpeg(context: Context, bmp: Bitmap): Uri? =
    saveMedia(context, "image/jpeg", "jpg") { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }

internal suspend fun exportGif(
    context: Context,
    src: Bitmap,
    faces: List<IndexedValue<Rect>>,
    p: Params
): Uri? {
    val bytes = withContext(Dispatchers.Default) {
        val k = min(1f, 480f / max(src.width, src.height))
        val small = if (k < 1f) {
            Bitmap.createScaledBitmap(
                src,
                max(1, (src.width * k).toInt()),
                max(1, (src.height * k).toInt()),
                true
            )
        } else src
        val rects = faces.map {
            val r = it.value
            IndexedValue(
                it.index,
                Rect((r.left * k).toInt(), (r.top * k).toInt(), (r.right * k).toInt(), (r.bottom * k).toInt())
            )
        }
        val gif = GifWriter(small.width, small.height, 12)
        for (f in 0 until 10) {
            val frame = render(small, rects, p.copy(seed = p.seed * 100 + f))
            gif.addFrame(frame)
            frame.recycle()
        }
        gif.finish()
    }
    return saveMedia(context, "image/gif", "gif") { it.write(bytes) }
}

internal suspend fun processBatch(
    context: Context,
    uris: List<Uri>,
    p: Params,
    onProgress: (Int, Int) -> Unit
): Int {
    var ok = 0
    uris.forEachIndexed { n, uri ->
        onProgress(n + 1, uris.size)
        try {
            val bmp = withContext(Dispatchers.Default) { decode(context, uri) }
            val faces = detectFaces(bmp)
            if (faces.isEmpty() && p.watermark.isBlank()) {
                bmp.recycle()
                return@forEachIndexed
            }
            val sel = faces.withIndex().toList()
            val out = withContext(Dispatchers.Default) { render(bmp, sel, p.copy(seed = p.seed + n)) }
            if (saveJpeg(context, out) != null) ok++
            out.recycle()
            bmp.recycle()
        } catch (_: Exception) {
        }
    }
    return ok
}
