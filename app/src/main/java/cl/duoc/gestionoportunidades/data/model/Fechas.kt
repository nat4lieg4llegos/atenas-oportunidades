package cl.duoc.gestionoportunidades.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Conversión de fechas. La interfaz usa DD/MM/AAAA (formato chileno) y la base de datos ISO.
 * ResolverStyle.STRICT rechaza fechas imposibles como 31/02/2026.
 */
object Fechas {
    private val formatoUi: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val formatoIso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val formatoMarca: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")

    fun desdeUi(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim(), formatoUi)
    } catch (e: DateTimeParseException) {
        null
    }

    fun desdeIso(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim(), formatoIso)
    } catch (e: DateTimeParseException) {
        null
    }

    fun aUi(fecha: LocalDate): String = fecha.format(formatoUi)
    fun aIso(fecha: LocalDate): String = fecha.format(formatoIso)

    /** Convierte un texto ISO guardado a DD/MM/AAAA para mostrarlo; si no es válido, lo devuelve tal cual. */
    fun isoAUi(iso: String?): String = iso?.let { desdeIso(it)?.let(::aUi) ?: it }.orEmpty()

    /** Convierte un texto DD/MM/AAAA ingresado a ISO; devuelve null si no es una fecha válida. */
    fun uiAIso(texto: String): String? = desdeUi(texto)?.let(::aIso)

    fun marcaTiempo(ahora: LocalDateTime = LocalDateTime.now()): String = ahora.format(formatoMarca)
}
