package no.nav.helse.spare

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotliquery.Session

internal interface MeldingRepository {
    fun lagre(id: UUID, type: String, fødselsnummer: Long, opprettet: LocalDateTime, json: String)

    class PostgresRepository(private val dataSource: DataSource) : MeldingRepository {
        override fun lagre(id: UUID, type: String, fødselsnummer: Long, opprettet: LocalDateTime, json: String) {
            sessionOf(dataSource).use { session ->
                val meldingtype = session.opprettMeldingtype(type) ?: session.hentMeldingTypeId(type) ?: throw IllegalStateException("Kunne ikke opprette meldingtype: $type")

                @Language("PostgreSQL")
                val statement = """
                     INSERT INTO melding(id, melding_type_id, opprettet, fnr, json)
                     VALUES (:id, :melding_type_id, :opprettet, :fnr, to_json(:json))
                     ON CONFLICT DO NOTHING
                """

                session.run(queryOf(statement, mapOf(
                    "id" to id,
                    "melding_type_id" to meldingtype,
                    "opprettet" to opprettet,
                    "fnr" to fødselsnummer,
                    "json" to json
                )).asExecute)
            }
        }

        private fun Session.opprettMeldingtype(type: String): Int? {
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO melding_type(navn)
                VALUES(?)
                ON CONFLICT DO NOTHING
                RETURNING id
            """
            return run(queryOf(statement, type)
                .map { it.int("id") }
                .asSingle)
        }

        private fun Session.hentMeldingTypeId(type: String): Int? {
            @Language("PostgreSQL")
            val statement = """
                SELECT id FROM melding_type WHERE navn = ?
            """
            return run(queryOf(statement, type)
                .map { it.int("id") }
                .asSingle)
        }
    }
}

internal class Melding(
    private val id: UUID,
    private val type: Meldingtype,
    private val fødselsnummer: Long,
    private val opprettet: LocalDateTime,
    private val json: String
) {
    fun lagre(repository: MeldingRepository) {
        repository.lagre(id, type.name, fødselsnummer, opprettet, json)
    }

    internal enum class Meldingtype {
        UTBETALING_UTBETALT, UTBETALT,
        UTBETALING_ANNULLERT,
        VEDTAK_FATTET
    }
}
