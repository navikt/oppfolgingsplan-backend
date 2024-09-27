package no.nav.syfo.aareg.utils

import no.nav.syfo.aareg.Ansettelsesperiode
import no.nav.syfo.aareg.Arbeidsavtale
import no.nav.syfo.aareg.Arbeidsforhold
import no.nav.syfo.aareg.OpplysningspliktigArbeidsgiver
import no.nav.syfo.aareg.OpplysningspliktigArbeidsgiver.Type
import no.nav.syfo.aareg.Periode
import no.nav.syfo.aareg.Person
import java.time.LocalDate

object AaregClientTestUtils {
    const val ORGNUMMER = "123456789"
    const val WRONG_ORGNUMMER = "987654321"
    const val AT_AKTORID = "1234567890987"
    const val AT_FNR = "12345678901"
    val VALID_DATE: LocalDate = LocalDate.now().plusMonths(1)
    val PASSED_DATE: LocalDate = LocalDate.of(1970, 1, 2)
    const val YRKESKODE = "1234567"

    const val YRKESNAVN = "yrkesnavn"
    const val YRKESNAVN_CAPITALIZED = "Yrkesnavn"
    const val STILLINGSPROSENT = 50.0

    fun simpleArbeidsforhold(): Arbeidsforhold {
        return Arbeidsforhold(
            arbeidsgiver =
            OpplysningspliktigArbeidsgiver(
                organisasjonsnummer = ORGNUMMER,
                type = (Type.Organisasjon)
            ),
            arbeidstaker =
            Person(
                aktoerId = AT_AKTORID,
                type = Person.Type.Person,
                offentligIdent = AT_FNR
            )
        )
    }

    fun validArbeidsforhold(): Arbeidsforhold {
        return mockArbeidsforhold(ORGNUMMER, Type.Organisasjon, VALID_DATE)
    }

    fun arbeidsforholdTypePerson(): Arbeidsforhold {
        return mockArbeidsforhold(ORGNUMMER, Type.Person, VALID_DATE)
    }

    fun arbeidsforholdWithPassedDate(): Arbeidsforhold {
        return mockArbeidsforhold(ORGNUMMER, Type.Organisasjon, PASSED_DATE)
    }

    fun arbeidsforholdWithWrongOrgnummer(): Arbeidsforhold {
        return mockArbeidsforhold(WRONG_ORGNUMMER, Type.Organisasjon, VALID_DATE)
    }

    private fun mockArbeidsforhold(orgnummer: String, type: Type, tom: LocalDate): Arbeidsforhold {
        return Arbeidsforhold(
            arbeidsgiver =
            OpplysningspliktigArbeidsgiver(
                organisasjonsnummer = orgnummer,
                type = (type)
            ),
            arbeidstaker =
            Person(
                aktoerId = AT_AKTORID,
                type = Person.Type.Person,
                offentligIdent = AT_FNR
            ),
            ansettelsesperiode =
            Ansettelsesperiode(
                periode =
                Periode(
                    fom = (LocalDate.now().minusYears(1).toString()),
                    tom = (tom.toString())
                )
            ),
            arbeidsavtaler =
            listOf(
                Arbeidsavtale(
                    yrke = (YRKESKODE),
                    stillingsprosent = (STILLINGSPROSENT)
                )
            )
        )
    }
}
