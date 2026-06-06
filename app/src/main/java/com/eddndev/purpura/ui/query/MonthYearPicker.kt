package com.eddndev.purpura.ui.query

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import com.eddndev.purpura.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

// Selector de Mes/Ano para Consultar (REQ-QUERY-005..006). Android no trae un picker de mes, asi que
// armamos un dialogo con NumberPicker(s): la rueda de mes muestra nombres en espanol y la de ano un
// rango fijo. onCancel (cancelar/atras/tocar fuera) permite al Fragment revertir a "Todos" si se
// cierra sin elegir (mismo criterio que el selector de fecha de Dia/Rango). Se devuelve el AlertDialog
// para que el Fragment lo descarte en onDestroyView y no lo filtre al rotar; como NO usamos el
// listener de dismiss, ese descarte programatico no dispara onCancel (no revierte el modo al rotar).
object MonthYearPicker {

    private const val MIN_YEAR = 2000
    private const val MAX_YEAR = 2100
    private val LOCALE = Locale("es", "MX")

    fun showMonth(
        context: Context,
        initialYear: Int,
        initialMonth: Int,
        onPicked: (year: Int, month: Int) -> Unit,
        onCancel: () -> Unit,
    ): AlertDialog {
        val monthPicker = NumberPicker(context).apply {
            minValue = 1
            maxValue = 12
            displayedValues = monthNames()
            value = initialMonth.coerceIn(1, 12)
            wrapSelectorWheel = false
        }
        val yearPicker = yearPicker(context, initialYear)
        return showDialog(
            context = context,
            titleRes = R.string.query_month_picker_title,
            pickers = listOf(monthPicker, yearPicker),
            onAccept = { onPicked(yearPicker.value, monthPicker.value) },
            onCancel = onCancel,
        )
    }

    fun showYear(
        context: Context,
        initialYear: Int,
        onPicked: (year: Int) -> Unit,
        onCancel: () -> Unit,
    ): AlertDialog {
        val yearPicker = yearPicker(context, initialYear)
        return showDialog(
            context = context,
            titleRes = R.string.query_year_picker_title,
            pickers = listOf(yearPicker),
            onAccept = { onPicked(yearPicker.value) },
            onCancel = onCancel,
        )
    }

    // "Junio 2026" para el boton de periodo. Vive aqui para que el formato de mes quede junto al
    // picker que lo elige (primer caracter en mayuscula, como una etiqueta).
    fun label(year: Int, month: Int): String =
        Month.of(month.coerceIn(1, 12)).getDisplayName(TextStyle.FULL, LOCALE)
            .replaceFirstChar { it.titlecase(LOCALE) } + " " + year

    private fun yearPicker(context: Context, initialYear: Int): NumberPicker =
        NumberPicker(context).apply {
            minValue = MIN_YEAR
            maxValue = MAX_YEAR
            value = initialYear.coerceIn(MIN_YEAR, MAX_YEAR)
            wrapSelectorWheel = false
        }

    private fun monthNames(): Array<String> = (1..12).map { month ->
        Month.of(month).getDisplayName(TextStyle.FULL, LOCALE)
            .replaceFirstChar { it.titlecase(LOCALE) }
    }.toTypedArray()

    private fun showDialog(
        context: Context,
        titleRes: Int,
        pickers: List<NumberPicker>,
        onAccept: () -> Unit,
        onCancel: () -> Unit,
    ): AlertDialog {
        val pad = (PADDING_DP * context.resources.displayMetrics.density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            pickers.forEach { picker ->
                addView(
                    picker,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
            }
        }
        // Semantica de cancelacion (no de dismiss): el boton Cancelar y setOnCancelListener (atras /
        // tocar fuera) son cancelaciones reales del usuario -> onCancel. Aceptar -> onAccept. Un
        // dismiss() programatico (descarte del Fragment al rotar) NO dispara nada, asi no revierte el
        // modo. Cancelar via boton no llama tambien al cancel-listener: solo lo hace atras/tocar-fuera.
        return MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setView(container)
            .setNegativeButton(R.string.action_cancel) { _, _ -> onCancel() }
            .setPositiveButton(R.string.action_accept) { _, _ -> onAccept() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private const val PADDING_DP = 16
}
