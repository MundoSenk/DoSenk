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


import host.senk.dosenk.data.local.dao.BlockProfileDao
import host.senk.dosenk.data.local.entity.BlockProfileEntity
import kotlinx.coroutines.flow.first

@HiltViewModel
class EditBlockViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val blockProfileDao: BlockProfileDao
) : ViewModel() {

    // El chisme para tu Header sádico
    val currentUserAlias = userPreferences.userAlias.asLiveData()

    private val _installedApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val installedApps: StateFlow<List<AppUsageInfo>> = _installedApps

    fun loadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val report = AppUsageManager.getTopVices(context, daysToLookBack = 7, topCount = 1000)
            _installedApps.value = report.topVices
        }
    }

    // 🚨 FUNCIÓN PARA GUARDAR EN LA BASE DE DATOS
    fun saveCustomBlock(blockName: String, jsonBlockList: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Sacamos el UUID del usuario directamente de las preferencias
            val userUuid = userPreferences.userToken.first()

            // 2. Creamos la entidad
            val newProfile = BlockProfileEntity(
                userUuid = userUuid,
                name = blockName,
                blockedAppsJson = jsonBlockList
            )

            // 3. La guardamos en Room
            blockProfileDao.insertProfile(newProfile)

            // 4. Le avisamos a la pantalla que ya terminamos
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}