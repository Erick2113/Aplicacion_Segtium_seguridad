package com.example.wearableseguridad

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Pedir permisos de notificaciones
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // INICIAR EL SERVICIO DE VIGILANCIA
        val intentService = Intent(this, NotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentService)
        } else {
            startService(intentService)
        }

        //  Configurar el menú de navegación
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Pantalla inicial
        cambiarFragmento(PanelFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_panel -> cambiarFragmento(PanelFragment())
                R.id.nav_alerts -> cambiarFragmento(AlertsFragment())
                R.id.nav_workers -> cambiarFragmento(WorkersFragment())
                R.id.nav_settings -> cambiarFragmento(AjustesFragment())
            }
            true
        }
    }

    // Función para intercambiar las pantallas
    private fun cambiarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}