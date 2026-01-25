package host.senk.dosenk.util

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import host.senk.dosenk.R

// Función de Extensión: Ahora todas las Views saben pintarse solas
fun View.applyDoSenkGradient(
    orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT,
    cornerRadius: Float = 0f
) {
    val context = this.context

    // Preparamos las variables para cachar los colores
    val typedValueStart = TypedValue()
    val typedValueEnd = TypedValue()
    val theme = context.theme

    // Intentamos leer los atributos del tema actual (doSkinStart y doSkinEnd)
    val hasStart = theme.resolveAttribute(R.attr.doSkinStart, typedValueStart, true)
    val hasEnd = theme.resolveAttribute(R.attr.doSkinEnd, typedValueEnd, true)

    // Si no encuentra colores (por seguridad), no hace nada o usa default
    if (!hasStart || !hasEnd) return

    val startColor = typedValueStart.data
    val endColor = typedValueEnd.data

    //  Creamos el gradiente
    val gradient = GradientDrawable(
        orientation,
        intArrayOf(startColor, endColor)
    )

    // Si pediste esquinas redondas, se las ponemos
    if (cornerRadius > 0) {
        gradient.cornerRadius = cornerRadius
    }


    this.background = gradient
}