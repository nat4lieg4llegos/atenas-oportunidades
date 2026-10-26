package cl.duoc.gestionoportunidades.ui.detalle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.di.AppContainer
import cl.duoc.gestionoportunidades.nativo.Recordatorios
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleUiState(
    val oportunidad: Oportunidad? = null,
    val seguimientos: List<Seguimiento> = emptyList(),
    val cargando: Boolean = true,
    val eliminada: Boolean = false,
    val mensaje: String? = null
)

class DetalleViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppContainer.oportunidades(app)
    private val auth = AppContainer.auth(app)

    private val _estado = MutableStateFlow(DetalleUiState())
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()
    private var observacion: Job? = null

    /** Observa el repositorio: si se edita la oportunidad o se agrega un seguimiento, la ficha se actualiza sola. */
    fun cargar(id: Long) {
        if (observacion != null) return
        observacion = viewModelScope.launch {
            repo.oportunidades.collect { lista ->
                if (_estado.value.eliminada) return@collect
                val o = lista.firstOrNull { it.id == id } ?: repo.obtener(id)
                val historial = if (o != null) repo.seguimientos(id) else emptyList()
                _estado.update { it.copy(oportunidad = o, seguimientos = historial, cargando = false) }
            }
        }
    }

    fun cambiarEstado(nuevo: String) {
        val o = _estado.value.oportunidad ?: return
        viewModelScope.launch {
            repo.cambiarEstado(o, nuevo, responsable = auth.usuarioActual()?.nombre ?: o.responsable)
            _estado.update { it.copy(mensaje = "Estado actualizado a \"$nuevo\"") }
        }
    }

    fun eliminar() {
        val o = _estado.value.oportunidad ?: return
        viewModelScope.launch {
            _estado.update { it.copy(eliminada = true) }
            repo.eliminar(o)
        }
    }

    /** Programa una notificación de prueba en 10 segundos (para demostrar el recurso nativo). */
    fun probarRecordatorio() {
        val o = _estado.value.oportunidad ?: return
        val app = getApplication<Application>()
        if (!Recordatorios.tienePermiso(app)) {
            permisoNotificacionDenegado()
            return
        }
        Recordatorios.crearCanal(app)
        Recordatorios.programarPrueba(
            app, o.id,
            titulo = "Seguimiento: ${o.nombre}",
            texto = "Recuerda contactar a ${o.organizacion}. Estado actual: ${o.estado}."
        )
        _estado.update { it.copy(mensaje = "Recordatorio programado: llegará en 10 segundos") }
    }

    fun permisoNotificacionDenegado() = _estado.update {
        it.copy(mensaje = "Sin permiso de notificaciones: los recordatorios se verán solo en el panel de alertas")
    }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }
}
