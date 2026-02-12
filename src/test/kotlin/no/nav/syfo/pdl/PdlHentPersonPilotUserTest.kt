package no.nav.syfo.pdl

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PdlHentPersonPilotUserTest {

    @Test
    fun `isPilotUser er true for Vestland kommune med gtType KOMMUNE`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "4614", // Stord
                gtBydel = null,
            ),
        )

        assertTrue(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er true for Bergen med gtType BYDEL`() {
        val pdlBergen1 = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "BYDEL",
                gtLand = null,
                gtKommune = null,
                gtBydel = "460101", // Bergen kommune + bydel
            ),
        )
        val pdlBergen2 = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "BYDEL",
                gtLand = null,
                gtKommune = null,
                gtBydel = "460199", // Bergen kommune + annen bydel
            ),
        )

        assertTrue(pdlBergen1.isPilotUser())
        assertTrue(pdlBergen2.isPilotUser())
    }

    @Test
    fun `isPilotUser er false for Etne kommune`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "4611", // Etne - excluded from pilot
                gtBydel = null,
            ),
        )

        assertFalse(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er true for avvik case med BYDEL men gtKommune satt`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "BYDEL",
                gtLand = null,
                gtKommune = "4601", // Avvik: gtKommune satt selv om gtType er BYDEL
                gtBydel = null,
            ),
        )

        assertTrue(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er false for Oslo med gtType BYDEL (ikke Vestland)`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "BYDEL",
                gtLand = null,
                gtKommune = null,
                gtBydel = "030101", // Oslo (ikke Vestland)
            ),
        )

        assertFalse(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er false for ikke-Vestland kommune og null`() {
        val pdlIkkePilot = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "0301", // Oslo
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

    @Test
    fun `isPilotUser er false for gtType UTLAND`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "UTLAND",
                gtLand = "SWE",
                gtKommune = null,
                gtBydel = null,
            ),
        )

        assertFalse(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er false for gtType UDEFINERT`() {
        val pdl = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "UDEFINERT",
                gtLand = null,
                gtKommune = null,
                gtBydel = null,
            ),
        )

        assertFalse(pdl.isPilotUser())
    }

    @Test
    fun `isPilotUser er true for alle Vestland kommuner utenom Etne`() {
        // Test a few more Vestland municipalities
        val sogndal = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "4640", // Sogndal
                gtBydel = null,
            ),
        )
        val kinn = PdlHentPerson(
            hentPerson = null,
            hentGeografiskTilknytning = GeografiskTilknytning(
                gtType = "KOMMUNE",
                gtLand = null,
                gtKommune = "4602", // Kinn
                gtBydel = null,
            ),
        )

        assertTrue(sogndal.isPilotUser())
        assertTrue(kinn.isPilotUser())
    }
}
