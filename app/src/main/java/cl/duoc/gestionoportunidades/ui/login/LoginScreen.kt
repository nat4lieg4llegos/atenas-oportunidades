package cl.duoc.gestionoportunidades.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.gestionoportunidades.ui.components.CampoTexto
import cl.duoc.gestionoportunidades.validacion.CampoLogin

@Composable
fun LoginScreen(alIngresar: () -> Unit, vm: LoginViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.autenticado) { if (estado.autenticado) alIngresar() }

    // Animación de "sacudida" del formulario cada vez que falla un intento.
    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(estado.intentosFallidos) {
        if (estado.intentosFallidos > 0) {
            for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) {
                sacudida.animateTo(x, spring(stiffness = Spring.StiffnessHigh))
            }
        }
    }
    // Entrada animada de la tarjeta al abrir la pantalla.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("A", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Text("Agencia Atenas", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Gestión móvil de oportunidades y activaciones corporativas",
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))

                AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { it / 3 }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationX = sacudida.value },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)
                            CampoTexto(
                                etiqueta = "Usuario",
                                valor = estado.usuario,
                                alCambiar = vm::onUsuario,
                                icono = Icons.Filled.Person,
                                error = estado.errores[CampoLogin.USUARIO],
                                obligatorio = true
                            )
                            CampoTexto(
                                etiqueta = "Contraseña",
                                valor = estado.contrasena,
                                alCambiar = vm::onContrasena,
                                icono = Icons.Filled.Lock,
                                error = estado.errores[CampoLogin.CONTRASENA],
                                obligatorio = true,
                                teclado = KeyboardType.Password,
                                transformacion = if (estado.mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                                iconoFinal = {
                                    TextButton(onClick = vm::alternarVisibilidad) {
                                        Text(if (estado.mostrarContrasena) "Ocultar" else "Ver")
                                    }
                                }
                            )
                            AnimatedVisibility(visible = estado.errorGeneral != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text(
                                        estado.errorGeneral.orEmpty(), color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = vm::ingresar,
                                enabled = !estado.cargando,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (estado.cargando) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Ingresar", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        "Cuentas ficticias de prueba: ejecutivo / Atenas2026 · supervisor / Atenas2026",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
