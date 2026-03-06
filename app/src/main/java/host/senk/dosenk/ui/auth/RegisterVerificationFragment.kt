package host.senk.dosenk.ui.auth

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R

@AndroidEntryPoint
class RegisterVerificationFragment : Fragment(R.layout.fragment_register_verification) {

    private val viewModel: RegistrationViewModel by activityViewModels()
    private var countDownTimer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etOtp = view.findViewById<EditText>(R.id.etOtp)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val tvResend = view.findViewById<TextView>(R.id.tvResendTimer)
        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)

        applySkin(view, btnNext)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        btnNext.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa el código", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnNext.isEnabled = false
            btnNext.text = "Verificando..."

            viewModel.verifyCode(code) { success, message ->
                btnNext.isEnabled = true
                btnNext.text = "Siguiente"
                if (success) {
                    findNavController().navigate(R.id.action_verification_to_ready)
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            tvUserEmail.text = email
        }

        startOtpTimer(tvResend)
    }

    private fun startOtpTimer(timerTextView: TextView) {
        timerTextView.isEnabled = false
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Reenviar en 00:${millisUntilFinished / 1000}"
            }
            override fun onFinish() {
                timerTextView.isEnabled = true
                timerTextView.text = "Reenviar código"
                timerTextView.setOnClickListener {
                    viewModel.resendOtp { success, message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        if (success) startOtpTimer(timerTextView)
                    }
                }
            }
        }.start()
    }

    private fun applySkin(view: View, btnNext: Button) {
        val skinIndex = viewModel.userSkinIndex.value ?: 0
        val config = getSkinConfig(skinIndex)
        val context = requireContext()

        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)

        view.findViewById<View>(R.id.rootLayout).background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(startColor, endColor)
        )
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)

        val accentColor = if(config.isDark) ContextCompat.getColor(context, R.color.dark_btn) else endColor
        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)
        btnNext.backgroundTintList = ColorStateList.valueOf(accentColor)

        intArrayOf(R.id.step1, R.id.step2, R.id.step3).forEach {
            view.findViewById<View>(it)?.setBackgroundColor(accentColor)
        }
    }

    private fun getSkinConfig(index: Int) = when(index) {
        1 -> SkinConfig(R.color.red_start, R.color.red_end, R.drawable.ic_logo_square_red)
        2 -> SkinConfig(R.color.dark_bg, R.color.dark_bg, R.drawable.ic_logo_square_dark, true)
        3 -> SkinConfig(R.color.teal_start, R.color.teal_end, R.drawable.ic_logo_square_teal)
        else -> SkinConfig(R.color.purple_start, R.color.purple_end, R.drawable.ic_logo_square_purple)
    }

    data class SkinConfig(val startColor: Int, val endColor: Int, val logoRes: Int, val isDark: Boolean = false)

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}