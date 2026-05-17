package com.example.wearableseguridad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WorkerAlertsFragment : Fragment() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var adapter: AlertaAdapter
    private lateinit var tvNoAlerts: TextView
    private val myAlertsList = mutableListOf<Alerta>()
    private var idChalecoActual: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_worker_alerts, container, false)

        rvAlerts = view.findViewById(R.id.rvWorkerAlerts)
        tvNoAlerts = view.findViewById(R.id.tvNoAlertsWorker)
        rvAlerts.layoutManager = LinearLayoutManager(context)
        adapter = AlertaAdapter(myAlertsList)
        rvAlerts.adapter = adapter

        obtenerIdChalecoYAlertas()

        return view
    }

    private fun obtenerIdChalecoYAlertas() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val refTrabajadores = FirebaseDatabase.getInstance().getReference("trabajadores")

        // primero buscamos el ID del chaleco asignado a este correo
        refTrabajadores.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                for (chalecoSnapshot in snapshot.children) {
                    val correo = chalecoSnapshot.child("correo").getValue(String::class.java)
                    if (correo?.lowercase() == userEmail.lowercase()) {
                        idChalecoActual = chalecoSnapshot.key
                        cargarMisAlertas()
                        break
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun cargarMisAlertas() {
        val idBusqueda = idChalecoActual ?: return
        val ref = FirebaseDatabase.getInstance().getReference("historial_emergencias")

        // filtramos para que el trabajador solo vea sus alertas
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                myAlertsList.clear()
                for (alertaSnapshot in snapshot.children) {
                    val alerta = alertaSnapshot.getValue(Alerta::class.java)
                    // Comparamos el id_trabajador de la alerta con el chaleco del usuario
                    if (alerta != null && alerta.id_trabajador == idBusqueda) {
                        myAlertsList.add(alerta)
                    }
                }
                
                myAlertsList.reverse() // Más recientes arriba
                adapter.notifyDataSetChanged()

                if (myAlertsList.isEmpty()) {
                    tvNoAlerts.visibility = View.VISIBLE
                } else {
                    tvNoAlerts.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}