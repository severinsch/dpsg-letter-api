package dpsg

import dpsg.plugins.*
import dpsg.services.UserService

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.resources.*


fun main() {

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Resources)

    val userService = UserService()
    userService.loadUsers()

    configureSecurity(userService)
    configureSerialization()
    configureRouting(userService)
}
