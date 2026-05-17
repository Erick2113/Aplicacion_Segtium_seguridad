package com.example.wearableseguridad

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            verificarAccesoTotal(email)
                        } else {
                            Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Llena los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarAccesoTotal(email: String) {
        val uid = auth.currentUser?.uid ?: ""
        
        //  usuario maestro siempre entra
        if (email == "admin@segtium.com") {
            irAAdmin()
            return
        }

        //  Verificar si existe como Admin Secundario
        val refAdmin = FirebaseDatabase.getInstance().getReference("usuarios_admin").child(uid)
        refAdmin.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                irAAdmin()
            } else {
                // 3. Si no es admin, verificar si es un trabajador activo
                verificarSiEsTrabajador(email)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error de red, intenta de nuevo", Toast.LENGTH_SHORT).show()
            auth.signOut()
        }
    }

    private fun verificarSiEsTrabajador(email: String) {
        val refTrabajadores = FirebaseDatabase.getInstance().getReference("trabajadores")
        refTrabajadores.get().addOnSuccessListener { snapshot ->
            var esValido = false
            for (chaleco in snapshot.children) {
                val correoDB = chaleco.child("correo").getValue(String::class.java)
                if (correoDB?.lowercase() == email.lowercase()) {
                    esValido = true
                    break
                }
            }

            if (esValido) {
                irATrabajador()
            } else {
                // sui no esta en nignun lugar se blouea el acceso
                Toast.makeText(this, "Esta cuenta ha sido dada de baja o eliminada.", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
        }
    }

    private fun irAAdmin() {
        Toast.makeText(this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irATrabajador() {
        Toast.makeText(this, "Bienvenido Trabajador", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}