package cl.duoc.gestionoportunidades.validacion

import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import java.time.LocalDate

enum class CampoOportunidad {
    NOMBRE, ORGANIZACION, TIPO, DESCRIPCION, FUENTE, FECHA_DETECCION, FECHA_EVENTO, HORARIO,
    COMUNA, REGION, PUBLICO_OBJETIVO, PUBLICO_ESPERADO, PRIORIDAD, POTENCIAL, ESTADO, RESPONSABLE,
    CONTACTO_NOMBRE, CONTACTO_CARGO, CONTACTO_AREA, CONTACTO_CORREO, CONTACTO_TELEFONO,
    CONDICIONES, COSTO, OBSERVACIONES, DATOS_FICTICIOS
}

/** Datos tal como los escribe el usuario en el formulario (todo texto, antes de convertir). */
data class OportunidadFormulario(
    val nombre: String = "",
    val organizacion: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val fuente: String = "",
    val fechaDeteccion: String = "",
    val fechaEvento: String = "",
    val horario: String = "",
    val comuna: String = "",
    val region: String = "",
    val publicoObjetivo: String = "",
    val publicoEsperado: String = "",
    val prioridad: String = "Media",
    val potencial: String = "Medio",
    val estado: String = Catalogos.ESTADO_INICIAL,
    val responsable: String = "",
    val contactoNombre: String = "",
    val contactoCargo: String = "",
    val contactoArea: String = "",
    val contactoCorreo: String = "",
    val contactoTelefono: String = "",
    val propuestaEnviada: Boolean = false,
    val condiciones: String = "",
    val costo: String = "",
    val observaciones: String = "",
    val datosFicticios: Boolean = false
)

/**
 * Reglas de negocio del formulario de oportunidad. No conoce la interfaz:
 * recibe datos y devuelve un mensaje por campo con error.
 */
object ValidadorOportunidad {
    const val MAX_PUBLICO = 1_000_000
    const val MAX_DESCRIPCION = 300

    fun validar(f: OportunidadFormulario, hoy: LocalDate = LocalDate.now()): ResultadoValidacion<CampoOportunidad> {
        val e = mutableMapOf<CampoOportunidad, String>()

        // Datos de la oportunidad
        when {
            f.nombre.isBlank() -> e[CampoOportunidad.NOMBRE] = "Ingresa el nombre de la oportunidad"
            f.nombre.trim().length < 3 -> e[CampoOportunidad.NOMBRE] = "Debe tener al menos 3 caracteres"
            f.nombre.trim().length > 80 -> e[CampoOportunidad.NOMBRE] = "Máximo 80 caracteres"
        }
        when {
            f.organizacion.isBlank() -> e[CampoOportunidad.ORGANIZACION] = "Ingresa la organización responsable"
            f.organizacion.trim().length < 3 -> e[CampoOportunidad.ORGANIZACION] = "Debe tener al menos 3 caracteres"
        }
        if (f.tipo !in Catalogos.TIPOS) e[CampoOportunidad.TIPO] = "Selecciona el tipo de oportunidad"
        if (f.descripcion.trim().length > MAX_DESCRIPCION) {
            e[CampoOportunidad.DESCRIPCION] = "Máximo $MAX_DESCRIPCION caracteres (${f.descripcion.trim().length})"
        }
        if (f.fuente !in Catalogos.FUENTES) e[CampoOportunidad.FUENTE] = "Selecciona dónde se detectó"

        // Fechas: detección no futura; el evento no puede ocurrir antes de ser detectado.
        val deteccion = Fechas.desdeUi(f.fechaDeteccion)
        when {
            f.fechaDeteccion.isBlank() -> e[CampoOportunidad.FECHA_DETECCION] = "Ingresa la fecha de detección"
            deteccion == null -> e[CampoOportunidad.FECHA_DETECCION] = "Fecha inválida. Usa DD/MM/AAAA"
            deteccion.isAfter(hoy) -> e[CampoOportunidad.FECHA_DETECCION] = "No puede ser una fecha futura"
        }
        val evento = Fechas.desdeUi(f.fechaEvento)
        when {
            f.fechaEvento.isBlank() -> e[CampoOportunidad.FECHA_EVENTO] = "Ingresa la fecha del evento"
            evento == null -> e[CampoOportunidad.FECHA_EVENTO] = "Fecha inválida. Usa DD/MM/AAAA"
            deteccion != null && evento.isBefore(deteccion) ->
                e[CampoOportunidad.FECHA_EVENTO] = "El evento no puede ser anterior a su detección"
            evento.isAfter(hoy.plusYears(2)) -> e[CampoOportunidad.FECHA_EVENTO] = "Máximo 2 años hacia adelante"
        }
        if (f.horario.isNotBlank() && !Reglas.horarioValido(f.horario)) {
            e[CampoOportunidad.HORARIO] = "Usa HH:MM - HH:MM (inicio antes del término)"
        }

        // Ubicación general (el caso pide comuna/región, no direcciones particulares)
        when {
            f.comuna.isBlank() -> e[CampoOportunidad.COMUNA] = "Ingresa la comuna"
            f.comuna.trim().length < 3 -> e[CampoOportunidad.COMUNA] = "Comuna demasiado corta"
        }
        if (f.region !in Catalogos.REGIONES) e[CampoOportunidad.REGION] = "Selecciona la región"

        // Público
        val publico = f.publicoEsperado.trim().toIntOrNull()
        when {
            f.publicoEsperado.isBlank() -> e[CampoOportunidad.PUBLICO_ESPERADO] = "Indica el público esperado"
            publico == null -> e[CampoOportunidad.PUBLICO_ESPERADO] = "Debe ser un número entero, sin puntos"
            publico <= 0 -> e[CampoOportunidad.PUBLICO_ESPERADO] = "Debe ser mayor que 0"
            publico > MAX_PUBLICO -> e[CampoOportunidad.PUBLICO_ESPERADO] = "Valor demasiado alto (máx. 1.000.000)"
        }

        // Gestión
        if (f.prioridad !in Catalogos.PRIORIDADES) e[CampoOportunidad.PRIORIDAD] = "Selecciona la prioridad"
        if (f.potencial !in Catalogos.POTENCIALES) e[CampoOportunidad.POTENCIAL] = "Selecciona el potencial"
        if (f.estado !in Catalogos.ESTADOS) e[CampoOportunidad.ESTADO] = "Selecciona el estado"
        when {
            f.responsable.isBlank() -> e[CampoOportunidad.RESPONSABLE] = "Asigna un responsable interno"
            !Reglas.nombrePersonaValido(f.responsable) -> e[CampoOportunidad.RESPONSABLE] = "Usa solo letras"
        }

        // Contacto ficticio: opcional, pero si se ingresa debe ser coherente.
        val hayContacto = f.contactoNombre.isNotBlank() || f.contactoCorreo.isNotBlank() || f.contactoTelefono.isNotBlank()
        if (hayContacto) {
            when {
                f.contactoNombre.isBlank() -> e[CampoOportunidad.CONTACTO_NOMBRE] = "Ingresa el nombre del contacto"
                !Reglas.nombrePersonaValido(f.contactoNombre) -> e[CampoOportunidad.CONTACTO_NOMBRE] = "Usa solo letras"
            }
            if (f.contactoArea !in Catalogos.AREAS_CONTACTO) e[CampoOportunidad.CONTACTO_AREA] = "Selecciona el área del contacto"
            if (f.contactoCorreo.isBlank() && f.contactoTelefono.isBlank()) {
                e[CampoOportunidad.CONTACTO_CORREO] = "Ingresa al menos un correo o teléfono"
            }
        }
        if (f.contactoCorreo.isNotBlank() && !Reglas.correoValido(f.contactoCorreo)) {
            e[CampoOportunidad.CONTACTO_CORREO] = "Correo inválido (ej: nombre@empresa.cl)"
        }
        if (f.contactoTelefono.isNotBlank() && !Reglas.telefonoValido(f.contactoTelefono)) {
            e[CampoOportunidad.CONTACTO_TELEFONO] = "Formato: +56 9 1234 5678"
        }
        if (f.estado in setOf("Contacto realizado", "Propuesta enviada", "En espera de respuesta", "Participación confirmada") &&
            f.contactoNombre.isBlank()
        ) {
            e.putIfAbsent(CampoOportunidad.CONTACTO_NOMBRE, "Este estado requiere registrar el contacto")
        }

        // Condiciones
        val costo = f.costo.trim().toLongOrNull()
        if (f.costo.isNotBlank()) {
            when {
                costo == null -> e[CampoOportunidad.COSTO] = "Ingresa un monto en pesos, sin puntos"
                costo < 0 -> e[CampoOportunidad.COSTO] = "El costo no puede ser negativo"
            }
        }
        if (f.observaciones.trim().length > 500) e[CampoOportunidad.OBSERVACIONES] = "Máximo 500 caracteres"

        if (!f.datosFicticios) e[CampoOportunidad.DATOS_FICTICIOS] = "Confirma que los datos son ficticios o autorizados"

        return ResultadoValidacion(e)
    }
}
