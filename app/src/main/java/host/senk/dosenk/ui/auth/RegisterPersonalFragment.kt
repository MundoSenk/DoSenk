package host.senk.dosenk.ui.auth

import android.app.DatePickerDialog
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
import java.util.Calendar

@AndroidEntryPoint
class RegisterPersonalFragment : Fragment(R.layout.fragment_register_personal) {

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recuperamos el color con el que se quedó el carrusel
        val currentSkin = viewModel.userSkinIndex.value ?: 0
        applySkin(view, currentSkin)

        val etBirthDate = view.findViewById<EditText>(R.id.etBirthDate)


        etBirthDate.setOnClickListener {
            //  Obtener fecha actual para iniciar el calendario
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Abrir el DatePickerDialog
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formatear la fecha para MySQL (YYYY-MM-DD)
                    // Nota: Los meses en Java empiezan en 0, por eso sumamos 1
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    etBirthDate.setText(formattedDate)
                },
                year,
                month,
                day
            )
            //  Limitar para que no elijan fechas futuras
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }


        view.findViewById<Button>(R.id.btnNext).setOnClickListener {
            // 1. CAPTURAR DATOS
            val name = view.findViewById<EditText>(R.id.etName).text.toString().trim()
            val last = view.findViewById<EditText>(R.id.etLastName).text.toString().trim()
            val birth = view.findViewById<EditText>(R.id.etBirthDate).text.toString().trim()

            //  VALIDAR
            if (name.isEmpty() || last.isEmpty() || birth.isEmpty()) {
                Toast.makeText(context, "Llena tus datos, gallo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //  GUARDAR EN MOCHILA
            viewModel.firstName = name
            viewModel.lastName = last
            viewModel.birthDate = birth

            //  AVANZAR
            findNavController().navigate(R.id.action_personal_to_account)
        }
    }

    private fun applySkin(view: View, skinIndex: Int) {
        val context = requireContext()
        val config = getSkinConfig(skinIndex)

        // Gradiante
        val startColor = ContextCompat.getColor(context, config.startColor)
        val endColor = ContextCompat.getColor(context, config.endColor)
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )
        view.findViewById<View>(R.id.rootLayout).background = gradient

        // Logo
        view.findViewById<ImageView>(R.id.ivHeaderLogo).setImageResource(config.logoRes)

        // Textos y Colores
        val accentColor = if(config.isDark) ContextCompat.getColor(context, R.color.dark_btn) else endColor

        view.findViewById<TextView>(R.id.tvSectionTitle).setTextColor(accentColor)
        view.findViewById<View>(R.id.step1).setBackgroundColor(accentColor)
        view.findViewById<Button>(R.id.btnNext).backgroundTintList = ColorStateList.valueOf(accentColor)
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
        val startColor: Int, val endColor: Int, val logoRes: Int, val isDark: Boolean = false
    )
}