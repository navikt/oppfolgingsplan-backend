package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.domain.GodkjenningDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.PGodkjenning
import no.nav.syfo.oppfolgingsplan.repository.domain.toGodkjenningDTOList
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.convert
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.nesteSekvensverdi
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.sanitizeUserInput
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.support.SqlLobValue
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.time.LocalDateTime

@Repository
class GodkjenningerDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    fun godkjenningerByOppfoelgingsdialogId(oppfoelgingsdialogId: Long): List<GodkjenningDTO> {
        val query = "select * from godkjenning where oppfoelgingsdialog_id = ?"
        val godkjenninger = jdbcTemplate.query(query, GodkjenningerRowMapper(), oppfoelgingsdialogId)
        return godkjenninger.toGodkjenningDTOList()
    }

    fun deleteAllByOppfoelgingsdialogId(oppfoelgingsdialogId: Long) {
        jdbcTemplate.update("delete from godkjenning where oppfoelgingsdialog_id = ?", oppfoelgingsdialogId)
    }

    fun create(godkjenning: GodkjenningDTO) {
        val godkjenningId = nesteSekvensverdi("GODKJENNING_ID_SEQ", jdbcTemplate)
        val namedParameters = MapSqlParameterSource()
            .addValue("godkjenning_id", godkjenningId)
            .addValue("oppfoelgingsdialog_id", godkjenning.oppfoelgingsdialogId)
            .addValue("aktoer_id", godkjenning.godkjentAvAktoerId)
            .addValue("godkjent", godkjenning.godkjent)
            .addValue("beskrivelse", SqlLobValue(sanitizeUserInput(godkjenning.beskrivelse)), Types.CLOB)
            .addValue("fom", convert(godkjenning.gyldighetstidspunkt?.fom))
            .addValue("tom", convert(godkjenning.gyldighetstidspunkt?.tom))
            .addValue("evalueres", convert(godkjenning.gyldighetstidspunkt?.evalueres))
            .addValue("del_med_nav", godkjenning.delMedNav)
            .addValue("created", convert(LocalDateTime.now()))
        namedParameterJdbcTemplate.update(
            """
                insert into godkjenning 
                (godkjenning_id, oppfoelgingsdialog_id, aktoer_id, godkjent, beskrivelse, fom, tom, evalueres, del_med_nav, created) 
                values(:godkjenning_id, :oppfoelgingsdialog_id, :aktoer_id, :godkjent, 
                :beskrivelse, :fom, :tom, :evalueres, :del_med_nav, :created)
                """,
            namedParameters
        )
    }

    private inner class GodkjenningerRowMapper : RowMapper<PGodkjenning> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): PGodkjenning {
            return PGodkjenning(
                id = rs.getLong("godkjenning_id"),
                oppfoelgingsdialogId = rs.getLong("oppfoelgingsdialog_id"),
                aktoerId = rs.getString("aktoer_id"),
                godkjent = rs.getBoolean("godkjent"),
                beskrivelse = rs.getString("beskrivelse"),
                fom = convert(rs.getTimestamp("fom")),
                tom = convert(rs.getTimestamp("tom")),
                evalueres = convert(rs.getTimestamp("evalueres")),
                delMedNav = rs.getBoolean("del_med_nav"),
                created = convert(rs.getTimestamp("created"))!!
            )
        }
    }
}
