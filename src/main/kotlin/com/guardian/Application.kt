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

            // LISTA: Devuelve exactamente lo que hay en Railway AHORA MISMO
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

            // GUARDAR: Inserta y confirma con status 200 (OK) para evitar errores en algunas Apps
            post("/index.php") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val nom = params["nombre"] ?: ""
                    val num = params["numero"] ?: ""
                    val par = params["parentesco"] ?: ""

                    logger.info("Guardando contacto: $nom, Numero: $num")

                    val generatedId = DatabaseFactory.dbQuery {
                        ContactosTable.insertAndGetId {
                            it[nombre] = nom
                            it[numero] = num
                            it[parentesco] = par
                        }
                    }
                    
                    // Respondemos con 200 OK y status: success (formato estándar)
                    call.respond(HttpStatusCode.OK, mapOf(
                        "status" to "success", 
                        "message" to "Contacto guardado",
                        "id" to generatedId.value
                    ))
                } catch (e: Exception) {
                    logger.error("Error guardar: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, mapOf(
                        "status" to "error", 
                        "message" to (e.message ?: "Error al guardar")
                    ))
                }
            }

            // ELIMINAR
            post("/eliminar.php") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val idStr = params["id"] ?: params["ID"] ?: throw Exception("ID no proporcionado")
                    val idFinal = idStr.toIntOrNull() ?: throw Exception("ID inválido")

                    val deleted = DatabaseFactory.dbQuery {
                        ContactosTable.deleteWhere { ContactosTable.id eq idFinal }
                    }
                    
                    if (deleted > 0) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("status" to "error", "message" to "ID no encontrado"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error al eliminar"))
                }
            }

            // AUTH
            post("/registro") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val em = params["email"] ?: throw Exception("Email requerido")
                    val con = params["password"] ?: params["contraseña"] ?: throw Exception("Contraseña requerida")

                    DatabaseFactory.dbQuery {
                        UsuariosTable.insert {
                            it[email] = em
                            it[contraseña] = con
                        }
                    }
                    call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error en registro"))
                }
            }

            post("/login") {
                try {
                    val params = call.receive<Map<String, String>>()
                    val em = params["email"] ?: ""
                    val con = params["password"] ?: params["contraseña"] ?: ""

                    val user = DatabaseFactory.dbQuery {
                        UsuariosTable.select { (UsuariosTable.email eq em) and (UsuariosTable.contraseña eq con) }.singleOrNull()
                    }

                    if (user != null) {
                        call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Credenciales inválidas"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Error login"))
                }
            }
        }
    }.start(wait = true)
}
