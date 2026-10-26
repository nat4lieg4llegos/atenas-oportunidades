package cl.duoc.gestionoportunidades.nativo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Comuna y región aproximadas obtenidas a partir de una lectura puntual de ubicación. */
data class UbicacionAproximada(val comuna: String?, val regionTexto: String?, val latitud: Double, val longitud: Double)

/**
 * Recurso nativo: ubicación. Cumple las restricciones del caso:
 * es opcional, se lee una sola vez a pedido del usuario, no hay seguimiento continuo
 * y no se guardan coordenadas (solo se completa comuna y región del evento).
 */
class UbicacionHelper(private val context: Context) {

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun tienePermisoPreciso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Devuelve null si no hay permiso, el GPS está apagado o no se obtuvo lectura en 15 segundos. */
    suspend fun obtenerUbicacion(): UbicacionAproximada? {
        if (!tienePermiso()) return null
        val ubicacion = withTimeoutOrNull(15_000) { leerUbicacion() } ?: return null
        val direccion = geocodificar(ubicacion)
        return UbicacionAproximada(
            comuna = direccion?.locality ?: direccion?.subAdminArea,
            regionTexto = direccion?.adminArea,
            latitud = ubicacion.latitude,
            longitud = ubicacion.longitude
        )
    }

    @SuppressLint("MissingPermission") // el permiso se verifica en tienePermiso()
    private suspend fun leerUbicacion(): Location? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val proveedores = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (tienePermisoPreciso()) add(LocationManager.GPS_PROVIDER)
        }.filter { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }

        if (proveedores.isEmpty()) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (p in proveedores) {
                val actual = try {
                    suspendCancellableCoroutine<Location?> { cont ->
                        lm.getCurrentLocation(p, null, ContextCompat.getMainExecutor(context)) { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                    }
                } catch (e: SecurityException) {
                    null
                }
                if (actual != null) return actual
            }
        }
        // Android 8–10 o sin lectura nueva: última ubicación conocida.
        return proveedores.mapNotNull { p ->
            try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null }
        }.maxByOrNull { it.time }
    }

    private suspend fun geocodificar(l: Location): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("es", "CL"))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Address?> { cont ->
                    geocoder.getFromLocation(l.latitude, l.longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    })
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(l.latitude, l.longitude, 1)?.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
