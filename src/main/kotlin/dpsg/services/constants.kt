package dpsg.services

import dpsg.Logo

data class LogoSettings(
    val file: String,
    val width: String,
    val xShift: String,
    val yShift: String
)

// Logo generation done using https://dpsg.de/de/logogenerator with 600px width and 27px font size
// gray border from generated logo removed using GIMP

val LOGO_SETTINGS = mapOf(
    Logo.DPSG to LogoSettings("pictures/logos/dpsg_logo", "5.2cm", "-0.6cm", "-1.3cm"),
    Logo.Moosburg to LogoSettings("pictures/logos/moosburg_logo_fixed_border", "5.2cm", "-0.6cm", "-1.3cm"),
    Logo.Langenbach to LogoSettings("pictures/logos/langenbach_logo_cropped", "4.6cm", "-0.5cm", "-0.5cm")
)