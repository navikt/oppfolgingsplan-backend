package no.nav.syfo.metric

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Metrikk @Autowired constructor(
    private val registry: MeterRegistry
) {
    fun countOutgoingReponses(navn: String, statusCode: Int) {
        registry.counter(
            addPrefix(navn),
            Tags.of(
                "type",
                "info",
                "status",
                statusCode.toString()
            )
        ).increment()
    }

    fun tellHendelse(navn: String) {
        registry.counter(
            addPrefix(navn),
            Tags.of("type", "info")
        ).increment()
    }

    private fun addPrefix(navn: String): String {
        val metricPrefix = "oppfolgingsplan_backend_"
        return metricPrefix + navn
    }
}
