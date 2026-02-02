package host.senk.dosenk.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.data.local.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_login) { // Usamos el layout de login de fondo para que no se vea feo el parpadeo

    @Inject lateinit var userPreferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Decidimos el destino inmediatamente
        lifecycleScope.launch {
            val isLoggedIn = userPreferences.isUserLoggedIn.first()
            val isSetupFinished = userPreferences.isSetupFinished.first()

            if (isLoggedIn) {
                if (isSetupFinished) {
                    // iniciado al home
                    findNavController().navigate(R.id.action_splash_to_home)
                } else {
                    // Registrado pero no terminó al Wizard
                    findNavController().navigate(R.id.action_splash_to_wizard)
                }
            } else {
                // Nuevo al Login
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }
    }
}