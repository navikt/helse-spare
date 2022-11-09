import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTarget = "17"

val junitJupiterVersion = "5.9.0"
val testcontainersVersion = "1.17.4"
val rapidsAndRiversVersion = "2022100711511665136276.49acbaae4ed4"
val hikariCPVersion = "5.0.1"
val postgresqlVersion = "42.5.0"
val flywayCoreVersion = "9.7.0"
val kotliqueryVersion = "1.9.0"

val mainClass = "no.nav.helse.spare.AppKt"

plugins {
    kotlin("jvm") version "1.7.20"
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
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
