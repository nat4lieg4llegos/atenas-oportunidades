package cl.duoc.gestionoportunidades.validacion

enum class CampoLogin { USUARIO, CONTRASENA }

/** Valida el formato de las credenciales antes de consultar al repositorio de autenticación. */
object ValidadorLogin {
    private val USUARIO = Regex("^[a-zA-Z0-9._]+$")

    fun validar(usuario: String, contrasena: String): ResultadoValidacion<CampoLogin> {
        val errores = mutableMapOf<CampoLogin, String>()
        val u = usuario.trim()
        when {
            u.isEmpty() -> errores[CampoLogin.USUARIO] = "Ingresa tu usuario"
            u.length < 4 -> errores[CampoLogin.USUARIO] = "El usuario debe tener al menos 4 caracteres"
            !USUARIO.matches(u) -> errores[CampoLogin.USUARIO] = "Usa solo letras, números, punto o guion bajo"
        }
        when {
            contrasena.isEmpty() -> errores[CampoLogin.CONTRASENA] = "Ingresa tu contraseña"
            contrasena.length < 6 -> errores[CampoLogin.CONTRASENA] = "La contraseña debe tener al menos 6 caracteres"
        }
        return ResultadoValidacion(errores)
    }
}
