package com.guardian.database

import org.jetbrains.exposed.sql.Table

object ContactosTable : Table("contactos") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 255)
    val numero = varchar("numero", 20)
    val parentesco = varchar("parentesco", 100) // <-- Asegurado con S

    override val primaryKey = PrimaryKey(id)
}
