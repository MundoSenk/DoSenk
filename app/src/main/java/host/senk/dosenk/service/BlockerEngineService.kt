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
import android.widget.TextView
import androidx.core.app.NotificationCompat
import host.senk.dosenk.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.TreeMap

class BlockerEngineService : Service() {

    companion object {

        val timeLeftFlow = kotlinx.coroutines.flow.MutableStateFlow(150)
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var tvBossTimer: TextView

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var engineJob: Job? = null

    private var timeLeftSeconds = 150 // Los 2:30 minutos
    private var defaultLauncherPackage: String = ""

    override fun onCreate() {
        super.onCreate()

        // Encontrar el launcher por defecto
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
        overlayView = LayoutInflater.from(themeContext).inflate(R.layout.layout_boss_overlay, null)

        tvBossTimer = overlayView.findViewById(R.id.tvBossTimer)


        // LA MAGIA DEL BOTÓN DE RENDICIÓN
        val btnGiveUp = overlayView.findViewById<View>(R.id.btnGiveUp)
        btnGiveUp.setOnClickListener {
            overlayView.visibility = View.GONE

            //regreso a do
            val appIntent = Intent(this, host.senk.dosenk.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(appIntent)
        }


        overlayView.visibility = View.GONE
        windowManager.addView(overlayView, layoutParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Los servicios inmortales exigen una Notificación obligatoria
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "DO_BLOCKER_CHANNEL")
            .setContentTitle(">Do Modo Dios Activado")
            .setContentText("El jefe te está vigilando...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()

        startForeground(1, notification)

        // ARRANCAR EL MOTOR Y EL RELOJ
        startEngine()

        return START_STICKY // Si el sistema lo mata, que lo reviva
    }

    private fun startEngine() {
        engineJob = serviceScope.launch {
            while (isActive && timeLeftSeconds > 0) {
                // ctualizar Reloj
                val minutes = timeLeftSeconds / 60
                val seconds = timeLeftSeconds % 60
                tvBossTimer.text = String.format("00:%02d:%02d", minutes, seconds)

                timeLeftFlow.value = timeLeftSeconds
                //  Vigilar qué app está abierta
                checkForegroundApp()

                // Esperar un segundo
                delay(1000)
                timeLeftSeconds--
            }

            timeLeftFlow.value = 0
            stopSelf()
        }
    }

    private fun checkForegroundApp() {
        val currentApp = getForegroundAppPackage()

        // Reglas del juego: Permitimos el menú de inicio, nuestra app, y la app de teléfono
        val isLauncher = currentApp == defaultLauncherPackage
        val isMyApp = currentApp == packageName
        val isPhone = currentApp.contains("dialer") || currentApp.contains("telecom") || currentApp.contains("incallui")

        if (!isLauncher && !isMyApp && !isPhone && currentApp.isNotEmpty()) {
            // ¡ESTÁ INTENTANDO ESCAPAR! Mostramos la jaula
            if (overlayView.visibility == View.GONE) {
                overlayView.visibility = View.VISIBLE
            }
        } else {
            // Está en el inicio o en nuestra app, lo dejamos respirar
            if (overlayView.visibility == View.VISIBLE) {
                overlayView.visibility = View.GONE
            }
        }
    }

    // Usamos el permiso de UsageStats para espiar la app actual
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
            val channel = NotificationChannel("DO_BLOCKER_CHANNEL", ">Do Motor de Bloqueo", NotificationManager.IMPORTANCE_LOW)
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