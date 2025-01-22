val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

group = "dpsg"
version = "0.0.1"

application {
    mainClass.set("dpsg.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-webjars-jvm")
    implementation("org.webjars:jquery:3.2.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-hsts")
    implementation("io.ktor:ktor-server-forwarded-header")
    implementation("io.ktor:ktor-server-http-redirect")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
