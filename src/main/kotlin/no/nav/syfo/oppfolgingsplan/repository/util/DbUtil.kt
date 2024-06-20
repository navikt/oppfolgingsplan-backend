package no.nav.syfo.oppfolgingsplan.repository.util

import org.apache.commons.text.StringEscapeUtils
import org.owasp.html.HtmlPolicyBuilder
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

object DbUtil {

    private val log = LoggerFactory.getLogger(DbUtil::class.java)
    private val sanitizer = HtmlPolicyBuilder().toFactory()

    fun nesteSekvensverdi(sekvensnavn: String, jdbcTemplate: JdbcTemplate): Long {
        return jdbcTemplate.queryForObject("select $sekvensnavn.nextval from dual") { rs, _ -> rs.getLong(1) }!!
    }

    fun convert(timestamp: LocalDate?): Timestamp? {
        return timestamp?.atStartOfDay()?.let { Timestamp.valueOf(it) }
    }

    fun convert(timestamp: LocalDateTime?): Timestamp? {
        return timestamp?.let { Timestamp.valueOf(it) }
    }

    fun convert(timestamp: Timestamp?): LocalDateTime? {
        return timestamp?.toLocalDateTime()
    }

    fun sanitizeUserInput(userinput: String?): String {
        val sanitizedInput =
            StringEscapeUtils.unescapeHtml4(sanitizer.sanitize(StringEscapeUtils.unescapeHtml4(userinput)))
        if (sanitizedInput != userinput && userinput != null) {
            log.warn(
                "Dette er ikke en feil, men burde vært stoppet av regexen i frontend. " +
                    "Finn ut hvorfor og evt. oppdater regex. \n" +
                    "Det ble strippet vekk innhold slik at denne teksten: {} \n" +
                    "ble til denne teksten: {}",
                userinput,
                sanitizedInput
            )
        }
        return sanitizedInput
    }
}
