package host.senk.dosenk.ui.dashboard

import android.content.Intent
import android.os.Bundle
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

    // Conectamos con el ViewModel
    private val sharedViewModel: SetupViewModel by activityViewModels()

    private lateinit var rvTopVices: RecyclerView
    private lateinit var tvStatsTitle: TextView
    private lateinit var tvStatsSubtitle: TextView
    private lateinit var cardPermission: CardView
    private lateinit var cardVices: CardView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnAcceptPunishment: Button
    private lateinit var tvVerdict: TextView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionAndLoadStats()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias XML
        rvTopVices = view.findViewById(R.id.rvTopVices)
        tvStatsTitle = view.findViewById(R.id.tvStatsTitle)
        tvStatsSubtitle = view.findViewById(R.id.tvStatsSubtitle)
        cardPermission = view.findViewById(R.id.cardPermission)
        cardVices = view.findViewById(R.id.cardVices)
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission)
        btnAcceptPunishment = view.findViewById(R.id.btnAcceptPunishment)
        tvVerdict = view.findViewById(R.id.tvVerdict)

        // Gradientes
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.cardVices)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 24f)

        rvTopVices.layoutManager = LinearLayoutManager(requireContext())

        btnGrantPermission.setOnClickListener {
            val intent = AppUsageManager.getPermissionSettingsIntent()
            requestPermissionLauncher.launch(intent)
        }

        btnAcceptPunishment.setOnClickListener {
            // para mandarlo sl a TutorialDashboardFragment
            findNavController().navigate(R.id.action_global_homeFragment)
        }

        checkPermissionAndLoadStats()
    }

    private fun checkPermissionAndLoadStats() {
        if (AppUsageManager.hasUsageStatsPermission(requireContext())) {
            showLoadingState()
            loadVices()
        } else {
            showPermissionState()
        }
    }

    private fun showPermissionState() {
        tvStatsTitle.text = "Analizando tu personalidad..."
        tvStatsSubtitle.text = "Para ser tu jefe, necesito ver tus trapos sucios. Dame acceso a tu tiempo de uso de tu pantallita."
        cardPermission.visibility = View.VISIBLE
        cardVices.visibility = View.GONE
        btnAcceptPunishment.visibility = View.GONE
        tvVerdict.visibility = View.GONE
    }

    private fun showLoadingState() {
        tvStatsTitle.text = "Calculando tu nivel de perdición..."
        tvStatsSubtitle.text = "Dame un segundo..."
        cardPermission.visibility = View.GONE
    }

    private fun loadVices() {
        // Le ordenamos al ViewModel que haga el cálculo pesado
        sharedViewModel.loadUsageStats(requireContext())

        // Nos sentamos a observar los resultados
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.usageReport.collect { report ->
                // Si el reporte ya no es nulo, significa que ya terminó de calcular
                if (report != null) {
                    // Sacamos las 5 apps de la caja
                    val topVicesList = report.topVices

                    if (topVicesList.isNotEmpty()) {
                        tvStatsTitle.text = "Resultados del Análisis"
                        tvStatsSubtitle.text = "Los números no mienten."

                        // Le pasamos la LISTA pura al Adapter
                        rvTopVices.adapter = TopVicesAdapter(topVicesList)

                        cardVices.visibility = View.VISIBLE
                        btnAcceptPunishment.visibility = View.VISIBLE
                        tvVerdict.visibility = View.VISIBLE

                        val worstApp = topVicesList[0]
                        tvVerdict.text = "¿${AppUsageManager.formatTime(worstApp.timeInForegroundMillis)} en ${worstApp.appName}?, Y así querías conquistar el mundo? Ay, pues a comenzar!"
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