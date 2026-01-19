package host.senk.dosenk.ui.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor() : ViewModel() {

    // Aquí guardamos qué es el usuario
    var isStudent = false
    var isEmployee = false
    var isBusiness = false

    // Aquí guardamos los dibujos de cada etapa
    var schoolGrid: Array<IntArray>? = null
    var workGrid: Array<IntArray>? = null
    var businessGrid: Array<IntArray>? = null

    // Esta función combina los dibujos anteriores para bloquear celdas (ponerlas grises)
    fun getCombinedBlockedGrid(): Array<IntArray> {
        val cols = 7
        val rows = 24 // Ajustado a 24 horas
        val blocked = Array(cols) { IntArray(rows) { 0 } }

        // Si ya pintó escuela, bloquéalo
        schoolGrid?.let { grid -> addGridToBlocked(blocked, grid) }

        // Si ya pintó trabajo, bloquéalo también
        workGrid?.let { grid -> addGridToBlocked(blocked, grid) }

        return blocked
    }

    private fun addGridToBlocked(target: Array<IntArray>, source: Array<IntArray>) {
        for (c in 0 until 7) {
            for (r in 0 until 24) {
                if (source[c][r] == 1) target[c][r] = 1
            }
        }
    }
}