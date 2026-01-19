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
class SetupSchoolFragment : Fragment(R.layout.fragment_setup_grid) {

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val paintView = view.findViewById<TimeGridPaintView>(R.id.timeGrid)
        view.findViewById<TextView>(R.id.tvPhaseTitle).text = "Horario ESCOLAR"

        // Ponemos el color del tema
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.doSkinButton, typedValue, true)
        paintView.setThemeColor(typedValue.data)

        view.findViewById<Button>(R.id.btnNextPhase).setOnClickListener {
            // Guardamos el dibujo de la escuela
            viewModel.schoolGrid = paintView.getCurrentSelection()

            //pa ir a la siguiente seleccion
            when {
                viewModel.isEmployee -> findNavController().navigate(R.id.action_school_to_work)
                viewModel.isBusiness -> findNavController().navigate(R.id.action_school_to_business)
                else -> Toast.makeText(context, "¡Terminaste!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}