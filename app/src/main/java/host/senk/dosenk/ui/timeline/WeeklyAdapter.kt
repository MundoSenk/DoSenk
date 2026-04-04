package host.senk.dosenk.ui.timeline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.senk.dosenk.R
import host.senk.dosenk.util.applyDoSenkGradient

class WeeklyAdapter(private var items: List<WeeklyCardItem>) : RecyclerView.Adapter<WeeklyAdapter.WeeklyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyViewHolder {
        // Asumiendo que crearás un XML llamado item_weekly_card.xml
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weekly_card, parent, false)
        return WeeklyViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeeklyViewHolder, position: Int) {
        val item = items[position]

        // Textos principales
        holder.tvDayName.text = "${item.dayName} ${item.dateDay}"
        holder.tvMissionsCount.text = item.totalMissions.toString()
        holder.tvProjectsCount.text = item.totalProjects.toString()

        //  Aplicar el color del tema
        holder.tvDayName.setTextColor(getColorFromTheme(holder.itemView, R.attr.doSkinButton))

        // Pinta los fondos de los números con gradiente
        holder.layoutMissionGradient.applyDoSenkGradient(cornerRadius = 16f)
        holder.layoutProjectGradient.applyDoSenkGradient(cornerRadius = 16f)

        // Pintar las misiones importantes (píldoras)
        val pillViews = listOf(holder.tvImportant1, holder.tvImportant2, holder.tvImportant3)
        for (i in pillViews.indices) {
            if (i < item.importantMissions.size) {
                pillViews[i].visibility = View.VISIBLE
                pillViews[i].text = item.importantMissions[i]
                // Aplicar fondo a las píldoras
                pillViews[i].applyDoSenkGradient(cornerRadius = 8f)
            } else {
                pillViews[i].visibility = View.GONE
            }
        }

        // Efecto visual si es HOY (puedes ponerle un borde o algo especial luego)
        if (item.isToday) {
            holder.tvDayName.textSize = 22f
        } else {
            holder.tvDayName.textSize = 18f
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<WeeklyCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // Función auxiliar para sacar el color del tema activo
    private fun getColorFromTheme(view: View, attrId: Int): Int {
        val typedValue = android.util.TypedValue()
        view.context.theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    inner class WeeklyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayName: TextView = view.findViewById(R.id.tvDayName)
        val tvMissionsCount: TextView = view.findViewById(R.id.tvMissionsCount)
        val tvProjectsCount: TextView = view.findViewById(R.id.tvProjectsCount)

        val layoutMissionGradient: View = view.findViewById(R.id.layoutMissionGradient)
        val layoutProjectGradient: View = view.findViewById(R.id.layoutProjectGradient)

        val tvImportant1: TextView = view.findViewById(R.id.tvImportant1)
        val tvImportant2: TextView = view.findViewById(R.id.tvImportant2)
        val tvImportant3: TextView = view.findViewById(R.id.tvImportant3)
    }
}