package no.nav.syfo.fellesKodeverk

import no.nav.syfo.felleskodeverk.Beskrivelse
import no.nav.syfo.felleskodeverk.Betydning
import no.nav.syfo.felleskodeverk.KodeverkKoderBetydningerResponse
import java.util.Date

fun fellesKodeverkResponseBody(stillingsnavn: String, stillingskode: String): KodeverkKoderBetydningerResponse {
    val beskrivelse = Beskrivelse(stillingsnavn, stillingsnavn)
    val beskrivelser: MutableMap<String, Beskrivelse> = HashMap()
    beskrivelser[SPRAK] = beskrivelse
    val betydning = Betydning(beskrivelser, Date().toString(), Date().toString())
    val betydninger: MutableMap<String, List<Betydning>> = HashMap()
    betydninger[stillingskode] = listOf(betydning)
    return KodeverkKoderBetydningerResponse(betydninger)
}

fun fellesKodeverkResponseBodyWithWrongKode(): KodeverkKoderBetydningerResponse {
    val beskrivelse = Beskrivelse(WRONG_STILLINGSNAVN, WRONG_STILLINGSNAVN)
    val beskrivelser: MutableMap<String, Beskrivelse> = HashMap()
    beskrivelser[SPRAK] = beskrivelse
    val betydning = Betydning(beskrivelser, Date().toString(), Date().toString())
    val betydninger: MutableMap<String, List<Betydning>> = HashMap()
    betydninger[WRONG_STILLINGSKODE] = listOf(betydning)
    return KodeverkKoderBetydningerResponse(betydninger)
}

const val STILLINGSNAVN = "Special Agent"
const val WRONG_STILLINGSNAVN = "Deputy Director"
const val STILLINGSKODE = "1234567"
const val WRONG_STILLINGSKODE = "9876543"
const val SPRAK = "nb"
