package mx.familiasquesuman.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.familiasquesuman.app.FqsApplication
import mx.familiasquesuman.app.MainActivity
import mx.familiasquesuman.app.R
import mx.familiasquesuman.app.ui.nav.Routes

/**
 * Notificaciones push (RF16). El backend las manda por Firebase con el canal
 * fqs_avisos. Con la app cerrada, Android las muestra solo y al tocarlas abre
 * MainActivity con los datos como extras. Con la app abierta llegan a
 * onMessageReceived y se muestran aquí con el mismo formato.
 */
object AvisosPush {
    const val CANAL = "fqs_avisos"

    /** Se llama al iniciar la app. Crear un canal que ya existe no hace nada. */
    fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Avisos de Familias que Suman", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Recordatorios de tus actividades, nuevas actividades de tus favoritas y respuestas a tus propuestas"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    /**
     * Pantalla que abre una notificación según sus datos. Solo se aceptan
     * identificadores con forma de UUID, así un mensaje manipulado no puede armar
     * otra ruta de navegación.
     */
    fun rutaDesde(datos: Bundle?): String? {
        if (datos == null) return null
        val uuid = Regex("^[0-9a-fA-F-]{36}$")
        fun id(clave: String) = datos.getString(clave)?.takeIf { uuid.matches(it) }
        return id("actividadId")?.let { Routes.actividadDetalle(it) }
            ?: id("campanaId")?.let { Routes.campanaDonacion(it) }
            ?: id("propuestaId")?.let { Routes.estadoPropuesta(it) }
            ?: id("asociacionId")?.let { Routes.asociacionDetalle(it) }
            ?: if (datos.getString("notificacionId") != null) Routes.NOTIFICACIONES else null
    }
}

class FqsMessagingService : FirebaseMessagingService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Firebase cambió el token de este teléfono, se avisa al backend si hay sesión. */
    override fun onNewToken(token: String) {
        val repo = (application as FqsApplication).repositorio
        alcance.launch { repo.registrarDispositivo(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val aviso = message.notification ?: return
        val extras = Bundle().apply { message.data.forEach { (k, v) -> putString(k, v) } }
        val abrir = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtras(extras)
        }
        val pendiente = PendingIntent.getActivity(
            this,
            message.messageId?.hashCode() ?: 0,
            abrir,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(this, AvisosPush.CANAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(aviso.title)
            .setContentText(aviso.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.body))
            .setAutoCancel(true)
            .setContentIntent(pendiente)
            .build()
        val gestor = NotificationManagerCompat.from(this)
        // Sin el permiso de notificaciones (Android 13 o más) no se muestra nada
        if (gestor.areNotificationsEnabled()) {
            runCatching { gestor.notify(message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(), notificacion) }
        }
    }
}
