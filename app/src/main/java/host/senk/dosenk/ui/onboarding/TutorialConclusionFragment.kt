package host.senk.dosenk.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.service.BlockerEngineService
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TutorialConclusionFragment : Fragment(R.layout.fragment_tutorial_conclusion) {

    private val viewModel: TutorialMissionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvFinalCountdown = view.findViewById<TextView>(R.id.tvFinalCountdown)
        val tvDogMessage = view.findViewById<TextView>(R.id.tvDogMessage)
        val btnFinalAction = view.findViewById<TextView>(R.id.btnFinalAction)

        val layoutTimerWait = view.findViewById<View>(R.id.layoutTimerWait)
        val layoutThemeSelection = view.findViewById<View>(R.id.layoutThemeSelection)

        // ¡AQUÍ NOS CONECTAMOS AL RELOJ DEL SERVICIO!
        viewLifecycleOwner.lifecycleScope.launch {
            BlockerEngineService.timeLeftFlow.collect { secondsLeft ->
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                tvFinalCountdown.text = String.format("00:%02d:%02d", minutes, seconds)

                // ¿SE ACABÓ EL TIEMPO?
                if (secondsLeft <= 0) {
                    // Cambiamos al Estado 2 (Temas)
                    layoutTimerWait.visibility = View.GONE
                    layoutThemeSelection.visibility = View.VISIBLE

                    tvDogMessage.text = "¡SOBREVIVISTE!\nEspero que hayas aprendido la lección.\nAhora sí, escoge tu color favorito y te libero."

                    btnFinalAction.text = "¡Ir a mi Dashboard!"
                    btnFinalAction.setTextColor(requireContext().getColor(R.color.black)) // O usa el color primario

                    // Activamos el botón para graduarse
                    btnFinalAction.setOnClickListener {
                        viewModel.finishOnboarding {
                            // TODO: Mandarlo al HOME REAL (El Dashboard chingón)
                            // findNavController().navigate(R.id.action_tutorialConclusion_to_realHome)
                        }
                    }
                }
            }
        }
    }
}