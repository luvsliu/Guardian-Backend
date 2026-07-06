# Guardian-Backend (Kotlin/Ktor)

Este proyecto ha sido transformado de PHP a un Backend moderno en Kotlin usando **Ktor**.

## Estructura del Proyecto
- `src/main/kotlin/com/guardian/Application.kt`: Punto de entrada del servidor y definición de rutas.
- `src/main/kotlin/com/guardian/database/DatabaseFactory.kt`: Configuración de la base de datos MySQL (Exposed + HikariCP).
- `src/main/kotlin/com/guardian/models/EmergencyContact.kt`: Definición del modelo de datos y la tabla de la base de datos.

## Endpoints
- `GET /`: Comprobar si el servidor está en línea.
- `POST /guardar`: Guarda un nuevo contacto de emergencia.
  - Body: `{"nombre": "Juan", "numero": "123456789", "parentesco": "Amigo"}`
- `GET /contactos`: Lista todos los contactos guardados.

## Configuración para Railway
El proyecto está configurado para leer las variables de entorno de Railway (que son las mismas que usabas para MySQL):
- `MYSQLHOST`
- `MYSQLPORT`
- `MYSQLDATABASE`
- `MYSQLUSER`
- `MYSQLPASSWORD`
- `PORT` (puerto del servidor)

## Cómo ejecutar localmente
1. Asegúrate de tener una base de datos MySQL corriendo.
2. Configura las variables de entorno mencionadas arriba.
3. Ejecuta `./gradlew run` o inicia `ApplicationKt` desde IntelliJ.
