package no.nav.syfo.auth.tokenx

import java.io.Serializable
import java.time.LocalDateTime

@SuppressWarnings("SerialVersionUIDInSerializableClass")
data class TokenXToken(
    val accessToken: String,
    val expires: LocalDateTime
) : Serializable
