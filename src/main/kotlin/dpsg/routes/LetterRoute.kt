package dpsg.routes

import dpsg.LetterConfigModel
import dpsg.services.buildLetter
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.letter() {
    route("/letter") {
        post {
            try {
                val config = call.receive<LetterConfigModel>()

                val file = buildLetter(config)
                if (file == null) {
                    call.respond(HttpStatusCode.InternalServerError, "Error building letter")
                    return@post
                }
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "letter.pdf")
                        .toString()
                )
                call.respondFile(file)
            } catch (ex: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid config: ${ex.message}")
            } catch (ex: JsonConvertException) {
                println("JSON CONVERT EXCEPTION") // TODO how to catch and add info?
                call.respond(HttpStatusCode.BadRequest, "Invalid JSON: ${ex.message}")
            }
        }
    }
}