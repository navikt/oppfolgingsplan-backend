
package no.nav.syfo.aareg

import java.io.Serializable

data class Person(
    var type: Type,
    var aktoerId: String,
    var offentligIdent: String
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
    enum class Type {
        Person
    }
}
