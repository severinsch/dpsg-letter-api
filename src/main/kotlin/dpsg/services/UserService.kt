package dpsg.services

import dpsg.OrganizationTemplate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import java.io.File

// User data classes
@Serializable
data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val organization: String,
    val template: OrganizationTemplate?,
)

// User service
class UserService {
    private val users = mutableMapOf<String, User>()

    fun loadUsers() {
        val json = File("src/main/resources/users.json").readText()
        val users = Json.decodeFromString<List<User>>(json)
        users.forEach { this.users[it.username] = it }
        println("Loaded ${users.size} users: ${this.users}")
    }

    fun getTemplateForUser(username: String): OrganizationTemplate? {
        println("Getting template for user $username from available ${users.keys}")
        return users[username]?.template
    }

    fun addUser(username: String, password: String, organization: String, template: OrganizationTemplate?) {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = User(
            id = users.size + 1,
            username = username,
            passwordHash = hashedPassword,
            organization = organization,
            template = template,
        )
        users[username] = user
    }

    fun validateUser(username: String, password: String): User? {
        val user = users[username] ?: return null
        return if (BCrypt.checkpw(password, user.passwordHash)) user else null
    }
}