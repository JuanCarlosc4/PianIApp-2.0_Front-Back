package com.piania.app.ui.partituras

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    // VERSIÓN LIMPIA/LIGERA (Para fotos de pantalla o imágenes digitales)
    fun prepararImagenParaSubida(context: Context, imageUri: Uri): File? {
        try {
            // 1. Cargar imagen
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var originalBitmap = context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // 2. ESCALADO NORMAL (3000px)
            // No forzamos resoluciones gigantes. 3000px es un buen equilibrio.
            val targetWidth = 3000
            val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            if (originalBitmap != scaledBitmap && !originalBitmap.isRecycled) {
                originalBitmap.recycle()
            }

            // 3. GRISES SUAVES (IMPORTANTE: Sin efecto "Negrita")
            // Solo quitamos el color, no tocamos el contraste para no empeorar el Moiré.
            val finalBitmap = toSimpleGrayscale(scaledBitmap)

            if (scaledBitmap != finalBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }

            // 4. GUARDAR
            val file = File(context.cacheDir, "partitura_procesada.jpg")
            FileOutputStream(file).use { out ->
                // Calidad alta
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (!finalBitmap.isRecycled) finalBitmap.recycle()

            return file

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // --- FILTRO DE GRISES ESTÁNDAR ---
    private fun toSimpleGrayscale(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // Solo quitar color

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)

        return dest
    }
}