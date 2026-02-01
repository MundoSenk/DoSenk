package host.senk.dosenk.ui.auth

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

        val btnNext = view.findViewById<Button>(R.id.btnNext)

        btnNext.setOnClickListener {
            //  CAPTURAR DATOS
            val user = view.findViewById<EditText>(R.id.etUsername).text.toString().trim()
            val pass = view.findViewById<EditText>(R.id.etPassword).text.toString().trim()
            val confirm = view.findViewById<EditText>(R.id.etConfirmPassword).text.toString().trim()
            val email = view.findViewById<EditText>(R.id.etEmail).text.toString().trim()

            //  VALIDACIONES LOCALES (Lo básico)
            if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
                Toast.makeText(context, "Faltan datos clave", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != confirm) {
                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //  VALIDACIÓN REMOTA

            // Bloqueamos el botón para que no le piquen 20 veces
            btnNext.isEnabled = false
            btnNext.text = "Verificando..."

            //  Llamamos al ViewModel (que llama al Repo -> PHP -> MySQL)
            viewModel.validateAccountAvailability(user, email) { isAvailable, message ->

                // Ya respondió el server, reactivamos botón
                btnNext.isEnabled = true
                btnNext.text = "Siguiente!"

                if (isAvailable) {
                    // Guardamos la contraseña en la mochila (el user y mail ya se guardaron en el VM)
                    viewModel.password = pass

                    // Avanzamos a la siguiente pantalla
                    findNavController().navigate(R.id.action_account_to_verification)
                } else {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    // MAS TARDE LO marcamos en rojo
                }
            }
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