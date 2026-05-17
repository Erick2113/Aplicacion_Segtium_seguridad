package com.example.wearableseguridad

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

class NotificationService : Service() {

    private val CHANNEL_ID = "EMERGENCIAS_WEARABLE"
    private val alertasActivas = mutableMapOf<String, Boolean>()
    private lateinit var database: DatabaseReference
    private var valueEventListener: ValueEventListener? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        iniciarPrimerPlano()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarMonitoreo()
        return START_STICKY
    }

    private fun iniciarPrimerPlano() {
        // Notificación persistente para que Android no mate el servicio
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Segtium: Vigilancia Activa")
            .setContentText("Protegiendo al personal en tiempo real...")
            .setSmallIcon(R.drawable.outline_add_alert_24)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun iniciarMonitoreo() {
        database = FirebaseDatabase.getInstance().getReference("trabajadores")
        val prefs = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tiempoActual = System.currentTimeMillis()
                val limiteTemp = prefs.getFloat("limite_temp", 38.0f)
                val notisPermitidas = prefs.getBoolean("notificaciones_activas", true)

                for (trabajadorSnapshot in snapshot.children) {
                    val id = trabajadorSnapshot.key ?: ""

                    // Solo monitorear si el trabajador tiene un perfil registrado
                    if (!trabajadorSnapshot.hasChild("nombre")) continue

                    val ultimoLatido = trabajadorSnapshot.child("ultimo_latido").getValue(Long::class.java) ?: 0L
                    val conectadoEnDB = trabajadorSnapshot.child("estado_conexion").getValue(Boolean::class.java) ?: false

                    // UMBRAL DE SEGURIDAD
                    val estaRealmenteConectado = conectadoEnDB && (tiempoActual - ultimoLatido < 20000)

                    if (estaRealmenteConectado) {
                        val nombre = trabajadorSnapshot.child("nombre").value?.toString() ?: "Trabajador"

                        val caida = trabajadorSnapshot.child("alerta_caida").value == true
                        val panico = trabajadorSnapshot.child("boton_panico").value == true
                        val tempStr = trabajadorSnapshot.child("temperatura").value?.toString() ?: "0.0"
                        val tempValue = tempStr.toFloatOrNull() ?: 0f

                        // Gestión de Alertas Críticas
                        gestionarAlerta(id, "caida", caida, nombre, "¡Posible caída detectada!", notisPermitidas)
                        gestionarAlerta(id, "panico", panico, nombre, "¡BOTÓN DE PÁNICO ACTIVADO!", notisPermitidas)

                        // Gestión de Alerta de Temperatura
                        if (tempValue > limiteTemp) {
                            gestionarAlerta(id, "temp", true, nombre, "Temperatura crítica: $tempStr°C", notisPermitidas)
                        } else {
                            gestionarAlerta(id, "temp", false, nombre, "", notisPermitidas)
                        }
                    } else {
                        // Limpiar memoria si se apaga
                        alertasActivas.remove("${id}_caida")
                        alertasActivas.remove("${id}_panico")
                        alertasActivas.remove("${id}_temp")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(valueEventListener!!)
    }

    private fun gestionarAlerta(id: String, tipo: String, activo: Boolean, nombre: String, mensaje: String, permitted: Boolean) {
        val keyAlerta = "${id}_$tipo"
        if (activo) {
            if (alertasActivas[keyAlerta] != true) {
                if (permitted) {
                    vibrarTelefono()
                    lanzarNotificacionPush(nombre, mensaje, keyAlerta.hashCode())
                }
                alertasActivas[keyAlerta] = true
            }
        } else {
            alertasActivas[keyAlerta] = false
        }
    }

    private fun lanzarNotificacionPush(nombre: String, mensaje: String, notificationId: Int) {
        val intent = Intent(this, AdminActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_add_alert_24)
            .setContentTitle(nombre)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Para que aparezca arriba de otras notificaciones
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun vibrarTelefono() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Patrón: 0ms espera, 500ms vibra, 200ms espera, 500ms vibra
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Alertas Críticas Segtium", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones de emergencia de chalecos inteligentes"
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        valueEventListener?.let { database.removeEventListener(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}