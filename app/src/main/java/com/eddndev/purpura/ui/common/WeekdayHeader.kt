package com.eddndev.purpura.ui.common

import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eddndev.purpura.R
import java.time.temporal.WeekFields
import java.util.Locale

// Llena un contenedor con las 7 etiquetas de dias de la semana segun el primer dia del locale
// (es-MX = domingo), para que su orden coincida con el de MonthGrid.cells. Compartido por el
// Calendario y el Mapa de calor.
fun LinearLayout.bindWeekdayHeader(locale: Locale) {
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    removeAllViews()
    MonthGrid.weekdayLabels(firstDayOfWeek, locale).forEach { label ->
        val cell = TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextAppearance(R.style.TextAppearance_Purpura_LabelMedium)
            setTextColor(ContextCompat.getColor(context, R.color.purpura_on_surface_variant))
        }
        addView(cell)
    }
}
