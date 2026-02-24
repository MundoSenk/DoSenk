package host.senk.dosenk.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.AppUsageManager
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SetupStatsFragment : Fragment(R.layout.fragment_setup_stats) {

    private lateinit var rvTopVices: RecyclerView
    private lateinit var tvStatsTitle: TextView
    private lateinit var tvStatsSubtitle: TextView
    private lateinit var cardPermission: CardView
    private lateinit var cardVices: CardView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnAcceptPunishment: Button
    private lateinit var tvVerdict: TextView

    // Contrato para pedir el permiso y saber cuándo el usuario regresa a la app
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Cuando regrese de los ajustes, verificamos si ya nos dio el permiso
        checkPermissionAndLoadStats()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // xml
        rvTopVices = view.findViewById(R.id.rvTopVices)
        tvStatsTitle = view.findViewById(R.id.tvStatsTitle)
        tvStatsSubtitle = view.findViewById(R.id.tvStatsSubtitle)
        cardPermission = view.findViewById(R.id.cardPermission)
        cardVices = view.findViewById(R.id.cardVices)
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission)
        btnAcceptPunishment = view.findViewById(R.id.btnAcceptPunishment)
        tvVerdict = view.findViewById(R.id.tvVerdict)

        //  gradientes
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.cardVices)
            ?.findViewById<View>(R.id.layoutStatsGradient)
            ?.applyDoSenkGradient(cornerRadius = 24f)

        // Configurar RecyclerView
        rvTopVices.layoutManager = LinearLayoutManager(requireContext())

        // Botón para ir a los ajustes a dar el permiso
        btnGrantPermission.setOnClickListener {
            val intent = AppUsageManager.getPermissionSettingsIntent()
            requestPermissionLauncher.launch(intent)
        }

        // Botón final para ir al Home
        btnAcceptPunishment.setOnClickListener {
            findNavController().navigate(R.id.action_global_homeFragment)
        }

        // verificamos el permiso
        checkPermissionAndLoadStats()
    }

    private fun checkPermissionAndLoadStats() {
        if (AppUsageManager.hasUsageStatsPermission(requireContext())) {
            // Ya tenemos el permiso, cambiamos la UI y cargamos las stats
            showLoadingState()
            loadVices()
        } else {
            // No hay permiso, mostramos la tarjeta para pedirlo
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
        // Mantenemos lo demás oculto mientras carga
    }

    private fun loadVices() {
        // Lanzamos una corrutina en IO porque getTopVices consulta al sistema y puede ser pesado
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Obtenemos el top 5 de peores vicios
            val topVices = AppUsageManager.getTopVices(requireContext(), daysToLookBack = 3, topCount = 5)

            // Volvemos al hilo principal (Main) para actualizar la UI
            withContext(Dispatchers.Main) {
                if (topVices.isNotEmpty()) {
                    // Actualizamos la UI con los resultados
                    tvStatsTitle.text = "Resultados del Análisis"
                    tvStatsSubtitle.text = "Los números no mienten."

                    // Llenamos el RecyclerView con nuestro nuevo Adapter
                    rvTopVices.adapter = TopVicesAdapter(topVices)

                    // Mostramos la tarjeta y el botón de castigo
                    cardVices.visibility = View.VISIBLE
                    btnAcceptPunishment.visibility = View.VISIBLE

                    // Texto del sargento
                    tvVerdict.visibility = View.VISIBLE
                    val worstApp = topVices[0]
                    tvVerdict.text = "¿${AppUsageManager.formatTime(worstApp.timeInForegroundMillis)} en ${worstApp.appName}? Y así querías conquistar el mundo. Te voy a destruir esos vicios."
                } else {
                    // Caso raro: No usó el celular hoy
                    tvStatsTitle.text = "Vaya, vaya..."
                    tvStatsSubtitle.text = "Parece que eres un monje y no usaste apps hoy. Aún así, seré tu sargento."
                    btnAcceptPunishment.visibility = View.VISIBLE
                }
            }
        }
    }
}