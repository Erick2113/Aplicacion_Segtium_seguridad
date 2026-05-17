package com.example.wearableseguridad

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable
import com.example.wearableseguridad.R
class InactiveWorkerDetailFragment : Fragment() {

    private var trabajador: Trabajador? = null
    private var idBaja: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inactive_worker_detail, container, false)

        trabajador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("trabajador", Trabajador::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("trabajador") as? Trabajador
        }
        idBaja = arguments?.getString("idBaja")

        val tvNombre = view.findViewById<TextView>(R.id.tvInactiveDetailNombre)
        val tvArea = view.findViewById<TextView>(R.id.tvInactiveDetailArea)
        val tvEmail = view.findViewById<TextView>(R.id.tvInactiveDetailEmail)
        val btnReactivar = view.findViewById<Button>(R.id.btnReactivar)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminarPermanente)

        trabajador?.let {
            tvNombre.text = it.nombre
            tvArea.text = "Área: ${it.area}"
            tvEmail.text = it.correo
        }

        btnReactivar.setOnClickListener {
            mostrarDialogoSeleccionChaleco()
        }

        btnEliminar.setOnClickListener {
            mostrarDialogoConfirmarEliminar()
        }

        return view
    }

    private fun mostrarDialogoSeleccionChaleco() {
        val chalecosPosibles = listOf("EMP-001", "EMP-002", "EMP-003", "EMP-004", "EMP-005")
        val refActivos = FirebaseDatabase.getInstance().getReference("trabajadores")

        refActivos.get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener

           //disponibilidad de chaleco
            val chalecosDisponibles = chalecosPosibles.filter { id ->
                val nodoChaleco = snapshot.child(id)
                !nodoChaleco.exists() || !nodoChaleco.hasChild("nombre")
            }

            if (chalecosDisponibles.isEmpty()) {
                Toast.makeText(context, "No hay chalecos disponibles en este momento", Toast.LENGTH_SHORT).show()
            } else {
                val arrayAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, chalecosDisponibles)
                AlertDialog.Builder(requireContext())
                    .setTitle("Seleccionar Chaleco para Reactivación")
                    .setAdapter(arrayAdapter) { _, which ->
                        ejecutarReactivacion(chalecosDisponibles[which])
                    }
                    .show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al verificar disponibilidad de chalecos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ejecutarReactivacion(nuevoChaleco: String) {
        val t = trabajador ?: return
        val id = idBaja ?: return

        val refActivos = FirebaseDatabase.getInstance().getReference("trabajadores")
        val refBajas = FirebaseDatabase.getInstance().getReference("trabajadores_baja")

        // Creamos copia actualizada
        val trabajadorActivo = t.copy(activo = true, idChaleco = nuevoChaleco)

        // Usamos updateChildren para integrarnos con los datos del hardware si ya existen
        val actualizaciones = mapOf(
            "nombre" to trabajadorActivo.nombre,
            "correo" to trabajadorActivo.correo,
            "area" to trabajadorActivo.area,
            "activo" to true,
            "idChaleco" to nuevoChaleco,
            "estado_conexion" to false // Reinicio preventivo
        )

        refActivos.child(nuevoChaleco).updateChildren(actualizaciones).addOnSuccessListener {
            refBajas.child(id).removeValue().addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "${t.nombre} ha sido reactivado en $nuevoChaleco", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al reactivar en la base de datos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoConfirmarEliminar() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Permanentemente")
            .setMessage("¿Estás seguro de eliminar a ${trabajador?.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                ejecutarEliminacion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarEliminacion() {
        val id = idBaja ?: return
        FirebaseDatabase.getInstance().getReference("trabajadores_baja").child(id)
            .removeValue()
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Usuario eliminado permanentemente", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
    }
}