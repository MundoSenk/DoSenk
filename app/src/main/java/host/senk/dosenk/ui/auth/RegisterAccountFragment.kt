package host.senk.dosenk.ui.auth

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R

@AndroidEntryPoint
class RegisterAccountFragment : Fragment(R.layout.fragment_register_account) {

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applySkin(view)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<Button>(R.id.btnNext).setOnClickListener {

            findNavController().navigate(R.id.action_account_to_verification)
        }
    }

    private fun applySkin(view: View) {
        val skinIndex = viewModel.userSkinIndex.value ?: 0
        val context = requireContext()
        val config = getSkinConfig(skinIndex)

        // para hacer el gradiante con el que empieza y culmina
        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )
        // se aplica a todo el fondo
        view.findViewById<View>(R.id.rootLayout).background = gradient

        // pintar el logo
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)

        // cual es el de los demas elementos
        val accentColor = if(config.isDark) ContextCompat.getColor(context, R.color.dark_btn) else endColor

        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)
        view.findViewById<Button>(R.id.btnNext).backgroundTintList = ColorStateList.valueOf(accentColor)

        // en que paso vamos
        view.findViewById<View>(R.id.step1).setBackgroundColor(accentColor)
        view.findViewById<View>(R.id.step2).setBackgroundColor(accentColor)
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