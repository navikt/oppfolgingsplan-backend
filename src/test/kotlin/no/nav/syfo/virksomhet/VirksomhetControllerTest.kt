package no.nav.syfo.virksomhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.domain.Virksomhet
import no.nav.syfo.ereg.EregClient
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus

class VirksomhetControllerTest : FunSpec({

    val contextHolder = mockk<TokenValidationContextHolder>()
    val mockTokenValidationContext = mockk<TokenValidationContext>()
    val mockJwtTokenClaims = mockk<JwtTokenClaims>()

    val eregClient = mockk<EregClient>()
    val virksomhetsNavn = "Tull og fanteri AS"
    val virksomhet = Virksomhet(VIRKSOMHETSNUMMER, virksomhetsNavn)

    val virksomhetController = VirksomhetController(
        contextHolder = contextHolder,
        eregClient = eregClient,
        oppfolgingsplanClientId = "123456789"
    )

    beforeTest {
        every { contextHolder.getTokenValidationContext() } returns mockTokenValidationContext
        every { mockTokenValidationContext.getClaims("tokenx") } returns mockJwtTokenClaims
        every { mockJwtTokenClaims.getStringClaim("client_id") } returns "123456789"
        every { eregClient.virksomhetsnavn(VIRKSOMHETSNUMMER) } returns virksomhetsNavn
    }

    test("should return virksomhet with valid virksomhetsnummer") {
        val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet(VIRKSOMHETSNUMMER)
        val body = res.body

        res.statusCode shouldBe HttpStatus.OK
        body?.virksomhetsnummer shouldBe virksomhet.virksomhetsnummer
        body?.navn shouldBe virksomhet.navn
    }

    test("should return 400 status code with invalid virksomhetsnummer") {
        val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet("12345678")

        res.statusCode shouldBe HttpStatus.BAD_REQUEST
        res.body shouldBe null
    }

    test("should return 400 status code with invalid contains numeric virksomhetsnummer") {
        val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet("a12345678")

        res.statusCode shouldBe HttpStatus.BAD_REQUEST
        res.body shouldBe null
    }
})
