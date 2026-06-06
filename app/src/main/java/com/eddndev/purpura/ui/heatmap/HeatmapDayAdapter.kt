package com.eddndev.purpura.ui.heatmap

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.ItemHeatmapDayBinding

// Rejilla de mosaicos del mapa de calor (7 columnas). Cada celda es un dia tenido por su nivel de
// intensidad (heatmap_0..4) o un relleno vacio. Lista pequena y fija: notifyDataSetChanged.
class HeatmapDayAdapter(
    private val onDayClick: (HeatmapCell.Day) -> Unit,
) : RecyclerView.Adapter<HeatmapDayAdapter.CellViewHolder>() {

    private var cells: List<HeatmapCell> = emptyList()

    fun submit(newCells: List<HeatmapCell>) {
        cells = newCells
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cells.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val binding = ItemHeatmapDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CellViewHolder(binding, onDayClick)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        holder.bind(cells[position])
    }

    class CellViewHolder(
        private val binding: ItemHeatmapDayBinding,
        private val onDayClick: (HeatmapCell.Day) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: HeatmapCell) {
            when (cell) {
                is HeatmapCell.Empty -> {
                    binding.dayNumber.text = ""
                    binding.heatmapDayCell.background = null
                    binding.heatmapDayCell.isClickable = false
                    binding.heatmapDayCell.setOnClickListener(null)
                }
                is HeatmapCell.Day -> bindDay(cell)
            }
        }

        private fun bindDay(cell: HeatmapCell.Day) {
            val context = binding.root.context
            binding.dayNumber.text = cell.date.dayOfMonth.toString()
            binding.heatmapDayCell.background =
                ContextCompat.getDrawable(context, R.drawable.bg_heatmap_cell)
            binding.heatmapDayCell.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, colorRes(cell.level)),
            )
            // Contraste del numero en claro Y oscuro (los tonos se invierten en values-night):
            // 0..2 usan on_surface (tematizado), 3 blanco, 4 un color tematizado (blanco/oscuro).
            val textColorRes = when (cell.level) {
                in 0..2 -> R.color.purpura_on_surface
                3 -> R.color.white
                else -> R.color.heatmap_label_intense
            }
            binding.dayNumber.setTextColor(ContextCompat.getColor(context, textColorRes))
            binding.heatmapDayCell.isClickable = true
            binding.heatmapDayCell.setOnClickListener { onDayClick(cell) }
        }

        private fun colorRes(level: Int): Int = when (level) {
            0 -> R.color.heatmap_0
            1 -> R.color.heatmap_1
            2 -> R.color.heatmap_2
            3 -> R.color.heatmap_3
            else -> R.color.heatmap_4
        }
    }
}
