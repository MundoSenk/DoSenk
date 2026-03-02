package host.senk.dosenk.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import host.senk.dosenk.util.AccessibilityUtils // PA LLEVARLO A AL ACCESIBILIDAD




@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    // Para el reloj
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timeRunnable: Runnable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ///MANEJO DE INSETS (Para que la cámara no tape el Header)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Así el fondo blanco llega hasta arriba, pero el contenido baja.
            v.updatePadding(top = insets.top, bottom = insets.bottom)

            WindowInsetsCompat.CONSUMED
        }




        // PINTAR GRADIENTES
        view.findViewById<View>(R.id.stats)?.findViewById<View>(R.id.layoutStatsGradient)?.applyDoSenkGradient(cornerRadius = 20f)
        view.findViewById<View>(R.id.bottomNav)?.findViewById<View>(R.id.layoutBottomGradient)?.applyDoSenkGradient()
        //header
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)


        //  REFERENCIAS A VISTAS
        val tvDate = view.findViewById<View>(R.id.stats).findViewById<TextView>(R.id.tvDate)
        val tvTime = view.findViewById<View>(R.id.stats).findViewById<TextView>(R.id.tvTime)
        val tvUser = view.findViewById<View>(R.id.header).findViewById<TextView>(R.id.tvUsername)



        // FECHA (Lunes 2 Febrero...)
        val sdfDate = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        tvDate.text = sdfDate.format(Date()).capitalize()

        //  RELOJ EN VIVO
        timeRunnable = object : Runnable {
            override fun run() {
                val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvTime.text = sdfTime.format(Date())
                handler.postDelayed(this, 1000) // Se actualiza cada segundo
            }
        }
        handler.post(timeRunnable)

        //  USUARIO REAL (Mayúsculas)
        viewModel.currentUserAlias.observe(viewLifecycleOwner) { alias ->

            // Ejemplo: "harold" -> "HAROLD"
            tvUser.text = "Bienvenido, @${alias}"
        }



        //REFERENCIAS AL card
        val cardsGrid = view.findViewById<View>(R.id.cards_grid)
        val cardEmergency = cardsGrid.findViewById<View>(R.id.cardEmergency)
        val layoutEmergency = cardsGrid.findViewById<View>(R.id.layoutEmergencyGradient)
        val tvStatus = cardsGrid.findViewById<TextView>(R.id.tvEmergencyTitle)

        //pintado del boton de emergencia
        layoutEmergency.applyDoSenkGradient(cornerRadius = 20f)

        //ESCUCHAR EL CLICK del boton de emergencia
        cardEmergency.setOnClickListener {

            // A. Verificamos si tiene el permiso
            if (AccessibilityUtils.isServiceEnabled(requireContext())) {
                // TIENE PERMISO ACTIVAMOS Y DESACTIVAMOS
                viewModel.toggleEmergencyMode()
            } else {
                // NO TIENE PERMISO -> Lo mandamos a configurar
                showPermissionDialog()
            }
        }

        // OBSERVAR EL CAMBIO DE ESTADO (REACTIVO)
        viewModel.isEmergencyActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {

                tvStatus.text = "¡ACTIVADO!\nSOLO LLAMADAS"
                cardEmergency.alpha = 1.0f

                // AQUÍ DISPARAREMOS EL SERVICIO DE ACCESIBILIDAD DESPUÉS
            } else {
                // MODO NORMAL 😌
                tvStatus.text = "¡BLOQUEO DE\nEMERGENCIA!"
                cardEmergency.alpha = 0.9f // Un poco apagado si quieres
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable) // Detener reloj al salir para no gastar batería
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }


    private fun showPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permiso Requerido ")
            .setMessage("Para bloquear aplicaciones y que te concentres de verdad, necesito permiso de Accesibilidad.\n\nBusca 'Do Senk' en la lista y actívalo.")
            .setPositiveButton("IR A ACTIVAR") { _, _ ->
                // INTENT MÁGICO: Lo lleva directo a la configuración
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}