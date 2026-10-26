package cl.duoc.gestionoportunidades.ui.lista

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.data.model.Catalogos
import cl.duoc.gestionoportunidades.data.model.Fechas
import cl.duoc.gestionoportunidades.data.model.Oportunidad
import cl.duoc.gestionoportunidades.ui.components.CampoSelector
import cl.duoc.gestionoportunidades.ui.components.EtiquetaEstado
import cl.duoc.gestionoportunidades.ui.components.EtiquetaPrioridad

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListaScreen(
    alVolver: () -> Unit,
    alNueva: () -> Unit,
    alAbrir: (Long) -> Unit,
    vm: ListaViewModel = viewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val f = estado.filtros
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oportunidades") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { mostrarFiltros = !mostrarFiltros }) {
                        Icon(Icons.Filled.Settings, contentDescription = if (mostrarFiltros) "Ocultar filtros" else "Mostrar filtros")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = alNueva) { Icon(Icons.Filled.Add, contentDescription = "Nueva oportunidad") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = f.texto,
                onValueChange = vm::onTexto,
                label = { Text("Buscar por nombre, organización, comuna o responsable") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (f.texto.isNotEmpty()) {
                        IconButton(onClick = { vm.onTexto("") }) { Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda") }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // Filtros rápidos por estado
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = f.soloProximos, onClick = vm::onSoloProximos,
                        label = { Text("Próximos 30 días") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                items(Catalogos.ESTADOS) { e ->
                    FilterChip(selected = f.estado == e, onClick = { vm.onEstado(e) }, label = { Text(e) })
                }
            }

            // Panel de filtros avanzados (se expande con animación)
            AnimatedVisibility(
                visible = mostrarFiltros,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Catalogos.PRIORIDADES.forEach { p ->
                            FilterChip(selected = f.prioridad == p, onClick = { vm.onPrioridad(p) }, label = { Text("Prioridad $p") })
                        }
                    }
                    CampoSelector("Tipo", f.tipo.orEmpty(), listOf("Todos") + Catalogos.TIPOS,
                        { vm.onTipo(it.takeIf { v -> v != "Todos" }) }, Icons.Filled.Info, null)
                    CampoSelector("Región", f.region.orEmpty(), listOf("Todas") + Catalogos.REGIONES,
                        { vm.onRegion(it.takeIf { v -> v != "Todas" }) }, Icons.Filled.LocationOn, null)
                    Text("Ordenar por", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Orden.entries.forEach { o ->
                            FilterChip(selected = f.orden == o, onClick = { vm.onOrden(o) }, label = { Text(o.etiqueta) })
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${estado.resultados.size} de ${estado.total} oportunidades",
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f)
                )
                if (f.activos > 0) TextButton(onClick = vm::limpiar) { Text("Limpiar filtros (${f.activos})") }
            }

            if (!estado.cargando && estado.resultados.isEmpty()) {
                EstadoVacio(hayDatos = estado.total > 0)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.resultados, key = { it.id }) { o ->
                        TarjetaOportunidad(o, onClick = { alAbrir(o.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOportunidad(o: Oportunidad, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(o.organizacion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                EtiquetaPrioridad(o.prioridad)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${Fechas.isoAUi(o.fechaEvento)}  ·  ", style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${o.comuna}, ${o.region}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${o.responsable}  ·  ${o.tipo}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            EtiquetaEstado(o.estado)
        }
    }
}

@Composable
private fun EstadoVacio(hayDatos: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (hayDatos) "Ninguna oportunidad coincide con la búsqueda o los filtros." else "Aún no hay oportunidades registradas.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (hayDatos) "Prueba con otro texto o limpia los filtros." else "Usa el botón + para registrar la primera.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
