package dpsg.routes

import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.apiRoute() {
    route("/api/v1") {
        letter()
    }
}