package host.senk.dosenk.ui.home

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import host.senk.dosenk.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class MissionTimerManager(
    private val view: View,
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context
) {
    private var countdownJob: Job? = null

    // RXML
    private val tvTitle: TextView = view.findViewById(R.id.tvNextMissionTitle)
    private val tvTimer: TextView = view.findViewById(R.id.tvNextMissionTimer)
    private val tvSubtitle: TextView = view.findViewById(R.id.tvNextMissionSubtitle)
    private val btnRevisar: View = view.findViewById(R.id.btnCheckTask)

    // El HomeFragment inyecta el Estado reactivo aquí
    fun bindMissionState(missionState: StateFlow<MissionCardState>) {
        lifecycleOwner.lifecycleScope.launch {
            missionState.collect { state ->
                handleStateChange(state)
            }
        }
    }

    private fun handleStateChange(state: MissionCardState) {
        when (state) {
            is MissionCardState.Idle -> {
                tvTitle.text = "SIN MISIONES"
                tvTimer.text = "--:--:--"
                tvSubtitle.text = "Todo tranquilo por ahora."
                btnRevisar.visibility = View.VISIBLE
                stopCountdown()
            }
            is MissionCardState.Pending -> {
                tvTitle.text = "PRÓXIMA\nTAREA EN:"
                tvSubtitle.text = "¿Listo para:\n${state.missionName}?"
                btnRevisar.visibility = View.VISIBLE
                startCountdown(state.secondsUntilStart)
            }
            is MissionCardState.Active -> {
                tvTitle.text = "¡MISIÓN\nACTIVA!"
                tvSubtitle.text = "Bloqueo: ${state.blockType}"
                btnRevisar.visibility = View.GONE // Ocultamos el botón porque está en castigo
                startCountdown(state.secondsLeft)
            }
        }
    }

    private fun startCountdown(totalSeconds: Int) {
        stopCountdown()

        countdownJob = lifecycleOwner.lifecycleScope.launch {
            var timeInSeconds = totalSeconds

            while (isActive && timeInSeconds >= 0) {
                val hours = timeInSeconds / 3600
                val minutes = (timeInSeconds % 3600) / 60
                val seconds = timeInSeconds % 60

                if (hours > 0) {
                    tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    tvTimer.text = String.format("00:%02d:%02d", minutes, seconds)
                }

                if (timeInSeconds == 0) break

                delay(1000)
                timeInSeconds--
            }
        }
    }

    fun stopCountdown() {
        countdownJob?.cancel()
    }
}