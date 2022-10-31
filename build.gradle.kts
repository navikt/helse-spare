import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTarget = "17"

val junitJupiterVersion = "5.8.2"
val testcontainersVersion = "1.17.1"
val rapidsAndRiversVersion = "2022100711511665136276.49acbaae4ed4"
val hikariCPVersion = "5.0.1"
val vaultJdbcVersion = "1.3.10"
val flywayCoreVersion = "8.5.5"
val kotliqueryVersion = "1.7.0"

val mainClass = "no.nav.helse.spare.AppKt"

plugins {
    kotlin("jvm") version "1.6.21"
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

tasks {

    named<KotlinCompile>("compileKotlin") {
        kotlinOptions.jvmTarget = jvmTarget
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = jvmTarget
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.4.2"
    }
}
