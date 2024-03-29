package no.nav.syfo.auth.oidc

import no.nav.security.token.support.core.context.TokenValidationContextHolder

object TokenUtil {

    @JvmStatic
    fun getIssuerToken(contextHolder: TokenValidationContextHolder, issuer: String): String {
        val context = contextHolder.getTokenValidationContext()
        return context.getJwtToken(issuer)?.encodedToken
            ?: throw TokenValidationException("Klarte ikke hente token fra issuer: $issuer")
    }

    class TokenValidationException(message: String) : RuntimeException(message)
}
