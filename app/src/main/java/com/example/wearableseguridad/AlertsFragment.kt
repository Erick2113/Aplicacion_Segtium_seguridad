package com.example.wearableseguridad

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AlertsFragment : Fragment() {

    private lateinit var rvAlertas: RecyclerView
    private lateinit var alertaAdapter: AlertaAdapter
    private val listaDeAlertas = mutableListOf<Alerta>()
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)

        rvAlertas = view.findViewById(R.id.rvHistorialAlertas)
        rvAlertas.layoutManager = LinearLayoutManager(requireContext())

        alertaAdapter = AlertaAdapter(listaDeAlertas)
        rvAlertas.adapter = alertaAdapter

        database = FirebaseDatabase.getInstance().getReference("historial_emergencias")
        leerHistorial()

        return view
    }

    private fun leerHistorial() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                listaDeAlertas.clear()

                for (alertaSnapshot in snapshot.children) {
                    try {
                        val alerta = alertaSnapshot.getValue(Alerta::class.java)
                        if (alerta != null) {

                            // Verificamos que el nombre en la alerta no sea el ID por defecto

                            val nombreAlerta = alerta.nombre ?: ""
                            val noEsNombreGenerico = !nombreAlerta.startsWith("EMP-")
                            val tieneNombreReal = nombreAlerta.isNotEmpty() && nombreAlerta != "Sin nombre"

                            if (tieneNombreReal && noEsNombreGenerico) {
                                listaDeAlertas.add(alerta)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseError", "Dato mal formado en historial: ${e.message}")
                    }
                }

                // Invertimos para ver las más recientes arriba
                listaDeAlertas.reverse()
                alertaAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error al leer historial: ${error.message}")
            }
        })
    }
}