package no.nav.syfo.aareg.service

import no.nav.syfo.aareg.AaregClient
import no.nav.syfo.aareg.AaregUtils.stillingsprosentWithMaxScale
import no.nav.syfo.aareg.Arbeidsavtale
import no.nav.syfo.aareg.Arbeidsforhold
import no.nav.syfo.aareg.OpplysningspliktigArbeidsgiver
import no.nav.syfo.aareg.model.Stilling
import no.nav.syfo.felleskodeverk.FellesKodeverkClient
import no.nav.syfo.felleskodeverk.KodeverkKoderBetydningerResponse
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.util.lowerCapitalize
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.ParseException
import java.time.LocalDate

@Service
class ArbeidsforholdService(
    private val aaregClient: AaregClient,
    private val fellesKodeverkClient: FellesKodeverkClient,
    private val pdlClient: PdlClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    fun arbeidstakersStillingerForOrgnummer(aktorId: String, fom: LocalDate, orgnummer: String): List<Stilling> {
        val fnr: String = pdlClient.fnr(aktorId)
        val arbeidsforholdList: List<Arbeidsforhold> = aaregClient.arbeidsforholdArbeidstaker(fnr)
        log.info("Arbeidsforhold for arbeidstaker: $arbeidsforholdList")
        return arbeidsforholdList2StillingForOrgnummer(arbeidsforholdList, orgnummer, fom)
    }

    fun arbeidstakersStillingerForOrgnummer(fnr: String, orgnummerList: List<String>): List<Stilling> {
        return arbeidstakersStillinger(fnr)
            .filter { stilling -> orgnummerList.contains(stilling.orgnummer) }
    }

    fun arbeidstakersStillinger(fnr: String): List<Stilling> {
        val kodeverkBetydninger = fellesKodeverkClient.kodeverkKoderBetydninger()
        return try {
            aaregClient.arbeidsforholdArbeidstaker(fnr)
                .filter { arbeidsforhold ->
                    arbeidsforhold.arbeidsgiver!!.type.equals(OpplysningspliktigArbeidsgiver.Type.Organisasjon)
                }
                .flatMap { arbeidsforhold ->
                    arbeidsforhold.arbeidsavtaler!!
                        .sortedWith(compareBy<Arbeidsavtale, String?>(nullsLast()) { it.gyldighetsperiode!!.fom })
                        .map {
                            Stilling(
                                yrke = stillingsnavnFromKode(it.yrke, kodeverkBetydninger),
                                prosent = stillingsprosentWithMaxScale(it.stillingsprosent),
                                fom = beregnRiktigFom(
                                    it.gyldighetsperiode!!.fom,
                                    arbeidsforhold.ansettelsesperiode!!.periode.fom
                                ),
                                tom = beregnRiktigTom(
                                    it.gyldighetsperiode!!.tom,
                                    arbeidsforhold.ansettelsesperiode!!.periode.tom
                                ),
                                orgnummer = arbeidsforhold.arbeidsgiver!!.organisasjonsnummer
                            )
                        }
                }
        } catch (e: ParseException) {
            log.error("Failed to get arbeidsforhold for arbeidstaker", e)
            emptyList()
        }
    }

    fun beregnRiktigFom(gyldighetsperiodeFom: String?, ansettelsesperiodeFom: String): LocalDate {
        /* Gyldighetsperiode sier noe om hvilken måned arbeidsavtalen er rapportert inn,
         og starter på den 1. i måneden selv om arbeidsforholdet startet senere.
         Så dersom gyldighetsperiode er før ansettelsesperioden er det riktig å bruke ansettelsesperioden sin fom.*/
        val ansattFom = ansettelsesperiodeFom.tilLocalDate()
        return if (gyldighetsperiodeFom == null || LocalDate.parse(gyldighetsperiodeFom).isBefore(ansattFom)) {
            ansattFom
        } else {
            gyldighetsperiodeFom.tilLocalDate()
        }
    }

    fun beregnRiktigTom(gyldighetsperiodeTom: String?, ansettelsesperiodeTom: String?): LocalDate? {
        /* Den siste arbeidsavtalen har alltid tom = null, selv om arbeidsforholdet er avsluttet.
         Så dersom tom = null og ansettelsesperiodens tom ikke er null,
         er det riktig å bruke ansettelsesperioden sin tom */
        return if (gyldighetsperiodeTom != null) {
            gyldighetsperiodeTom.tilLocalDate()
        } else if (ansettelsesperiodeTom != null) {
            ansettelsesperiodeTom.tilLocalDate()
        } else {
            null
        }
    }

    private fun arbeidsforholdList2StillingForOrgnummer(
        arbeidsforholdListe: List<Arbeidsforhold>,
        orgnummer: String,
        fom: LocalDate
    ): List<Stilling> {
        val kodeverkBetydninger = fellesKodeverkClient.kodeverkKoderBetydninger()
        return arbeidsforholdListe
            .filter { arbeidsforhold ->
                arbeidsforhold.arbeidsgiver!!.type.equals(OpplysningspliktigArbeidsgiver.Type.Organisasjon)
            }
            .filter { arbeidsforhold -> arbeidsforhold.arbeidsgiver!!.organisasjonsnummer.equals(orgnummer) }
            .filter { arbeidsforhold ->
                arbeidsforhold.ansettelsesperiode!!.periode.tom == null ||
                    !arbeidsforhold.ansettelsesperiode!!.periode.tom.tilLocalDate()
                        .isBefore(fom)
            }
            .flatMap { arbeidsforhold ->
                arbeidsforhold.arbeidsavtaler!!
            }
            .map { arbeidsavtale ->
                Stilling(
                    yrke = stillingsnavnFromKode(arbeidsavtale.yrke, kodeverkBetydninger),
                    prosent = stillingsprosentWithMaxScale(arbeidsavtale.stillingsprosent)
                )
            }
    }

    private fun stillingsnavnFromKode(
        stillingskode: String,
        kodeverkBetydninger: KodeverkKoderBetydningerResponse
    ): String {
        val stillingsnavnFraFellesKodeverk =
            kodeverkBetydninger.betydninger?.get(stillingskode)?.get(0)?.beskrivelser?.get("nb")?.tekst
        if (stillingsnavnFraFellesKodeverk == null) {
            log.info("Couldn't find navn for stillingskode: $stillingskode")
        }
        val stillingsnavn = stillingsnavnFraFellesKodeverk ?: "Ugyldig yrkeskode $stillingskode"
        return stillingsnavn.lowerCapitalize()
    }

    private fun String.tilLocalDate(): LocalDate {
        return LocalDate.parse(this)
    }
}
