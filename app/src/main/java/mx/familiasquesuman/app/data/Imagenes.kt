package mx.familiasquesuman.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepara una foto elegida por la persona antes de subirla.
 * Se reduce a 1600 px como máximo, se endereza según la orientación de la cámara y se
 * vuelve a codificar como JPEG. Al recodificar se pierden todos los metadatos (incluida
 * la ubicación GPS), así nunca salen del teléfono. El backend además los vuelve a quitar.
 */
object Imagenes {
    private const val LADO_MAXIMO = 1600
    private const val CALIDAD = 85

    /**
     * Archivo temporal donde la app de cámara guarda la foto de un testimonio (RF-12).
     * Vive en la caché privada, se borran las fotos anteriores y el backend recibe solo
     * la versión reducida y sin metadatos.
     */
    fun nuevaFotoTemporal(context: Context): Uri {
        val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
        carpeta.listFiles()?.forEach { it.delete() }
        val archivo = File(carpeta, "testimonio_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    }

    suspend fun prepararParaSubir(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            if (limites.outWidth <= 0 || limites.outHeight <= 0) return@runCatching null

            var muestra = 1
            while (limites.outWidth / (muestra * 2) >= LADO_MAXIMO || limites.outHeight / (muestra * 2) >= LADO_MAXIMO) {
                muestra *= 2
            }
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                ?: return@runCatching null

            val grados = resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val escala = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(original.width, original.height))
            val matriz = Matrix().apply {
                postScale(escala, escala)
                postRotate(grados)
            }
            val final = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
            val salida = ByteArrayOutputStream()
            final.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
            if (final !== original) final.recycle()
            original.recycle()
            salida.toByteArray().takeIf { it.size <= 5 * 1024 * 1024 }
        }.getOrNull()
    }
}
