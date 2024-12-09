package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.metric.Metrikk
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.inject.Inject
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.tokendings.TokenDingsConsumer
import no.nav.syfo.pdl.PdlClient

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/arbeidstaker/sykmeldinger"])
class ArbeidstakerSykmeldingerControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metrikk: Metrikk,
    private val pdlClient: PdlClient,
    private val arbeidstakerSykmeldingerConsumer: ArbeidstakerSykmeldingerConsumer,
    private val tokenDingsConsumer: TokenDingsConsumer,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
    @Value("\${syfosmregister.id}")
    private val targetApp: String? = null,
) {
    @ResponseBody
    @GetMapping
    fun getSendteSykmeldinger(@RequestParam(required = false) today: String?): ResponseEntity<List<SykmeldingV2>> {
        metrikk.tellHendelse("get_sykmeldinger")
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val issuerToken = getIssuerToken(contextHolder, TOKENX)
        val exchangedToken = tokenDingsConsumer.exchangeToken(issuerToken, targetApp!!)
        val bearerToken = "Bearer $exchangedToken"

        val oppslattIdentAktorId = pdlClient.aktorid(innloggetIdent)
        val isTodayPresent = today.toBoolean()
        val sendteSykmeldinger = arbeidstakerSykmeldingerConsumer.getSendteSykmeldinger(oppslattIdentAktorId, bearerToken, isTodayPresent)
            .map { sykmeldinger: List<Sykmelding> -> sykmeldinger.map { it.toSykmeldingV2() } }
            .orElseGet { emptyList() }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(sendteSykmeldinger)
    }
}
