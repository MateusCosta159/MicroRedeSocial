package com.mateus.microredesocial.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

class Base64Converter {

    companion object {
        private const val MAX_DIMENSION = 800
    }

    fun bitmapToString(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        if (scaled != bitmap) scaled.recycle()
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    fun stringToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return bitmap
        val ratio = minOf(MAX_DIMENSION.toFloat() / w, MAX_DIMENSION.toFloat() / h)
        return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }
}