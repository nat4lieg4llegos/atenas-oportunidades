# Atenas Oportunidades — Gestión móvil de oportunidades y activaciones corporativas

Aplicación Android desarrollada para la **Evaluación Parcial 2 de DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC), basada en el caso semestral de **Agencia Atenas**: una agencia de marketing directo que necesita detectar con anticipación eventos corporativos, ferias y actividades municipales donde ofrecer sus convenios, y hacer seguimiento centralizado de cada gestión.

> Todos los datos de la aplicación son **ficticios**. No se usan nombres, correos, teléfonos, RUT ni credenciales reales.

## Integrantes
| Nombre | Rol en el proyecto |
|---|---|
| Natalie Gallegos Andulce | Configuración del proyecto, persistencia SQLite, validaciones, pantallas, recursos nativos e integración |
| Pamela Cocq | Formulario base de registro de oportunidad con validaciones (Guía 11) y apoyo en pruebas |

**Sección:** DSY1105-003D · **Equipo N°:** 5 

## Problema que resuelve
El equipo comercial de la agencia busca oportunidades manualmente en redes sociales y sitios web, y la información queda dispersa entre correos, mensajes y notas personales. Las oportunidades se detectan tarde y no se sabe qué fue contactado, qué propuesta se envió ni qué gestión está atrasada. La app centraliza el registro, la priorización y el seguimiento de cada oportunidad.

## Funcionalidades implementadas
- **Inicio de sesión** con cuentas ficticias, validación por campo y control de sesión (la sesión se mantiene al reabrir la app; se puede cerrar sesión).
- **Panel de inicio** con indicadores animados (total, eventos próximos, seguimientos atrasados y confirmadas) y **alertas de gestión**: seguimientos atrasados, eventos en los próximos 7 días y oportunidades sin contacto.
- **Registro y edición de oportunidades**: datos del evento, organización, tipo, fuente, fechas, horario, comuna y región, público objetivo y esperado, prioridad, potencial, estado, responsable, contacto ficticio, condiciones, costo referencial y observaciones.
- **Validación completa por campo** con ícono de error, mensaje específico e indicación de campo válido. Incluye fechas reales (rechaza 31/02), coherencia entre fechas, horario, correo, teléfono chileno, números positivos y reglas de negocio (por ejemplo, "Propuesta enviada" exige contacto registrado).
- **Consulta, búsqueda y filtros**: por texto (nombre, organización, comuna, responsable), estado, prioridad, tipo, región y eventos próximos, con tres órdenes posibles.
- **Ficha de oportunidad** con cambio de estado, edición, eliminación con confirmación e **historial de gestión**. Los cambios de estado quedan registrados automáticamente.
- **Registro de acciones de seguimiento** (los 12 tipos de acción del caso) con próxima acción y fecha.
- **Persistencia local con SQLite**: los datos se conservan al cerrar la app. Hay dos tablas relacionadas (oportunidades y seguimientos) con eliminación en cascada.

### Recursos nativos
| Recurso | Uso en el flujo | Seguridad y permisos |
|---|---|---|
| **Ubicación** | En el formulario, completa automáticamente comuna y región del evento. | Opcional; el permiso se pide en contexto. Lectura única, sin seguimiento continuo, **no se guardan coordenadas** (restricción del caso). Si se rechaza, se ingresa manualmente. |
| **Cámara** | Fotografía del afiche, stand o material del evento, visible en el formulario y en la ficha. | Usa la app de cámara del sistema (no requiere el permiso CAMERA). La imagen se guarda en el almacenamiento privado de la app y se borra al eliminar la oportunidad. |
| **Notificaciones** | Recordatorio a las 09:00 del día del próximo seguimiento, más un botón "Probar notificación (10 s)" en la ficha. Al tocar la notificación se abre la ficha. | En Android 13+ el permiso se pide al programar un recordatorio. Si se rechaza, la app sigue funcionando y las alertas se ven en el panel. |

### Animaciones
- Transiciones de deslizamiento entre todas las pantallas (ida y vuelta).
- Sacudida del formulario de login ante credenciales inválidas, y entrada animada de la tarjeta.
- Mensajes de error que aparecen y desaparecen con animación; resumen de errores animado y desplazamiento automático al inicio del formulario.
- Indicadores numéricos del panel que se animan al cambiar.
- Alertas que entran deslizándose, ítems de lista que se reordenan con animación al filtrar, panel de filtros desplegable e ítems del historial expandibles.
- Snackbars de confirmación ("Oportunidad registrada", "Estado actualizado", etc.).

## Arquitectura (MVVM)
```
UI (Jetpack Compose + Material 3)
   │  eventos ↓            ↑ StateFlow (estado inmutable)
ViewModel (una por pantalla)  ──→  validacion/ (reglas puras, sin Android, con pruebas unitarias)
   │
Repository (única fuente de datos, emite la lista actualizada tras cada cambio)
   │
DAO  ──→  SQLite (AtenasDbHelper)
```

```
app/src/main/java/cl/duoc/gestionoportunidades/
├── data/
│   ├── local/        AtenasDbHelper, OportunidadDao, SeguimientoDao, SesionManager
│   ├── model/        Oportunidad, Seguimiento, Usuario, Catalogos (valores del caso), Fechas
│   └── repository/   OportunidadRepository, AuthRepository
├── di/               AppContainer (instancias únicas de repositorios)
├── validacion/       ValidadorLogin, ValidadorOportunidad, ValidadorSeguimiento
├── nativo/           UbicacionHelper, FotoHelper, Recordatorios, RecordatorioReceiver
├── ui/
│   ├── components/   Campos con validación visual, etiquetas de estado, indicadores
│   ├── login/ inicio/ lista/ formulario/ detalle/ seguimiento/   (Screen + ViewModel)
│   ├── navigation/   AppNavigation (rutas y transiciones), Rutas
│   └── theme/        Tema Material 3 de marca (claro y oscuro)
└── MainActivity.kt
```

La validación está **desacoplada de la interfaz**: las pantallas solo envían eventos al ViewModel, el ViewModel usa los validadores de `validacion/` y la pantalla dibuja los errores que recibe en el estado. Tras el primer intento de guardar, los errores se revalidan en vivo mientras el usuario corrige.

## Requisitos y ejecución
1. Android Studio (versión reciente, compatible con AGP 9.4) y JDK 17 o superior.
2. Abrir la carpeta del proyecto (la que contiene `settings.gradle.kts`) con **File → Open**.
3. Esperar a que termine **Gradle Sync** (la primera vez requiere internet).
4. Elegir un emulador (API 26 o superior; se recomienda API 33+ con Google Play) o un teléfono con depuración USB.
5. Presionar **Run ▶**.
6. Ingresar con una cuenta ficticia:
   - `ejecutivo` / `Atenas2026`
   - `supervisor` / `Atenas2026`

La primera vez se cargan 3 oportunidades de demostración con fechas relativas a hoy (una con seguimiento atrasado y otra con evento próximo, para ver las alertas).

### Probar los recursos nativos en el emulador
- **Ubicación:** en el emulador, *Extended controls (···) → Location*, fijar un punto (por ejemplo, Puente Alto) y presionar *Set location*. Luego, en el formulario, usar "Completar comuna con mi ubicación".
- **Cámara:** el emulador muestra una escena virtual; tomar la foto y confirmar.
- **Notificaciones:** en una ficha, "Probar notificación (10 s)", aceptar el permiso y esperar el aviso.

## Pruebas unitarias
`app/src/test/.../validacion/` contiene 15 pruebas de las reglas de validación (formularios vacíos, fechas imposibles, coherencia de fechas, números, correo, teléfono, horario, reglas de estado y login).
Para ejecutarlas: clic derecho en la carpeta `test` → **Run 'Tests in ...'**, o bien `./gradlew test`.

## Tecnologías
Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel + StateFlow · SQLite (SQLiteOpenHelper) · AlarmManager + NotificationCompat · LocationManager + Geocoder · ActivityResult (cámara y permisos) · JUnit 4

## Planificación
Tablero Trello: https://trello.com/b/wLAJs7FL/atenas-oportunidades-ep2-dsy1105

## Pendiente para la EFT
Módulo de organizaciones y contactos como entidades propias, resultado de activación, perfiles con permisos distintos (supervisor/marketing), consumo de API REST con Retrofit y microservicios Spring Boot, y APK firmado en modo release.
