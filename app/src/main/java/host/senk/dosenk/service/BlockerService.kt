package host.senk.dosenk.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import host.senk.dosenk.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlockerService : AccessibilityService() {

    @Inject lateinit var userPreferences: UserPreferences

    // Corrutina para escuchar las preferencias en vivo
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Bandera local para no consultar BD a cada milisegundo
    private var isEmergencyActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Nos suscribimos al "Flow" para saber cuándo cambia el modo
        serviceScope.launch {
            userPreferences.isEmergencyActive.collectLatest { isActive ->
                isEmergencyActive = isActive
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Si el evento es nulo o NO estamos en emergencia, no hacemos nada
        if (event == null || !isEmergencyActive) return

        // Solo nos interesa cuando cambia la ventana (abres una app)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val packageName = event.packageName?.toString() ?: return

            // LA LISTA BLANCA (LO QUE SÍ DEJAS PASAR)
            if (isAllowedApp(packageName)) return

            //  SI LLEGAMOS AQUÍ, ES UNA APP PROHIBIDA

            // Bloqueo "Suave": Te mandamos al Home
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Regaño visual
            Toast.makeText(applicationContext, "¡ESTÁS EN MODO EMERGENCIA! CENTRATE", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAllowedApp(pkg: String): Boolean {
        return when {
            //  Nuestra propia App (Obvio, para poder desactivarlo)
            pkg == packageName -> true

            //  El Teléfono / Llamadas (Es emergencia, necesitas llamar)
            pkg.contains("dialer") -> true
            pkg.contains("android.phone") -> true
            pkg.contains("contacts") -> true
            pkg.contains("telecom") -> true

            //  System UI (Barra de notificaciones, launcher, teclado)
            pkg.contains("systemui") -> true
            pkg.contains("launcher") -> true
            pkg.contains("inputmethod") -> true // Teclado
            pkg.contains("gboard") -> true      // Teclado Google

            // TODO LO DEMÁS: ¡MUERE!
            else -> false
        }
    }

    override fun onInterrupt() {
        // Se llama si el sistema mata el servicio
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Limpiamos corrutinas
    }
}