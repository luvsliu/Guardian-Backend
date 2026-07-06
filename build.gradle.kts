plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    application
}

group = "com.guardian"
version = "0.0.1"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.guardian.ApplicationKt")
}

tasks.jar {
    // Nombre fijo para que Railway lo encuentre siempre
    archiveFileName.set("app.jar")

    manifest {
        attributes["Main-Class"] = "com.guardian.ApplicationKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Incluimos las dependencias para crear un Fat JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // CRÍTICO: Excluir archivos de firma que causan SecurityException
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation) 
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikaricp)
    implementation(libs.mysql.connector)
}
