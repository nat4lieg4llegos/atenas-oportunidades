package cl.duoc.gestionoportunidades.validacion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidadorOportunidadTest {
    private val hoy = LocalDate.of(2026, 10, 26)

    private val valido = OportunidadFormulario(
        nombre = "Feria de beneficios laborales",
        organizacion = "Empresa Horizonte Ficticia",
        tipo = "Feria empresarial",
        fuente = "Sitio web",
        fechaDeteccion = "20/10/2026",
        fechaEvento = "15/11/2026",
        horario = "09:00 - 17:00",
        comuna = "Puente Alto",
        region = "Metropolitana",
        publicoEsperado = "250",
        responsable = "Ejecutiva Demo",
        datosFicticios = true
    )

    @Test
    fun formularioCompleto_esValido() {
        val r = ValidadorOportunidad.validar(valido, hoy)
        assertTrue(r.errores.toString(), r.esValido)
    }

    @Test
    fun formularioVacio_marcaCadaCampoObligatorio() {
        val r = ValidadorOportunidad.validar(OportunidadFormulario(prioridad = "", potencial = "", estado = ""), hoy)
        listOf(
            CampoOportunidad.NOMBRE, CampoOportunidad.ORGANIZACION, CampoOportunidad.TIPO, CampoOportunidad.FUENTE,
            CampoOportunidad.FECHA_DETECCION, CampoOportunidad.FECHA_EVENTO, CampoOportunidad.COMUNA,
            CampoOportunidad.REGION, CampoOportunidad.PUBLICO_ESPERADO, CampoOportunidad.PRIORIDAD,
            CampoOportunidad.POTENCIAL, CampoOportunidad.ESTADO, CampoOportunidad.RESPONSABLE,
            CampoOportunidad.DATOS_FICTICIOS
        ).forEach { assertTrue("Falta error en $it", r.error(it) != null) }
    }

    @Test
    fun fechaImposible_esRechazada() {
        val r = ValidadorOportunidad.validar(valido.copy(fechaEvento = "31/02/2027"), hoy)
        assertEquals("Fecha inválida. Usa DD/MM/AAAA", r.error(CampoOportunidad.FECHA_EVENTO))
    }

    @Test
    fun eventoAnteriorADeteccion_esRechazado() {
        val r = ValidadorOportunidad.validar(valido.copy(fechaEvento = "19/10/2026"), hoy)
        assertEquals("El evento no puede ser anterior a su detección", r.error(CampoOportunidad.FECHA_EVENTO))
    }

    @Test
    fun deteccionFutura_esRechazada() {
        val r = ValidadorOportunidad.validar(valido.copy(fechaDeteccion = "27/10/2026"), hoy)
        assertFalse(r.esValido)
        assertTrue(r.error(CampoOportunidad.FECHA_DETECCION) != null)
    }

    @Test
    fun publicoNoNumericoONegativo_tieneMensajeEspecifico() {
        assertEquals(
            "Debe ser un número entero, sin puntos",
            ValidadorOportunidad.validar(valido.copy(publicoEsperado = "abc"), hoy).error(CampoOportunidad.PUBLICO_ESPERADO)
        )
        assertEquals(
            "Debe ser mayor que 0",
            ValidadorOportunidad.validar(valido.copy(publicoEsperado = "-5"), hoy).error(CampoOportunidad.PUBLICO_ESPERADO)
        )
    }

    @Test
    fun horarioInvertido_esRechazado() {
        val r = ValidadorOportunidad.validar(valido.copy(horario = "18:00 - 09:00"), hoy)
        assertTrue(r.error(CampoOportunidad.HORARIO) != null)
    }

    @Test
    fun contactoIncompletoOInvalido_esRechazado() {
        val sinMedio = ValidadorOportunidad.validar(valido.copy(contactoNombre = "Alex Demo", contactoArea = "Bienestar"), hoy)
        assertEquals("Ingresa al menos un correo o teléfono", sinMedio.error(CampoOportunidad.CONTACTO_CORREO))

        val malo = ValidadorOportunidad.validar(
            valido.copy(contactoNombre = "Alex Demo", contactoArea = "Bienestar", contactoCorreo = "alex@", contactoTelefono = "1234"),
            hoy
        )
        assertTrue(malo.error(CampoOportunidad.CONTACTO_CORREO) != null)
        assertTrue(malo.error(CampoOportunidad.CONTACTO_TELEFONO) != null)

        val bueno = ValidadorOportunidad.validar(
            valido.copy(contactoNombre = "Alex Demo", contactoArea = "Bienestar", contactoCorreo = "alex.demo@ejemplo.cl", contactoTelefono = "+56 9 1234 5678"),
            hoy
        )
        assertTrue(bueno.errores.toString(), bueno.esValido)
    }

    @Test
    fun estadoConContacto_exigeNombreDeContacto() {
        val r = ValidadorOportunidad.validar(valido.copy(estado = "Propuesta enviada"), hoy)
        assertEquals("Este estado requiere registrar el contacto", r.error(CampoOportunidad.CONTACTO_NOMBRE))
    }

    @Test
    fun costoNegativo_esRechazado() {
        val r = ValidadorOportunidad.validar(valido.copy(costo = "-1000"), hoy)
        assertEquals("El costo no puede ser negativo", r.error(CampoOportunidad.COSTO))
        assertNull(ValidadorOportunidad.validar(valido.copy(costo = "150000"), hoy).error(CampoOportunidad.COSTO))
    }
}
