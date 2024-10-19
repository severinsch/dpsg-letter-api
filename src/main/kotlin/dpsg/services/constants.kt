package dpsg.services

import dpsg.Logo

data class LogoSettings(
    val file: String,
    val width: String,
    val xShift: String,
    val yShift: String
)

val LOGO_SETTINGS = mapOf(
    Logo.DPSG to LogoSettings("pictures/logos/dpsg_logo", "5.2cm", "-0.6cm", "-1.3cm"),
    Logo.Langenbach to LogoSettings("pictures/logos/langenbach_logo_cropped", "4.6cm", "-0.5cm", "-0.5cm")
)