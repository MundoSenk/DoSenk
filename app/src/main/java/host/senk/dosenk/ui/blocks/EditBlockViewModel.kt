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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

import host.senk.dosenk.data.remote.ApiService
import host.senk.dosenk.data.local.dao.BlockProfileDao
import host.senk.dosenk.data.local.dao.MissionDao
import host.senk.dosenk.data.local.entity.BlockProfileEntity
import host.senk.dosenk.data.remote.model.BlockProfileDto
import host.senk.dosenk.data.remote.model.SyncBlocksRequest

@HiltViewModel
class EditBlockViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val blockProfileDao: BlockProfileDao,
    private val missionDao: MissionDao,
    private val api: ApiService
) : ViewModel() {

    val currentUserAlias = userPreferences.userAlias.asLiveData()

    private val _installedApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val installedApps: StateFlow<List<AppUsageInfo>> = _installedApps

    fun loadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val report = AppUsageManager.getTopVices(context, daysToLookBack = 7, topCount = 1000)
            _installedApps.value = report.topVices
        }
    }

    // FUNCIÓN PARA OBTENER LOS BLOQUEOS DE RESPALDO
    suspend fun getFallbackBlocks(blockToExclude: String): List<String> {
        val allProfiles = blockProfileDao.getAllProfiles().first()
        val customNames = allProfiles.map { it.name }.filter { it != blockToExclude }

        // Siempre le damos la opción de Dios como castigo supremo de respaldo
        return listOf("Dios") + customNames
    }

    //  GUARDADO
    fun saveCustomBlock(originalName: String?, blockName: String, jsonBlockList: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        val reservedWords = listOf("dios", "humano", "adicto")
        if (reservedWords.contains(blockName.lowercase())) {
            onError("No puedes usar nombres del sistema como '$blockName'.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val userUuid = userPreferences.userToken.first()

            if (originalName != null && originalName != blockName) {
                // SI LE CAMBIÓ EL NOMBRE AL BLOQUEO, ACTUALIZAMOS LAS MISIONES PARA QUE NO SE ROMPAN
                missionDao.reassignMissions(oldBlockName = originalName, newBlockName = blockName)
                blockProfileDao.deleteProfileByName(originalName, userUuid)
            }

            blockProfileDao.deleteProfileByName(blockName, userUuid)

            val newProfile = BlockProfileEntity(userUuid = userUuid, name = blockName, blockedAppsJson = jsonBlockList)
            blockProfileDao.insertProfile(newProfile)

            try {
                val allBlocks = blockProfileDao.getAllProfiles().first()
                val dtoList = allBlocks.map { BlockProfileDto(it.name, it.blockedAppsJson) }
                api.syncBlocks(SyncBlocksRequest(userUuid, dtoList))
            } catch (e: Exception) {}

            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    // BORRADO Y REASIGNACIÓN SUPREMA
    fun deleteAndReassignBlock(oldBlockName: String, newBlockName: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userUuid = userPreferences.userToken.first()

            //  Mudamos las misiones al nuevo castigo elegido
            missionDao.reassignMissions(oldBlockName, newBlockName)

            //  Ejecutamos al bloqueo viejo
            blockProfileDao.deleteProfileByName(oldBlockName, userUuid)

            // Sincronizamos la nube
            try {
                val allBlocks = blockProfileDao.getAllProfiles().first()
                val dtoList = allBlocks.map { BlockProfileDto(it.name, it.blockedAppsJson) }
                api.syncBlocks(SyncBlocksRequest(userUuid, dtoList))
            } catch (e: Exception) {}

            withContext(Dispatchers.Main) { onComplete() }
        }
    }
}