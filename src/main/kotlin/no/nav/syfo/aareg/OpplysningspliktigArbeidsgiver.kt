package no.nav.syfo.aareg

import java.io.Serializable

data class OpplysningspliktigArbeidsgiver(
    var organisasjonsnummer: String,
    var type: Type
) : Serializable {
    enum class Type {
        Organisasjon,
        Person
    }
    companion object {
        private const val serialVersionUID: Long = 1
    }
}
