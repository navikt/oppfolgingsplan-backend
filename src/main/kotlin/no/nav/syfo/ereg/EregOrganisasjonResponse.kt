package no.nav.syfo.ereg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonResponse(
        val navn: EregOrganisasjonNavn
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EregOrganisasjonNavn(
        val navnelinje1: String,
        val redigertnavn: String?
)

fun EregOrganisasjonResponse.navn(): String {
    return this.navn.let {
        if (it.redigertnavn?.isNotEmpty() == true) {
            it.redigertnavn
        } else {
            it.navnelinje1
        }
    }
}
