package no.nav.helse.spare

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spare.Melding.Meldingtype.*
import org.flywaydb.core.Flyway

fun main() {
    launchApp(System.getenv())
}

private fun launchApp(env: Map<String, String>) {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DATABASE_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s",
            requireNotNull(env["DATABASE_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(env["DATABASE_DATABASE"]) { "database name must be set if jdbc url is not provided" })
        username = requireNotNull(env["DATABASE_USERNAME"]) { "databasebrukernavn må settes" }
        password = requireNotNull(env["DATABASE_PASSWORD"]) { "databasepassord må settes" }
        maximumPoolSize = 2
        connectionTimeout = Duration.ofSeconds(30).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
    }

    RapidApplication.create(env).apply {
        val ds = HikariDataSource(hikariConfig)
        val repository = MeldingRepository.PostgresRepository(ds)
        MeldingRiver(this, repository, UTBETALT)
        MeldingRiver(this, repository, UTBETALING_UTBETALT)
        MeldingRiver(this, repository, UTBETALING_ANNULLERT)
        MeldingRiver(this, repository, VEDTAK_FATTET)
        register(AppStatusListener(ds))
    }.start()
}

private class AppStatusListener(
    private val dataSource: HikariDataSource
) : RapidsConnection.StatusListener {
    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataSource.close()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        Flyway.configure()
            .dataSource(dataSource)
            .lockRetryCount(-1)
            .load()
            .migrate()
    }
}
