package no.nav.syfo.narmesteleder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.tokendings.TokenDingsConsumer
import java.time.LocalDate
import java.time.LocalDateTime

const val baseUrl = "http://localhost:9000"
const val ansattFnr = "12345678910"

class NarmesteLederClientTest : FunSpec({
    val tokenDingsConsumer: TokenDingsConsumer = mockk<TokenDingsConsumer>()
    val contextHolder: TokenValidationContextHolder = mockk<TokenValidationContextHolder>()
    val mockTokenValidationContext = mockk<TokenValidationContext>()
    val mockJwtTokenClaims = mockk<JwtTokenClaims>()
    val mockJwtToken = mockk<JwtToken>()
    val narmesteLederClient = NarmesteLederClient(
        baseUrl = baseUrl,
        targetApp = "hei",
        tokenDingsConsumer = tokenDingsConsumer,
        contextHolder = contextHolder
    )
    val isnarmestelederServer = WireMockServer(9000)
    listener(WireMockListener(isnarmestelederServer, ListenerMode.PER_TEST))

    beforeTest {
        every { contextHolder.getTokenValidationContext() } returns mockTokenValidationContext
        every { mockTokenValidationContext.getClaims(TokenXUtil.TokenXIssuer.TOKENX) } returns mockJwtTokenClaims
        every { mockJwtTokenClaims.getStringClaim("pid") } returns ansattFnr
        every { mockJwtTokenClaims.getStringClaim("client_id") } returns "clientId"
        every { tokenDingsConsumer.exchangeToken(any(), any()) } returns "123abc"
        every { mockTokenValidationContext.getJwtToken(any()) } returns mockJwtToken
        every { mockJwtToken.tokenAsString } returns "heihei"
    }

    test("Henter alle ledere uavhengig av status") {
        val aktivLederIBedrift1 = createNarmestelederRelasjon(
            narmesteLederPersonIdentNumber = "123",
            virksomhetsnummer = "999",
            status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
        )
        val inaktivLederIBedrift1 = createNarmestelederRelasjon(
            narmesteLederPersonIdentNumber = "234",
            virksomhetsnummer = "999",
            status = NarmesteLederRelasjonStatus.DEAKTIVERT_LEDER
        )
        val aktivLederIBedrift2 = createNarmestelederRelasjon(
            narmesteLederPersonIdentNumber = "6667",
            virksomhetsnummer = "876",
            status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
        )

        isnarmestelederServer.stubNarmestelederRelasjoner(
            listOf(aktivLederIBedrift1, inaktivLederIBedrift1, aktivLederIBedrift2)
        )

        val alleLedere = narmesteLederClient.alleLedereForSykmeldt(ansattFnr)

        alleLedere.size shouldBe 3
    }

    test("aktivNarmesteLederIVirksomhet") {
        val aktivLederIBedrift1 = createNarmestelederRelasjon(
            narmesteLederPersonIdentNumber = "123",
            virksomhetsnummer = "999",
            status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
        )
        val inaktivLederIBedrift1 = createNarmestelederRelasjon(
            narmesteLederPersonIdentNumber = "234",
            virksomhetsnummer = "999",
            status = NarmesteLederRelasjonStatus.DEAKTIVERT_LEDER
        )
        val aktivLederIBedrift2 = createNarmestelederRelasjon(
            narmesteLederPersonIdentNumber = "6667",
            virksomhetsnummer = "876",
            status = NarmesteLederRelasjonStatus.INNMELDT_AKTIV
        )

        isnarmestelederServer.stubNarmestelederRelasjoner(
            listOf(aktivLederIBedrift1, inaktivLederIBedrift1, aktivLederIBedrift2)
        )

        val narmesteLederIVirksomhet = narmesteLederClient.aktivNarmesteLederIVirksomhet(ansattFnr, "999")

        narmesteLederIVirksomhet shouldBe aktivLederIBedrift1
    }
})

fun WireMockServer.stubNarmestelederRelasjoner(narmesteLederRelasjoner: List<NarmesteLederRelasjonDTO>) {
    this.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/api/selvbetjening/v1/narmestelederrelasjoner"))
            .willReturn(
                aResponse()
                    .withBody(objectMapper.writeValueAsString(narmesteLederRelasjoner))
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
            )
    )
}

fun createNarmestelederRelasjon(
    narmesteLederPersonIdentNumber: String,
    virksomhetsnummer: String,
    status: NarmesteLederRelasjonStatus
): NarmesteLederRelasjonDTO {
    return NarmesteLederRelasjonDTO(
        uuid = "123",
        narmesteLederEpost = "hei@hei.no",
        narmesteLederNavn = "Jens Jensen",
        virksomhetsnummer = virksomhetsnummer,
        narmesteLederTelefonnummer = "9988776655",
        narmesteLederPersonIdentNumber = narmesteLederPersonIdentNumber,
        status = status.name,
        arbeidsgiverForskutterer = false,
        aktivFom = LocalDate.now().minusWeeks(30),
        aktivTom = null,
        timestamp = LocalDateTime.now(),
        virksomhetsnavn = "Hopp",
        arbeidstakerPersonIdentNumber = ansattFnr,
    )
}

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
