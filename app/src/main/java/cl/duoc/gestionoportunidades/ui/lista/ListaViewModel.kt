package cl.duoc.gestionoportunidades.ui.lista

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Orden(val etiqueta: String) { FECHA("Fecha del evento"), PRIORIDAD("Prioridad"), RECIENTES("Recién actualizadas") }

data class Filtros(
    val texto: String = "",
    val estado: String? = null,
    val prioridad: String? = null,
    val tipo: String? = null,
    val region: String? = null,
    val soloProximos: Boolean = false,
    val orden: Orden = Orden.FECHA
) {
    val activos: Int get() = listOf(estado, prioridad, tipo, region).count { it != null } + (if (soloProximos) 1 else 0)
}

data class ListaUiState(
    val filtros: Filtros = Filtros(),
    val resultados: List<Oportunidad> = emptyList(),
    val total: Int = 0,
    val cargando: Boolean = true
)

/** Búsqueda y filtros del caso: por nombre/organización/comuna/responsable, estado, prioridad, tipo, región y fecha. */
class ListaViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppContainer.oportunidades(app)
    private val filtros = MutableStateFlow(Filtros())

    val estado: StateFlow<ListaUiState> = combine(repo.oportunidades, filtros) { lista, f ->
        ListaUiState(filtros = f, resultados = aplicar(lista, f, LocalDate.now()), total = lista.size, cargando = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaUiState())

    init {
        viewModelScope.launch { repo.refrescar() }
    }

    fun onTexto(v: String) = filtros.update { it.copy(texto = v) }
    fun onEstado(v: String?) = filtros.update { it.copy(estado = if (it.estado == v) null else v) }
    fun onPrioridad(v: String?) = filtros.update { it.copy(prioridad = if (it.prioridad == v) null else v) }
    fun onTipo(v: String?) = filtros.update { it.copy(tipo = v) }
    fun onRegion(v: String?) = filtros.update { it.copy(region = v) }
    fun onSoloProximos() = filtros.update { it.copy(soloProximos = !it.soloProximos) }
    fun onOrden(o: Orden) = filtros.update { it.copy(orden = o) }
    fun limpiar() = filtros.update { Filtros(texto = it.texto, orden = it.orden) }

    companion object {
        /** Función pura (sin Android) para poder razonar y probar la lógica de filtrado. */
        fun aplicar(lista: List<Oportunidad>, f: Filtros, hoy: LocalDate): List<Oportunidad> {
            val q = f.texto.trim()
            val filtrada = lista.filter { o ->
                (q.isEmpty() || listOf(o.nombre, o.organizacion, o.comuna, o.responsable, o.contactoNombre)
                    .any { it.contains(q, ignoreCase = true) }) &&
                    (f.estado == null || o.estado == f.estado) &&
                    (f.prioridad == null || o.prioridad == f.prioridad) &&
                    (f.tipo == null || o.tipo == f.tipo) &&
                    (f.region == null || o.region == f.region) &&
                    (!f.soloProximos || o.eventoProximo(hoy, dias = 30))
            }
            val pesoPrioridad = mapOf("Alta" to 0, "Media" to 1, "Baja" to 2)
            return when (f.orden) {
                Orden.FECHA -> filtrada.sortedBy { it.fechaEvento }
                Orden.PRIORIDAD -> filtrada.sortedWith(compareBy({ pesoPrioridad[it.prioridad] ?: 3 }, { it.fechaEvento }))
                Orden.RECIENTES -> filtrada.sortedByDescending { it.id }
            }
        }
    }
}
