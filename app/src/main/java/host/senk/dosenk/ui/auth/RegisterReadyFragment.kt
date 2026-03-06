package host.senk.dosenk.ui.auth

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.ui.MainActivity

@AndroidEntryPoint
class RegisterReadyFragment : Fragment(R.layout.fragment_register_ready) {

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applySkin(view)

        val btnFinish = view.findViewById<Button>(R.id.btnFinish)
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val cbMental = view.findViewById<CheckBox>(R.id.cbMental)
        val btnBack = view.findViewById<TextView>(R.id.btnBack)

        // logica de validación de Checkboxes
        val checkListener = {
            val isReady = cbTerms.isChecked && cbMental.isChecked
            btnFinish.isEnabled = isReady
            btnFinish.alpha = if (isReady) 1.0f else 0.5f
        }

        cbTerms.setOnCheckedChangeListener { _, _ -> checkListener() }
        cbMental.setOnCheckedChangeListener { _, _ -> checkListener() }

        // Botón atrás (por si quiere revisar algo)
        btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        //  Ir al MainActivity
        btnFinish.setOnClickListener {
            btnFinish.isEnabled = false
            btnFinish.text = "¡Todo listo!"

            Toast.makeText(requireContext(), "¡Bienvenido a DoSenk!", Toast.LENGTH_SHORT).show()

            //  limpiar el stack de registro
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun applySkin(view: View) {
        val skinIndex = viewModel.userSkinIndex.value ?: 0
        val config = getSkinConfig(skinIndex)
        val context = requireContext()

        // Fondo Gradiente
        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(startColor, endColor)
        )
        view.findViewById<View>(R.id.rootLayout).background = gradient

        // Logo e Identidad
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)

        val accentColor = if(config.isDark) ContextCompat.getColor(context, R.color.dark_btn) else endColor
        val colorStateList = ColorStateList.valueOf(accentColor)

        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)

        // Botón
        val btnFinish = view.findViewById<Button>(R.id.btnFinish)
        btnFinish.backgroundTintList = colorStateList

        // Checkboxes con el color de la skin
        CompoundButtonCompat.setButtonTintList(view.findViewById(R.id.cbTerms), colorStateList)
        CompoundButtonCompat.setButtonTintList(view.findViewById(R.id.cbMental), colorStateList)

        // Pintar los pasos de la barra de progreso (todos hasta el 4)
        val steps = intArrayOf(R.id.step1, R.id.step2, R.id.step3, R.id.step4)
        steps.forEach { id ->
            view.findViewById<View>(id)?.setBackgroundColor(accentColor)
        }
    }

    private fun getSkinConfig(index: Int) = when(index) {
        1 -> SkinConfig(R.color.red_start, R.color.red_end, R.drawable.ic_logo_square_red)
        2 -> SkinConfig(R.color.dark_bg, R.color.dark_bg, R.drawable.ic_logo_square_dark, true)
        3 -> SkinConfig(R.color.teal_start, R.color.teal_end, R.drawable.ic_logo_square_teal)
        else -> SkinConfig(R.color.purple_start, R.color.purple_end, R.drawable.ic_logo_square_purple)
    }

    data class SkinConfig(val startColor: Int, val endColor: Int, val logoRes: Int, val isDark: Boolean = false)
}