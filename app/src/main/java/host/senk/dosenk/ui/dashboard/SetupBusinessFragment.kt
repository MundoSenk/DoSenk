package host.senk.dosenk.ui.dashboard

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.ui.custom.TimeGridPaintView

@AndroidEntryPoint
class SetupBusinessFragment : Fragment(R.layout.fragment_setup_grid) { // Reutilizamos el layout del Grid

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paintView = view.findViewById<TimeGridPaintView>(R.id.timeGrid)

        //Título y Color
        view.findViewById<TextView>(R.id.tvPhaseTitle).text = "Horario DE NEGOCIO"
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.doSkinButton, typedValue, true)
        paintView.setThemeColor(typedValue.data)

        // BLOQUEO: Sumamos Escuela + Trabajo para que salgan grises
        paintView.setBlockedCells(viewModel.getCombinedBlockedGrid())

        // Botón Finaliza
        val btnNext = view.findViewById<Button>(R.id.btnNextPhase)
        btnNext.text = "Finalizar"

        btnNext.setOnClickListener {
            // Guardamos el último grid
            viewModel.businessGrid = paintView.getCurrentSelection()


            Toast.makeText(context, "¡Configuración guardada con éxito!", Toast.LENGTH_LONG).show()


            // findNavController().navigate(R.id.action_global_homeFragment)
        }
    }
}