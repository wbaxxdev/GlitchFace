package com.example.glitchface

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.max
import kotlin.random.Random

internal enum class Mode(val label: String) {
    GLITCH("Глитч"), PIXEL("Пиксели"), BLUR("Размытие"), EMOJI("Эмодзи")
}

internal enum class Palette(val label: String, val tint: Int, val mono: Boolean, val scan: Boolean) {
    NONE("Без фильтра", 0, false, false),
    PINK("Розовый", Color.argb(45, 200, 140, 220), false, true),
    GREEN("Матрица", Color.argb(70, 0, 255, 90), false, true),
    MONO("Ч/б", 0, true, true),
    NEON("Неон", Color.argb(60, 0, 200, 255), false, true)
}

internal data class Params(
    val mode: Mode,
    val intensity: Float,
    val palette: Palette,
    val emoji: String,
    val watermark: String,
    val seed: Int
)

internal val EMOJIS = listOf("😎", "🙈", "🤡", "👽", "🐵", "🦊", "💀", "🌸")

internal fun render(src: Bitmap, faces: List<IndexedValue<Rect>>, p: Params): Bitmap {
    var out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val bounds = Rect(0, 0, src.width, src.height)

    for ((i, rect) in faces) {
        val r = Rect(rect)
        if (!r.intersect(bounds) || r.width() < 8 || r.height() < 8) continue
        val piece = Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height())
        when (p.mode) {
            Mode.GLITCH -> glitchRect(canvas, piece, r, Random(p.seed * 1000 + i), p.intensity)
            Mode.PIXEL -> pixelRect(canvas, piece, r, p.intensity)
            Mode.BLUR -> blurRect(canvas, piece, r, p.intensity)
            Mode.EMOJI -> {
                blurRect(canvas, piece, r, 1f)
                emojiRect(canvas, r, p.emoji)
            }
        }
        if (piece !== src) piece.recycle()
    }

    if (p.palette != Palette.NONE) {
        val res = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
        val c2 = Canvas(res)
        val paint = Paint()
        paint.colorFilter = if (p.palette.mono) {
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        } else {
            PorterDuffColorFilter(p.palette.tint, PorterDuff.Mode.SRC_ATOP)
        }
        c2.drawBitmap(out, 0f, 0f, paint)
        if (p.palette.scan) {
            val step = max(3, res.height / 450)
            val line = Paint().apply {
                color = Color.argb(34, 0, 0, 0)
                strokeWidth = max(1f, step / 3f)
            }
            var yy = 0
            while (yy < res.height) {
                c2.drawLine(0f, yy.toFloat(), res.width.toFloat(), yy.toFloat(), line)
                yy += step
            }
        }
        out.recycle()
        out = res
    }

    if (p.watermark.isNotBlank()) {
        val c3 = Canvas(out)
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = out.width * 0.04f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
            setShadowLayer(4f, 0f, 2f, Color.argb(160, 0, 0, 0))
        }
        val pad = out.width * 0.03f
        c3.drawText(p.watermark.trim(), out.width - pad, out.height - pad, t)
    }
    return out
}

private fun glitchRect(canvas: Canvas, piece: Bitmap, r: Rect, rnd: Random, intensity: Float) {
    val w = piece.width
    val hgt = piece.height
    val minH = max(2, hgt / 90)
    val maxH = max(minH + 1, (hgt / (8f + 28f * intensity)).toInt())
    val winMin = max(2, w / 12)
    val winMax = max(winMin + 1, (w * (0.75f - 0.55f * intensity)).toInt())
    val gapChance = 0.04f + 0.10f * intensity
    val gap = Paint().apply { color = Color.argb(210, 255, 255, 255) }

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
}

private fun pixelRect(canvas: Canvas, piece: Bitmap, r: Rect, intensity: Float) {
    val block = max(4f, r.width() * (0.04f + 0.14f * intensity)).toInt()
    val sw = max(1, r.width() / block)
    val sh = max(1, r.height() / block)
    val small = Bitmap.createScaledBitmap(piece, sw, sh, true)
    val paint = Paint().apply { isFilterBitmap = false }
    canvas.drawBitmap(small, null, r, paint)
    if (small !== piece) small.recycle()
}

private fun blurRect(canvas: Canvas, piece: Bitmap, r: Rect, intensity: Float) {
    val f = 6f + 34f * intensity
    val sw = max(1, (piece.width / f).toInt())
    val sh = max(1, (piece.height / f).toInt())
    val small = Bitmap.createScaledBitmap(piece, sw, sh, true)
    val paint = Paint().apply { isFilterBitmap = true }
    canvas.drawBitmap(small, null, r, paint)
    if (small !== piece) small.recycle()
}

private fun emojiRect(canvas: Canvas, r: Rect, emoji: String) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = max(r.width(), r.height()) * 0.85f
    }
    val fm = paint.fontMetrics
    canvas.drawText(emoji, r.exactCenterX(), r.exactCenterY() - (fm.ascent + fm.descent) / 2f, paint)
}
