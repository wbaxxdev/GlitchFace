package com.example.glitchface

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/** Простой GIF-энкодер: палитра 3-3-2 (256 цветов) + дизеринг, LZW. */
internal class GifWriter(private val w: Int, private val h: Int, private val delayCs: Int) {
    private val out = ByteArrayOutputStream()
    private var started = false

    private fun le(v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    private fun header() {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        le(w); le(h)
        out.write(0xF7); out.write(0); out.write(0)
        for (i in 0 until 256) {
            out.write(((i shr 5) and 7) * 255 / 7)
            out.write(((i shr 2) and 7) * 255 / 7)
            out.write((i and 3) * 255 / 3)
        }
        out.write(0x21); out.write(0xFF); out.write(0x0B)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(0x03); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x00)
        started = true
    }

    fun addFrame(bmp: Bitmap) {
        if (!started) header()
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val idx = ByteArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = px[y * w + x]
                val d = BAYER[(y and 3) * 4 + (x and 3)] - 8
                val r = (((c shr 16) and 0xFF) + d * 2).coerceIn(0, 255)
                val g = (((c shr 8) and 0xFF) + d * 2).coerceIn(0, 255)
                val b = ((c and 0xFF) + d * 4).coerceIn(0, 255)
                val ri = minOf(7, (r + 16) shr 5)
                val gi = minOf(7, (g + 16) shr 5)
                val bi = minOf(3, (b + 32) shr 6)
                idx[y * w + x] = ((ri shl 5) or (gi shl 2) or bi).toByte()
            }
        }
        out.write(0x21); out.write(0xF9); out.write(4); out.write(0x04)
        le(delayCs)
        out.write(0); out.write(0)
        out.write(0x2C); le(0); le(0); le(w); le(h); out.write(0)
        lzw(idx)
    }

    fun finish(): ByteArray {
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun lzw(pixels: ByteArray) {
        val minCode = 8
        out.write(minCode)
        val clear = 1 shl minCode
        val eoi = clear + 1
        var codeSize = minCode + 1
        var next = eoi + 1
        val dict = HashMap<Int, Int>()
        val bits = BitSink(out)
        bits.write(clear, codeSize)
        var prefix = pixels[0].toInt() and 0xFF
        for (i in 1 until pixels.size) {
            val c = pixels[i].toInt() and 0xFF
            val key = (prefix shl 8) or c
            val found = dict[key]
            if (found != null) {
                prefix = found
            } else {
                bits.write(prefix, codeSize)
                if (next < 4096) {
                    dict[key] = next
                    if (next == (1 shl codeSize) && codeSize < 12) codeSize++
                    next++
                } else {
                    bits.write(clear, codeSize)
                    dict.clear()
                    codeSize = minCode + 1
                    next = eoi + 1
                }
                prefix = c
            }
        }
        bits.write(prefix, codeSize)
        bits.write(eoi, codeSize)
        bits.flush()
        out.write(0)
    }

    private class BitSink(private val out: ByteArrayOutputStream) {
        private var acc = 0
        private var n = 0
        private val buf = ByteArray(255)
        private var len = 0

        fun write(code: Int, size: Int) {
            acc = acc or (code shl n)
            n += size
            while (n >= 8) {
                put(acc and 0xFF)
                acc = acc ushr 8
                n -= 8
            }
        }

        private fun put(b: Int) {
            buf[len++] = b.toByte()
            if (len == 255) block()
        }

        private fun block() {
            if (len > 0) {
                out.write(len)
                out.write(buf, 0, len)
                len = 0
            }
        }

        fun flush() {
            if (n > 0) {
                put(acc and 0xFF)
                acc = 0
                n = 0
            }
            block()
        }
    }

    companion object {
        private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)
    }
}
