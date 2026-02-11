package no.nav.syfo.pdl

/**
 * Sjekker om en bruker er del av piloten for ny oppfølgingsplan.
 *
 * Piloten rulles ut til hele Vestland fylke (kommunenummer 46xx)
 * utenom Etne kommune (4611).
 */
object PilotKommuner {
    private const val VESTLAND_FYLKESKODE = "46"
    private const val ETNE_KOMMUNENUMMER = "4611"

    /**
     * Extracts kommunenummer from GeografiskTilknytning and checks if it's a pilot kommune.
     *
     * Pilot criteria: Vestland fylke (kommunenummer starting with "46") except Etne (4611)
     *
     * Handles different gtType values:
     * - KOMMUNE: uses gtKommune directly
     * - BYDEL: extracts first 4 digits from gtBydel (kommunenummer + bydelsnummer format)
     * - UTLAND/UDEFINERT: returns false
     *
     * Note: Bergen, Oslo, Stavanger, and Trondheim typically have gtType = BYDEL
     */
    fun erPilot(geografiskTilknytning: GeografiskTilknytning?): Boolean {
        if (geografiskTilknytning == null) return false

        val kommunenr = extractKommunenummer(geografiskTilknytning)
        return kommunenr != null &&
            kommunenr.startsWith(VESTLAND_FYLKESKODE) &&
            kommunenr != ETNE_KOMMUNENUMMER
    }

    private fun extractKommunenummer(gt: GeografiskTilknytning): String? {
        return when (gt.gtType) {
            "KOMMUNE" -> gt.gtKommune
            "BYDEL" -> {
                if (!gt.gtKommune.isNullOrBlank()) {
                    gt.gtKommune
                } else if (gt.gtBydel != null && gt.gtBydel.length >= 4) {
                    // gtBydel format: 6 digits (kommunenummer + bydelsnummer), e.g., "460101"
                    gt.gtBydel.take(4)
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
