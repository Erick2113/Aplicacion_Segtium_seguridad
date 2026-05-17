package com.example.wearableseguridad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class InactiveWorkersFragment : Fragment() {

    private lateinit var rvInactive: RecyclerView
    private lateinit var adapter: TrabajadorAdapter
    private val listaBajas = mutableListOf<Trabajador>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inactive_workers, container, false)

        rvInactive = view.findViewById(R.id.rvInactiveWorkers)
        rvInactive.layoutManager = LinearLayoutManager(context)

        // Al tocar un trabajador, abrimos la pantalla de detalles de baja
        adapter = TrabajadorAdapter(listaBajas) { trabajador ->
            abrirDetalleBaja(trabajador)
        }
        rvInactive.adapter = adapter

        cargarBajasDesdeFirebase()

        return view
    }

    private fun abrirDetalleBaja(trabajador: Trabajador) {
        val fragment = InactiveWorkerDetailFragment()
        val args = Bundle()
        args.putSerializable("trabajador", trabajador)
        args.putString("idBaja", trabajador.idChaleco)
        fragment.arguments = args


        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun cargarBajasDesdeFirebase() {
        val ref = FirebaseDatabase.getInstance().getReference("trabajadores_baja")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                listaBajas.clear()
                for (bajaSnapshot in snapshot.children) {
                    val trabajador = bajaSnapshot.getValue(Trabajador::class.java)
                    if (trabajador != null) {
                        trabajador.idChaleco = bajaSnapshot.key ?: ""
                        listaBajas.add(trabajador)
                    }
                }
                adapter.actualizarLista(listaBajas)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
