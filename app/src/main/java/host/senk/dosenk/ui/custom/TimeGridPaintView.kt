package host.senk.dosenk.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TimeGridPaintView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val numColumns = 7
    private val numRows = 24

    // 0=Libre, 1=Pintado (Activo), 2=Bloqueado (Gris)
    private val gridState = Array(numColumns) { IntArray(numRows) { 0 } }

    private val paintCell = Paint().apply { style = Paint.Style.FILL }
    private val paintBorder = Paint().apply {
        style = Paint.Style.STROKE
        color = 0xFFE0E0E0.toInt()
        strokeWidth = 2f
    }

    private var activeColor = 0xFFFF0000.toInt()
    private var blockedColor = 0xFFB0B0B0.toInt()
    private var emptyColor = 0xFFFFFFFF.toInt()

    private var cellWidth = 0f
    private var cellHeight = 0f
    private val cellRect = RectF()

    // Variable para saber si en este movimiento estamos Pintando o Borrando
    private var currentActionParams = 1

    fun setThemeColor(color: Int) {
        activeColor = color
        invalidate()
    }

    fun setBlockedCells(blockedGrid: Array<IntArray>) {
        for (c in 0 until numColumns) {
            for (r in 0 until numRows) {
                if (blockedGrid[c][r] == 1) gridState[c][r] = 2 // Lo convertimos en bloqueado
            }
        }
        invalidate()
    }

    fun getCurrentSelection(): Array<IntArray> {
        val selection = Array(numColumns) { IntArray(numRows) { 0 } }
        for (c in 0 until numColumns) {
            for (r in 0 until numRows) {
                if (gridState[c][r] == 1) selection[c][r] = 1
            }
        }
        return selection
    }

    // Limpiar selección activa
    fun clearActiveSelection() {
        for (c in 0 until numColumns) {
            for (r in 0 until numRows) {
                if (gridState[c][r] == 1) gridState[c][r] = 0
            }
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        // Forzamos que la altura sea (altura de celda * 24 filas)
        // Vamos a asumir una altura fija de celda de unos 40dp (ajustar con la densidad de pantalla)
        val density = resources.displayMetrics.density
        val desiredHeight = (40 * density * numRows).toInt()

        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, desiredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellWidth = w / numColumns.toFloat()
        // La altura de celda se calcula basada en la altura total que definimos en onMeasure
        cellHeight = h / numRows.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (c in 0 until numColumns) {
            for (r in 0 until numRows) {
                val state = gridState[c][r]
                paintCell.color = when(state) {
                    1 -> activeColor
                    2 -> blockedColor
                    else -> emptyColor
                }

                val left = c * cellWidth
                val top = r * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                cellRect.set(left + 4, top + 4, right - 4, bottom - 4)
                canvas.drawRoundRect(cellRect, 12f, 12f, paintCell)

                if (state == 0) {
                    canvas.drawRoundRect(cellRect, 12f, 12f, paintBorder)
                }
            }
        }
    }




    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val col = (event.x / cellWidth).toInt().coerceIn(0, numColumns - 1)
        val row = (event.y / cellHeight).toInt().coerceIn(0, numRows - 1)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                // Si toco algo vacío (0)
                // Si toco algo lleno (1)
                // Si está bloqueado (2)
                if (gridState[col][row] != 2) {
                    currentActionParams = if (gridState[col][row] == 0) 1 else 0
                    gridState[col][row] = currentActionParams
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Aplico la misma acción mientras arrastra el dedo
                if (gridState[col][row] != 2) {
                    gridState[col][row] = currentActionParams
                    invalidate()
                }
            }
        }
        return true
    }
}