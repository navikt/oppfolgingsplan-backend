package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.domain.GodkjentPlanDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.PGodkjentPlan
import no.nav.syfo.oppfolgingsplan.repository.domain.toGodkjentPlanDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.toGodkjentPlanDTOList
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.convert
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.nesteSekvensverdi
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDateTime

@Repository
class GodkjentplanDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun godkjentPlanByOppfolgingsplanId(oppfolgingsplanId: Long): GodkjentPlanDTO? {
        val query = "SELECT * FROM godkjentplan WHERE oppfoelgingsdialog_id = ?"
        val godkjentPlan = jdbcTemplate.query(query, GodkjentPlanRowMapper(), oppfolgingsplanId)
        return godkjentPlan.firstOrNull()?.toGodkjentPlanDTO()
    }

    fun godkjentPlanIdByOppfolgingsplanId(oppfolgingsplanId: Long): Long {
        return jdbcTemplate.queryForObject(
            "SELECT godkjentplan_id FROM godkjentplan WHERE oppfoelgingsdialog_id = ?",
            Long::class.java,
            oppfolgingsplanId
        )
    }

    fun hentIkkeJournalfoertePlaner(): List<GodkjentPlanDTO> {
        val godkjentePlaner = jdbcTemplate.query(
            "SELECT * FROM godkjentplan WHERE journalpost_id IS NULL AND delt_med_nav = 1",
            GodkjentPlanRowMapper()
        )
        return godkjentePlaner.toGodkjentPlanDTOList()
    }

    fun create(godkjentPlan: GodkjentPlanDTO): GodkjentPlanDTO {
        val id = nesteSekvensverdi("GODKJENTPLAN_ID_SEQ", jdbcTemplate)
        val namedParameters = MapSqlParameterSource()
            .addValue("godkjentplan_id", id)
            .addValue("oppfoelgingsdialog_id", godkjentPlan.oppfoelgingsdialogId)
            .addValue("dokument_uuid", godkjentPlan.dokumentUuid)
            .addValue("created", convert(LocalDateTime.now()))
            .addValue("fom", convert(godkjentPlan.gyldighetstidspunkt.fom?.atStartOfDay()))
            .addValue("tom", convert(godkjentPlan.gyldighetstidspunkt.tom?.atStartOfDay()))
            .addValue("evalueres", convert(godkjentPlan.gyldighetstidspunkt.evalueres?.atStartOfDay()))
            .addValue("tvungen_godkjenning", godkjentPlan.tvungenGodkjenning)
            .addValue("delt_med_nav", godkjentPlan.deltMedNAV)
            .addValue("delt_med_nav_tidspunkt", convert(godkjentPlan.deltMedNAVTidspunkt))
            .addValue("delt_med_fastlege", false)
        namedParameterJdbcTemplate.update(
            """
                INSERT INTO godkjentplan 
                (godkjentplan_id, oppfoelgingsdialog_id, dokument_uuid, created, fom, tom, evalueres, tvungen_godkjenning, 
                delt_med_nav, delt_med_nav_tidspunkt, delt_med_fastlege) 
                VALUES(:godkjentplan_id, :oppfoelgingsdialog_id, :dokument_uuid, :created, :fom, :tom, :evalueres, :tvungen_godkjenning, 
                :delt_med_nav, :delt_med_nav_tidspunkt, :delt_med_fastlege)
                """,
            namedParameters
        )
        return godkjentPlan.copy(id = id)
    }

    fun sakId(oppfolgingsplanId: Long, sakId: String) {
        jdbcTemplate.update(
            "UPDATE godkjentplan SET sak_id = ? WHERE oppfoelgingsdialog_id = ?",
            sakId,
            oppfolgingsplanId
        )
    }

    fun journalpostId(oppfolgingsplanId: Long, journalpostId: String) {
        jdbcTemplate.update(
            "UPDATE godkjentplan SET journalpost_id = ? WHERE oppfoelgingsdialog_id = ?",
            journalpostId,
            oppfolgingsplanId
        )
    }

    fun delMedNav(oppfolgingsplanId: Long, deltMedNavTidspunkt: LocalDateTime) {
        jdbcTemplate.update(
            "UPDATE godkjentplan SET delt_med_nav = 1, delt_med_nav_tidspunkt = ? WHERE oppfoelgingsdialog_id = ?",
            convert(deltMedNavTidspunkt),
            oppfolgingsplanId
        )
    }

    fun delMedNavTildelEnhet(oppfolgingsplanId: Long) {
        jdbcTemplate.update(
            "UPDATE godkjentplan SET tildelt_enhet = ? WHERE oppfoelgingsdialog_id = ?",
            "",
            oppfolgingsplanId
        )
    }

    fun delMedFastlege(oppfolgingsplanId: Long) {
        jdbcTemplate.update(
            "UPDATE godkjentplan SET delt_med_fastlege = 1, " +
                "delt_med_fastlege_tidspunkt = ? WHERE oppfoelgingsdialog_id = ?",
            convert(LocalDateTime.now()),
            oppfolgingsplanId
        )
    }

    private inner class GodkjentPlanRowMapper : RowMapper<PGodkjentPlan> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): PGodkjentPlan {
            return PGodkjentPlan(
                id = rs.getLong("godkjentplan_id"),
                oppfoelgingsdialogId = rs.getLong("oppfoelgingsdialog_id"),
                dokumentUuid = rs.getString("dokument_uuid"),
                created = convert(rs.getTimestamp("created"))!!,
                fom = convert(rs.getTimestamp("fom")),
                tom = convert(rs.getTimestamp("tom")),
                evalueres = convert(rs.getTimestamp("evalueres")),
                deltMedNavTidspunkt = convert(rs.getTimestamp("delt_med_nav_tidspunkt")),
                deltMedFastlegeTidspunkt = convert(rs.getTimestamp("delt_med_fastlege_tidspunkt")),
                tvungenGodkjenning = rs.getBoolean("tvungen_godkjenning"),
                deltMedNav = rs.getBoolean("delt_med_nav"),
                deltMedFastlege = rs.getBoolean("delt_med_fastlege"),
                avbruttTidspunkt = convert(rs.getTimestamp("avbrutt_tidspunkt")),
                avbruttAv = rs.getString("avbrutt_av"),
                sakId = rs.getString("sak_id"),
                journalpostId = rs.getString("journalpost_id"),
                tildeltEnhet = rs.getString("tildelt_enhet")
            )
        }
    }
}
