package host.senk.dosenk.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.remote.model.ScheduleData
import host.senk.dosenk.data.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // ESTADOS (Qué seleccionó)
    var isStudent = false
    var isEmployee = false
    var isBusiness = false

    // DATOS (Los dibujos) - Array<IntArray> de 7x24
    var schoolGrid: Array<IntArray>? = null
    var workGrid: Array<IntArray>? = null
    var businessGrid: Array<IntArray>? = null

    // COMBINAR BLOQUEOS (Para que en Trabajo se vea gris lo de Escuela)
    fun getCombinedBlockedGrid(): Array<IntArray> {
        val combined = Array(7) { IntArray(24) { 0 } }

        // Sumamos Escuela
        schoolGrid?.let { grid ->
            for (d in 0..6) for (h in 0..23) if (grid[d][h] == 1) combined[d][h] = 1
        }

        // Sumamos Trabajo (solo si estamos en la fase de negocio)
        workGrid?.let { grid ->
            for (d in 0..6) for (h in 0..23) if (grid[d][h] == 1) combined[d][h] = 1
        }

        return combined
    }

    // --- GUARDAR TODO EN LA NUBE ---
    fun finalSave(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val list = mutableListOf<ScheduleData>()
            val gson = Gson()

            // Convertimos los grids a JSON String para mandarlos
            if (isStudent && schoolGrid != null) {
                list.add(ScheduleData("SCHOOL", gson.toJson(schoolGrid)))
            }
            if (isEmployee && workGrid != null) {
                list.add(ScheduleData("WORK", gson.toJson(workGrid)))
            }
            if (isBusiness && businessGrid != null) {
                list.add(ScheduleData("BUSINESS", gson.toJson(businessGrid)))
            }

            // Llamamos al repo
            val success = repository.saveSchedules(list)

            if (success) {
                onSuccess()
            } else {
                onError("No se pudo guardar la configuración.")
            }
        }
    }
}