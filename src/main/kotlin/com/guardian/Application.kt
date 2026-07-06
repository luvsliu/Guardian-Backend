package com.guardian

import com.guardian.database.DatabaseFactory
import com.guardian.database.ContactosTable
import com.guardian.database.UsuariosTable
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
                call.respondText("Guardian Backend is active and stable!")
            }

            // --- SECCIÓN DE CONTACTOS (index.php y eliminar.php) ---
            
            post("/index.php") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val nombreParam = params["nombre"] ?: ""
                    val numeroParam = params["numero"] ?: ""
                    val parentescoParam = params["parentesco"] ?: ""

                    DatabaseFactory.dbQuery {
                        ContactosTable.insert {
                            it[nombre] = nombreParam
                            it[numero] = numeroParam
                            it[parentesco] = parentescoParam
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
                    val idStr = params["id"] ?: throw Exception("ID no proporcionado")
                    val idAEliminar = idStr.toIntOrNull() ?: throw Exception("ID inválido")

                    DatabaseFactory.dbQuery {
                        ContactosTable.deleteWhere { ContactosTable.id eq idAEliminar }
                    }
                    call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to (e.message ?: "Error al eliminar")))
                }
            }

            // --- SECCIÓN DE AUTENTICACIÓN (registro y login) ---

            post("/registro") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val emailParam = params["email"] ?: throw Exception("Email requerido")
                    val passwordParam = params["password"] ?: throw Exception("Password requerido")

                    DatabaseFactory.dbQuery {
                        UsuariosTable.insert {
                            it[email] = emailParam
                            it[password] = passwordParam
                        }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error: Posible email duplicado"))
                }
            }

            post("/login") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val emailParam = params["email"] ?: ""
                    val passwordParam = params["password"] ?: ""

                    val userExists = DatabaseFactory.dbQuery {
                        UsuariosTable.select { 
                            (UsuariosTable.email eq emailParam) and (UsuariosTable.password eq passwordParam) 
                        }.count() > 0
                    }

                    if (userExists) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success", "message" to "Login correcto"))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Credenciales inválidas"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error en el servidor"))
                }
            }
        }
    }.start(wait = true)
}
