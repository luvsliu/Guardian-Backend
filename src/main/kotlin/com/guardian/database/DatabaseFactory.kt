package com.guardian.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var database: Database? = null

    fun init() {
        val host = System.getenv("MYSQLHOST") ?: "localhost"
        val port = System.getenv("MYSQLPORT") ?: "3306"
        val db = System.getenv("MYSQLDATABASE") ?: "railway"
        val user = System.getenv("MYSQLUSER") ?: "root"
        val pass = System.getenv("MYSQLPASSWORD") ?: ""

        val jdbcUrl = "jdbc:mysql://$host:$port/$db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        
        logger.info("Connecting to database at $host:$port/$db")
        
        database = Database.connect(
            url = jdbcUrl,
            driver = "com.mysql.cj.jdbc.Driver",
            user = user,
            password = pass
        )

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ContactosTable)
        }
    }
    
    fun checkConnection() {
        transaction(database) {
            exec("SELECT 1")
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
