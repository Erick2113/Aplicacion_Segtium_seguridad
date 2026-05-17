package com.example.wearableseguridad

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

class WorkersFragment : Fragment() {

    private lateinit var rvTrabajadores: RecyclerView
    private lateinit var trabajadorAdapter: TrabajadorAdapter
    private val listaDatosCrudos = mutableListOf<Trabajador>()
    private val listaTrabajadores = mutableListOf<Trabajador>()

    private lateinit var tvTotalWorkers: TextView
    private lateinit var tvOnSiteWorkers: TextView
    private lateinit var tvOffSiteWorkers: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val runnableRevisor = object : Runnable {
        override fun run() {
            revisarDesconexionesLocales()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workers, container, false)

        rvTrabajadores = view.findViewById(R.id.rvTrabajadores)
        rvTrabajadores.layoutManager = LinearLayoutManager(context)

        tvTotalWorkers = view.findViewById(R.id.tvTotalWorkers)
        tvOnSiteWorkers = view.findViewById(R.id.tvOnSiteWorkers)
        tvOffSiteWorkers = view.findViewById(R.id.tvOffSiteWorkers)

        trabajadorAdapter = TrabajadorAdapter(listaTrabajadores) { trabajador ->
            abrirDetallesTrabajador(trabajador)
        }
        rvTrabajadores.adapter = trabajadorAdapter

        view.findViewById<EditText>(R.id.etBuscarTrabajador).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                trabajadorAdapter.filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<FloatingActionButton>(R.id.fabAgregarTrabajador).setOnClickListener {
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
        }

        cargarTrabajadoresDesdeFirebase()
        return view
    }

    private fun cargarTrabajadoresDesdeFirebase() {
        val database = FirebaseDatabase.getInstance().getReference("trabajadores")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                listaDatosCrudos.clear()

                for (trabajadorSnapshot in snapshot.children) {

                    // Si el chaleco no tiene "nombre" , no lo mostramos
                    if (!trabajadorSnapshot.hasChild("nombre")) continue

                    try {
                        val nombre = trabajadorSnapshot.child("nombre").value?.toString() ?: "Sin nombre"
                        val temp = trabajadorSnapshot.child("temperatura").value?.toString() ?: "--"
                        val hum = trabajadorSnapshot.child("humedad").value?.toString() ?: "--"
                        val bat = trabajadorSnapshot.child("bateria").value?.toString() ?: "--"
                        val latido = trabajadorSnapshot.child("ultimo_latido").getValue(Long::class.java) ?: 0L
                        val conectado = trabajadorSnapshot.child("estado_conexion").getValue(Boolean::class.java) ?: false

                        val caida = trabajadorSnapshot.child("alerta_caida").value == true
                        val panico = trabajadorSnapshot.child("boton_panico").value == true

                        val trabajador = Trabajador(
                            idChaleco = trabajadorSnapshot.key ?: "",
                            nombre = nombre,
                            temperatura = temp,
                            humedad = hum,
                            bateria = bat,
                            ultimo_latido = latido,
                            estado_conexion = conectado,
                            alerta_caida = caida,
                            boton_panico = panico
                        )
                        listaDatosCrudos.add(trabajador)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                revisarDesconexionesLocales()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun revisarDesconexionesLocales() {
        if (!isAdded) return

        listaTrabajadores.clear()
        var total = 0
        var enSitio = 0
        var fuera = 0
        val tiempoActual = System.currentTimeMillis()

        for (trabajador in listaDatosCrudos) {
            val tiempoSinSenal = tiempoActual - trabajador.ultimo_latido

            // Lógica de conexion
            val estaRealmenteConectado = trabajador.estado_conexion &&
                    trabajador.ultimo_latido != 0L &&
                    tiempoSinSenal < 20000

            if (!estaRealmenteConectado) {
                // si está offline, forzamos los guiones en la UI
                listaTrabajadores.add(trabajador.copy(
                    temperatura = "--",
                    humedad = "--",
                    bateria = "--",
                    estado_conexion = false
                ))
                fuera++
            } else {
                listaTrabajadores.add(trabajador)
                enSitio++
            }
            total++
        }

        trabajadorAdapter.actualizarLista(listaTrabajadores.toList())
        tvTotalWorkers.text = total.toString()
        tvOnSiteWorkers.text = enSitio.toString()
        tvOffSiteWorkers.text = fuera.toString()
    }

    override fun onResume() {
        super.onResume()
        handler.post(runnableRevisor)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnableRevisor)
    }

    private fun abrirDetallesTrabajador(trabajador: Trabajador) {
        if (!isAdded) return
        val fragment = WorkerDetailFragment().apply {
            arguments = Bundle().apply {
                putString("idChaleco", trabajador.idChaleco)
                putString("nombre", trabajador.nombre)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}