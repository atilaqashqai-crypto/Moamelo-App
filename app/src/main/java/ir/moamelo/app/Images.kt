package ir.moamelo.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

// Reads a picked photo, fixes rotation, shrinks it (max 1600px) and returns JPEG bytes
suspend fun uriToJpeg(ctx: Context, uri: Uri, maxSide: Int = 1600): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val resolver = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return@withContext null

        var orientation = ExifInterface.ORIENTATION_NORMAL
        resolver.openInputStream(uri)?.use {
            orientation = ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        }
        val scale = maxSide.toFloat() / maxOf(decoded.width, decoded.height)
        if (scale < 1f) m.postScale(scale, scale)
        val out = if (m.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        val bos = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.JPEG, 82, bos)
        bos.toByteArray()
    } catch (e: Exception) {
        null
    }
}
