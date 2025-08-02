package dpsg.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.health() {
    get("/health") {
        call.respondText("OK")
    }
}

fun Route.apiRoute() {
        letter()
        health()
}