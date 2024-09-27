package no.nav.syfo.aareg.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainNoNulls
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.aareg.AaregClient
import no.nav.syfo.aareg.Ansettelsesperiode
import no.nav.syfo.aareg.Arbeidsavtale
import no.nav.syfo.aareg.Gyldighetsperiode
import no.nav.syfo.aareg.Periode
import no.nav.syfo.aareg.model.Stilling
import no.nav.syfo.aareg.utils.AaregClientTestUtils.AT_AKTORID
import no.nav.syfo.aareg.utils.AaregClientTestUtils.AT_FNR
import no.nav.syfo.aareg.utils.AaregClientTestUtils.ORGNUMMER
import no.nav.syfo.aareg.utils.AaregClientTestUtils.STILLINGSPROSENT
import no.nav.syfo.aareg.utils.AaregClientTestUtils.YRKESKODE
import no.nav.syfo.aareg.utils.AaregClientTestUtils.YRKESNAVN
import no.nav.syfo.aareg.utils.AaregClientTestUtils.YRKESNAVN_CAPITALIZED
import no.nav.syfo.aareg.utils.AaregClientTestUtils.arbeidsforholdTypePerson
import no.nav.syfo.aareg.utils.AaregClientTestUtils.arbeidsforholdWithPassedDate
import no.nav.syfo.aareg.utils.AaregClientTestUtils.arbeidsforholdWithWrongOrgnummer
import no.nav.syfo.aareg.utils.AaregClientTestUtils.validArbeidsforhold
import no.nav.syfo.fellesKodeverk.STILLINGSKODE
import no.nav.syfo.fellesKodeverk.fellesKodeverkResponseBody
import no.nav.syfo.fellesKodeverk.fellesKodeverkResponseBodyWithWrongKode
import no.nav.syfo.felleskodeverk.FellesKodeverkClient
import no.nav.syfo.pdl.PdlClient
import java.time.LocalDate
import java.time.LocalDate.now

class ArbeidsforholdServiceTest : DescribeSpec({

    var aaregClient = mockk<AaregClient>()

    val fellesKodeverkConsumer = mockk<FellesKodeverkClient>()

    var pdlClient = mockk<PdlClient>()

    var arbeidsforholdService = ArbeidsforholdService(aaregClient, fellesKodeverkConsumer, pdlClient)

    beforeTest {
        every { fellesKodeverkConsumer.kodeverkKoderBetydninger() } returns fellesKodeverkResponseBody(
            YRKESNAVN,
            YRKESKODE
        )
    }

    it("arbeidstakers stillinger for orgnummer should return correct yrke") {
        val arbeidsforholdList = listOf(validArbeidsforhold())

        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList
        every { pdlClient.fnr(AT_AKTORID) } returns AT_FNR
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)
        verifyStilling(actualStillingList, arbeidsforholdService)
    }

    it("arbeidstakers stillinger for orgnummer should return custom message if navn not found") {
        val arbeidsforholdList = listOf(validArbeidsforhold())
        every { fellesKodeverkConsumer.kodeverkKoderBetydninger() } returns fellesKodeverkResponseBodyWithWrongKode()
        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList
        every { pdlClient.fnr(AT_AKTORID) } returns AT_FNR
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)

        val stilling = actualStillingList[0]
        stilling.yrke shouldBe "Ugyldig yrkeskode $STILLINGSKODE"
    }

    it("arbeidstakers stillinger for orgnummer should only return stillinger with type organization") {
        val arbeidsforholdList = listOf(validArbeidsforhold(), arbeidsforholdTypePerson())

        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList
        every { pdlClient.fnr(AT_AKTORID) } returns AT_FNR
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)
        verifyStilling(actualStillingList, arbeidsforholdService)
    }

    it("arbeidstakers stillinger for orgnummer should only return stillinger valid on date") {
        val arbeidsforholdList = listOf(validArbeidsforhold(), arbeidsforholdWithPassedDate())

        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList
        every { pdlClient.fnr(AT_AKTORID) } returns AT_FNR
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)
        verifyStilling(actualStillingList, arbeidsforholdService)
    }

    it("arbeidstakers stillinger for orgnummer should only return stillinger with orgnummer") {
        val arbeidsforholdList = listOf(validArbeidsforhold(), arbeidsforholdWithWrongOrgnummer())

        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList
        every { pdlClient.fnr(AT_AKTORID) } returns AT_FNR
        val actualStillingList =
            arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)
        verifyStilling(actualStillingList, arbeidsforholdService)
    }

    it("arbeidstakers stillinger for orgnummer should return empty list when no valid arbeidsforhold") {
        val arbeidsforholdList = listOf(
            arbeidsforholdTypePerson(),
            arbeidsforholdWithPassedDate(),
            arbeidsforholdWithWrongOrgnummer()
        )
        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList
        every { pdlClient.fnr(AT_AKTORID) } returns AT_FNR
        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_AKTORID, now(), ORGNUMMER)

        actualStillingList.size shouldBe 0
    }

    it("should map arbeidsforhold with only one arbeidsavtale") {
        val startDate = now().minusYears(1)
        val stopDate = now().plusYears(99)
        val arbeidsforholdList = listOf(
            validArbeidsforhold().apply {
                ansettelsesperiode = ansettelsesperiode(startDate, null)
                arbeidsavtaler =
                    listOf(
                        Arbeidsavtale(
                            yrke = YRKESKODE,
                            stillingsprosent = STILLINGSPROSENT,
                            gyldighetsperiode = Gyldighetsperiode(
                                fom = startDate.withDayOfMonth(1).toString(),
                                tom = stopDate.toString()
                            )
                        )
                    )
            }
        )
        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_FNR, listOf(ORGNUMMER))

        actualStillingList.shouldContainNoNulls()
        val stilling1 = actualStillingList[0]
        stilling1.yrke shouldBe YRKESNAVN_CAPITALIZED
        stilling1.prosent shouldBe arbeidsforholdService.stillingsprosentWithMaxScale(STILLINGSPROSENT)
        stilling1.fom shouldBe startDate
        stilling1.tom shouldBe stopDate
    }

    it("should map arbeidsforhold with only avsluttet arbeidsavtale") {
        val startDate = now().minusYears(1)
        val stopDate = now().minusDays(1)
        val arbeidsforholdList = listOf(
            validArbeidsforhold().apply {
                ansettelsesperiode = ansettelsesperiode(startDate, stopDate)
                arbeidsavtaler =
                    listOf(
                        Arbeidsavtale(
                            yrke = YRKESKODE,
                            stillingsprosent = STILLINGSPROSENT,
                            gyldighetsperiode = Gyldighetsperiode(
                                fom = startDate.withDayOfMonth(1).toString(),
                                tom = stopDate.toString()
                            )
                        )
                    )
            }
        )
        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_FNR, listOf(ORGNUMMER))
        actualStillingList.shouldContainNoNulls()
        val stilling1 = actualStillingList[0]
        stilling1.yrke shouldBe YRKESNAVN_CAPITALIZED
        stilling1.prosent shouldBe arbeidsforholdService.stillingsprosentWithMaxScale(STILLINGSPROSENT)
        stilling1.fom shouldBe startDate
        stilling1.tom shouldBe stopDate
    }

    it("should map arbeidsforhold with two arbeidsavtaler") {
        val stilling1StartDate = now().minusYears(1)
        val stilling1StopDate = now().minusMonths(1).withDayOfMonth(1).minusDays(1)
        val stilling2StartDate = now().minusMonths(1).withDayOfMonth(1)
        val stilling2StopDate = now().plusYears(99)
        val stilling2Stillingsprosent = 80.0
        val arbeidsforholdList = listOf(
            validArbeidsforhold().apply {
                ansettelsesperiode = ansettelsesperiode(stilling1StartDate, null)
                arbeidsavtaler =
                    listOf(
                        Arbeidsavtale(
                            yrke = YRKESKODE,
                            stillingsprosent = STILLINGSPROSENT,
                            gyldighetsperiode = Gyldighetsperiode(
                                fom = stilling1StartDate.withDayOfMonth(1).toString(),
                                tom = stilling1StopDate.toString()
                            )
                        ),
                        Arbeidsavtale(
                            yrke = "123",
                            stillingsprosent = stilling2Stillingsprosent,
                            gyldighetsperiode = Gyldighetsperiode(
                                fom = stilling2StartDate.toString(),
                                tom = stilling2StopDate.toString()
                            )
                        )
                    )
            }
        )
        every { aaregClient.arbeidsforholdArbeidstaker(AT_FNR) } returns arbeidsforholdList

        val actualStillingList = arbeidsforholdService.arbeidstakersStillingerForOrgnummer(AT_FNR, listOf(ORGNUMMER))

        actualStillingList.shouldContainNoNulls()

        val stilling1 = actualStillingList[0]
        stilling1.yrke shouldBe YRKESNAVN_CAPITALIZED
        stilling1.prosent shouldBe arbeidsforholdService.stillingsprosentWithMaxScale(STILLINGSPROSENT)
        stilling1.fom shouldBe stilling1StartDate
        stilling1.tom shouldBe stilling1StopDate

        val stilling2 = actualStillingList[1]
        stilling2.yrke shouldBe "Ugyldig yrkeskode 123"
        stilling2.prosent shouldBe arbeidsforholdService.stillingsprosentWithMaxScale(stilling2Stillingsprosent)
        stilling2.fom shouldBe stilling2StartDate
        stilling2.tom shouldBe stilling2StopDate
    }
})

fun verifyStilling(stillingList: List<Stilling>, arbeidsforholdService: ArbeidsforholdService) {
    stillingList.size shouldBe 1
    val stilling = stillingList[0]
    stilling.yrke shouldBe YRKESNAVN_CAPITALIZED
    stilling.prosent shouldBe arbeidsforholdService.stillingsprosentWithMaxScale(STILLINGSPROSENT)
}

fun ansettelsesperiode(fom: LocalDate?, tom: LocalDate?): Ansettelsesperiode {
    return Ansettelsesperiode(
        periode = Periode(
            fom = fom.toString(),
            tom = tom.toString()
        )
    )
}
