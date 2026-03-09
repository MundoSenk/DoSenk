package host.senk.dosenk.service

import android.app.AlarmManager
import android.app.PendingIntent
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

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun getMissionDao(): MissionDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            android.util.Log.d("DOSENK_DEBUG", "¡Celular encendido! Analizando escenarios...")

            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootEntryPoint::class.java
            )
            val missionDao = hiltEntryPoint.getMissionDao()

            CoroutineScope(Dispatchers.IO).launch {
                val currentTime = System.currentTimeMillis()


                //////////////////  REVISAMOS SI HABÍA UNA MISIÓN ACTIVA CUANDO SE APAGÓ
                val activeMission = missionDao.getActiveMission().firstOrNull()

                if (activeMission != null) {
                    val endTime = activeMission.executionDate + (activeMission.durationMinutes * 60 * 1000L)

                    if (currentTime < endTime) {
                        // ESCENARIO 1: El tramposo prendió el celular antes de que acabara su castigo
                        val remainingSeconds = ((endTime - currentTime) / 1000).toInt()
                        lanzarBloqueoInmediato(context, remainingSeconds, activeMission.name)
                    } else {
                        // ESCENARIO 2: Se apagó a medio castigo, pero ya pasó la hora límite. Lo perdonamos.
                        val completedMission = activeMission.copy(status = "completed")
                        missionDao.updateMission(completedMission)
                    }
                }


                //////////////// REVISAMOS LAS MISIONES PENDIENTES
                val pendingMission = missionDao.getNextPendingMission().firstOrNull()

                if (pendingMission != null) {
                    val endTime = pendingMission.executionDate + (pendingMission.durationMinutes * 60 * 1000L)

                    if (currentTime >= endTime) {
                        // ESCENARIO 1: Estuvo apagado durante TODA la ventana de la misión. Misión cumplida.
                        val completedMission = pendingMission.copy(status = "completed")
                        missionDao.updateMission(completedMission)

                    } else if (currentTime >= pendingMission.executionDate && currentTime < endTime) {
                        // ESCENARIO 2 : La alarma no sonó porque estaba apagado, pero AHORITA es la hora del castigo.
                        val activatedMission = pendingMission.copy(status = "active")
                        missionDao.updateMission(activatedMission)

                        val remainingSeconds = ((endTime - currentTime) / 1000).toInt()
                        lanzarBloqueoInmediato(context, remainingSeconds, pendingMission.name)

                    } else {
                        // ESCENARIO 3: La misión es en el futuro. Volvemos a armar la bomba tranquilamente.
                        reprogramarAlarma(context, pendingMission)
                    }
                }
            }
        }
    }


    // Función auxiliar blindada con el Despertador Nuclear
    private fun lanzarBloqueoInmediato(context: Context, remainingSeconds: Int, missionName: String) {
        val serviceIntent = Intent(context, MissionBlockerService::class.java).apply {
            putExtra("DURATION_SECONDS", remainingSeconds)
            // ¡Mensaje humillante personalizado!
            putExtra("MISSION_NAME", "¿Creíste que apagando el teléfono te salvarías? Idiota.\n\n$missionName")
            putExtra("IS_TIME_PUNISHMENT", false)
        }

        // En lugar de iniciarlo nosotros, programamos la alarma
        // para que estalle 2 segundos después de encender el celular.
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                999, // ID único para el castigo inmediato
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                999,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            val dummyIntent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            // Disparamos en 2 segundos para darle tiempo a Android de respirar
            val alarmClockInfo = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 2000, dummyIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            // Permisos denegados
        }
    }

    // Función auxiliar para reprogramar misiones futuras
    private fun reprogramarAlarma(context: Context, mission: host.senk.dosenk.data.local.entity.MissionEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val serviceIntent = Intent(context, MissionBlockerService::class.java).apply {
            putExtra("MISSION_NAME", mission.name)
            putExtra("DURATION_SECONDS", mission.durationMinutes * 60)
            putExtra("IS_TIME_PUNISHMENT", false)
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context, (mission.executionDate / 1000).toInt(), serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context, (mission.executionDate / 1000).toInt(), serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        try {
            val dummyIntent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(mission.executionDate, dummyIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            // Permisos revocados
        }
    }
}