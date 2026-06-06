package com.eddndev.purpura.ui.calendar

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.ItemCalendarDayBinding
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.ui.common.EventDisplay
import java.time.LocalDate

// Rejilla de dias del mes (7 columnas). Cada celda es un dia (numero + hasta MAX_DOTS puntos por
// tipo) o una celda vacia de relleno. La lista es pequena y fija: notifyDataSetChanged por estado.
class CalendarDayAdapter(
    private val onDayClick: (LocalDate) -> Unit,
) : RecyclerView.Adapter<CalendarDayAdapter.CellViewHolder>() {

    private var cells: List<CalendarCell> = emptyList()

    fun submit(newCells: List<CalendarCell>) {
        cells = newCells
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cells.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CellViewHolder(binding, onDayClick)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        holder.bind(cells[position])
    }

    class CellViewHolder(
        private val binding: ItemCalendarDayBinding,
        private val onDayClick: (LocalDate) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: CalendarCell) {
            when (cell) {
                is CalendarCell.Empty -> bindEmpty()
                is CalendarCell.Day -> bindDay(cell)
            }
        }

        private fun bindEmpty() {
            binding.dayNumber.text = ""
            binding.dotsRow.removeAllViews()
            binding.root.background = null
            binding.root.isClickable = false
            binding.root.setOnClickListener(null)
        }

        private fun bindDay(cell: CalendarCell.Day) {
            val context = binding.root.context
            binding.dayNumber.text = cell.date.dayOfMonth.toString()

            val backgroundRes = when {
                cell.isSelected -> R.drawable.bg_calendar_day_selected
                cell.isToday -> R.drawable.bg_calendar_day_today
                else -> 0
            }
            binding.root.background =
                if (backgroundRes == 0) null else ContextCompat.getDrawable(context, backgroundRes)

            val textColorRes = when {
                cell.isSelected -> R.color.purpura_on_primary_container
                cell.isToday -> R.color.purpura_primary
                else -> R.color.purpura_on_surface
            }
            binding.dayNumber.setTextColor(ContextCompat.getColor(context, textColorRes))

            bindDots(cell.typeDots)
            binding.root.isClickable = true
            binding.root.setOnClickListener { onDayClick(cell.date) }
        }

        private fun bindDots(types: List<EventType>) {
            val context = binding.root.context
            binding.dotsRow.removeAllViews()
            val size = context.resources.getDimensionPixelSize(R.dimen.event_dot_size)
            val gap = context.resources.getDimensionPixelSize(R.dimen.space_xxs)
            types.take(MAX_DOTS).forEach { type ->
                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginStart = gap
                        marginEnd = gap
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.bg_dot)
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, EventDisplay.typeColor(type)),
                    )
                }
                binding.dotsRow.addView(dot)
            }
        }

        private companion object {
            const val MAX_DOTS = 3
        }
    }
}
