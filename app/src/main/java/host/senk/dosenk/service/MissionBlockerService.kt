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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    private var defaultLauncherPackage: String = ""
    private var endTimeRealtime: Long = 0L
    private var isTimePunishment = false

    private var blockType: String = "Dios" // Por defecto, bloquea todo
    private var blackList: Set<String> = emptySet()

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
        overlayView = LayoutInflater.from(themeContext).inflate(R.layout.layout_mission_overlay, null)

        tvTimer = overlayView.findViewById(R.id.tvRealMissionTimer)
        tvMissionName = overlayView.findViewById(R.id.tvMissionNameTitle)

        overlayView.visibility = View.GONE

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (android.provider.Settings.canDrawOverlays(this)) {
                    windowManager.addView(overlayView, layoutParams)
                }
            } else {
                windowManager.addView(overlayView, layoutParams)
            }
        } catch (e: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "DO_MISSION_CHANNEL")
            .setContentTitle(">Do: Misión Activa")
            .setContentText("No intentes escapar...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()

        try {
            startForeground(2, notification)
        } catch (e: Exception) {}

        isTimePunishment = intent?.getBooleanExtra("IS_TIME_PUNISHMENT", false) ?: false

        // RECIBIMOS LA ORDEN DEL CEREBRO
        blockType = intent?.getStringExtra("BLOCK_TYPE") ?: "Dios"
        val jsonList = intent?.getStringExtra("BLOCK_LIST_JSON") ?: "[]"

        // DESENCRIPTAMOS LA LISTA NEGRA SI NO ES DIOS NI HUMANO
        if (blockType != "Dios" && blockType != "Humano") {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                blackList = Gson().fromJson(jsonList, type)
            } catch (e: Exception) {
                blackList = emptySet()
            }
        }

        val btnGiveUp = overlayView.findViewById<Button>(R.id.btnGiveUpReal)

        if (isTimePunishment) {
            endTimeRealtime = Long.MAX_VALUE
            tvTimer.text = "TRAMPA"
            tvMissionName.text = intent?.getStringExtra("MISSION_NAME") ?: "¡TRAMPA DETECTADA!"
            btnGiveUp.text = "Ir a Ajustes"
            btnGiveUp.setOnClickListener {
                val settingsIntent = Intent(android.provider.Settings.ACTION_DATE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(settingsIntent)
            }
        } else {
            val durationSeconds = intent?.getIntExtra("DURATION_SECONDS", 60) ?: 60
            endTimeRealtime = android.os.SystemClock.elapsedRealtime() + (durationSeconds * 1000L)
            val missionName = intent?.getStringExtra("MISSION_NAME") ?: "Castigo Activo"
            tvMissionName.text = "¿Listo para:\n$missionName?"
            btnGiveUp.text = "Perdóname"
            btnGiveUp.setOnClickListener {
                overlayView.visibility = View.GONE
                val appIntent = Intent(this, host.senk.dosenk.ui.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(appIntent)
            }
        }

        startEngine()
        return START_STICKY
    }

    private fun startEngine() {
        engineJob = serviceScope.launch {
            while (isActive) {
                val timeLeftMillis = endTimeRealtime - android.os.SystemClock.elapsedRealtime()

                if (timeLeftMillis <= 0) {
                    //  Quitamos la pantalla negra
                    if (::overlayView.isInitialized) {
                        overlayView.visibility = View.GONE
                    }

                    // Abrimos la app automáticamente con un "Aviso de Victoria"
                    val victoryIntent = Intent(this@MissionBlockerService, host.senk.dosenk.ui.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("MISSION_VICTORY", true) // El aviso secreto
                    }
                    startActivity(victoryIntent)

                    //  Apagamos el servicio
                    stopSelf()
                    break
                }

                if (!isTimePunishment) {
                    val remainingSeconds = (timeLeftMillis / 1000).toInt()
                    val hours = remainingSeconds / 3600
                    val minutes = (remainingSeconds % 3600) / 60
                    val seconds = remainingSeconds % 60
                    tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }

                checkForegroundApp()
                delay(1000)
            }

            overlayView.visibility = View.GONE
            stopSelf()
        }
    }

    // EL CEREBRO DEL FRANCOTIRADOR
    private fun checkForegroundApp() {
        val currentApp = getForegroundAppPackage()

        // Las intocables
        val isLauncher = currentApp == defaultLauncherPackage
        val isMyApp = currentApp == packageName
        val isPhone = currentApp.contains("dialer") || currentApp.contains("telecom") || currentApp.contains("incallui")
        val isSettings = currentApp.contains("settings") || currentApp == "com.android.settings"

        if (currentApp.isEmpty() || isLauncher || isMyApp || isPhone || isSettings) {
            // Estás a salvo. Ocultamos el bloqueo.
            if (overlayView.visibility == View.VISIBLE) overlayView.visibility = View.GONE
            return
        }

        // Lógica de Castigo
        val shouldBlock = when (blockType) {
            "Dios" -> true // Bloquea todo lo que no sea lo de arriba
            "Humano", "Adicto" -> false
            else -> {
                // ES UN BLOQUEO PERSONALIZADO. ¿La app actual está en la lista negra?
                blackList.contains(currentApp)
            }
        }

        if (shouldBlock) {
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