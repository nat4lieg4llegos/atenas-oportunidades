package cl.duoc.gestionoportunidades.ui.inicio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.ui.components.TarjetaIndicador
import cl.duoc.gestionoportunidades.ui.theme.EstadoAlerta
import cl.duoc.gestionoportunidades.ui.theme.EstadoExito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    alVerLista: () -> Unit,
    alNueva: () -> Unit,
    alAbrir: (Long) -> Unit,
    alCerrarSesion: () -> Unit,
    vm: InicioViewModel = viewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agencia Atenas") },
                actions = {
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = alNueva,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva oportunidad") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Hola, ${estado.usuario?.nombre ?: "usuario"}", style = MaterialTheme.typography.titleLarge)
                            Text(
                                listOfNotNull(estado.usuario?.perfil, estado.usuario?.zona).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TarjetaIndicador("Oportunidades", estado.total, Icons.AutoMirrored.Filled.List, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    TarjetaIndicador("Eventos en 30 días", estado.proximos, Icons.Filled.DateRange, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TarjetaIndicador("Seguimientos atrasados", estado.atrasados, Icons.Filled.Warning, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    TarjetaIndicador("Confirmadas", estado.confirmadas, Icons.Filled.CheckCircle, EstadoExito, Modifier.weight(1f))
                }
            }
            item {
                OutlinedButton(onClick = alVerLista, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Consultar y filtrar oportunidades")
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Alertas de gestión", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (!estado.cargando && estado.alertas.isEmpty()) {
                item {
                    Text(
                        "No hay alertas: todas las gestiones están al día.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(estado.alertas, key = { "${it.oportunidadId}-${it.tipo}" }) { alerta ->
                // Cada alerta entra deslizándose desde la derecha.
                val entrada = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = entrada,
                    enter = fadeIn() + slideInHorizontally { it / 4 },
                    modifier = Modifier.animateItem()
                ) {
                    TarjetaAlerta(alerta, onClick = { alAbrir(alerta.oportunidadId) })
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tus oportunidades quedan guardadas en el dispositivo.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; vm.cerrarSesion(); alCerrarSesion() }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaAlerta(alerta: Alerta, onClick: () -> Unit) {
    val (icono, color, etiqueta) = when (alerta.tipo) {
        TipoAlerta.ATRASADO -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, "Atrasado")
        TipoAlerta.PROXIMO -> Triple(Icons.Filled.DateRange, MaterialTheme.colorScheme.secondary, "Próximo")
        TipoAlerta.SIN_CONTACTO -> Triple(Icons.Filled.Phone, EstadoAlerta, "Sin contacto")
    }
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = etiqueta, tint = color)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(alerta.titulo, style = MaterialTheme.typography.titleMedium)
                Text(alerta.detalle, style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
    }
}
