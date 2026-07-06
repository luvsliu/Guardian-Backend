package com.guardian.model

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyContact(
    val id: Int? = null,
    val nombre: String,
    val numero: String,
    val parentesco: String
)
