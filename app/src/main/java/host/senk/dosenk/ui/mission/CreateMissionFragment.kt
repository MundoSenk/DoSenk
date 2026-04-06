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
import host.senk.dosenk.ui.blocks.BlockZoneFragment
import host.senk.dosenk.util.requestAutoStartPermission

@AndroidEntryPoint
class CreateMissionFragment : Fragment(R.layout.fragment_create_mission) {

    private val viewModel: CreateMissionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  XML
        val rootLayout = view.findViewById<View>(R.id.createMissionRoot)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val etMissionName = view.findViewById<EditText>(R.id.etMissionName)
        val btnDatePicker = view.findViewById<TextView>(R.id.btnDatePicker)
        val btnTimePicker = view.findViewById<TextView>(R.id.btnTimePicker)
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
            val dialogView = layoutInflater.inflate(R.layout.dialog_duration_picker, null)
            val npHours = dialogView.findViewById<android.widget.NumberPicker>(R.id.npHours)
            val npMinutes = dialogView.findViewById<android.widget.NumberPicker>(R.id.npMinutes)

            npHours.minValue = 0
            npHours.maxValue = 23
            npMinutes.minValue = 0
            npMinutes.maxValue = 59

            val currentTime = viewModel.durationMinutes.value
            npHours.value = currentTime / 60
            npMinutes.value = currentTime % 60

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Tiempo Personalizado")
                .setView(dialogView)
                .setPositiveButton("Aceptar") { _, _ ->
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

        //  MODO EDICIÓN O CREACIÓN
        val missionId = arguments?.getString("missionId")
        val templateId = arguments?.getString("templateId")
        val btnDelete = view.findViewById<View>(R.id.btnDelete)

        if (templateId != null) {
            tvTitle.text = "Editar Rutina"
            btnNext.text = "¡Actualizar Rutina!"
            btnDelete.visibility = View.VISIBLE

            if (viewModel.currentEditingTemplateId != templateId) {
                viewModel.loadTemplateForEditing(templateId)
            }
        } else if (missionId != null) {
            tvTitle.text = "Editar Misión"
            btnNext.text = "¡Actualizar Misión!"
            btnDelete.visibility = View.VISIBLE

            if (viewModel.currentEditingMissionId != missionId) {
                viewModel.loadMissionForEditing(missionId)
            }
        } else {
            tvTitle.text = "Nueva Misión"
            btnNext.text = "¡A asignar el bloqueo!"
            btnDelete.visibility = View.GONE
        }


        btnDelete.setOnClickListener {
            val isRoutine = templateId != null
            val msg = if (isRoutine) "Vas a eliminar TODA LA RUTINA y sus misiones futuras. ¿Estás seguro, gallo cobarde?"
            else "¿Te vas a rajar y eliminar este castigo?"

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("¿Destruir misión?")
                .setMessage(msg)
                .setPositiveButton("Sí, soy débil") { _, _ ->
                    viewModel.deleteCurrentTask {
                        Toast.makeText(requireContext(), "Misión aniquilada.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
                .setNegativeButton("No, soy fuerte", null)
                .show()
        }

        // ESCUCHAMOS CUANDO LOS DATOS ESTÉN LISTOS PARA RELLENAR LOS TEXTOS
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.missionLoaded.collect { mission ->
                etMissionName.setText(mission.name)
                view.findViewById<EditText>(R.id.etMissionDescription).setText(mission.description)

                val sdf = SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                btnDatePicker.text = sdf.format(java.util.Date(mission.executionDate))
                btnDatePicker.applyDoSenkGradient(cornerRadius = 16f)
                btnDatePicker.setTextColor(requireContext().getColor(R.color.white))

                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = mission.executionDate }
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val minute = calendar.get(java.util.Calendar.MINUTE)
                val amPm = if (hour >= 12) "PM" else "AM"
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                btnTimePicker.text = String.format("%02d:%02d %s", hour12, minute, amPm)
                btnTimePicker.applyDoSenkGradient(cornerRadius = 16f)
                btnTimePicker.setTextColor(requireContext().getColor(R.color.white))
            }
        }

        ////////////////// HORA DE INICIOOOO
        btnTimePicker.setOnClickListener {
            val currentTime = java.util.Calendar.getInstance()
            val hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = currentTime.get(java.util.Calendar.MINUTE)

            val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                viewModel.setStartTime(selectedHour, selectedMinute)

                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val timeStr = String.format("%02d:%02d %s", hour12, selectedMinute, amPm)

                btnTimePicker.text = timeStr
                btnTimePicker.applyDoSenkGradient(cornerRadius = 16f)
                btnTimePicker.setTextColor(requireContext().getColor(R.color.white))
            }, hour, minute, false)

            timePicker.setTitle("¿A qué hora inicia el castigo?")
            timePicker.show()
        }

        ////////////////// DÍA ÚNICO (Calendario)
        btnDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Día de la misión")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { timestamp ->
                viewModel.setExecutionDate(timestamp)

                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

                val dateStr = sdf.format(Date(timestamp))
                btnDatePicker.text = dateStr
                btnDatePicker.applyDoSenkGradient(cornerRadius = 16f)
                btnDatePicker.setTextColor(requireContext().getColor(R.color.white))

                //  Si escoge un día único, limpiamos las repeticiones
                viewModel.clearRepeatDays()
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }



        //  Asumo que en tu ViewModel Lunes=1, Martes=2, ..., Domingo=7
        val repeatButtons = mapOf(
            1 to view.findViewById<TextView>(R.id.btnDay1), // Lunes
            2 to view.findViewById<TextView>(R.id.btnDay2), // Martes
            3 to view.findViewById<TextView>(R.id.btnDay3), // Miércoles
            4 to view.findViewById<TextView>(R.id.btnDay4), // Jueves
            5 to view.findViewById<TextView>(R.id.btnDay5), // Viernes
            6 to view.findViewById<TextView>(R.id.btnDay6), // Sábado
            7 to view.findViewById<TextView>(R.id.btnDay7)  // Domingo
        )

        // Observamos la lista de días seleccionados desde el ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedRepeatDays.collect { selectedDays ->

                // 1. Pintamos/Despintamos las bolitas según la lista
                repeatButtons.forEach { (dayIndex, button) ->
                    if (selectedDays.contains(dayIndex)) {
                        // Está seleccionado: lo pintamos con gradiente
                        button.applyDoSenkGradient(cornerRadius = 50f)
                        button.setTextColor(requireContext().getColor(R.color.white))
                    } else {
                        // No está seleccionado: fondo blanco normal
                        button.background = requireContext().getDrawable(R.drawable.bg_capsule_white)
                        button.setTextColor(requireContext().getColor(R.color.black))
                    }
                }

                // 2. MAGIA UX: Si hay días seleccionados, el botón "Día único" se apaga y se reinicia
                if (selectedDays.isNotEmpty()) {
                    btnDatePicker.text = "Inhabilitado"
                    btnDatePicker.background = requireContext().getDrawable(R.drawable.bg_dashed_border)
                    btnDatePicker.setTextColor(requireContext().getColor(R.color.black))
                    btnDatePicker.alpha = 0.5f // Lo hacemos semi-transparente
                    btnDatePicker.isEnabled = false
                } else {
                    // Si no hay repeticiones, revive el botón "Día único"
                    btnDatePicker.alpha = 1.0f
                    btnDatePicker.isEnabled = true

                    // Si ya tenía una fecha seleccionada, la volvemos a pintar, si no, lo dejamos gris
                    if (viewModel.executionDate.value != null) {
                        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        btnDatePicker.text = sdf.format(Date(viewModel.executionDate.value!!))
                        btnDatePicker.applyDoSenkGradient(cornerRadius = 16f)
                        btnDatePicker.setTextColor(requireContext().getColor(R.color.white))
                    } else {
                        btnDatePicker.text = "Día único"
                        btnDatePicker.background = requireContext().getDrawable(R.drawable.bg_dashed_border)
                        btnDatePicker.setTextColor(requireContext().getColor(R.color.black))
                    }
                }
            }
        }

        // Le damos vida a los clics de las bolitas
        repeatButtons.forEach { (dayIndex, button) ->
            button.setOnClickListener {
                viewModel.toggleRepeatDay(dayIndex)
            }
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


        // BOTÓN DE AVANZAR AL BLOQUEO
        btnNext.setOnClickListener {
            viewModel.missionName = etMissionName.text.toString()
            val etDesc = view.findViewById<EditText>(R.id.etMissionDescription)
            viewModel.missionDescription = etDesc.text.toString()

            if (viewModel.isFormValid()) {

                if (!viewModel.isTimeValid()) {
                    Toast.makeText(requireContext(), "No puedes programar misiones en el pasado, gallo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Función interna para revisar permisos antes de viajar
                fun checkCadenerosAndNavigate() {
                    val isAutoTime = android.provider.Settings.Global.getInt(requireContext().contentResolver, android.provider.Settings.Global.AUTO_TIME, 0) == 1
                    if (!isAutoTime) {
                        Toast.makeText(requireContext(), "¡Tramposo! Activa la 'Fecha y hora automática'.", Toast.LENGTH_LONG).show()
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_DATE_SETTINGS))
                        return
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(requireContext())) {
                        Toast.makeText(requireContext(), "¡>Do necesita permiso de sobreponerse para encerrarte!", Toast.LENGTH_LONG).show()
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${requireContext().packageName}")))
                        return
                    }

                    val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(requireContext(), "¡>Do necesita permiso de Alarmas!", Toast.LENGTH_LONG).show()
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        return
                    }

                    val bundle = android.os.Bundle().apply { putBoolean("isSelectionMode", true) }
                    findNavController().navigate(R.id.action_createMissionFragment_to_blockZoneFragment, bundle)
                }

                //  EL ESCUDO ANTI-CLONES Y RADAR
                viewLifecycleOwner.lifecycleScope.launch {

                    // Si NO es repetitiva, usamos el Radar
                    if (viewModel.selectedRepeatDays.value.isEmpty()) {
                        val exactStartMs = viewModel.calculateFinalTimestamp()
                        val collisionMsg = viewModel.radarCheck(exactStartMs, viewModel.durationMinutes.value)

                        if (collisionMsg != null) {

                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("¡Choque de Horarios!")
                                .setMessage("Esto choca con $collisionMsg. ¿Deseas sobreescribir este horario y forzar la misión?")
                                .setPositiveButton("Sí, Sobreescribir") { _, _ ->
                                    viewModel.isManualOverride = true // Guardamos la decisión en la mochila
                                    checkCadenerosAndNavigate()
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                            return@launch
                        }
                    }

                    // Si es repetitiva o si el radar dice que está libre:
                    viewModel.isManualOverride = false
                    checkCadenerosAndNavigate()
                }

            } else {
                Toast.makeText(requireContext(), "Ponle nombre y fecha (o días), gallo", Toast.LENGTH_SHORT).show()
            }
        }

        // BOTÓN REGRESAR
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}