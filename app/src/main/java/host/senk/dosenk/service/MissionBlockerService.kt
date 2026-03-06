package host.senk.dosenk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import host.senk.dosenk.R
import kotlinx.coroutines.*
import java.util.TreeMap

class MissionBlockerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var tvTimer: TextView
    private lateinit var tvMissionName: TextView

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var engineJob: Job? = null

    private var timeLeftSeconds = 0
    private var defaultLauncherPackage: String = ""

    override fun onCreate() {
        super.onCreate()

        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        defaultLauncherPackage = resolveInfo?.activityInfo?.packageName ?: ""

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_DoSenk_Teal)
        // INFLAMOS EL NUEVO LAYOUT
        overlayView = LayoutInflater.from(themeContext).inflate(R.layout.layout_mission_overlay, null)

        tvTimer = overlayView.findViewById(R.id.tvRealMissionTimer)
        tvMissionName = overlayView.findViewById(R.id.tvMissionNameTitle)

        val btnGiveUp = overlayView.findViewById<Button>(R.id.btnGiveUpReal)
        btnGiveUp.setOnClickListener {
            overlayView.visibility = View.GONE
            val appIntent = Intent(this, host.senk.dosenk.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(appIntent)
        }

        overlayView.visibility = View.GONE
        windowManager.addView(overlayView, layoutParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "DO_MISSION_CHANNEL")
            .setContentTitle(">Do: Misión Activa")
            .setContentText("Tengo tu telefonito")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()

        startForeground(2, notification) // Usamos ID 2 para no chocar con el tutorial

        // saca los datos de ka bd
        timeLeftSeconds = intent?.getIntExtra("DURATION_SECONDS", 60) ?: 60
        val missionName = intent?.getStringExtra("MISSION_NAME") ?: "Castigo Activo"

        tvMissionName.text = "¿Listo para:\n$missionName?"

        startEngine()

        return START_STICKY
    }

    private fun startEngine() {
        engineJob = serviceScope.launch {
            while (isActive && timeLeftSeconds > 0) {
                val hours = timeLeftSeconds / 3600
                val minutes = (timeLeftSeconds % 3600) / 60
                val seconds = timeLeftSeconds % 60
                tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                checkForegroundApp()

                delay(1000)
                timeLeftSeconds--
            }

            overlayView.visibility = View.GONE
            stopSelf()
        }
    }

    private fun checkForegroundApp() {
        val currentApp = getForegroundAppPackage()
        val isLauncher = currentApp == defaultLauncherPackage
        val isMyApp = currentApp == packageName
        val isPhone = currentApp.contains("dialer") || currentApp.contains("telecom") || currentApp.contains("incallui")

        if (!isLauncher && !isMyApp && !isPhone && currentApp.isNotEmpty()) {
            if (overlayView.visibility == View.GONE) overlayView.visibility = View.VISIBLE
        } else {
            if (overlayView.visibility == View.VISIBLE) overlayView.visibility = View.GONE
        }
    }

    private fun getForegroundAppPackage(): String {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

        if (appList != null && appList.isNotEmpty()) {
            val sortedMap = TreeMap<Long, UsageStats>()
            for (usageStats in appList) {
                sortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (sortedMap.isNotEmpty()) {
                return sortedMap[sortedMap.lastKey()]?.packageName ?: ""
            }
        }
        return ""
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("DO_MISSION_CHANNEL", ">Do Misiones", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engineJob?.cancel()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}