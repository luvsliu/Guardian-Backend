package com.guardian.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

// Usamos IntIdTable para que Exposed maneje el ID autoincremental de forma nativa
object ContactosTable : IntIdTable("contactos") {
    val nombre = varchar("nombre", 255)
    val numero = varchar("numero", 20)
    val parentesco = varchar("parentesco", 100)
}

object UsuariosTable : IntIdTable("usuarios") {
    val email = varchar("email", 255).uniqueIndex()
    val contraseña = varchar("contraseña", 255)
}
