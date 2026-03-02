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
class SplashFragment : Fragment(R.layout.fragment_login) {

    @Inject lateinit var userPreferences: UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val isLoggedIn = userPreferences.isUserLoggedIn.first()
            val currentStage = userPreferences.setupFinished.first()

            if (isLoggedIn) {
                when (currentStage) {
                    0 -> findNavController().navigate(R.id.action_splash_to_wizard) // No ha hecho Time Painting
                    1 -> findNavController().navigate(R.id.action_splash_to_stats) // No ha obtenido su nivel
                    2 ->findNavController().navigate(R.id.action_splashFragment_to_MissionFragment) ///nO HA VIVIDO EL bloqueo
                    3 ->findNavController().navigate(R.id.action_splashFragment_to_TutoConclusion)

                    else -> findNavController().navigate(R.id.action_splash_to_home)
                }
            } else {
                // Nuevo al Login
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }
    }
}