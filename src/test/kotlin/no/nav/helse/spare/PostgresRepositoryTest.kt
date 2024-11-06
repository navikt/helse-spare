package no.nav.helse.spare

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import com.github.navikt.tbd_libs.test_support.TestDataSource
import java.time.LocalDateTime
import java.util.*
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

val databaseContainer = DatabaseContainers.container("spare", CleanupStrategy.tables("melding_type,melding"))

internal class PostgresRepositoryTest {
    private lateinit var testDataSource: TestDataSource
    private val dataSource get() = testDataSource.ds
    private lateinit var repository: MeldingRepository

    @BeforeEach
    internal fun before() {
        testDataSource = databaseContainer.nyTilkobling()
        repository = MeldingRepository.PostgresRepository(dataSource)
    }

    @AfterEach
    internal fun teardown() {
        databaseContainer.droppTilkobling(testDataSource)
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
