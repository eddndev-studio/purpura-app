package com.eddndev.purpura.ui.common

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.ItemEventBinding
import com.eddndev.purpura.domain.model.Event

// Lista de tarjetas de evento, compartida por Inicio y Consultar. DiffUtil por id (y por
// contenido, al ser Event un data class) para animar solo lo que cambia. El click se delega
// via `onClick` (navegar al Detalle).
class EventListAdapter(
    private val onClick: (Event) -> Unit,
) : ListAdapter<Event, EventListAdapter.EventViewHolder>(DIFF) {

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
            val typeColor = ContextCompat.getColor(context, EventDisplay.typeColor(event.type))

            // Tesela de fecha leading: SIEMPRE morada (marca consistente); el tipo se lee en el chip.
            // El fondo y los colores de la tesela/hora son estaticos en item_event.xml.
            binding.monthText.text = EventDisplay.formatMonthAbbrev(event.startsAt)
            binding.dayNumberText.text = EventDisplay.formatDayNumber(event.startsAt)
            binding.timeText.text = EventDisplay.formatTime(event.startsAt)

            binding.descriptionText.text = event.description

            // Contacto opcional: GONE si viene vacio (el goneMarginTop del badgeRow colapsa el hueco).
            val contactName = event.contact.name
            binding.contactText.text = contactName
            binding.contactText.isVisible = contactName.isNotBlank()

            binding.typeBadge.apply {
                setText(EventDisplay.typeLabel(event.type))
                setTextColor(typeColor)
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

            // a11y: al fragmentar fecha/hora, TalkBack debe leer la tarjeta como una unidad.
            binding.eventCard.contentDescription = context.getString(
                R.string.event_card_a11y,
                EventDisplay.formatFullDate(event.startsAt),
                EventDisplay.formatTime(event.startsAt),
                event.description,
                context.getString(EventDisplay.typeLabel(event.type)),
                context.getString(EventDisplay.statusLabel(event.status)),
            )

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
