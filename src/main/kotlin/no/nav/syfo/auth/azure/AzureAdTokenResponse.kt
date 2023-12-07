package no.nav.syfo.auth.azure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import java.time.LocalDateTime

@SuppressWarnings("SerialVersionUIDInSerializableClass", "ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureAdTokenResponse(
    val access_token: String,
    val expires_in: Long
) : Serializable

fun AzureAdTokenResponse.toAzureAdToken(): AzureAdToken {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return AzureAdToken(
        accessToken = this.access_token,
        expires = expiresOn
    )
}
