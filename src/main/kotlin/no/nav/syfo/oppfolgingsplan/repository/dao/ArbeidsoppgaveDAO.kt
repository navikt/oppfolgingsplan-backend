package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.domain.ArbeidsoppgaveDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.PArbeidsoppgave
import no.nav.syfo.oppfolgingsplan.repository.domain.toArbeidsoppgaveDTOList
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.convert
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
class ArbeidsoppgaveDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    fun arbeidsoppgaverByOppfoelgingsdialogId(oppfoelgingsdialogId: Long): List<ArbeidsoppgaveDTO> {
        return jdbcTemplate.query(
            "select * from arbeidsoppgave where oppfoelgingsdialog_id = ?",
            ArbeidsoppgaveRowMapper(),
            oppfoelgingsdialogId
        ).toArbeidsoppgaveDTOList()
    }

    fun finnArbeidsoppgave(id: Long): PArbeidsoppgave? {
        return jdbcTemplate.queryForObject(
            "select * from arbeidsoppgave where arbeidsoppgave_id = ?",
            ArbeidsoppgaveRowMapper(),
            id
        )
    }

    @Suppress("LongMethod")
    fun create(arbeidsoppgave: ArbeidsoppgaveDTO): Long {
        val arbeidsoppgaveId =
            jdbcTemplate.queryForObject("SELECT NEXTVAL('ARBEIDSOPPGAVE_ID_SEQ')", Long::class.java)!!
        val namedParameters = MapSqlParameterSource()
            .addValue("arbeidsoppgave_id", arbeidsoppgaveId)
            .addValue("oppfoelgingsdialog_id", arbeidsoppgave.oppfoelgingsdialogId)
            .addValue("navn", sanitizeUserInput(arbeidsoppgave.navn))
            .addValue("er_vurdert_av_sykmeldt", arbeidsoppgave.erVurdertAvSykmeldt)
            .addValue("opprettet_av", arbeidsoppgave.opprettetAvAktoerId)
            .addValue("sist_endret_av", arbeidsoppgave.opprettetAvAktoerId)
            .addValue("sist_endret_dato", LocalDateTime.now())
            .addValue("opprettet_dato", LocalDateTime.now())
            .addValue("gjennomfoering_status", arbeidsoppgave.gjennomfoering?.gjennomfoeringStatus)
            .addValue("paa_annet_sted", arbeidsoppgave.gjennomfoering?.paaAnnetSted)
            .addValue("med_mer_tid", arbeidsoppgave.gjennomfoering?.medMerTid)
            .addValue("med_hjelp", arbeidsoppgave.gjennomfoering?.medHjelp)
            .addValue(
                "beskrivelse",
                SqlLobValue(sanitizeUserInput(arbeidsoppgave.gjennomfoering?.kanBeskrivelse)),
                Types.CLOB
            )
            .addValue(
                "kan_ikke_beskrivelse",
                SqlLobValue(sanitizeUserInput(arbeidsoppgave.gjennomfoering?.kanIkkeBeskrivelse)),
                Types.CLOB
            )

        val sqlQuery = """
            insert into arbeidsoppgave (
                arbeidsoppgave_id,
                oppfoelgingsdialog_id,
                navn,
                er_vurdert_av_sykmeldt,
                opprettet_av,
                sist_endret_av,
                sist_endret_dato,
                opprettet_dato,
                paa_annet_sted,
                med_mer_tid, 
                med_hjelp, 
                beskrivelse, 
                kan_ikke_beskrivelse, 
                gjennomfoering_status
            ) values (
                :arbeidsoppgave_id, 
                :oppfoelgingsdialog_id, 
                :navn,
                :er_vurdert_av_sykmeldt, 
                :opprettet_av, 
                :sist_endret_av,
                :sist_endret_dato, 
                :opprettet_dato, 
                :paa_annet_sted,
                :med_mer_tid, 
                :med_hjelp, 
                :beskrivelse, 
                :kan_ikke_beskrivelse,
                :gjennomfoering_status
            )
        """

        namedParameterJdbcTemplate.update(sqlQuery, namedParameters)
        return arbeidsoppgaveId
    }

    fun update(arbeidsoppgave: ArbeidsoppgaveDTO): Long {
        val namedParameters = MapSqlParameterSource()
            .addValue("arbeidsoppgave_id", arbeidsoppgave.id)
            .addValue("navn", sanitizeUserInput(arbeidsoppgave.navn))
            .addValue("er_vurdert_av_sykmeldt", arbeidsoppgave.erVurdertAvSykmeldt)
            .addValue("sist_endret_av", arbeidsoppgave.sistEndretAvAktoerId)
            .addValue("sist_endret_dato", LocalDateTime.now())
            .addValue("gjennomfoering_status", arbeidsoppgave.gjennomfoering?.gjennomfoeringStatus)
            .addValue("paa_annet_sted", arbeidsoppgave.gjennomfoering?.paaAnnetSted)
            .addValue("med_mer_tid", arbeidsoppgave.gjennomfoering?.medMerTid)
            .addValue("med_hjelp", arbeidsoppgave.gjennomfoering?.medHjelp)
            .addValue(
                "beskrivelse",
                SqlLobValue(sanitizeUserInput(arbeidsoppgave.gjennomfoering?.kanBeskrivelse)),
                Types.CLOB
            )
            .addValue(
                "kan_ikke_beskrivelse",
                SqlLobValue(sanitizeUserInput(arbeidsoppgave.gjennomfoering?.kanIkkeBeskrivelse)),
                Types.CLOB
            )
        namedParameterJdbcTemplate.update(
            """
            update arbeidsoppgave set 
                navn = :navn, 
                er_vurdert_av_sykmeldt = :er_vurdert_av_sykmeldt,
                sist_endret_av = :sist_endret_av, 
                sist_endret_dato = :sist_endret_dato, 
                gjennomfoering_status = :gjennomfoering_status, 
                paa_annet_sted = :paa_annet_sted,
                med_mer_tid = :med_mer_tid, 
                med_hjelp = :med_hjelp, 
                beskrivelse = :beskrivelse, 
                kan_ikke_beskrivelse = :kan_ikke_beskrivelse 
            where arbeidsoppgave_id = :arbeidsoppgave_id
        """,
            namedParameters
        )

        return arbeidsoppgave.id!!
    }

    fun delete(id: Long) {
        jdbcTemplate.update("delete from arbeidsoppgave where arbeidsoppgave_id = ?", id)
    }

    private class ArbeidsoppgaveRowMapper : RowMapper<PArbeidsoppgave> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): PArbeidsoppgave {
            return PArbeidsoppgave(
                id = rs.getLong("arbeidsoppgave_id"),
                oppfoelgingsdialogId = rs.getLong("oppfoelgingsdialog_id"),
                navn = rs.getString("navn"),
                erVurdertAvSykmeldt = rs.getBoolean("er_vurdert_av_sykmeldt"),
                opprettetAvAktoerId = rs.getString("opprettet_av"),
                sistEndretAvAktoerId = rs.getString("sist_endret_av"),
                sistEndretDato = convert(rs.getTimestamp("sist_endret_dato")),
                opprettetDato = convert(rs.getTimestamp("opprettet_dato"))!!,
                gjennomfoeringStatus = rs.getString("gjennomfoering_status"),
                paaAnnetSted = rs.getBoolean("paa_annet_sted"),
                medMerTid = rs.getBoolean("med_mer_tid"),
                medHjelp = rs.getBoolean("med_hjelp"),
                kanBeskrivelse = rs.getString("beskrivelse"),
                kanIkkeBeskrivelse = rs.getString("kan_ikke_beskrivelse")
            )
        }
    }
}
