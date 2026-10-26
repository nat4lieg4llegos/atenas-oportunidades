package cl.duoc.gestionoportunidades.ui.formulario

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.nativo.FotoHelper
import cl.duoc.gestionoportunidades.ui.components.CampoCasilla
import cl.duoc.gestionoportunidades.ui.components.CampoFecha
import cl.duoc.gestionoportunidades.ui.components.CampoSelector
import cl.duoc.gestionoportunidades.ui.components.CampoTexto
import cl.duoc.gestionoportunidades.ui.components.SelectorChips
import cl.duoc.gestionoportunidades.ui.components.TituloSeccion
import cl.duoc.gestionoportunidades.validacion.CampoOportunidad as C

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OportunidadFormScreen(
    id: Long,
    alVolver: () -> Unit,
    alGuardar: (Long) -> Unit,
    vm: OportunidadFormViewModel = viewModel()
) {
    LaunchedEffect(id) { vm.cargar(id) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val f = estado.form
    val err = estado.errores
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val snackbar = remember { SnackbarHostState() }
    var confirmarDescarte by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.guardadoId) { estado.guardadoId?.let(alGuardar) }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() }
    }
    // Tras un intento fallido, sube al inicio donde está el resumen de errores.
    LaunchedEffect(estado.intentosFallidos) { if (estado.intentosFallidos > 0) scroll.animateScrollTo(0) }

    val volver: () -> Unit = {
        if (estado.hayCambios) {
            confirmarDescarte = true
        } else {
            alVolver()
        }
    }
    BackHandler(onBack = volver)

    // Permiso de ubicación: se pide en contexto, solo cuando el usuario toca el botón.
    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) vm.usarUbicacion() else vm.permisoUbicacionDenegado()
    }
    // Cámara: se usa la app de cámara del sistema y se recibe una vista previa (Bitmap).
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) vm.onFotoTomada(bitmap) else vm.mostrarMensaje("No se tomó ninguna foto")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar oportunidad" else "Nueva oportunidad") },
                navigationIcon = {
                    IconButton(onClick = volver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResumenErrores(cantidad = err.size)
            Text(
                "Los campos con * son obligatorios. Usa solo datos ficticios o autorizados.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TituloSeccion("Oportunidad", Icons.Filled.Star)
            CampoTexto("Nombre de la oportunidad", f.nombre, { vm.onCambio(C.NOMBRE, it) }, Icons.Filled.Create, err[C.NOMBRE], obligatorio = true)
            CampoTexto("Organización responsable", f.organizacion, { vm.onCambio(C.ORGANIZACION, it) }, Icons.Filled.Home, err[C.ORGANIZACION], obligatorio = true)
            CampoSelector("Tipo de oportunidad", f.tipo, Catalogos.TIPOS, { vm.onCambio(C.TIPO, it) }, Icons.AutoMirrored.Filled.List, err[C.TIPO], obligatorio = true)
            CampoSelector("Fuente de información", f.fuente, Catalogos.FUENTES, { vm.onCambio(C.FUENTE, it) }, Icons.Filled.Search, err[C.FUENTE], obligatorio = true)
            CampoFecha("Fecha de detección", f.fechaDeteccion, { vm.onCambio(C.FECHA_DETECCION, it) }, err[C.FECHA_DETECCION], obligatorio = true)
            CampoTexto(
                "Descripción", f.descripcion, { vm.onCambio(C.DESCRIPCION, it) }, Icons.Filled.Info, err[C.DESCRIPCION],
                multilinea = true, ayuda = "${f.descripcion.length}/300"
            )

            TituloSeccion("Evento", Icons.Filled.Place)
            CampoFecha("Fecha del evento", f.fechaEvento, { vm.onCambio(C.FECHA_EVENTO, it) }, err[C.FECHA_EVENTO], obligatorio = true)
            CampoTexto(
                "Horario", f.horario, { vm.onCambio(C.HORARIO, it) }, Icons.Filled.Info, err[C.HORARIO],
                ayuda = "Ejemplo: 09:00 - 17:00"
            )
            CampoTexto("Comuna", f.comuna, { vm.onCambio(C.COMUNA, it) }, Icons.Filled.LocationOn, err[C.COMUNA], obligatorio = true)
            CampoSelector("Región", f.region, Catalogos.REGIONES, { vm.onCambio(C.REGION, it) }, Icons.Filled.Place, err[C.REGION], obligatorio = true)
            BotonUbicacion(
                buscando = estado.buscandoUbicacion,
                mensaje = estado.mensajeUbicacion,
                alPulsar = {
                    val concedido = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                    if (concedido) vm.usarUbicacion()
                    else permisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }
            )
            CampoTexto("Público objetivo", f.publicoObjetivo, { vm.onCambio(C.PUBLICO_OBJETIVO, it) }, Icons.Filled.Face, err[C.PUBLICO_OBJETIVO],
                ayuda = "Ejemplo: trabajadores y sus familias")
            CampoTexto("Público esperado (personas)", f.publicoEsperado, { vm.onCambio(C.PUBLICO_ESPERADO, it) }, Icons.Filled.Person,
                err[C.PUBLICO_ESPERADO], obligatorio = true, teclado = KeyboardType.Number)

            TituloSeccion("Gestión comercial", Icons.Filled.ThumbUp)
            SelectorChips("Prioridad *", Catalogos.PRIORIDADES, f.prioridad, { vm.onCambio(C.PRIORIDAD, it) }, err[C.PRIORIDAD])
            CampoSelector("Potencial comercial", f.potencial, Catalogos.POTENCIALES, { vm.onCambio(C.POTENCIAL, it) }, Icons.Filled.Star, err[C.POTENCIAL], obligatorio = true)
            CampoSelector("Estado de la gestión", f.estado, Catalogos.ESTADOS, { vm.onCambio(C.ESTADO, it) }, Icons.Filled.Info, err[C.ESTADO], obligatorio = true)
            CampoTexto("Responsable interno", f.responsable, { vm.onCambio(C.RESPONSABLE, it) }, Icons.Filled.AccountBox, err[C.RESPONSABLE], obligatorio = true)
            CampoCasilla("Propuesta enviada", f.propuestaEnviada, vm::onPropuestaEnviada)

            TituloSeccion("Contacto corporativo (ficticio)", Icons.Filled.Person)
            CampoTexto("Nombre del contacto", f.contactoNombre, { vm.onCambio(C.CONTACTO_NOMBRE, it) }, Icons.Filled.Person, err[C.CONTACTO_NOMBRE])
            CampoTexto("Cargo o función", f.contactoCargo, { vm.onCambio(C.CONTACTO_CARGO, it) }, Icons.Filled.AccountBox, err[C.CONTACTO_CARGO])
            CampoSelector("Área del contacto", f.contactoArea, Catalogos.AREAS_CONTACTO, { vm.onCambio(C.CONTACTO_AREA, it) }, Icons.Filled.Home, err[C.CONTACTO_AREA])
            CampoTexto("Correo ficticio", f.contactoCorreo, { vm.onCambio(C.CONTACTO_CORREO, it) }, Icons.Filled.Email, err[C.CONTACTO_CORREO],
                teclado = KeyboardType.Email, ayuda = "Ejemplo: nombre@empresa-ficticia.cl")
            CampoTexto("Teléfono ficticio", f.contactoTelefono, { vm.onCambio(C.CONTACTO_TELEFONO, it) }, Icons.Filled.Phone, err[C.CONTACTO_TELEFONO],
                teclado = KeyboardType.Phone, ayuda = "Formato: +56 9 1234 5678")

            TituloSeccion("Condiciones y observaciones", Icons.Filled.Build)
            CampoTexto("Condiciones de participación", f.condiciones, { vm.onCambio(C.CONDICIONES, it) }, Icons.Filled.Info, err[C.CONDICIONES], multilinea = true)
            CampoTexto("Costo referencial ficticio (CLP)", f.costo, { vm.onCambio(C.COSTO, it) }, Icons.Filled.Info, err[C.COSTO],
                teclado = KeyboardType.Number, ayuda = "Monto en pesos, sin puntos")
            CampoTexto("Observaciones", f.observaciones, { vm.onCambio(C.OBSERVACIONES, it) }, Icons.Filled.Create, err[C.OBSERVACIONES], multilinea = true)

            TituloSeccion("Material del evento", Icons.Filled.Face)
            SeccionFoto(
                ruta = estado.fotoRuta,
                alTomar = {
                    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                        vm.mostrarMensaje("Este dispositivo no tiene cámara")
                    } else {
                        try {
                            camara.launch(null)
                        } catch (e: ActivityNotFoundException) {
                            vm.mostrarMensaje("No hay una app de cámara disponible")
                        }
                    }
                },
                alQuitar = vm::quitarFoto
            )

            CampoCasilla(
                "Confirmo que los datos ingresados son ficticios o autorizados *",
                f.datosFicticios, vm::onDatosFicticios, err[C.DATOS_FICTICIOS]
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = vm::guardar,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (estado.esEdicion) "Guardar cambios" else "Registrar oportunidad")
                }
            }
            OutlinedButton(onClick = volver, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmarDescarte) {
        AlertDialog(
            onDismissRequest = { confirmarDescarte = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Los datos que ingresaste no se guardarán.") },
            confirmButton = { TextButton(onClick = { confirmarDescarte = false; alVolver() }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarDescarte = false }) { Text("Seguir editando") } }
        )
    }
}

/** Tarjeta que resume cuántos campos tienen error; aparece y desaparece con animación. */
@Composable
private fun ResumenErrores(cantidad: Int) {
    AnimatedVisibility(visible = cantidad > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    if (cantidad == 1) "Hay 1 campo por corregir" else "Hay $cantidad campos por corregir",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun BotonUbicacion(buscando: Boolean, mensaje: String?, alPulsar: () -> Unit) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        OutlinedButton(onClick = alPulsar, enabled = !buscando, modifier = Modifier.fillMaxWidth()) {
            if (buscando) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text("Completar comuna con mi ubicación (opcional)")
        }
        Text(
            mensaje ?: "Se lee la ubicación una sola vez y no se guardan coordenadas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

@Composable
private fun SeccionFoto(ruta: String?, alTomar: () -> Unit, alQuitar: () -> Unit) {
    val imagen by produceState<Bitmap?>(initialValue = null, ruta) { value = FotoHelper.cargar(ruta) }
    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        imagen?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Foto del material del evento",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = alTomar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Face, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (ruta == null) "Tomar foto" else "Cambiar foto")
            }
            if (ruta != null) {
                OutlinedButton(onClick = alQuitar) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar foto")
                }
            }
        }
        Text(
            "Fotografía el afiche, stand o material del evento. No fotografíes personas.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
