package host.senk.dosenk.ui.onboarding

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.service.BlockerEngineService
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TutorialConclusionFragment : Fragment(R.layout.fragment_tutorial_conclusion) {

    private val viewModel: TutorialMissionViewModel by viewModels()
    @Inject
    lateinit var userPreferences: host.senk.dosenk.data.local.UserPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val layoutLogoGradient = view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)
        val layoutStatsGradient = view.findViewById<View>(R.id.cardStats)?.findViewById<View>(R.id.layoutStatsGradient)
        val tvDogBadge = view.findViewById<TextView>(R.id.tvDogBadge)
        val tvGoodbyeTitle = view.findViewById<TextView>(R.id.tvGoodbyeTitle)
        val btnEnterHome = view.findViewById<TextView>(R.id.btnEnterHome)

        //
        layoutLogoGradient?.applyDoSenkGradient(cornerRadius = 12f)
        layoutStatsGradient?.applyDoSenkGradient(cornerRadius = 24f)
        tvDogBadge?.applyDoSenkGradient(cornerRadius = 50f)


        val tvHeaderUsername = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        val tvRankStat = view.findViewById<View>(R.id.cardStats)?.findViewById<TextView>(R.id.tvDisciplinaStatus)
        val tvTimerCountdown = view.findViewById<TextView>(R.id.tvTimerCountdown)
        val cardTimerFloating = view.findViewById<View>(R.id.cardTimerFloating)

        val layoutStateMocking = view.findViewById<View>(R.id.layoutStateMocking)
        val layoutStateTheme = view.findViewById<View>(R.id.layoutStateTheme)
        val layoutStateGoodbye = view.findViewById<View>(R.id.layoutStateGoodbye)

        cardTimerFloating.translationZ = 100f

        // Cargar datos
        viewLifecycleOwner.lifecycleScope.launch { viewModel.realAlias.collect { tvHeaderUsername?.text = "Bienvenido, $it" } }
        viewLifecycleOwner.lifecycleScope.launch { viewModel.realRankName.collect { tvRankStat?.text = it } }

        //  CONECTARSE AL RELOJ DEL JEFE
        viewLifecycleOwner.lifecycleScope.launch {
            BlockerEngineService.timeLeftFlow.collect { secondsLeft ->
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                tvTimerCountdown.text = String.format("00:%02d:%02d", minutes, seconds)

                // TIEMPO CUMPLIDO
                if (secondsLeft <= 0) {
                    layoutStateMocking.visibility = View.GONE
                    layoutStateTheme.visibility = View.VISIBLE
                    cardTimerFloating.visibility = View.GONE
                }
            }
        }


        val themeClickListener = View.OnClickListener { v ->

            // Determinamos qué tema eligió según el botón que tocó
            val (chosenThemeResId, themeNameStr) = when(v.id) {
                R.id.btnThemeTeal -> Pair(R.style.Theme_DoSenk_Teal, "teal")
                R.id.btnThemeRed -> Pair(R.style.Theme_DoSenk_Red, "red")
                R.id.btnThemePurple -> Pair(R.style.Theme_DoSenk_Purple, "purple")
                else -> Pair(R.style.Theme_DoSenk_Teal, "teal")
            }

            // LE CAMBIAMOS EL TEMA A LA ACTIVIDAD EN CALIENTE
            requireActivity().setTheme(chosenThemeResId)

            // USAMOS TU FUNCIÓN EXTENSION PARA REPINTAR LOS GRADIENTES
            layoutLogoGradient?.applyDoSenkGradient(cornerRadius = 12f)
            layoutStatsGradient?.applyDoSenkGradient(cornerRadius = 24f)
            tvDogBadge?.applyDoSenkGradient(cornerRadius = 50f)

            // SACAMOS EL COLOR PURO (doSkinButton) PARA PINTAR TEXTOS
            val typedValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.doSkinButton, typedValue, true)
            val buttonColor = typedValue.data

            tvGoodbyeTitle.setTextColor(buttonColor)
            btnEnterHome.setTextColor(buttonColor)

            // GUARDAMOS SU DECISIÓN EN LA BASE DE DATOS

            viewModel.saveAppTheme(themeNameStr)

            // AVANZAMOS A LA DESPEDIDA
            layoutStateTheme.visibility = View.GONE
            layoutStateGoodbye.visibility = View.VISIBLE
        }

        view.findViewById<View>(R.id.btnThemeTeal).setOnClickListener(themeClickListener)
        view.findViewById<View>(R.id.btnThemeRed).setOnClickListener(themeClickListener)
        view.findViewById<View>(R.id.btnThemePurple).setOnClickListener(themeClickListener)

        // GRADUACIÓN AL HOME REAL
        btnEnterHome.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                userPreferences.saveStartDate(System.currentTimeMillis())

                // Pasamos al Estado 4 y lo mandamos al Dashboard real
                viewModel.finishOnboarding {
                    findNavController().navigate(R.id.action_TutoConclusion_to_home)
                }
            }
        }
    }
}