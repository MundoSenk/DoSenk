package host.senk.dosenk.ui.onboarding

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TutorialBlockSelectionFragment : Fragment(R.layout.fragment_tutorial_block_selection) {

    private val viewModel: TutorialMissionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  PINTAR GRADIENTES DEL TEMA
        view.findViewById<View>(R.id.header)
            ?.findViewById<View>(R.id.layoutLogoGradient)
            ?.applyDoSenkGradient(cornerRadius = 12f)

        view.findViewById<View>(R.id.bottomNav)
            ?.findViewById<View>(R.id.layoutBottomGradient)
            ?.applyDoSenkGradient()

        view.findViewById<View>(R.id.bannerZonaBloqueo)?.applyDoSenkGradient(cornerRadius = 20f)
        view.findViewById<View>(R.id.bgHumano)?.applyDoSenkGradient(cornerRadius = 0f)
        view.findViewById<View>(R.id.bgDios)?.applyDoSenkGradient(cornerRadius = 0f)

        //  MODIFICAR EL HEADER
        val tvHeaderUsername = view.findViewById<View>(R.id.header)?.findViewById<TextView>(R.id.tvUsername)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.realAlias.collect { alias ->
                tvHeaderUsername?.text = "Bloqueos, $alias"
            }
        }

        // FUERZA BRUTA DE Z-INDEX PARA RESALTAR DIOS
        val dimOverlay = view.findViewById<View>(R.id.dimOverlay)
        val cardDios = view.findViewById<View>(R.id.cardDios)
        val bottomNav = view.findViewById<View>(R.id.bottomNav)

        // El Nav se queda atrás
        bottomNav?.translationZ = 0f
        // La tarjeta de Dios salta al frente
        cardDios?.translationZ = 120f


        // ANIMACIÓN DEL LATIDO (Seducción Infinita)
        val btnEscogelo = view.findViewById<View>(R.id.btnEscogelo)
        val layoutConfirmationPopup = view.findViewById<View>(R.id.layoutConfirmationPopup)
        val btnConfirmYes = view.findViewById<View>(R.id.btnConfirmYes)

        var pulseAnim = ObjectAnimator.ofPropertyValuesHolder(
            btnEscogelo,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f)
        ).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }

        //  ACCIÓN FINAL
        btnEscogelo.setOnClickListener {
            // Apagamos el latido viejo
            pulseAnim.cancel()
            btnEscogelo.scaleX = 1f
            btnEscogelo.scaleY = 1f

            // el popup saleee
            layoutConfirmationPopup.visibility = View.VISIBLE

            // Prendemos el latido en el botón de Sí
            pulseAnim = ObjectAnimator.ofPropertyValuesHolder(
                btnConfirmYes,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f)
            ).apply {
                duration = 800
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }

        btnConfirmYes.setOnClickListener {
            findNavController().navigate(R.id.action_BlockZoneTuto_to_TutoHome)
        }
    }
}