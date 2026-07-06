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

            // Listar contactos con IDs reales
            get("/lista.php") {
                try {
                    val contactos = DatabaseFactory.dbQuery {
                        ContactosTable.selectAll().map {
                            mapOf(
                                "id" to it[ContactosTable.id].value,
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

            // Guardar contacto
            post("/index.php") {
                try {
                    // Recibir como texto para máxima flexibilidad
                    val body = call.receiveText()
                    logger.info("Guardar - Body: $body")
                    
                    // Extraer datos de forma manual para evitar fallos de parseo
                    val nombre = if (body.contains("\"nombre\"")) body.substringAfter("\"nombre\"").substringAfter("\"").substringBefore("\"") else ""
                    val numero = if (body.contains("\"numero\"")) body.substringAfter("\"numero\"").substringAfter("\"").substringBefore("\"") else ""
                    val parentesco = if (body.contains("\"parentesco\"")) body.substringAfter("\"parentesco\"").substringAfter("\"").substringBefore("\"") else ""

                    val generatedId = DatabaseFactory.dbQuery {
                        ContactosTable.insertAndGetId {
                            it[ContactosTable.nombre] = nombre
                            it[ContactosTable.numero] = numero
                            it[ContactosTable.parentesco] = parentesco
                        }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("status" to "success", "id" to generatedId.value))
                } catch (e: Exception) {
                    logger.error("Error guardar: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to (e.message ?: "Error al guardar")))
                }
            }

            // Eliminar contacto - VERSIÓN ULTRA ROBUSTA
            post("/eliminar.php") {
                try {
                    val bodyText = call.receiveText().trim()
                    logger.info("ELIMINAR - Body: '$bodyText'")
                    
                    // Extraer ID buscando el número en el texto
                    val idStr = bodyText.filter { it.isDigit() }
                    
                    if (idStr.isEmpty()) throw Exception("No se encontró un ID numérico en el envío")
                    
                    val idFinal = idStr.toInt()

                    val deleted = DatabaseFactory.dbQuery {
                        ContactosTable.deleteWhere { ContactosTable.id eq idFinal }
                    }
                    
                    if (deleted > 0) {
                        logger.info("Eliminado exitosamente ID: $idFinal")
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                    } else {
                        logger.warn("No se encontró el ID: $idFinal")
                        call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "ID $idFinal no existe"))
                    }
                } catch (e: Exception) {
                    logger.error("Error eliminar: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error en el servidor al eliminar"))
                }
            }

            // Autenticación - Registro
            post("/registro") {
                try {
                    val body = call.receiveText()
                    val email = if (body.contains("\"email\"")) body.substringAfter("\"email\"").substringAfter("\"").substringBefore("\"") else ""
                    val pass = if (body.contains("\"password\"")) body.substringAfter("\"password\"").substringAfter("\"").substringBefore("\"") 
                               else if (body.contains("\"contraseña\"")) body.substringAfter("\"contraseña\"").substringAfter("\"").substringBefore("\"") else ""

                    DatabaseFactory.dbQuery {
                        UsuariosTable.insert {
                            it[UsuariosTable.email] = email
                            it[UsuariosTable.contraseña] = pass
                        }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error: Email duplicado o datos inválidos"))
                }
            }

            // Autenticación - Login
            post("/login") {
                try {
                    val body = call.receiveText()
                    val email = if (body.contains("\"email\"")) body.substringAfter("\"email\"").substringAfter("\"").substringBefore("\"") else ""
                    val pass = if (body.contains("\"password\"")) body.substringAfter("\"password\"").substringAfter("\"").substringBefore("\"") 
                               else if (body.contains("\"contraseña\"")) body.substringAfter("\"contraseña\"").substringAfter("\"").substringBefore("\"") else ""

                    val user = DatabaseFactory.dbQuery {
                        UsuariosTable.select { (UsuariosTable.email eq email) and (UsuariosTable.contraseña eq pass) }.singleOrNull()
                    }

                    if (user != null) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Credenciales inválidas"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error en el login"))
                }
            }
        }
    }.start(wait = true)
}
