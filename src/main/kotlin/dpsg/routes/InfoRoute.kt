package dpsg.routes

import dpsg.Logo
import dpsg.services.LOGO_SETTINGS
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.io.path.*


fun Route.infoRoute() {
    route("/logo/{orgName}") {
        get {
            if (call.parameters["orgName"] == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing organization name")
                return@get
            }
            val orgName = call.parameters["orgName"]!!
            println("Requested logo for organization: ${orgName.lowercase()}")
            val logo: Logo = when (orgName.lowercase()) {
                "dpsg" -> Logo.DPSG
                "langenbach" -> Logo.Langenbach
                "moosburg" -> Logo.Moosburg
                else -> {
                    call.respond(HttpStatusCode.NotFound, "Logo not found for organization: $orgName")
                    return@get
                }
            }
            val settings = LOGO_SETTINGS[logo]
            if (settings == null) {
                call.respond(HttpStatusCode.InternalServerError, "Logo settings not found for organization: $orgName")
                return@get
            }
            val resourceBasePath = System.getenv("RESOURCES_BASE_PATH") ?: "src/main/resources"
            val file = Path(resourceBasePath, settings.file)
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, "Logo file not found for organization: $orgName")
            } else {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
                )
                call.respondFile(file.toFile())
            }
        }
    }
}