package host.senk.dosenk.ui.dashboard

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
            tvUser.text = "Bienvenido, @${alias.uppercase()}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeRunnable) // Detener reloj al salir para no gastar batería
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}