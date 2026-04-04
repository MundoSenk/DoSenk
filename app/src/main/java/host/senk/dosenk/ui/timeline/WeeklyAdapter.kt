package host.senk.dosenk.ui.timeline

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient

class WeeklyAdapter(
    private var items: List<WeeklyCardItem>,
    private val onDayClick: (Long) -> Unit
) : RecyclerView.Adapter<WeeklyAdapter.WeeklyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weekly_card, parent, false)
        return WeeklyViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeeklyViewHolder, position: Int) {
        val item = items[position]

        // TEXTOS PRINCIPALES (HEADER)
        holder.tvDayName.text = "${item.dayName}, ${item.dayNumber}"
        holder.tvMonthName.text = item.monthYear
        holder.tvMissionsCount.text = item.totalMissions.toString()
        holder.tvProjectsCount.text = item.totalProjects.toString()
        holder.tvDayXp.text = item.dayXp.toString()



        if (item.isToday) {
            holder.tvDayName.text = "¡HOY! ${item.dayName}"
        } else {
            holder.tvDayName.text = "${item.dayName}, ${item.dayNumber}"
        }

        // APLICAR EL COLOR DEL TEMA (GRADIENTES)

        // El Header completo
        holder.layoutDayHeader.applyDoSenkGradient(cornerRadius = 24f) // Cornej radius alto para matchear la card

        // El número de XP
        val activeColor = TypedValue()
        holder.itemView.context.theme.resolveAttribute(R.attr.doSkinButton, activeColor, true)
        holder.tvDayXp.setTextColor(activeColor.data)

        // PINTAR LAS MISIONES IMPORTANTES (PÍLDORAS TRANSLARENTE CON BORDE DASHED)
        val pillViews = listOf(holder.tvImportant1, holder.tvImportant2, holder.tvImportant3)
        for (i in pillViews.indices) {
            if (i < item.importantMissions.size) {
                pillViews[i].visibility = View.VISIBLE
                pillViews[i].text = item.importantMissions[i]
                // Aplicar fondo transparente con borde dashed
                pillViews[i].applyDoSenkGradient(cornerRadius = 24f)
            } else {
                pillViews[i].visibility = View.GONE
            }
        }

        //EL CLIC MÁGICO DE NAVEGACIÓN
        holder.itemView.setOnClickListener {
            onDayClick(item.timestamp)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<WeeklyCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class WeeklyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayName: TextView = view.findViewById(R.id.tvDayName)
        val tvMonthName: TextView = view.findViewById(R.id.tvMonthName)

        val tvMissionsCount: TextView = view.findViewById(R.id.tvMissionsCount)
        val tvProjectsCount: TextView = view.findViewById(R.id.tvProjectsCount)
        val tvDayXp: TextView = view.findViewById(R.id.tvDayXp)

        val layoutDayHeader: View = view.findViewById(R.id.layoutDayHeader)

        val tvImportant1: TextView = view.findViewById(R.id.tvImportant1)
        val tvImportant2: TextView = view.findViewById(R.id.tvImportant2)
        val tvImportant3: TextView = view.findViewById(R.id.tvImportant3)
    }
}