package no.nav.syfo.sts

import java.time.LocalDateTime

data class StsToken(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int
) {
    // Expire 10 seconds before token expiration

    val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 10L)

    companion object {
        fun shouldRenew(token: StsToken?): Boolean {
            if (token == null) {
                return true
            }

            return isExpired(token)
        }

        private fun isExpired(token: StsToken): Boolean {
            return token.expirationTime.isBefore(LocalDateTime.now())
        }
    }
}
