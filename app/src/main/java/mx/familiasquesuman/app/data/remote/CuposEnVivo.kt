package mx.familiasquesuman.app.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mx.familiasquesuman.app.data.CambioCupo
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/**
 * Cupos en tiempo real (RF-05). Mantiene abierto el flujo Server-Sent Events del
 * backend mientras la app está en primer plano y publica cada cambio de cupo.
 *
 * Usa el mismo cliente HTTP de la app, así el token se agrega solo y, si venció, el
 * autenticador lo renueva antes de reconectar. El backend cierra cada flujo a los 15
 * minutos a propósito, por eso un cierre normal se reconecta enseguida. Si falla la
 * red se reintenta con espera creciente hasta un minuto.
 */
class CuposEnVivo(clienteBase: OkHttpClient, private val url: String) {

    private val _cambios = MutableSharedFlow<CambioCupo>(extraBufferCapacity = 64)
    val cambios: SharedFlow<CambioCupo> = _cambios.asSharedFlow()

    // El backend manda un latido cada 25 segundos, si en 70 no llega nada la conexión murió
    private val fabrica = EventSources.createFactory(
        clienteBase.newBuilder().readTimeout(70, TimeUnit.SECONDS).build()
    )
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val candado = Any()
    private var fuente: EventSource? = null
    private var reintento: Job? = null
    private var activo = false
    private var fallos = 0

    /** Abre el flujo si no estaba abierto. */
    fun iniciar() = synchronized(candado) {
        if (activo) return@synchronized
        activo = true
        fallos = 0
        conectar()
    }

    /** Cierra el flujo, por ejemplo al pasar a segundo plano o al cerrar sesión. */
    fun detener() = synchronized(candado) {
        activo = false
        reintento?.cancel()
        reintento = null
        fuente?.cancel()
        fuente = null
    }

    private fun conectar() {
        val peticion = Request.Builder().url(url).header("Accept", "text/event-stream").build()
        fuente = fabrica.newEventSource(peticion, oyente)
    }

    private fun reprogramar(esperaMs: Long) = synchronized(candado) {
        if (!activo) return@synchronized
        fuente = null
        reintento?.cancel()
        reintento = alcance.launch {
            delay(esperaMs)
            synchronized(candado) { if (activo && fuente == null) conectar() }
        }
    }

    private fun esperaPorFallo(): Long {
        fallos = (fallos + 1).coerceAtMost(6)
        // 2, 4, 8, 16, 32 y 60 segundos
        return (1000L shl fallos).coerceAtMost(60_000L)
    }

    private val oyente = object : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            fallos = 0
        }

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            if (type != "cupos") return
            val evento = runCatching { jsonFqs.decodeFromString(CuposEventoDto.serializer(), data) }.getOrNull() ?: return
            _cambios.tryEmit(CambioCupo(evento.actividadId, evento.cupoTotal, evento.cuposDisponibles))
        }

        override fun onClosed(eventSource: EventSource) {
            // Cierre normal del servidor, se vuelve a conectar con un token vigente
            reprogramar(1_000L)
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            when (response?.code) {
                // Sin sesión válida, el autenticador ya avisó la expiración
                401, 403 -> detener()
                else -> reprogramar(esperaPorFallo())
            }
        }
    }
}
