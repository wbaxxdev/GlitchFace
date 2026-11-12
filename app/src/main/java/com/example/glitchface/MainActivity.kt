package com.example.glitchface

import android.content.ContentValues
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.graphics.ImageDecoder
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var imageView: ImageView
    private var result: Bitmap? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { d, _, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            d.isMutableRequired = true
        }
        process(bmp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val pick = Button(this).apply {
            text = "Выбрать фото"
            setOnClickListener {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        val save = Button(this).apply {
            text = "Сохранить"
            setOnClickListener { result?.let { saveToGallery(it) } }
        }
        root.addView(imageView); root.addView(pick); root.addView(save)
        setContentView(root)
    }

    private fun process(bmp: Bitmap) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        FaceDetection.getClient(options)
            .process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { faces ->
                val out = glitchFaces(bmp, faces.map { it.boundingBox })
                result = out
                imageView.setImageBitmap(out)
                if (faces.isEmpty())
                    Toast.makeText(this, "Лица не найдены", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun glitchFaces(src: Bitmap, faces: List<Rect>): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val rnd = Random(System.nanoTime())
        val bounds = Rect(0, 0, src.width, src.height)

        for (f in faces) {
            val padX = (f.width() * 0.2f).toInt()
            val padY = (f.height() * 0.25f).toInt()
            val r = Rect(f.left - padX, f.top - padY, f.right + padX, f.bottom + padY)
            if (!r.intersect(bounds) || r.width() < 8 || r.height() < 8) continue

            val piece = Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height())
            val w = piece.width
            var y = 0
            while (y < piece.height) {
                val minH = max(2, piece.height / 70)
                val maxH = max(minH + 1, piece.height / 12)
                val h = min(rnd.nextInt(minH, maxH), piece.height - y)

                val winW = rnd.nextInt(max(2, w / 10), max(3, w / 2))
                val winX = rnd.nextInt(0, max(1, w - winW))
                val srcRect = Rect(winX, y, winX + winW, y + h)
                val dst = Rect(r.left, r.top + y, r.right, r.top + y + h)
                canvas.drawBitmap(piece, srcRect, dst, null)

                if (rnd.nextFloat() < 0.08f) {
                    val p = Paint().apply { color = Color.argb(200, 255, 255, 255) }
                    val x0 = r.left + rnd.nextInt(0, w / 2)
                    canvas.drawRect(x0.toFloat(), dst.top.toFloat(),
                        (x0 + rnd.nextInt(w / 8, w / 2)).toFloat(), dst.bottom.toFloat(), p)
                }
                y += h
            }
            piece.recycle()
        }

        val line = Paint().apply { color = Color.argb(30, 0, 0, 0); strokeWidth = 1f }
        var yy = 0
        while (yy < out.height) {
            canvas.drawLine(0f, yy.toFloat(), out.width.toFloat(), yy.toFloat(), line)
            yy += 3
        }
        val tint = Paint().apply {
            colorFilter = PorterDuffColorFilter(Color.argb(40, 200, 140, 220), PorterDuff.Mode.SRC_ATOP)
        }
        val tinted = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
        Canvas(tinted).drawBitmap(out, 0f, 0f, tint)
        return tinted
    }

    private fun saveToGallery(bmp: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "glitch_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlitchFace")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
    }
}
