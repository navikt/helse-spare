package no.nav.helse.spare

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.spare.Meldingtype.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

internal class MeldingRiverTest {

    private lateinit var repository: TestRepository
    private lateinit var rapids: TestRapid

    @BeforeEach
    fun setup() {
        repository = TestRepository()
        rapids = TestRapid()
    }

    @Test
    fun `tolker melding`() {
        val type = UTBETALT
        val id = UUID.randomUUID()
        val fnr = Random.nextLong()
        val opprettet = LocalDateTime.now()

        MeldingRiver(rapids, repository, type)
        sendMelding(id, type, "$fnr", opprettet = opprettet)

        assertEquals(1, repository.antall())
        assertEquals(id, repository.id(0))
        assertEquals(fnr, repository.fødselsnummer(0))
        assertEquals(opprettet, repository.opprettet(0))
    }

    @Test
    fun `tolker typer`() {
        values().also { verdier ->
            verdier.forEach {
                MeldingRiver(rapids, repository, it)
                sendMelding(type = it)
            }
            assertEquals(verdier.size, repository.antall())
        }
    }

    @Test
    fun `ignorerer andre typer`() {
        MeldingRiver(rapids, repository, UTBETALT)
        sendMelding(type = VEDTAK_FATTET)
        assertEquals(0, repository.antall())
    }

    private fun sendMelding(id: UUID = UUID.randomUUID(), type: Meldingtype, fnr: String = "${Random.nextLong()}", aktørId: Long = 42L, opprettet: LocalDateTime = LocalDateTime.now()) {
        @Language("JSON")
        val melding = """
        {
          "@id": "$id",
          "@event_name": "${type.name.lowercase()}",
          "@opprettet": "$opprettet",
          "fødselsnummer": "$fnr",
          "aktørId": "$aktørId"
        }
        """
        rapids.sendTestMessage(melding)
    }

    private class TestRepository : MeldingRepository {
        private val ider = mutableSetOf<UUID>()
        private val typer = mutableMapOf<UUID, String>()
        private val fnr = mutableMapOf<UUID, Long>()
        private val opprettettidspunkt = mutableMapOf<UUID, LocalDateTime>()

        fun antall() = ider.size
        fun id(indeks: Int) = ider.elementAt(indeks)
        fun fødselsnummer(indeks: Int) = fnr.getValue(id(indeks))
        fun opprettet(indeks: Int) = opprettettidspunkt.getValue(id(indeks))

        override fun lagre(
            id: UUID,
            type: String,
            fødselsnummer: Long,
            aktørId: Long,
            opprettet: LocalDateTime,
            json: String
        ) {
            ider.add(id)
            typer[id] = type
            fnr[id] = fødselsnummer
            opprettettidspunkt[id] = opprettet
        }
    }
}
