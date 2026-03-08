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

class TimeTravelReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TimeTravelEntryPoint {
        fun getMissionDao(): MissionDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ¿El usuario intentó mover la hora desde los ajustes?
        if (intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_DATE_CHANGED) {

            // Revisamos si apagó la hora automática
            val isAutoTime = android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AUTO_TIME, 0
            ) == 1

            if (!isAutoTime) {
                val pendingResult = goAsync()
                val hiltEntryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    TimeTravelEntryPoint::class.java
                )
                val missionDao = hiltEntryPoint.getMissionDao()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // ¿Tenía misiones pendientes que intentaba saltarse?
                        val pendingMission = missionDao.getNextPendingMission().firstOrNull()
                        if (pendingMission != null) {

                            //  LO ATRAPAMOS. Lanzamos la pantalla negra directamente.
                            val serviceIntent = Intent(context, MissionBlockerService::class.java).apply {
                                putExtra("DURATION_SECONDS", 99999)
                                putExtra("MISSION_NAME", "¿Tramposo?\nPrende la Hora Automática para salir.")
                                putExtra("IS_TIME_PUNISHMENT", true) // ¡Bandera de Trampa!
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}