package cl.duoc.gestionoportunidades.data.local

import android.content.ContentValues
import cl.duoc.gestionoportunidades.data.model.Seguimiento

/** Acceso a la tabla de seguimientos (historial de gestión de cada oportunidad). */
class SeguimientoDao(private val helper: AtenasDbHelper) {

    fun listarPorOportunidad(oportunidadId: Long): List<Seguimiento> =
        helper.readableDatabase.query(
            Tablas.SEGUIMIENTOS, null, "oportunidad_id = ?", arrayOf(oportunidadId.toString()),
            null, null, "fecha DESC, id DESC"
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Seguimiento(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            oportunidadId = c.getLong(c.getColumnIndexOrThrow("oportunidad_id")),
                            tipoAccion = c.texto("tipo_accion"),
                            fecha = c.texto("fecha"),
                            resultado = c.texto("resultado"),
                            proximaAccion = c.texto("proxima_accion"),
                            fechaProximo = c.textoONulo("fecha_proximo"),
                            responsable = c.texto("responsable"),
                            registradoEn = c.texto("registrado_en")
                        )
                    )
                }
            }
        }

    fun insertar(s: Seguimiento): Long = helper.writableDatabase.insertOrThrow(
        Tablas.SEGUIMIENTOS, null,
        ContentValues().apply {
            put("oportunidad_id", s.oportunidadId); put("tipo_accion", s.tipoAccion); put("fecha", s.fecha)
            put("resultado", s.resultado); put("proxima_accion", s.proximaAccion); put("fecha_proximo", s.fechaProximo)
            put("responsable", s.responsable); put("registrado_en", s.registradoEn)
        }
    )
}
