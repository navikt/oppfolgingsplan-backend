package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.domain.Person
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.pdl.exceptions.NameFromPDLIsNull
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Supplier
import javax.inject.Inject

@Service
class BrukerprofilService @Inject constructor(
    pdlConsumer: PdlConsumer
) {
    private val pdlConsumer: PdlConsumer = pdlConsumer

    fun hentNavnByAktoerId(aktoerId: String): String {
        if (!aktoerId.matches("\\d{13}$".toRegex())) {
            throw RuntimeException()
        }
        val fnr: String = pdlConsumer.fnr(aktoerId)
        return Optional.ofNullable<T>(pdlConsumer.personName(fnr)).orElseThrow<RuntimeException>(
            Supplier<RuntimeException> { NameFromPDLIsNull("Name of person was null") })
    }

    fun hentNavnOgFnr(aktorId: String): Person {
        if (!aktorId.matches("\\d{13}$".toRegex())) {
            throw RuntimeException()
        }
        val fnr: String = pdlConsumer.fnr(aktorId)
        val navn: String = Optional.ofNullable<T>(pdlConsumer.personName(fnr)).orElseThrow<RuntimeException>(
            Supplier<RuntimeException> { NameFromPDLIsNull("Name of person was null") })

        return Person()
            .navn(navn)
            .fnr(fnr)
    }
}

