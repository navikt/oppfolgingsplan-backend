package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.domain.TiltakDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.PTiltak
import no.nav.syfo.oppfolgingsplan.repository.domain.toTiltakDTO
import no.nav.syfo.oppfolgingsplan.repository.util.DbUtil
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
import java.util.*
import java.util.stream.Collectors

@Repository
class TiltakDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val kommentarDAO: KommentarDAO
) {
    fun finnTiltakByOppfoelgingsdialogId(oppfoelgingsdialogId: Long): List<TiltakDTO> {
        val tiltakListe = Optional.ofNullable(
            jdbcTemplate.query(
                "SELECT * FROM tiltak WHERE oppfoelgingsdialog_id = ?",
                TiltakRowMapper(),
                oppfoelgingsdialogId
            )
        ).orElse(emptyList())
        return tiltakListe
            .stream()
            .map { pTiltak: PTiltak -> pTiltak.toTiltakDTO(kommentarDAO.finnKommentarerByTiltakId(pTiltak.id)) }
            .collect(Collectors.toList())
    }

    fun finnTiltakById(id: Long): TiltakDTO {
        val pTiltak = jdbcTemplate.queryForObject("SELECT * FROM tiltak WHERE tiltak_id = ?", TiltakRowMapper(), id)
        if (pTiltak != null) {
            return pTiltak.toTiltakDTO(kommentarDAO.finnKommentarerByTiltakId(pTiltak.id))
        } else {
            throw RuntimeException("No Tiltak was found in database with given ID")
        }
    }

    fun create(tiltak: TiltakDTO): TiltakDTO {
        val id = DbUtil.nesteSekvensverdi("TILTAK_ID_SEQ", jdbcTemplate)
        val namedParameters = MapSqlParameterSource()
            .addValue("tiltak_id", id)
            .addValue("oppfoelgingsdialog_id", tiltak.oppfoelgingsdialogId)
            .addValue("navn", DbUtil.sanitizeUserInput(tiltak.navn))
            .addValue("fom", DbUtil.convert(tiltak.fom))
            .addValue("tom", DbUtil.convert(tiltak.tom))
            .addValue("beskrivelse", SqlLobValue(DbUtil.sanitizeUserInput(tiltak.beskrivelse)), Types.CLOB)
            .addValue(
                "beskrivelse_ikke_aktuelt",
                SqlLobValue(DbUtil.sanitizeUserInput(tiltak.beskrivelseIkkeAktuelt)),
                Types.CLOB
            )
            .addValue("opprettet_av", tiltak.opprettetAvAktoerId)
            .addValue("sist_endret_av", tiltak.sistEndretAvAktoerId)
            .addValue("opprettet_dato", DbUtil.convert(LocalDateTime.now()))
            .addValue("sist_endret_dato", DbUtil.convert(LocalDateTime.now()))
            .addValue("status", tiltak.status)
            .addValue("gjennomfoering", SqlLobValue(DbUtil.sanitizeUserInput(tiltak.gjennomfoering)), Types.CLOB)
        namedParameterJdbcTemplate.update(
            "INSERT INTO tiltak (tiltak_id, oppfoelgingsdialog_id, navn, fom, tom, beskrivelse, beskrivelse_ikke_aktuelt, " +
                "opprettet_av, sist_endret_av, opprettet_dato, sist_endret_dato, status, gjennomfoering ) " +
                "VALUES(:tiltak_id, :oppfoelgingsdialog_id, :navn, :fom, :tom, :beskrivelse, :beskrivelse_ikke_aktuelt, " +
                ":opprettet_av, :sist_endret_av, :opprettet_dato, :sist_endret_dato, :status, :gjennomfoering)",
            namedParameters
        )
        return tiltak.copy(id = id)
    }

    fun update(tiltak: TiltakDTO): TiltakDTO {
        val namedParameters = MapSqlParameterSource()
            .addValue("tiltak_id", tiltak.id)
            .addValue("navn", DbUtil.sanitizeUserInput(tiltak.navn))
            .addValue("fom", DbUtil.convert(tiltak.fom))
            .addValue("tom", DbUtil.convert(tiltak.tom))
            .addValue("beskrivelse", SqlLobValue(DbUtil.sanitizeUserInput(tiltak.beskrivelse)), Types.CLOB)
            .addValue(
                "beskrivelse_ikke_aktuelt",
                SqlLobValue(DbUtil.sanitizeUserInput(tiltak.beskrivelseIkkeAktuelt)),
                Types.CLOB
            )
            .addValue("sist_endret_av", tiltak.sistEndretAvAktoerId)
            .addValue("sist_endret_dato", DbUtil.convert(LocalDateTime.now()))
            .addValue("status", tiltak.status)
            .addValue("gjennomfoering", SqlLobValue(DbUtil.sanitizeUserInput(tiltak.gjennomfoering)), Types.CLOB)
        namedParameterJdbcTemplate.update(
            "UPDATE tiltak SET navn = :navn, fom = :fom, tom = :tom, beskrivelse = :beskrivelse, beskrivelse_ikke_aktuelt = :beskrivelse_ikke_aktuelt, " +
                "sist_endret_av = :sist_endret_av, sist_endret_dato = :sist_endret_dato, status = :status, gjennomfoering = :gjennomfoering WHERE " +
                "tiltak_id = :tiltak_id",
            namedParameters
        )
        return tiltak
    }

    fun deleteById(id: Long?) {
        jdbcTemplate.update("DELETE FROM tiltak WHERE tiltak_id = ?", id)
    }

    private inner class TiltakRowMapper : RowMapper<PTiltak> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): PTiltak {
            val pTiltak = PTiltak(
                id = rs.getLong("tiltak_id"),
                oppfoelgingsdialogId = rs.getLong("oppfoelgingsdialog_id"),
                navn = rs.getString("navn"),
                fom = DbUtil.convert(rs.getTimestamp("fom")),
                tom = DbUtil.convert(rs.getTimestamp("tom")),
                beskrivelse = rs.getString("beskrivelse"),
                beskrivelseIkkeAktuelt = rs.getString("beskrivelse_ikke_aktuelt"),
                opprettetAvAktoerId = rs.getString("opprettet_av"),
                sistEndretAvAktoerId = rs.getString("sist_endret_av"),
                sistEndretDato = DbUtil.convert(rs.getTimestamp("sist_endret_dato")),
                opprettetDato = DbUtil.convert(rs.getTimestamp("opprettet_dato")),
                status = rs.getString("status"),
                gjennomfoering = rs.getString("gjennomfoering")
            )
            return pTiltak
        }
    }
}
