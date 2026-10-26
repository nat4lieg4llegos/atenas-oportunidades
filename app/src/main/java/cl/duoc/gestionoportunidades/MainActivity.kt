package cl.duoc.gestionoportunidades

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duoc.gestionoportunidades.di.AppContainer
import cl.duoc.gestionoportunidades.nativo.Recordatorios
import cl.duoc.gestionoportunidades.ui.navigation.AppNavigation
import cl.duoc.gestionoportunidades.ui.theme.GestionOportunidadesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Recordatorios.crearCanal(this)

        val haySesion = AppContainer.auth(this).usuarioActual() != null
        // Si la app se abrió desde una notificación, llega el id de la oportunidad.
        val oportunidadDesdeNotificacion = intent.getLongExtra(Recordatorios.EXTRA_ID, 0L)
            .takeIf { it > 0 && savedInstanceState == null }

        setContent {
            GestionOportunidadesTheme {
                AppNavigation(haySesion = haySesion, oportunidadInicial = oportunidadDesdeNotificacion)
            }
        }
    }
}
