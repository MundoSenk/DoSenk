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




        val btnTimePicker = view.findViewById<TextView>(R.id.btnTimePicker)

        ////////////////// HORA DE INICIOOOO
        btnTimePicker.setOnClickListener {
            val currentTime = java.util.Calendar.getInstance()
            val hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = currentTime.get(java.util.Calendar.MINUTE)

            val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                viewModel.setStartTime(selectedHour, selectedMinute)

                // Formateamos para que se vea bonito (ej. 05:09 PM)
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                val timeStr = String.format("%02d:%02d %s", hour12, selectedMinute, amPm)

                btnTimePicker.text = timeStr
                btnTimePicker.applyDoSenkGradient(cornerRadius = 16f)
                btnTimePicker.setTextColor(requireContext().getColor(R.color.white))
            }, hour, minute, false) // false = formato 12 hrs (AM/PM)

            timePicker.setTitle("¿A qué hora inicia el castigo?")
            timePicker.show()
        }


        ////////////////// DÍA (Calendario)
        btnDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Día de la misión")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { timestamp ->
                viewModel.setExecutionDate(timestamp)

                // lo debe leer en utc
                val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC") // <--- ¡LA LÍNEA MÁGICA!

                val dateStr = sdf.format(Date(timestamp))
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
            val etDesc = view.findViewById<EditText>(R.id.etMissionDescription)
            viewModel.missionDescription = etDesc.text.toString()

            if (viewModel.isFormValid()) {

                // CADENERO 0: NO PERMITIR MISIONES EN EL PASADO (Esto no ocupa base de datos)
                if (!viewModel.isTimeValid()) {
                    Toast.makeText(requireContext(), "No puedes programar misiones en el pasado, gallo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // 🚨 CADENERO 0.5: ESCUDO ANTI-CLONES (Requiere base de datos, así que lanzamos Corrutina)
                viewLifecycleOwner.lifecycleScope.launch {

                    // LE PREGUNTAMOS AL CEREBRO
                    if (viewModel.hasTimeConflict()) {
                        Toast.makeText(requireContext(), "¡Ya tienes una misión a esa hora! No puedes hacer dos cosas a la vez.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    // SI NO HAY CONFLICTO, SEGUIMOS CON LOS DEMÁS CADENEROS...

                    // CADENERO 1: EL TIEMPO AUTOMÁTICO (ANTI-VIAJEROS)
                    val isAutoTime = android.provider.Settings.Global.getInt(
                        requireContext().contentResolver,
                        android.provider.Settings.Global.AUTO_TIME, 0
                    ) == 1

                    if (!isAutoTime) {
                        Toast.makeText(requireContext(), "¡Tramposo! Activa la 'Fecha y hora automática' en tus ajustes para usar >Do.", Toast.LENGTH_LONG).show()
                        val intent = android.content.Intent(android.provider.Settings.ACTION_DATE_SETTINGS)
                        startActivity(intent)
                        return@launch
                    }

                    // CADENERO 2: PERMISO DE PANTALLA NEGRA
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (!android.provider.Settings.canDrawOverlays(requireContext())) {
                            Toast.makeText(requireContext(), "¡>Do necesita permiso de sobreponerse para encerrarte!", Toast.LENGTH_LONG).show()
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${requireContext().packageName}")
                            )
                            startActivity(intent)
                            return@launch
                        }
                    }

                    // CADENERO 3: PERMISO DE ALARMAS EXACTAS
                    val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (!alarmManager.canScheduleExactAlarms()) {
                            Toast.makeText(requireContext(), "¡>Do necesita permiso de Alarmas para castigarte a tiempo!", Toast.LENGTH_LONG).show()
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            startActivity(intent)
                            return@launch
                        }
                    }

                    // CADENERO 4: INICIO AUTOMÁTICO Y BATERÍA
                    val prefs = requireContext().getSharedPreferences("DoSenkPrefs", android.content.Context.MODE_PRIVATE)
                    val hasSeenAutoStart = prefs.getBoolean("hasSeenAutoStart", false)

                    if (!hasSeenAutoStart) {
                        prefs.edit().putBoolean("hasSeenAutoStart", true).apply()
                        Toast.makeText(requireContext(), "Importante: Para que las misiones funcionen, desactiva la optimización de batería para >Do.", Toast.LENGTH_LONG).show()
                        requestAutoStartPermission(requireContext())
                        return@launch
                    }

                    //  SI PASA TODOS LOS CADENEROS, ENTRA A LA ZONA DE BLOQUEO
                    val bundle = android.os.Bundle().apply {
                        putBoolean("isSelectionMode", true)
                    }
                    findNavController().navigate(R.id.action_createMissionFragment_to_blockZoneFragment, bundle)
                }

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