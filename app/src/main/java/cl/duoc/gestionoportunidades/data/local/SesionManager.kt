package cl.duoc.gestionoportunidades.data.local

import android.content.Context
import cl.duoc.gestionoportunidades.data.model.Usuario

/** Control de sesión: guarda el usuario ficticio autenticado en SharedPreferences (nunca la contraseña). */
class SesionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sesion_atenas", Context.MODE_PRIVATE)

    fun guardar(u: Usuario) {
        prefs.edit()
            .putString(K_USUARIO, u.usuario).putString(K_NOMBRE, u.nombre)
            .putString(K_PERFIL, u.perfil).putString(K_ZONA, u.zona)
            .apply()
    }

    fun usuarioActual(): Usuario? {
        val usuario = prefs.getString(K_USUARIO, null) ?: return null
        return Usuario(
            usuario = usuario,
            nombre = prefs.getString(K_NOMBRE, "").orEmpty(),
            perfil = prefs.getString(K_PERFIL, "").orEmpty(),
            zona = prefs.getString(K_ZONA, "").orEmpty()
        )
    }

    fun cerrar() = prefs.edit().clear().apply()

    private companion object {
        const val K_USUARIO = "usuario"
        const val K_NOMBRE = "nombre"
        const val K_PERFIL = "perfil"
        const val K_ZONA = "zona"
    }
}
