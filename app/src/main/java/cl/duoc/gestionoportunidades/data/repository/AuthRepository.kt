package cl.duoc.gestionoportunidades.data.repository

import cl.duoc.gestionoportunidades.data.local.SesionManager
import cl.duoc.gestionoportunidades.data.model.Usuario

/**
 * Autenticación con cuentas ficticias (el caso prohíbe credenciales reales o institucionales).
 * En la EFT este repositorio se reemplazará por una llamada a la API REST.
 */
class AuthRepository(private val sesion: SesionManager) {

    private data class Cuenta(val clave: String, val usuario: Usuario)

    private val cuentas = mapOf(
        "ejecutivo" to Cuenta("Atenas2026", Usuario("ejecutivo", "Ejecutiva Demo", "Ejecutivo comercial", "Santiago Sur")),
        "supervisor" to Cuenta("Atenas2026", Usuario("supervisor", "Supervisor Demo", "Supervisor comercial", "Región Metropolitana"))
    )

    fun iniciarSesion(usuario: String, contrasena: String): Usuario? {
        val cuenta = cuentas[usuario.trim().lowercase()] ?: return null
        if (cuenta.clave != contrasena) return null
        sesion.guardar(cuenta.usuario)
        return cuenta.usuario
    }

    fun usuarioActual(): Usuario? = sesion.usuarioActual()

    fun cerrarSesion() = sesion.cerrar()
}
