package cl.duoc.gestionoportunidades.ui.detalle

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.data.model.Seguimiento
import cl.duoc.gestionoportunidades.nativo.FotoHelper
import cl.duoc.gestionoportunidades.nativo.Recordatorios
import cl.duoc.gestionoportunidades.ui.components.EtiquetaEstado
import cl.duoc.gestionoportunidades.ui.components.EtiquetaPrioridad
import cl.duoc.gestionoportunidades.ui.components.FilaDato
import cl.duoc.gestionoportunidades.ui.components.TituloSeccion
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    id: Long,
    mensajeExterno: String?,
    alConsumirMensaje: () -> Unit,
    alVolver: () -> Unit,
    alEditar: (Long) -> Unit,
    alNuevoSeguimiento: (Long) -> Unit,
    vm: DetalleViewModel = viewModel()
) {
    LaunchedEffect(id) { vm.cargar(id) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.eliminada) { if (estado.eliminada) alVolver() }
    LaunchedEffect(estado.mensaje) { estado.mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() } }
    // Mensajes que envían otras pantallas al volver (p. ej. "Seguimiento registrado").
    val alcance = rememberCoroutineScope()
    LaunchedEffect(mensajeExterno) {
        mensajeExterno?.let { m ->
            alConsumirMensaje()
            alcance.launch { snackbar.showSnackbar(m) }
        }
    }

    // Permiso de notificaciones (Android 13+), pedido justo cuando se quiere programar un recordatorio.
    val permisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.probarRecordatorio() else vm.permisoNotificacionDenegado()
    }
    val probarRecordatorio = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Recordatorios.tienePermiso(context)) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.probarRecordatorio()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha de oportunidad") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    if (estado.oportunidad != null) {
                        IconButton(onClick = { alEditar(id) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                        IconButton(onClick = { confirmarEliminar = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val o = estado.oportunidad
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            o == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("La oportunidad no existe o fue eliminada.")
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Encabezado(o, alCambiarEstado = vm::cambiarEstado) }
                item { FotoEvento(o.fotoRuta) }
                item {
                    Seccion("Evento", Icons.Filled.Place) {
                        FilaDato("Tipo", o.tipo)
                        FilaDato("Fecha", Fechas.isoAUi(o.fechaEvento))
                        FilaDato("Horario", o.horario)
                        FilaDato("Ubicación", "${o.comuna}, ${o.region}")
                        FilaDato("Público objetivo", o.publicoObjetivo)
                        FilaDato("Público esperado", "${o.publicoEsperado} personas")
                        FilaDato("Descripción", o.descripcion)
                    }
                }
                item {
                    Seccion("Gestión comercial", Icons.Filled.ThumbUp) {
                        FilaDato("Potencial", o.potencial)
                        FilaDato("Responsable", o.responsable)
                        FilaDato("Fuente", o.fuente)
                        FilaDato("Detectada el", Fechas.isoAUi(o.fechaDeteccion))
                        FilaDato("Propuesta enviada", if (o.propuestaEnviada) "Sí" else "No")
                        FilaDato("Costo referencial", if (o.costoReferencial > 0) formatoPesos(o.costoReferencial) else null)
                        FilaDato("Última actualización", o.actualizado)
                    }
                }
                item {
                    Seccion("Contacto ficticio", Icons.Filled.Person) {
                        if (o.contactoNombre.isBlank()) {
                            Text("Sin contacto registrado todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilaDato("Nombre", o.contactoNombre)
                        FilaDato("Cargo", o.contactoCargo)
                        FilaDato("Área", o.contactoArea)
                        FilaDato("Correo", o.contactoCorreo)
                        FilaDato("Teléfono", o.contactoTelefono)
                    }
                }
                if (o.condiciones.isNotBlank() || o.observaciones.isNotBlank()) {
                    item {
                        Seccion("Condiciones y observaciones", Icons.Filled.Build) {
                            FilaDato("Condiciones", o.condiciones)
                            FilaDato("Observaciones", o.observaciones)
                        }
                    }
                }
                item { TarjetaRecordatorio(o, alProbar = probarRecordatorio) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Historial de gestión (${estado.seguimientos.size})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp).weight(1f)
                        )
                    }
                }
                item {
                    Button(onClick = { alNuevoSeguimiento(o.id) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Registrar acción de seguimiento")
                    }
                }
                items(estado.seguimientos, key = { it.id }) { s ->
                    ItemHistorial(s, Modifier.animateItem())
                }
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar oportunidad?") },
            text = { Text("Se eliminarán también su historial y su foto. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmarEliminar = false; vm.eliminar() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Encabezado(o: Oportunidad, alCambiarEstado: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(o.nombre, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(o.organizacion, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EtiquetaEstado(o.estado)
                EtiquetaPrioridad(o.prioridad)
            }
            Box {
                OutlinedButton(onClick = { menu = true }) {
                    Text("Cambiar estado")
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    Catalogos.ESTADOS.forEach { e ->
                        DropdownMenuItem(
                            text = { Text(e) },
                            enabled = e != o.estado,
                            onClick = { menu = false; alCambiarEstado(e) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FotoEvento(ruta: String?) {
    val imagen by produceState<Bitmap?>(initialValue = null, ruta) { value = FotoHelper.cargar(ruta) }
    AnimatedVisibility(visible = imagen != null) {
        imagen?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Foto del material del evento",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun Seccion(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector, contenido: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TituloSeccion(titulo, icono)
            Spacer(Modifier.height(8.dp))
            contenido()
        }
    }
}

@Composable
private fun TarjetaRecordatorio(o: Oportunidad, alProbar: () -> Unit) {
    val hoy = LocalDate.now()
    val atrasado = o.seguimientoAtrasado(hoy)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (atrasado) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (atrasado) Icons.Filled.Warning else Icons.Filled.Notifications, contentDescription = null)
                Text("Recordatorios", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                when {
                    o.proximoSeguimiento == null -> "Sin seguimiento programado. Registra una acción con fecha de próxima acción."
                    atrasado -> "Seguimiento ATRASADO desde el ${Fechas.isoAUi(o.proximoSeguimiento)}."
                    else -> "Próximo seguimiento: ${Fechas.isoAUi(o.proximoSeguimiento)} (aviso a las 09:00)."
                }
            )
            OutlinedButton(onClick = alProbar) {
                Icon(Icons.Filled.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Probar notificación (10 s)")
            }
        }
    }
}

@Composable
private fun ItemHistorial(s: Seguimiento, modifier: Modifier = Modifier) {
    var expandido by remember { mutableStateOf(false) }
    val esCambioEstado = s.tipoAccion == Catalogos.ACCION_CAMBIO_ESTADO
    OutlinedCard(onClick = { expandido = !expandido }, modifier = modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (esCambioEstado) Icons.Filled.Refresh else Icons.Filled.DateRange,
                contentDescription = null,
                tint = if (esCambioEstado) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.tipoAccion, style = MaterialTheme.typography.titleMedium)
                Text("${Fechas.isoAUi(s.fecha)} · ${s.responsable}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.resultado, style = MaterialTheme.typography.bodyMedium, maxLines = if (expandido) Int.MAX_VALUE else 2)
                if (expandido && s.proximaAccion.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            " Próxima acción: ${s.proximaAccion} (${Fechas.isoAUi(s.fechaProximo)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Icon(
                if (expandido) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expandido) "Contraer" else "Expandir"
            )
        }
    }
}

private fun formatoPesos(monto: Long): String =
    "$" + NumberFormat.getIntegerInstance(Locale("es", "CL")).format(monto)
