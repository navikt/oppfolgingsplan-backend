package no.nav.syfo.virksomhet

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.AbstractRessursTilgangTest
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.domain.Virksomhet
import no.nav.syfo.ereg.EregConsumer
import no.nav.syfo.testhelper.UserConstants.LEDER_FNR
import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
import org.mockito.Mockito.`when`
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import javax.inject.Inject

class VirksomhetControllerTest : FunSpec({
    /* val eregConsumer = mockk<EregConsumer>()
    val mockTokenValidationContext = mockk<TokenValidationContext>()
    val contextHolder = mockk<TokenValidationContextHolder>()
    val oppfolgingsplanClientId = "123456789"
    val virksomhetController = VirksomhetController(
        contextHolder,
        eregConsumer,
        oppfolgingsplanClientId
    )

    val virksomhetsNavn = "Tull og fanteri AS"

    val virksomhet = Virksomhet(VIRKSOMHETSNUMMER, virksomhetsNavn)

    test("virksomhet ok") {
        every { eregConsumer.virksomhetsnavn(VIRKSOMHETSNUMMER) } returns virksomhetsNavn
        val res: ResponseEntity<*> = virksomhetController.getVirksomhet(VIRKSOMHETSNUMMER)
        val body = res.body as Virksomhet
        assertEquals(200, res.statusCode.value().toLong())
        assertEquals(virksomhet.virksomhetsnummer, body.virksomhetsnummer)
        assertEquals(virksomhet.navn, body.navn)
    }

    test("virksomhet invalid virksomhetsnummer") {
        every { eregConsumer.virksomhetsnavn(VIRKSOMHETSNUMMER) } returns virksomhetsNavn
        val res: ResponseEntity<*> = virksomhetController.getVirksomhet("12345678")
        assertNull(res.body)
        assertEquals(418, res.statusCodeValue.toLong())
    }*/
    val contextHolder = mockk<TokenValidationContextHolder>()
    val mockTokenValidationContext = mockk<TokenValidationContext>()
    val mockJwtTokenClaims = mockk<JwtTokenClaims>()
    val brukertilgangService = mockk<BrukertilgangService>()

    val eregConsumer = mockk<EregConsumer>()
    //val contextHolder = mockk<TokenValidationContextHolder>()
    val virksomhetsNavn = "Tull og fanteri AS"
    val virksomhet = Virksomhet(VIRKSOMHETSNUMMER, virksomhetsNavn)

    val virksomhetController = VirksomhetController(
        contextHolder = contextHolder,
        eregConsumer = eregConsumer,
        oppfolgingsplanClientId = "123456789"
    )

    beforeTest {
        every { contextHolder.getTokenValidationContext() } returns mockTokenValidationContext
        every { mockTokenValidationContext.getClaims("tokenx") } returns mockJwtTokenClaims
        //every { mockJwtTokenClaims.getStringClaim("pid") } returns validFnr
        every { mockJwtTokenClaims.getStringClaim("client_id") } returns "123456789"
        every { eregConsumer.virksomhetsnavn(VIRKSOMHETSNUMMER) } returns virksomhetsNavn
    }

    test("should return virksomhet with valid virksomhetsnummer") {
        val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet(VIRKSOMHETSNUMMER)
        val body = res.body

        res.statusCode shouldBe HttpStatus.OK
        body?.virksomhetsnummer shouldBe virksomhet.virksomhetsnummer
        body?.navn shouldBe virksomhet.navn
    }

    test("should return 418 status code with invalid virksomhetsnummer") {
        val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet("12345678")

        res.statusCode shouldBe HttpStatus.I_AM_A_TEAPOT
        res.body shouldBe null
    }
})
/*
class VirksomhetControllerTest : AbstractRessursTilgangTest() {
    import io.kotest.core.spec.style.FunSpec
    import io.kotest.matchers.shouldBe
    import io.mockk.every
    import io.mockk.mockk
    import no.nav.syfo.domain.Virksomhet
    import no.nav.syfo.ereg.EregConsumer
    import no.nav.syfo.testhelper.UserConstants.VIRKSOMHETSNUMMER
    import org.springframework.http.HttpStatus
    import org.springframework.http.ResponseEntity

    class VirksomhetControllerTest : FunSpec({

        val eregConsumer = mockk<EregConsumer>()
        val virksomhetsNavn = "Tull og fanteri AS"
        val virksomhet = Virksomhet(VIRKSOMHETSNUMMER, virksomhetsNavn)

        val virksomhetController = VirksomhetController(
            contextHolder = mockk(),
            eregConsumer = eregConsumer,
            oppfolgingsplanClientId = "123456789"
        )

        beforeTest {
            every { eregConsumer.virksomhetsnavn(VIRKSOMHETSNUMMER) } returns virksomhetsNavn
        }

        test("should return virksomhet with valid virksomhetsnummer") {
            val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet(VIRKSOMHETSNUMMER)
            val body = res.body

            res.statusCode shouldBe HttpStatus.OK
            body?.virksomhetsnummer shouldBe virksomhet.virksomhetsnummer
            body?.navn shouldBe virksomhet.navn
        }

        test("should return 418 status code with invalid virksomhetsnummer") {
            val res: ResponseEntity<Virksomhet> = virksomhetController.getVirksomhet("12345678")

            res.statusCode shouldBe HttpStatus.I_AM_A_TEAPOT
            res.body shouldBe null
        }
    })
    @MockBean
    lateinit var eregConsumer: EregConsumer

    @Inject
    private lateinit var virksomhetController: VirksomhetController

    private val virksomhetsNavn = "Tull og fanteri AS"

    private val virksomhet = Virksomhet(VIRKSOMHETSNUMMER, virksomhetsNavn)

    @Test
    fun virksomhet_ok() {
        tokenValidationTestUtil.logInAsUser(LEDER_FNR)
        `when`(eregConsumer.virksomhetsnavn(VIRKSOMHETSNUMMER))
            .thenReturn(virksomhetsNavn)
        val res: ResponseEntity<*> = virksomhetController.getVirksomhet(VIRKSOMHETSNUMMER)
        val body = res.body as Virksomhet
        assertEquals(200, res.statusCode.value().toLong())
        assertEquals(virksomhet.virksomhetsnummer, body.virksomhetsnummer)
        assertEquals(virksomhet.navn, body.navn)
    }

    @Test
    fun virksomhet_invalid_virksomhetsnummer() {
        tokenValidationTestUtil.logInAsUser(LEDER_FNR)
        `when`(eregConsumer.virksomhetsnavn(VIRKSOMHETSNUMMER))
            .thenReturn(virksomhetsNavn)
        val res: ResponseEntity<*> = virksomhetController.getVirksomhet("12345678")
        assertNull(res.body)
        assertEquals(418, res.statusCodeValue.toLong())
    }
}*/
