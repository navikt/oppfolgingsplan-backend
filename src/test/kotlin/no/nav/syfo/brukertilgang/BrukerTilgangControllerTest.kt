package no.nav.syfo.brukertilgang

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.metric.Metrikk
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BrukerTilgangControllerTest : FunSpec({
    val contextHolder = mockk<TokenValidationContextHolder>()
    val mockTokenValidationContext = mockk<TokenValidationContext>()
    val mockJwtTokenClaims = mockk<JwtTokenClaims>()
    val brukertilgangConsumer = mockk<BrukertilgangClient>()
    val brukertilgangService = mockk<BrukertilgangService>()
    val metrikk = mockk<Metrikk>(relaxed = true)
    val controller =
        BrukerTilgangController(contextHolder, brukertilgangConsumer, brukertilgangService, metrikk, "clientId")

    val validFnr = "12345678910"
    val invalidFnr = "123"

    beforeTest {
        every { contextHolder.getTokenValidationContext() } returns mockTokenValidationContext
        every { mockTokenValidationContext.getClaims(TokenXUtil.TokenXIssuer.TOKENX) } returns mockJwtTokenClaims
        every { mockJwtTokenClaims.getStringClaim("pid") } returns validFnr
        every { mockJwtTokenClaims.getStringClaim("client_id") } returns "clientId"
    }

    test("harTilgang returns no access if brukertilgang returns false") {
        every { brukertilgangService.tilgangTilOppslattIdent(any(), any()) } returns false
        shouldThrowExactly<ResponseStatusException> {
            controller.harTilgang(invalidFnr)
        }.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    test("harTilgang returns access if brukertilgang returns true") {
        every { brukertilgangService.tilgangTilOppslattIdent(any(), any()) } returns true
        val response = controller.harTilgang(validFnr)
        response.harTilgang shouldBe true
    }

    test("accessToAnsatt returns no access if brukertilgang returns false") {
        every { brukertilgangConsumer.hasAccessToAnsatt(any()) } returns false
        val response = controller.accessToAnsatt(invalidFnr)
        response.tilgang shouldBe false
    }

    test("accessToAnsatt returns access if brukertilgang returns true") {
        every { brukertilgangConsumer.hasAccessToAnsatt(any()) } returns true
        val response = controller.accessToAnsatt(validFnr)
        response.tilgang shouldBe true
    }
})
