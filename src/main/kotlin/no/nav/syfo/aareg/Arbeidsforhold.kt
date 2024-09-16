package no.nav.syfo.aareg

import java.io.Serializable

data class Arbeidsforhold(
    var ansettelsesperiode: Ansettelsesperiode? = null,
    var antallTimerForTimeloennet: List<AntallTimerForTimeloennet>? = null,
    var arbeidsavtaler: List<Arbeidsavtale>? = null,
    var arbeidsforholdId: String? = null,
    var arbeidsgiver: OpplysningspliktigArbeidsgiver? = null,
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
