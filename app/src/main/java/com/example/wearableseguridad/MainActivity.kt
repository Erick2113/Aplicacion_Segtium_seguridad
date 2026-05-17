package com.example.wearableseguridad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Pedir permisos de notificaciones para el trabajador (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 2. INICIAR EL SERVICIO DE VIGILANCIA PARA EL TRABAJADOR
        // Esto permite que el trabajador reciba sus propias alertas y simulacros incluso con la app cerrada
        val intentService = Intent(this, NotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentService)
        } else {
            startService(intentService)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.worker_bottom_navigation)

        // Pantalla inicial del trabajador
        if (savedInstanceState == null) {
            cambiarFragmento(WorkerDashboardFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_worker_dashboard -> cambiarFragmento(WorkerDashboardFragment())
                R.id.nav_worker_alerts -> cambiarFragmento(WorkerAlertsFragment())
                R.id.nav_worker_settings -> cambiarFragmento(WorkerSettingsFragment())
            }
            true
        }
    }

    private fun cambiarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.worker_fragment_container, fragment)
            .commit()
    }
}