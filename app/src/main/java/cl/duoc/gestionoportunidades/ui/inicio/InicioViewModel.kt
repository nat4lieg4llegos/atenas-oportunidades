package cl.duoc.gestionoportunidades.ui.inicio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Usuario
import cl.duoc.gestionoportunidades.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class TipoAlerta { ATRASADO, PROXIMO, SIN_CONTACTO }

data class Alerta(val oportunidadId: Long, val tipo: TipoAlerta, val titulo: String, val detalle: String)

data class InicioUiState(
    val usuario: Usuario? = null,
    val total: Int = 0,
    val proximos: Int = 0,
    val atrasados: Int = 0,
    val confirmadas: Int = 0,
    val alertas: List<Alerta> = emptyList(),
    val cargando: Boolean = true
)

class InicioViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppContainer.oportunidades(app)
    private val auth = AppContainer.auth(app)

    val estado: StateFlow<InicioUiState> = repo.oportunidades
        .map { lista -> construirEstado(lista, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState(usuario = auth.usuarioActual()))

    init {
        viewModelScope.launch { repo.refrescar() }
    }

    fun cerrarSesion() = auth.cerrarSesion()

    private fun construirEstado(lista: List<Oportunidad>, hoy: LocalDate): InicioUiState {
        val alertas = buildList {
            lista.filter { it.seguimientoAtrasado(hoy) }.forEach {
                val dias = ChronoUnit.DAYS.between(it.proximoSeguimientoLocal ?: hoy, hoy)
                add(Alerta(it.id, TipoAlerta.ATRASADO, it.nombre, "Seguimiento atrasado hace $dias día(s)"))
            }
            lista.filter { it.eventoProximo(hoy, dias = 7) }.forEach {
                val dias = ChronoUnit.DAYS.between(hoy, it.fechaEventoLocal ?: hoy)
                val cuando = if (dias == 0L) "hoy" else "en $dias día(s)"
                add(Alerta(it.id, TipoAlerta.PROXIMO, it.nombre, "Evento $cuando · ${Fechas.isoAUi(it.fechaEvento)}"))
            }
            lista.filter { it.sinContacto && it.eventoProximo(hoy, dias = 30) }.forEach {
                add(Alerta(it.id, TipoAlerta.SIN_CONTACTO, it.nombre, "Sin contacto y el evento es pronto: ${it.estado}"))
            }
        }.distinctBy { it.oportunidadId to it.tipo }

        return InicioUiState(
            usuario = auth.usuarioActual(),
            total = lista.size,
            proximos = lista.count { it.eventoProximo(hoy, dias = 30) },
            atrasados = lista.count { it.seguimientoAtrasado(hoy) },
            confirmadas = lista.count { it.estado == "Participación confirmada" },
            alertas = alertas,
            cargando = false
        )
    }
}
