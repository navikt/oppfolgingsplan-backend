package no.nav.syfo.pdl

import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

// Lenke til relevant behandling i behandlingskatalogen:
// https://behandlingskatalog.nais.adeo.no/process/team/6a3b85e0-0e06-4f58-95bb-4318e31c4b2b/cca7c846-e5a5-4a10-bc7e-6abd6fc1b0f5
const val BEHANDLINGSNUMMER_OPPFOLGINGSPLAN = "B275"
const val PDL_BEHANDLINGSNUMMER_HEADER = "behandlingsnummer"

@Service
class PdlClient(
    private val metric: Metrikk,
    @Value("\${pdl.client.id}") private val pdlClientId: String,
    @Value("\${pdl.url}") private val pdlUrl: String,
    private val azureAdTokenClient: AzureAdTokenClient,
) {
    fun person(ident: String): PdlHentPerson? {
        metric.tellHendelse("call_pdl")

        val query = this::class.java.getResource("/pdl/hentPerson.graphql").readText().replace("[\n\r]", "")
        val entity = createRequestEntity(PdlRequest(query, Variables(ident)))
        try {
            val pdlPerson = RestTemplate().exchange(
                pdlUrl,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<PdlPersonResponse>() {}
            )

            val pdlPersonReponse = pdlPerson.body!!
            return if (pdlPersonReponse.errors != null && pdlPersonReponse.errors.isNotEmpty()) {
                metric.tellHendelse("call_pdl_fail")
                pdlPersonReponse.errors.forEach {
                    LOG.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                }
                null
            } else {
                metric.tellHendelse("call_pdl_success")
                pdlPersonReponse.data
            }
        } catch (exception: RestClientResponseException) {
            metric.tellHendelse("call_pdl_fail")
            LOG.error("Error from PDL with request-url: $pdlUrl", exception)
            throw exception
        }
    }

    private fun createRequestEntity(request: PdlRequest): HttpEntity<PdlRequest> {
        val token: String = azureAdTokenClient.getSystemToken(pdlClientId)
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(PDL_BEHANDLINGSNUMMER_HEADER, BEHANDLINGSNUMMER_OPPFOLGINGSPLAN)
        headers.set(AUTHORIZATION, bearerHeader(token))
        return HttpEntity(request, headers)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PdlClient::class.java)
    }
}
