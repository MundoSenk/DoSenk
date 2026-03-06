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

        val btnBack = view.findViewById<TextView>(R.id.btnBack)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupObservers(btnNext)

        btnNext.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()
            val mail = etEmail.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty() || mail.isEmpty()) {
                Toast.makeText(requireContext(), "Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.username = user
            viewModel.email = mail
            viewModel.password = pass

            viewModel.startRegistration()
        }
    }

    private fun setupObservers(btnNext: Button) {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            btnNext.isEnabled = !isLoading
            btnNext.text = if (isLoading) "Procesando..." else "Siguiente!"
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.navigateToVerify.observe(viewLifecycleOwner) { email ->
            if (!email.isNullOrEmpty()) {
                try {
                    val controller = findNavController()
                    if (controller.currentDestination?.id == R.id.registerAccountFragment) {
                        controller.navigate(R.id.action_account_to_verification)
                        viewModel.clearNavigationToVerify()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applySkin(view: View) {
        val skinIndex = viewModel.userSkinIndex.value ?: 0
        val context = requireContext()
        val config = getSkinConfig(skinIndex)

        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )

        view.findViewById<View>(R.id.rootLayout).background = gradient
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)

        val accentColor = if (config.isDark) {
            ContextCompat.getColor(context, R.color.dark_btn)
        } else {
            endColor
        }

        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)
        view.findViewById<Button>(R.id.btnNext).backgroundTintList =
            ColorStateList.valueOf(accentColor)

        view.findViewById<View>(R.id.step1).setBackgroundColor(accentColor)
        view.findViewById<View>(R.id.step2).setBackgroundColor(accentColor)
    }

    private fun getSkinConfig(index: Int): SkinConfig {
        return when (index) {
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