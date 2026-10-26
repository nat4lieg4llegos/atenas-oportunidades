package cl.duoc.gestionoportunidades.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = AtenasAzul,
    onPrimary = Color.White,
    primaryContainer = AtenasAzulContenedor,
    onPrimaryContainer = Color(0xFF001C38),
    secondary = AtenasTurquesa,
    onSecondary = Color.White,
    secondaryContainer = AtenasTurquesaContenedor,
    onSecondaryContainer = Color(0xFF002020),
    tertiary = AtenasCoral,
    onTertiary = Color.White,
    tertiaryContainer = AtenasCoralContenedor,
    onTertiaryContainer = Color(0xFF3B0A00),
    background = FondoClaro,
    surface = Color.White
)

private val EsquemaOscuro = darkColorScheme(
    primary = AtenasAzulClaro,
    onPrimary = Color(0xFF00315B),
    primaryContainer = AtenasAzulOscuroContenedor,
    onPrimaryContainer = AtenasAzulContenedor,
    secondary = AtenasTurquesaClaro,
    onSecondary = Color(0xFF003737),
    secondaryContainer = AtenasTurquesaOscuroContenedor,
    onSecondaryContainer = AtenasTurquesaContenedor,
    tertiary = AtenasCoralClaro,
    onTertiary = Color(0xFF5E1604),
    tertiaryContainer = AtenasCoralOscuroContenedor,
    onTertiaryContainer = AtenasCoralContenedor,
    background = FondoOscuro,
    surface = SuperficieOscura
)

/**
 * Tema Material 3 de la app. El color dinámico (Android 12+) se desactiva a propósito
 * para que la identidad visual de Agencia Atenas sea la misma en todos los dispositivos.
 */
@Composable
fun GestionOportunidadesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}
