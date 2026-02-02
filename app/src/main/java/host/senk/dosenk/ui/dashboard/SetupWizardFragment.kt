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
import host.senk.dosenk.util.applyDoSenkGradient

@AndroidEntryPoint
class SetupWizardFragment : Fragment(R.layout.fragment_setup_wizard) {

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pintamos del gradiante que venga desde el registro
        view.findViewById<View>(R.id.stats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 20f)

        view.findViewById<View>(R.id.layoutWizardGradient)
            ?.applyDoSenkGradient(
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                cornerRadius = 0f
            )

        //header
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.bottomNav)
            ?.findViewById<View>(R.id.layoutBottomGradient)
            ?.applyDoSenkGradient()


        // LÓGICA DE CHECKBOXES (Exclusión Mutua)
        val cbStudent = view.findViewById<CheckBox>(R.id.cbStudent)
        val cbEmployee = view.findViewById<CheckBox>(R.id.cbEmployee)
        val cbBusiness = view.findViewById<CheckBox>(R.id.cbBusiness)
        val cbNone = view.findViewById<CheckBox>(R.id.cbNone)

        //  Si marcas "Nada de nada", limpia los demás
        cbNone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbStudent.isChecked = false
                cbEmployee.isChecked = false
                cbBusiness.isChecked = false
            }
        }

        //  Si marcas cualquier rol, limpia "Nada de nada"
        val roleListener = { _: View, isChecked: Boolean ->
            if (isChecked) cbNone.isChecked = false
        }
        cbStudent.setOnCheckedChangeListener(roleListener)
        cbEmployee.setOnCheckedChangeListener(roleListener)
        cbBusiness.setOnCheckedChangeListener(roleListener)


        // NAVEGACIÓN INTELIGENTE
        view.findViewById<Button>(R.id.btnStartPainting).setOnClickListener {

            // Guardamos estado en ViewModel
            viewModel.isStudent = cbStudent.isChecked
            viewModel.isEmployee = cbEmployee.isChecked
            viewModel.isBusiness = cbBusiness.isChecked
            val isNone = cbNone.isChecked

            // VALIDACIÓN: ¿Seleccionó algo?
            if (!viewModel.isStudent && !viewModel.isEmployee && !viewModel.isBusiness && !isNone) {
                Toast.makeText(context, "¡Selecciona al menos una opción, gallo!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // DECISIÓN DE RUTA
            if (isNone) {
                // Guardar vacío (para que marque setup_finished = true)
                viewModel.finalSave(
                    onSuccess = {
                        findNavController().navigate(R.id.action_global_homeFragment)
                    },
                    onError = { Toast.makeText(context, "Error al saltar", Toast.LENGTH_SHORT).show() }
                )
            } else {
                navigateToFirstPaintingStep()
            }
        }
    }

    private fun navigateToFirstPaintingStep() {
        when {
            viewModel.isStudent -> findNavController().navigate(R.id.action_wizard_to_school)
            viewModel.isEmployee -> findNavController().navigate(R.id.action_wizard_to_work)
            viewModel.isBusiness -> findNavController().navigate(R.id.action_wizard_to_business)
        }
    }



}