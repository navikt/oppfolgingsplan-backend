package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.v2.domain.sykmelding.SykmeldingV2
import no.nav.syfo.api.v2.domain.sykmelding.toSykmeldingV2
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.model.Sykmelding
import no.nav.syfo.oidc.TokenUtil.getIssuerToken
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.sykmeldinger.ArbeidstakerSykmeldingerConsumer
import no.nav.syfo.tokenx.TokenXUtil
import no.nav.syfo.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.tokenx.tokendings.TokenDingsConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v2/arbeidstaker/sykmeldinger"])
class ArbeidstakerSykmeldingerControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val metrikk: Metrikk,
    private val pdlConsumer: PdlConsumer,
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

        val oppslattIdentAktorId = pdlConsumer.aktorid(innloggetIdent)
        val isTodayPresent = today.toBoolean()
        val sendteSykmeldinger = arbeidstakerSykmeldingerConsumer.getSendteSykmeldinger(oppslattIdentAktorId, bearerToken, isTodayPresent)
            .map { sykmeldinger: List<Sykmelding> -> sykmeldinger.map { it.toSykmeldingV2() } }
            .orElseGet { emptyList() }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(sendteSykmeldinger)
    }
}
