package no.nav.syfo.auth.azure

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.nio.charset.Charset

@Component
class AzureAdTokenClient @Autowired constructor(
    @Value("\${azure.app.client.id}") private val azureAppClientId: String,
    @Value("\${azure.app.client.secret}") private val azureAppClientSecret: String,
    @Value("\${azure.openid.config.token.endpoint}") private val azureTokenEndpoint: String
) {
    fun getOnBehalfOfToken(scopeClientId: String, token: String): String {
        return getToken(requestEntity(scopeClientId, token))
    }

    fun getSystemToken(scopeClientId: String): String {
        return getToken(systemTokenRequestEntity(scopeClientId))
    }

    private fun getToken(requestEntity: HttpEntity<MultiValueMap<String, String>>): String {
        val response = RestTemplate().postForEntity(azureTokenEndpoint, requestEntity, AzureAdTokenResponse::class.java)
        return response.body?.toAzureAdToken()?.accessToken ?: throw RestClientResponseException(
            "Failed to get token",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "",
            HttpHeaders(),
            ByteArray(0),
            Charset.defaultCharset()
        )
    }

    private fun commonRequestBody(scopeClientId: String): LinkedMultiValueMap<String, String> {
        val body = LinkedMultiValueMap<String, String>()
        body.add("client_id", azureAppClientId)
        body.add("scope", scopeClientId)
        body.add("client_secret", azureAppClientSecret)
        return body
    }

    private fun requestEntity(scopeClientId: String, token: String): HttpEntity<MultiValueMap<String, String>> {
        val body = commonRequestBody(scopeClientId)
        body.add("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
        body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
        body.add("assertion", token)
        body.add("requested_token_use", "on_behalf_of")
        return HttpEntity(body, HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA })
    }

    private fun systemTokenRequestEntity(scopeClientId: String): HttpEntity<MultiValueMap<String, String>> {
        val body = commonRequestBody(scopeClientId)
        body.add("grant_type", "client_credentials")
        return HttpEntity(body, HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA })
    }
}
