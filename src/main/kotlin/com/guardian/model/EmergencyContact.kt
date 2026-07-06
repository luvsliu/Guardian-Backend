package com.guardian.model

import kotlinx.serialization.Serializable

/**
 * MODELO DE DATOS PARA EL BACKEND
 * Este archivo permite que el servidor Ktor entienda el JSON que envía la App.
 */
@Serializable
data class EmergencyContact(
    val nombre: String,
    val numero: String,
    val parentesco: String // <-- Asegúrate de usar la S aquí
)
