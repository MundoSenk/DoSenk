package host.senk.dosenk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import host.senk.dosenk.data.local.dao.MissionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MissionTriggerReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MissionTriggerEntryPoint {
        fun getMissionDao(): MissionDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("DOSENK_DEBUG", "¡Alarma recibida! Despertando a la bestia...")
        val pendingResult = goAsync()

        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MissionTriggerEntryPoint::class.java
        )
        val missionDao = hiltEntryPoint.getMissionDao()

        val missionName = intent.getStringExtra("MISSION_NAME") ?: return
        val durationMinutes = intent.getIntExtra("DURATION_MINUTES", 0)

        //  LA AUDITORÍA DE LA HORA EN PLENA MADRUGADA
        val isAutoTime = android.provider.Settings.Global.getInt(
            context.contentResolver,
            android.provider.Settings.Global.AUTO_TIME, 0
        ) == 1

        // Preparamos el golpe Lanzar el servicio de Bloqueo
        val serviceIntent = Intent(context, MissionBlockerService::class.java).apply {
            if (!isAutoTime) {
                // MODO TRAMPA
                putExtra("DURATION_SECONDS", 99999)
                putExtra("MISSION_NAME", "¡Trampa!\nPrende la Hora Automática.")
                putExtra("IS_TIME_PUNISHMENT", true)
            } else {
                // MODO NORMAL
                putExtra("DURATION_SECONDS", durationMinutes * 60)
                putExtra("MISSION_NAME", missionName)
                putExtra("IS_TIME_PUNISHMENT", false)
            }
        }

        // AQUÍ LANZAMOS EL BLOQUEO DIRECTAMENTE
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Bypass para restricciones raras en segundo plano
            context.startService(serviceIntent)
        }

        // Actualizamos la base de datos
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mission = missionDao.getNextPendingMission().firstOrNull()
                if (mission != null && mission.name == missionName) {
                    val updatedMission = mission.copy(
                        status = "active",
                        executionDate = System.currentTimeMillis()
                    )
                    missionDao.updateMission(updatedMission)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}