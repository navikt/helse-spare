plugins {
    alias(libs.plugins.sykepenger.deployable)
}

sykepengerDeployable {
    mainClass = "no.nav.helse.spare.AppKt"
}

dependencies {
    implementation(libs.rapidsAndRivers)
    implementation(libs.hikariCP)
    implementation(libs.postgresql)
    implementation(libs.flyway.databasePostgresql)
    implementation(libs.kotliquery)

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation(libs.tbdLibs.postgresTestdatabaser)
}
