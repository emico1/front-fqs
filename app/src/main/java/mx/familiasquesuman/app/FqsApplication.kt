package mx.familiasquesuman.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import mx.familiasquesuman.app.data.Repositorio
import mx.familiasquesuman.app.data.local.DatosLocales
import mx.familiasquesuman.app.data.local.SincronizarEnvios
import mx.familiasquesuman.app.data.remote.ClienteHttp
import mx.familiasquesuman.app.data.sesion.AlmacenSesion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mx.familiasquesuman.app.push.AvisosPush

/**
 * Crea una sola vez el cliente HTTP, el almacén de sesión, los datos guardados para usar
 * sin conexión y el repositorio.
 * Coil usa el mismo cliente, así las imágenes protegidas del backend se descargan
 * con el token de la sesión.
 */
class FqsApplication : Application(), ImageLoaderFactory {

    lateinit var repositorio: Repositorio
        private set

    private lateinit var http: ClienteHttp

    /**
     * Pantalla que pidió abrir una notificación push tocada. MainActivity la llena
     * y la navegación la consume en cuanto hay sesión.
     */
    val enlacePendiente = MutableStateFlow<String?>(null)

    /** Tareas de la app que no dependen de una pantalla (poda de datos guardados al abrir). */
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val sesion = AlmacenSesion(this)
        http = ClienteHttp(
            baseUrl = BuildConfig.API_BASE_URL,
            sesion = sesion,
            // El modelo ayuda a reconocer el teléfono en Mis dispositivos, no identifica a la persona
            agente = "FamiliasQueSuman-Android/${BuildConfig.VERSION_NAME} (${Build.MANUFACTURER} ${Build.MODEL}; Android ${Build.VERSION.RELEASE})"
                .replace(Regex("[^\\x20-\\x7E]"), ""),
            pines = BuildConfig.API_PINS.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
        repositorio = Repositorio(
            http = http,
            sesion = sesion,
            local = DatosLocales(this),
            programarEnvios = { SincronizarEnvios.programar(this) },
            cancelarEnvios = { SincronizarEnvios.cancelar(this) }
        )
        AvisosPush.crearCanal(this)
        alcance.launch { repositorio.alIniciar() }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { http.okHttp }
            .crossfade(true)
            .build()
}
