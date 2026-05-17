package com.example.wearableseguridad

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Trabajador(
    var idChaleco: String = "",
    val nombre: String = "",
    val correo: String = "",
    val area: String = "",
    val alerta_caida: Boolean = false,
    val boton_panico: Boolean = false,
    val temperatura: String = "--",
    val humedad: String = "--",
    val bateria: String = "--",
    val estado_conexion: Boolean = false,
    val ultimo_latido: Long = 0L,
    val activo: Boolean = true
) : Serializable