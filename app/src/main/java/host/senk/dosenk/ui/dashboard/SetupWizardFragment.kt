package host.senk.dosenk.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R

@AndroidEntryPoint
class SetupWizardFragment : Fragment(R.layout.fragment_setup_wizard) {

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnStartPainting).setOnClickListener {
            // 1. Guardamos la selección en la mochila
            viewModel.isStudent = view.findViewById<CheckBox>(R.id.cbStudent).isChecked
            viewModel.isEmployee = view.findViewById<CheckBox>(R.id.cbEmployee).isChecked
            viewModel.isBusiness = view.findViewById<CheckBox>(R.id.cbBusiness).isChecked

            // 2. Decidimos a dónde ir
            when {
                viewModel.isStudent -> findNavController().navigate(R.id.action_wizard_to_school)
                viewModel.isEmployee -> findNavController().navigate(R.id.action_wizard_to_work)
                viewModel.isBusiness -> findNavController().navigate(R.id.action_wizard_to_business)
                else -> Toast.makeText(context, "¡Selecciona algo, gallo!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}