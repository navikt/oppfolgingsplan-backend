package no.nav.syfo.util

import java.util.Base64

fun basicCredentials(credentialUsername: String?, credentialPassword: String?): String {
    return "Basic ${Base64.getEncoder().encodeToString("$credentialUsername:$credentialPassword".toByteArray())}"
}

fun bearerHeader(token: String?): String {
    return "Bearer $token"
}
