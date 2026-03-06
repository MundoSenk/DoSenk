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
        //  Le decimos a Android
        val pendingResult = goAsync()

        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MissionTriggerEntryPoint::class.java
        )
        val missionDao = hiltEntryPoint.getMissionDao()

        val missionName = intent.getStringExtra("MISSION_NAME") ?: return
        val durationMinutes = intent.getIntExtra("DURATION_MINUTES", 0)

        val endTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

        // Despertamos a la Bestia (Pantalla Negra)
        val serviceIntent = Intent(context, MissionBlockerService::class.java).apply {
            putExtra("END_TIME_MILLIS", endTimeMillis)
            putExtra("MISSION_NAME", missionName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // EL ARREGLO DEL BUCLE: Leemos UNA VEZ con firstOrNull()
                val mission = missionDao.getNextPendingMission().firstOrNull()
                if (mission != null && mission.name == missionName) {
                    val updatedMission = mission.copy(status = "active")
                    missionDao.updateMission(updatedMission)
                }
            } finally {
                // APAGAMOS EL ESCUDO:
                pendingResult.finish()
            }
        }
    }
}