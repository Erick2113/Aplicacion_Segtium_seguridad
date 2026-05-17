package com.example.wearableseguridad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminAdapter(
    private var admins: List<AdminUser>,
    private val onItemClick: (AdminUser) -> Unit
) : RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvAdminNombre)
        val tvCorreo: TextView = view.findViewById(R.id.tvAdminCorreo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val admin = admins[position]
        holder.tvNombre.text = admin.nombre
        holder.tvCorreo.text = admin.correo
        holder.itemView.setOnClickListener { onItemClick(admin) }
    }

    override fun getItemCount() = admins.size

    fun updateList(newList: List<AdminUser>) {
        admins = newList
        notifyDataSetChanged()
    }
}

data class AdminUser(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = ""
)