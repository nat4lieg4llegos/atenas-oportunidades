package cl.duoc.gestionoportunidades.nativo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Recurso nativo: cámara. La foto (afiche, stand o material del evento) se recibe desde la
 * app de cámara del sistema y se guarda en el almacenamiento interno y privado de la app.
 */
object FotoHelper {
    suspend fun guardar(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
            val archivo = File(carpeta, "evento_${System.currentTimeMillis()}.jpg")
            FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            archivo.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cargar(ruta: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (ruta == null || !File(ruta).exists()) null else BitmapFactory.decodeFile(ruta)
    }

    /** Borra una foto tomada que finalmente no se guardó con la oportunidad. */
    fun descartar(ruta: String?) {
        ruta?.let { File(it).delete() }
    }
}
