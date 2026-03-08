package host.senk.dosenk.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent


import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.provider.Settings



fun requestAutoStartPermission(context: Context) {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val intent = Intent()

    try {
        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("poco") -> {
                // Abre la pantalla oculta de HyperOS/MIUI
                intent.component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            manufacturer.contains("tecno") || manufacturer.contains("infinix") -> {
                // Abre el Phone Master de HiOS
                intent.component = ComponentName(
                    "com.transsion.phonemaster",
                    "com.cyin.himgr.widget.activity.MainSettingActivity"
                )
            }
            else -> {
                // Para Android puro (Motorola, Pixel), abrimos los ajustes de optimización de batería
                intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

    } catch (e: Exception) {
        // Si el celular tiene una versión rara y falla, lo mandamos a los ajustes normales de la app
        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    }
}