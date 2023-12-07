package no.nav.syfo.kontaktinfo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class DigitalKontaktinfo(
    val kanVarsles: Boolean,
    val reservert: Boolean,
    val mobiltelefonnummer: String?,
    val epostadresse: String?,
) : Serializable

fun DigitalKontaktinfo.toKontaktinfo(fnr: String): Kontaktinfo {
    return Kontaktinfo(
        fnr = fnr,
        epost = this.epostadresse,
        tlf = this.mobiltelefonnummer,
        skalHaVarsel = !this.reservert && this.kanVarsles,
    )
}

object KontaktinfoMapper {

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    fun mapPerson(json: String): DigitalKontaktinfo {
        val jsonNode = objectMapper.readTree(json)

        jsonNode.let {
            return DigitalKontaktinfo(
                it["kanVarsles"]?.asBoolean() ?: false,
                it["reservert"]?.asBoolean() ?: true,
                it["mobiltelefonnummer"]?.asText(),
                it["epostadresse"]?.asText()
            )
        }
    }
}
