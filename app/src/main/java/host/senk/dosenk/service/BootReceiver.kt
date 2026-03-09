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
import host.senk.dosenk.data.local.entity.MissionEntity
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
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            android.util.Log.d("DOSENK_DEBUG", "¿porque apagaste el telefono?")

            //  SOSTENEMOS LA PUERTA PARA QUE ANDROID NO NOS MATE
            val pendingResult = goAsync()

            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootEntryPoint::class.java
            )
            val missionDao = hiltEntryPoint.getMissionDao()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentTime = System.currentTimeMillis()
                    var penaltySeconds = 0
                    var penaltyName = ""

                    val activeMission = missionDao.getActiveMission().firstOrNull()
                    val pendingMission = missionDao.getNextPendingMission().firstOrNull()

                    suspend fun processMission(mission: MissionEntity, wasAlreadyActive: Boolean) {
                        val endTime = mission.executionDate + (mission.durationMinutes * 60 * 1000L)

                        if (currentTime >= endTime) {
                            // ESCENARIO 3: Se apagó y ya pasó la hora límite. Lo perdonamos.
                            missionDao.updateMission(mission.copy(status = "completed"))

                        } else if (currentTime >= mission.executionDate && currentTime < endTime) {
                            // ESCENARIO 1: Prendió el celular en plena hora de castigo
                            val humillacion = if (wasAlreadyActive) {
                                "¿Creíste que apagando el teléfono te salvarías? Idiota.\n\n${mission.name}"
                            } else {
                                mission.name
                            }

                            missionDao.updateMission(mission.copy(status = "active", name = humillacion))

                            // Guardamos la condena para ejecutarla
                            penaltySeconds = ((endTime - currentTime) / 1000).toInt()
                            penaltyName = humillacion

                        } else {
                            // ESCENARIO 2: Es en el futuro. Reprogramamos la bomba.
                            reprogramarAlarma(context, mission)
                        }
                    }

                    if (activeMission != null) processMission(activeMission, true)
                    if (pendingMission != null) processMission(pendingMission, false)


                    // 🚨 EL MISIL NUCLEAR DIRECTO AL SERVICIO (No a MainActivity)

                    if (penaltySeconds > 0) {
                        val serviceIntent = Intent(context, MissionBlockerService::class.java).apply {
                            putExtra("DURATION_SECONDS", penaltySeconds)
                            putExtra("MISSION_NAME", penaltyName)
                            putExtra("IS_TIME_PUNISHMENT", false)
                        }

                        // Usamos getForegroundService
                        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            PendingIntent.getForegroundService(
                                context, 999, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        } else {
                            PendingIntent.getService(
                                context, 999, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }

                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val dummyIntent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)

                        // Detonamos la pantalla negra en 2 segundos usando el AlarmClock supremo
                        val alarmClockInfo = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 2000, dummyIntent)
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    }

                } finally {
                    //  SOLTAMOS LA PUERTA (Importante para no gastar batería)
                    pendingResult.finish()
                }
            }
        }
    }

    private fun reprogramarAlarma(context: Context, mission: MissionEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerIntent = Intent(context, MissionTriggerReceiver::class.java).apply {
            putExtra("MISSION_NAME", mission.name)
            putExtra("DURATION_MINUTES", mission.durationMinutes)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (mission.executionDate / 1000).toInt(),
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val dummyIntent = PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(mission.executionDate, dummyIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {}
    }
}