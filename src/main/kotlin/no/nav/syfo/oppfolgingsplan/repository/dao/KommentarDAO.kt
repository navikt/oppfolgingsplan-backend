package no.nav.syfo.oppfolgingsplan.repository.dao

import no.nav.syfo.oppfolgingsplan.domain.KommentarDTO
import no.nav.syfo.oppfolgingsplan.repository.domain.PKommentar
import no.nav.syfo.oppfolgingsplan.repository.domain.toKommentarDTO
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
class KommentarDAO(
    private val jdbcTemplate: JdbcTemplate,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    fun finnKommentar(id: Long): KommentarDTO {
        val pKommentar =
            (jdbcTemplate.queryForObject("SELECT * FROM kommentar WHERE kommentar_id = ?", KommentarRowMapper(), id))
        if (pKommentar != null) {
            return pKommentar.toKommentarDTO()
        } else {
            throw KommentarNotFoundException("No Kommentar was found in database with given ID")
        }
    }

    fun finnKommentarerByTiltakId(tiltakId: Long): List<KommentarDTO> {
        val kommentarList = Optional.ofNullable(
            jdbcTemplate.query(
                "SELECT * FROM kommentar WHERE tiltak_id = ?",
                KommentarRowMapper(),
                tiltakId
            )
        ).orElse(emptyList())
        return kommentarList.stream()
            .map { pKommentar: PKommentar -> pKommentar.toKommentarDTO() }
            .collect(Collectors.toList())
    }

    fun create(kommentar: KommentarDTO): KommentarDTO {
        val id = DbUtil.nesteSekvensverdi("KOMMENTAR_ID_SEQ", jdbcTemplate)
        val namedParameters = MapSqlParameterSource()
            .addValue("kommentar_id", id)
            .addValue("tiltak_id", kommentar.tiltakId)
            .addValue("tekst", SqlLobValue(DbUtil.sanitizeUserInput(kommentar.tekst)), Types.CLOB)
            .addValue("sist_endret_av", kommentar.sistEndretAvAktoerId)
            .addValue("sist_endret_dato", DbUtil.convert(LocalDateTime.now()))
            .addValue("opprettet_av", kommentar.opprettetAvAktoerId)
            .addValue("opprettet_dato", DbUtil.convert(LocalDateTime.now()))
        namedParameterJdbcTemplate.update(
            """
                INSERT INTO kommentar 
                (kommentar_id, tiltak_id, tekst, sist_endret_av, sist_endret_dato, opprettet_av, opprettet_dato) 
                VALUES(:kommentar_id, :tiltak_id, :tekst, :sist_endret_av, :sist_endret_dato, :opprettet_av, :opprettet_dato)
                """,
            namedParameters
        )
        return kommentar.copy(id = id)
    }

    fun delete(id: Long?) {
        jdbcTemplate.update("DELETE FROM kommentar WHERE kommentar_id = ?", id)
    }

    fun update(kommentar: KommentarDTO): KommentarDTO {
        val namedParameters = MapSqlParameterSource()
            .addValue("kommentar_id", kommentar.id)
            .addValue("tekst", SqlLobValue(DbUtil.sanitizeUserInput(kommentar.tekst)), Types.CLOB)
            .addValue("sist_endret_av", kommentar.sistEndretAvAktoerId)
            .addValue("sist_endret_dato", DbUtil.convert(LocalDateTime.now()))
        namedParameterJdbcTemplate.update(
            "UPDATE kommentar " +
                "SET tekst = :tekst, sist_endret_av = :sist_endret_av, sist_endret_dato = :sist_endret_dato " +
                "WHERE kommentar_id = :kommentar_id",
            namedParameters
        )
        return kommentar
    }

    private inner class KommentarRowMapper : RowMapper<PKommentar> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): PKommentar {
            val pKommentar = PKommentar(
                id = rs.getLong("kommentar_id"),
                tiltakId = rs.getLong("tiltak_id"),
                tekst = rs.getString("tekst"),
                sistEndretAvAktoerId = rs.getString("sist_endret_av"),
                sistEndretDato = DbUtil.convert(rs.getTimestamp("sist_endret_dato")),
                opprettetAvAktoerId = rs.getString("opprettet_av"),
                opprettetDato = DbUtil.convert(rs.getTimestamp("opprettet_dato"))
            )
            return pKommentar
        }
    }

    class KommentarNotFoundException(message: String) : Exception(message)
}
