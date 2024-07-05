package no.nav.syfo.aareg.service

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.aareg.*
import no.nav.syfo.aareg.AaregConsumer
import no.nav.syfo.aareg.AaregUtils
import no.nav.syfo.aareg.Arbeidsforhold
import no.nav.syfo.aareg.utils.AaregConsumerTestUtils.*
import no.nav.syfo.aareg.utils.AaregConsumerTestUtils.YRKESKODE
import no.nav.syfo.aareg.utils.AaregConsumerTestUtils.YRKESNAVN
import no.nav.syfo.fellesKodeverk.*
import no.nav.syfo.felleskodeverk.FellesKodeverkClient
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.model.Stilling
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.sts.StsClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import java.time.LocalDate.now

const val AAREG_URL = "http://localhost:9000"
//@RunWith(MockitoJUnitRunner::class)
class ArbeidsforholdServiceTest : FunSpec({

    val metrikk = mockk<Metrikk>(relaxed = true)
    // TODO check if still needed after applying TokenX/AzureAD
    val stsClient = mockk<StsClient>()
    val aaregConsumer = AaregClient(metrikk, stsClient, AAREG_URL)

    val fellesKodeverkConsumer = mockk<FellesKodeverkClient>()

    val pdlConsumer =mockk<PdlClient>()

    val arbeidsforholdService= mockk<ArbeidsforholdService>()

    beforeTest {
        every { fellesKodeverkConsumer.kodeverkKoderBetydninger() }
    }
    @Before
    fun setup() {
        `when`(fellesKodeverkConsumer.kodeverkKoderBetydninger()).thenReturn(fellesKodeverkResponseBody(YRKESNAVN, YRKESKODE))
    }

    @Test
    fun arbeidstakersStillingerForOrgnummerShouldReturnCorrectYrke() {
        val arbeidsforholdList = listOf(validArbeidsforhold())

        test_arbeidstakersStillingerForOrgnummer(arbeidsforholdList)
    }

    @Test
    fun arbeidstakersStillingerForOrgnummerShouldReturnCustomMessageIfNavnNotFound() {
        val arbeidsforholdList = listOf(validArbeidsforhold())
        `when`(fellesKodeverkConsumer.kodeverkKoderBetydninger()).thenReturn(fellesKodeverkResponseBodyWithWrongKode())
        `when`(aaregConsumer.arbeidsforholdArbeidstaker(AT_FNR)).thenReturn(arbeidsforholdList)
        `when`(pdlConsumer.fnr(AT_AKTORID)).thenReturn(AT_FNR)
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)

        val stilling = actualStillingList[0]
        assertThat(stilling.yrke).isEqualTo("Ugyldig yrkeskode $STILLINGSKODE")
    }

    @Test
    fun arbeidstakersStillingerForOrgnummerShouldOnlyReturnStillingerWithTypeOrganization() {
        val arbeidsforholdList = listOf(validArbeidsforhold(), arbeidsforholdTypePerson())

        test_arbeidstakersStillingerForOrgnummer(arbeidsforholdList)
    }

    @Test
    fun arbeidstakersStillingerForOrgnummerShouldOnlyReturnStillingerValidOnDate() {
        val arbeidsforholdList = listOf(validArbeidsforhold(), arbeidsforholdWithPassedDate())

        test_arbeidstakersStillingerForOrgnummer(arbeidsforholdList)
    }

    @Test
    fun arbeidstakersStillingerForOrgnummerShouldOnlyReturnStillingerWithOrgnummer() {
        val arbeidsforholdList = listOf(validArbeidsforhold(), arbeidsforholdWithWrongOrgnummer())

        test_arbeidstakersStillingerForOrgnummer(arbeidsforholdList)
    }

    @Test
    fun arbeidstakersStillingerForOrgnummerShouldReturnEmptyListWhenNoValidArbeidsforhold() {
        val arbeidsforholdList = listOf(
            arbeidsforholdTypePerson(),
            arbeidsforholdWithPassedDate(),
            arbeidsforholdWithWrongOrgnummer()
        )

        `when`(aaregConsumer.arbeidsforholdArbeidstaker(AT_FNR)).thenReturn(arbeidsforholdList)
        `when`(pdlConsumer.fnr(AT_AKTORID)).thenReturn(AT_FNR)

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)

        assertThat(actualStillingList).isEmpty()
    }

    @Test
    fun sholdMapArbeidsforholdWithOnlyOneArbeidsavtale() {
        val startDate = now().minusYears(1)
        val arbeidsforholdList = listOf(
            validArbeidsforhold().apply {
                ansettelsesperiode = ansettelsesperiode(startDate, null)
                arbeidsavtaler =
                    listOf(
                        Arbeidsavtale()
                            .yrke(YRKESKODE)
                            .stillingsprosent(STILLINGSPROSENT)
                            .gyldighetsperiode(
                                Gyldighetsperiode()
                                    .fom(startDate.withDayOfMonth(1).toString())
                            )
                    )
            }
        )
        `when`(aaregConsumer.arbeidsforholdArbeidstaker(AT_FNR)).thenReturn(arbeidsforholdList)

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_FNR, listOf(ORGNUMMER))

        assertThat(actualStillingList).isNotEmpty
        val stilling1 = actualStillingList[0]
        assertThat(stilling1.yrke).isEqualTo(YRKESNAVN_CAPITALIZED)
        assertThat(stilling1.prosent).isEqualTo(AaregUtils.stillingsprosentWithMaxScale(STILLINGSPROSENT))
        assertThat(stilling1.fom).isEqualTo(startDate)
        assertThat(stilling1.tom).isNull()
    }

    @Test
    fun sholdMapArbeidsforholdWithOnlyAvsluttetArbeidsavtale() {
        val startDate = now().minusYears(1)
        val stopDate = now().minusDays(1)
        val arbeidsforholdList = listOf(
            validArbeidsforhold().apply {
                ansettelsesperiode = ansettelsesperiode(startDate, stopDate)
                arbeidsavtaler =
                    listOf(
                        Arbeidsavtale()
                            .yrke(YRKESKODE)
                            .stillingsprosent(STILLINGSPROSENT)
                            .gyldighetsperiode(
                                Gyldighetsperiode()
                                    .fom(startDate.withDayOfMonth(1).toString())
                            )
                    )
            }
        )
        `when`(aaregConsumer.arbeidsforholdArbeidstaker(AT_FNR)).thenReturn(arbeidsforholdList)

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_FNR, listOf(ORGNUMMER))

        assertThat(actualStillingList).isNotEmpty
        val stilling1 = actualStillingList[0]
        assertThat(stilling1.yrke).isEqualTo(YRKESNAVN_CAPITALIZED)
        assertThat(stilling1.prosent).isEqualTo(AaregUtils.stillingsprosentWithMaxScale(STILLINGSPROSENT))
        assertThat(stilling1.fom).isEqualTo(startDate)
        assertThat(stilling1.tom).isEqualTo(stopDate)
    }

    @Test
    fun sholdMapArbeidsforholdWithTwoArbeidsavtaler() {
        val startDate = now().minusYears(1)
        val stilling1StopDate = now().minusMonths(1).withDayOfMonth(1).minusDays(1)
        val stilling2StartDate = now().minusMonths(1).withDayOfMonth(1)
        val stilling2Stillingsprosent = 80.0
        val arbeidsforholdList = listOf(
            validArbeidsforhold().apply {
                ansettelsesperiode = ansettelsesperiode(startDate, null)
                arbeidsavtaler =
                    listOf(
                        Arbeidsavtale()
                            .yrke(YRKESKODE)
                            .stillingsprosent(STILLINGSPROSENT)
                            .gyldighetsperiode(
                                Gyldighetsperiode()
                                    .fom(startDate.withDayOfMonth(1).toString())
                                    .tom(stilling1StopDate.toString())
                            ),
                        Arbeidsavtale()
                            .yrke("123")
                            .stillingsprosent(stilling2Stillingsprosent)
                            .gyldighetsperiode(
                                Gyldighetsperiode()
                                    .fom(stilling2StartDate.toString())
                            )
                    )
            }
        )
        `when`(aaregConsumer.arbeidsforholdArbeidstaker(AT_FNR)).thenReturn(arbeidsforholdList)

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_FNR, listOf(ORGNUMMER))

        assertThat(actualStillingList).isNotEmpty

        val stilling1 = actualStillingList[0]
        assertThat(stilling1.yrke).isEqualTo(YRKESNAVN_CAPITALIZED)
        assertThat(stilling1.prosent).isEqualTo(AaregUtils.stillingsprosentWithMaxScale(STILLINGSPROSENT))
        assertThat(stilling1.fom).isEqualTo(startDate)
        assertThat(stilling1.tom).isEqualTo(stilling1StopDate)

        val stilling2 = actualStillingList[1]
        assertThat(stilling2.yrke).isEqualTo("Ugyldig yrkeskode 123")
        assertThat(stilling2.prosent).isEqualTo(AaregUtils.stillingsprosentWithMaxScale(stilling2Stillingsprosent))
        assertThat(stilling2.fom).isEqualTo(stilling2StartDate)
        assertThat(stilling2.tom).isNull()
    }

    private fun test_arbeidstakersStillingerForOrgnummer(arbeidsforholdList: List<Arbeidsforhold>) {
        `when`(aaregConsumer.arbeidsforholdArbeidstaker(AT_FNR)).thenReturn(arbeidsforholdList)
        `when`(pdlConsumer.fnr(AT_AKTORID)).thenReturn(AT_FNR)
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)
        verifyStilling(actualStillingList)
    }

    private fun verifyStilling(stillingList: List<Stilling>) {
        assertThat(stillingList.size).isEqualTo(1)
        val stilling = stillingList[0]
        assertThat(stilling.yrke).isEqualTo(YRKESNAVN_CAPITALIZED)
        assertThat(stilling.prosent).isEqualTo(AaregUtils.stillingsprosentWithMaxScale(STILLINGSPROSENT))
    }

    private fun ansettelsesperiode(fom: LocalDate?, tom: LocalDate?): Ansettelsesperiode {
        return Ansettelsesperiode()
            .periode(
                Periode()
                    .fom(fom?.toString())
                    .tom(tom?.toString())
            )
    }
})

