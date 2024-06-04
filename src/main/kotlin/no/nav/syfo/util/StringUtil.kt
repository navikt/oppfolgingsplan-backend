package no.nav.syfo.util

import java.util.regex.Pattern

fun fodselsnummerValid(fnr: String): Boolean = Pattern.compile("\\d{11}").matcher(fnr).matches()

fun fodselsnummerInvalid(fnr: String): Boolean = !fodselsnummerValid(fnr)

fun String.lowerCapitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}
