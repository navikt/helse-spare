package no.nav.helse.spare

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PostgresRepositoryTest {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var dataSource: DataSource

    private lateinit var repository: MeldingRepository

    @BeforeAll
    internal fun setupAll() {
        postgres.start()
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 2
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        }
        dataSource = HikariDataSource(hikariConfig)
        repository = MeldingRepository.PostgresRepository(dataSource)
    }

    @AfterAll
    internal fun teardown() {
        postgres.stop()
    }

    @BeforeEach
    internal fun setupEach() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .cleanDisabled(false)
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
        val aktørId = 42L
        val type = "EN_TYPE"
        val opprettet = LocalDateTime.now()
        repository.lagre(id, type, fnr, aktørId, opprettet, json())

        assertEquals(1, antallMeldingtyper())
        assertEquals(1, antallMeldinger())
    }

    @Test
    fun `oppretter meldinger`() {
        val fnr = 123456789L
        val aktørId = 42L
        val opprettet = LocalDateTime.now()
        repository.lagre(UUID.randomUUID(), "TYPE_1", fnr, aktørId, opprettet, json())
        repository.lagre(UUID.randomUUID(), "TYPE_2", fnr, aktørId, opprettet, json())

        assertEquals(2, antallMeldingtyper())
        assertEquals(2, antallMeldinger())
    }

    @Test
    fun `oppretter ikke duplikate meldinger`() {
        val id = UUID.randomUUID()
        val fnr = 123456789L
        val aktørId = 42L
        val type = "EN_TYPE"
        val opprettet = LocalDateTime.now()
        repository.lagre(id, type, fnr, aktørId, opprettet, json())
        repository.lagre(id, type, fnr, aktørId, opprettet, json())

        assertEquals(1, antallMeldingtyper())
        assertEquals(1, antallMeldinger())
    }

    @Test
    fun `oppretter ikke duplikate meldinger med ulik type`() {
        val id = UUID.randomUUID()
        val fnr = 123456789L
        val aktørId = 42L
        val opprettet = LocalDateTime.now()
        repository.lagre(id, "TYPE_1", fnr, aktørId, opprettet, json())
        repository.lagre(id, "TYPE_2", fnr, aktørId, opprettet, json())

        assertEquals(2, antallMeldingtyper())
        assertEquals(1, antallMeldinger())
    }

    @Test
    fun `oppretter ikke duplikate meldingstype`() {
        val fnr = 123456789L
        val aktørId = 42L
        val type = "EN_TYPE"
        val opprettet = LocalDateTime.now()
        repository.lagre(UUID.randomUUID(), type, fnr, aktørId, opprettet, json())
        repository.lagre(UUID.randomUUID(), type, fnr, aktørId, opprettet, json())

        assertEquals(1, antallMeldingtyper())
        assertEquals(2, antallMeldinger())
    }

    private fun json() = """
        {"aktørId":"42"}
    """.trimIndent()

    private fun antallMeldinger() =
        using(sessionOf(dataSource)) { it.run(queryOf("SELECT COUNT(1) FROM melding").map { it.int(1) }.asSingle) }

    private fun antallMeldingtyper() =
        using(sessionOf(dataSource)) { it.run(queryOf("SELECT COUNT(1) FROM melding_type").map { it.int(1) }.asSingle) }
}
