package cl.duoc.gestionoportunidades.validacion

/** Resultado de validar un formulario: un mensaje por campo con error. */
data class ResultadoValidacion<C>(val errores: Map<C, String>) {
    val esValido: Boolean get() = errores.isEmpty()
    fun error(campo: C): String? = errores[campo]
}

/** Reglas reutilizables, sin dependencias de Android (se pueden probar con JUnit). */
internal object Reglas {
    private val CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    // Teléfono móvil chileno ficticio: +56 9 1234 5678 (espacios opcionales).
    private val TELEFONO_CL = Regex("^\\+?56\\s?9\\s?\\d{4}\\s?\\d{4}$")
    private val HORARIO = Regex("^([01]\\d|2[0-3]):([0-5]\\d)\\s?-\\s?([01]\\d|2[0-3]):([0-5]\\d)$")
    private val SOLO_TEXTO_NOMBRE = Regex("^[\\p{L} .'-]+$")

    fun correoValido(s: String) = CORREO.matches(s.trim())
    fun telefonoValido(s: String) = TELEFONO_CL.matches(s.trim())
    fun nombrePersonaValido(s: String) = SOLO_TEXTO_NOMBRE.matches(s.trim())

    /** Horario "HH:mm - HH:mm" donde el inicio es anterior al término. */
    fun horarioValido(s: String): Boolean {
        val m = HORARIO.matchEntire(s.trim()) ?: return false
        val (h1, m1, h2, m2) = m.destructured
        return h1.toInt() * 60 + m1.toInt() < h2.toInt() * 60 + m2.toInt()
    }
}
