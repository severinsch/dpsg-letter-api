package dpsg

import dpsg.utils.LocalDateSerializer
import io.ktor.resources.*
import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class Role {
    Vorstand,
    Vorstaendin,
    Kurat,
    Kuratin,
}

enum class Logo {
    DPSG,
    Langenbach,
    Moosburg,
}

@Serializable
data class Vorstand(
    val name: String,
    val role: Role,
    val email: String,
    val phone: String? = null
)

@Serializable
data class BankInformation(
    val orgName: String,
    val bankName: String,
    val iban: String
)

@Serializable
@Resource("/letter")
data class LetterConfigModel(
    val title: String,
    val content: String,
    val includeSignUp: Boolean,
    val signUpIncludeAbroadClause: Boolean,
    val includeFrontPage: Boolean,
    val includeHolidayLawPage: Boolean,
    val place: String,
    val address: String,
    val organizationName: String,
    val website: String? = null,
    val people: List<Vorstand>,
    val bankInformation: BankInformation? = null,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    val logo: Logo = Logo.Langenbach
)