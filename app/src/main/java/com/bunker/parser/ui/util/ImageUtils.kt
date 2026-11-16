package com.bunker.parser.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

object ImageUtils {
    fun loadScaledBitmap(filePath: String?, maxDim: Int = 1920): ImageBitmap? {
        if (filePath.isNullOrBlank()) return null
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, boundsOptions)

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(filePath, decodeOptions)
            bitmap?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
