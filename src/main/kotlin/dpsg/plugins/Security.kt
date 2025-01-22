package dpsg.plugins

import dpsg.BankInformation
import dpsg.OrganizationTemplate
import dpsg.Role
import dpsg.Vorstand
import dpsg.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.hsts.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: Int,
    val username: String,
    val organization: String
)

@Serializable
data class Credentials(
    val username: String,
    val password: String
)

fun Application.configureSecurity(userService: UserService) {
    //install(HSTS)
    //install(XForwardedHeaders)
    //install(HttpsRedirect)

    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600 // 1 hour
        }
    }

    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                session
            }
            challenge {
                println("IN CHALLENGE")
                if (call.request.header("X-Requested-With") != "XMLHttpRequest") {
                    call.respondRedirect("/login")
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "message" to "Authentication required",
                        "code" to "AUTH_REQUIRED"
                    ))
                }
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

    routing {
        post("/login") {
            val credentials = call.receive<Credentials>()
            val user = userService.validateUser(credentials.username, credentials.password)

            if (user != null) {
                call.sessions.set(UserSession(user.id, user.username, user.organization))
                call.respond(HttpStatusCode.OK, mapOf("message" to "Login successful"))
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid credentials"))
            }
        }
    }
}
