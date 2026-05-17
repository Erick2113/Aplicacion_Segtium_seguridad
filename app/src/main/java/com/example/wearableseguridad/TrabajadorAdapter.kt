package com.example.wearableseguridad

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TrabajadorAdapter(
    private var listaOriginal: List<Trabajador>,
    private val onTrabajadorClick: (Trabajador) -> Unit
) : RecyclerView.Adapter<TrabajadorAdapter.TrabajadorViewHolder>() {

    private var listaFiltrada: List<Trabajador> = listaOriginal

    class TrabajadorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreTrabajador)
        val tvCargo: TextView = itemView.findViewById(R.id.tvCargoTrabajador)
        val tvEstadoTexto: TextView = itemView.findViewById(R.id.tvEstadoTexto)
        val viewEstadoPunto: View = itemView.findViewById(R.id.viewEstadoPunto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrabajadorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trabajador, parent, false)
        return TrabajadorViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrabajadorViewHolder, position: Int) {
        val trabajador = listaFiltrada[position]
        holder.tvNombre.text = trabajador.nombre
        holder.tvCargo.text = trabajador.area



        val tiempoActual = System.currentTimeMillis()
        val tiempoSinSenal = tiempoActual - trabajador.ultimo_latido

        //  Definimos si está desconectado de verdad
        val estaDesconectado = !trabajador.estado_conexion || trabajador.ultimo_latido == 0L || tiempoSinSenal > 20000


        when {
            estaDesconectado -> {
                // Si no hay señal, ignoramos alertas
                holder.tvEstadoTexto.text = "Desconectado"
                holder.tvEstadoTexto.setTextColor(Color.parseColor("#999999")) // Gris
                holder.viewEstadoPunto.setBackgroundResource(R.drawable.circle_yellow)
            }
            trabajador.alerta_caida || trabajador.boton_panico -> {
                // Si está conectado y hay una emergencia real
                holder.tvEstadoTexto.text = "Emergencia"
                holder.tvEstadoTexto.setTextColor(Color.parseColor("#EF4444"))
                holder.viewEstadoPunto.setBackgroundResource(R.drawable.circle_yellow)
            }
            else -> {
                // Conectado y normal
                holder.tvEstadoTexto.text = "En sitio"
                holder.tvEstadoTexto.setTextColor(Color.parseColor("#2B434D"))
                holder.viewEstadoPunto.setBackgroundResource(R.drawable.circle_green)
            }
        }

        holder.itemView.setOnClickListener { onTrabajadorClick(trabajador) }
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun filtrar(texto: String) {
        listaFiltrada = if (texto.isEmpty()) {
            listaOriginal
        } else {
            listaOriginal.filter {
                it.nombre.lowercase(Locale.ROOT).contains(texto.lowercase(Locale.ROOT))
            }
        }
        notifyDataSetChanged()
    }

    fun actualizarLista(nuevaLista: List<Trabajador>) {
        listaOriginal = nuevaLista
        listaFiltrada = nuevaLista
        notifyDataSetChanged()
    }
}