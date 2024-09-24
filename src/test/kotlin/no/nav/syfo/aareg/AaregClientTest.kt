package no.nav.syfo.aareg

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.aareg.exceptions.RestErrorFromAareg
import no.nav.syfo.aareg.utils.AaregClientTestUtils.AT_AKTORID
import no.nav.syfo.aareg.utils.AaregClientTestUtils.AT_FNR
import no.nav.syfo.aareg.utils.AaregClientTestUtils.ORGNUMMER
import no.nav.syfo.aareg.utils.AaregClientTestUtils.simpleArbeidsforhold
import no.nav.syfo.auth.azure.AzureAdTokenClient
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.narmesteleder.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.util.ReflectionTestUtils

const val AAREG_URL = "http://localhost:9000"
const val AAREG_SCOPE = "scope"

class AaregClientTest : FunSpec({
    val metrikk = mockk<Metrikk>(relaxed = true)

    val azureAdTokenClient = mockk<AzureAdTokenClient>()

    val aaregClient = AaregClient(metrikk, azureAdTokenClient, AAREG_URL, AAREG_SCOPE)

    val isAaregServer = WireMockServer(9000)
    listener(WireMockListener(isAaregServer, ListenerMode.PER_TEST))

    beforeTest {
        ReflectionTestUtils.setField(aaregClient, "url", AAREG_URL)
        every { azureAdTokenClient.getSystemToken("scope") } returns "token"
    }
    afterTest { isAaregServer.resetAll() }

    test("get arbeidsforhold arbeidstaker") {

        val expectedArbeidsforholdList = listOf(simpleArbeidsforhold())
        isAaregServer.stubAaregRelasjoner(expectedArbeidsforholdList)
        val actualArbeidsforholdList = aaregClient.arbeidsforholdArbeidstaker(AT_FNR)

        assertThat(actualArbeidsforholdList.size).isEqualTo(1)

        val arbeidsforhold = actualArbeidsforholdList[0]

        assertThat(arbeidsforhold.arbeidsgiver?.organisasjonsnummer).isEqualTo(ORGNUMMER)
        assertThat(arbeidsforhold.arbeidstaker?.aktoerId).isEqualTo(AT_AKTORID)
        assertThat(arbeidsforhold.arbeidstaker?.offentligIdent).isEqualTo(AT_FNR)

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
                    .withBody(objectMapper.writeValueAsString(aaregRelasjoner))
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
            )
    )
}
