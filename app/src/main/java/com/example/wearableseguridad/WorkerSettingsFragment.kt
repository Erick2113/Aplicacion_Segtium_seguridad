package com.example.wearableseguridad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class WorkerSettingsFragment : Fragment() {

    private lateinit var tvSelectedTone: TextView

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
                val name = ringtone.getTitle(requireContext())
                tvSelectedTone.text = "Tono: $name"

                val prefs = requireActivity().getSharedPreferences("WorkerPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("alerta_tono_uri", uri.toString()).apply()
                Toast.makeText(context, "Tono de alerta actualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_worker_settings, container, false)

        val switchNotif = view.findViewById<SwitchCompat>(R.id.switchWorkerNotif)
        val switchVibrate = view.findViewById<SwitchCompat>(R.id.switchWorkerVibrate)
        val tvEmail = view.findViewById<TextView>(R.id.tvWorkerEmail)
        val btnLogout = view.findViewById<Button>(R.id.btnWorkerLogout)
        val btnSelectTone = view.findViewById<Button>(R.id.btnSelectTone)
        val btnChangePass = view.findViewById<Button>(R.id.btnWorkerChangePassword)
        tvSelectedTone = view.findViewById(R.id.tvSelectedTone)

        val prefs = requireActivity().getSharedPreferences("WorkerPrefs", Context.MODE_PRIVATE)
        val currentUser = FirebaseAuth.getInstance().currentUser
        tvEmail.text = currentUser?.email ?: "Sin correo"

        // Cargar preferencias
        switchNotif.isChecked = prefs.getBoolean("notificaciones", true)
        switchVibrate.isChecked = prefs.getBoolean("vibracion", true)

        val savedToneUri = prefs.getString("alerta_tono_uri", null)
        if (savedToneUri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(requireContext(), Uri.parse(savedToneUri))
                tvSelectedTone.text = "Tono: ${ringtone.getTitle(requireContext())}"
            } catch (e: Exception) {
                tvSelectedTone.text = "Tono: Predeterminado"
            }
        }

        switchNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notificaciones", isChecked).apply()
        }

        switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibracion", isChecked).apply()
        }

        btnSelectTone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Selecciona un Tono de Alerta")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, savedToneUri?.let { Uri.parse(it) })
            }
            ringtonePickerLauncher.launch(intent)
        }

        btnChangePass.setOnClickListener {
            mostrarDialogoCambiarPass()
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }

    private fun mostrarDialogoCambiarPass() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etPassActual = EditText(requireContext())
        etPassActual.hint = "Contraseña actual"
        etPassActual.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etPassActual)

        val etPassNueva = EditText(requireContext())
        etPassNueva.hint = "Nueva contraseña (min. 6)"
        etPassNueva.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etPassNueva)

        AlertDialog.Builder(requireContext())
            .setTitle("Seguridad de la Cuenta")
            .setView(layout)
            .setPositiveButton("Actualizar") { _, _ ->
                val passActual = etPassActual.text.toString().trim()
                val passNueva = etPassNueva.text.toString().trim()

                if (passActual.isEmpty() || passNueva.length < 6) {
                    Toast.makeText(context, "Datos incompletos o contraseña muy corta", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email

                if (user != null && email != null) {
                    val credential = EmailAuthProvider.getCredential(email, passActual)

                    user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                        if (reAuthTask.isSuccessful) {
                            user.updatePassword(passNueva).addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(context, "¡Contraseña actualizada!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al actualizar: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "La contraseña actual no es correcta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}