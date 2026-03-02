package host.senk.dosenk.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.remote.model.ScheduleData
import host.senk.dosenk.data.repository.AuthRepository
import host.senk.dosenk.util.AppUsageInfo
import host.senk.dosenk.util.AppUsageManager
import host.senk.dosenk.util.UsageReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import host.senk.dosenk.data.local.UserPreferences
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {


    // ESTADOS DE LOS HORARIOS TIME PAINTING ////////////////////////////


    val userAlias = userPreferences.userAlias
    var isStudent = false
    var isEmployee = false
    var isBusiness = false

    var schoolGrid: Array<IntArray>? = null
    var workGrid: Array<IntArray>? = null
    var businessGrid: Array<IntArray>? = null

    fun getCombinedBlockedGrid(): Array<IntArray> {
        val combined = Array(7) { IntArray(24) { 0 } }

        schoolGrid?.let { grid ->
            for (d in 0..6) for (h in 0..23) if (grid[d][h] == 1) combined[d][h] = 1
        }

        workGrid?.let { grid ->
            for (d in 0..6) for (h in 0..23) if (grid[d][h] == 1) combined[d][h] = 1
        }

        return combined
    }

    fun finalSave(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val list = mutableListOf<ScheduleData>()
            val gson = Gson()

            if (isStudent && schoolGrid != null) {
                list.add(ScheduleData("SCHOOL", gson.toJson(schoolGrid)))
            }
            if (isEmployee && workGrid != null) {
                list.add(ScheduleData("WORK", gson.toJson(workGrid)))
            }
            if (isBusiness && businessGrid != null) {
                list.add(ScheduleData("BUSINESS", gson.toJson(businessGrid)))
            }

            val success = repository.saveSchedules(list)

            if (success) {
                onSuccess()
            } else {
                onError("No se pudo guardar la configuración.")
            }
        }
    }

//////////////////////////////////////////////////////////////////



    // ESTADOS DE LAS ESTADÍSTICAS /////////////////


    // Aquí guardamos los datos para que el TutorialDashboard (
    var trueTotalTimeMs: Long = 0L
    var worstAppsList: List<AppUsageInfo> = emptyList()

    // Estado observable para que el Fragment sepa cuándo ya cargaron las stats
    private val _usageReport = MutableStateFlow<UsageReport?>(null)
    val usageReport: StateFlow<UsageReport?> = _usageReport.asStateFlow()

    // La función que hace el trabajo sucio en segundo plano
    fun loadUsageStats(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // Pedimos el reporte de 7 días y top 5
            val report = AppUsageManager.getTopVices(context, daysToLookBack = 7, topCount = 5)

            // Guardamos en memoria para la siguiente pantalla
            trueTotalTimeMs = report.totalTimeMs
            worstAppsList = report.topVices

            // Avisamos a la Vista  que ya tenemos los datos
            _usageReport.value = report
        }
    }
}