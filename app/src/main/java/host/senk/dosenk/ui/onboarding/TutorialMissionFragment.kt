package host.senk.dosenk.ui.onboarding

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch


import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class TutorialMissionFragment : Fragment(R.layout.fragment_tutorial_mission) {

    private val viewModel: TutorialMissionViewModel by viewModels()
    private var pulseAnim: ObjectAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. PINTAR EL FALSO HOME Y BOTTOM NAV
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.cardStats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 24f)

        view.findViewById<View>(R.id.bottomNav)
            ?.findViewById<View>(R.id.layoutBottomGradient)
            ?.applyDoSenkGradient()

        // xml
        val layoutBottomDialogs = view.findViewById<View>(R.id.layoutBottomDialogs)
        val layoutState1 = view.findViewById<View>(R.id.layoutState1)
        val layoutState2 = view.findViewById<View>(R.id.layoutState2)
        val layoutTopDialog = view.findViewById<View>(R.id.layoutTopDialog)
        val layoutCreationMenu = view.findViewById<View>(R.id.layoutCreationMenu)

        val bottomNav = view.findViewById<View>(R.id.bottomNav)

        // Botones
        val btnOkEntiendo = view.findViewById<View>(R.id.btnOkEntiendo)
        val btnSiAyudame = view.findViewById<View>(R.id.btnSiAyudame)
        val cardMisionDiaria = view.findViewById<View>(R.id.cardMisionDiaria)

        // Textos
        val tvDecepcionadoText = view.findViewById<TextView>(R.id.tvDecepcionadoText)
        val tvHeaderUsername = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        val tvRankStat = view.findViewById<View>(R.id.cardStats)?.findViewById<TextView>(R.id.tvDisciplinaStatus)

        // Tomamos el contenedor del botón para animarlo COMPLETO (con todo y sombra)
        val fabAddContainer = bottomNav?.findViewById<View>(R.id.fabAddContainer)

        val cardStatsView = view.findViewById<View>(R.id.cardStats)
        val bottomNavView = view.findViewById<View>(R.id.bottomNav)

        cardStatsView?.translationZ = 100f
        bottomNavView?.translationZ = 0f

        //  CARGAR DATOS REALES
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realRankName.collect { rank ->
                tvDecepcionadoText.text = "Mírenlo todo un rango ${rank.uppercase()}\nPuess.... HAY MUCHO QUE HACER!\nTe mostraré como funciona >DO!"
                tvRankStat?.text = rank
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realAlias.collect { alias ->
                tvHeaderUsername?.text = "Bienvenido, $alias"
            }
        }

        // MÁQUINA DE ESTADOS
        btnOkEntiendo.setOnClickListener {
            layoutState1.visibility = View.GONE
            layoutState2.visibility = View.VISIBLE
        }

        btnSiAyudame.setOnClickListener {
            layoutBottomDialogs.visibility = View.GONE
            layoutTopDialog.visibility = View.VISIBLE

            // ¡EL SUBIBAJA DE LUCES!
            cardStatsView?.translationZ = 0f  // Hundimos las estadísticas en la oscuridad
            bottomNavView?.translationZ = 100f // Sacamos el Nav hacia la luz

            // El botón empieza a latir para invitar a tocarlo
            fabAddContainer?.let {
                pulseAnim = ObjectAnimator.ofPropertyValuesHolder(
                    it,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f, 1.0f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f, 1.0f)
                ).apply {
                    duration = 1000 // 1 segundo
                    repeatCount = ObjectAnimator.INFINITE // Infinito
                    start()
                }
            }
        }

        // Clic en el botón +
        fabAddContainer?.setOnClickListener {
            if (layoutTopDialog.visibility == View.VISIBLE) {
                // Detenemos el latido
                pulseAnim?.cancel()
                fabAddContainer.scaleX = 1f
                fabAddContainer.scaleY = 1f

                // MAGIA 3: Regresamos el Nav a la oscuridad para que el menú principal lo cubra bien
                bottomNav?.translationZ = 0f

                layoutTopDialog.visibility = View.GONE
                layoutCreationMenu.visibility = View.VISIBLE
            }
        }

        // Clic final en Misión Diaria -> Terminar Onboarding
        cardMisionDiaria.setOnClickListener {

                findNavController().navigate(R.id.action_Tuto_Mission_to_CreateMission)

        }
    }
}