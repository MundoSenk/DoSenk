package host.senk.dosenk.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TutorialCreateMissionFragment : Fragment(R.layout.fragment_tutorial_create_mission) {

    // Reusamos el mismo ViewModel para no repetir consultas de Base de Datos
    private val viewModel: TutorialMissionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // PINTAR EL FALSO HOME Y LOS BORDES
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.cardStats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 24f)

        view.findViewById<View>(R.id.gradientBorder)
            ?.applyDoSenkGradient(cornerRadius = 32f)

        // CARGAR EL NOMBRE DEL USUARIO Y LA FECHA
        val tvHeaderUsername = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        val tvDateSimulated = view.findViewById<TextView>(R.id.tvDateSimulated)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realAlias.collect { alias ->
                tvHeaderUsername?.text = "Bienvenido, $alias"
            }
        }

        // Ponemos la fecha de Hoy dinámicamente
        val currentDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
        tvDateSimulated.text = "Hoy ($currentDate)"


        //  LA ACCIÓN FINAL
        val btnAssignBlock = view.findViewById<Button>(R.id.btnAssignBlock)

        btnAssignBlock.setOnClickListener {
            
        }
    }
}