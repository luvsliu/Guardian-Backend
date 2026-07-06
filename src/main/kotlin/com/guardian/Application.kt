package com.guardian

import com.guardian.database.DatabaseFactory
import com.guardian.database.ContactosTable
import com.guardian.model.EmergencyContact
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        DatabaseFactory.init()

        routing {
            get("/") {
                call.respondText("Guardian Backend is running on Railway!")
            }

            post("/index.php") {
                try {
                    val contacto = call.receive<EmergencyContact>()
                    
                    DatabaseFactory.dbQuery {
                        ContactosTable.insert {
                            it[nombre] = contacto.nombre
                            it[numero] = contacto.numero
                            it[parentesco] = contacto.parentesco
                        }
                    }
                    
                    call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
                    )
                }
            }

            // NUEVA RUTA: Eliminar por ID
            post("/eliminar.php") {
                try {
                    // Recibimos el ID desde el JSON enviado por la App
                    val params = call.receive<Map<String, Int>>()
                    val idAEliminar = params["id"] ?: throw Exception("ID no proporcionado")

                    DatabaseFactory.dbQuery {
                        ContactosTable.deleteWhere { ContactosTable.id eq idAEliminar }
                    }
                    
                    call.respond(HttpStatusCode.OK, mapOf("status" to "success", "message" to "Contacto eliminado"))
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
                    )
                }
            }
        }
    }.start(wait = true)
}
