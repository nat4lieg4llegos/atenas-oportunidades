package cl.duoc.gestionoportunidades.ui.seguimiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.di.AppContainer
import cl.duoc.gestionoportunidades.nativo.Recordatorios
import cl.duoc.gestionoportunidades.validacion.CampoSeguimiento
import cl.duoc.gestionoportunidades.validacion.SeguimientoFormulario
import cl.duoc.gestionoportunidades.validacion.ValidadorSeguimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SeguimientoFormState(
    val oportunidad: Oportunidad? = null,
    val form: SeguimientoFormulario = SeguimientoFormulario(fecha = Fechas.aUi(LocalDate.now())),
    val errores: Map<CampoSeguimiento, String> = emptyMap(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val intentosFallidos: Int = 0,
    val mensajeGuardado: String? = null
)

class SeguimientoFormViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppContainer.oportunidades(app)
    private val auth = AppContainer.auth(app)

    private val _estado = MutableStateFlow(SeguimientoFormState())
    val estado: StateFlow<SeguimientoFormState> = _estado.asStateFlow()
    private var validarEnVivo = false

    fun cargar(oportunidadId: Long) {
        if (_estado.value.oportunidad?.id == oportunidadId) return
        viewModelScope.launch { _estado.update { it.copy(oportunidad = repo.obtener(oportunidadId)) } }
    }

    fun onCambio(campo: CampoSeguimiento, valor: String) {
        val f = _estado.value.form
        val nuevo = when (campo) {
            CampoSeguimiento.TIPO_ACCION -> f.copy(tipoAccion = valor)
            CampoSeguimiento.FECHA -> f.copy(fecha = valor)
            CampoSeguimiento.RESULTADO -> f.copy(resultado = valor)
            CampoSeguimiento.PROXIMA_ACCION -> f.copy(proximaAccion = valor)
            CampoSeguimiento.FECHA_PROXIMO -> f.copy(fechaProximo = valor)
        }
        _estado.update {
            it.copy(form = nuevo, errores = if (validarEnVivo) ValidadorSeguimiento.validar(nuevo).errores else it.errores - campo)
        }
    }

    fun onRecordatorio(v: Boolean) = _estado.update { it.copy(form = it.form.copy(crearRecordatorio = v)) }

    /** Valida antes de pedir permisos: la pantalla solo pide el permiso si el formulario es válido. */
    fun validar(): Boolean {
        val r = ValidadorSeguimiento.validar(_estado.value.form)
        if (!r.esValido) {
            validarEnVivo = true
            _estado.update { it.copy(errores = r.errores, intentosFallidos = it.intentosFallidos + 1) }
        }
        return r.esValido
    }

    fun guardar() {
        val actual = _estado.value
        val o = actual.oportunidad ?: return
        if (actual.guardando || !validar()) return
        val f = actual.form
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val fechaProximo = Fechas.desdeUi(f.fechaProximo)
            repo.agregarSeguimiento(
                Seguimiento(
                    oportunidadId = o.id,
                    tipoAccion = f.tipoAccion,
                    fecha = Fechas.uiAIso(f.fecha).orEmpty(),
                    resultado = f.resultado.trim(),
                    proximaAccion = f.proximaAccion.trim(),
                    fechaProximo = fechaProximo?.let(Fechas::aIso),
                    responsable = auth.usuarioActual()?.nombre ?: o.responsable
                )
            )
            val app = getApplication<Application>()
            val mensaje = when {
                fechaProximo == null || !f.crearRecordatorio -> "Seguimiento registrado"
                Recordatorios.tienePermiso(app) -> {
                    Recordatorios.crearCanal(app)
                    Recordatorios.programarParaFecha(
                        app, o.id, fechaProximo,
                        titulo = "${f.proximaAccion.trim()}: ${o.nombre}",
                        texto = "Hoy corresponde el seguimiento con ${o.organizacion}."
                    )
                    "Seguimiento registrado. Recordatorio el ${Fechas.aUi(fechaProximo)} a las 09:00"
                }
                else -> "Seguimiento registrado (sin permiso de notificaciones, se mostrará en Alertas)"
            }
            _estado.update { it.copy(guardando = false, guardado = true, mensajeGuardado = mensaje) }
        }
    }
}
