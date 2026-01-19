package host.senk.dosenk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        // APLICAR EL TEMA GLOBALMENTE
        setTheme(getThemeStyle(themeIndex))


        setContentView(R.layout.activity_main)
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
}