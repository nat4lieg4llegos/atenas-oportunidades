package cl.duoc.gestionoportunidades.data.local

import android.content.ContentValues
import android.database.Cursor
import cl.duoc.gestionoportunidades.data.model.Oportunidad

/** Acceso a la tabla de oportunidades (consultas SQL parametrizadas para evitar inyección). */
class OportunidadDao(private val helper: AtenasDbHelper) {

    fun listar(): List<Oportunidad> =
        helper.readableDatabase.query(
            Tablas.OPORTUNIDADES, null, null, null, null, null, "fecha_evento ASC, id DESC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.aOportunidad()) } }

    fun obtener(id: Long): Oportunidad? =
        helper.readableDatabase.query(
            Tablas.OPORTUNIDADES, null, "id = ?", arrayOf(id.toString()), null, null, null
        ).use { c -> if (c.moveToFirst()) c.aOportunidad() else null }

    fun insertar(o: Oportunidad): Long = helper.writableDatabase.insertOrThrow(Tablas.OPORTUNIDADES, null, o.aValores())

    fun actualizar(o: Oportunidad): Int =
        helper.writableDatabase.update(Tablas.OPORTUNIDADES, o.aValores(), "id = ?", arrayOf(o.id.toString()))

    fun eliminar(id: Long): Int = helper.writableDatabase.delete(Tablas.OPORTUNIDADES, "id = ?", arrayOf(id.toString()))

    private fun Oportunidad.aValores() = ContentValues().apply {
        put("nombre", nombre); put("organizacion", organizacion); put("tipo", tipo)
        put("descripcion", descripcion); put("fuente", fuente)
        put("fecha_deteccion", fechaDeteccion); put("fecha_evento", fechaEvento); put("horario", horario)
        put("comuna", comuna); put("region", region)
        put("publico_objetivo", publicoObjetivo); put("publico_esperado", publicoEsperado)
        put("prioridad", prioridad); put("potencial", potencial); put("estado", estado); put("responsable", responsable)
        put("contacto_nombre", contactoNombre); put("contacto_cargo", contactoCargo); put("contacto_area", contactoArea)
        put("contacto_correo", contactoCorreo); put("contacto_telefono", contactoTelefono)
        put("propuesta_enviada", if (propuestaEnviada) 1 else 0)
        put("condiciones", condiciones); put("costo_referencial", costoReferencial)
        put("observaciones", observaciones); put("foto_ruta", fotoRuta)
        put("proximo_seguimiento", proximoSeguimiento); put("actualizado", actualizado)
    }

    private fun Cursor.aOportunidad() = Oportunidad(
        id = getLong(getColumnIndexOrThrow("id")),
        nombre = texto("nombre"),
        organizacion = texto("organizacion"),
        tipo = texto("tipo"),
        descripcion = texto("descripcion"),
        fuente = texto("fuente"),
        fechaDeteccion = texto("fecha_deteccion"),
        fechaEvento = texto("fecha_evento"),
        horario = texto("horario"),
        comuna = texto("comuna"),
        region = texto("region"),
        publicoObjetivo = texto("publico_objetivo"),
        publicoEsperado = getInt(getColumnIndexOrThrow("publico_esperado")),
        prioridad = texto("prioridad"),
        potencial = texto("potencial"),
        estado = texto("estado"),
        responsable = texto("responsable"),
        contactoNombre = texto("contacto_nombre"),
        contactoCargo = texto("contacto_cargo"),
        contactoArea = texto("contacto_area"),
        contactoCorreo = texto("contacto_correo"),
        contactoTelefono = texto("contacto_telefono"),
        propuestaEnviada = getInt(getColumnIndexOrThrow("propuesta_enviada")) == 1,
        condiciones = texto("condiciones"),
        costoReferencial = getLong(getColumnIndexOrThrow("costo_referencial")),
        observaciones = texto("observaciones"),
        fotoRuta = textoONulo("foto_ruta"),
        proximoSeguimiento = textoONulo("proximo_seguimiento"),
        actualizado = texto("actualizado")
    )
}

internal fun Cursor.texto(columna: String): String = getString(getColumnIndexOrThrow(columna)).orEmpty()

internal fun Cursor.textoONulo(columna: String): String? {
    val i = getColumnIndexOrThrow(columna)
    return if (isNull(i)) null else getString(i)
}
