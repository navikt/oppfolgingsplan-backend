package no.nav.syfo.aareg

import java.io.Serializable
import java.util.Date

data class Arbeidsforhold(
    var ansettelsesperiode: Ansettelsesperiode = Ansettelsesperiode(
        periode = Periode(Date().toString(), null)
    ),
    var antallTimerForTimeloennet: List<AntallTimerForTimeloennet>? = null,
    var arbeidsavtaler: List<Arbeidsavtale> = emptyList(),
    var arbeidsforholdId: String? = null,
    var arbeidsgiver: OpplysningspliktigArbeidsgiver = OpplysningspliktigArbeidsgiver(
        organisasjonsnummer = "",
        type = OpplysningspliktigArbeidsgiver.Type.Organisasjon
    ),
    var arbeidstaker: Person? = null,
    var innrapportertEtterAOrdningen: Boolean = false,
    var navArbeidsforholdId: Int = 0,
    var opplysningspliktig: OpplysningspliktigArbeidsgiver? = null,
    var permisjonPermitteringer: List<PermisjonPermittering>? = null,
    var registrert: String? = null,
    var sistBekreftet: String? = null,
    var sporingsinformasjon: Sporingsinformasjon? = null,
    var type: String? = null,
    var utenlandsopphold: List<Utenlandsopphold>? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
