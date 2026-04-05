package host.senk.dosenk.ui.mission

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.GameEngine
import host.senk.dosenk.util.applyDoSenkGradient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MissionSummaryFragment : Fragment(R.layout.fragment_mission_summary) {

    // Compartimos el mismo ViewModel para tener acceso a los datos del formulario
    private val viewModel: CreateMissionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets(view)

        val selectedBlockType = arguments?.getString("blockType") ?: "Humano"

        val tvMissionName = view.findViewById<TextView>(R.id.tvMissionName)
        val tvMissionDesc = view.findViewById<TextView>(R.id.tvMissionDesc)
        val tvBaseXp = view.findViewById<TextView>(R.id.tvBaseXp)
        val tvStreakXp = view.findViewById<TextView>(R.id.tvStreakXp)
        val tvMultiplierValue = view.findViewById<TextView>(R.id.tvMultiplierValue)
        val tvTotalXp = view.findViewById<TextView>(R.id.tvTotalXp)
        val btnAcceptMission = view.findViewById<TextView>(R.id.btnAcceptMission)

        tvMissionName.text = viewModel.missionName
        tvMissionDesc.text = if (viewModel.missionDescription.isNotBlank()) viewModel.missionDescription else "Sin detalles adicionales."

        // El botón empieza apagado hasta que calculemos la matemática
        btnAcceptMission.isEnabled = false
        btnAcceptMission.text = "Calculando contrato..."
        btnAcceptMission.applyDoSenkGradient(cornerRadius = 24f)

        //
        viewLifecycleOwner.lifecycleScope.launch {

            // Pide el ticket real desempaquetando la BD
            val ticket = viewModel.generateRealTicket(selectedBlockType, requireContext())

            // PINTAMOS EL TICKET
            tvBaseXp.text = "${ticket.baseXP} XP"
            tvStreakXp.text = "+ ${ticket.streakBonusXP} XP"

            if (ticket.multiplier < 1.0) {
                tvMultiplierValue.text = "x${ticket.multiplier} (Modo Cobarde)"
                tvMultiplierValue.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
            } else {
                tvMultiplierValue.text = "x${ticket.multiplier}"
                tvMultiplierValue.setTextColor(android.graphics.Color.parseColor("#00C853"))
            }

            // ANIMACIÓN SÁDICA DEL TOTAL
            val activeColor = android.util.TypedValue()
            requireContext().theme.resolveAttribute(R.attr.doSkinButton, activeColor, true)
            tvTotalXp.setTextColor(activeColor.data)

            GameEngine.animateXpCounter(tvTotalXp, ticket.totalXP)

            // Despertamos el botón
            btnAcceptMission.isEnabled = true
            btnAcceptMission.text = "¡ACEPTAR MISIÓN!"
        }

        // GUARDAR EN LA BASE DE DATOS
        btnAcceptMission.setOnClickListener {
            btnAcceptMission.isEnabled = false
            btnAcceptMission.text = "Firmando contrato..."

            viewModel.saveMissionToDatabase(selectedBlockType) {
                Toast.makeText(requireContext(), "¡Contrato firmado! No te rajes.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_summary_to_TimeLime)
            }
        }
    }

    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}