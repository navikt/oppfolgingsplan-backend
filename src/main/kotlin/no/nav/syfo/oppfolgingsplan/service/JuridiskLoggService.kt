package no.nav.syfo.oppfolgingsplan.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Service
class JuridiskLoggService {
    @Value("\${lagrejuridisklogg.rest.url}")
    private val altinnUrl: String? = null

    @Value("\${srv.username}")
    private val altinnUsername: String? = null

    @Value("\${srv.password}")
    private val systemPassword: String? = null

    fun loggSendOppfoelgingsdialogTilAltinn(oppfolgingsplanAltinn: OppfolgingsplanAltinn) {
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanAltinn.oppfolgingsplan
        try {
            val loggMelding: LoggMelding = LoggMelding()
                .meldingsId(oppfolgingsplan.uuid)
                .avsender(oppfolgingsplan.arbeidstaker.aktoerId)
                .mottaker(oppfolgingsplan.virksomhet.virksomhetsnummer)
                .meldingsInnhold(oppfolgingsplanAltinn.getHashOppfoelgingsdialogPDF())

            val rt = RestTemplate()
            rt.messageConverters.add(MappingJackson2HttpMessageConverter())
            rt.messageConverters.add(StringHttpMessageConverter())

            val credentials: String = basicCredentials(altinnUsername, systemPassword)
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.add(HttpHeaders.AUTHORIZATION, credentials)
            val requestPost: HttpEntity<LoggMelding> = HttpEntity<LoggMelding>(loggMelding, headers)

            rt.exchange(altinnUrl, HttpMethod.POST, requestPost, LoggMelding::class.java)
            log.info(
                "Logget sending av oppfølgingsplan med id {} i juridisk loggSendOppfoelgingsdialogTilAltinn",
                oppfolgingsplan.id
            )
        } catch (e: RestClientResponseException) {
            log.error(
                "Klientfeil mot JuridiskLogg ved logging av sendt oppfølgingsplan med id {} til Altinn. HTTP-status: {} og {}",
                oppfolgingsplan.id,
                e.statusCode.value(),
                e.statusText,
                e
            )
            throw e
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JuridiskLoggService::class.java)
    }
}
