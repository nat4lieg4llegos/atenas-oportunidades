package cl.duoc.gestionoportunidades.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.gestionoportunidades.ui.detalle.DetalleScreen
import cl.duoc.gestionoportunidades.ui.formulario.OportunidadFormScreen
import cl.duoc.gestionoportunidades.ui.inicio.InicioScreen
import cl.duoc.gestionoportunidades.ui.lista.ListaScreen
import cl.duoc.gestionoportunidades.ui.login.LoginScreen
import cl.duoc.gestionoportunidades.ui.seguimiento.SeguimientoFormScreen

private const val DURACION = 320

/**
 * Grafo de navegación: Login → Inicio → Lista → Detalle → (Formulario | Seguimiento).
 * Todas las transiciones son animadas: deslizamiento al avanzar y al volver.
 */
@Composable
fun AppNavigation(haySesion: Boolean, oportunidadInicial: Long?) {
    val nav = rememberNavController()

    // Al tocar una notificación se abre directamente la ficha de esa oportunidad.
    LaunchedEffect(oportunidadInicial) {
        if (haySesion && oportunidadInicial != null && oportunidadInicial > 0) nav.navigate(Rutas.detalle(oportunidadInicial))
    }

    NavHost(
        navController = nav,
        startDestination = if (haySesion) Rutas.INICIO else Rutas.LOGIN,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(DURACION)) + fadeIn(tween(DURACION)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(DURACION)) { it / 4 } + fadeOut(tween(DURACION)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(DURACION)) { it / 4 } + fadeIn(tween(DURACION)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(DURACION)) + fadeOut(tween(DURACION)) }
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(alIngresar = {
                nav.navigate(Rutas.INICIO) { popUpTo(Rutas.LOGIN) { inclusive = true } }
            })
        }
        composable(Rutas.INICIO) {
            InicioScreen(
                alVerLista = { nav.navigate(Rutas.LISTA) },
                alNueva = { nav.navigate(Rutas.formulario()) },
                alAbrir = { nav.navigate(Rutas.detalle(it)) },
                alCerrarSesion = {
                    nav.navigate(Rutas.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
                }
            )
        }
        composable(Rutas.LISTA) {
            ListaScreen(
                alVolver = { nav.popBackStack() },
                alNueva = { nav.navigate(Rutas.formulario()) },
                alAbrir = { nav.navigate(Rutas.detalle(it)) }
            )
        }
        composable(Rutas.FORMULARIO, arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong(Rutas.ARG_ID) ?: 0L
            OportunidadFormScreen(
                id = id,
                alVolver = { nav.popBackStack() },
                alGuardar = { idGuardado ->
                    if (id == 0L) {
                        // Alta nueva: se reemplaza el formulario por la ficha recién creada.
                        nav.navigate(Rutas.detalle(idGuardado)) { popUpTo(Rutas.FORMULARIO) { inclusive = true } }
                        enviarMensaje(nav, "Oportunidad registrada correctamente", alDestinoActual = true)
                    } else {
                        enviarMensaje(nav, "Cambios guardados")
                        nav.popBackStack()
                    }
                }
            )
        }
        composable(Rutas.DETALLE, arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })) { entry ->
            val mensaje by entry.savedStateHandle.getStateFlow<String?>(Rutas.CLAVE_MENSAJE, null).collectAsStateWithLifecycle()
            DetalleScreen(
                id = entry.arguments?.getLong(Rutas.ARG_ID) ?: 0L,
                mensajeExterno = mensaje,
                alConsumirMensaje = { entry.savedStateHandle[Rutas.CLAVE_MENSAJE] = null },
                alVolver = { nav.popBackStack() },
                alEditar = { nav.navigate(Rutas.formulario(it)) },
                alNuevoSeguimiento = { nav.navigate(Rutas.seguimiento(it)) }
            )
        }
        composable(Rutas.SEGUIMIENTO, arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.LongType })) { entry ->
            SeguimientoFormScreen(
                oportunidadId = entry.arguments?.getLong(Rutas.ARG_ID) ?: 0L,
                alVolver = { nav.popBackStack() },
                alGuardar = { mensaje ->
                    enviarMensaje(nav, mensaje)
                    nav.popBackStack()
                }
            )
        }
    }
}

/** Deja un mensaje para que la pantalla de destino lo muestre en un Snackbar. */
private fun enviarMensaje(nav: NavHostController, mensaje: String, alDestinoActual: Boolean = false) {
    val destino = if (alDestinoActual) nav.currentBackStackEntry else nav.previousBackStackEntry
    destino?.savedStateHandle?.set(Rutas.CLAVE_MENSAJE, mensaje)
}
