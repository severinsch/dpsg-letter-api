package dpsg.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.health() {
    get("/health") {
        call.respondText("OK")
    }
}

fun Routing.apiRoute() {
    route("/api/v1") {
        letter()
    }
    route("/api/v1") {
        health()
    }
}