package no.nav.helse.spare

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spare.Melding.Meldingtype.*
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway

fun main() {
    launchApp(System.getenv())
}

private fun launchApp(env: Map<String, String>) {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = env.getValue("JDBC_URL").removeSuffix("/") + "/" + env.getValue("DB_NAME")
        maximumPoolSize = 3
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }

    val ds = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, env.getValue("VAULT_MOUNTPATH"), env.getValue("DB_NAME") + "-user")
    val migrationRole = env.getValue("DB_NAME") + "-admin"
    val migrationDs = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, env.getValue("VAULT_MOUNTPATH"), migrationRole)

    val repository = MeldingRepository.PostgresRepository(ds)
    RapidApplication.create(env).apply {
        MeldingRiver(this, repository, UTBETALT)
        MeldingRiver(this, repository, UTBETALING_UTBETALT)
        MeldingRiver(this, repository, UTBETALING_ANNULLERT)
        MeldingRiver(this, repository, VEDTAK_FATTET)

        register(AppStatusListener(ds, migrationDs, migrationRole))
    }.start()
}

private class AppStatusListener(
    private val dataSource: HikariDataSource,
    private val migrationDataSource: HikariDataSource,
    private val migrationRole: String
) : RapidsConnection.StatusListener {
    override fun onShutdown(rapidsConnection: RapidsConnection) {
        dataSource.close()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        Flyway.configure()
            .dataSource(migrationDataSource)
            .initSql("SET ROLE \"$migrationRole\"")
            .load()
            .migrate()
        migrationDataSource.close()
    }
}
