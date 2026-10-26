package cl.duoc.gestionoportunidades.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.gestionoportunidades.ui.theme.EstadoAlerta
import cl.duoc.gestionoportunidades.ui.theme.EstadoExito
import cl.duoc.gestionoportunidades.ui.theme.EstadoNeutro

/** Etiqueta de estado: siempre combina color + ícono + texto (accesible, no depende solo del color). */
@Composable
fun EtiquetaEstado(estado: String, modifier: Modifier = Modifier) {
    val (color, icono) = estiloEstado(estado)
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(estado, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun estiloEstado(estado: String): Pair<Color, ImageVector> = when (estado) {
    "Participación confirmada", "Activación realizada" -> EstadoExito to Icons.Filled.CheckCircle
    "Oportunidad descartada", "Activación cancelada" -> MaterialTheme.colorScheme.error to Icons.Filled.Close
    "Propuesta enviada", "En espera de respuesta" -> MaterialTheme.colorScheme.secondary to Icons.AutoMirrored.Filled.Send
    "Contacto pendiente" -> EstadoAlerta to Icons.Filled.Warning
    "Contacto realizado" -> MaterialTheme.colorScheme.primary to Icons.Filled.Refresh
    else -> EstadoNeutro to Icons.Filled.Info
}

/** Etiqueta de prioridad con estrella; la prioridad alta usa el color terciario de la marca. */
@Composable
fun EtiquetaPrioridad(prioridad: String) {
    val color = when (prioridad) {
        "Alta" -> MaterialTheme.colorScheme.tertiary
        "Media" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text("Prioridad $prioridad", style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(start = 2.dp))
    }
}

/** Indicador numérico del panel; el número se anima al cambiar. */
@Composable
fun TarjetaIndicador(titulo: String, valor: Int, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val animado by animateIntAsState(targetValue = valor, animationSpec = tween(600), label = "indicador")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, contentDescription = null, tint = color)
            Text(animado.toString(), style = MaterialTheme.typography.headlineMedium, color = color)
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Título de sección dentro de formularios y fichas. */
@Composable
fun TituloSeccion(texto: String, icono: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(Modifier.padding(top = 6.dp))
    }
}

/** Fila "etiqueta: valor" de la ficha de detalle. Oculta la fila si no hay valor. */
@Composable
fun FilaDato(etiqueta: String, valor: String?) {
    if (valor.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.58f))
    }
}
