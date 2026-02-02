package host.senk.dosenk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.data.local.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // LEER TEMA ANTES DE INFLAR LA VISTA
        val themeIndex = runBlocking { userPreferences.themeIndex.first() }

        //  APLICAR EL TEMA GLOBALMENTE
        setTheme(getThemeStyle(themeIndex))

        setContentView(R.layout.activity_main)
        ///escondemos las  barras
        hideSystemUI()
    }


    private fun getThemeStyle(index: Int): Int {
        return when (index) {
            0 -> R.style.Theme_DoSenk_Purple
            1 -> R.style.Theme_DoSenk_Red
            2 -> R.style.Theme_DoSenk_Dark
            3 -> R.style.Theme_DoSenk_Teal
            else -> R.style.Theme_DoSenk_Purple
        }
    }

    // --- LÓGICA INMERSIVA (Nueva) ---
    private fun hideSystemUI() {
        // Le decimos a la ventana que nosotros controlamos el espacio
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Configuración para que las barras aparezcan al deslizar y se oculten solas
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Ocultamos barra de estado  y navegación
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}