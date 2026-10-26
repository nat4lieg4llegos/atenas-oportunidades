package cl.duoc.gestionoportunidades.data.model

/** Acción de seguimiento comercial asociada a una oportunidad (forma el historial de gestión). */
data class Seguimiento(
    val id: Long = 0,
    val oportunidadId: Long,
    val tipoAccion: String,
    val fecha: String,
    val resultado: String,
    val proximaAccion: String,
    val fechaProximo: String?,
    val responsable: String,
    val registradoEn: String = ""
)
