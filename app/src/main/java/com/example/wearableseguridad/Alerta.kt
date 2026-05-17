package com.example.wearableseguridad

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Alerta(
    var id_trabajador: String? = "",
    var nombre: String? = "",
    var area: String? = "",
    var tipo_emergencia: String? = "",
    var timestamp: Any? = null
)