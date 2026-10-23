package mx.familiasquesuman.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mx.familiasquesuman.app.ui.nav.AppNavHost
import mx.familiasquesuman.app.ui.theme.FamiliasQueSumanTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import mx.familiasquesuman.app.push.AvisosPush

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo al abrir desde cero, no al rotar la pantalla
        if (savedInstanceState == null) recibirEnlace(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            val esPantallaAncha =
                windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            FamiliasQueSumanApp(esPantallaAncha)
        }
    }

    /** La app ya estaba abierta y se tocó una notificación push. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibirEnlace(intent)
    }

    private fun recibirEnlace(intent: Intent?) {
        val ruta = AvisosPush.rutaDesde(intent?.extras) ?: return
        (application as FqsApplication).enlacePendiente.value = ruta
    }
}

@Composable
fun FamiliasQueSumanApp(esPantallaAncha: Boolean) {
    FamiliasQueSumanTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost(esPantallaAncha = esPantallaAncha)
        }
    }
}
