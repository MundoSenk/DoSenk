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



// COn el que guardamos la suma y la lista de apps nas usadas
data class UsageReport(
    val topVices: List<AppUsageInfo>,
    val totalTimeMs: Long
)

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
    fun getTopVices(context: Context, daysToLookBack: Int = 7, topCount: Int = 5): UsageReport {
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

        // Filtramos para obtener SOLO apps lanzables (sin basura del sistema)
        val launchableApps = packageManager.getInstalledPackages(PackageManager.MATCH_ALL)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { it.packageName }
            .toSet()

        val healthyApps = setOf(
            "jp.pokemon.pokemonsleep",
            context.packageName

        )

        // Filtramos el mapa para quedarnos solo con las apps que son lanchaubles
        val validAppsMap = aggregatedStats.filter {
            it.value > 0 &&
                    launchableApps.contains(it.key) &&
                    !healthyApps.contains(it.key)
        }

        //  Sumamos  el tiempo de todas las apps válidas
        val trueTotalTimeMs = validAppsMap.values.sum()

        // Ahora sí, armamos la lista para sacar el top 5
        val vicesList = validAppsMap.mapNotNull { entry ->
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
                null
            }
        }
            .sortedByDescending { it.timeInForegroundMillis }
            .take(topCount)

        // Devolvemos ambos datos
        return UsageReport(topVices = vicesList, totalTimeMs = trueTotalTimeMs)
    }

    // Formatear tiempo para el humano
    fun formatTime(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = ((millis / (1000 * 60)) % 60).toInt()
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}