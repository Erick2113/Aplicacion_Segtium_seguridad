package com.example.wearableseguridad

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class ProfileSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_selection)

        // Conectamos las tarjetas
        val cardEmpleado = findViewById<MaterialCardView>(R.id.cardEmpleado)
        val cardAdmin = findViewById<MaterialCardView>(R.id.cardAdmin)

        // Si el jefe elige registrar a un rmpleado
        cardEmpleado.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("TIPO_PERFIL", "empleado") // Metemos el rol
            startActivity(intent)
            finish()
        }

        // Si el jefe elige registrar a un administrador
        cardAdmin.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("TIPO_PERFIL", "administrador") // Metemos el rol
            startActivity(intent)
            finish()
        }
    }
}