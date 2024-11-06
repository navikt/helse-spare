package no.nav.helse.spare

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.*

internal class MeldingRiver(
    rapidsConnection: RapidsConnection,
    private val repository: MeldingRepository,
    private val meldingtype: Meldingtype
) : River.PacketListener {
    private companion object {
        private val log = LoggerFactory.getLogger(MeldingRiver::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", meldingtype.name.lowercase())
                it.requireKey("fødselsnummer", "@id", "aktørId")
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error("Forstod ikke ${meldingtype.name.lowercase()} (se sikkerLog for detaljer)")
        sikkerLog.error("Forstod ikke ${meldingtype.name.lowercase()}: ${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val id = UUID.fromString(packet["@id"].asText())
        val fnr = packet["fødselsnummer"].asLong()
        val aktørId = packet["aktørId"].asLong()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        val json = packet.toJson()
        repository.lagre(id, meldingtype.name, fnr, aktørId, opprettet, json)
    }
}
