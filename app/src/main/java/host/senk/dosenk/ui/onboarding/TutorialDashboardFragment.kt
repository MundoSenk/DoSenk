package host.senk.dosenk.ui.onboarding

import android.R.attr.theme
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.ui.dashboard.SetupViewModel
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.view.ViewGroup
import android.widget.LinearLayout


@AndroidEntryPoint
class TutorialDashboardFragment : Fragment(R.layout.fragment_tutorial_dashboard) {

    // Extraemos la información del fragmento anterior
    private val sharedSetupViewModel: SetupViewModel by activityViewModels()

    private val tutorialViewModel: TutorialViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // los contenedores xml que estamos trayendo
        val layoutState1 = view.findViewById<View>(R.id.layoutState1)
        val layoutState2 = view.findViewById<View>(R.id.layoutState2)
        val layoutRankReveal = view.findViewById<View>(R.id.layoutRankReveal)

        //xml botobnes y textosz
        val btnYes = view.findViewById<View>(R.id.btnYes)
        val btnWhatever = view.findViewById<View>(R.id.btnWhatever)
        val tvRankTitle = view.findViewById<TextView>(R.id.tvRankTitle)
        val btnAcceptRank = view.findViewById<Button>(R.id.btnAcceptRank)

        // PINTADO DE GRADIENTES (UI)
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.cardStats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 24f)

        // Acción compartida para los dos botones del perrito
        val startCalculationAction = View.OnClickListener {
            // Pasamos del Estado 1 al 2
            layoutState1.visibility = View.GONE
            layoutState2.visibility = View.VISIBLE

            // Gamificación: Hacemos que la app "piense"
            viewLifecycleOwner.lifecycleScope.launch {
                delay(3000) // 3 segundos de suspenso

                // 1. Calculamos el rango
                val totalTime = sharedSetupViewModel.trueTotalTimeMs
                tutorialViewModel.calculateRank(totalTime)
                val userRank = tutorialViewModel.calculatedRank

                tvRankTitle.text = "Tu rango es:\n'${userRank.uppercase()}'"

                //  BARRA ANIMADA ////////////////////////////////////

                val layoutRankList = view.findViewById<View>(R.id.layoutRankList)
                val llRanks = view.findViewById<LinearLayout>(R.id.llRanks)
                val trackFill = view.findViewById<View>(R.id.trackFill)

                llRanks.removeAllViews()

                // La lista oficial de rangos (De Arriba hacia Abajo)
                val rangosOficiales = listOf("Dios", "As", "Idóneo", "Skywalker", "Potencial", "Olvidable", "Purgatorio", "Infierno")
                var targetIndex = 7 // Por defect hasta el fondo

                // Inflamos las 8 filas dinámicamente
                rangosOficiales.forEachIndexed { index, rankName ->
                    val itemNode = layoutInflater.inflate(R.layout.item_rank_node, llRanks, false)
                    itemNode.findViewById<TextView>(R.id.tvRankName).text = rankName

                    // Si encontramos el rango del usuario, guardamos el índice y pintamos el texto del color del tema
                    if (rankName.equals(userRank, ignoreCase = true)) {
                        targetIndex = index
                        // Pintar el texto del rango ganador
                        itemNode.findViewById<TextView>(R.id.tvRankName).setTextColor(theme)
                    }
                    llRanks.addView(itemNode)
                }

                // Mostramos el overlay oscuro
                layoutRankReveal.alpha = 0f
                layoutRankReveal.visibility = View.VISIBLE
                layoutRankReveal.animate().alpha(1f).setDuration(600).start()

                // ANIMACIÓN DE LA BARRA
                layoutRankList.post {
                    val totalHeight = llRanks.height
                    val targetView = llRanks.getChildAt(targetIndex)

                    // Calculamos el centro del nodo ganador
                    val targetCenterY = targetView.top + (targetView.height / 2)

                    // La altura que debe llenarse
                    val fillHeight = totalHeight - targetCenterY

                    // Animamos la altura de 0 a fillHeight
                    val animator = ValueAnimator.ofInt(0, fillHeight)
                    animator.duration = 1500 // 1.5 segundos
                    animator.interpolator = DecelerateInterpolator()
                    animator.addUpdateListener { anim ->
                        val value = anim.animatedValue as Int
                        val lp = trackFill.layoutParams
                        lp.height = value
                        trackFill.layoutParams = lp
                    }
                    animator.start()
                }
            }
        }





        btnYes.setOnClickListener(startCalculationAction)
        btnWhatever.setOnClickListener(startCalculationAction)

        btnAcceptRank.setOnClickListener {
            // Desactivamos el botón para que no le pique dos veces
            btnAcceptRank.isEnabled = false
            btnAcceptRank.text = "Guardando.."

            tutorialViewModel.saveToDatabase(
                totalTimeMs = sharedSetupViewModel.trueTotalTimeMs,
                vicesList = sharedSetupViewModel.worstAppsList,
                onSuccess = { 
                    //findNavController().navigate(R.id.action_global_homeFragment)
                },
                onError = {
                    btnAcceptRank.isEnabled = true
                    btnAcceptRank.text = "ACEPTAR!"

                }
            )
        }
    }
}