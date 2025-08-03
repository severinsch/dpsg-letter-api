package dpsg.services

import dpsg.LetterConfigModel
import dpsg.Role
import kotlinx.io.IOException
import java.io.File
import java.time.format.DateTimeFormatter

fun buildLetter(config: LetterConfigModel): String? {
    validateConfig(config)

    val contentFile = createContentFile(config)
    val settingsFile = createSettingsFile(config)

    // create build dir and copy latex files there
    val buildDir = File("build_latex")
    if (!buildDir.isDirectory) {
        buildDir.mkdirs()
    }
    val resourceBasePath = System.getenv("RESOURCES_BASE_PATH") ?: "/app/resources"

    val latexDir = File(resourceBasePath, "/latex")
    latexDir.copyRecursively(buildDir, true)

    // copy logo file
    val logoSettings = LOGO_SETTINGS[config.logo] ?: throw IllegalStateException("Logo settings not found")
    val logoFile = File(resourceBasePath, logoSettings.file)
    if (!logoFile.exists()) {
        throw IllegalStateException("Logo file not found: ${logoFile.absolutePath}")
    }
    val logoBuildPath = File(buildDir, "pictures/logos/${logoFile.name}")
    logoFile.copyTo(logoBuildPath, overwrite = true)

    // add content and settings files
    val contentPath = File(buildDir, "generated")
    contentPath.mkdirs()
    File(contentPath, "content.tex").writeText(contentFile)
    File(contentPath, "settings.tex").writeText(settingsFile)

    // run latexmk, using latexmk instead of pdflatex to ensure multiple compilations if necessary (e.g. for first compile in empty dir)
    val cmd = "latexmk -no-shell-escape -pdf -output-directory=out main.tex"
    val parts = cmd.split("\\s".toRegex())
    // use build_latex as working directory
    try {
        val process =
            ProcessBuilder(*parts.toTypedArray())
                .apply {
                    directory(buildDir)

                    redirectOutput(ProcessBuilder.Redirect.PIPE)
                    redirectError(ProcessBuilder.Redirect.PIPE)
                }
                .start()

        val exitCode = process.waitFor()
        //val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        if (exitCode != 0) {
            println("Error: $error")
            return null
        } else {
            //println("Output: $output")
            return "build_latex/out/main.pdf"
        }

    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun sanitizeForLatex(input: String): String {
    // replace special characters with LaTeX-safe equivalents
    return input
        .replace("\\", "\\textbackslash ")  // order important here
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("&", "\\&")
        .replace("%", "\\%")
        .replace("$", "\\$")
        .replace("^", "\\textasciicircum{}")
        .replace("~", "\\textasciitilde{}")
        .replace("_", "\\_")}

fun validateConfig(config: LetterConfigModel) {
    if (config.title.isBlank() || config.content.isBlank() || config.place.isBlank() || config.address.isBlank() || config.organizationName.isBlank()) {
        throw IllegalStateException("All fields must be filled")
    }
    if (config.people.size < 2) {
        throw IllegalStateException("There must be at least two people in the Vorstand")
    }
}

fun createContentFile(config: LetterConfigModel): String {
    val content = "# ${config.title} {-}\n\n${config.content}"  // {-} removes section numbering
    return convertMarkdownToLatex(content) ?: return ""
}

fun convertMarkdownToLatex(content: String): String? {
    val buildDir = File(System.getProperty("user.dir"))

    val cmd = "pandoc --lua-filter=src/main/lua_filters/metadata_table.lua -f markdown+hard_line_breaks -t latex"
    val parts = cmd.split("\\s".toRegex())

    try {
        val process =
            ProcessBuilder(*parts.toTypedArray())
                .apply {
                    directory(buildDir)

                    redirectOutput(ProcessBuilder.Redirect.PIPE)
                    redirectError(ProcessBuilder.Redirect.PIPE)
                }
                .start()

        process.outputStream.bufferedWriter().use {
            it.write(content)
            it.flush()
        }

        val exitCode = process.waitFor()

        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        if (exitCode != 0) {
            println("Error: $error")
        } else {
            return output
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

fun createSettingsFile(config: LetterConfigModel): String {
    fun createBooleanSetting(name: String, value: Boolean): String {
        val creationStmnt = "\\newboolean{$name}"
        val settingStmnt = "\\setboolean{$name}{$value}"
        return "$creationStmnt\n$settingStmnt"
    }

    var settingsFile = ""

    // set booleans
    val signUp = createBooleanSetting("includeSignUp", config.includeSignUp)
    val frontPage = createBooleanSetting("includeFrontPage", config.includeFrontPage)
    val holidayLaw = createBooleanSetting("includeHolidayLawPage", config.includeHolidayLawPage)
    val abroadClause = createBooleanSetting("signUpIncludeAbroadClause", config.signUpIncludeAbroadClause)
    settingsFile += "$signUp\n$frontPage\n$holidayLaw\n$abroadClause\n"

    // set date
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val formattedDate = config.date.format(formatter)
    val currentDateCommand = "\\newcommand{\\currentDate}{$formattedDate}"
    settingsFile += currentDateCommand + "\n"

    // set sidebar information
    val place = "\\newcommand{\\place}{${sanitizeForLatex(config.place)}}"
    val address = "\\newcommand{\\address}{${sanitizeForLatex(config.address)}}"
    val orgName = "\\newcommand{\\orgName}{${sanitizeForLatex(config.organizationName)}}"
    // only set website if it is provided
    val website = if (config.website != null) "\\newcommand{\\website}{${sanitizeForLatex(config.website)}}\n" else ""
    settingsFile += "$place\n$address\n$orgName\n$website"


    val indexToWord = mapOf(0 to "One", 1 to "Two", 2 to "Three")
    fun getRoleName(role: Role): String {
        return when (role) {
            Role.Vorstand -> "Stammesvorsitzender"
            Role.Vorstaendin -> "Stammesvorsitzende"
            Role.Kurat -> "Kurat"
            Role.Kuratin -> "Kuratin"
        }
    }
    for ((index, person) in config.people.withIndex()) {
        val name = "\\newcommand{\\person${indexToWord[index]}Name}{${sanitizeForLatex(person.name)}}"
        val role = "\\newcommand{\\person${indexToWord[index]}Role}{${getRoleName(person.role)}}"
        val email = "\\newcommand{\\person${indexToWord[index]}Email}{${sanitizeForLatex(person.email)}}"
        val phone = if (person.phone != null) "\\newcommand{\\person${indexToWord[index]}Phone}{${sanitizeForLatex(person.phone)}}\n" else ""
        settingsFile += "$name\n$role\n$email\n$phone"
    }

    val bankInfo = if (config.bankInformation != null) (
            "\\newcommand{\\bankOrgName}{${sanitizeForLatex(config.bankInformation.orgName)}}\n" +
            "\\newcommand{\\bankName}{${sanitizeForLatex(config.bankInformation.bankName)}}\n" +
            "\\newcommand{\\iban}{${sanitizeForLatex(config.bankInformation.iban)}}\n"
    ) else ""
    settingsFile += bankInfo

    val logoSettings = LOGO_SETTINGS[config.logo] ?: throw IllegalStateException("Logo settings not found")
    val logoFile = "\\newcommand{\\logoFile}{${logoSettings.file}}"
    val logoWidth = "\\newcommand{\\logoWidth}{${logoSettings.width}}"
    val logoXShift = "\\newcommand{\\logoShiftX}{${logoSettings.xShift}}"
    val logoYShift = "\\newcommand{\\logoShiftY}{${logoSettings.yShift}}"
    settingsFile += "$logoFile\n$logoWidth\n$logoXShift\n$logoYShift\n"

    return settingsFile
}