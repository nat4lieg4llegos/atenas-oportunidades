package cl.duoc.gestionoportunidades.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.gestionoportunidades.data.model.Fechas
import java.time.Instant
import java.time.ZoneOffset

/**
 * Mensaje de apoyo bajo cada campo: muestra el error (con ícono de advertencia) o, si no hay error,
 * una ayuda opcional. Así todos los campos tienen la misma retroalimentación visual.
 */
@Composable
private fun TextoApoyo(error: String?, ayuda: String?) {
    when {
        error != null -> Text(error, color = MaterialTheme.colorScheme.error)
        ayuda != null -> Text(ayuda, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IconoEstadoCampo(error: String?, valido: Boolean) {
    when {
        error != null -> Icon(Icons.Filled.Warning, contentDescription = "Error: $error", tint = MaterialTheme.colorScheme.error)
        valido -> Icon(Icons.Filled.CheckCircle, contentDescription = "Campo válido", tint = MaterialTheme.colorScheme.secondary)
    }
}

/** Campo de texto con ícono descriptivo, ícono de error/válido y mensaje por campo. */
@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    icono: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    obligatorio: Boolean = false,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    multilinea: Boolean = false,
    transformacion: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(if (obligatorio) "$etiqueta *" else etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = {
            if (iconoFinal != null) iconoFinal() else IconoEstadoCampo(error, valido = obligatorio && valor.isNotBlank())
        },
        isError = error != null,
        supportingText = if (error != null || ayuda != null) {
            { TextoApoyo(error, ayuda) }
        } else null,
        singleLine = !multilinea,
        minLines = if (multilinea) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        visualTransformation = transformacion,
        modifier = modifier.fillMaxWidth()
    )
}

/** Selector desplegable (lista cerrada de valores del caso). */
@Composable
fun CampoSelector(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    alSeleccionar: (String) -> Unit,
    icono: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    obligatorio: Boolean = false
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (obligatorio) "$etiqueta *" else etiqueta) },
            placeholder = { Text("Selecciona una opción") },
            leadingIcon = { Icon(icono, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (error != null) IconoEstadoCampo(error, false)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Abrir opciones")
                }
            },
            isError = error != null,
            supportingText = if (error != null) {
                { TextoApoyo(error, null) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente que abre el menú al tocar cualquier parte del campo.
        Box(
            Modifier
                .matchParentSize()
                .semantics { contentDescription = "$etiqueta: ${valor.ifBlank { "sin seleccionar" }}" }
                .clickable { abierto = true }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = { alSeleccionar(opcion); abierto = false },
                    trailingIcon = if (opcion == valor) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = "Seleccionado") }
                    } else null
                )
            }
        }
    }
}

/** Campo de fecha DD/MM/AAAA: se puede escribir o elegir desde el calendario Material 3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    obligatorio: Boolean = false
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    CampoTexto(
        etiqueta = etiqueta,
        valor = valor,
        alCambiar = alCambiar,
        icono = Icons.Filled.DateRange,
        error = error,
        modifier = modifier,
        obligatorio = obligatorio,
        ayuda = "Formato DD/MM/AAAA",
        teclado = KeyboardType.Number,
        iconoFinal = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (error != null) IconoEstadoCampo(error, false)
                IconButton(onClick = { mostrarCalendario = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Abrir calendario")
                }
            }
        }
    )
    if (mostrarCalendario) {
        val inicial = Fechas.desdeUi(valor)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val estado = rememberDatePickerState(initialSelectedDateMillis = inicial)
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        alCambiar(Fechas.aUi(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Grupo de chips de selección única (p. ej. prioridad). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SelectorChips(
    titulo: String,
    opciones: List<String>,
    seleccion: String,
    alSeleccionar: (String) -> Unit,
    error: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = opcion == seleccion,
                    onClick = { alSeleccionar(opcion) },
                    label = { Text(opcion) },
                    leadingIcon = if (opcion == seleccion) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null
                )
            }
        }
        MensajeError(error)
    }
}

/** Casilla de verificación con mensaje de error animado. */
@Composable
fun CampoCasilla(texto: String, marcado: Boolean, alCambiar: (Boolean) -> Unit, error: String? = null) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { alCambiar(!marcado) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = marcado, onCheckedChange = alCambiar)
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
        MensajeError(error)
    }
}

/** Mensaje de error con ícono que aparece/desaparece con animación. */
@Composable
fun MensajeError(error: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 2.dp)) {
            Icon(
                Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                error.orEmpty(), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
