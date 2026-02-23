package host.senk.dosenk.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.senk.dosenk.R
import host.senk.dosenk.util.AppUsageInfo
import host.senk.dosenk.util.AppUsageManager

class TopVicesAdapter(private val vicesList: List<AppUsageInfo>) :
    RecyclerView.Adapter<TopVicesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvAppTime: TextView = view.findViewById(R.id.tvAppTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_vice, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vice = vicesList[position]

        holder.tvAppName.text = vice.appName
        holder.tvAppTime.text = AppUsageManager.formatTime(vice.timeInForegroundMillis)

        // Colocamos el ícono real de la app que extrajimos
        vice.icon?.let {
            holder.ivAppIcon.setImageDrawable(it)
        }
    }

    override fun getItemCount() = vicesList.size
}