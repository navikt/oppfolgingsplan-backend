package no.nav.syfo.pdl

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PdlHentPersonPilotUserTest {

    @Test
    fun `isPilotUser er true for pilotkommune`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "4614",
                gtBydel = null,
            ),
        )

        assertTrue(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er false for ikke-pilotkommune og null`() {
        val pdlIkkePilot = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "0301",
                gtBydel = null,
            ),
        )
        val pdlNull = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = null,
        )

        assertFalse(pdlIkkePilot.isPilotUser())
        assertFalse(pdlNull.isPilotUser())
    }
}
