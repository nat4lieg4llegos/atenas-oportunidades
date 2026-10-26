package cl.duoc.gestionoportunidades.validacion

import cl.duoc.gestionoportunidades.data.model.Catalogos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadorSeguimientoYLoginTest {
    private val hoy = LocalDate.of(2026, 10, 26)

    @Test
    fun seguimientoValido_conProximaAccion() {
        val r = ValidadorSeguimiento.validar(
            SeguimientoFormulario("Primer contacto", "26/10/2026", "Se envió correo de presentación", "Llamada", "30/10/2026"),
            hoy
        )
        assertTrue(r.errores.toString(), r.esValido)
    }

    @Test
    fun proximaAccionSinFecha_yFechaPasada_sonRechazadas() {
        val sinFecha = ValidadorSeguimiento.validar(
            SeguimientoFormulario("Llamada", "26/10/2026", "Sin respuesta", "Reintentar", ""), hoy
        )
        assertEquals("Indica cuándo se hará la próxima acción", sinFecha.error(CampoSeguimiento.FECHA_PROXIMO))

        val pasada = ValidadorSeguimiento.validar(
            SeguimientoFormulario("Llamada", "20/10/2026", "Sin respuesta", "Reintentar", "21/10/2026"), hoy
        )
        assertEquals("Debe ser hoy o una fecha futura", pasada.error(CampoSeguimiento.FECHA_PROXIMO))
    }

    @Test
    fun accionFutura_esRechazada() {
        val r = ValidadorSeguimiento.validar(SeguimientoFormulario("Reunión", "01/11/2026", "Reunión agendada"), hoy)
        assertTrue(r.error(CampoSeguimiento.FECHA) != null)
    }

    @Test
    fun login_validaFormato() {
        val vacio = ValidadorLogin.validar("", "")
        assertEquals("Ingresa tu usuario", vacio.error(CampoLogin.USUARIO))
        assertEquals("Ingresa tu contraseña", vacio.error(CampoLogin.CONTRASENA))
        assertEquals("La contraseña debe tener al menos 6 caracteres", ValidadorLogin.validar("ejecutivo", "123").error(CampoLogin.CONTRASENA))
        assertTrue(ValidadorLogin.validar("ejecutivo", "Atenas2026").esValido)
    }

    @Test
    fun region_seAsociaDesdeTextoDelGeocodificador() {
        assertEquals("Metropolitana", Catalogos.regionDesdeTexto("Región Metropolitana de Santiago"))
        assertEquals("Valparaíso", Catalogos.regionDesdeTexto("Valparaiso"))
        assertEquals("O'Higgins", Catalogos.regionDesdeTexto("Región del Libertador Gral. Bernardo O’Higgins"))
        assertEquals(null, Catalogos.regionDesdeTexto(""))
    }
}
