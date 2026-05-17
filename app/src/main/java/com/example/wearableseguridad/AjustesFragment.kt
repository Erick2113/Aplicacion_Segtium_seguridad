package com.example.wearableseguridad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AjustesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_ajustes, container, false)

        val switchVigilancia = view.findViewById<Switch>(R.id.switchVigilancia)
        val switchAlarmaGlobal = view.findViewById<Switch>(R.id.switchAlarmaGlobal)
        val switchNotificaciones = view.findViewById<Switch>(R.id.switchNotificaciones)
        val etLimiteTemp = view.findViewById<EditText>(R.id.etLimiteTemp)
        val btnGuardarTemp = view.findViewById<Button>(R.id.btnGuardarTemp)
        val switchSimulacro = view.findViewById<Switch>(R.id.switchSimulacro)
        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesion)

        val cardAdminMaster = view.findViewById<View>(R.id.cardAdminMaster)
        val btnAgregarAdmin = view.findViewById<Button>(R.id.btnAgregarAdmin)
        val btnVerAdmins = view.findViewById<Button>(R.id.btnVerAdmins)
        val btnVerBajas = view.findViewById<Button>(R.id.btnVerBajas)

        val prefs = requireActivity().getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: ""
        val isMaster = userEmail == "admin@segtium.com"

        // Lógica de Visibilidad de Administración
        cardAdminMaster.visibility = View.VISIBLE
        btnAgregarAdmin.visibility = if (isMaster) View.VISIBLE else View.GONE
        btnVerAdmins.visibility = if (isMaster) View.VISIBLE else View.GONE
        btnVerBajas.visibility = View.VISIBLE

        btnAgregarAdmin.setOnClickListener {
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            intent.putExtra("isRegisteringAdmin", true)
            startActivity(intent)
        }

        btnVerAdmins.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminsFragment())
                .addToBackStack(null)
                .commit()
        }

        btnVerBajas.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InactiveWorkersFragment())
                .addToBackStack(null)
                .commit()
        }

        // Cargar preferencias guardadas
        switchVigilancia.isChecked = prefs.getBoolean("vigilancia_activa", true)
        switchAlarmaGlobal.isChecked = prefs.getBoolean("alarma_global", true)
        switchNotificaciones.isChecked = prefs.getBoolean("notificaciones_activas", true)
        etLimiteTemp.setText(prefs.getFloat("limite_temp", 38.0f).toString())

        //  Vigilancia Activa (Foreground Service)
        switchVigilancia.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vigilancia_activa", isChecked).apply()
            val intent = Intent(requireContext(), NotificationService::class.java)
            if (isChecked) {
                ContextCompat.startForegroundService(requireContext(), intent)
                Toast.makeText(context, "Vigilancia en segundo plano activada", Toast.LENGTH_SHORT).show()
            } else {
                requireContext().stopService(intent)
                Toast.makeText(context, "Vigilancia desactivada", Toast.LENGTH_SHORT).show()
            }
        }

        switchAlarmaGlobal.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alarma_global", isChecked).apply()
        }

        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notificaciones_activas", isChecked).apply()
        }

        // Límite de Temperatura por la app
        btnGuardarTemp.setOnClickListener {
            val nuevoLimiteTexto = etLimiteTemp.text.toString()
            if (nuevoLimiteTexto.isNotEmpty()) {
                val nuevoLimite = nuevoLimiteTexto.toFloatOrNull()
                if (nuevoLimite != null) {
                    prefs.edit().putFloat("limite_temp", nuevoLimite).apply()
                    // Se envía a la ruta común que leen ambos Arduinos
                    FirebaseDatabase.getInstance().getReference("configuracion/limite_temp")
                        .setValue(nuevoLimite)
                    Toast.makeText(context, "Umbral de temperatura guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Modo Simulacro
        switchSimulacro.setOnCheckedChangeListener { _, isChecked ->
            val database = FirebaseDatabase.getInstance()
            // Activamos simulacro en ambos IDs conocidos para la simulación
            database.getReference("trabajadores/EMP-001/simulacro").setValue(isChecked)
            database.getReference("trabajadores/EMP-002/simulacro").setValue(isChecked)

            // Escribir en configuración global para otros usos
            database.getReference("configuracion/simulacro_activo").setValue(isChecked)

            val msg = if (isChecked) "¡Simulacro Activado!" else "Simulacro Finalizado"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        btnCerrarSesion.setOnClickListener {
            val intentService = Intent(requireContext(), NotificationService::class.java)
            requireContext().stopService(intentService)
            FirebaseAuth.getInstance().signOut()
            val intentLogin = Intent(requireContext(), LoginActivity::class.java)
            intentLogin.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentLogin)
        }

        return view
    }
}