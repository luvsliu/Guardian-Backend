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

            // NUEVA RUTA: Listar contactos para ver IDs reales
            get("/lista.php") {
                try {
                    val contactos = DatabaseFactory.dbQuery {
                        ContactosTable.selectAll().map {
                            mapOf(
                                "id" to it[ContactosTable.id],
                                "nombre" to it[ContactosTable.nombre],
                                "numero" to it[ContactosTable.numero],
                                "parentesco" to it[ContactosTable.parentesco]
                            )
                        }
                    }
                    call.respond(contactos)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Error al listar")))
                }
            }

            post("/eliminar.php") {
                val logger = LoggerFactory.getLogger("Eliminar")
                try {
                    // Intentamos recibir como Map primero
                    val bodyText = call.receiveText()
                    logger.info("Cuerpo recibido en eliminar: $bodyText")
                    
                    // Extraer ID manualmente de un JSON simple si falla el Map
                    val idStr = if (bodyText.contains("\"id\":")) {
                        bodyText.substringAfter("\"id\":").substringBefore(",").substringBefore("}").trim().replace("\"", "").replace(":", "")
                    } else {
                        // Si no es JSON manual, intentamos parsear de nuevo
                        ""
                    }
                    
                    val idFinal = idStr.toIntOrNull() ?: throw Exception("No se pudo obtener un ID numérico de: $bodyText")

                    val rowsDeleted = DatabaseFactory.dbQuery {
                        ContactosTable.deleteWhere { ContactosTable.id eq idFinal }
                    }
                    
                    logger.info("ID a eliminar: $idFinal. Filas borradas: $rowsDeleted")
                    call.respond(HttpStatusCode.OK, mapOf("status" to "success", "deletedRows" to rowsDeleted))
                } catch (e: Exception) {
                    logger.error("Fallo total al eliminar: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to (e.message ?: "Error desconocido"))
                    )
                }
            }

            // --- SECCIÓN DE AUTENTICACIÓN (registro y login) ---

            post("/registro") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val emailParam = params["email"] ?: throw Exception("Email requerido")
                    val passwordParam = params["password"] ?: params["contraseña"] ?: throw Exception("Password requerido")

                    DatabaseFactory.dbQuery {
                        UsuariosTable.insert {
                            it[email] = emailParam
                            it[contraseña] = passwordParam
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
                    val passwordParam = params["password"] ?: params["contraseña"] ?: ""

                    val userExists = DatabaseFactory.dbQuery {
                        UsuariosTable.select { 
                            (UsuariosTable.email eq emailParam) and (UsuariosTable.contraseña eq passwordParam)
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
