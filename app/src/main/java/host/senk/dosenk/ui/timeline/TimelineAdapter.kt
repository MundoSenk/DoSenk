package host.senk.dosenk.ui.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.senk.dosenk.R

import host.senk.dosenk.data.local.entity.MissionEntity

import host.senk.dosenk.util.applyDoSenkGradient

class TimelineAdapter(
    private var items: List<TimelineItem>,
    private var pixelsPerMinute: Float = 4f,
    private val onMissionClick: (MissionEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_MISSION = 0
        const val TYPE_EMPTY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TimelineItem.MissionCard -> TYPE_MISSION
            is TimelineItem.EmptySlot -> TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_MISSION) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline_mission, parent, false)
            MissionViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline_empty, parent, false)
            EmptyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        val density = holder.itemView.context.resources.displayMetrics.density
        val pixelMultiplier = pixelsPerMinute * density

        if (holder is MissionViewHolder && item is TimelineItem.MissionCard) {
            holder.tvTime.text = item.timeLabel
            holder.tvTitle.text = item.mission.name
            holder.tvDescription.text = item.mission.description

            // Pintamos el fondo de la tarjeta
            holder.layoutMissionGradient.applyDoSenkGradient(cornerRadius = 16f)

            // Llenamos las píldoras nuevas
            holder.tvBlockType.text = "Bloqueo: ${item.mission.blockType}"
            holder.tvMissionType.text = if (item.mission.assignmentType == "auto") "Autoasignado" else "Misión diaria"

            //  Hora de inicio + la duracion
            try {
                val timeParts = item.timeLabel.split(":")
                if (timeParts.size == 2) {
                    val startH = timeParts[0].toInt()
                    val startM = timeParts[1].toInt()
                    val totalM = startM + item.mission.durationMinutes
                    val endH = (startH + (totalM / 60)) % 24
                    val endM = totalM % 60
                    val endStr = String.format("%02d:%02d", endH, endM)
                    holder.tvExecutionTime.text = "Ejecución: ${item.timeLabel}-$endStr"
                }
            } catch (e: Exception) {
                holder.tvExecutionTime.text = "Duración: ${item.mission.durationMinutes} min"
            }

            // Ajustamos el alto de la tarjeta
            val heightInPixels = (item.mission.durationMinutes * pixelMultiplier).toInt()
            val layoutParams = holder.cardMission.layoutParams
            layoutParams.height = heightInPixels
            holder.cardMission.layoutParams = layoutParams

            holder.cardMission.setOnClickListener {
                onMissionClick(item.mission)
            }

        } else if (holder is EmptyViewHolder && item is TimelineItem.EmptySlot) {
            holder.tvTime.text = item.timeLabel

            val heightInPixels = (item.durationMinutes * pixelMultiplier).toInt()
            val layoutParams = holder.cardEmpty.layoutParams
            layoutParams.height = heightInPixels
            holder.cardEmpty.layoutParams = layoutParams
        }
    }

    override fun getItemCount() = items.size

    // FUNCIÓN PARA EL PINCH-TO-ZOOM
    fun updateScale(newPixelsPerMinute: Float) {
        pixelsPerMinute = newPixelsPerMinute
        notifyDataSetChanged() // Recalcula todas las alturas
    }

    // ---xml ---
    inner class MissionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvStartTime)
        val tvTitle: TextView = view.findViewById(R.id.tvMissionTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvMissionDescription)

        val tvMissionType: TextView = view.findViewById(R.id.tvMissionType)
        val tvBlockType: TextView = view.findViewById(R.id.tvBlockType)
        val tvExecutionTime: TextView = view.findViewById(R.id.tvExecutionTime)

        val cardMission: View = view.findViewById(R.id.cardMission)
        val layoutMissionGradient: View = view.findViewById(R.id.layoutMissionGradient)
    }

    inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvStartTime)
        val cardEmpty: View = view.findViewById(R.id.cardEmptySlot)
    }
}