package host.senk.dosenk.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import java.util.Calendar
import android.util.Log


// Estructura para guardar la info del vicio
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val timeInForegroundMillis: Long,
    val icon: Drawable? = null
)

object AppUsageManager {

    // Verifica si el usuario ya nos dio permiso
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Intent para mandar al usuario a la pantalla de Ajustes
    fun getPermissionSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    // Extrae los vicios
    fun getTopVices(context: Context, daysToLookBack: Int = 3, topCount: Int = 3): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -daysToLookBack)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val aggregatedStats = mutableMapOf<String, Long>()
        for (stat in stats) {
            val currentTotal = aggregatedStats.getOrDefault(stat.packageName, 0L)
            aggregatedStats[stat.packageName] = currentTotal + stat.totalTimeInForeground
        }


        Log.d("DoSenkStats", "========== INICIANDO REPORTE BRUTO (Últimos $daysToLookBack días) ==========")
        for ((packageName, time) in aggregatedStats) {
            if (time > 0) {
                Log.d("DoSenkStats", "BRUTO -> Paquete: $packageName | Tiempo: ${formatTime(time)} ($time ms)")
            }
        }

        val launchableApps = packageManager.getInstalledPackages(PackageManager.MATCH_ALL)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.packageName }
            .toSet()


        Log.d("DoSenkStats", "========== APLICANDO FILTRO DE APPS 'LANZABLES' ==========")

        val finalResult = aggregatedStats
            .filter { it.value > 0 && launchableApps.contains(it.key) }
            .mapNotNull { entry ->
                try {
                    val packageName = entry.key
                    val totalTime = entry.value

                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)

                    AppUsageInfo(
                        packageName = packageName,
                        appName = appName,
                        timeInForegroundMillis = totalTime,
                        icon = icon
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.e("DoSenkStats", "ERROR -> No se encontró la app: ${entry.key}")
                    null
                }
            }
            .sortedByDescending { it.timeInForegroundMillis }

        Log.d("DoSenkStats", "========== RANKING FINAL (ANTES DEL TOP $topCount) ==========")
        for (vice in finalResult) {
            Log.d("DoSenkStats", "RANKING -> App: ${vice.appName} (${vice.packageName}) | Tiempo: ${formatTime(vice.timeInForegroundMillis)}")
        }

        return finalResult.take(topCount)
    }

    // Formatear tiempo para el humano
    fun formatTime(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = ((millis / (1000 * 60)) % 60).toInt()
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}