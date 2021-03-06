package no.nav.helse.spare

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PostgresRepositoryTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource

    private lateinit var repository: MeldingRepository

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
        dataSource = HikariDataSource(hikariConfig)
        repository = MeldingRepository.PostgresRepository(dataSource)
    }

    @AfterAll
    internal fun teardown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setupEach() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    @Test
    fun `oppretter meldingstype`() {
        val id = UUID.randomUUID()
        val fnr = 123456789L
        val type = "EN_TYPE"
        val opprettet = LocalDateTime.now()
        repository.lagre(id, type, fnr, opprettet, "{}")

        assertEquals(1, antallMeldingtyper())
        assertEquals(1, antallMeldinger())
    }

    @Test
    fun `oppretter meldinger`() {
        val fnr = 123456789L
        val opprettet = LocalDateTime.now()
        repository.lagre(UUID.randomUUID(), "TYPE_1", fnr, opprettet, "{}")
        repository.lagre(UUID.randomUUID(), "TYPE_2", fnr, opprettet, "{}")

        assertEquals(2, antallMeldingtyper())
        assertEquals(2, antallMeldinger())
    }

    @Test
    fun `oppretter ikke duplikate meldinger`() {
        val id = UUID.randomUUID()
        val fnr = 123456789L
        val type = "EN_TYPE"
        val opprettet = LocalDateTime.now()
        repository.lagre(id, type, fnr, opprettet, "{}")
        repository.lagre(id, type, fnr, opprettet, "{}")

        assertEquals(1, antallMeldingtyper())
        assertEquals(1, antallMeldinger())
    }

    @Test
    fun `oppretter ikke duplikate meldinger med ulik type`() {
        val id = UUID.randomUUID()
        val fnr = 123456789L
        val opprettet = LocalDateTime.now()
        repository.lagre(id, "TYPE_1", fnr, opprettet, "{}")
        repository.lagre(id, "TYPE_2", fnr, opprettet, "{}")

        assertEquals(2, antallMeldingtyper())
        assertEquals(1, antallMeldinger())
    }

    @Test
    fun `oppretter ikke duplikate meldingstype`() {
        val fnr = 123456789L
        val type = "EN_TYPE"
        val opprettet = LocalDateTime.now()
        repository.lagre(UUID.randomUUID(), type, fnr, opprettet, "{}")
        repository.lagre(UUID.randomUUID(), type, fnr, opprettet, "{}")

        assertEquals(1, antallMeldingtyper())
        assertEquals(2, antallMeldinger())
    }

    private fun antallMeldinger() =
        using(sessionOf(dataSource)) { it.run(queryOf("SELECT COUNT(1) FROM melding").map { it.int(1) }.asSingle) }

    private fun antallMeldingtyper() =
        using(sessionOf(dataSource)) { it.run(queryOf("SELECT COUNT(1) FROM melding_type").map { it.int(1) }.asSingle) }
}
