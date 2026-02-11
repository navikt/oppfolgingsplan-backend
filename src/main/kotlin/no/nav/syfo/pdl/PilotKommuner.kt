package no.nav.syfo.pdl

import org.slf4j.LoggerFactory

/**
 * Sjekker om en bruker er del av piloten for ny oppfølgingsplan.
 *
 * Piloten rulles ut til hele Vestland fylke (kommunenummer 46xx)
 * utenom Etne kommune (4611).
 */
object PilotKommuner {
    private val LOG = LoggerFactory.getLogger(PilotKommuner::class.java)

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
        if (geografiskTilknytning == null) {
            LOG.debug("GeografiskTilknytning is null, not pilot user")
            return false
        }

        val kommunenr = extractKommunenummer(geografiskTilknytning)
        val isPilot = kommunenr != null &&
            kommunenr.startsWith(VESTLAND_FYLKESKODE) &&
            kommunenr != ETNE_KOMMUNENUMMER

        if (kommunenr != null && kommunenr.startsWith("46")) {
            LOG.info(
                "Vestland user check: gtType={}, gtKommune={}, gtBydel={}, extracted={}, isPilot={}, isEtne={}",
                geografiskTilknytning.gtType,
                geografiskTilknytning.gtKommune,
                geografiskTilknytning.gtBydel,
                kommunenr,
                isPilot,
                kommunenr == ETNE_KOMMUNENUMMER
            )
        } else {
            LOG.debug(
                "Non-Vestland user: gtType={}, kommunenr={}, isPilot={}",
                geografiskTilknytning.gtType,
                kommunenr,
                isPilot
            )
        }

        return isPilot
    }

    private fun extractKommunenummer(gt: GeografiskTilknytning): String? {
        return when (gt.gtType) {
            "KOMMUNE" -> {
                gt.gtKommune
            }
            "BYDEL" -> {
                // Avvik case: gtKommune may be set even when gtType is BYDEL
                if (gt.gtKommune != null) {
                    LOG.debug("BYDEL with gtKommune set (avvik case): using gtKommune={}", gt.gtKommune)
                    gt.gtKommune
                } else if (gt.gtBydel != null && gt.gtBydel.length >= 4) {
                    // gtBydel format: 6 digits (kommunenummer + bydelsnummer), e.g., "460101"
                    val kommunenr = gt.gtBydel.take(4)
                    LOG.debug("BYDEL: extracted kommunenr={} from gtBydel={}", kommunenr, gt.gtBydel)
                    kommunenr
                } else {
                    LOG.warn("BYDEL type but no valid gtBydel or gtKommune: gtBydel={}", gt.gtBydel)
                    null
                }
            }
            "UTLAND", "UDEFINERT" -> {
                LOG.debug("gtType={}, not checking for pilot", gt.gtType)
                null
            }
            else -> {
                LOG.warn("Unexpected gtType={}, gtKommune={}, gtBydel={}", gt.gtType, gt.gtKommune, gt.gtBydel)
                null
            }
        }
    }
}
