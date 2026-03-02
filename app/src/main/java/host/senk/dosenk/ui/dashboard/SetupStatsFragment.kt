package host.senk.dosenk.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.AppUsageManager
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupStatsFragment : Fragment(R.layout.fragment_setup_stats) {

    private val sharedViewModel: SetupViewModel by activityViewModels()

    private lateinit var rvTopVices: RecyclerView
    private lateinit var tvStatsTitle: TextView
    private lateinit var tvStatsSubtitle: TextView
    private lateinit var cardPermission: CardView
    private lateinit var cardVices: CardView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnAcceptPunishment: Button
    private lateinit var tvVerdict: TextView

    // Este launcher nos sirve para AMBOS permisos. Cada vez que regresa de ajustes, volvemos a checar.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionAndLoadStats()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTopVices = view.findViewById(R.id.rvTopVices)
        tvStatsTitle = view.findViewById(R.id.tvStatsTitle)
        tvStatsSubtitle = view.findViewById(R.id.tvStatsSubtitle)
        cardPermission = view.findViewById(R.id.cardPermission)
        cardVices = view.findViewById(R.id.cardVices)
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission)
        btnAcceptPunishment = view.findViewById(R.id.btnAcceptPunishment)
        tvVerdict = view.findViewById(R.id.tvVerdict)

        view.findViewById<View>(R.id.header)?.findViewById<View>(R.id.layoutLogoGradient)?.applyDoSenkGradient(cornerRadius = 12f)
        view.findViewById<View>(R.id.cardVices)?.findViewById<View>(R.id.layoutStatsGradient)?.applyDoSenkGradient(cornerRadius = 24f)

        rvTopVices.layoutManager = LinearLayoutManager(requireContext())

        //  Decide qué permiso pedir
        btnGrantPermission.setOnClickListener {
            if (!AppUsageManager.hasUsageStatsPermission(requireContext())) {
                // Pedimos el de Uso de Apps
                val intent = AppUsageManager.getPermissionSettingsIntent()
                requestPermissionLauncher.launch(intent)
            } else if (!Settings.canDrawOverlays(requireContext())) {
                // Pedimos el de Ventanas Flotantes
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                requestPermissionLauncher.launch(intent)
            }
        }

        btnAcceptPunishment.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_tutorialDashboardFragment)
        }

        checkPermissionAndLoadStats()
    }

    private fun checkPermissionAndLoadStats() {
        val hasUsageStats = AppUsageManager.hasUsageStatsPermission(requireContext())
        val hasOverlay = Settings.canDrawOverlays(requireContext())

        // Si ya tiene los dos, arrancamos la magia
        if (hasUsageStats && hasOverlay) {
            showLoadingState()
            loadVices()
        } else {
            showPermissionState(hasUsageStats, hasOverlay)
        }
    }

    private fun showPermissionState(hasUsageStats: Boolean, hasOverlay: Boolean) {
        cardPermission.visibility = View.VISIBLE
        cardVices.visibility = View.GONE
        btnAcceptPunishment.visibility = View.GONE
        tvVerdict.visibility = View.GONE

        // Cambiamos el texto de la tarjeta dependiendo de lo que falte
        if (!hasUsageStats) {
            tvStatsTitle.text = "Analizando tu personalidad..."
            tvStatsSubtitle.text = "Para ser tu jefe, necesito ver tus trapos sucios. Dame acceso a tu tiempo de uso de tu pantallita."
            btnGrantPermission.text = "¡Revisar mi celular!"
        } else if (!hasOverlay) {
            tvStatsTitle.text = "Un último detalle..."
            tvStatsSubtitle.text = "Para poder castigarte, necesito el poder de aparecer sobre otras apps. ¡Dame el control!"
            btnGrantPermission.text = "¡Dar poder absoluto!"
        }
    }

    private fun showLoadingState() {
        tvStatsTitle.text = "Calculando tu nivel de perdición..."
        tvStatsSubtitle.text = "Dame un segundo..."
        cardPermission.visibility = View.GONE
    }

    private fun loadVices() {
        sharedViewModel.loadUsageStats(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.usageReport.collect { report ->
                if (report != null) {
                    val topVicesList = report.topVices
                    if (topVicesList.isNotEmpty()) {
                        tvStatsTitle.text = "Resultados del Análisis"
                        tvStatsSubtitle.text = "Los números no mienten."
                        rvTopVices.adapter = TopVicesAdapter(topVicesList)

                        cardVices.visibility = View.VISIBLE
                        btnAcceptPunishment.visibility = View.VISIBLE
                        tvVerdict.visibility = View.VISIBLE

                        val worstApp = topVicesList[0]
                        tvVerdict.text = "¿${AppUsageManager.formatTime(worstApp.timeInForegroundMillis)} en ${worstApp.appName}?, ¿Y así querías conquistar el mundo? Ay, pues a comenzar!"
                    } else {
                        tvStatsTitle.text = "Vaya, vaya..."
                        tvStatsSubtitle.text = "Parece que eres un monje y no usaste apps hoy."
                        btnAcceptPunishment.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}