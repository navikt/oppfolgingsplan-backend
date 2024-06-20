package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.domain.GodkjenningDTO
import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.POppfoelgingsdialog
import no.nav.syfo.oppfolgingsplan.repository.domain.toOppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.toOppfolgingsplanDTOList
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil.convert
import no.nav.syfo.oppfolgingsplan.util.erArbeidstakeren
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.*

@Repository
class OppfolgingsplanDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val arbeidsoppgaveDAO: ArbeidsoppgaveDAO,
    private val godkjenningerDAO: GodkjenningerDAO,
    private val godkjentplanDAO: GodkjentplanDAO,
    private val tiltakDAO: TiltakDAO
) {

    fun create(oppfolgingsplan: OppfolgingsplanDTO): OppfolgingsplanDTO {
        val id = DbUtil.nesteSekvensverdi("OPPFOELGINGSDIALOG_ID_SEQ", jdbcTemplate)
        val namedParameters = MapSqlParameterSource()
            .addValue("oppfoelgingsdialog_id", id)
            .addValue("uuid", UUID.randomUUID().toString())
            .addValue("aktoer_id", oppfolgingsplan.arbeidstaker.aktoerId)
            .addValue("virksomhetsnummer", oppfolgingsplan.virksomhet.virksomhetsnummer)
            .addValue("opprettet_av", oppfolgingsplan.opprettetAvAktoerId)
            .addValue("created", convert(LocalDateTime.now()))
            .addValue("arbeidsgiver_sist_innlogget", convert(oppfolgingsplan.arbeidsgiver.sistInnlogget))
            .addValue("sykmeldt_sist_innlogget", convert(oppfolgingsplan.arbeidstaker.sistInnlogget))
            .addValue("sist_endret_av", oppfolgingsplan.sistEndretAvAktoerId)
            .addValue("sist_endret", convert(LocalDateTime.now()))
            .addValue("arbeidsgiver_sist_aksessert", convert(oppfolgingsplan.arbeidsgiver.sistAksessert))
            .addValue("sykmeldt_sist_aksessert", convert(oppfolgingsplan.arbeidstaker.sistInnlogget))
            .addValue("arbeidsgiver_sist_endret", convert(oppfolgingsplan.arbeidsgiver.sisteEndring))
            .addValue("sykmeldt_sist_endret", convert(oppfolgingsplan.arbeidstaker.sisteEndring))
            .addValue("samtykke_sykmeldt", null)
            .addValue("samtykke_arbeidsgiver", null)
            .addValue("sm_fnr", oppfolgingsplan.arbeidstaker.fnr)
            .addValue("opprettet_av_fnr", oppfolgingsplan.opprettetAvFnr)
            .addValue("sist_endret_av_fnr", oppfolgingsplan.sistEndretAvFnr)

        namedParameterJdbcTemplate.update(
            "insert into oppfoelgingsdialog " +
                "(oppfoelgingsdialog_id, uuid, aktoer_id, virksomhetsnummer, opprettet_av, created, arbeidsgiver_sist_innlogget, " +
                "sykmeldt_sist_innlogget, sist_endret_av, sist_endret, arbeidsgiver_sist_aksessert, sykmeldt_sist_aksessert, " +
                "arbeidsgiver_sist_endret, sykmeldt_sist_endret, samtykke_sykmeldt, samtykke_arbeidsgiver, sm_fnr, opprettet_av_fnr, sist_endret_av_fnr) " +
                "values(:oppfoelgingsdialog_id, :uuid, :aktoer_id, :virksomhetsnummer, :opprettet_av, :created, :arbeidsgiver_sist_innlogget, " +
                ":sykmeldt_sist_innlogget, :sist_endret_av, :sist_endret, :arbeidsgiver_sist_aksessert, :sykmeldt_sist_aksessert, " +
                ":arbeidsgiver_sist_endret, :sykmeldt_sist_endret, :samtykke_sykmeldt, :samtykke_arbeidsgiver, :sm_fnr, :opprettet_av_fnr, :sist_endret_av_fnr)",
            namedParameters
        )

        return oppfolgingsplan.copy(id = id)
    }

    fun oppfolgingsplanByTiltakId(tiltakId: Long): OppfolgingsplanDTO? {
        val query = """
        select * from oppfoelgingsdialog join tiltak 
        on tiltak.oppfoelgingsdialog_id = oppfoelgingsdialog.oppfoelgingsdialog_id 
        where tiltak_id = ?
    """
        return jdbcTemplate.queryForObject(query, OppfoelgingsdialogRowMapper(), tiltakId)?.toOppfolgingsplanDTO()
    }

    fun finnOppfolgingsplanMedId(oppfoelgingsdialogId: Long): OppfolgingsplanDTO? {
        return jdbcTemplate.queryForObject(
            "select * from oppfoelgingsdialog where oppfoelgingsdialog_id = ?",
            OppfoelgingsdialogRowMapper(),
            oppfoelgingsdialogId
        )?.toOppfolgingsplanDTO()
    }

    fun oppfolgingsplanerKnyttetTilSykmeldt(aktoerId: String): List<OppfolgingsplanDTO> {
        return jdbcTemplate.query(
            "select * from oppfoelgingsdialog where aktoer_id = ?",
            OppfoelgingsdialogRowMapper(),
            aktoerId
        ).toOppfolgingsplanDTOList()
    }

    fun oppfolgingsplanerKnyttetTilSykmeldtogVirksomhet(
        sykmeldtAktoerId: String,
        virksomhetsnummer: String
    ): List<OppfolgingsplanDTO> {
        return jdbcTemplate.query(
            "select * from oppfoelgingsdialog where aktoer_id = ? and virksomhetsnummer = ?",
            OppfoelgingsdialogRowMapper(),
            sykmeldtAktoerId,
            virksomhetsnummer
        ).toOppfolgingsplanDTOList()
    }

    fun populate(oppfolgingplan: OppfolgingsplanDTO): OppfolgingsplanDTO {
        val arbeidsoppgaveListe = arbeidsoppgaveDAO.arbeidsoppgaverByOppfoelgingsdialogId(oppfolgingplan.id)
        val tiltakListe = tiltakDAO.finnTiltakByOppfoelgingsdialogId(oppfolgingplan.id)
        val godkjentPlan = godkjentplanDAO.godkjentPlanByOppfolgingsplanId(oppfolgingplan.id)
        val godkjenninger = godkjenningerDAO.godkjenningerByOppfoelgingsdialogId(oppfolgingplan.id).also {
            if (godkjentPlan != null && it.isNotEmpty()) {
                log.warn("Sletter godkjenning som finnes selv om oppfølgingsplanen allerede er godkjent")
                godkjenningerDAO.deleteAllByOppfoelgingsdialogId(godkjentPlan.oppfoelgingsdialogId)
                emptyList<GodkjenningDTO>()
            }
        }

        return oppfolgingplan.copy(
            godkjentPlan = godkjentPlan,
            godkjenninger = godkjenninger,
            arbeidsoppgaveListe = arbeidsoppgaveListe,
            tiltakListe = tiltakListe
        )
    }

    fun sistEndretAv(oppfoelgingsplanId: Long, innloggetAktoerId: String) {
        val oppfolgingsplan = finnOppfolgingsplanMedId(oppfoelgingsplanId)
            ?: throw RuntimeException("Could not find oppfolgingsplan")

        if (erArbeidstakeren(oppfolgingsplan, innloggetAktoerId)) {
            jdbcTemplate.update(
                "update oppfoelgingsdialog set sykmeldt_sist_endret = ?, sist_endret = ?, sist_endret_av = ? where oppfoelgingsdialog_id = ?",
                convert(LocalDateTime.now()),
                convert(LocalDateTime.now()),
                innloggetAktoerId,
                oppfoelgingsplanId
            )
        } else {
            jdbcTemplate.update(
                "update oppfoelgingsdialog set arbeidsgiver_sist_endret = ?, sist_endret = ?, sist_endret_av = ? where oppfoelgingsdialog_id = ?",
                convert(LocalDateTime.now()),
                convert(LocalDateTime.now()),
                innloggetAktoerId,
                oppfoelgingsplanId
            )
        }
    }

    fun lagreSamtykkeArbeidsgiver(oppfolgingplanId: Long, samtykke: Boolean) {
        jdbcTemplate.update(
            "update oppfoelgingsdialog set samtykke_arbeidsgiver = ? where oppfoelgingsdialog_id = ?",
            samtykke,
            oppfolgingplanId
        )
    }

    fun lagreSamtykkeSykmeldt(oppfolgingsplanId: Long, samtykke: Boolean) {
        jdbcTemplate.update(
            "update oppfoelgingsdialog set samtykke_sykmeldt = ? where oppfoelgingsdialog_id = ?",
            samtykke,
            oppfolgingsplanId
        )
    }

    fun oppdaterSistInnlogget(oppfolgingsplan: OppfolgingsplanDTO, innloggetAktoerId: String) {
        if (innloggetAktoerId == oppfolgingsplan.arbeidstaker.aktoerId) {
            jdbcTemplate.update(
                "update oppfoelgingsdialog set sykmeldt_sist_innlogget = ? where oppfoelgingsdialog_id = ?",
                convert(LocalDateTime.now()),
                oppfolgingsplan.id
            )
        } else {
            jdbcTemplate.update(
                "update oppfoelgingsdialog set arbeidsgiver_sist_innlogget = ? where oppfoelgingsdialog_id = ?",
                convert(LocalDateTime.now()),
                oppfolgingsplan.id
            )
        }
    }

    fun oppdaterSistAksessert(oppfolgingsplan: OppfolgingsplanDTO, innloggetAktoerId: String) {
        if (innloggetAktoerId == oppfolgingsplan.arbeidstaker.aktoerId) {
            jdbcTemplate.update(
                "update oppfoelgingsdialog set sykmeldt_sist_aksessert = ? where oppfoelgingsdialog_id = ?",
                convert(LocalDateTime.now()),
                oppfolgingsplan.id
            )
        } else {
            jdbcTemplate.update(
                "update oppfoelgingsdialog set arbeidsgiver_sist_aksessert = ? where oppfoelgingsdialog_id = ?",
                convert(LocalDateTime.now()),
                oppfolgingsplan.id
            )
        }
    }

    fun avbryt(oppfolgingsplanId: Long, innloggetAktoerId: String) {
        jdbcTemplate.update(
            "update godkjentplan set avbrutt_av = ?, avbrutt_tidspunkt = ? where oppfoelgingsdialog_id = ?",
            innloggetAktoerId,
            convert(LocalDateTime.now()),
            oppfolgingsplanId
        )
    }

    fun nullstillSamtykke(oppfolgingsplanId: Long) {
        jdbcTemplate.update(
            "update oppfoelgingsdialog set samtykke_sykmeldt = ? where oppfoelgingsdialog_id = ?",
            null,
            oppfolgingsplanId
        )
        jdbcTemplate.update(
            "update oppfoelgingsdialog set samtykke_arbeidsgiver = ? where oppfoelgingsdialog_id = ?",
            null,
            oppfolgingsplanId
        )
    }

    fun deleteOppfolgingsplan(oppfolgingsdialogId: Long) {
        jdbcTemplate.update("DELETE ARBEIDSOPPGAVE WHERE OPPFOELGINGSDIALOG_ID = ?", oppfolgingsdialogId)
        jdbcTemplate.query(
            "SELECT * FROM TILTAK WHERE OPPFOELGINGSDIALOG_ID = ?",
            { rs, rowNum -> rs.getString("TILTAK_ID") },
            oppfolgingsdialogId
        )
            .forEach { tiltakId -> jdbcTemplate.update("DELETE KOMMENTAR WHERE TILTAK_ID = ?", tiltakId) }
        jdbcTemplate.update("DELETE TILTAK WHERE OPPFOELGINGSDIALOG_ID = ?", oppfolgingsdialogId)
        jdbcTemplate.update("DELETE GODKJENNING WHERE OPPFOELGINGSDIALOG_ID = ?", oppfolgingsdialogId)
        jdbcTemplate.query(
            "SELECT * from GODKJENTPLAN WHERE OPPFOELGINGSDIALOG_ID = ?",
            { rs, rowNum -> rs.getString("DOKUMENT_UUID") },
            oppfolgingsdialogId
        )
            .forEach { uuid -> jdbcTemplate.update("DELETE DOKUMENT WHERE DOKUMENT_UUID = ?", uuid) }
        jdbcTemplate.query(
            "SELECT * from GODKJENTPLAN WHERE OPPFOELGINGSDIALOG_ID = ?",
            { rs, rowNum -> rs.getLong("GODKJENTPLAN_ID") },
            oppfolgingsdialogId
        )
            .forEach { id -> jdbcTemplate.update("DELETE VEILEDER_BEHANDLING WHERE GODKJENTPLAN_ID = ?", id) }
        jdbcTemplate.update("DELETE GODKJENTPLAN WHERE OPPFOELGINGSDIALOG_ID = ?", oppfolgingsdialogId)
        jdbcTemplate.update("DELETE OPPFOELGINGSDIALOG WHERE OPPFOELGINGSDIALOG_ID = ?", oppfolgingsdialogId)
    }

    fun hentDialogIDerByAktoerId(aktoerId: String): List<Long> {
        return jdbcTemplate.query(
            "SELECT * FROM OPPFOELGINGSDIALOG WHERE AKTOER_ID = ?",
            { rs, rowNum -> rs.getLong("OPPFOELGINGSDIALOG_ID") },
            aktoerId
        )
    }

    private inner class OppfoelgingsdialogRowMapper : RowMapper<POppfoelgingsdialog> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): POppfoelgingsdialog {
            return POppfoelgingsdialog(
                id = rs.getLong("oppfoelgingsdialog_id"),
                uuid = rs.getString("uuid"),
                aktoerId = rs.getString("aktoer_id"),
                virksomhetsnummer = rs.getString("virksomhetsnummer"),
                opprettetAv = rs.getString("opprettet_av"),
                created = convert(rs.getTimestamp("created")),
                sisteInnloggingArbeidsgiver = convert(rs.getTimestamp("arbeidsgiver_sist_innlogget")),
                sisteInnloggingSykmeldt = convert(rs.getTimestamp("sykmeldt_sist_innlogget")),
                sistEndretAv = rs.getString("sist_endret_av"),
                sistEndret = convert(rs.getTimestamp("sist_endret")),
                sistAksessertArbeidsgiver = convert(rs.getTimestamp("arbeidsgiver_sist_aksessert")),
                sistAksessertSykmeldt = convert(rs.getTimestamp("sykmeldt_sist_aksessert")),
                sistEndretArbeidsgiver = convert(rs.getTimestamp("arbeidsgiver_sist_endret")),
                sistEndretSykmeldt = convert(rs.getTimestamp("sykmeldt_sist_endret")),
                samtykkeSykmeldt = rs.getBoolean("samtykke_sykmeldt").let { if (rs.wasNull()) null else it },
                samtykkeArbeidsgiver = rs.getBoolean("samtykke_arbeidsgiver").let { if (rs.wasNull()) null else it },
                smFnr = rs.getString("sm_fnr"),
                opprettetAvFnr = rs.getString("opprettet_av_fnr"),
                sistEndretAvFnr = rs.getString("sist_endret_av_fnr")
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppfolgingsplanDAO::class.java)
    }
}
