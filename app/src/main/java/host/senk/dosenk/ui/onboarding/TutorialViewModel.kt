package host.senk.dosenk.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.repository.AuthRepository
import host.senk.dosenk.util.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject



@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    var calculatedRank: String = "Desconocido"

    // La matemática dura para dictar el rango
    fun calculateRank(totalTimeMs: Long) {
        // Promedio diario (dividimos entre 7 días)
        val dailyAverageMs = totalTimeMs / 7
        // Convertrimos a hoas
        val hours = dailyAverageMs / (1000 * 60 * 60).toFloat()

        calculatedRank = when {
            hours >= 10f -> "Infierno"
            hours >= 7f -> "Purgatorio"
            hours >= 5f -> "Olvidable"
            hours >= 4f -> "Potencial"
            hours >= 3f -> "Skywalker"
            hours >= 2f -> "Idóneo"
            hours >= 1f -> "As"
            else -> "Dios"
        }
    }

    fun saveToDatabase(
        totalTimeMs: Long,
        vicesList: List<AppUsageInfo>,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dailyAverageMs = totalTimeMs / 7
            val hours = dailyAverageMs / (1000 * 60 * 60).toFloat()

            val success = repository.saveDisciplineLevel(
                dailyHours = hours,
                rankName = calculatedRank,
                vices = vicesList
            )

            withContext(Dispatchers.Main) {
                if (success) onSuccess() else onError()
            }
        }
    }
}