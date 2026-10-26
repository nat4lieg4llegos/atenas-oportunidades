package cl.duoc.gestionoportunidades.di

import android.content.Context
import cl.duoc.gestionoportunidades.data.local.AtenasDbHelper
import cl.duoc.gestionoportunidades.data.local.OportunidadDao
import cl.duoc.gestionoportunidades.data.local.SeguimientoDao
import cl.duoc.gestionoportunidades.data.local.SesionManager
import cl.duoc.gestionoportunidades.data.repository.AuthRepository
import cl.duoc.gestionoportunidades.data.repository.OportunidadRepository

/**
 * Contenedor de dependencias simple (Service Locator): crea una sola instancia de la base
 * de datos y de los repositorios para toda la app, y los ViewModels los obtienen desde aquí.
 */
object AppContainer {
    @Volatile private var oportunidadRepo: OportunidadRepository? = null
    @Volatile private var authRepo: AuthRepository? = null

    fun oportunidades(context: Context): OportunidadRepository =
        oportunidadRepo ?: synchronized(this) {
            oportunidadRepo ?: AtenasDbHelper(context.applicationContext).let { helper ->
                OportunidadRepository(OportunidadDao(helper), SeguimientoDao(helper))
            }.also { oportunidadRepo = it }
        }

    fun auth(context: Context): AuthRepository =
        authRepo ?: synchronized(this) {
            authRepo ?: AuthRepository(SesionManager(context.applicationContext)).also { authRepo = it }
        }
}
