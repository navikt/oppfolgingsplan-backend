package no.nav.syfo.ereg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonResponse(
        val navn: EregOrganisasjonNavn
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
        val navnelinje1: String,
        val sammensattnavn: String?
)

fun EregOrganisasjonResponse.navn(): String {
    return this.navn.let {
        if (it.sammensattnavn?.isNotEmpty() == true) {
            it.sammensattnavn
        } else {
            it.navnelinje1
        }
    }
}
