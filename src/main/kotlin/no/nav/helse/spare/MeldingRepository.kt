package no.nav.helse.spare

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal interface MeldingRepository {
    fun lagre(id: UUID, type: String, fødselsnummer: Long, opprettet: LocalDateTime, json: String)

    class PostgresRepository(private val dataSource: DataSource) : MeldingRepository {
        override fun lagre(id: UUID, type: String, fødselsnummer: Long, opprettet: LocalDateTime, json: String) {
            val meldingtype = opprettMeldingtype(type)

            @Language("PostgreSQL")
            val statement = """
                 INSERT INTO melding(id, melding_type_id, opprettet, fnr, json)
                 VALUES (:id, :melding_type_id, :opprettet, :fnr, to_json(:json))
                 ON CONFLICT DO NOTHING
            """
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf(statement, mapOf(
                    "id" to id,
                    "melding_type_id" to meldingtype,
                    "opprettet" to opprettet,
                    "fnr" to fødselsnummer,
                    "json" to json
                )).asExecute)
            }
        }

        private fun opprettMeldingtype(type: String): Int {
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO melding_type(navn)
                VALUES(?)
                ON CONFLICT (navn) DO UPDATE SET navn=EXCLUDED.navn
                RETURNING id
            """
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf(statement, type)
                    .map { it.int("id") }
                    .asSingle)
            } ?: throw IllegalStateException("Kunne ikke opprette meldingtype: $type")
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
