package cl.duoc.gestionoportunidades.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duoc.gestionoportunidades.data.model.Fechas
import java.time.LocalDate

/**
 * Base de datos local SQLite (el caso indica "Room o SQLite").
 * Tablas: oportunidades y seguimientos (historial), relacionadas por clave foránea.
 */
class AtenasDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NOMBRE_BD, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${Tablas.OPORTUNIDADES} (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                organizacion TEXT NOT NULL,
                tipo TEXT NOT NULL,
                descripcion TEXT NOT NULL DEFAULT '',
                fuente TEXT NOT NULL DEFAULT '',
                fecha_deteccion TEXT NOT NULL,
                fecha_evento TEXT NOT NULL,
                horario TEXT NOT NULL DEFAULT '',
                comuna TEXT NOT NULL,
                region TEXT NOT NULL,
                publico_objetivo TEXT NOT NULL DEFAULT '',
                publico_esperado INTEGER NOT NULL,
                prioridad TEXT NOT NULL,
                potencial TEXT NOT NULL,
                estado TEXT NOT NULL,
                responsable TEXT NOT NULL,
                contacto_nombre TEXT NOT NULL DEFAULT '',
                contacto_cargo TEXT NOT NULL DEFAULT '',
                contacto_area TEXT NOT NULL DEFAULT '',
                contacto_correo TEXT NOT NULL DEFAULT '',
                contacto_telefono TEXT NOT NULL DEFAULT '',
                propuesta_enviada INTEGER NOT NULL DEFAULT 0,
                condiciones TEXT NOT NULL DEFAULT '',
                costo_referencial INTEGER NOT NULL DEFAULT 0,
                observaciones TEXT NOT NULL DEFAULT '',
                foto_ruta TEXT,
                proximo_seguimiento TEXT,
                actualizado TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE ${Tablas.SEGUIMIENTOS} (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                oportunidad_id INTEGER NOT NULL REFERENCES ${Tablas.OPORTUNIDADES}(id) ON DELETE CASCADE,
                tipo_accion TEXT NOT NULL,
                fecha TEXT NOT NULL,
                resultado TEXT NOT NULL,
                proxima_accion TEXT NOT NULL DEFAULT '',
                fecha_proximo TEXT,
                responsable TEXT NOT NULL DEFAULT '',
                registrado_en TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_seguimientos_oportunidad ON ${Tablas.SEGUIMIENTOS}(oportunidad_id)")
        insertarDatosDemo(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión académica: se recrea el esquema. En producción se escribirían migraciones.
        db.execSQL("DROP TABLE IF EXISTS ${Tablas.SEGUIMIENTOS}")
        db.execSQL("DROP TABLE IF EXISTS ${Tablas.OPORTUNIDADES}")
        onCreate(db)
    }

    /**
     * Datos sintéticos de demostración: se insertan solo al crear la base, por lo que
     * si el usuario los elimina no vuelven a aparecer. Las fechas son relativas a hoy
     * para que siempre existan eventos próximos y un seguimiento atrasado que mostrar.
     */
    private fun insertarDatosDemo(db: SQLiteDatabase) {
        val hoy = LocalDate.now()
        val ahora = Fechas.marcaTiempo()
        fun demo(
            nombre: String, org: String, tipo: String, diasEvento: Long, comuna: String, prioridad: String,
            potencial: String, estado: String, contacto: String, area: String, publico: Int, proximo: Long?
        ): Long {
            val v = ContentValues().apply {
                put("nombre", nombre); put("organizacion", org); put("tipo", tipo)
                put("descripcion", "Registro sintético para la demostración académica.")
                put("fuente", "Sitio web")
                put("fecha_deteccion", Fechas.aIso(hoy.minusDays(5)))
                put("fecha_evento", Fechas.aIso(hoy.plusDays(diasEvento)))
                put("horario", "09:00 - 17:00"); put("comuna", comuna); put("region", "Metropolitana")
                put("publico_objetivo", "Trabajadores y sus familias"); put("publico_esperado", publico)
                put("prioridad", prioridad); put("potencial", potencial); put("estado", estado)
                put("responsable", "Ejecutiva Demo")
                put("contacto_nombre", contacto); put("contacto_cargo", "Coordinación")
                put("contacto_area", if (contacto.isBlank()) "" else area)
                put("contacto_correo", if (contacto.isBlank()) "" else "contacto.demo@ejemplo.cl")
                put("contacto_telefono", if (contacto.isBlank()) "" else "+56 9 0000 0000")
                put("propuesta_enviada", if (estado == "Propuesta enviada" || estado == "En espera de respuesta") 1 else 0)
                put("condiciones", "Stand de 3x3 m, condiciones ficticias por confirmar")
                put("costo_referencial", 150000L)
                put("observaciones", "No contactar personas ni organizaciones reales.")
                put("proximo_seguimiento", proximo?.let { Fechas.aIso(hoy.plusDays(it)) })
                put("actualizado", ahora)
            }
            return db.insert(Tablas.OPORTUNIDADES, null, v)
        }
        fun historial(id: Long, accion: String, diasAtras: Long, resultado: String) {
            db.insert(Tablas.SEGUIMIENTOS, null, ContentValues().apply {
                put("oportunidad_id", id); put("tipo_accion", accion)
                put("fecha", Fechas.aIso(hoy.minusDays(diasAtras))); put("resultado", resultado)
                put("proxima_accion", ""); put("responsable", "Ejecutiva Demo"); put("registrado_en", ahora)
            })
        }

        val a = demo("Feria de beneficios laborales", "Empresa Horizonte Ficticia", "Feria empresarial", 6,
            "Puente Alto", "Alta", "Alto", "Contacto pendiente", "", "", 400, null)
        historial(a, "Investigación inicial", 5, "Evento detectado en sitio web ficticio de la empresa.")

        val b = demo("Jornada de bienestar municipal", "Municipalidad Ejemplo", "Actividad municipal", 20,
            "La Florida", "Media", "Medio", "Propuesta enviada", "Sam Ejemplo", "Bienestar", 250, -2)
        historial(b, "Primer contacto", 4, "Contacto ficticio interesado en conocer los convenios.")
        historial(b, "Envío de propuesta", 3, "Propuesta enviada con condiciones referenciales.")

        val c = demo("Encuentro anual de proveedores", "Instituto Cordillera Demo", "Evento corporativo", 45,
            "Santiago", "Baja", "Medio", "Evento identificado", "", "", 120, null)
        historial(c, "Investigación inicial", 2, "Se revisa afinidad con el público objetivo.")
    }

    companion object {
        const val NOMBRE_BD = "agencia_atenas.db"
        const val VERSION = 1
    }
}

object Tablas {
    const val OPORTUNIDADES = "oportunidades"
    const val SEGUIMIENTOS = "seguimientos"
}
