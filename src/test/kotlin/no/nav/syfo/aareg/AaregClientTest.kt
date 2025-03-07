package no.nav.syfo.aareg

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.aareg.exceptions.RestErrorFromAareg
import no.nav.syfo.aareg.utils.AaregClientTestUtils.AT_AKTORID
import no.nav.syfo.aareg.utils.AaregClientTestUtils.AT_FNR
import no.nav.syfo.aareg.utils.AaregClientTestUtils.ORGNUMMER
import no.nav.syfo.aareg.utils.AaregClientTestUtils.simpleArbeidsforhold
import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.cache.ValkeyStore
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.util.configuredJacksonMapper
import org.springframework.test.util.ReflectionTestUtils

const val AAREG_URL = "http://localhost:9000"
const val AAREG_SCOPE = "scope"

class AaregClientTest : FunSpec({
    val metrikk = mockk<Metrikk>(relaxed = true)

    val azureAdTokenClient = mockk<AzureAdTokenClient>()

    val valkeyStore = mockk<ValkeyStore>(relaxed = true)

    val aaregClient = AaregClient(metrikk, azureAdTokenClient, valkeyStore, AAREG_URL, AAREG_SCOPE)

    val isAaregServer = WireMockServer(9000)
    listener(WireMockListener(isAaregServer, ListenerMode.PER_TEST))

    beforeTest {
        ReflectionTestUtils.setField(aaregClient, "url", AAREG_URL)
        every { azureAdTokenClient.getSystemToken("scope") } returns "token"
        every { valkeyStore.getListObject(any<String>(), Arbeidsforhold::class.java) } returns null
    }
    afterTest { isAaregServer.resetAll() }

    test("get arbeidsforhold arbeidstaker") {

        val expectedArbeidsforholdList = listOf(simpleArbeidsforhold())
        isAaregServer.stubAaregRelasjoner(expectedArbeidsforholdList)
        val actualArbeidsforholdList = aaregClient.arbeidsforholdArbeidstaker(AT_FNR)
        actualArbeidsforholdList.size shouldBe 1

        val arbeidsforhold = actualArbeidsforholdList[0]

        arbeidsforhold.arbeidsgiver.organisasjonsnummer shouldBe ORGNUMMER
        arbeidsforhold.arbeidstaker?.aktoerId shouldBe AT_AKTORID
        arbeidsforhold.arbeidstaker?.offentligIdent shouldBe AT_FNR

        verify { metrikk.tellHendelse("call_aareg") }
        verify { metrikk.tellHendelse("call_aareg_success") }
    }

    test("arbeidsforholdArbeidstaker fail") {

        shouldThrowExactly<RestErrorFromAareg> {
            aaregClient.arbeidsforholdArbeidstaker(AT_FNR)
        }
    }
})

fun WireMockServer.stubAaregRelasjoner(aaregRelasjoner: List<Arbeidsforhold>) {
    this.stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/api/v1/arbeidstaker/arbeidsforhold"))
            .willReturn(
                aResponse()
                    .withBody(configuredJacksonMapper().writeValueAsString(aaregRelasjoner))
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
            )
    )
}
