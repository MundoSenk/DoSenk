package host.senk.dosenk.ui.blocks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import host.senk.dosenk.R
import host.senk.dosenk.util.AppUsageInfo
import host.senk.dosenk.util.AppUsageManager

class AppBlockAdapter(
    private var apps: List<AppUsageInfo>,
    // Aquí guardamos los nombres de paquete com.whatsapp de las apps bloqueadas
    val selectedPackages: MutableSet<String>
) : RecyclerView.Adapter<AppBlockAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbBlockApp: CheckBox = view.findViewById(R.id.cbBlockApp)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvAppUsage: TextView = view.findViewById(R.id.tvAppUsage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_block, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        holder.tvAppName.text = app.appName
        holder.ivAppIcon.setImageDrawable(app.icon)
        holder.tvAppUsage.text = "${AppUsageManager.formatTime(app.timeInForegroundMillis)} esta semana"

        // Quitamos el listener un momento para que no haga "falsos positivos" al hacer scroll
        holder.cbBlockApp.setOnCheckedChangeListener(null)

        // Palomeamos la lista negra del usuario.
        holder.cbBlockApp.isChecked = selectedPackages.contains(app.packageName)

        // lO QUE PICA
        holder.cbBlockApp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPackages.add(app.packageName)
            } else {
                selectedPackages.remove(app.packageName)
            }
        }

        // Hacer que tocar toda la tarjeta también cambie el checkbox
        holder.itemView.setOnClickListener {
            holder.cbBlockApp.isChecked = !holder.cbBlockApp.isChecked
        }
    }

    override fun getItemCount() = apps.size

    fun updateData(newApps: List<AppUsageInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}