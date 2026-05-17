package com.example.wearableseguridad

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WorkerDetailFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private var valueEventListener: ValueEventListener? = null
    private var idChaleco: String? = null
    private var trabajadorActual: Trabajador? = null

    private lateinit var tvNombre: TextView
    private lateinit var tvArea: TextView
    private lateinit var tvChaleco: TextView
    private lateinit var tvConexion: TextView
    private lateinit var tvTemp: TextView
    private lateinit var tvHum: TextView
    private lateinit var viewIndicadorCaida: View
    private lateinit var tvEstadoCaida: TextView
    private lateinit var viewIndicadorPanico: View
    private lateinit var tvEstadoPanico: TextView
    private lateinit var btnDarDeBaja: Button
    private lateinit var btnResetPassword: Button

    private val handler = Handler(Looper.getMainLooper())
    private val runnableReloj = object : Runnable {
        override fun run() {
            actualizarTiempoYEstado()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_worker_detail, container, false)

        idChaleco = arguments?.getString("idChaleco")

        tvNombre = view.findViewById(R.id.tvDetailNombre)
        tvArea = view.findViewById(R.id.tvDetailArea)
        tvChaleco = view.findViewById(R.id.tvDetailChaleco)
        tvConexion = view.findViewById(R.id.tvDetailConexion)
        tvTemp = view.findViewById(R.id.tvDetailTemp)
        tvHum = view.findViewById(R.id.tvDetailHum)
        viewIndicadorCaida = view.findViewById(R.id.viewIndicadorCaida)
        tvEstadoCaida = view.findViewById(R.id.tvEstadoCaida)
        viewIndicadorPanico = view.findViewById(R.id.viewIndicadorPanico)
        tvEstadoPanico = view.findViewById(R.id.tvEstadoPanico)
        btnDarDeBaja = view.findViewById(R.id.btnDarDeBaja)
        btnResetPassword = view.findViewById(R.id.btnResetPassword)

        tvChaleco.text = "ID: $idChaleco"

        if (idChaleco != null) {
            database = FirebaseDatabase.getInstance().getReference("trabajadores").child(idChaleco!!)
            escucharDatosEnVivo()
        }

        btnDarDeBaja.setOnClickListener { mostrarDialogoBaja() }
        btnResetPassword.setOnClickListener { mostrarDialogoReset() }

        return view
    }

    private fun escucharDatosEnVivo() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || context == null) return

                try {
                    val nombre = snapshot.child("nombre").value?.toString() ?: "Sin nombre"
                    val area = snapshot.child("area").value?.toString() ?: "Sin área"
                    val temp = snapshot.child("temperatura").value?.toString() ?: "0.0"
                    val hum = snapshot.child("humedad").value?.toString() ?: "0"
                    val conectado = snapshot.child("estado_conexion").getValue(Boolean::class.java) ?: false
                    val latido = snapshot.child("ultimo_latido").getValue(Long::class.java) ?: 0L
                    val caida = snapshot.child("alerta_caida").getValue(Boolean::class.java) ?: false
                    val panico = snapshot.child("boton_panico").getValue(Boolean::class.java) ?: false
                    val correo = snapshot.child("correo").value?.toString() ?: ""

                    trabajadorActual = Trabajador(
                        idChaleco = snapshot.key ?: "",
                        nombre = nombre,
                        area = area,
                        temperatura = temp,
                        humedad = hum,
                        estado_conexion = conectado,
                        ultimo_latido = latido,
                        alerta_caida = caida,
                        boton_panico = panico,
                        correo = correo,
                        activo = true
                    )

                    tvNombre.text = nombre
                    tvArea.text = area
                    actualizarTiempoYEstado()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(valueEventListener!!)
    }

    private fun actualizarTiempoYEstado() {
        if (!isAdded || activity == null) return

        val trabajador = trabajadorActual ?: return
        val tiempoActual = System.currentTimeMillis()
        val tiempoSinSenal = tiempoActual - trabajador.ultimo_latido
        val segundosPasados = tiempoSinSenal / 1000

        val estaDesconectado = !trabajador.estado_conexion || trabajador.ultimo_latido == 0L || tiempoSinSenal > 20000

        val tempFloat = trabajador.temperatura.toFloatOrNull() ?: 0f

        if (estaDesconectado || tempFloat == 0f) {
            tvConexion.text = if (tempFloat == 0f && !estaDesconectado) "Esperando datos..." else "Desconectado"
            tvConexion.setTextColor(Color.parseColor("#EF4444"))

            tvTemp.text = "--°C"
            tvHum.text = "--%"
            tvTemp.setTextColor(Color.parseColor("#999999"))
            tvHum.setTextColor(Color.parseColor("#999999"))

            configurarIndicador(viewIndicadorCaida, tvEstadoCaida, false, "Sin señal", true)
            configurarIndicador(viewIndicadorPanico, tvEstadoPanico, false, "Sin señal", true)
        } else {
            tvConexion.text = "Última conexión: hace $segundosPasados seg"
            tvConexion.setTextColor(Color.parseColor("#4CAF50"))

            tvTemp.text = "${trabajador.temperatura}°C"
            tvHum.text = "${trabajador.humedad}%"
            tvHum.setTextColor(Color.parseColor("#2B434D"))

            val prefs = activity?.getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
            val limite = prefs?.getFloat("limite_temp", 38.0f) ?: 38.0f

            tvTemp.setTextColor(if (tempFloat > limite) Color.parseColor("#EF4444") else Color.parseColor("#2B434D"))

            configurarIndicador(viewIndicadorCaida, tvEstadoCaida, trabajador.alerta_caida, "¡CAÍDA!")
            configurarIndicador(viewIndicadorPanico, tvEstadoPanico, trabajador.boton_panico, "¡PÁNICO!")
        }
    }

    private fun configurarIndicador(view: View, text: TextView, activo: Boolean, msg: String, offline: Boolean = false) {
        if (offline) {
            view.setBackgroundResource(R.drawable.circle_yellow)
            text.text = "Sin señal"
            text.setTextColor(Color.parseColor("#999999"))
        } else if (activo) {
            view.setBackgroundResource(R.drawable.circle_yellow)
            text.text = msg
            text.setTextColor(Color.parseColor("#EF4444"))
        } else {
            view.setBackgroundResource(R.drawable.circle_green)
            text.text = "Normal"
            text.setTextColor(Color.parseColor("#999999"))
        }
    }

    private fun mostrarDialogoReset() {
        val email = trabajadorActual?.correo ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Restablecer")
            .setMessage("¿Enviar correo a $email?")
            .setPositiveButton("Enviar") { _, _ ->
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener { Toast.makeText(context, "Correo enviado", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun mostrarDialogoBaja() {
        AlertDialog.Builder(requireContext())
            .setTitle("Dar de Baja")
            .setMessage("¿Estás seguro de dar de baja a este trabajador? El chaleco quedará libre.")
            .setPositiveButton("Sí") { _, _ -> ejecutarBaja() }
            .setNegativeButton("No", null).show()
    }

    private fun ejecutarBaja() {
        val id = idChaleco ?: return
        val t = trabajadorActual ?: return

        val refBajas = FirebaseDatabase.getInstance().getReference("trabajadores_baja")
        
        // Guardamos en la carpeta de bajas
        refBajas.child(id).setValue(t.copy(activo = false)).addOnSuccessListener {
            // 2. Borramos de la carpeta activa para liberar el chaleco
            database.removeValue().addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Trabajador dado de baja con éxito", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al procesar la baja", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { database.removeEventListener(it) }
        handler.removeCallbacks(runnableReloj)
    }
}