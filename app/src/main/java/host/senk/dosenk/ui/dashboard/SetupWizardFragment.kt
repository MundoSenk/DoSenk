package host.senk.dosenk.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupWizardFragment : Fragment(R.layout.fragment_setup_wizard) {

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  PINTADO DE GRADIENTES
        view.findViewById<View>(R.id.stats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 20f)

        view.findViewById<View>(R.id.layoutWizardGradient)
            ?.applyDoSenkGradient(
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                cornerRadius = 0f
            )

        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.bottomNav)
            ?.findViewById<View>(R.id.layoutBottomGradient)
            ?.applyDoSenkGradient()

        //  INYECCIÓN DEL NOMBRE REAL
        val tvHeaderUsername = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        val tvGreetingSubtitle = view.findViewById<TextView>(R.id.tvGreetingSubtitle)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userAlias.collect { realName ->
                // Actualizamos el Header
                tvHeaderUsername?.text = "Bienvenido, $realName"

                // Actualizamos el texto insultante del tutorial
                tvGreetingSubtitle?.text = "Perfecto! Ya era hora, $realName.\nPermíteme conocer a mi nueva víctima...\n ¡DIGO! A mi nuevo jefe"
            }
        }

        // LÓGICA DE CHECKBOXES
        val cbStudent = view.findViewById<CheckBox>(R.id.cbStudent)
        val cbEmployee = view.findViewById<CheckBox>(R.id.cbEmployee)
        val cbBusiness = view.findViewById<CheckBox>(R.id.cbBusiness)
        val cbNone = view.findViewById<CheckBox>(R.id.cbNone)

        cbNone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbStudent.isChecked = false
                cbEmployee.isChecked = false
                cbBusiness.isChecked = false
            }
        }

        val roleListener = { _: View, isChecked: Boolean ->
            if (isChecked) cbNone.isChecked = false
        }
        cbStudent.setOnCheckedChangeListener(roleListener)
        cbEmployee.setOnCheckedChangeListener(roleListener)
        cbBusiness.setOnCheckedChangeListener(roleListener)

        //  NAVEGACIÓN INTELIGENTE Y GUARDADO
        view.findViewById<Button>(R.id.btnStartPainting).setOnClickListener {

            viewModel.isStudent = cbStudent.isChecked
            viewModel.isEmployee = cbEmployee.isChecked
            viewModel.isBusiness = cbBusiness.isChecked
            val isNone = cbNone.isChecked

            // VALIDACIÓN
            if (!viewModel.isStudent && !viewModel.isEmployee && !viewModel.isBusiness && !isNone) {
                Toast.makeText(context, "¡Selecciona al menos una opción, gallo!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // DECISIÓN DE RUTA
            if (isNone) {
                // Va directo a sacar las estadísticas (Asegúrate de que este ID coincida con tu nav_graph)
                findNavController().navigate(R.id.setupStatsFragment)
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