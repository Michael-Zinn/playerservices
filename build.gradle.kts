plugins {
    kotlin("jvm") version "1.9.0"
    id("io.papermc.paperweight.userdev") version "1.5.6" // the latest version can be found on the Gradle Plugin Portal
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.48.0"
}

group = "de.michaelzinn.playerservices"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.assemble {
    dependsOn("reobfJar")
}
tasks.build {
    dependsOn("shadowJar")
}
tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
