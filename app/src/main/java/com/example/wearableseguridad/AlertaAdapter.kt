package com.example.wearableseguridad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AlertaAdapter(private val listaAlertas: List<Alerta>) : RecyclerView.Adapter<AlertaAdapter.AlertaViewHolder>() {

    class AlertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvAlertaNombre)
        val tvTipo: TextView = itemView.findViewById(R.id.tvAlertaTipo)
        val tvFecha: TextView = itemView.findViewById(R.id.tvAlertaFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alerta, parent, false)
        return AlertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = listaAlertas[position]


        val nombre = alerta.nombre ?: ""

        // Si el nombre llega vacío o como ID de chaleco ponemos un texto de advertencia

        if (nombre.isEmpty() || nombre.startsWith("EMP-")) {
            holder.tvNombre.text = "Alerta de sistema (No asignado)"
        } else {
            holder.tvNombre.text = nombre
        }

        holder.tvTipo.text = "Alerta: ${alerta.tipo_emergencia ?: "N/A"}"

        val ts = alerta.timestamp
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

        // Manejo Timestamp
        when (ts) {
            is Long -> holder.tvFecha.text = sdf.format(Date(ts))
            is Map<*, *> -> holder.tvFecha.text = "Sincronizando..."
            else -> holder.tvFecha.text = "Reciente"
        }
    }

    override fun getItemCount(): Int = listaAlertas.size
}