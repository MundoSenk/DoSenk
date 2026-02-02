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
import host.senk.dosenk.util.applyDoSenkGradient

@AndroidEntryPoint
class SetupWorkFragment : Fragment(R.layout.fragment_setup_grid) {

    private val viewModel: SetupViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Esconder botones
        view.findViewById<View>(R.id.stats)?.visibility = View.GONE
        view.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE


        // Pintamos del gradiante que venga desde el registro
        view.findViewById<View>(R.id.stats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 20f)

        view.findViewById<View>(R.id.layoutWizardGradient)
            ?.applyDoSenkGradient(
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                cornerRadius = 0f
            )

        view.findViewById<View>(R.id.bottomNav)
            ?.findViewById<View>(R.id.layoutBottomGradient)
            ?.applyDoSenkGradient()


        //header
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        val paintView = view.findViewById<TimeGridPaintView>(R.id.timeGrid)
        view.findViewById<TextView>(R.id.tvPhaseTitle).text = "Horario LABORAL"

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.doSkinButton, typedValue, true)
        paintView.setThemeColor(typedValue.data)

        // Bloqueamos lo que ya pintó en la escuela
        paintView.setBlockedCells(viewModel.getCombinedBlockedGrid())

        view.findViewById<Button>(R.id.btnNextPhase).setOnClickListener {
            viewModel.workGrid = paintView.getCurrentSelection()

            when {
                viewModel.isBusiness -> findNavController().navigate(R.id.action_work_to_business)
                else -> saveAndFinish() // Mismo método que arriba
            }
        }

    }



    private fun saveAndFinish() {
        // Bloquear botón o mostrar loading...
        viewModel.finalSave(
            onSuccess = {
                Toast.makeText(context, "¡Listo gallo!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_global_homeFragment)
            },
            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        )
    }




}