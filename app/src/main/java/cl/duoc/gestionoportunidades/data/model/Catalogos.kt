package cl.duoc.gestionoportunidades.data.model

/** Valores definidos por el caso semestral "Gestión móvil de oportunidades y activaciones corporativas". */
object Catalogos {
    val TIPOS = listOf(
        "Evento corporativo", "Feria empresarial", "Actividad municipal", "Jornada de beneficios",
        "Feria institucional", "Actividad de trabajadores", "Activación comercial", "Otra instancia relevante"
    )

    const val ESTADO_INICIAL = "Evento identificado"
    val ESTADOS = listOf(
        "Evento identificado", "Contacto pendiente", "Contacto realizado", "Propuesta enviada",
        "En espera de respuesta", "Participación confirmada", "Oportunidad descartada",
        "Activación realizada", "Activación cancelada"
    )
    val ESTADOS_CERRADOS = setOf("Oportunidad descartada", "Activación realizada", "Activación cancelada")
    val ESTADOS_ACTIVOS = ESTADOS.toSet() - ESTADOS_CERRADOS
    val ESTADOS_SIN_CONTACTO = setOf("Evento identificado", "Contacto pendiente")

    val PRIORIDADES = listOf("Alta", "Media", "Baja")
    val POTENCIALES = listOf("Alto", "Medio", "Bajo")

    val AREAS_CONTACTO = listOf(
        "Eventos", "Activaciones", "Marketing", "Comunicaciones",
        "Recursos Humanos", "Bienestar", "Beneficios", "Administración"
    )

    val TIPOS_ACCION = listOf(
        "Investigación inicial", "Búsqueda de contacto", "Primer contacto", "Envío de presentación",
        "Envío de propuesta", "Solicitud de antecedentes", "Reunión", "Llamada", "Seguimiento",
        "Recepción de respuesta", "Confirmación", "Cierre"
    )
    const val ACCION_CAMBIO_ESTADO = "Cambio de estado"

    val FUENTES = listOf("Redes sociales", "Sitio web", "Correo electrónico", "Contacto directo", "Referencia", "Otra")

    val REGIONES = listOf(
        "Arica y Parinacota", "Tarapacá", "Antofagasta", "Atacama", "Coquimbo", "Valparaíso",
        "Metropolitana", "O'Higgins", "Maule", "Ñuble", "Biobío", "La Araucanía",
        "Los Ríos", "Los Lagos", "Aysén", "Magallanes"
    )

    /** Asocia el nombre que entrega el geocodificador (p. ej. "Región Metropolitana de Santiago") con el catálogo. */
    fun regionDesdeTexto(texto: String?): String? {
        if (texto.isNullOrBlank()) return null
        val normal = normalizar(texto)
        return REGIONES.firstOrNull { normal.contains(normalizar(it)) }
            ?: if (normal.contains("libertador") || normal.contains("ohiggins")) "O'Higgins" else null
    }

    private fun normalizar(s: String): String = s.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
        .replace("'", "").replace("’", "")
}
