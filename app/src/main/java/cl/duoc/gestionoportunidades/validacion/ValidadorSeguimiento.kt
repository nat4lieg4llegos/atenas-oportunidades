package cl.duoc.gestionoportunidades.validacion

import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import java.time.LocalDate

enum class CampoSeguimiento { TIPO_ACCION, FECHA, RESULTADO, PROXIMA_ACCION, FECHA_PROXIMO }

data class SeguimientoFormulario(
    val tipoAccion: String = "",
    val fecha: String = "",
    val resultado: String = "",
    val proximaAccion: String = "",
    val fechaProximo: String = "",
    val crearRecordatorio: Boolean = true
)

object ValidadorSeguimiento {
    fun validar(f: SeguimientoFormulario, hoy: LocalDate = LocalDate.now()): ResultadoValidacion<CampoSeguimiento> {
        val e = mutableMapOf<CampoSeguimiento, String>()
        if (f.tipoAccion !in Catalogos.TIPOS_ACCION) e[CampoSeguimiento.TIPO_ACCION] = "Selecciona el tipo de acción"

        val fecha = Fechas.desdeUi(f.fecha)
        when {
            f.fecha.isBlank() -> e[CampoSeguimiento.FECHA] = "Ingresa la fecha de la acción"
            fecha == null -> e[CampoSeguimiento.FECHA] = "Fecha inválida. Usa DD/MM/AAAA"
            fecha.isAfter(hoy) -> e[CampoSeguimiento.FECHA] = "Registra acciones ya realizadas (no futuras)"
        }
        when {
            f.resultado.isBlank() -> e[CampoSeguimiento.RESULTADO] = "Describe el resultado de la acción"
            f.resultado.trim().length < 5 -> e[CampoSeguimiento.RESULTADO] = "Describe con al menos 5 caracteres"
            f.resultado.trim().length > 300 -> e[CampoSeguimiento.RESULTADO] = "Máximo 300 caracteres"
        }

        // Si se programa una próxima acción, debe tener fecha (y viceversa).
        val proxima = Fechas.desdeUi(f.fechaProximo)
        if (f.proximaAccion.isBlank() && f.fechaProximo.isNotBlank()) {
            e[CampoSeguimiento.PROXIMA_ACCION] = "Indica qué acción se hará en esa fecha"
        }
        when {
            f.proximaAccion.isNotBlank() && f.fechaProximo.isBlank() ->
                e[CampoSeguimiento.FECHA_PROXIMO] = "Indica cuándo se hará la próxima acción"
            f.fechaProximo.isNotBlank() && proxima == null -> e[CampoSeguimiento.FECHA_PROXIMO] = "Fecha inválida. Usa DD/MM/AAAA"
            proxima != null && proxima.isBefore(hoy) -> e[CampoSeguimiento.FECHA_PROXIMO] = "Debe ser hoy o una fecha futura"
            proxima != null && fecha != null && proxima.isBefore(fecha) ->
                e[CampoSeguimiento.FECHA_PROXIMO] = "No puede ser anterior a la acción registrada"
        }
        return ResultadoValidacion(e)
    }
}
