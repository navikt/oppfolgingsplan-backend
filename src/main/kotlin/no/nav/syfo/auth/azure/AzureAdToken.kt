package no.nav.syfo.auth.azure

import java.io.Serializable
import java.time.LocalDateTime

@SuppressWarnings("SerialVersionUIDInSerializableClass")
data class AzureAdToken(
    val accessToken: String,
    val expires: LocalDateTime
) : Serializable
