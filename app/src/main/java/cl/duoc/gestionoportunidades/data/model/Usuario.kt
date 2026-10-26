package cl.duoc.gestionoportunidades.data.model

/** Usuario ficticio de la aplicación (el caso prohíbe credenciales reales). */
data class Usuario(
    val usuario: String,
    val nombre: String,
    val perfil: String,
    val zona: String
)
