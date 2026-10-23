package mx.familiasquesuman.app.data.local

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import mx.familiasquesuman.app.FqsApplication
import mx.familiasquesuman.app.data.ResultadoEnvios

/**
 * Manda los envíos pendientes cuando hay red. WorkManager espera a que haya conexión,
 * sobrevive a que se cierre la app o se reinicie el teléfono, y si el servidor no responde
 * vuelve a intentar con espera exponencial (30 segundos, 1 minuto, 2 minutos...).
 */
class SincronizarEnvios(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as FqsApplication).repositorio
        return when (repo.enviarPendientes()) {
            ResultadoEnvios.Listo -> Result.success()
            ResultadoEnvios.Reintentar -> Result.retry()
            // Sin sesión no se puede mandar nada. Los envíos se borran al cerrar sesión
            ResultadoEnvios.SinSesion -> Result.success()
        }
    }

    companion object {
        private const val NOMBRE = "fqs_envios_pendientes"

        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SincronizarEnvios>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // Si ya hay un envío en curso, este se forma detrás y revisa lo que llegó después
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
        }

        fun cancelar(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NOMBRE)
        }
    }
}
