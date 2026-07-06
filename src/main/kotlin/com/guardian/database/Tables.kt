package com.guardian.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

// Usamos IntIdTable para que Exposed maneje el ID autoincremental de forma nativa
object ContactosTable : IntIdTable("contactos") {
    val nombre = varchar("nombre", 255)
    val numero = varchar("numero", 255) // Aumentado de 20 a 255 para evitar truncamiento
    val parentesco = varchar("parentesco", 255)
}

object UsuariosTable : IntIdTable("usuarios") {
    val email = varchar("email", 255).uniqueIndex()
    val contraseña = varchar("contraseña", 255)
}
