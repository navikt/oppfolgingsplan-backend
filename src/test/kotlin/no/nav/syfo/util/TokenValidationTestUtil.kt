package no.nav.syfo.util


import com.nimbusds.jwt.SignedJWT
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.syfo.auth.oidc.OIDCIssuer
import no.nav.syfo.auth.tokenx.TokenXUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TokenValidationTestUtil(
    @Value("\${oppfolgingsplan.frontend.client.id}") val clientId: String
) {
    @Autowired
    private lateinit var contextHolder: TokenValidationContextHolder

    @Autowired
    private lateinit var mockOAuthServer: MockOAuth2Server

    fun logInAsUser(
        userFnr: String
    ) {
        val signedJwtToken = mockOAuthServer.issueToken(
            issuerId = "tokenx-client-id",
            subject = "oppfolgingsplan-frontend",
            audience = "syfooppfolgingsplanservice",
            claims = mapOf(
                "client_id" to clientId,
                "acr" to "Level4",
                "pid" to userFnr
            ),
            expiry = 60L
        )
        setTokenInValidationContext(signedJwtToken, TokenXUtil.TokenXIssuer.TOKENX)
    }

    fun logInAsNavCounselor(
        username: String
    ) {
        val signedJwtToken = mockOAuthServer.issueToken(
            issuerId = "azuread-v2-issuer",
            subject = "modiasyfoperson",
            audience = "syfooppfolgingsplanservice",
            claims = mapOf(
                "NAVident" to username
            ),
            expiry = 60L
        )
        setTokenInValidationContext(signedJwtToken, OIDCIssuer.INTERN_AZUREAD_V2)
    }

    fun logout() {
        contextHolder.setTokenValidationContext(null)
    }

    private fun setTokenInValidationContext(signedJwtToken: SignedJWT, issuer: String) {
        contextHolder.setTokenValidationContext(
            TokenValidationContext(
                mutableMapOf(issuer to JwtToken(signedJwtToken.serialize()))
            )
        )
    }
}