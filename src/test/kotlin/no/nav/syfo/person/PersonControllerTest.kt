package no.nav.syfo.person

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.pdl.GeografiskTilknytning
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.PdlHentPerson
import no.nav.syfo.pdl.PdlPerson
import no.nav.syfo.pdl.PdlPersonNavn
import org.springframework.http.HttpStatus

class PersonControllerTest : FunSpec({
    val contextHolder = mockk<TokenValidationContextHolder>()
    val tokenValidationContext = mockk<TokenValidationContext>()
    val jwtTokenClaims = mockk<JwtTokenClaims>()
    val pdlClient = mockk<PdlClient>()
    val brukertilgangService = mockk<BrukertilgangService>()

    val fnr = "12345678910"
    val innloggetFnr = "10987654321"

    beforeTest {
        every { contextHolder.getTokenValidationContext() } returns tokenValidationContext
        every { tokenValidationContext.getClaims(TokenXUtil.TokenXIssuer.TOKENX) } returns jwtTokenClaims
        every { jwtTokenClaims.getStringClaim("client_id") } returns "oppfolgingsplan-client-id"
        every { jwtTokenClaims.getStringClaim("pid") } returns innloggetFnr
        every { brukertilgangService.tilgangTilOppslattIdent(innloggetFnr, fnr) } returns true
    }

    test("getPerson returnerer pilotUser true nar gammel oppfolgingsplan er skrudd av uavhengig av geografisk tilknytning") {
        val controller = personController(
            contextHolder = contextHolder,
            pdlClient = pdlClient,
            brukertilgangService = brukertilgangService,
            brukGammelOppfolgingsplan = false,
        )
        every { pdlClient.person(fnr) } returns pdlPerson(kommune = "0301")

        val response = controller.getPerson(fnr)

        response.statusCode shouldBe HttpStatus.OK
        response.body?.pilotUser shouldBe true
    }

    test("getPerson returnerer pilotUser false nar gammel oppfolgingsplan er skrudd pa og bruker ikke er pilot") {
        val controller = personController(
            contextHolder = contextHolder,
            pdlClient = pdlClient,
            brukertilgangService = brukertilgangService,
            brukGammelOppfolgingsplan = true,
        )
        every { pdlClient.person(fnr) } returns pdlPerson(kommune = "0301")

        val response = controller.getPerson(fnr)

        response.statusCode shouldBe HttpStatus.OK
        response.body?.pilotUser shouldBe false
    }

    test("getPerson returnerer pilotUser true nar gammel oppfolgingsplan er skrudd pa og bruker er pilot") {
        val controller = personController(
            contextHolder = contextHolder,
            pdlClient = pdlClient,
            brukertilgangService = brukertilgangService,
            brukGammelOppfolgingsplan = true,
        )
        every { pdlClient.person(fnr) } returns pdlPerson(kommune = "4614")

        val response = controller.getPerson(fnr)

        response.statusCode shouldBe HttpStatus.OK
        response.body?.pilotUser shouldBe true
    }
})

private fun personController(
    contextHolder: TokenValidationContextHolder,
    pdlClient: PdlClient,
    brukertilgangService: BrukertilgangService,
    brukGammelOppfolgingsplan: Boolean,
) =
    PersonController(
        contextHolder = contextHolder,
        pdlClient = pdlClient,
        brukertilgangService = brukertilgangService,
        oppfolgingsplanClientId = "oppfolgingsplan-client-id",
        dialogmoteClientId = "dialogmote-client-id",
        dinesykmeldteClientId = "dinesykmeldte-client-id",
        dittSykefravaerClientId = "dittsykefravaer-client-id",
        brukGammelOppfolgingsplan = brukGammelOppfolgingsplan,
    )

private fun pdlPerson(kommune: String) = PdlHentPerson(
    hentPerson = PdlPerson(
        navn = listOf(
            PdlPersonNavn(
                fornavn = "OLA",
                mellomnavn = null,
                etternavn = "NORDMANN",
            )
        ),
        adressebeskyttelse = null,
    ),
    hentGeografiskTilknytning = GeografiskTilknytning(
        gtType = "KOMMUNE",
        gtLand = null,
        gtKommune = kommune,
        gtBydel = null,
    ),
)
