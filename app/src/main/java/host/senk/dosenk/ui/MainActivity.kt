package host.senk.dosenk.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.ui.mission.VictoryBottomSheet
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

        // REVISAMOS SI LA APP SE ABRIÓ DESDE CERO POR UNA VICTORIA
        checkVictoryIntent(intent)
    }

    // SI LA APP YA ESTABA ABIERTA EN EL FONDO (MINIMIZADA), ENTRA POR AQUÍ
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) // Actualizamos el intent de la activity
        checkVictoryIntent(intent)
    }

    //  CAZADOR DE VICTORIAS
    private fun checkVictoryIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("MISSION_VICTORY", false) == true) {

            // Limpiamos la trampa para que no se repita si el usuario gira el celular
            intent.removeExtra("MISSION_VICTORY")

            // TODO (Lógica Real):
            // 1. Aquí buscarías en tu MissionDao la misión con status = "active".
            // 2. Cambiarías su status a "completed".
            // 3. Extraerías su 'potentialXp' y su 'multiplier'.
            // 4. Se los pasarías al BottomSheet.

            // Por ahora, lanzamos la coreografía con datos sádicos de prueba:
            val bottomSheet = VictoryBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt("baseXP", 45)
                    putInt("streakXP", 5)
                    putDouble("multiplier", 5.0)
                    putInt("totalXP", 250)
                }
            }

            ///Escupimos el trofeo
            bottomSheet.show(supportFragmentManager, "VictorySheet")
        }
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

    // LÓGICA INMERSIVA
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        insetsController.hide(WindowInsetsCompat.Type.navigationBars())

        insetsController.show(WindowInsetsCompat.Type.statusBars())
    }
}