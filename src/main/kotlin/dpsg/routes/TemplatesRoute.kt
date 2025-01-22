package dpsg.routes

import dpsg.OrganizationTemplate
import dpsg.plugins.UserSession
import dpsg.services.UserService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.templates(userService: UserService) {
    get("/template") {
        val session = call.principal<UserSession>()
        println("SESSION: $session")
        if (session == null) {
            call.respond("No User")
            return@get
        }
        val template = userService.getTemplateForUser(session.username)
        if (template == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.respond(template)
    }
}

