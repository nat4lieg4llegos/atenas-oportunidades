package cl.duoc.gestionoportunidades.nativo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Recibe la alarma programada y publica la notificación de recordatorio. */
class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Recordatorios.mostrar(
            context,
            oportunidadId = intent.getLongExtra(Recordatorios.EXTRA_ID, 0L),
            titulo = intent.getStringExtra(Recordatorios.EXTRA_TITULO) ?: "Recordatorio de seguimiento",
            texto = intent.getStringExtra(Recordatorios.EXTRA_TEXTO) ?: "Tienes una gestión pendiente."
        )
    }
}
