package host.senk.dosenk.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import host.senk.dosenk.ui.nav.AddMenuBottomSheet
import kotlinx.coroutines.launch

import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    // RELOJ DEL SISTEMA
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeRunnable: Runnable

    private lateinit var missionTimerManager: MissionTimerManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // To_do en orden, como un buen libro
        setupInsets(view)
        setupGradients(view)
        setupHeaderClock(view)
        setupEmergencyCard(view)
        setupFab(view)
        setupBottomNav(view)

        // DELEGAMOS LA TARJETA DE MISIÓN A SU MANAGER
        val cardsGrid = view.findViewById<View>(R.id.cards_grid)
        missionTimerManager = MissionTimerManager(cardsGrid, viewLifecycleOwner, requireContext())
        missionTimerManager.bindMissionState(viewModel.missionState)


    }


    // AGREGAR MISIONES Y RECORDATORIOS  / PROEWYECTO

    private fun setupFab(view: View) {
        val fabAdd = view.findViewById<View>(R.id.fabAddContainer)

        fabAdd.setOnClickListener {
            val bottomSheet = AddMenuBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddMenuBottomSheet")
        }
    }


    // CONFIGURACIONES DE UI DE PINTADOS Y ETC

    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupGradients(view: View) {
        view.findViewById<View>(R.id.stats)?.findViewById<View>(R.id.layoutStatsGradient)?.applyDoSenkGradient(cornerRadius = 20f)
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        view.findViewById<View>(R.id.cards_grid)?.findViewById<View>(R.id.layoutEmergencyGradient)?.applyDoSenkGradient(cornerRadius = 20f)
    }

    private fun setupHeaderClock(view: View) {
        val tvDate = view.findViewById<View>(R.id.stats).findViewById<TextView>(R.id.tvDate)
        val tvTime = view.findViewById<View>(R.id.stats).findViewById<TextView>(R.id.tvTime)
        val tvUser = view.findViewById<View>(R.id.header).findViewById<TextView>(R.id.tvUsername)
        val tvRankStat = view.findViewById<View>(R.id.stats).findViewById<TextView>(R.id.tvDisciplinaStatus)

        tvDate.text = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault()).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        timeRunnable = object : Runnable {
            override fun run() {
                tvTime.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timeRunnable)

        viewModel.currentUserAlias.observe(viewLifecycleOwner) { alias ->
            tvUser.text = "Bienvenido, @${alias}"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realRankName.collect { rank ->
                tvRankStat.text = rank
            }
        }
    }

    private fun setupEmergencyCard(view: View) {
        val cardsGrid = view.findViewById<View>(R.id.cards_grid)
        val cardEmergency = cardsGrid.findViewById<View>(R.id.cardEmergency)
        val tvStatus = cardsGrid.findViewById<TextView>(R.id.tvEmergencyTitle)

        viewModel.isEmergencyActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                tvStatus.text = "¡ACTIVADO!\nSOLO LLAMADAS"
                cardEmergency.alpha = 1.0f
            } else {
                tvStatus.text = "¡BLOQUEO DE\nEMERGENCIA!"
                cardEmergency.alpha = 0.9f
            }
        }
    }


    private fun setupBottomNav(view: View) {

        val navTimeline = view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_timeline)

        navTimeline?.setOnClickListener {
            // Viaje al Timeline
            findNavController().navigate(R.id.action_homeFragment_to_TimeLime)
        }

        val navBlocks = view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.nav_blocks)

        navBlocks?.setOnClickListener {
            // Viaje al blocks
            findNavController().navigate(R.id.action_homeFragment_to_BlockZone)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable)

        if (::missionTimerManager.isInitialized) {
            missionTimerManager.stopCountdown()
        }
    }
}