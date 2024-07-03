package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class OpplysningspliktigArbeidsgiver(
    var organisasjonsnummer: String,
    var type: Type
) : Serializable {
    enum class Type {
        Organisasjon,
        Person
    }
}
