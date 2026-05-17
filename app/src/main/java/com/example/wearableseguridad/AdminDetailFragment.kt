package com.example.wearableseguridad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AdminDetailFragment : Fragment() {

    private var adminUid: String? = null
    private var adminNombre: String? = null
    private var adminCorreo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_detail, container, false)

        adminUid = arguments?.getString("adminUid")
        adminNombre = arguments?.getString("adminNombre")
        adminCorreo = arguments?.getString("adminCorreo")

        val tvNombre = view.findViewById<TextView>(R.id.tvAdminDetailNombre)
        val tvCorreo = view.findViewById<TextView>(R.id.tvAdminDetailCorreo)
        val btnReset = view.findViewById<Button>(R.id.btnResetAdminPass)
        val btnEliminar = view.findViewById<Button>(R.id.btnEliminarAdmin)

        tvNombre.text = adminNombre
        tvCorreo.text = adminCorreo

        btnReset.setOnClickListener {
            mostrarDialogoReset()
        }

        btnEliminar.setOnClickListener {
            mostrarDialogoEliminar()
        }

        return view
    }

    private fun mostrarDialogoReset() {
        val email = adminCorreo ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Restablecer Contraseña")
            .setMessage("¿Enviar un correo de recuperación a $email?")
            .setPositiveButton("Enviar") { _, _ ->
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Correo de restablecimiento enviado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminar() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Administrador")
            .setMessage("¿Estás seguro de que deseas eliminar a $adminNombre?.")
            .setPositiveButton("Eliminar") { _, _ ->
                adminUid?.let { uid ->
                    FirebaseDatabase.getInstance().getReference("usuarios_admin").child(uid)
                        .removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Administrador eliminado de la lista", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}