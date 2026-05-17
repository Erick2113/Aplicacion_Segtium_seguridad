package com.example.wearableseguridad

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isRegisteringAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        isRegisteringAdmin = intent.getBooleanExtra("isRegisteringAdmin", false)

        val tvTitle = findViewById<TextView>(R.id.tvRegisterTitle)
        val etNombre = findViewById<EditText>(R.id.etRegisterNombre)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etArea = findViewById<EditText>(R.id.etRegisterArea)
        val spinnerChaleco = findViewById<Spinner>(R.id.spinnerChaleco)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        if (isRegisteringAdmin) {
            tvTitle.text = "Registrar Administrador"
            etArea.visibility = View.GONE
            findViewById<View>(R.id.tvLabelChaleco)?.visibility = View.GONE
            spinnerChaleco.visibility = View.GONE
            btnRegister.text = "Crear Administrador"
        }

        val listaChalecos = arrayOf("EMP-001", "EMP-002", "EMP-003", "EMP-004", "EMP-005")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaChalecos)
        spinnerChaleco.adapter = adapter

        tvBackToLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val secondaryAuth = getSecondaryAuth()

            if (isRegisteringAdmin) {
                crearCuentaAdmin(secondaryAuth, correo, password, nombre)
            } else {
                val area = etArea.text.toString().trim()
                val chaleco = spinnerChaleco.selectedItem.toString()
                if (area.isEmpty()) {
                    Toast.makeText(this, "Completa el área", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                verificarYCrearTrabajador(secondaryAuth, correo, password, nombre, area, chaleco)
            }
        }
    }

    private fun getSecondaryAuth(): FirebaseAuth {
        return try {
            val secondaryApp = FirebaseApp.getInstance("SecondaryApp")
            FirebaseAuth.getInstance(secondaryApp)
        } catch (e: Exception) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyBpQI83AIYPaaHplD3lS3BCkqBUo1x63Ik")
                .setApplicationId("1:63035192415:android:15cafb94ae4b4c149457ea")
                .setProjectId("wearable-cloud-9bc89")
                .setDatabaseUrl("https://wearable-cloud-9bc89-default-rtdb.firebaseio.com")
                .build()
            val secondaryApp = FirebaseApp.initializeApp(this, options, "SecondaryApp")
            FirebaseAuth.getInstance(secondaryApp)
        }
    }

    private fun crearCuentaAdmin(secondaryAuth: FirebaseAuth, correo: String, password: String, nombre: String) {
        secondaryAuth.createUserWithEmailAndPassword(correo, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = task.result?.user?.uid ?: ""
                val adminData = hashMapOf("nombre" to nombre, "correo" to correo, "rol" to "admin")
                FirebaseDatabase.getInstance().getReference("usuarios_admin").child(uid).setValue(adminData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Administrador creado exitosamente", Toast.LENGTH_LONG).show()
                        finish()
                    }
                secondaryAuth.signOut()
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verificarYCrearTrabajador(secondaryAuth: FirebaseAuth, correo: String, password: String, nombre: String, area: String, chaleco: String) {
        val database = FirebaseDatabase.getInstance().getReference("trabajadores")

        database.child(chaleco).get().addOnSuccessListener { snapshot ->

            // Un chaleco solo está ocupado si ya tiene un trabajador asignado
            if (snapshot.exists() && snapshot.hasChild("nombre")) {
                Toast.makeText(this, "El chaleco $chaleco ya está ocupado por otra persona", Toast.LENGTH_SHORT).show()
            } else {
                // Si el nodo no existe, o existe pero no tiene nombre creado por hardware
                secondaryAuth.createUserWithEmailAndPassword(correo, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: ""
                        val trabajadorData = hashMapOf(
                            "nombre" to nombre,
                            "correo" to correo,
                            "area" to area,
                            "activo" to true,
                            "alerta_caida" to false,
                            "boton_panico" to false,
                            "temperatura" to "--",
                            "humedad" to "--",
                            "estado_conexion" to false,
                            "ultimo_latido" to 0L,
                            "idChaleco" to chaleco
                        )

                        // Usamos updateChildren para no borrar lo que el hardware esté enviando en ese momento
                        database.child(chaleco).updateChildren(trabajadorData as Map<String, Any>).addOnSuccessListener {
                            Toast.makeText(this, "Trabajador registrado en $chaleco", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        secondaryAuth.signOut()
                    } else {
                        Toast.makeText(this, "Error Auth: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error de conexión con la base de datos", Toast.LENGTH_SHORT).show()
        }
    }
}