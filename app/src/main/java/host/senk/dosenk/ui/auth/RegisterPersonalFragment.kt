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
class RegisterPersonalFragment : Fragment(R.layout.fragment_register_personal) {

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // recuperamos el color cone l que se quedo el carrusel
        val currentSkin = viewModel.userSkinIndex.value ?: 0

        // Pintar la UI con GRADIANTES y LOGOS correctos
        applySkin(view, currentSkin)

        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            findNavController().navigate(R.id.action_personal_to_account)
        }





    }

    private fun applySkin(view: View, skinIndex: Int) {
        val context = requireContext()

        // Obtenemos la configuración visual según el índice
        val config = getSkinConfig(skinIndex)

        // para piontar el gradiante del que inicia y cUlmina
        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, // O LEFT_RIGHT si prefieres como en el login
            intArrayOf(startColor, endColor)
        )

        view.findViewById<View>(R.id.rootLayout).background = gradient

        // logo que le corresponde
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)

        // pintamos textos y colores
        val accentColor = if(config.isDark) ContextCompat.getColor(context, R.color.dark_btn) else endColor

        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)
        view.findViewById<View>(R.id.step1).setBackgroundColor(accentColor) // Barra de progreso
        view.findViewById<Button>(R.id.btnNext).backgroundTintList = ColorStateList.valueOf(accentColor)
    }

    // "Base de datos" local de estilos para no sufrir con attrs en tiempo de ejecución
    private fun getSkinConfig(index: Int): SkinConfig {
        return when(index) {
            0 -> SkinConfig(R.color.purple_start, R.color.purple_end, R.drawable.ic_logo_square_purple) // Asegúrate de tener estas imágenes
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