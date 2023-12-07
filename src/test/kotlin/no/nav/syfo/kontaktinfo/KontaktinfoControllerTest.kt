package no.nav.syfo.kontaktinfo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.brukertilgang.BrukertilgangService
import org.springframework.http.HttpStatus

class KontaktinfoControllerTest : FunSpec({
    val contextHolder = mockk<TokenValidationContextHolder>()
    val mockTokenValidationContext = mockk<TokenValidationContext>()
    val mockJwtTokenClaims = mockk<JwtTokenClaims>()
    val brukertilgangService = mockk<BrukertilgangService>()
    val krrClient = mockk<KrrClient>()
    val controller = KontaktinfoController(contextHolder, brukertilgangService, krrClient, "clientId")

    val validFnr = "12345678910"
    val invalidFnr = "123"

    beforeTest {
        every { contextHolder.tokenValidationContext } returns mockTokenValidationContext
        every { mockTokenValidationContext.getClaims(TOKENX) } returns mockJwtTokenClaims
        every { mockJwtTokenClaims.getStringClaim("pid") } returns validFnr
        every { mockJwtTokenClaims.getStringClaim("client_id") } returns "clientId"
    }

    test("Invalid fnr returns forbidden") {
        every { brukertilgangService.tilgangTilOppslattIdent(any(), any()) } returns true
        val response = controller.getKontaktinfo(invalidFnr)
        response.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    test("No access from KRR returns forbidden") {
        every { brukertilgangService.tilgangTilOppslattIdent(any(), any()) } returns false
        val response = controller.getKontaktinfo(validFnr)
        response.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    test("Valid fnr and access from KRR returns kontaktinfo") {
        every { brukertilgangService.tilgangTilOppslattIdent(any(), any()) } returns true
        val krrResponse = DigitalKontaktinfo(
            kanVarsles = true,
            reservert = false,
            epostadresse = "test@nav.no",
            mobiltelefonnummer = "12345678"
        )
        every { krrClient.kontaktinformasjon(validFnr) } returns krrResponse
        val controllerResponse = controller.getKontaktinfo(validFnr)
        controllerResponse.statusCode shouldBe HttpStatus.OK
        controllerResponse.body shouldBe krrResponse.toKontaktinfo(validFnr)
    }
})
