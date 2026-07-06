package com.guardian.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        // Obtenemos variables y damos un valor por defecto para evitar que sean 'null'
        val host = java.lang.System.getenv("MYSQLHOST") ?: "localhost"
        val port = java.lang.System.getenv("MYSQLPORT") ?: "3306"
        val db = java.lang.System.getenv("MYSQLDATABASE") ?: "railway"
        val user = java.lang.System.getenv("MYSQLUSER") ?: "root"
        val pass = java.lang.System.getenv("MYSQLPASSWORD") ?: ""

        // Agregamos parámetros de SSL y Timezone para evitar errores comunes en MySQL 8+
        val jdbcUrl = "jdbc:mysql://$host:$port/$db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

        // Conectamos a la base de datos
        val database = Database.connect(
            url = jdbcUrl,
            driver = "com.mysql.cj.jdbc.Driver",
            user = user,
            password = pass
        )

        // Usamos createMissingTablesAndColumns: es más seguro porque
        // si la tabla ya existe, no intenta crearla de nuevo y dar error.
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ContactosTable)
        }
    }

    // Esta función permite ejecutar consultas SQL de forma segura y rápida
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
