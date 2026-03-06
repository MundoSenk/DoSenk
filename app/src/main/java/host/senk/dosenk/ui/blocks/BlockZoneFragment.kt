package host.senk.dosenk.ui.blocks

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.ui.mission.CreateMissionViewModel
import host.senk.dosenk.util.applyDoSenkGradient

@AndroidEntryPoint
class BlockZoneFragment : Fragment(R.layout.fragment_block_zone) {

    // La mochila sigue aquí, pero solo la usaremos si estamos en modo "Selección"
    private val viewModel: CreateMissionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. PINTAMOS LOS GRADIENTES
        view.findViewById<View>(R.id.layoutZoneHeader).applyDoSenkGradient()
        view.findViewById<View>(R.id.btnEditCustomBlock).applyDoSenkGradient(cornerRadius = 16f)
        view.findViewById<View>(R.id.bgHumanoGradient).applyDoSenkGradient()
        view.findViewById<View>(R.id.bgDiosGradient).applyDoSenkGradient()

        val btnHumano = view.findViewById<TextView>(R.id.btnChooseHumano)
        val btnDios = view.findViewById<TextView>(R.id.btnChooseDios)

        btnHumano.applyDoSenkGradient(cornerRadius = 16f)
        btnDios.applyDoSenkGradient(cornerRadius = 16f)

        //  LA DOBLE PERSONALIDAD (El chisme que le pasamos al navegar)


        // Leemos el argumento (por defecto es false, asumiendo que viene del BottomNav)
        val isSelectionMode = arguments?.getBoolean("isSelectionMode") ?: false

        if (isSelectionMode) {
            btnHumano.text = "¡ESCÓGELO!"
            btnDios.text = "¡ESCÓGELO!"

            btnHumano.setOnClickListener { view ->
                saveAndNavigate("Humano", view)
            }
            btnDios.setOnClickListener { view ->
                saveAndNavigate("Dios", view)
            }

        } else {
            //  VIENE DEL MENÚ NAVEGABLE (Modo Vitrina)
            btnHumano.text = "MUÉSTRAMELO"
            btnDios.text = "MUÉSTRAMELO"

            btnHumano.setOnClickListener {
                Toast.makeText(requireContext(), "Demostración: Bloqueo Humano activado ", Toast.LENGTH_SHORT).show()
                // TODO: Lanzar demostración del BlockerEngineService
            }
            btnDios.setOnClickListener {
                Toast.makeText(requireContext(), "Demostración: Bloqueo Dios activado ", Toast.LENGTH_SHORT).show()
                // TODO: Lanzar demostración del BlockerEngineService
            }
        }

        view.findViewById<View>(R.id.btnEditCustomBlock).setOnClickListener {
            Toast.makeText(requireContext(), "Editando bloqueo personalizado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndNavigate(blockType: String, clickedButton: View) {
        // Se desactiav el boton para no dublicar registros
        clickedButton.isEnabled = false
        findNavController().navigate(R.id.action_BlockZone_to_home)

        viewModel.saveMissionToDatabase(blockType) {
            Toast.makeText(requireContext(), "¡Misión $blockType programada!", Toast.LENGTH_SHORT).show()

            // viaje al home
            clickedButton.isEnabled = true

            // Regresamos al Home destruyendo el historial
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }
}