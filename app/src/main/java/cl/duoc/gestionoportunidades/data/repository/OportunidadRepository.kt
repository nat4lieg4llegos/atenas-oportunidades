package cl.duoc.gestionoportunidades.data.repository

import cl.duoc.gestionoportunidades.data.local.OportunidadDao
import cl.duoc.gestionoportunidades.data.local.SeguimientoDao
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Única fuente de datos para los ViewModels. Expone la lista como StateFlow y la
 * vuelve a emitir después de cada escritura, para que todas las pantallas se actualicen solas.
 * Todo el acceso a SQLite se ejecuta fuera del hilo principal (Dispatchers.IO).
 */
class OportunidadRepository(
    private val oportunidadDao: OportunidadDao,
    private val seguimientoDao: SeguimientoDao
) {
    private val _oportunidades = MutableStateFlow<List<Oportunidad>>(emptyList())
    val oportunidades: StateFlow<List<Oportunidad>> = _oportunidades.asStateFlow()

    suspend fun refrescar() = withContext(Dispatchers.IO) {
        _oportunidades.value = oportunidadDao.listar()
    }

    suspend fun obtener(id: Long): Oportunidad? = withContext(Dispatchers.IO) { oportunidadDao.obtener(id) }

    suspend fun seguimientos(oportunidadId: Long): List<Seguimiento> =
        withContext(Dispatchers.IO) { seguimientoDao.listarPorOportunidad(oportunidadId) }

    /** Inserta o actualiza. Si cambió el estado, lo deja registrado en el historial. Devuelve el id. */
    suspend fun guardar(o: Oportunidad, responsable: String): Long = withContext(Dispatchers.IO) {
        val ahora = Fechas.marcaTiempo()
        val id = if (o.id == 0L) {
            val nuevoId = oportunidadDao.insertar(o.copy(actualizado = ahora))
            registrarAccion(nuevoId, "Investigación inicial", "Oportunidad registrada en la aplicación.", responsable, ahora)
            nuevoId
        } else {
            val anterior = oportunidadDao.obtener(o.id)
            oportunidadDao.actualizar(o.copy(actualizado = ahora))
            if (anterior != null && anterior.estado != o.estado) {
                registrarAccion(o.id, Catalogos.ACCION_CAMBIO_ESTADO, "${anterior.estado} → ${o.estado}", responsable, ahora)
            }
            if (anterior?.fotoRuta != null && anterior.fotoRuta != o.fotoRuta) File(anterior.fotoRuta).delete()
            o.id
        }
        _oportunidades.value = oportunidadDao.listar()
        id
    }

    suspend fun cambiarEstado(o: Oportunidad, nuevoEstado: String, responsable: String) {
        if (o.estado == nuevoEstado) return
        guardar(o.copy(estado = nuevoEstado, propuestaEnviada = o.propuestaEnviada || nuevoEstado == "Propuesta enviada"), responsable)
    }

    suspend fun eliminar(o: Oportunidad) = withContext(Dispatchers.IO) {
        oportunidadDao.eliminar(o.id) // los seguimientos se borran en cascada
        o.fotoRuta?.let { File(it).delete() }
        _oportunidades.value = oportunidadDao.listar()
    }

    /** Registra un seguimiento y actualiza la fecha del próximo seguimiento de la oportunidad. */
    suspend fun agregarSeguimiento(s: Seguimiento) = withContext(Dispatchers.IO) {
        val ahora = Fechas.marcaTiempo()
        seguimientoDao.insertar(s.copy(registradoEn = ahora))
        oportunidadDao.obtener(s.oportunidadId)?.let { o ->
            val estadoSugerido = if (o.estado in Catalogos.ESTADOS_SIN_CONTACTO && s.tipoAccion == "Primer contacto") {
                "Contacto realizado"
            } else o.estado
            if (estadoSugerido != o.estado) {
                seguimientoDao.insertar(
                    Seguimiento(
                        oportunidadId = o.id, tipoAccion = Catalogos.ACCION_CAMBIO_ESTADO,
                        fecha = Fechas.aIso(LocalDate.now()), resultado = "${o.estado} → $estadoSugerido",
                        proximaAccion = "", fechaProximo = null, responsable = s.responsable, registradoEn = ahora
                    )
                )
            }
            oportunidadDao.actualizar(
                o.copy(
                    estado = estadoSugerido,
                    propuestaEnviada = o.propuestaEnviada || s.tipoAccion == "Envío de propuesta",
                    proximoSeguimiento = s.fechaProximo,
                    actualizado = ahora
                )
            )
        }
        _oportunidades.value = oportunidadDao.listar()
    }

    private fun registrarAccion(id: Long, tipo: String, resultado: String, responsable: String, ahora: String) {
        seguimientoDao.insertar(
            Seguimiento(
                oportunidadId = id, tipoAccion = tipo, fecha = Fechas.aIso(LocalDate.now()),
                resultado = resultado, proximaAccion = "", fechaProximo = null,
                responsable = responsable, registradoEn = ahora
            )
        )
    }
}
