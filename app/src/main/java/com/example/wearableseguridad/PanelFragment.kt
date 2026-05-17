package com.example.wearableseguridad

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class PanelFragment : Fragment() {

    private lateinit var tvPanelTrabajadores: TextView
    private lateinit var tvPanelAlertas: TextView
    private lateinit var tvClimaGrados: TextView
    private lateinit var tvClimaEstado: TextView
    private lateinit var spinnerUbicacion: Spinner
    private lateinit var rvAlertasPendientes: RecyclerView

    private val alertasPendientesList = mutableListOf<Pair<String, Alerta>>()
    private lateinit var adaptador: AlertasPendientesAdapter

    private val ubicaciones = mapOf(
        "Ahuachapán" to Pair(13.9214, -89.845),
        "Cabañas" to Pair(13.8642, -88.7522),
        "Chalatenango" to Pair(14.0406, -88.9367),
        "Cuscatlán" to Pair(13.8631, -89.0561),
        "La Libertad" to Pair(13.6769, -89.2881),
        "La Paz" to Pair(13.4833, -88.9833),
        "La Unión" to Pair(13.3369, -87.8439),
        "Morazán" to Pair(13.8114, -88.1186),
        "San Miguel" to Pair(13.4833, -88.1833),
        "San Salvador" to Pair(13.6929, -89.2182),
        "San Vicente" to Pair(13.6442, -88.7836),
        "Santa Ana" to Pair(13.9942, -89.5597),
        "Sonsonate" to Pair(13.7189, -89.7242),
        "Usulután" to Pair(13.3456, -88.4394)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_panel, container, false)

        tvPanelTrabajadores = view.findViewById(R.id.tvPanelTrabajadores)
        tvPanelAlertas = view.findViewById(R.id.tvPanelAlertas)
        tvClimaGrados = view.findViewById(R.id.tvClimaGrados)
        tvClimaEstado = view.findViewById(R.id.tvClimaEstado)
        spinnerUbicacion = view.findViewById(R.id.spinnerUbicacion)
        rvAlertasPendientes = view.findViewById(R.id.rvAlertasPendientes)

        rvAlertasPendientes.layoutManager = LinearLayoutManager(context)
        adaptador = AlertasPendientesAdapter(alertasPendientesList) { keyAlerta ->
            marcarAlertaComoAtendida(keyAlerta)
        }
        rvAlertasPendientes.adapter = adaptador

        configurarSpinner()
        cargarResumenConexion()  // tvPanelTrabajadores
        cargarAlertasPendientes() // tvPanelAlertas

        return view
    }

    private fun configurarSpinner() {
        val activityRef = activity ?: return
        val prefs = activityRef.getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val ciudadGuardada = prefs.getString("ciudad_clima", "San Salvador") ?: "San Salvador"

        val ciudades = ubicaciones.keys.sorted().toList()
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ciudades)
        spinnerUbicacion.adapter = spinnerAdapter

        val posicionGuardada = ciudades.indexOf(ciudadGuardada)
        if (posicionGuardada >= 0) spinnerUbicacion.setSelection(posicionGuardada)

        spinnerUbicacion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val ciudadElegida = ciudades[position]
                prefs.edit().putString("ciudad_clima", ciudadElegida).apply()
                ubicaciones[ciudadElegida]?.let { cargarClimaLocal(it.first, it.second) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    //  Personal conectado "En Sitio"
    private fun cargarResumenConexion() {
        val database = FirebaseDatabase.getInstance().getReference("trabajadores")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                var totalEnSitio = 0
                val tiempoActual = System.currentTimeMillis()

                for (trabajadorSnapshot in snapshot.children) {
                    if (!trabajadorSnapshot.hasChild("nombre")) continue

                    val ultimoLatido = trabajadorSnapshot.child("ultimo_latido").getValue(Long::class.java) ?: 0L
                    val estadoConexion = trabajadorSnapshot.child("estado_conexion").getValue(Boolean::class.java) ?: false

                    if (estadoConexion && (tiempoActual - ultimoLatido < 20000)) {
                        totalEnSitio++
                    }
                }
                tvPanelTrabajadores.text = totalEnSitio.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Gestiona tvPanelAlertas y la lista inferior
    private fun cargarAlertasPendientes() {
        val database = FirebaseDatabase.getInstance().getReference("historial_emergencias")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                alertasPendientesList.clear()
                var pendientesContadorManual = 0

                for (alertaSnapshot in snapshot.children) {
                    val estado = alertaSnapshot.child("estado").getValue(String::class.java) ?: "pendiente"

                    if (estado != "atendida") {
                        try {
                            val nombre = alertaSnapshot.child("nombre").value?.toString() ?: ""

                            // Solo procesar si el nombre es real no solo el nombre de chaleco
                            if (nombre.isNotEmpty() && !nombre.startsWith("EMP-") && nombre != "Desconocido") {
                                pendientesContadorManual++

                                val id = alertaSnapshot.child("id_trabajador").value?.toString() ?: ""
                                val area = alertaSnapshot.child("area").value?.toString() ?: "N/A"
                                val tipo = alertaSnapshot.child("tipo_emergencia").value?.toString() ?: "Alerta"
                                val ts = alertaSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                                val alerta = Alerta(id, nombre, area, tipo, ts)
                                alertasPendientesList.add(Pair(alertaSnapshot.key!!, alerta))
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                // Actualiza tvPanelAlertas este numero soloo baja si se atiende
                tvPanelAlertas.text = pendientesContadorManual.toString()

                alertasPendientesList.reverse()
                adaptador.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun marcarAlertaComoAtendida(keyAlerta: String) {
        val ref = FirebaseDatabase.getInstance().getReference("historial_emergencias").child(keyAlerta)
        val actualizaciones = mapOf("estado" to "atendida", "hora_atendida" to ServerValue.TIMESTAMP)
        ref.updateChildren(actualizaciones).addOnSuccessListener {
            if (isAdded) Toast.makeText(context, "Emergencia atendida", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarClimaLocal(lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                val respuesta = URL(apiUrl).readText()
                val json = JSONObject(respuesta)
                val climaActual = json.getJSONObject("current_weather")
                val temperatura = climaActual.getDouble("temperature")
                val esDeDia = climaActual.getInt("is_day")

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        tvClimaGrados.text = "${temperatura}°C"
                        tvClimaEstado.text = if (esDeDia == 1) "Despejado" else "Noche"
                    }
                }
            } catch (e: Exception) { }
        }
    }
}