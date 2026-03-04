package host.senk.dosenk.ui.mission

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CreateMissionViewModel @Inject constructor() : ViewModel() {

    // Slibgeton como mochilita para guardar datos antes de hacer una mision en la room

    var missionName = ""

    private val _durationMinutes = MutableStateFlow(45)
    val durationMinutes: StateFlow<Int> = _durationMinutes

    private val _executionDate = MutableStateFlow<Long?>(null) //
    val executionDate: StateFlow<Long?> = _executionDate

    private val _assignmentType = MutableStateFlow("manual") //
    val assignmentType: StateFlow<String> = _assignmentType

    // FUNCIONES PARA ACTUALIZAR LA MOCHILA

    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes
    }

    fun setExecutionDate(timestamp: Long) {
        _executionDate.value = timestamp
    }

    fun setAssignmentType(type: String) {
        _assignmentType.value = type
    }

    // Validar antes de pasar a la Zona de Bloqueos
    fun isFormValid(): Boolean {
        return missionName.isNotBlank() && _executionDate.value != null
    }
}