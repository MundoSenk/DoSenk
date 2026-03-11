package host.senk.dosenk.ui.blocks

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.ui.mission.CreateMissionViewModel
import host.senk.dosenk.ui.nav.AddMenuBottomSheet
import host.senk.dosenk.util.applyDoSenkGradient

@AndroidEntryPoint
class BlockZoneFragment : Fragment(R.layout.fragment_block_zone) {

    // La mochila sigue aquí, pero solo la usaremos si estamos en modo "Selección"
    private val viewModel: CreateMissionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        setupGradients(view)
        setupInteractiveElements(view)
        setupBackButton()

        val btnHumano = view.findViewById<TextView>(R.id.btnChooseHumano)
        val btnDios = view.findViewById<TextView>(R.id.btnChooseDios)

        btnHumano.applyDoSenkGradient(cornerRadius = 16f)
        btnDios.applyDoSenkGradient(cornerRadius = 16f)

        // LA DOBLE PERSONALIDAD (El chisme que le pasamos al navegar)
        val isSelectionMode = arguments?.getBoolean("isSelectionMode") ?: false

        if (isSelectionMode) {
            btnHumano.text = "¡ESCÓGELO!"
            btnDios.text = "¡ESCÓGELO!"

            btnHumano.setOnClickListener { v -> saveAndNavigate("Humano", v) }
            btnDios.setOnClickListener { v -> saveAndNavigate("Dios", v) }
        } else {
            // MODO VITRINA (Llegó desde la Navbar)
            btnHumano.text = "MUÉSTRAMELO"
            btnDios.text = "MUÉSTRAMELO"

            btnHumano.setOnClickListener {
                Toast.makeText(requireContext(), "Demostración: Bloqueo Humano activado ", Toast.LENGTH_SHORT).show()
            }
            btnDios.setOnClickListener {
                Toast.makeText(requireContext(), "Demostración: Bloqueo Dios activado ", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.btnEditCustomBlock).setOnClickListener {
            Toast.makeText(requireContext(), "Editando bloqueo personalizado", Toast.LENGTH_SHORT).show()
        }
    }

    // MAGIA DE COLORES
    private fun setupGradients(view: View) {
        view.findViewById<View>(R.id.layoutZoneHeader).applyDoSenkGradient()
        view.findViewById<View>(R.id.btnEditCustomBlock).applyDoSenkGradient(cornerRadius = 16f)
        view.findViewById<View>(R.id.bgHumanoGradient).applyDoSenkGradient()
        view.findViewById<View>(R.id.bgDiosGradient).applyDoSenkGradient()

        // Pintamos el Header y el Nav
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
    }

    //  RESPETAR LA BARRA DE ESTADO
    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    //  CLICS DE NAVEGACIÓN
    private fun setupInteractiveElements(view: View) {
        // Botón FAB Central
        val fabAdd = view.findViewById<View>(R.id.fabAddContainer)

        fabAdd.setOnClickListener {
            val bottomSheet = AddMenuBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddMenuBottomSheet")
        }

        // Navegar de regreso a Inicio o Timeline
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_home)?.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_timeline)?.setOnClickListener {
            findNavController().navigate(R.id.action_BlockZone_to_TimeLime)
        }

        // El Header text estático por ahora
        val tvUser = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        tvUser?.text = "Bloqueos, @User"
    }

    // COMPORTAMIENTO DEL BOTÓN "ATRÁS"
    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun saveAndNavigate(blockType: String, clickedButton: View) {
        clickedButton.isEnabled = false

        viewModel.saveMissionToDatabase(blockType) {
            Toast.makeText(requireContext(), "¡Misión $blockType programada!", Toast.LENGTH_SHORT).show()
            clickedButton.isEnabled = true
            // Viajamos al Timeline para ver nuestra obra de arte
            findNavController().navigate(R.id.action_BlockZone_to_TimeLime)
        }
    }
}