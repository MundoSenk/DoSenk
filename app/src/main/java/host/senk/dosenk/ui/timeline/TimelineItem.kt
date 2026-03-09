package host.senk.dosenk.ui.timeline

import host.senk.dosenk.data.local.entity.MissionEntity

// El envoltorio donde meteremos las misiones
sealed class TimelineItem {

    // tippo 1 Una misión real programada
    data class MissionCard(
        val timeLabel: String, // Ej: "15:00"
        val mission: MissionEntity
    ) : TimelineItem()

    //  tipo 2 Un hueco libre en la agenda
    data class EmptySlot(
        val timeLabel: String, // Ej: "16:00"
        val durationMinutes: Int
    ) : TimelineItem()
}