package dpsg.routes

import dpsg.services.UserService
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.health() {
    get("/health") {
        call.respondText("OK")
    }
}

fun Routing.apiRoute(userService: UserService) {
    route("/api/v1") {
        letter()
    }
    route("/api/v1") {
        health()
    }
    route("/api/v1") {
        templates(userService)
    }
}