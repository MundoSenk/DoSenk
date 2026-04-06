package host.senk.dosenk.domain //

import host.senk.dosenk.data.local.dao.MissionDao
import host.senk.dosenk.data.local.entity.MissionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionCloneManager @Inject constructor(
    private val missionDao: MissionDao
) {
    suspend fun generateClonesForNext7Days() = withContext(Dispatchers.IO) {
        val templates = missionDao.getActiveTemplates()
        if (templates.isEmpty()) return@withContext

        val calendar = java.util.Calendar.getInstance()
        // Ponemos el reloj a la medianoche de hoy
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // Miramos 7 días al futuro
        for (i in 0..6) {
            val currentDayMs = calendar.timeInMillis

            // Ajuste: Calendar en Java el Domingo es 1.
            var dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            dayOfWeek = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1

            for (template in templates) {
                // ¿Esta rutina se debe hacer este día de la semana?
                if (template.daysOfWeek.contains(dayOfWeek)) {

                    val exactStartMs = currentDayMs + (template.startTimeMin * 60 * 1000L)
                    val exactEndMs = exactStartMs + (template.durationMinutes * 60 * 1000L)

                    // 1. ¿Ya existe una misión que choca aquí?
                    val collisions = missionDao.getCollidingMissions(exactStartMs, exactEndMs)
                    val hasManualOverride = collisions.any { it.isManualOverride }

                    // 2. ¿Ya habíamos clonado esta rutina para este día exacto?
                    val alreadyCloned = collisions.any { it.templateUuid == template.uuid }

                    // Si la vía está libre y no lo hemos clonado, lo creamos
                    if (!hasManualOverride && !alreadyCloned) {
                        val clone = MissionEntity(
                            userUuid = template.userUuid,
                            name = template.name,
                            description = template.description,
                            durationMinutes = template.durationMinutes,
                            executionDate = exactStartMs,
                            assignmentType = template.assignmentType,
                            blockType = template.blockType,
                            potentialXp = template.potentialXp,
                            templateUuid = template.uuid,
                            isManualOverride = false,
                            status = "pending" // Nace pendiente
                        )
                        missionDao.insertMission(clone)
                    }
                }
            }
            // Avanzamos al siguiente día
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
    }
}