package com.eddndev.purpura.ui.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eddndev.purpura.databinding.ItemEventBinding
import com.eddndev.purpura.domain.model.Event

// Lista de eventos de Inicio. DiffUtil por id (y por contenido, al ser Event un data class) para
// animar solo lo que cambia. El click se delega via `onClick`: hoy es un seam (TODO detalle).
class HomeEventAdapter(
    private val onClick: (Event) -> Unit,
) : ListAdapter<Event, HomeEventAdapter.EventViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onClick: (Event) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            val context = binding.root.context
            binding.dateText.text = EventDisplay.formatWhen(context, event.startsAt)
            binding.descriptionText.text = event.description
            binding.contactText.text = event.contact.name

            binding.typeBadge.apply {
                setText(EventDisplay.typeLabel(event.type))
                setTextColor(ContextCompat.getColor(context, EventDisplay.typeColor(event.type)))
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, EventDisplay.typeContainer(event.type)),
                )
            }
            binding.statusBadge.apply {
                setText(EventDisplay.statusLabel(event.status))
                setTextColor(ContextCompat.getColor(context, EventDisplay.statusColor(event.status)))
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, EventDisplay.statusContainer(event.status)),
                )
            }

            binding.root.setOnClickListener { onClick(event) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(old: Event, new: Event) = old.id == new.id
            override fun areContentsTheSame(old: Event, new: Event) = old == new
        }
    }
}
