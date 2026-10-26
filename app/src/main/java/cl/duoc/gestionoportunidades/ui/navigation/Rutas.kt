package cl.duoc.gestionoportunidades.ui.navigation

/** Rutas de navegación de la app. Los ids viajan como argumentos de la ruta. */
object Rutas {
    const val LOGIN = "login"
    const val INICIO = "inicio"
    const val LISTA = "lista"
    const val FORMULARIO = "formulario/{id}"
    const val DETALLE = "detalle/{id}"
    const val SEGUIMIENTO = "seguimiento/{id}"
    const val ARG_ID = "id"
    const val CLAVE_MENSAJE = "mensaje"

    fun formulario(id: Long = 0L) = "formulario/$id"
    fun detalle(id: Long) = "detalle/$id"
    fun seguimiento(id: Long) = "seguimiento/$id"
}
