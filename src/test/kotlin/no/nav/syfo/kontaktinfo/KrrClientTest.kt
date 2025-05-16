package no.nav.syfo.kontaktinfo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.cache.ValkeyStore
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.configuredJacksonMapper
import org.springframework.http.HttpStatus
import java.util.UUID

const val BASE_URL = "http://localhost:9000"
const val REST_PATH = "/rest/v1/personer"

class KrrClientTest : FunSpec({
    val azureAdTokenConsumer = mockk<AzureAdTokenClient>()
    val metric = mockk<Metrikk>()
    val krrScope = "some-scope"
    val krrUrl = "$BASE_URL$REST_PATH"
    val valkeyStore = mockk<ValkeyStore>()

    val validFnr = "12345678910"
    val unknownFnr = "01987654321"

    val krrClient = KrrClient(azureAdTokenConsumer, metric, krrScope, krrUrl, valkeyStore)
    val krrServer = WireMockServer(9000)
    listener(WireMockListener(krrServer, ListenerMode.PER_TEST))
    beforeTest {
        every { azureAdTokenConsumer.getSystemToken(krrScope) } returns UUID.randomUUID().toString()
        every { metric.countOutgoingReponses(KrrClient.METRIC_CALL_KRR, any()) } returns Unit
        every { valkeyStore.setObject(any(), any<DigitalKontaktinfo>(), any()) } returns Unit
    }

    test("Skips request and returns value from Valkey if found") {
        val digitalKontaktinfo = DigitalKontaktinfo(
            epostadresse = "some.name@acme.com",
        )
        every { valkeyStore.getObject("krr_fnr_$validFnr", DigitalKontaktinfo::class.java) } returns digitalKontaktinfo
        val response = krrClient.kontaktinformasjon(validFnr)
        response shouldBe digitalKontaktinfo
    }

    test("Throws error on request when fnr is not included in response") {
        every { valkeyStore.getObject("krr_fnr_$unknownFnr", DigitalKontaktinfo::class.java) } returns null
        krrServer.stubPersonerResponse(
            PostPersonerResponse(
                personer = emptyMap(),
                feil = mapOf(unknownFnr to "Not Found")
            )
        )
        val exception = shouldThrow<KrrRequestFailedException> {
            krrClient.kontaktinformasjon(unknownFnr)
        }
        exception.message shouldContain "Response did not contain person"
    }

    test("Throws error with did not contain person when response contains json with with invalid contract") {
        every { valkeyStore.getObject("krr_fnr_$validFnr", DigitalKontaktinfo::class.java) } returns null
        krrServer.stubPersonerWithCustomResponse(
            response = mapOf("what" to "ever"),
            HttpStatus.OK
        )
        val exception = shouldThrow<KrrRequestFailedException> {
            krrClient.kontaktinformasjon(validFnr)
        }
        exception.message shouldContain "Response did not contain person"
    }

    test("Throws error with message for unexpected status on response") {
        every { valkeyStore.getObject("krr_fnr_$validFnr", DigitalKontaktinfo::class.java) } returns null
        krrServer.stubPersonerWithCustomResponse(
            response = mapOf("what" to "ever"),
            HttpStatus.ACCEPTED
        )
        val exception = shouldThrow<KrrRequestFailedException> {
            krrClient.kontaktinformasjon(validFnr)
        }
        exception.message shouldContain "Received response with status code: ${HttpStatus.ACCEPTED}"
    }

    test("Returns KontaktInfo on successfull request") {
        val digitalKontaktInfo = DigitalKontaktinfo(
            epostadresse = "$validFnr@acme.com",
        )
        every { valkeyStore.getObject("krr_fnr_$validFnr", DigitalKontaktinfo::class.java) } returns null
        krrServer.stubPersonerResponse(
            PostPersonerResponse(
                personer = mapOf(validFnr to digitalKontaktInfo),
                feil = emptyMap()
            )
        )
        val response = krrClient.kontaktinformasjon(validFnr)
        response.epostadresse shouldBe digitalKontaktInfo.epostadresse
        verify(exactly = 1) {
            valkeyStore.setObject(key = eq("krr_fnr_$validFnr"), value = eq(digitalKontaktInfo), any())
        }
    }
})

fun WireMockServer.stubPersonerResponse(response: PostPersonerResponse) {
    this.stubFor(
        WireMock.post(WireMock.urlPathEqualTo(REST_PATH))
            .willReturn(
                aResponse()
                    .withBody(configuredJacksonMapper().writeValueAsString(response))
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
            )
    )
}

fun WireMockServer.stubPersonerWithCustomResponse(response: Map<String, String>, statusCode: HttpStatus) {
    this.stubFor(
        WireMock.post(WireMock.urlPathEqualTo(REST_PATH))
            .willReturn(
                aResponse()
                    .withBody(configuredJacksonMapper().writeValueAsString(response))
                    .withHeader("Content-Type", "application/json")
                    .withStatus(statusCode.value())
            )
    )
}
