package com.guardian.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

@Serializable
data class EmergencyContact(
    val id: Int? = null,
    val nombre: String,
    val numero: String,
    val parentesco: String
)

object ContactosTable : IntIdTable("contactos") {
    val nombre = varchar("nombre", 255)
    val numero = varchar("numero", 20)
    val parentesco = varchar("parentesco", 100)
}
