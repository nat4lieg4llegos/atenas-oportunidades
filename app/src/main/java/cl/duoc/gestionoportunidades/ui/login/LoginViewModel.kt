package cl.duoc.gestionoportunidades.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.gestionoportunidades.di.AppContainer
import cl.duoc.gestionoportunidades.validacion.CampoLogin
import cl.duoc.gestionoportunidades.validacion.ValidadorLogin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val mostrarContrasena: Boolean = false,
    val errores: Map<CampoLogin, String> = emptyMap(),
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    val intentosFallidos: Int = 0,
    val autenticado: Boolean = false
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = AppContainer.auth(app)

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun onUsuario(valor: String) = _estado.update {
        it.copy(usuario = valor, errores = it.errores - CampoLogin.USUARIO, errorGeneral = null)
    }

    fun onContrasena(valor: String) = _estado.update {
        it.copy(contrasena = valor, errores = it.errores - CampoLogin.CONTRASENA, errorGeneral = null)
    }

    fun alternarVisibilidad() = _estado.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }

    fun ingresar() {
        val actual = _estado.value
        val resultado = ValidadorLogin.validar(actual.usuario, actual.contrasena)
        if (!resultado.esValido) {
            _estado.update { it.copy(errores = resultado.errores, intentosFallidos = it.intentosFallidos + 1) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, errorGeneral = null) }
            delay(600) // simula la latencia de un servicio para mostrar el indicador de carga
            val usuario = auth.iniciarSesion(actual.usuario, actual.contrasena)
            _estado.update {
                if (usuario != null) {
                    it.copy(cargando = false, autenticado = true)
                } else {
                    it.copy(
                        cargando = false,
                        errorGeneral = "Usuario o contraseña incorrectos",
                        intentosFallidos = it.intentosFallidos + 1
                    )
                }
            }
        }
    }
}
