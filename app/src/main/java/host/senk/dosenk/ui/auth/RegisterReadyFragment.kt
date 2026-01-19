package host.senk.dosenk.ui.auth

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R

@AndroidEntryPoint
class RegisterReadyFragment : Fragment(R.layout.fragment_register_ready) {

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applySkin(view)

        val btnFinish = view.findViewById<Button>(R.id.btnFinish)
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val cbMental = view.findViewById<CheckBox>(R.id.cbMental)

        // Lógica de validación: Ambos deben estar marcados
        val checkListener = {
            val isReady = cbTerms.isChecked && cbMental.isChecked
            btnFinish.isEnabled = isReady
            btnFinish.alpha = if (isReady) 1.0f else 0.5f
        }

        cbTerms.setOnCheckedChangeListener { _, _ -> checkListener() }
        cbMental.setOnCheckedChangeListener { _, _ -> checkListener() }

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        btnFinish.setOnClickListener {

            // Guardar en DataStore
            viewModel.completeRegistration()
            // AQUÍ TERMINA EL REGISTRO
            // TODO: Enviar datos al backend
            Toast.makeText(context, "¡Bienvenido al infierno de la productividad!", Toast.LENGTH_LONG).show()



            findNavController().navigate(R.id.action_ready_to_dashboard)
            requireActivity().recreate()

        }
    }

    private fun applySkin(view: View) {
        val skinIndex = viewModel.userSkinIndex.value ?: 0
        val context = requireContext()
        val config = getSkinConfig(skinIndex)

        // GRADIENTE
        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )
        view.findViewById<View>(R.id.rootLayout).background = gradient

        // LOGO
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)


        val accentColor = if(config.isDark) ContextCompat.getColor(context, R.color.dark_btn) else endColor

        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)
        view.findViewById<Button>(R.id.btnFinish).backgroundTintList = ColorStateList.valueOf(accentColor)

        // PINTAR CHECKBOXES
        val colorStateList = ColorStateList.valueOf(accentColor)
        CompoundButtonCompat.setButtonTintList(view.findViewById(R.id.cbTerms), colorStateList)
        CompoundButtonCompat.setButtonTintList(view.findViewById(R.id.cbMental), colorStateList)

        // los pasos pinatdos
        view.findViewById<View>(R.id.step1).setBackgroundColor(accentColor)
        view.findViewById<View>(R.id.step2).setBackgroundColor(accentColor)
        view.findViewById<View>(R.id.step3).setBackgroundColor(accentColor)
        view.findViewById<View>(R.id.step4).setBackgroundColor(accentColor)
    }

    private fun getSkinConfig(index: Int): SkinConfig {
        return when(index) {
            0 -> SkinConfig(R.color.purple_start, R.color.purple_end, R.drawable.ic_logo_square_purple)
            1 -> SkinConfig(R.color.red_start, R.color.red_end, R.drawable.ic_logo_square_red)
            2 -> SkinConfig(R.color.dark_bg, R.color.dark_bg, R.drawable.ic_logo_square_dark, isDark = true)
            3 -> SkinConfig(R.color.teal_start, R.color.teal_end, R.drawable.ic_logo_square_teal)
            else -> SkinConfig(R.color.purple_start, R.color.purple_end, R.drawable.ic_logo_square_purple)
        }
    }

    data class SkinConfig(
        val startColor: Int,
        val endColor: Int,
        val logoRes: Int,
        val isDark: Boolean = false
    )
}