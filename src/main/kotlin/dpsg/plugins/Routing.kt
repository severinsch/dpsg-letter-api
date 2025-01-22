package dpsg.plugins

import dpsg.routes.apiRoute
import dpsg.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureRouting(userService: UserService) {
    install(CORS) {
        anyHost()

        methods.add(HttpMethod.Get)
        methods.add(HttpMethod.Post)
        methods.add(HttpMethod.Delete)
        methods.add(HttpMethod.Put)

        // Allow common headers and any other headers your client might need to send.
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        // Allow sending credentials, this is important if your API requires authentication.
        allowCredentials = true

        // You may need to expose headers so that the client-side JavaScript can access them.
        exposeHeader(HttpHeaders.Authorization)
        exposeHeader(HttpHeaders.WWWAuthenticate)

        // Configuring cache to remember CORS preflight request; default is 24 hours.
        maxAgeInSeconds = 24 * 60 * 60
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
    routing {
        openAPI(path="openapi")
    }

    routing {
        apiRoute(userService = userService)
    }
}
