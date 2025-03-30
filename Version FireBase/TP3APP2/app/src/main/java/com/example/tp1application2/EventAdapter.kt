package com.example.tp1application2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private var events: MutableList<String>,
    private val onEditClick: (String, Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textEventDescription: TextView = view.findViewById(R.id.textEventDescription)
        val btnEditEvent: ImageButton = view.findViewById(R.id.btnEditEvent)
        val btnDeleteEvent: ImageButton = view.findViewById(R.id.btnDeleteEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.textEventDescription.text = event

        holder.btnEditEvent.setOnClickListener {
            onEditClick(event, position)
        }

        holder.btnDeleteEvent.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateList(newEvents: MutableList<String>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
