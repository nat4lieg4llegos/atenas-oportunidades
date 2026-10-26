package cl.duoc.gestionoportunidades.ui.formulario

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.di.AppContainer
import cl.duoc.gestionoportunidades.nativo.FotoHelper
import cl.duoc.gestionoportunidades.nativo.UbicacionHelper
import cl.duoc.gestionoportunidades.validacion.CampoOportunidad
import cl.duoc.gestionoportunidades.validacion.OportunidadFormulario
import cl.duoc.gestionoportunidades.validacion.ValidadorOportunidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OportunidadFormState(
    val form: OportunidadFormulario = OportunidadFormulario(),
    val errores: Map<CampoOportunidad, String> = emptyMap(),
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val guardadoId: Long? = null,
    val hayCambios: Boolean = false,
    val intentosFallidos: Int = 0,
    val fotoRuta: String? = null,
    val buscandoUbicacion: Boolean = false,
    val mensajeUbicacion: String? = null,
    val mensaje: String? = null
)

/**
 * Lógica del formulario de oportunidad. La pantalla solo envía eventos (cambios de campo,
 * guardar, ubicación, foto) y dibuja el estado; las reglas viven en [ValidadorOportunidad].
 */
class OportunidadFormViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AppContainer.oportunidades(app)
    private val auth = AppContainer.auth(app)
    private val ubicacion = UbicacionHelper(app)

    private val _estado = MutableStateFlow(OportunidadFormState())
    val estado: StateFlow<OportunidadFormState> = _estado.asStateFlow()

    private var original: Oportunidad? = null
    private var idCargado: Long? = null
    private var validarEnVivo = false

    fun cargar(id: Long) {
        if (idCargado == id) return // evita recargar al rotar la pantalla
        idCargado = id
        if (id == 0L) {
            _estado.value = OportunidadFormState(
                form = OportunidadFormulario(
                    fechaDeteccion = Fechas.aUi(LocalDate.now()),
                    responsable = auth.usuarioActual()?.nombre.orEmpty()
                )
            )
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val o = repo.obtener(id)
            original = o
            _estado.value = if (o == null) {
                OportunidadFormState(mensaje = "No se encontró la oportunidad")
            } else {
                OportunidadFormState(form = o.aFormulario(), esEdicion = true, fotoRuta = o.fotoRuta)
            }
        }
    }

    fun onCambio(campo: CampoOportunidad, valor: String) {
        val f = _estado.value.form
        val nuevo = when (campo) {
            CampoOportunidad.NOMBRE -> f.copy(nombre = valor)
            CampoOportunidad.ORGANIZACION -> f.copy(organizacion = valor)
            CampoOportunidad.TIPO -> f.copy(tipo = valor)
            CampoOportunidad.DESCRIPCION -> f.copy(descripcion = valor)
            CampoOportunidad.FUENTE -> f.copy(fuente = valor)
            CampoOportunidad.FECHA_DETECCION -> f.copy(fechaDeteccion = valor)
            CampoOportunidad.FECHA_EVENTO -> f.copy(fechaEvento = valor)
            CampoOportunidad.HORARIO -> f.copy(horario = valor)
            CampoOportunidad.COMUNA -> f.copy(comuna = valor)
            CampoOportunidad.REGION -> f.copy(region = valor)
            CampoOportunidad.PUBLICO_OBJETIVO -> f.copy(publicoObjetivo = valor)
            CampoOportunidad.PUBLICO_ESPERADO -> f.copy(publicoEsperado = valor.filter { it.isDigit() || it == '-' })
            CampoOportunidad.PRIORIDAD -> f.copy(prioridad = valor)
            CampoOportunidad.POTENCIAL -> f.copy(potencial = valor)
            CampoOportunidad.ESTADO -> f.copy(estado = valor)
            CampoOportunidad.RESPONSABLE -> f.copy(responsable = valor)
            CampoOportunidad.CONTACTO_NOMBRE -> f.copy(contactoNombre = valor)
            CampoOportunidad.CONTACTO_CARGO -> f.copy(contactoCargo = valor)
            CampoOportunidad.CONTACTO_AREA -> f.copy(contactoArea = valor)
            CampoOportunidad.CONTACTO_CORREO -> f.copy(contactoCorreo = valor.trim())
            CampoOportunidad.CONTACTO_TELEFONO -> f.copy(contactoTelefono = valor)
            CampoOportunidad.CONDICIONES -> f.copy(condiciones = valor)
            CampoOportunidad.COSTO -> f.copy(costo = valor.filter { it.isDigit() || it == '-' })
            CampoOportunidad.OBSERVACIONES -> f.copy(observaciones = valor)
            CampoOportunidad.DATOS_FICTICIOS -> f
        }
        actualizarFormulario(nuevo, campo)
    }

    fun onPropuestaEnviada(v: Boolean) = actualizarFormulario(_estado.value.form.copy(propuestaEnviada = v), null)
    fun onDatosFicticios(v: Boolean) =
        actualizarFormulario(_estado.value.form.copy(datosFicticios = v), CampoOportunidad.DATOS_FICTICIOS)

    /**
     * Antes del primer intento de guardar solo se limpia el error del campo editado;
     * después se revalida todo en vivo, para que el usuario vea cómo desaparecen los errores.
     */
    private fun actualizarFormulario(nuevo: OportunidadFormulario, campo: CampoOportunidad?) {
        _estado.update {
            val errores = when {
                validarEnVivo -> ValidadorOportunidad.validar(nuevo).errores
                campo != null -> it.errores - campo
                else -> it.errores
            }
            it.copy(form = nuevo, errores = errores, hayCambios = true)
        }
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando) return
        val resultado = ValidadorOportunidad.validar(actual.form)
        if (!resultado.esValido) {
            validarEnVivo = true
            _estado.update {
                it.copy(
                    errores = resultado.errores,
                    intentosFallidos = it.intentosFallidos + 1,
                    mensaje = "Revisa ${resultado.errores.size} campo(s) marcados en rojo"
                )
            }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val oportunidad = actual.form.aOportunidad(original, actual.fotoRuta)
            val id = repo.guardar(oportunidad, responsable = auth.usuarioActual()?.nombre ?: actual.form.responsable)
            _estado.update { it.copy(guardando = false, guardadoId = id, hayCambios = false) }
        }
    }

    // ---- Recurso nativo: ubicación ----
    fun usarUbicacion() {
        viewModelScope.launch {
            _estado.update { it.copy(buscandoUbicacion = true, mensajeUbicacion = "Buscando ubicación aproximada…") }
            val u = ubicacion.obtenerUbicacion()
            if (u == null) {
                _estado.update {
                    it.copy(
                        buscandoUbicacion = false,
                        mensajeUbicacion = "No se pudo obtener la ubicación (GPS apagado o sin señal). Ingresa la comuna manualmente."
                    )
                }
                return@launch
            }
            val region = Catalogos.regionDesdeTexto(u.regionTexto)
            val f = _estado.value.form
            val nuevo = f.copy(
                comuna = u.comuna ?: f.comuna,
                region = region ?: f.region
            )
            actualizarFormulario(nuevo, CampoOportunidad.COMUNA)
            _estado.update {
                it.copy(
                    buscandoUbicacion = false,
                    errores = if (region != null) it.errores - CampoOportunidad.REGION else it.errores,
                    mensajeUbicacion = if (u.comuna != null) {
                        "Comuna detectada: ${u.comuna}${region?.let { r -> ", región $r" } ?: ""}. Puedes modificarla."
                    } else {
                        "Ubicación obtenida, pero no se identificó la comuna. Ingrésala manualmente."
                    }
                )
            }
        }
    }

    fun permisoUbicacionDenegado() = _estado.update {
        it.copy(mensajeUbicacion = "Permiso denegado. La ubicación es opcional: ingresa la comuna manualmente.")
    }

    // ---- Recurso nativo: cámara ----
    fun onFotoTomada(bitmap: Bitmap) {
        viewModelScope.launch {
            val ruta = FotoHelper.guardar(getApplication(), bitmap)
            if (ruta == null) {
                _estado.update { it.copy(mensaje = "No se pudo guardar la foto") }
                return@launch
            }
            descartarFotoTemporal()
            _estado.update { it.copy(fotoRuta = ruta, hayCambios = true, mensaje = "Foto agregada") }
        }
    }

    fun quitarFoto() {
        descartarFotoTemporal()
        _estado.update { it.copy(fotoRuta = null, hayCambios = true) }
    }

    fun mostrarMensaje(texto: String) = _estado.update { it.copy(mensaje = texto) }
    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    /** Borra la foto nueva si no es la que ya estaba guardada en la base de datos. */
    private fun descartarFotoTemporal() {
        val ruta = _estado.value.fotoRuta
        if (ruta != null && ruta != original?.fotoRuta) FotoHelper.descartar(ruta)
    }

    override fun onCleared() {
        // Si se sale sin guardar, no deja fotos huérfanas en el almacenamiento.
        if (_estado.value.guardadoId == null) descartarFotoTemporal()
        super.onCleared()
    }
}

private fun Oportunidad.aFormulario() = OportunidadFormulario(
    nombre = nombre, organizacion = organizacion, tipo = tipo, descripcion = descripcion, fuente = fuente,
    fechaDeteccion = Fechas.isoAUi(fechaDeteccion), fechaEvento = Fechas.isoAUi(fechaEvento), horario = horario,
    comuna = comuna, region = region, publicoObjetivo = publicoObjetivo, publicoEsperado = publicoEsperado.toString(),
    prioridad = prioridad, potencial = potencial, estado = estado, responsable = responsable,
    contactoNombre = contactoNombre, contactoCargo = contactoCargo, contactoArea = contactoArea,
    contactoCorreo = contactoCorreo, contactoTelefono = contactoTelefono, propuestaEnviada = propuestaEnviada,
    condiciones = condiciones, costo = if (costoReferencial == 0L) "" else costoReferencial.toString(),
    observaciones = observaciones, datosFicticios = true
)

/** Solo se llama con un formulario ya validado, por eso las conversiones no fallan. */
private fun OportunidadFormulario.aOportunidad(original: Oportunidad?, fotoRuta: String?) = Oportunidad(
    id = original?.id ?: 0L,
    nombre = nombre.trim(), organizacion = organizacion.trim(), tipo = tipo, descripcion = descripcion.trim(),
    fuente = fuente, fechaDeteccion = Fechas.uiAIso(fechaDeteccion).orEmpty(), fechaEvento = Fechas.uiAIso(fechaEvento).orEmpty(),
    horario = horario.trim(), comuna = comuna.trim(), region = region, publicoObjetivo = publicoObjetivo.trim(),
    publicoEsperado = publicoEsperado.trim().toInt(), prioridad = prioridad, potencial = potencial, estado = estado,
    responsable = responsable.trim(), contactoNombre = contactoNombre.trim(), contactoCargo = contactoCargo.trim(),
    contactoArea = if (contactoNombre.isBlank()) "" else contactoArea, contactoCorreo = contactoCorreo.trim(),
    contactoTelefono = contactoTelefono.trim(), propuestaEnviada = propuestaEnviada || estado == "Propuesta enviada",
    condiciones = condiciones.trim(), costoReferencial = costo.trim().toLongOrNull() ?: 0L,
    observaciones = observaciones.trim(), fotoRuta = fotoRuta, proximoSeguimiento = original?.proximoSeguimiento
)
