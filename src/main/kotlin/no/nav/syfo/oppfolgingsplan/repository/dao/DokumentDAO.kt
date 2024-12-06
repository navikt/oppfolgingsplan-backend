package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.repository.domain.Dokument
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.support.SqlLobValue
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

@Repository
class DokumentDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    fun hent(uuid: String?): ByteArray? {
        return jdbcTemplate.queryForObject(
            "select * from dokument where dokument_uuid = ?",
            DokumentRowMapper(),
            uuid
        )?.pdf
    }

    fun lagre(dokument: Dokument) {
        val namedParameters: MapSqlParameterSource = MapSqlParameterSource()
            .addValue("dokument_uuid", dokument.uuid)
            .addValue("pdf", SqlLobValue(dokument.pdf), Types.BLOB)
            .addValue("xml", SqlLobValue(dokument.xml), Types.CLOB)

        namedParameterJdbcTemplate.update(
            "insert into dokument (dokument_uuid, pdf, xml) values(:dokument_uuid, :pdf, :xml)",
            namedParameters
        )
    }

    private inner class DokumentRowMapper : RowMapper<Dokument> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): Dokument {
            return Dokument(
                xml = rs.getString("xml"),
                uuid = rs.getString("dokument_uuid"),
                pdf = rs.getBytes("pdf")
            )
        }
    }
}
