package com.guardian

import com.guardian.database.DatabaseFactory
import com.guardian.database.ContactosTable
import com.guardian.database.UsuariosTable
import com.guardian.model.EmergencyContact
import com.guardian.model.User
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Application")
    val port = System.getenv("PORT")?.toInt() ?: 8080
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        DatabaseFactory.init()

        routing {
            get("/") {
                call.respondText("Guardian Backend is running with Authentication!")
            }

            // --- RUTAS DE CONTACTOS ---
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
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to (e.message ?: "Unknown error")))
                }
            }

            post("/eliminar.php") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val idString = params["id"] ?: throw Exception("ID no proporcionado")
                    val idAEliminar = idString.toIntOrNull() ?: throw Exception("ID debe ser un número")
                    DatabaseFactory.dbQuery {
                        ContactosTable.deleteWhere { ContactosTable.id eq idAEliminar }
                    }
                    call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to (e.message ?: "Unknown error")))
                }
            }

            // --- RUTAS DE AUTENTICACIÓN ---
            post("/registro") {
                try {
                    val user = call.receive<User>()
                    DatabaseFactory.dbQuery {
                        UsuariosTable.insert {
                            it[email] = user.email
                            it[password] = user.password
                        }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("status" to "success", "message" to "Usuario registrado"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to (e.message ?: "Ya existe un usuario con este email")))
                }
            }

            post("/login") {
                try {
                    val credentials = call.receive<User>()
                    val user = DatabaseFactory.dbQuery {
                        UsuariosTable.select { 
                            (UsuariosTable.email eq credentials.email) and (UsuariosTable.password eq credentials.password) 
                        }.singleOrNull()
                    }

                    if (user != null) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success", "message" to "Login correcto"))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Credenciales incorrectas"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to (e.message ?: "Error en el login")))
                }
            }
        }
    }.start(wait = true)
}
