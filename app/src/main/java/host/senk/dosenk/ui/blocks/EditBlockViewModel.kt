package host.senk.dosenk.ui.blocks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.util.AppUsageInfo
import host.senk.dosenk.util.AppUsageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditBlockViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    // El chisme para tu Header sádico
    val currentUserAlias = userPreferences.userAlias.asLiveData()

    private val _installedApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val installedApps: StateFlow<List<AppUsageInfo>> = _installedApps

    // Traemos TODAS las apps usadas (Top 1000)
    fun loadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // Le pedimos el reporte a tu herramienta, pero le decimos que traiga hasta 1000 apps
            val report = AppUsageManager.getTopVices(context, daysToLookBack = 7, topCount = 1000)
            _installedApps.value = report.topVices
        }
    }
}