package no.nav.syfo.auth.tokenx

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.domain.Fodselsnummer
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

object TokenXUtil {
    @Throws(ResponseStatusException::class)
    fun validateTokenXClaims(
        contextHolder: TokenValidationContextHolder,
        vararg requestedClientId: String,
    ): JwtTokenClaims {
        val context = contextHolder.tokenValidationContext
        val claims = context.getClaims(TokenXIssuer.TOKENX)
        val clientId = claims.getStringClaim("client_id")

        if (!requestedClientId.toList().contains(clientId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Uventet client id $clientId")
        }
        return claims
    }

    fun JwtTokenClaims.fnrFromIdportenTokenX(): Fodselsnummer {
        return Fodselsnummer(this.getStringClaim("pid"))
    }

    fun fnrFromIdportenTokenX(contextHolder: TokenValidationContextHolder): Fodselsnummer {
        val context = contextHolder.tokenValidationContext
        val claims = context.getClaims(TokenXIssuer.TOKENX)
        return Fodselsnummer(claims.getStringClaim("pid"))
    }

    object TokenXIssuer {
        const val TOKENX = "tokenx"
    }
}
