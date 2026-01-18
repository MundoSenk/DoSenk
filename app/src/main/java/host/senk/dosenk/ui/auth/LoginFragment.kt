package host.senk.dosenk.ui.auth

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import host.senk.dosenk.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import androidx.navigation.fragment.findNavController //Pa la navegacion

import androidx.fragment.app.activityViewModels // pa paserle l datp

class LoginFragment : Fragment() {

    // Variables para controlar el carrusel
    private var carouselJob: Job? = null
    private var currentSkinIndex = 0

    // Variable para recordar el color de acento actual y poder animar desde él
    private var currentAccentColor: Int = 0


    private val registerViewModel: RegistrationViewModel by activityViewModels()

    // Referencias a las vistas (Views)
    private lateinit var backgroundView: View
    private lateinit var ivLogo: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvForgot: TextView
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    // Definimos los colores de las skin, aqui el pintado debe ser manual si o si
    // Estructura: [StartColor, EndColor, LogoRes, TextColor]
    private val skins by lazy {
        listOf(
            // 0
            SkinData(R.color.purple_start, R.color.purple_end, R.drawable.logo_purple),
            // 1
            SkinData(R.color.red_start, R.color.red_end, R.drawable.logo_red),
            // 2
            SkinData(R.color.dark_bg, R.color.dark_bg, R.drawable.logo_dark, isDark = true),
            // 3
            SkinData(R.color.teal_start, R.color.teal_end, R.drawable.logo_teal)
        )
    }

    data class SkinData(
        val startColorRes: Int,
        val endColorRes: Int,
        val logoRes: Int,
        val isDark: Boolean = false
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // Inicializamos lo que vamos a cambiar del xml
        backgroundView = view.findViewById(R.id.background_view)
        ivLogo = view.findViewById(R.id.ivLogo)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvForgot = view.findViewById(R.id.tvForgot)
        btnLogin = view.findViewById(R.id.btnLogin)
        tvRegister = view.findViewById(R.id.tvRegister)

        // Pa capturar el color con el que se quedara para el registro
        tvRegister.setOnClickListener {
            saveCurrentThemeAndContinue()
        }

        // boton para el login
        btnLogin.setOnClickListener {

        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // llamamos el aplicar skin de una pa que no se quede hueco
        applySkin(skins[currentSkinIndex], animate = false)

        //iniciar el carrusel con un delay asemejando los 5 segundos pa que el usuario pueda verlo
        carouselJob = lifecycleScope.launch {
            delay(4000) /// dejamos 4 pa que los 5 normales se sientan todos igual jajja
            while (isActive) {
                currentSkinIndex = (currentSkinIndex + 1) % skins.size
                applySkin(skins[currentSkinIndex], animate = true)
                delay(4000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopCarousel() //esto conserva la baetria del usuario si se mueve
    }



    private fun stopCarousel() {
        carouselJob?.cancel()
    }

    ///Esta e sla magia de la animacion
    private fun applySkin(skin: SkinData, animate: Boolean = true) {
        val context = requireContext()
        val startColor = ContextCompat.getColor(context, skin.startColorRes)
        val endColor = ContextCompat.getColor(context, skin.endColorRes)

        // Determinar el color de acento destino
        val targetAccentColor = if (skin.isDark) {
            ContextCompat.getColor(context, R.color.dark_btn) // Asegúrate de tener este color definido
        } else {
            endColor
        }

        // manejo de los gardiebrs
        val newGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )

        if (animate && backgroundView.background != null) {
            // trabnsicion suave
            val oldBackground = backgroundView.background
            val transition = android.graphics.drawable.TransitionDrawable(
                arrayOf(oldBackground, newGradient)
            )
            backgroundView.background = transition
            transition.startTransition(800) // la suavidad
        } else {

            backgroundView.background = newGradient
        }

        // para el logo es diferente es un apagar prender o de menos a mas o como se llame
        // en español el fade out jaja
        if (animate) {
            ivLogo.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    ivLogo.setImageResource(skin.logoRes)
                    ivLogo.animate().alpha(1f).setDuration(300).start()
                }.start()
        } else {
            ivLogo.setImageResource(skin.logoRes)
        }

        // intercambio de lso colores
        if (animate && currentAccentColor != 0) {
            val colorAnimator = ValueAnimator.ofArgb(currentAccentColor, targetAccentColor)
            colorAnimator.duration = 800 // debe ser el mismo que el de la suavidad
            colorAnimator.addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                // Se bsuca el intermedio para que s enoet naturalita, asi como las que no estan operadas...
                tvTitle.setTextColor(color)
                tvForgot.setTextColor(color)
                btnLogin.backgroundTintList = android.content.res.ColorStateList.valueOf(color)

                // etUser.setHintTextColor(color) creo quese ve bien sin esta
            }
            colorAnimator.start()
        } else {
            // Cambio directo
            tvTitle.setTextColor(targetAccentColor)
            tvForgot.setTextColor(targetAccentColor)
            btnLogin.backgroundTintList = android.content.res.ColorStateList.valueOf(targetAccentColor)
        }

        // para nuestro while se prepare pal proximo cambio
        currentAccentColor = targetAccentColor
    }

    private fun saveCurrentThemeAndContinue() {

        // Cuando el usuario toca el de registro le detenemos el carrusel.
        stopCarousel()

        //Se guarda el color del tema!
        registerViewModel.setSkin(currentSkinIndex)
        // Para que la siguiente Activity sepa qué tema usar (R.style.Theme_DoSenk_Red, etc).
        findNavController().navigate(R.id.action_login_to_register)

    }
}