
package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Person(
    var type: Type,
    var aktoerId: String,
    var offentligIdent: String
) : Serializable {
    enum class Type {
        Person
    }
}
