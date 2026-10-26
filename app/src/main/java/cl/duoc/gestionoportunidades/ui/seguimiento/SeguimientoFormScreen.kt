package cl.duoc.gestionoportunidades.ui.seguimiento

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.nativo.Recordatorios
import cl.duoc.gestionoportunidades.ui.components.CampoCasilla
import cl.duoc.gestionoportunidades.ui.components.CampoFecha
import cl.duoc.gestionoportunidades.ui.components.CampoSelector
import cl.duoc.gestionoportunidades.ui.components.CampoTexto
import cl.duoc.gestionoportunidades.ui.components.TituloSeccion
import cl.duoc.gestionoportunidades.validacion.CampoSeguimiento as C

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoFormScreen(
    oportunidadId: Long,
    alVolver: () -> Unit,
    alGuardar: (String) -> Unit,
    vm: SeguimientoFormViewModel = viewModel()
) {
    LaunchedEffect(oportunidadId) { vm.cargar(oportunidadId) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val f = estado.form
    val err = estado.errores
    val context = LocalContext.current
    val scroll = rememberScrollState()

    LaunchedEffect(estado.guardado) { if (estado.guardado) alGuardar(estado.mensajeGuardado.orEmpty()) }
    LaunchedEffect(estado.intentosFallidos) { if (estado.intentosFallidos > 0) scroll.animateScrollTo(0) }

    // Se guarda igual si el permiso se rechaza: el recordatorio es un apoyo, no un requisito.
    val permisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.guardar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar seguimiento") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            estado.oportunidad?.let {
                Text(it.nombre, style = MaterialTheme.typography.titleLarge)
                Text("${it.organizacion} · Estado: ${it.estado}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            TituloSeccion("Acción realizada", Icons.Filled.Create)
            CampoSelector("Tipo de acción", f.tipoAccion, Catalogos.TIPOS_ACCION, { vm.onCambio(C.TIPO_ACCION, it) },
                Icons.AutoMirrored.Filled.List, err[C.TIPO_ACCION], obligatorio = true)
            CampoFecha("Fecha de la acción", f.fecha, { vm.onCambio(C.FECHA, it) }, err[C.FECHA], obligatorio = true)
            CampoTexto("Resultado", f.resultado, { vm.onCambio(C.RESULTADO, it) }, Icons.Filled.Create, err[C.RESULTADO],
                obligatorio = true, multilinea = true, ayuda = "${f.resultado.length}/300")

            TituloSeccion("Próxima acción", Icons.Filled.Refresh)
            CampoTexto("Próxima acción", f.proximaAccion, { vm.onCambio(C.PROXIMA_ACCION, it) }, Icons.Filled.Refresh,
                err[C.PROXIMA_ACCION], ayuda = "Ejemplo: llamar para confirmar condiciones")
            CampoFecha("Fecha del próximo seguimiento", f.fechaProximo, { vm.onCambio(C.FECHA_PROXIMO, it) }, err[C.FECHA_PROXIMO])
            AnimatedVisibility(visible = f.fechaProximo.isNotBlank()) {
                CampoCasilla("Crear recordatorio (notificación a las 09:00 de ese día)", f.crearRecordatorio, vm::onRecordatorio)
            }

            Button(
                onClick = {
                    val necesitaPermiso = f.crearRecordatorio && f.fechaProximo.isNotBlank() &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Recordatorios.tienePermiso(context)
                    if (necesitaPermiso && vm.validar()) {
                        permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.guardar()
                    }
                },
                enabled = !estado.guardando && estado.oportunidad != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                    Text("  Guardar seguimiento")
                }
            }
        }
    }
}
