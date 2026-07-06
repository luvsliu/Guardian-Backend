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
import org.jetbrains.exposed.sql.insert

fun main() {
    // IMPORTANTE: host = "0.0.0.0" es obligatorio para que Railway pueda ver el servidor
    val port = java.lang.System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        DatabaseFactory.init() // Conectamos a MySQL y creamos la tabla si falta

        routing {
            // Página de bienvenida al entrar desde el navegador
            get("/") {
                call.respondText("Guardian Backend is running correctly on Railway!")
            }

            // Esta es la ruta que llama tu App de Android
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
                    // Si algo falla, el servidor te dirá exactamente qué fue
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("status" to "error", "message" to (e.message ?: "Unknown error"))
                    )
                }
            }
        }
    }.start(wait = true)
}