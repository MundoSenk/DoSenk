package host.senk.dosenk.ui.mission

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CreateMissionFragment : Fragment(R.layout.fragment_create_mission) {

    private val viewModel: CreateMissionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  XML
        val rootLayout = view.findViewById<View>(R.id.createMissionRoot)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle) // Ya no lo pintamos
        val etMissionName = view.findViewById<EditText>(R.id.etMissionName)
        val btnDatePicker = view.findViewById<TextView>(R.id.btnDatePicker)
        val btnNext = view.findViewById<TextView>(R.id.btnNextToBlockZone)
        val btnCustomTime = view.findViewById<TextView>(R.id.btnCustomTime)

        rootLayout.applyDoSenkGradient()

        // Pintamos el botón final (este sí lleva degradado)
        btnNext.applyDoSenkGradient(cornerRadius = 24f)





        ///////////// SELECCION DE TIEMPOOOOOOOOOOOOO
        // CHIPS DE TIEMPO
        val timeButtons = mapOf(
            15 to view.findViewById<TextView>(R.id.btn15m),
            30 to view.findViewById<TextView>(R.id.btn30m),
            45 to view.findViewById<TextView>(R.id.btn45m),
            60 to view.findViewById<TextView>(R.id.btn60m)
        )
        val predefinedTimes = listOf(15, 30, 45, 60)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.durationMinutes.collect { selectedTime ->
                // Despintar todos
                timeButtons.forEach { (_, button) ->
                    button.background = requireContext().getDrawable(R.drawable.bg_capsule_white)
                    button.setTextColor(requireContext().getColor(R.color.black))
                }
                btnCustomTime.background = requireContext().getDrawable(R.drawable.bg_capsule_white)
                btnCustomTime.setTextColor(requireContext().getColor(R.color.black))
                btnCustomTime.text = "Personalizada"

                // Pintar el seleccionado
                if (predefinedTimes.contains(selectedTime)) {
                    timeButtons[selectedTime]?.applyDoSenkGradient(cornerRadius = 16f)
                    timeButtons[selectedTime]?.setTextColor(requireContext().getColor(R.color.white))
                } else if (selectedTime > 0) {
                    btnCustomTime.applyDoSenkGradient(cornerRadius = 16f)
                    btnCustomTime.setTextColor(requireContext().getColor(R.color.white))

                    val hours = selectedTime / 60
                    val mins = selectedTime % 60
                    if (hours > 0) {
                        btnCustomTime.text = "${hours}h ${mins}m"
                    } else {
                        btnCustomTime.text = "${mins} min"
                    }
                }
            }
        }

        timeButtons.forEach { (time, button) ->
            button.setOnClickListener { viewModel.setDuration(time) }
        }






        ////////////////// RELOJ TEMPORIZADOR
        btnCustomTime.setOnClickListener {
            // Inflamos el diseño de las rueditas que acabamos de crear
            val dialogView = layoutInflater.inflate(R.layout.dialog_duration_picker, null)
            val npHours = dialogView.findViewById<android.widget.NumberPicker>(R.id.npHours)
            val npMinutes = dialogView.findViewById<android.widget.NumberPicker>(R.id.npMinutes)

            //  Configuramos los límites de las rueditas
            npHours.minValue = 0
            npHours.maxValue = 23
            npMinutes.minValue = 0
            npMinutes.maxValue = 59

            //  Si el usuario ya tenía un tiempo elegido, que las rueditas empiecen ahí
            val currentTime = viewModel.durationMinutes.value
            npHours.value = currentTime / 60
            npMinutes.value = currentTime % 60

            //  Armamos el cuadro de diálogo
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Tiempo Personalizado")
                .setView(dialogView)
                .setPositiveButton("Aceptar") { _, _ ->
                    // Calculamos los minutos totales
                    val totalMinutes = (npHours.value * 60) + npMinutes.value

                    if (totalMinutes > 0) {
                        viewModel.setDuration(totalMinutes)
                    } else {
                        Toast.makeText(requireContext(), "Mínimo 1 minuto, ¿o que pretendes?", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }






        ////////////////// FECHA (Calendario)
        btnDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Día de la misión")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { timestamp ->
                viewModel.setExecutionDate(timestamp)
                val dateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
                btnDatePicker.text = dateStr
                btnDatePicker.applyDoSenkGradient(cornerRadius = 16f)
                btnDatePicker.setTextColor(requireContext().getColor(R.color.white))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }








        // ///////////// ASIGNACIÓN
        val btnAuto = view.findViewById<TextView>(R.id.btnAuto)
        val btnManual = view.findViewById<TextView>(R.id.btnManual)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assignmentType.collect { type ->
                if (type == "manual") {
                    btnManual.applyDoSenkGradient(cornerRadius = 24f)
                    btnManual.setTextColor(requireContext().getColor(R.color.white))
                    btnAuto.background = requireContext().getDrawable(R.drawable.bg_capsule_white)
                    btnAuto.setTextColor(requireContext().getColor(R.color.black))
                } else {
                    btnAuto.applyDoSenkGradient(cornerRadius = 24f)
                    btnAuto.setTextColor(requireContext().getColor(R.color.white))
                    btnManual.background = requireContext().getDrawable(R.drawable.bg_capsule_white)
                    btnManual.setTextColor(requireContext().getColor(R.color.black))
                }
            }
        }

        btnAuto.setOnClickListener { viewModel.setAssignmentType("auto") }
        btnManual.setOnClickListener { viewModel.setAssignmentType("manual") }

        // BOTÓN DE AVANZAR
        btnNext.setOnClickListener {
            viewModel.missionName = etMissionName.text.toString()
            if (viewModel.isFormValid()) {
                // findNavController().navigate(R.id.action_createMission_to_blockZone)
                Toast.makeText(requireContext(), " DECIDAMOS EL BLOQUEO", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Ponle nombre y fecha, gallo", Toast.LENGTH_SHORT).show()
            }
        }

        // BOTÓN REGRESAR
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}