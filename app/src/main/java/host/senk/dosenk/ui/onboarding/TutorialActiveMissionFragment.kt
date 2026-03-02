package host.senk.dosenk.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import host.senk.dosenk.service.BlockerEngineService

@AndroidEntryPoint
class TutorialActiveMissionFragment : Fragment(R.layout.fragment_tutorial_active_mission) {

    private val viewModel: TutorialMissionViewModel by viewModels()
    private var countdownJob: Job? = null


    private var kickOutJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. PINTAR EL FALSO HOME
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.cardStats)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 24f)

        // XML
        val tvHeaderUsername = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)
        val tvRankStat = view.findViewById<View>(R.id.cardStats)?.findViewById<TextView>(R.id.tvDisciplinaStatus)

        val cardTimerFloating = view.findViewById<View>(R.id.cardTimerFloating)
        val tvTimerTitle = view.findViewById<TextView>(R.id.tvTimerTitle)
        val tvTimerSubtitle = view.findViewById<TextView>(R.id.tvTimerSubtitle)
        val tvTimerCountdown = view.findViewById<TextView>(R.id.tvTimerCountdown)

        val layoutState1 = view.findViewById<View>(R.id.layoutState1)
        val layoutState2 = view.findViewById<View>(R.id.layoutState2)
        val layoutState3 = view.findViewById<View>(R.id.layoutState3)

        val btnAdelante = view.findViewById<View>(R.id.btnAdelante)
        val btnEntiendo = view.findViewById<View>(R.id.btnEntiendo)
        val btnNoDecidiEso = view.findViewById<View>(R.id.btnNoDecidiEso)
        val btnQueQuieresDecir = view.findViewById<View>(R.id.btnQueQuieresDecir)

        // CARGAR DATOS REALES
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realAlias.collect { alias ->
                tvHeaderUsername?.text = "Bienvenido, $alias"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realRankName.collect { rank ->
                tvRankStat?.text = rank
            }
        }

        // MaQUINA DE ESTADOS

        // ESTADO
        btnAdelante.setOnClickListener {
            layoutState1.visibility = View.GONE
            layoutState2.visibility = View.VISIBLE

            // Fuerza bruta para brillar
            cardTimerFloating.translationZ = 100f

            // Pop-up suave
            cardTimerFloating.visibility = View.VISIBLE
            cardTimerFloating.alpha = 0f
            cardTimerFloating.scaleX = 0.8f
            cardTimerFloating.scaleY = 0.8f
            cardTimerFloating.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .start()

            // ¡AQUÍ ESTÁ LA MAGIA DEL AUTO-AVANCE!
            startDynamicCountdown(tvTimerCountdown, 15) {
                // Si esta función se ejecuta, significa que el contador llegó a cero.
                if (layoutState2.visibility == View.VISIBLE) {
                    // Forzamos el clic
                    btnEntiendo.performClick()
                }
            }
        }

        // ESTADO 3
        btnEntiendo.setOnClickListener {
            layoutState2.visibility = View.GONE
            layoutState3.visibility = View.VISIBLE

            // Cambiamos textos
            tvTimerTitle.text = "¡MISIÓN\nACTIVA!"
            tvTimerSubtitle.text = "TIEMPO RESTANTE:"

            // Matamos el contador de 15s y arrancamos el 2:30
            startDynamicCountdown(tvTimerCountdown, 150)

            //¡LA TRAMPA DE LOS 10 SEGUNDOS
            kickOutJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(10000) // Esperamos 10 segundos en silencio...
                if (isActive) {
                    executeKickOutProtocol() //
                }
            }
        }

        // Aqui es lo interesante
        val finalAction = View.OnClickListener {
            // Cancelamos la trampa de los 10s
            kickOutJob?.cancel()
            executeKickOutProtocol()
        }

        btnNoDecidiEso.setOnClickListener(finalAction)
        btnQueQuieresDecir.setOnClickListener(finalAction)
    }

    // EXPULSIÓN
    private fun executeKickOutProtocol() {

        viewModel.finishOnboarding{
            findNavController().navigate(R.id.action_TutoHome_to_TutoConclusion)
            val serviceIntent = Intent(requireContext(), BlockerEngineService::class.java)
            requireContext().startForegroundService(serviceIntent)

            // LO EXPULSAMOS AL HOME DE ANDROID DE INMEDIATO
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)

        }

    }

    // En caso de que el usuario no presione el "entiendo"
    private fun startDynamicCountdown(
        textView: TextView,
        totalSeconds: Int,
        onFinish: (() -> Unit)? = null
    ) {
        countdownJob?.cancel()

        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            var timeInSeconds = totalSeconds

            while (isActive && timeInSeconds >= 0) {
                val minutes = timeInSeconds / 60
                val seconds = timeInSeconds % 60

                textView.text = String.format("00:%02d:%02d", minutes, seconds)

                if (timeInSeconds == 0) {
                    // Damos medio segundo para que el usuario alcance a ver el 00:00:00
                    delay(500)
                    // Disparamos la acción de auto-avance (si existe)
                    onFinish?.invoke()
                    break // Rompemos el ciclo
                }

                delay(1000) // Esperamos 1 segundo r
                timeInSeconds--
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
        kickOutJob?.cancel() // Matamos el contador de expulsión si sale del fragmento
    }
}