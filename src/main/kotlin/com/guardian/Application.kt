package com.guardian

import com.guardian.database.DatabaseFactory
import com.guardian.database.ContactosTable
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Application")
    val port = System.getenv("PORT")?.toInt() ?: 8080
    
    logger.info("Starting server on port $port")

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        try {
            logger.info("Initializing Database...")
            DatabaseFactory.init()
            logger.info("Database initialized successfully.")
        } catch (e: Exception) {
            logger.error("Failed to initialize database: ${e.message}", e)
            // No se detiene el servidor para que al menos Railway no lo marque como CRASHED
            // de inmediato y podamos ver los logs.
        }

        routing {
            get("/") {
                call.respondText("Guardian Backend is running! Database status: ${try { DatabaseFactory.checkConnection(); "Connected" } catch(e: Exception) { "Error: ${e.message}" }}")
            }

            post("/index.php") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val nombre = params["nombre"] ?: ""
                    val numero = params["numero"] ?: ""
                    val parentesco = params["parentesco"] ?: ""

                    logger.info("Received request to save contact: $nombre")

                    DatabaseFactory.dbQuery {
                        ContactosTable.insert {
                            it[ContactosTable.nombre] = nombre
                            it[ContactosTable.numero] = numero
                            it[ContactosTable.parentesco] = parentesco
                        }
                    }

                    call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
                } catch (e: Exception) {
                    logger.error("Error saving contact: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
                    )
                }
            }
        }
    }.start(wait = true)
}
