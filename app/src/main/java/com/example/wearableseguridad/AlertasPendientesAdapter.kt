package com.example.wearableseguridad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Recibimos la lista
class AlertasPendientesAdapter(
    private val lista: List<Pair<String, Alerta>>,
    private val onAtendidaClick: (String) -> Unit
) : RecyclerView.Adapter<AlertasPendientesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvAlertaPendienteNombre)
        val tvTipo: TextView = view.findViewById(R.id.tvAlertaPendienteTipo)
        val tvHora: TextView = view.findViewById(R.id.tvAlertaPendienteHora)
        val btnAtender: Button = view.findViewById(R.id.btnMarcarAtendida)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alerta_pendiente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        val keyFirebase = item.first
        val alerta = item.second

        holder.tvNombre.text = alerta.nombre ?: "Trabajador Desconocido"
        holder.tvTipo.text = alerta.tipo_emergencia ?: "Alerta"

        // Formatear la hora
        val ts = alerta.timestamp
        if (ts is Long) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            holder.tvHora.text = "Ocurrió a las: ${sdf.format(Date(ts))}"
        } else {
            holder.tvHora.text = "Hora registrada"
        }

        // Botón para marcar como atendida
        holder.btnAtender.setOnClickListener {
            onAtendidaClick(keyFirebase)
        }
    }

    override fun getItemCount(): Int = lista.size
}