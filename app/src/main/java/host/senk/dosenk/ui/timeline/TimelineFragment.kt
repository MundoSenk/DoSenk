package host.senk.dosenk.ui.timeline

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.data.local.entity.MissionEntity
import host.senk.dosenk.util.applyDoSenkGradient

@AndroidEntryPoint
class TimelineFragment : Fragment(R.layout.fragment_timeline) {

    private lateinit var rvTimeline: RecyclerView
    private lateinit var adapter: TimelineAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        setupGradients(view)
        setupBackButton()

        // NAVEGACIÓN DESDE EL MENÚ INFERIOR
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_timeline)?.setOnClickListener {
            findNavController().popBackStack()
        }

        // INICIALIZAR LA LISTA (Con nuestros datos de mentira por ahora)
        rvTimeline = view.findViewById(R.id.rvTimeline)
        rvTimeline.layoutManager = LinearLayoutManager(requireContext())

        val mockData = listOf(
            TimelineItem.MissionCard(
                timeLabel = "15:00",
                mission = MissionEntity(name = "Creación de Personajes", description = "En una libreta dibuja...", durationMinutes = 60, executionDate = 0L, assignmentType = "manual", blockType = "Humano", status = "pending")
            ),
            TimelineItem.EmptySlot(timeLabel = "16:00", durationMinutes = 60),
            TimelineItem.MissionCard(
                timeLabel = "17:00",
                mission = MissionEntity(name = "Misión Rápida", description = "Llamar a mi abuela", durationMinutes = 15, executionDate = 0L, assignmentType = "manual", blockType = "Dios", status = "pending")
            )
        )

        adapter = TimelineAdapter(mockData)
        rvTimeline.adapter = adapter
    }

    //  pintado de gradiantes
    private fun setupGradients(view: View) {
        // Pintamos el logo de arriba
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        // Pintamos tu nueva caja de filtros
        view.findViewById<View>(R.id.cardFilter)?.findViewById<View>(R.id.layoutFilterGradient)?.applyDoSenkGradient(cornerRadius = 16f)
        // Pintamos la barra de abajo
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
    }

    //  RESPETAR LA BARRA DE ESTADO Y GESTOS
    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    // FORZAR EL COMPORTAMIENTO DEL BOTÓN ATRÁS
    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_timeline_to_Home)
        }
    }
}