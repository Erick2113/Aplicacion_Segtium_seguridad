package com.example.wearableseguridad

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WorkerDashboardFragment : Fragment() {

    private var idChalecoActual: String? = null
    private lateinit var tvNombre: TextView
    private lateinit var tvArea: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvHum: TextView
    private lateinit var tvCaida: TextView
    private lateinit var tvPanico: TextView
    private lateinit var rootLayout: View
    
    private var trabajadorActual: Trabajador? = null

    // Cronómetro para detectar desconexión
    private val handler = Handler(Looper.getMainLooper())
    private val runnableReloj = object : Runnable {
        override fun run() {
            verificarEstadoConexion()
            handler.postDelayed(this, 2000) // Revisar cada 2 segundos
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_worker_dashboard, container, false)
        
        rootLayout = view.findViewById(R.id.fondoDashboard)
        tvNombre = view.findViewById(R.id.tvWorkerNombre)
        tvArea = view.findViewById(R.id.tvWorkerArea)
        tvTemp = view.findViewById(R.id.tvWorkerTemp)
        tvHum = view.findViewById(R.id.tvWorkerHum)
        tvCaida = view.findViewById(R.id.tvWorkerCaida)
        tvPanico = view.findViewById(R.id.tvWorkerPanico)

        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val refTrabajadores = FirebaseDatabase.getInstance().getReference("trabajadores")

        refTrabajadores.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                var encontrado = false
                for (chalecoSnapshot in snapshot.children) {
                    val correo = chalecoSnapshot.child("correo").getValue(String::class.java)
                    if (correo?.lowercase() == userEmail.lowercase()) {
                        encontrado = true
                        idChalecoActual = chalecoSnapshot.key
                        val trabajador = chalecoSnapshot.getValue(Trabajador::class.java)
                        if (trabajador != null) {
                            trabajadorActual = trabajador
                            actualizarUI(trabajador)
                        }
                        break
                    }
                }
                if (!encontrado) {
                    tvNombre.text = "Usuario no asignado"
                    tvTemp.text = "--°C"
                    tvHum.text = "--%"
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        return view
    }

    private fun verificarEstadoConexion() {
        val trabajador = trabajadorActual ?: return
        val tiempoActual = System.currentTimeMillis()
        val tiempoSinSenal = tiempoActual - trabajador.ultimo_latido

        // Si pasan más de 20 segundos sin señal forzamos el estado desconectado
        val estaDesconectado = !trabajador.estado_conexion || trabajador.ultimo_latido == 0L || tiempoSinSenal > 20000

        if (estaDesconectado) {
            tvTemp.text = "--°C"
            tvTemp.setTextColor(Color.GRAY)
            tvHum.text = "--%"
            tvHum.setTextColor(Color.GRAY)
            tvCaida.text = "Sensor de Caída: Sin señal"
            tvPanico.text = "Botón de Pánico: Sin señal"
            rootLayout.setBackgroundColor(Color.parseColor("#F3F0DF")) // Reset a crema
        }
    }

    private fun actualizarUI(trabajador: Trabajador) {
        val tiempoActual = System.currentTimeMillis()
        val tiempoSinSenal = tiempoActual - trabajador.ultimo_latido
        val estaDesconectado = !trabajador.estado_conexion || trabajador.ultimo_latido == 0L || tiempoSinSenal > 20000

        tvNombre.text = trabajador.nombre
        tvArea.text = "Área: ${trabajador.area}"

        if (estaDesconectado) {
            tvTemp.text = "--°C"
            tvTemp.setTextColor(Color.GRAY)
            tvHum.text = "--%"
            tvHum.setTextColor(Color.GRAY)
            tvCaida.text = "Sensor de Caída: Sin señal"
            tvPanico.text = "Botón de Pánico: Sin señal"
            rootLayout.setBackgroundColor(Color.parseColor("#F3F0DF"))
        } else {
            tvTemp.text = "${trabajador.temperatura}°C"
            tvTemp.setTextColor(Color.parseColor("#0B132B"))
            tvHum.text = "${trabajador.humedad}%"
            tvHum.setTextColor(Color.parseColor("#0B132B"))

            if (trabajador.alerta_caida) {
                tvCaida.text = "Sensor de Caída: ¡EMERGENCIA DETECTADA!"
                tvCaida.setTextColor(Color.RED)
            } else {
                tvCaida.text = "Sensor de Caída: Normal"
                tvCaida.setTextColor(Color.BLACK)
            }

            if (trabajador.boton_panico) {
                tvPanico.text = "Botón de Pánico: ¡ACTIVO!"
                tvPanico.setTextColor(Color.RED)
            } else {
                tvPanico.text = "Botón de Pánico: Inactivo"
                tvPanico.setTextColor(Color.BLACK)
            }

            // Alerta visual de fondo
            val tempFloat = trabajador.temperatura.toFloatOrNull() ?: 0f
            if (trabajador.alerta_caida || trabajador.boton_panico || tempFloat > 38.0) {
                rootLayout.setBackgroundColor(Color.parseColor("#FFCDD2"))
            } else {
                rootLayout.setBackgroundColor(Color.parseColor("#F3F0DF"))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(runnableReloj)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnableReloj)
    }
}