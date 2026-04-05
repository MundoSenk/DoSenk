package host.senk.dosenk.util

import android.animation.ValueAnimator
import android.widget.TextView

//  EL SISTEMA DE RANGOS (Leaderboard)
enum class DoRank(val threshold: Int, val title: String) {
    INFIERNO(0, "Infierno"),
    PURGATORIO(1000, "Purgatorio"),
    OLVIDABLE(3500, "Olvidable"),
    POTENCIAL(10000, "Potencial"),
    SKYWALKER(25000, "Skywalker"),
    IDONEO(60000, "Idóneo"),
    AS(120000, "As"),
    DIOS(250000, "Dios");

    companion object {
        fun getRankByXp(currentXp: Int): DoRank {
            return entries.lastOrNull { currentXp >= it.threshold } ?: INFIERNO
        }
    }
}

object GameEngine {

    // LA LÓGICA DEL TICKET DE PAGO (Calcula XP, Bonos y Multiplicadores)
    fun calculateTicket(
        durationMinutes: Int,
        streakDays: Int,
        topVices: List<String>,
        blockedApps: Set<String>
    ): MissionTicket {

        // 1. XP Base (1 min = 1 XP)
        val baseXP = durationMinutes

        // 2. Bono de Racha
        val streakBonusXP = (durationMinutes * streakDays) / 100

        // 3. Multiplicador por Vicio (Regla del Mayor)
        var maxMultiplier = 1.0

        if (blockedApps.isEmpty()) {
            // IMPUESTO A LA COBARDÍA (Modo Adicto o Bloqueo vacío)
            maxMultiplier = 0.5
        } else {
            // Buscamos si bloqueó alguna app del Top 5
            for (blockedApp in blockedApps) {
                val indexInTop = topVices.indexOf(blockedApp)
                if (indexInTop != -1) {
                    // Top 1 (index 0) = x6, Top 2 = x5... Top 5 = x2
                    val multiplierForThisApp = 6.0 - indexInTop
                    if (multiplierForThisApp > maxMultiplier) {
                        maxMultiplier = multiplierForThisApp
                    }
                }
            }
        }

        // 4. Total Final
        val totalXP = ((baseXP + streakBonusXP) * maxMultiplier).toInt()

        return MissionTicket(
            baseXP = baseXP,
            streakBonusXP = streakBonusXP,
            multiplier = maxMultiplier,
            totalXP = totalXP
        )
    }

 //ANIMACIÓN UX (Conteo Rápido)
    fun animateXpCounter(textView: TextView, finalValue: Int) {
        val animator = ValueAnimator.ofInt(0, finalValue)
        animator.duration = 1500 // 1.5 segundos de tensión
        animator.addUpdateListener { animation ->
            textView.text = "${animation.animatedValue} XP"
        }
        animator.start()
    }
}

// Estructura de datos para devolver el desglose a la UI
data class MissionTicket(
    val baseXP: Int,
    val streakBonusXP: Int,
    val multiplier: Double,
    val totalXP: Int
)